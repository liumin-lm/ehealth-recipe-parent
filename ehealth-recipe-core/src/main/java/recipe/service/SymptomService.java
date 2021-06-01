package recipe.service;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.ngari.opbase.xls.mode.ImportExcelInfoDTO;
import com.ngari.opbase.xls.service.IImportExcelInfoService;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.entity.Symptom;
import com.ngari.recipe.recipe.model.SymptomDTO;
import com.ngari.recipe.recipe.service.ISymptomService;
import ctd.account.UserRoleToken;
import ctd.persistence.DAOFactory;
import ctd.persistence.bean.QueryResult;
import ctd.persistence.exception.DAOException;
import ctd.util.AppContextHolder;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.constant.ErrorCode;
import recipe.dao.SymptomDAO;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author renfuhao
 * @date 2020/8/3
 * 症候服务
 */
@RpcBean("symptomService")
public class SymptomService implements ISymptomService {

    private static final Logger logger = LoggerFactory.getLogger(SymptomService.class);
    private static final String SUFFIX_2003 = ".xls";
    private static final String SUFFIX_2007 = ".xlsx";

    @Autowired
    private SymptomDAO symptomDAO;

    /**
     * 获取单元格值（字符串）
     * @param cell
     * @return
     */
    public static String getStrFromCell(Cell cell){
        if(cell==null){
            return null;
        }
        String strCell =cell.getStringCellValue();
        if(strCell!=null){
            strCell = strCell.trim();
            if(StringUtils.isEmpty(strCell)){
                strCell=null;
            }
        }
        return strCell ;
    }

    /**
     * 新增中医症候
     * @param symptom
     * @return
     */
    @RpcService
    public boolean addSymptomForOrgan(SymptomDTO symptom) {
        SymptomDAO symptomDAO = DAOFactory.getDAO(SymptomDAO.class);
        if (null == symptom) {
            throw new DAOException(DAOException.VALUE_NEEDED, "symptom is null");
        }
        logger.info("新增中医症候服务[addSymptomForOrgan]:" + JSONUtils.toString(symptom));
        Symptom convert = ObjectCopyUtils.convert(symptom, Symptom.class);
        //验证症候必要信息
        validate(convert);
        symptomDAO.save(convert);
        return true;

    }

    /**
     * 验证
     * @param symptom
     */
    private void validate(Symptom symptom) {
        if (null == symptom) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "症候信息不能为空");
        }
        if (StringUtils.isEmpty(symptom.getSymptomCode())) {
            throw new DAOException(DAOException.VALUE_NEEDED, "symptomCode is needed");
        }
        if (null == symptom.getSymptomName()) {
            throw new DAOException(DAOException.VALUE_NEEDED, "SympsomName is needed");
        }
        if (null == symptom.getOrganId()) {
            throw new DAOException(DAOException.VALUE_NEEDED, "organId is needed");
        }
        symptom.setCreateDate(new Date());
        symptom.setModifyDate(new Date());
    }

    /**
     * 根据机构Id和查询条件查询中医症候
     * @param organId
     * @param input
     * @param start
     * @param limit
     * @return
     */
    @RpcService
    public QueryResult<SymptomDTO> querSymptomByOrganIdAndName(Integer organId , String input, final int start, final int limit) {
        if (null == organId) {
            return  null;
        }
        SymptomDAO symptomDAO = DAOFactory.getDAO(SymptomDAO.class);
        QueryResult<SymptomDTO> symptomQueryResult = symptomDAO.queryTempByTimeAndName(organId, input, start, limit);
        logger.info("查询中医症候服务[queryymptomByOrganIdAndName]:" + JSONUtils.toString(symptomQueryResult.getItems()));
        return  symptomQueryResult;
    }

    /**
     * 根据机构Id查询中医症候
     * @param organId
     * @return
     */
    @RpcService
    public List<SymptomDTO> querSymptomByOrganId(Integer organId ) {
        if (null == organId) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "机构Id不能为空");
        }
        SymptomDAO symptomDAO = DAOFactory.getDAO(SymptomDAO.class);
        List<Symptom> byOrganId = symptomDAO.findByOrganId(organId);
        logger.info("查询中医症候服务[queryymptomByOrganIdAndName]:" + JSONUtils.toString(byOrganId));
        return  ObjectCopyUtils.convert(byOrganId, SymptomDTO.class);
    }

    /**
     * 根据机构Id 和 症候ID查询中医症候
     * @param organId
     * @return
     */
    @RpcService
    public SymptomDTO querSymptomByOrganIdAndSymptomId(Integer organId , Integer symptomId ) {
        if (null == organId) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "机构Id不能为空");
        }
        if (null == symptomId) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "症候Id不能为空");
        }
        SymptomDAO symptomDAO = DAOFactory.getDAO(SymptomDAO.class);
        Symptom byOrganIdAndSymptomId = symptomDAO.getByOrganIdAndSymptomId(organId,symptomId);
        logger.info("查询中医症候服务[queryymptomByOrganIdAndName]:" + JSONUtils.toString(byOrganIdAndSymptomId));
        return  ObjectCopyUtils.convert(byOrganIdAndSymptomId, SymptomDTO.class);
    }

    /**
     * 症候批量导入
     * @param buf
     * @param originalFilename
     * @param organId
     * @param operator
     * @param ossId
     * @return
     */
    @Override
    public Map<String, Object> readSymptomExcel(byte[] buf, String originalFilename, int organId, String operator, String ossId,String manageUnit) {
        logger.info(operator + "开始 readSymptomExcel 方法" + System.currentTimeMillis() + "当前进程=" + Thread.currentThread().getName());
        StringBuilder errMsgAll = new StringBuilder();
        Map<String, Object> result = Maps.newHashMap();
        if (StringUtils.isEmpty(operator)) {
            result.put("code", 609);
            result.put("msg", "operator is required");
            return result;
        }
        int length = buf.length;
        logger.info("readSymptomExcel byte[] length=" + length);
        int max = 1343518;
        //控制导入数据量
        if (max <= length) {
            result.put("code", 609);
            result.put("msg", "超过7000条数据,请分批导入");
            return result;
        }
        InputStream is = new ByteArrayInputStream(buf);
        //获得用户上传工作簿
        Workbook workbook = null;
        try {
            if (originalFilename.endsWith(SUFFIX_2003)) {
                workbook = new HSSFWorkbook(is);
            } else if (originalFilename.endsWith(SUFFIX_2007)) {
                //使用InputStream需要将所有内容缓冲到内存中，这会占用空间并占用时间
                //当数据量过大时，这里会非常耗时
                workbook = new XSSFWorkbook(is);
            } else {
                result.put("code", 609);
                result.put("msg", "上传文件格式有问题");
                return result;
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("readDrugExcel error ," + e.getMessage(),e);
            result.put("code", 609);
            result.put("msg", "上传文件格式有问题");
            return result;
        }
        Sheet sheet = workbook.getSheetAt(0);
        Integer total = sheet.getLastRowNum();
        if (total == null || total <= 0) {
            result.put("code", 609);
            result.put("msg", "data is required");
            return result;
        }

        Row row;
        List<String> errDrugListMatchList = Lists.newArrayList();
        Integer addNum = 0;
        Integer updateNum = 0;
        List<Symptom> symptomLists=Lists.newArrayList();

        for (int rowIndex = 0; rowIndex <= total; rowIndex++) {
            Symptom symptom;
            //循环获得每个行
            row = sheet.getRow(rowIndex);
            // 判断是否是模板
            if (rowIndex == 0) {
                String symptomCode = getStrFromCell(row.getCell(0));
                String pinyin= getStrFromCell(row.getCell(1));
                String  symptomName= getStrFromCell(row.getCell(2));
                if ("*编码".equals(symptomCode) && "拼音".equals(pinyin) && "*症候名称".equals(symptomName)) {
                    continue;
                } else {
                    result.put("code", 609);
                    result.put("msg", "模板有误，请确认！");
                    return result;
                }

            }
            symptom = new Symptom();
            StringBuilder errMsg = new StringBuilder();
            /*try{*/
            try {
                if (StringUtils.isEmpty(getStrFromCell(row.getCell(0)))) {
                    errMsg.append("症候编码不能为空").append(";");
                }
                /*if (symptomDAO.getByOrganIdAndSymptomCode(organId,getStrFromCell(row.getCell(0))) != null){
                    errMsg.append("该机构此症候编码已存在！").append(";");
                }*/
                symptom.setSymptomCode(getStrFromCell(row.getCell(0)));
            } catch (Exception e) {
                logger.error("症候编号有误 ," + e.getMessage(),e);
                errMsg.append("症候编号有误").append(";");
            }



            try {
                if (!StringUtils.isEmpty(getStrFromCell(row.getCell(1)))) {
                    symptom.setPinYin(getStrFromCell(row.getCell(1)));
                }
            } catch (Exception e) {
                logger.error("拼音有误 ," + e.getMessage(),e);
                errMsg.append("拼音有误").append(";");
            }

            try {
                if (StringUtils.isEmpty(getStrFromCell(row.getCell(2)))) {
                    errMsg.append("症候名称不能为空").append(";");
                }
                /*if (symptomDAO.getByOrganIdAndSymptomName(organId,getStrFromCell(row.getCell(2))) != null){
                    errMsg.append("该机构此症候名称已存在！").append(";");
                }*/
                symptom.setSymptomName(getStrFromCell(row.getCell(2)));
            } catch (Exception e) {
                logger.error("症候名称有误 ," + e.getMessage(),e);
                errMsg.append("症候名称有误").append(";");
            }
            symptom.setOrganId(organId);
            symptom.setCreateDate(new Date());
            symptom.setModifyDate(new Date());
            if (errMsg.length() > 1) {
                int showNum = rowIndex + 1;
                String error = ("【第" + showNum + "行】" + errMsg.substring(0, errMsg.length() - 1)+"\n");
                errMsgAll.append(error);
                errDrugListMatchList.add(error);
            } else {
                symptomLists.add(symptom);
            }
        }
        if (errDrugListMatchList.size()>0){

            IImportExcelInfoService iImportExcelInfoService = AppContextHolder.getBean("opbase.importExcelInfoService", IImportExcelInfoService.class);

            ImportExcelInfoDTO importExcelInfoDTO=new ImportExcelInfoDTO();
            //导入症候记录
            importExcelInfoDTO.setFileName(originalFilename);
            importExcelInfoDTO.setExcelType(15);
            importExcelInfoDTO.setUploaderName(operator);
            importExcelInfoDTO.setUploadDate(new Date());
            importExcelInfoDTO.setStatus(0);
            importExcelInfoDTO.setTotal(total);
            importExcelInfoDTO.setSuccess(addNum);
            importExcelInfoDTO.setExecuterName(operator);
            importExcelInfoDTO.setExecuteDate(new Date());
            importExcelInfoDTO.setErrMsg(errMsgAll.toString());
            importExcelInfoDTO.setOssId(ossId);
            importExcelInfoDTO.setManageUnit(manageUnit);
            importExcelInfoDTO = iImportExcelInfoService.addExcelInfo(importExcelInfoDTO);
            result.put("code", 609);
            result.put("msg", errDrugListMatchList);
            result.put("addNum",addNum);
            result.put("updateNum",updateNum);
            result.put("failNum",total-addNum-updateNum);
            result.put("ImportExcelInfoId",importExcelInfoDTO.getId());
            logger.info(operator + "结束 readDrugExcel 方法" + System.currentTimeMillis() + "当前进程=" + Thread.currentThread().getName());
            return result;

        }else {
            for (Symptom symptom1 : symptomLists) {
                try {
                    //自动匹配功能暂无法提供
                    if (symptomDAO.getByOrganIdAndSymptomCode(organId,symptom1.getSymptomCode()) != null){
                        symptomDAO.updateBySymptomCode(symptom1.getSymptomCode(),symptom1.getPinYin(),symptom1.getSymptomName(),symptom1.getOrganId());
                        updateNum++;
                    }else {
                        symptomDAO.save(symptom1);
                        addNum++;
                    }

                } catch (Exception e) {
                    logger.error("save  Symptom error " + e.getMessage(),e);
                }
            }
        }

        //导入药品记录
        IImportExcelInfoService iImportExcelInfoService = AppContextHolder.getBean("opbase.importExcelInfoService", IImportExcelInfoService.class);

        ImportExcelInfoDTO importExcelInfoDTO=new ImportExcelInfoDTO();
        //导入药品记录
        importExcelInfoDTO.setFileName(originalFilename);
        importExcelInfoDTO.setExcelType(15);
        importExcelInfoDTO.setUploaderName(operator);
        importExcelInfoDTO.setUploadDate(new Date());
        importExcelInfoDTO.setStatus(1);
        importExcelInfoDTO.setTotal(total);
        importExcelInfoDTO.setSuccess(addNum);
        importExcelInfoDTO.setExecuterName(operator);
        importExcelInfoDTO.setExecuteDate(new Date());
        importExcelInfoDTO.setOssId(ossId);
        importExcelInfoDTO = iImportExcelInfoService.addExcelInfo(importExcelInfoDTO);
        result.put("ImportExcelInfoId",importExcelInfoDTO.getId());
        result.put("addNum",addNum);
        result.put("updateNum",updateNum);
        result.put("failNum",total-addNum-updateNum);
        logger.info(operator + "结束 readDrugExcel 方法" + System.currentTimeMillis() + "当前进程=" + Thread.currentThread().getName());
        result.put("code", 200);
        return result;
    }


}
