package recipe.service;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.ngari.opbase.base.service.IBusActionLogService;
import com.ngari.opbase.xls.mode.ImportExcelInfoDTO;
import com.ngari.opbase.xls.service.IImportExcelInfoService;
import com.ngari.patient.dto.OrganDTO;
import com.ngari.patient.service.BasicAPI;
import com.ngari.patient.service.OrganService;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.entity.Symptom;
import com.ngari.recipe.entity.TcmTreatment;
import com.ngari.recipe.recipe.model.SymptomDTO;
import com.ngari.recipe.recipe.model.TcmTreatmentDTO;
import com.ngari.recipe.recipe.service.ITcmTreatmentService;
import ctd.account.UserRoleToken;
import ctd.persistence.DAOFactory;
import ctd.persistence.bean.QueryResult;
import ctd.persistence.exception.DAOException;
import ctd.spring.AppDomainContext;
import ctd.util.AppContextHolder;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.collections.CollectionUtils;
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
import org.springframework.util.ObjectUtils;
import recipe.constant.ErrorCode;
import recipe.dao.SymptomDAO;
import recipe.dao.TcmTreatmentDAO;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author renfuhao
 * @date 2022/03/29
 * 中医治法服务
 */
@RpcBean("tcmTreatmentService")
public class TcmTreatmentService implements ITcmTreatmentService {

    private static final Logger logger = LoggerFactory.getLogger(TcmTreatmentService.class);
    private static final String SUFFIX_2003 = ".xls";
    private static final String SUFFIX_2007 = ".xlsx";

    @Autowired
    TcmTreatmentDAO tcmTreatmentDAO;

    /**
     * 获取单元格值（字符串）
     *
     * @param cell
     * @return
     */
    public static String getStrFromCell(Cell cell) {
        if (cell == null) {
            return null;
        }
        String strCell = cell.getStringCellValue();
        if (strCell != null) {
            strCell = strCell.trim();
            if (StringUtils.isEmpty(strCell)) {
                strCell = null;
            }
        }
        return strCell;
    }

    /**
     * 新增验证
     *
     * @param treatmentDTO
     */
    @RpcService
    private Boolean validateAddNameOrCode(TcmTreatmentDTO treatmentDTO) {
        if (null == treatmentDTO) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "治法信息不能为空");
        }
        if (ObjectUtils.isEmpty(treatmentDTO.getOrganId())) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "机构ID不能为空");
        }
        if (!StringUtils.isEmpty(treatmentDTO.getTreatmentCode())) {
            TcmTreatment byOrganIdAndTreatmentName = tcmTreatmentDAO.getByOrganIdAndTreatmentCode(treatmentDTO.getOrganId(), treatmentDTO.getTreatmentCode());
            if (!ObjectUtils.isEmpty(byOrganIdAndTreatmentName)) {
                return false;
            }
        }
        if (!StringUtils.isEmpty(treatmentDTO.getTreatmentName())) {
            TcmTreatment byOrganIdAndTreatmentCode = tcmTreatmentDAO.getByOrganIdAndTreatmentName(treatmentDTO.getOrganId(), treatmentDTO.getTreatmentName());
            if (!ObjectUtils.isEmpty(byOrganIdAndTreatmentCode)) {
                return false;
            }
        }
        return true;
    }


    /**
     * 更新验证
     *
     * @param treatmentDTO
     */
    @RpcService
    private Boolean validateUpdateNameOrCode(TcmTreatmentDTO treatmentDTO) {
        if (null == treatmentDTO) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "治法信息不能为空");
        }
        if (ObjectUtils.isEmpty(treatmentDTO.getId())) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "治法ID不能为空");
        }
        if (ObjectUtils.isEmpty(treatmentDTO.getOrganId())) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "机构ID不能为空");
        }
        if (!StringUtils.isEmpty(treatmentDTO.getTreatmentCode())) {
            TcmTreatment byOrganIdAndTreatmentCode = tcmTreatmentDAO.getByOrganIdAndTreatmentCode(treatmentDTO.getOrganId(), treatmentDTO.getTreatmentCode());
            if (!ObjectUtils.isEmpty(byOrganIdAndTreatmentCode) && byOrganIdAndTreatmentCode.getId() != treatmentDTO.getId()) {
                return false;
            }
        }
        if (!StringUtils.isEmpty(treatmentDTO.getTreatmentName())) {
            TcmTreatment byOrganIdAndTreatmentName = tcmTreatmentDAO.getByOrganIdAndTreatmentName(treatmentDTO.getOrganId(), treatmentDTO.getTreatmentName());
            if (!ObjectUtils.isEmpty(byOrganIdAndTreatmentName) && byOrganIdAndTreatmentName.getId() != treatmentDTO.getId()) {
                return false;
            }
        }
        return true;
    }

    /**
     * 新增中医治法
     *
     * @param treatmentDTO
     * @return
     */
    @RpcService
    public boolean addTcmTreatmentOrgan(TcmTreatmentDTO treatmentDTO) {
        if (null == treatmentDTO) {
            throw new DAOException(DAOException.VALUE_NEEDED, "治法数据 is null");
        }
        logger.info("新增中医治法服务[addTcmTreatmentOrgan]:" + JSONUtils.toString(treatmentDTO));
        TcmTreatment convert = ObjectCopyUtils.convert(treatmentDTO, TcmTreatment.class);
        //验证症候必要信息
        validate(convert);
        TcmTreatment byOrganIdAndTreatmentName = tcmTreatmentDAO.getByOrganIdAndTreatmentName(treatmentDTO.getOrganId(), treatmentDTO.getTreatmentName());
        if (!ObjectUtils.isEmpty(byOrganIdAndTreatmentName)) {
            throw new DAOException(DAOException.VALUE_NEEDED, "该机构治法 名称已存在!");
        }
        TcmTreatment byOrganIdAndTreatmentCode = tcmTreatmentDAO.getByOrganIdAndTreatmentCode(treatmentDTO.getOrganId(), treatmentDTO.getTreatmentName());
        if (!ObjectUtils.isEmpty(byOrganIdAndTreatmentCode)) {
            throw new DAOException(DAOException.VALUE_NEEDED, "该机构治法 编码已存在!");
        }
        tcmTreatmentDAO.save(convert);
        return true;

    }
    /**
     * 验证
     *
     * @param treatment
     */
    private void validate(TcmTreatment treatment) {
        if (null == treatment) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "治法信息不能为空");
        }
        if (StringUtils.isEmpty(treatment.getTreatmentCode())) {
            throw new DAOException(DAOException.VALUE_NEEDED, "treatmentCode is needed");
        }
        if (null == treatment.getTreatmentName()) {
            throw new DAOException(DAOException.VALUE_NEEDED, "treatmentName is needed");
        }
        if (null == treatment.getOrganId()) {
            throw new DAOException(DAOException.VALUE_NEEDED, "organId is needed");
        }
        if (ObjectUtils.isEmpty(treatment.getId())) {
            treatment.setCreateDate(new Date());
        }
        treatment.setModifyDate(new Date());
    }



    /**
     * 更新中医治法
     *
     * @param treatmentDTO
     * @return
     */
    @RpcService
    public TcmTreatmentDTO updateSymptomForOrgan(TcmTreatmentDTO treatmentDTO) {
        if (null == treatmentDTO) {
            throw new DAOException(DAOException.VALUE_NEEDED, "治法传参 is null");
        }
        logger.info("更新中医治法服务[updateSymptomForOrgan]:" + JSONUtils.toString(treatmentDTO));
        TcmTreatment convert = ObjectCopyUtils.convert(treatmentDTO, TcmTreatment.class);
        //验证症候必要信息
        validate(convert);
        TcmTreatment byOrganIdAndTreatmentName = tcmTreatmentDAO.getByOrganIdAndTreatmentName(treatmentDTO.getOrganId(), treatmentDTO.getTreatmentName());
        if (!ObjectUtils.isEmpty(byOrganIdAndTreatmentName) && byOrganIdAndTreatmentName.getId() != treatmentDTO.getId()) {
            throw new DAOException(DAOException.VALUE_NEEDED, "该机构治法 名称已存在!");
        }
        TcmTreatment byOrganIdAndTreatmentCode = tcmTreatmentDAO.getByOrganIdAndTreatmentCode(treatmentDTO.getOrganId(), treatmentDTO.getTreatmentCode());
        if (!ObjectUtils.isEmpty(byOrganIdAndTreatmentCode) && byOrganIdAndTreatmentCode.getId() != treatmentDTO.getId()) {
            throw new DAOException(DAOException.VALUE_NEEDED, "该机构治法 编码已存在!");
        }
        TcmTreatment update = tcmTreatmentDAO.update(convert);
        return ObjectCopyUtils.convert(update, TcmTreatmentDTO.class);

    }

    /**
     * 批量删除机构治法数据
     *
     * @param tcmTreatmentIds 入参治法参数集合
     */
    @RpcService
    public void deleteTcmTreatmentByIds(List<Integer> tcmTreatmentIds, Integer organId) {
        if (CollectionUtils.isEmpty(tcmTreatmentIds)) {
            throw new DAOException(DAOException.VALUE_NEEDED, "symptomIds is required");
        }
        if (ObjectUtils.isEmpty(organId)) {
            throw new DAOException(DAOException.VALUE_NEEDED, "organId is required");
        }
        IBusActionLogService busActionLogService = AppDomainContext.getBean("opbase.busActionLogService", IBusActionLogService.class);
        OrganService organService = BasicAPI.getService(OrganService.class);
        OrganDTO organDTO = organService.getByOrganId(organId);
        StringBuilder msg = new StringBuilder("【" + organDTO.getName() + "】删除治法");
        for (Integer tcmTreatmentId : tcmTreatmentIds) {
            TcmTreatment tcmTreatment = tcmTreatmentDAO.get(tcmTreatmentId);
            msg.append("【" + tcmTreatment.getId() + "-" + tcmTreatment.getTreatmentName() + "】");
            deletetcmTreatmentById(tcmTreatmentId);
        }
        busActionLogService.recordBusinessLogRpcNew("机构治法管理", "", "TcmTreatment", msg.toString(), organDTO.getName());
    }

    /**
     * 删除机构治法数据
     *
     * @param tcmTreatmentId 入参治法参数
     */
    @RpcService
    public void deletetcmTreatmentById(Integer tcmTreatmentId) {
        if (tcmTreatmentId == null) {
            throw new DAOException(DAOException.VALUE_NEEDED, "tcmTreatmentId is required");
        }
        tcmTreatmentDAO.remove(tcmTreatmentId);
    }


    /**
     * 一键清除机构治法数据
     *
     * @param organId 入参机构参数
     */
    @RpcService
    public void deleteTcmTreatmentByOrganId(Integer organId) {
        if (organId == null) {
            throw new DAOException(DAOException.VALUE_NEEDED, "organId is required");
        }
        OrganService bean = AppDomainContext.getBean("basic.organService", OrganService.class);
        UserRoleToken urt = UserRoleToken.getCurrent();
        OrganDTO byOrganId = bean.getByOrganId(organId);
        if (ObjectUtils.isEmpty(byOrganId)) {
            throw new DAOException(DAOException.VALUE_NEEDED, "未找到该机构!");
        }
        tcmTreatmentDAO.deleteByOrganId(organId);
        IBusActionLogService busActionLogService = AppDomainContext.getBean("opbase.busActionLogService", IBusActionLogService.class);
        busActionLogService.recordBusinessLogRpcNew("机构治法管理", "", "TcmTreatment", "【" + urt.getUserName() + "】一键删除【" + byOrganId.getName()
                + "】治法", byOrganId.getName());
    }

    /**
     * 根据机构Id和查询条件查询中医治法
     *
     * @param organId
     * @param input
     * @param start
     * @param limit
     * @return
     */
    @Override
    public QueryResult<TcmTreatmentDTO> querTcmTreatmentByOrganIdAndName(Integer organId, String input, Boolean isRegulationSymptom, final int start, final int limit) {
        if (null == organId) {
            return null;
        }
        QueryResult<TcmTreatmentDTO> tcmTreatmentDTOQueryResult = tcmTreatmentDAO.queryTempByTimeAndName(organId, input, isRegulationSymptom, start, limit);
        logger.info("查询中医治法服务[querTcmTreatmentByOrganIdAndName]:" + JSONUtils.toString(tcmTreatmentDTOQueryResult.getItems()));
        return tcmTreatmentDTOQueryResult;
    }


    /**
     * 根据机构Id查询中医治法
     *
     * @param organId
     * @return
     */
    @Override
    public List<TcmTreatmentDTO> querTcmTreatmentByOrganId(Integer organId) {
        if (null == organId) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "机构Id不能为空");
        }
        List<TcmTreatment> byOrganId = tcmTreatmentDAO.findByOrganId(organId);
        logger.info("查询中医治法服务[querTcmTreatmentByOrganId]:" + JSONUtils.toString(byOrganId));
        return ObjectCopyUtils.convert(byOrganId, TcmTreatmentDTO.class);
    }

    /**
     * 根据机构Id查询中医治法未关联监管平台数量
     *
     * @param organId
     * @return
     */
    @RpcService
    public Long getCountByOrganId(Integer organId) {
        if (null == organId) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "机构Id不能为空");
        }
        Long byOrganId = tcmTreatmentDAO.getCountByOrganId(organId);
        return byOrganId;
    }




    /**
     * 症候批量导入
     *
     * @param buf
     * @param originalFilename
     * @param organId
     * @param operator
     * @param ossId
     * @return
     */
    @Override
    public Map<String, Object> readTcmTreatmentExcel(byte[] buf, String originalFilename, int organId, String operator, String ossId, String manageUnit) {
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
            logger.error("readDrugExcel error ," + e.getMessage(), e);
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
        List<TcmTreatment> treatmentList = Lists.newArrayList();

        for (int rowIndex = 0; rowIndex <= total; rowIndex++) {
            TcmTreatment treatment;
            //循环获得每个行
            row = sheet.getRow(rowIndex);
            // 判断是否是模板
            if (rowIndex == 0) {
                String symptomCode = getStrFromCell(row.getCell(0));
                String pinyin = getStrFromCell(row.getCell(1));
                String symptomName = getStrFromCell(row.getCell(2));
                if ("*治法编码".equals(symptomCode) && "*治法名称".equals(pinyin) && "关联监管治法编码".equals(symptomName)) {
                    continue;
                } else {
                    result.put("code", 609);
                    result.put("msg", "模板有误，请确认！");
                    return result;
                }

            }
            treatment = new TcmTreatment();
            StringBuilder errMsg = new StringBuilder();
            /*try{*/
            try {
                if (StringUtils.isEmpty(getStrFromCell(row.getCell(0)))) {
                    errMsg.append("治法编码不能为空").append(";");
                }
                treatment.setTreatmentCode(getStrFromCell(row.getCell(0)));
            } catch (Exception e) {
                logger.error("治法编码有误 ," + e.getMessage(), e);
                errMsg.append("治法编码有误").append(";");
            }

            try {
                if (StringUtils.isEmpty(getStrFromCell(row.getCell(1)))) {
                    errMsg.append("治法名称不能为空").append(";");
                }
                treatment.setTreatmentName(getStrFromCell(row.getCell(1)));
            } catch (Exception e) {
                logger.error("治法名称有误 ," + e.getMessage(), e);
                errMsg.append("治法名称有误").append(";");
            }

            try {
                if (!StringUtils.isEmpty(getStrFromCell(row.getCell(1))) && !StringUtils.isEmpty(getStrFromCell(row.getCell(0)))) {
                    TcmTreatment byOrganIdAndTreatmentNameAndTreatmentCode = tcmTreatmentDAO.getByOrganIdAndTreatmentNameAndTreatmentCode(organId, getStrFromCell(row.getCell(1)), getStrFromCell(row.getCell(0)));
                    if (ObjectUtils.isEmpty(byOrganIdAndTreatmentNameAndTreatmentCode)) {
                        if (tcmTreatmentDAO.getByOrganIdAndTreatmentName(organId, getStrFromCell(row.getCell(1))) != null) {
                            errMsg.append("该机构此治法名称已存在！").append(";");
                        }
                        if (tcmTreatmentDAO.getByOrganIdAndTreatmentCode(organId, getStrFromCell(row.getCell(0))) != null) {
                            errMsg.append("该机构此治法编码已存在！").append(";");
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("治法名称编码唯一校验有误 ," + e.getMessage(), e);
                errMsg.append("治法名称编码唯一校验有误").append(";");
            }



            try {
                if (!StringUtils.isEmpty(getStrFromCell(row.getCell(2)))) {
                    treatment.setRegulationTreatmentCode(getStrFromCell(row.getCell(2)));
                }
            } catch (Exception e) {
                logger.error("关联监管治法编码有误 ," + e.getMessage(), e);
                errMsg.append("关联监管治法编码有误").append(";");
            }
            try {
                if (!StringUtils.isEmpty(getStrFromCell(row.getCell(3)))) {
                    treatment.setRegulationTreatmentName(getStrFromCell(row.getCell(3)));
                }
            } catch (Exception e) {
                logger.error("关联监管治法名称有误 ," + e.getMessage(), e);
                errMsg.append("关联监管治法名称有误").append(";");
            }


            treatment.setOrganId(organId);
            treatment.setCreateDate(new Date());
            treatment.setModifyDate(new Date());
            if (errMsg.length() > 1) {
                int showNum = rowIndex + 1;
                String error = ("【第" + showNum + "行】" + errMsg.substring(0, errMsg.length() - 1) + "\n");
                errMsgAll.append(error);
                errDrugListMatchList.add(error);
            } else {
                treatmentList.add(treatment);
            }
        }
        if (errDrugListMatchList.size() > 0) {

            IImportExcelInfoService iImportExcelInfoService = AppContextHolder.getBean("opbase.importExcelInfoService", IImportExcelInfoService.class);

            ImportExcelInfoDTO importExcelInfoDTO = new ImportExcelInfoDTO();
            //导入症候记录
            importExcelInfoDTO.setFileName(originalFilename);
            importExcelInfoDTO.setExcelType(34);
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
            result.put("addNum", addNum);
            result.put("updateNum", updateNum);
            result.put("failNum", total - addNum - updateNum);
            result.put("ImportExcelInfoId", importExcelInfoDTO.getId());
            logger.info(operator + "结束 readDrugExcel 方法" + System.currentTimeMillis() + "当前进程=" + Thread.currentThread().getName());
            return result;

        } else {
            for (TcmTreatment treatment : treatmentList) {
                try {
                    //自动匹配功能暂无法提供
                    if (tcmTreatmentDAO.getByOrganIdAndTreatmentNameAndTreatmentCode(organId, treatment.getTreatmentName(), treatment.getTreatmentCode()) != null) {
                        TcmTreatment tcmTreatment = tcmTreatmentDAO.getByOrganIdAndTreatmentNameAndTreatmentCode(organId, treatment.getTreatmentName(), treatment.getTreatmentCode());
                        TcmTreatment updatevalidate = updatevalidate(tcmTreatment, treatment);
                        tcmTreatmentDAO.update(updatevalidate);
                        updateNum++;
                    } else {
                        tcmTreatmentDAO.save(treatment);
                        addNum++;
                    }

                } catch (Exception e) {
                    logger.error("save  Symptom error " + e.getMessage(), e);
                }
            }
        }

        //导入药品记录
        IImportExcelInfoService iImportExcelInfoService = AppContextHolder.getBean("opbase.importExcelInfoService", IImportExcelInfoService.class);

        ImportExcelInfoDTO importExcelInfoDTO = new ImportExcelInfoDTO();
        //导入药品记录
        importExcelInfoDTO.setFileName(originalFilename);
        importExcelInfoDTO.setExcelType(34);
        importExcelInfoDTO.setUploaderName(operator);
        importExcelInfoDTO.setUploadDate(new Date());
        importExcelInfoDTO.setStatus(1);
        importExcelInfoDTO.setTotal(total);
        importExcelInfoDTO.setSuccess(addNum);
        importExcelInfoDTO.setExecuterName(operator);
        importExcelInfoDTO.setExecuteDate(new Date());
        importExcelInfoDTO.setOssId(ossId);
        importExcelInfoDTO = iImportExcelInfoService.addExcelInfo(importExcelInfoDTO);
        result.put("ImportExcelInfoId", importExcelInfoDTO.getId());
        result.put("addNum", addNum);
        result.put("updateNum", updateNum);
        result.put("failNum", total - addNum - updateNum);
        logger.info(operator + "结束 readDrugExcel 方法" + System.currentTimeMillis() + "当前进程=" + Thread.currentThread().getName());
        result.put("code", 200);
        return result;
    }


    private TcmTreatment updatevalidate(TcmTreatment symptom, TcmTreatment symptom1) {

        if (!ObjectUtils.isEmpty(symptom1.getRegulationTreatmentName())) {
            symptom.setRegulationTreatmentName(symptom1.getRegulationTreatmentName());
        }
        if (!ObjectUtils.isEmpty(symptom1.getRegulationTreatmentCode())) {
            symptom.setRegulationTreatmentCode(symptom1.getRegulationTreatmentCode());
        }
        symptom.setModifyDate(new Date());
        return symptom;
    }



}
