package recipe.service;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.ngari.opbase.base.service.IBusActionLogService;
import com.ngari.opbase.util.OpSecurityUtil;
import com.ngari.opbase.xls.mode.ImportExcelInfoDTO;
import com.ngari.opbase.xls.service.IImportExcelInfoService;
import com.ngari.patient.dto.OrganDTO;
import com.ngari.patient.service.BasicAPI;
import com.ngari.patient.service.OrganService;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.entity.OrganDrugList;
import com.ngari.recipe.entity.Symptom;
import com.ngari.recipe.entity.TcmTreatment;
import com.ngari.recipe.recipe.model.SymptomDTO;
import com.ngari.recipe.recipe.service.ISymptomService;
import ctd.account.UserRoleToken;
import ctd.persistence.DAOFactory;
import ctd.persistence.bean.QueryResult;
import ctd.persistence.exception.DAOException;
import ctd.spring.AppDomainContext;
import ctd.util.AppContextHolder;
import ctd.util.JSONUtils;
import ctd.util.PyConverter;
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
import recipe.dao.OrganDrugListDAO;
import recipe.dao.SymptomDAO;
import recipe.dao.TcmTreatmentDAO;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    @Autowired
    private TcmTreatmentDAO treatmentDAO;

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
     * @param symptom
     */
    @RpcService
    public Boolean validateAddNameOrCode(SymptomDTO symptom) {
        if (null == symptom) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "证候信息不能为空");
        }
        if (ObjectUtils.isEmpty(symptom.getOrganId())) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "机构ID不能为空");
        }
        if (!StringUtils.isEmpty(symptom.getSymptomName())) {
            Symptom byOrganIdAndSymptomName = symptomDAO.getByOrganIdAndSymptomName(symptom.getOrganId(), symptom.getSymptomName());
            if (!ObjectUtils.isEmpty(byOrganIdAndSymptomName)) {
                return false;
            }
        }
        if (!StringUtils.isEmpty(symptom.getSymptomCode())) {
            Symptom byOrganIdAndSymptomCode = symptomDAO.getByOrganIdAndSymptomCode(symptom.getOrganId(), symptom.getSymptomCode());
            if (!ObjectUtils.isEmpty(byOrganIdAndSymptomCode)) {
                return false;
            }
        }
        return true;
    }


    /**
     * 更新验证
     *
     * @param symptom
     */
    @RpcService
    public Boolean validateUpdateNameOrCode(SymptomDTO symptom) {
        if (null == symptom) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "证候信息不能为空");
        }
        if (ObjectUtils.isEmpty(symptom.getSymptomId())) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "证候ID不能为空");
        }
        if (ObjectUtils.isEmpty(symptom.getOrganId())) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "机构ID不能为空");
        }
        if (!StringUtils.isEmpty(symptom.getSymptomCode())) {
            Symptom byOrganIdAndSymptomCode = symptomDAO.getByOrganIdAndSymptomCode(symptom.getOrganId(), symptom.getSymptomCode());
            if (!ObjectUtils.isEmpty(byOrganIdAndSymptomCode) && !byOrganIdAndSymptomCode.getSymptomId().equals(symptom.getSymptomId())) {
                return false;
            }
        }
        if (!StringUtils.isEmpty(symptom.getSymptomName())) {
            Symptom byOrganIdAndSymptomName = symptomDAO.getByOrganIdAndSymptomName(symptom.getOrganId(), symptom.getSymptomName());
            logger.info("中医证候更新验证[validateUpdateNameOrCode]:" + JSONUtils.toString(byOrganIdAndSymptomName));
            if (!ObjectUtils.isEmpty(byOrganIdAndSymptomName) && !byOrganIdAndSymptomName.getSymptomId().equals(symptom.getSymptomId())) {
                return false;
            }
        }
        return true;
    }


    /**
     * 新增中医症候
     *
     * @param symptom
     * @return
     */
    @RpcService
    public boolean addSymptomForOrgan(SymptomDTO symptom) {
        SymptomDAO symptomDAO = DAOFactory.getDAO(SymptomDAO.class);
        if (null == symptom) {
            throw new DAOException(DAOException.VALUE_NEEDED, "symptom is null");
        }
        logger.info("新增中医证候服务[addSymptomForOrgan]:" + JSONUtils.toString(symptom));
        Symptom convert = ObjectCopyUtils.convert(symptom, Symptom.class);
        //验证症候必要信息
        validate(convert);
        Symptom byOrganIdAndSymptomName = symptomDAO.getByOrganIdAndSymptomName(symptom.getOrganId(), symptom.getSymptomName());
        if (!ObjectUtils.isEmpty(byOrganIdAndSymptomName)) {
            throw new DAOException(DAOException.VALUE_NEEDED, "该机构证候 名称已存在!");
        }
        Symptom byOrganIdAndSymptomCode = symptomDAO.getByOrganIdAndSymptomCode(symptom.getOrganId(), symptom.getSymptomCode());
        if (!ObjectUtils.isEmpty(byOrganIdAndSymptomCode)) {
            throw new DAOException(DAOException.VALUE_NEEDED, "该机构证候 编码已存在!");
        }
        symptomDAO.save(convert);
        return true;

    }


    /**
     * 更新中医症候
     *
     * @param symptom
     * @return
     */
    @RpcService
    public SymptomDTO updateSymptomForOrgan(SymptomDTO symptom) {
        SymptomDAO symptomDAO = DAOFactory.getDAO(SymptomDAO.class);
        if (null == symptom) {
            throw new DAOException(DAOException.VALUE_NEEDED, "symptom is null");
        }
        logger.info("更新中医证候服务[updateSymptomForOrgan]:" + JSONUtils.toString(symptom));
        Symptom convert = ObjectCopyUtils.convert(symptom, Symptom.class);
        //验证症候必要信息
        validate(convert);
        Symptom byOrganIdAndSymptomName = symptomDAO.getByOrganIdAndSymptomName(symptom.getOrganId(), symptom.getSymptomName());
        if (!ObjectUtils.isEmpty(byOrganIdAndSymptomName) && !byOrganIdAndSymptomName.getSymptomId().equals(symptom.getSymptomId()) ) {
            throw new DAOException(DAOException.VALUE_NEEDED, "该机构证候 名称已存在!");
        }
        Symptom byOrganIdAndSymptomCode = symptomDAO.getByOrganIdAndSymptomCode(symptom.getOrganId(), symptom.getSymptomCode());
        if (!ObjectUtils.isEmpty(byOrganIdAndSymptomCode) && !byOrganIdAndSymptomCode.getSymptomId().equals(symptom.getSymptomId()) ) {
            throw new DAOException(DAOException.VALUE_NEEDED, "该机构证候 编码已存在!");
        }
        Symptom update = symptomDAO.update(convert);
        return ObjectCopyUtils.convert(update, SymptomDTO.class);

    }


    /**
     * 批量删除机构证候数据
     *
     * @param symptomIds 入参证候参数集合
     */
    @RpcService
    public void deleteSymptomByIds(List<Integer> symptomIds, Integer organId) {
        if (CollectionUtils.isEmpty(symptomIds)) {
            throw new DAOException(DAOException.VALUE_NEEDED, "symptomIds is required");
        }
        if (ObjectUtils.isEmpty(organId)) {
            throw new DAOException(DAOException.VALUE_NEEDED, "organId is required");
        }
        OpSecurityUtil.isAuthorisedOrgan(organId);
        IBusActionLogService busActionLogService = AppDomainContext.getBean("opbase.busActionLogService", IBusActionLogService.class);
        SymptomDAO symptomDAO = DAOFactory.getDAO(SymptomDAO.class);
        OrganService organService = BasicAPI.getService(OrganService.class);
        OrganDTO organDTO = organService.getByOrganId(organId);
        StringBuilder msg = new StringBuilder("批量删除中医证候");
        for (Integer symptomId : symptomIds) {
            Symptom symptom = symptomDAO.get(symptomId);
            msg.append("【" + symptom.getSymptomId() + "-" + symptom.getSymptomName() + "】");
            deleteSymptomById(symptomId);
        }
        busActionLogService.recordBusinessLogRpcNew("中医证候", "", "Symptom", msg.toString(), organDTO.getName());
    }


    /**
     * 删除机构证候数据
     *
     * @param symptomId 入参证候参数
     */
    @RpcService
    public void deleteSymptomById(Integer symptomId) {
        if (symptomId == null) {
            throw new DAOException(DAOException.VALUE_NEEDED, "symptomId is required");
        }
        SymptomDAO symptomDAO = DAOFactory.getDAO(SymptomDAO.class);
        Symptom symptom = symptomDAO.get(symptomId);
        if (symptom != null){
            OpSecurityUtil.isAuthorisedOrgan(symptom.getOrganId());
        }
        symptomDAO.remove(symptomId);
    }


    /**
     * 一键清除机构证候数据
     *
     * @param organId 入参机构参数
     */
    @RpcService
    public void deleteSymptomByOrganId(Integer organId) {
        if (organId == null) {
            throw new DAOException(DAOException.VALUE_NEEDED, "organId is required");
        }
        OpSecurityUtil.isAuthorisedOrgan(organId);
        OrganService bean = AppDomainContext.getBean("basic.organService", OrganService.class);
        UserRoleToken urt = UserRoleToken.getCurrent();
        OrganDTO byOrganId = bean.getByOrganId(organId);
        if (ObjectUtils.isEmpty(byOrganId)) {
            throw new DAOException(DAOException.VALUE_NEEDED, "未找到该机构!");
        }
        SymptomDAO symptomDAO = DAOFactory.getDAO(SymptomDAO.class);
        symptomDAO.deleteByOrganId(organId);
        IBusActionLogService busActionLogService = AppDomainContext.getBean("opbase.busActionLogService", IBusActionLogService.class);
        busActionLogService.recordBusinessLogRpcNew("中医证候", "", "Symptom", "一键清除中医证候", byOrganId.getName());
    }


    /**
     * 验证
     *
     * @param symptom
     */
    private void validate(Symptom symptom) {
        if (null == symptom) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "证候信息不能为空");
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
        /*if (StringUtils.isEmpty(symptom.getPinYin())) {
            String firstLetter = PyConverter.getFirstLetter(symptom.getSymptomName());
            symptom.setPinYin(firstLetter);
        }*/
        if (ObjectUtils.isEmpty(symptom.getSymptomId())) {
            symptom.setCreateDate(new Date());
        }
        symptom.setModifyDate(new Date());
    }

    /**
     * 根据机构Id和查询条件查询中医症候
     *
     * @param organId
     * @param input
     * @param start
     * @param limit
     * @return
     */
    @RpcService
    public QueryResult<SymptomDTO> querSymptomByOrganIdAndNameYypt(Integer organId, String input, Boolean isRegulationSymptom, final int start, final int limit) {
        if (null == organId) {
            return null;
        }
        OpSecurityUtil.isAuthorisedOrgan(organId);
        SymptomDAO symptomDAO = DAOFactory.getDAO(SymptomDAO.class);
        QueryResult<SymptomDTO> symptomQueryResult = symptomDAO.queryTempByTimeAndName(organId, input, isRegulationSymptom, start, limit);
        logger.info("查询中医证候服务[querSymptomByOrganIdAndNameYypt]:" + JSONUtils.toString(symptomQueryResult.getItems()));
        return symptomQueryResult;
    }

    /**
     * 根据机构Id和查询条件查询中医症候
     *
     * @param organId
     * @param input
     * @param start
     * @param limit
     * @return
     */
    @RpcService
    public QueryResult<SymptomDTO> querSymptomByOrganIdAndName(Integer organId, String input, final int start, final int limit) {
        if (null == organId) {
            return null;
        }
        SymptomDAO symptomDAO = DAOFactory.getDAO(SymptomDAO.class);
        QueryResult<SymptomDTO> symptomQueryResult = symptomDAO.queryTempByTimeAndName(organId, input, null, start, limit);
        logger.info("查询中医证候服务[queryymptomByOrganIdAndName]:" + JSONUtils.toString(symptomQueryResult.getItems()));
        return symptomQueryResult;
    }

    /**
     * 根据机构Id查询中医症候
     *
     * @param organId
     * @return
     */
    @RpcService
    public List<SymptomDTO> querSymptomByOrganId(Integer organId) {
        if (null == organId) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "机构Id不能为空");
        }
        SymptomDAO symptomDAO = DAOFactory.getDAO(SymptomDAO.class);
        List<Symptom> byOrganId = symptomDAO.findByOrganId(organId);
        logger.info("查询中医证候服务[queryymptomByOrganIdAndName]:" + JSONUtils.toString(byOrganId));
        return ObjectCopyUtils.convert(byOrganId, SymptomDTO.class);
    }

    /**
     * 根据机构Id和证候编码集合查询中医症候
     *
     * @param organId
     * @return
     */
    @Override
    public List<SymptomDTO> querSymptomByOrganIdAndCodes(Integer organId,List<String> codes) {
        if (null == organId) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "机构Id不能为空");
        }
        if (ObjectUtils.isEmpty(codes)) {
           return Lists.newArrayList();
        }
        SymptomDAO symptomDAO = DAOFactory.getDAO(SymptomDAO.class);
        List<Symptom> byOrganId = symptomDAO.findByOrganIdANDCodes(organId,codes);
        return ObjectCopyUtils.convert(byOrganId, SymptomDTO.class);
    }


    /**
     * 根据机构Id查询中医症候未关联监管平台数量
     *
     * @param organId
     * @return
     */
    @RpcService
    public Long getCountByOrganId(Integer organId) {
        if (null == organId) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "机构Id不能为空");
        }
        Long byOrganId = symptomDAO.getCountByOrganId(organId);
        return byOrganId;
    }

    /**
     * 根据机构Id 和 症候ID查询中医症候
     *
     * @param organId
     * @return
     */
    @RpcService
    public SymptomDTO querSymptomByOrganIdAndSymptomId(Integer organId, Integer symptomId) {
        if (null == organId) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "机构Id不能为空");
        }
        if (null == symptomId) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "症候Id不能为空");
        }
        SymptomDAO symptomDAO = DAOFactory.getDAO(SymptomDAO.class);
        Symptom byOrganIdAndSymptomId = symptomDAO.getByOrganIdAndSymptomId(organId, symptomId);
        logger.info("查询中医证候服务[queryymptomByOrganIdAndName]:" + JSONUtils.toString(byOrganIdAndSymptomId));
        return ObjectCopyUtils.convert(byOrganIdAndSymptomId, SymptomDTO.class);
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
    public Map<String, Object> readSymptomExcel(byte[] buf, String originalFilename, int organId, String operator, String ossId, String manageUnit) {
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
        List<Symptom> symptomLists = Lists.newArrayList();
        Map<Integer, String> textMap = Maps.newHashMap();
        Map<Integer, String> keyMap = Maps.newHashMap();

        for (int rowIndex = 0; rowIndex <= total; rowIndex++) {
            Symptom symptom;
            //循环获得每个行
            row = sheet.getRow(rowIndex);
            // 判断是否是模板
            if (rowIndex == 0) {
                String symptomCode = getStrFromCell(row.getCell(0));
                String pinyin = getStrFromCell(row.getCell(1));
                String symptomName = getStrFromCell(row.getCell(2));
                if ("*证候编码".equals(symptomCode) && "*证候名称".equals(pinyin) && "证候拼音".equals(symptomName)) {
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
                    errMsg.append("证候编码不能为空").append(";");
                }
                symptom.setSymptomCode(getStrFromCell(row.getCell(0)));
            } catch (Exception e) {
                logger.error("证候编码有误 ," + e.getMessage(), e);
                errMsg.append("证候编码有误").append(";");
            }

            try {
                if (StringUtils.isEmpty(getStrFromCell(row.getCell(1)))) {
                    errMsg.append("证候名称不能为空").append(";");
                }
                symptom.setSymptomName(getStrFromCell(row.getCell(1)));
            } catch (Exception e) {
                logger.error("证候名称有误 ," + e.getMessage(), e);
                errMsg.append("证候名称有误").append(";");
            }

            try {
                if (!StringUtils.isEmpty(getStrFromCell(row.getCell(1))) && !StringUtils.isEmpty(getStrFromCell(row.getCell(0)))) {
                    Symptom byOrganIdAndSymptomNameAndSymptomCode = symptomDAO.getByOrganIdAndSymptomNameAndSymptomCode(organId, getStrFromCell(row.getCell(1)), getStrFromCell(row.getCell(0)));
                    if (ObjectUtils.isEmpty(byOrganIdAndSymptomNameAndSymptomCode)) {
                        if (symptomDAO.getByOrganIdAndSymptomName(organId, getStrFromCell(row.getCell(1))) != null) {
                            errMsg.append("该机构此证候名称已存在！").append(";");
                        }
                        if (symptomDAO.getByOrganIdAndSymptomCode(organId, getStrFromCell(row.getCell(0))) != null) {
                            errMsg.append("该机构此证候编码已存在！").append(";");
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("症候名称编码唯一校验有误 ," + e.getMessage(), e);
                errMsg.append("症候名称编码唯一校验有误").append(";");
            }
            if (!StringUtils.isEmpty(getStrFromCell(row.getCell(1))) && !StringUtils.isEmpty(getStrFromCell(row.getCell(0)))) {
                if (textMap != null && textMap.size() > 0) {
                    Set<Integer> integers = textMap.keySet();
                    for (Integer integer : integers) {
                        if ( textMap.get(integer).equals(getStrFromCell(row.getCell(1)))) {
                            int i = integer + 1;
                            errMsg.append("证候名称与第[" + i + "]行重复!").append(";");
                        }
                    }

                }
                textMap.put(rowIndex, getStrFromCell(row.getCell(1)));
                if (keyMap != null && keyMap.size() > 0) {
                    Set<Integer> integers = keyMap.keySet();
                    for (Integer integer : integers) {
                        if ( keyMap.get(integer).equals(getStrFromCell(row.getCell(0)))) {
                            int i = integer + 1;
                            errMsg.append("证候编码与第[" + i + "]行重复!").append(";");
                        }
                    }

                }
                keyMap.put(rowIndex, getStrFromCell(row.getCell(0)));
            }

            try {
                if (!StringUtils.isEmpty(getStrFromCell(row.getCell(2)))) {
                    symptom.setPinYin(getStrFromCell(row.getCell(2)));
                } /*else {
                    if (!StringUtils.isEmpty(getStrFromCell(row.getCell(1)))) {
                        String firstLetter = PyConverter.getFirstLetter(symptom.getSymptomName());
                        symptom.setPinYin(firstLetter);
                    }
                }*/
            } catch (Exception e) {
                logger.error("拼音有误 ," + e.getMessage(), e);
                errMsg.append("拼音有误").append(";");
            }

            try {
                if (!StringUtils.isEmpty(getStrFromCell(row.getCell(3)))) {
                    TcmTreatment byOrganIdAndTreatmentCode = treatmentDAO.getByOrganIdAndTreatmentCode(organId, getStrFromCell(row.getCell(3)));
                    if (ObjectUtils.isEmpty(byOrganIdAndTreatmentCode)){
                        errMsg.append("机构未查询出此治法编码").append(";");
                    }else {
                        if (StringUtils.isEmpty(getStrFromCell(row.getCell(4)))){
                            errMsg.append("关联治法编码正确,但治法名称未给出").append(";");
                        }else {
                            if (!StringUtils.isEmpty(getStrFromCell(row.getCell(3))) && !StringUtils.isEmpty(getStrFromCell(row.getCell(4)))) {
                                TcmTreatment byOrganIdAndTreatmentNameAndTreatmentCode = treatmentDAO.getByOrganIdAndTreatmentNameAndTreatmentCode(organId, getStrFromCell(row.getCell(4)), getStrFromCell(row.getCell(3)));
                                if (ObjectUtils.isEmpty(byOrganIdAndTreatmentNameAndTreatmentCode)){
                                    errMsg.append("治法编码与名称联查为空!").append(";");
                                }
                            }
                            symptom.setTreatmentCode(getStrFromCell(row.getCell(3)));
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("关联治法编码有误 ," + e.getMessage(), e);
                errMsg.append("关联治法编码有误").append(";");
            }

            try {
                if (!StringUtils.isEmpty(getStrFromCell(row.getCell(4)))) {
                    TcmTreatment byOrganIdAndTreatmentName = treatmentDAO.getByOrganIdAndTreatmentName(organId, getStrFromCell(row.getCell(4)));
                    if (ObjectUtils.isEmpty(byOrganIdAndTreatmentName)){
                        errMsg.append("机构未查询出此治法名称").append(";");
                    }else {
                        if (StringUtils.isEmpty(getStrFromCell(row.getCell(3)))){
                            errMsg.append("关联治法名称正确,但治法编码未给出").append(";");
                        }else {
                            if (!StringUtils.isEmpty(getStrFromCell(row.getCell(3))) && !StringUtils.isEmpty(getStrFromCell(row.getCell(4)))) {
                                TcmTreatment byOrganIdAndTreatmentNameAndTreatmentCode = treatmentDAO.getByOrganIdAndTreatmentNameAndTreatmentCode(organId, getStrFromCell(row.getCell(4)), getStrFromCell(row.getCell(3)));
                                if (ObjectUtils.isEmpty(byOrganIdAndTreatmentNameAndTreatmentCode)){
                                    errMsg.append("治法编码与名称联查为空!").append(";");
                                }
                            }
                            symptom.setTreatmentName(getStrFromCell(row.getCell(4)));
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("关联治法名称有误 ," + e.getMessage(), e);
                errMsg.append("关联治法名称有误").append(";");
            }


            try {
                if (!StringUtils.isEmpty(getStrFromCell(row.getCell(5)))) {
                    symptom.setRegulationSymptomCode(getStrFromCell(row.getCell(5)));
                }
            } catch (Exception e) {
                logger.error("关联监管证候编码有误 ," + e.getMessage(), e);
                errMsg.append("关联监管证候编码有误").append(";");
            }
            try {
                if (!StringUtils.isEmpty(getStrFromCell(row.getCell(6)))) {
                    symptom.setRegulationSymptomName(getStrFromCell(row.getCell(6)));
                }
            } catch (Exception e) {
                logger.error("关联监管证候名称有误 ," + e.getMessage(), e);
                errMsg.append("关联监管证候名称有误").append(";");
            }


            symptom.setOrganId(organId);
            symptom.setCreateDate(new Date());
            symptom.setModifyDate(new Date());
            if (errMsg.length() > 1) {
                int showNum = rowIndex + 1;
                String error = ("【第" + showNum + "行】" + errMsg.substring(0, errMsg.length() - 1) + "\n");
                errMsgAll.append(error);
                errDrugListMatchList.add(error);
            } else {
                symptomLists.add(symptom);
            }
        }
        if (errDrugListMatchList.size() > 0) {

            IImportExcelInfoService iImportExcelInfoService = AppContextHolder.getBean("opbase.importExcelInfoService", IImportExcelInfoService.class);

            ImportExcelInfoDTO importExcelInfoDTO = new ImportExcelInfoDTO();
            //导入症候记录
            importExcelInfoDTO.setFileName(originalFilename);
            importExcelInfoDTO.setExcelType(35);
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
            for (Symptom symptom1 : symptomLists) {
                try {
                    //根据名称和编码 结合唯一去判断是否更新 非唯一名称或编码单独重复数据在导入数据处理时被过滤
                    if (symptomDAO.getByOrganIdAndSymptomNameAndSymptomCode(organId, symptom1.getSymptomName(), symptom1.getSymptomCode()) != null) {
                        Symptom symptom = symptomDAO.getByOrganIdAndSymptomNameAndSymptomCode(organId, symptom1.getSymptomName(), symptom1.getSymptomCode());
                        Symptom updatevalidate = updatevalidate(symptom, symptom1);
                        symptomDAO.update(updatevalidate);
                        updateNum++;
                    } else {
                        if (validateAddNameOrCode(ObjectCopyUtils.convert(symptom1,SymptomDTO.class))){
                            symptomDAO.save(symptom1);
                            addNum++;
                        }else {
                            continue;
                        }
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
        importExcelInfoDTO.setExcelType(35);
        importExcelInfoDTO.setUploaderName(operator);
        importExcelInfoDTO.setUploadDate(new Date());
        importExcelInfoDTO.setStatus(1);
        importExcelInfoDTO.setTotal(total);
        importExcelInfoDTO.setSuccess(addNum);
        importExcelInfoDTO.setExecuterName(operator);
        importExcelInfoDTO.setExecuteDate(new Date());
        importExcelInfoDTO.setOssId(ossId);
        importExcelInfoDTO.setManageUnit(manageUnit);
        importExcelInfoDTO = iImportExcelInfoService.addExcelInfo(importExcelInfoDTO);
        result.put("ImportExcelInfoId", importExcelInfoDTO.getId());
        result.put("addNum", addNum);
        result.put("updateNum", updateNum);
        result.put("failNum", total - addNum - updateNum);
        logger.info(operator + "结束 readDrugExcel 方法" + System.currentTimeMillis() + "当前进程=" + Thread.currentThread().getName());
        result.put("code", 200);
        return result;
    }


    private Symptom updatevalidate(Symptom symptom, Symptom symptom1) {
        if (!ObjectUtils.isEmpty(symptom1.getPinYin())) {
            symptom.setPinYin(symptom1.getPinYin());
        }else {
            symptom.setPinYin(null);
        }
        if (!ObjectUtils.isEmpty(symptom1.getTreatmentCode())) {
            symptom.setTreatmentCode(symptom1.getTreatmentCode());
        }else {
            symptom.setTreatmentCode(null);
        }
        if (!ObjectUtils.isEmpty(symptom1.getTreatmentName())) {
            symptom.setTreatmentName(symptom1.getTreatmentName());
        }else {
            symptom.setTreatmentName(null);
        }
        if (!ObjectUtils.isEmpty(symptom1.getRegulationSymptomCode())) {
            symptom.setRegulationSymptomCode(symptom1.getRegulationSymptomCode());
        }else {
            symptom.setRegulationSymptomCode(null);
        }
        if (!ObjectUtils.isEmpty(symptom1.getRegulationSymptomName())) {
            symptom.setRegulationSymptomName(symptom1.getRegulationSymptomName());
        }else {
            symptom.setRegulationSymptomName(null);
        }
        symptom.setModifyDate(new Date());
        return symptom;
    }

    /**
     * 组装上传到监管平台的中医证候、中医治法
     * */
    public Symptom assembleMultipleSymptom(Integer organId,String symptomIds) {
        logger.info("assembleMultipleSymptom organId={},symptomIds={}", organId, symptomIds);
        Symptom symptoms = new Symptom();
        String[] symptomIdArray = symptomIds.split(";");
        logger.info("assembleMultipleSymptom symptomIdArray={}", JSONUtils.toString(symptomIdArray));
        if (new Integer(1).equals(symptomIdArray.length)) {
            Symptom symptom = symptomDAO.getByOrganIdAndSymptomCode(organId, symptomIds);
            if(null != symptom){
                if(null != symptom.getTreatmentCode()){
                    TcmTreatment tcmTreatment = treatmentDAO.getByOrganIdAndTreatmentCode(organId, symptom.getTreatmentCode());
                    if(null != tcmTreatment){
                        if(null != tcmTreatment.getRegulationTreatmentCode()){
                            symptom.setTreatmentCode(tcmTreatment.getRegulationTreatmentCode());
                        }
                        if(null != tcmTreatment.getRegulationTreatmentName()){
                            symptom.setTreatmentName(tcmTreatment.getRegulationTreatmentName());
                        }
                    }
                }
            }
            logger.info("assembleMultipleSymptom symptom1={}", JSONUtils.toString(symptom));
            return symptom;
        }
        StringBuilder regulationSymptomCodes = new StringBuilder();
        StringBuilder regulationSymptomNames = new StringBuilder();
        StringBuilder regulationTreatmentCode = new StringBuilder();
        StringBuilder regulationTreatmentName = new StringBuilder();
        try {
            for (String symptomId : symptomIdArray) {
                Symptom symptom = symptomDAO.getByOrganIdAndSymptomCode(organId, symptomId);
                logger.info("assembleMultipleSymptom symptom={}", JSONUtils.toString(symptom));
                if(null != symptom){
                    if (null != symptom.getRegulationSymptomCode()) {
                        regulationSymptomCodes.append(symptom.getRegulationSymptomCode()).append("|");
                    }
                    if (null != symptom.getRegulationSymptomName()) {
                        regulationSymptomNames.append(symptom.getRegulationSymptomName()).append("|");
                    }
                    String treatmentCode = symptom.getTreatmentCode();
                    TcmTreatment tcmTreatment = treatmentDAO.getByOrganIdAndTreatmentCode(organId, treatmentCode);
                    logger.info("assembleMultipleSymptom tcmTreatment={}", JSONUtils.toString(tcmTreatment));
                    if(null != tcmTreatment){
                        if (null != tcmTreatment.getRegulationTreatmentCode()) {
                            regulationTreatmentCode.append(tcmTreatment.getRegulationTreatmentCode()).append("|");
                        }
                        if (null != tcmTreatment.getRegulationTreatmentName()) {
                            regulationTreatmentName.append(tcmTreatment.getRegulationTreatmentName()).append("|");
                        }
                    }
                }
            }
            if (StringUtils.isNotEmpty(regulationSymptomCodes.toString())) {
                symptoms.setRegulationSymptomCode(regulationSymptomCodes.substring(0, regulationSymptomCodes.length() - 1));
            }
            if (StringUtils.isNotEmpty(regulationSymptomNames.toString())) {
                symptoms.setRegulationSymptomName(regulationSymptomNames.substring(0, regulationSymptomNames.length() - 1));
            }
            if (StringUtils.isNotEmpty(regulationTreatmentCode.toString())) {
                symptoms.setTreatmentCode(regulationTreatmentCode.substring(0, regulationTreatmentCode.length() - 1));
            }
            if (StringUtils.isNotEmpty(regulationTreatmentName.toString())) {
                symptoms.setTreatmentName(regulationTreatmentName.substring(0, regulationTreatmentName.length() - 1));
            }
            logger.info("assembleMultipleSymptom symptoms={}",JSONUtils.toString(symptoms));
        }catch (Exception e){
            logger.error("assembleMultipleSymptom error ",e);
        }
        return symptoms;
    }


}
