package recipe.drugTool.service;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.ngari.his.recipe.mode.OrganDrugInfoTO;
import com.ngari.opbase.base.service.IBusActionLogService;
import com.ngari.opbase.log.mode.DataSyncDTO;
import com.ngari.opbase.log.service.IDataSyncLogService;
import com.ngari.opbase.xls.mode.ImportExcelInfoDTO;
import com.ngari.opbase.xls.service.IImportExcelInfoService;
import com.ngari.patient.service.OrganService;
import com.ngari.recipe.drugTool.service.ISaleDrugToolService;
import com.ngari.recipe.entity.*;
import ctd.account.UserRoleToken;
import ctd.persistence.exception.DAOException;
import ctd.spring.AppDomainContext;
import ctd.util.AppContextHolder;
import ctd.util.BeanUtils;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import recipe.aop.LogRecord;
import recipe.dao.*;
import recipe.service.DrugsEnterpriseConfigService;
import recipe.thread.RecipeBusiThreadPool;
import recipe.util.RedisClient;

import javax.annotation.Resource;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static ctd.persistence.DAOFactory.getDAO;

/**
 * created by renfuhao on 2020/6/11
 */
@RpcBean(value = "saleDrugToolService")
public class SaleDrugToolService implements ISaleDrugToolService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SaleDrugToolService.class);
    private static final String SUFFIX_2003 = ".xls";
    private static final String SUFFIX_2007 = ".xlsx";
    public static final String KEY_THE_DRUG_SYNC = "THE_SALEDRUG_SYNC";

    @Autowired
    private RedisClient redisClient;

    @Resource
    private DrugListDAO drugListDAO;

    @Resource
    private OrganDrugListDAO organDrugListDAO;

    @Resource
    private DrugsEnterpriseDAO drugsEnterpriseDAO;

    @Resource
    private OrganAndDrugsepRelationDAO relationDAO;


    @Resource
    private SaleDrugListDAO saleDrugListDAO;

    @Resource
    private ImportDrugRecordDAO importDrugRecordDAO;

    @Autowired
    private SaleDrugListSyncFieldDAO saleDrugListSyncFieldDAO;

    private IDataSyncLogService dataSyncLogService = AppDomainContext.getBean("opbase.dataSyncLogService", IDataSyncLogService.class);


    @Override
    public Map<String, Object> readDrugExcel(byte[] buf, String originalFilename, int organId, String operator, String ossId) {
        LOGGER.info(operator + "开始 readDrugExcel 方法" + System.currentTimeMillis() + "当前进程=" + Thread.currentThread().getName());
        StringBuilder errMsgAll = new StringBuilder();
        Map<String, Object> result = Maps.newHashMap();
        if (StringUtils.isEmpty(operator)) {
            result.put("code", 609);
            result.put("msg", "operator is required");
            return result;
        }
        int length = buf.length;
        LOGGER.info("readDrugExcel byte[] length=" + length);
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
            LOGGER.error("readDrugExcel error ," + e.getMessage(), e);
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

        SaleDrugList drug;
        Row row;
        List<String> errDrugListMatchList = Lists.newArrayList();
        Integer addNum = 0;
        Integer updateNum = 0;
        List<SaleDrugList> drugLists = Lists.newArrayList();

        for (int rowIndex = 0; rowIndex <= total; rowIndex++) {
            //循环获得每个行
            row = sheet.getRow(rowIndex);
            // 判断是否是模板
            if (rowIndex == 0) {
                String drugCode = getStrFromCell(row.getCell(2));
                String drugName = getStrFromCell(row.getCell(3));
                String status = getStrFromCell(row.getCell(5));
                if ("药品名*".equals(drugCode) && "商品名*".equals(drugName) && "状态".equals(status)) {
                    continue;
                } else {
                    result.put("code", 609);
                    result.put("msg", "模板有误，请确认！");
                    return result;
                }

            }
            drug = new SaleDrugList();
            StringBuilder errMsg = new StringBuilder();
            /*try{*/

            try {
                if (StringUtils.isEmpty(getStrFromCell(row.getCell(0)))) {
                    errMsg.append("【平台药品编码】未填写").append(";");
                }
                DrugList drugList = drugListDAO.get(Integer.parseInt(getStrFromCell(row.getCell(0)).trim()));
                if (ObjectUtils.isEmpty(drugList)) {
                    errMsg.append("平台未找到该平台通用药品").append(";");
                }
                if (StringUtils.isNotEmpty(getStrFromCell(row.getCell(0)))) {
                    SaleDrugList byDrugIdAndOrganId = saleDrugListDAO.getByDrugIdAndOrganId(Integer.parseInt(getStrFromCell(row.getCell(0)).trim()), organId);
                    if (!ObjectUtils.isEmpty(byDrugIdAndOrganId)) {
                        errMsg.append("药企已存在药品关联该平台药品").append(";");
                    }
                    drug.setDrugId(Integer.parseInt(getStrFromCell(row.getCell(0)).trim()));
                }
            } catch (Exception e) {
                LOGGER.error("平台药品编码有误 ," + e.getMessage(), e);
                errMsg.append("平台药品编码有误").append(";");
            }

            try {
                if (StringUtils.isEmpty(getStrFromCell(row.getCell(2)))) {
                    errMsg.append("【药品名】未填写").append(";");
                }
                drug.setDrugName(getStrFromCell(row.getCell(2)));
            } catch (Exception e) {
                LOGGER.error("药品名有误 ," + e.getMessage(), e);
                errMsg.append("药品名有误").append(";");
            }
            try {
                if (StringUtils.isEmpty(getStrFromCell(row.getCell(3)))) {
                    errMsg.append("【商品名】未填写").append(";");
                }
                drug.setSaleName(getStrFromCell(row.getCell(3)));
            } catch (Exception e) {
                LOGGER.error("药品商品名有误 ," + e.getMessage(), e);
                errMsg.append("药品商品名有误").append(";");
            }

            try {
                if (StringUtils.isEmpty(getStrFromCell(row.getCell(4)))) {
                    errMsg.append("【药企药品编码】未填写").append(";");
                }
                drug.setOrganDrugCode(getStrFromCell(row.getCell(4)));
                drug.setSaleDrugCode(getStrFromCell(row.getCell(4)));
            } catch (Exception e) {
                LOGGER.error("药企药品编码有误 ," + e.getMessage(), e);
                errMsg.append("药企药品编码有误").append(";");
            }

                /*try {
                    if (StringUtils.isNotEmpty(getStrFromCell(row.getCell(5)))) {
                        if ("有效".equals(getStrFromCell(row.getCell(5)).trim())){
                            drug.setStatus(1);
                        }else if ("无效".equals(getStrFromCell(row.getCell(5)).trim())){
                            drug.setStatus(0);
                        }
                    }
                } catch (Exception e) {
                    LOGGER.error("平台药品状态有误 ," + e.getMessage(), e);
                    errMsg.append("平台药品状态有误").append(";");
                }*/
            try {
                if (StringUtils.isEmpty(getStrFromCell(row.getCell(6)))) {
                    errMsg.append("【价格(不含税)】未填写").append(";");
                }
                String priceCell = getStrFromCell(row.getCell(6));
                drug.setPrice(new BigDecimal(priceCell));
            } catch (Exception e) {
                LOGGER.error("药品价格(不含税)有误 ," + e.getMessage(), e);
                errMsg.append("药品价格(不含税)有误").append(";");
            }
            try {
                if (StringUtils.isNotEmpty(getStrFromCell(row.getCell(7)))) {
                    drug.setRate(Double.parseDouble(getStrFromCell(row.getCell(7))));
                }
            } catch (Exception e) {
                LOGGER.error("药品税率有误," + e.getMessage(), e);
                errMsg.append("药品税率有误").append(";");
            }
            if (StringUtils.isNotEmpty(getStrFromCell(row.getCell(0)))) {
                DrugList drugList = drugListDAO.get(Integer.parseInt(getStrFromCell(row.getCell(0))));
                if (!ObjectUtils.isEmpty(drugList)) {
                    drug.setDrugSpec(drugList.getDrugSpec());
                }
            }

            if (!ObjectUtils.isEmpty(drug.getRate())) {
                drug.setRatePrice(drug.getPrice().doubleValue() * (1 - drug.getRate()));
            }

            drug.setStatus(1);
            drug.setOrganId(organId);
            drug.setInventory(new BigDecimal(100));
            drug.setCreateDt(new Date());
            drug.setLastModify(new Date());

            if (errMsg.length() > 1) {
                int showNum = rowIndex + 1;
                String error = ("【第" + showNum + "行】" + errMsg.substring(0, errMsg.length() - 1) + "\n");
                errMsgAll.append(error);
                errDrugListMatchList.add(error);
            } else {
                drugLists.add(drug);
            }
        }
        if (errDrugListMatchList.size() > 0) {

            IImportExcelInfoService iImportExcelInfoService = AppContextHolder.getBean("opbase.importExcelInfoService", IImportExcelInfoService.class);

            ImportExcelInfoDTO importExcelInfoDTO = new ImportExcelInfoDTO();
            //导入药品记录
            importExcelInfoDTO.setFileName(originalFilename);
            importExcelInfoDTO.setExcelType(14);
            importExcelInfoDTO.setUploaderName(operator);
            importExcelInfoDTO.setUploadDate(new Date());
            importExcelInfoDTO.setStatus(0);
            importExcelInfoDTO.setTotal(total);
            importExcelInfoDTO.setSuccess(addNum);
            importExcelInfoDTO.setExecuterName(operator);
            importExcelInfoDTO.setExecuteDate(new Date());
            importExcelInfoDTO.setErrMsg(errMsgAll.toString());
            importExcelInfoDTO.setOssId(ossId);
            importExcelInfoDTO = iImportExcelInfoService.addExcelInfo(importExcelInfoDTO);
            result.put("code", 609);
            result.put("msg", errDrugListMatchList);
            result.put("addNum", addNum);
            result.put("updateNum", updateNum);
            result.put("failNum", total - addNum - updateNum);
            result.put("ImportExcelInfoId", importExcelInfoDTO.getId());
            LOGGER.info(operator + "结束 readDrugExcel 方法" + System.currentTimeMillis() + "当前进程=" + Thread.currentThread().getName());
            return result;

        } else {
            for (SaleDrugList drugList : drugLists) {
                try {
                    List<SaleDrugList> byOrganIdAndDrugCode = saleDrugListDAO.findByOrganIdAndDrugCode(organId, drugList.getOrganDrugCode());
                    if (!ObjectUtils.isEmpty(byOrganIdAndDrugCode)) {
                        SaleDrugList saleDrugList = byOrganIdAndDrugCode.get(0);
                        saleDrugList.setDrugId(drugList.getDrugId());
                        saleDrugList.setDrugName(drugList.getDrugName());
                        saleDrugList.setSaleName(drugList.getSaleName());
                        saleDrugList.setDrugSpec(drugList.getDrugSpec());
                        saleDrugList.setOrganId(organId);
                        saleDrugList.setStatus(1);
                        saleDrugList.setPrice(drugList.getPrice());
                        saleDrugListDAO.update(drugList);
                        updateNum++;
                    } else {
                        saleDrugListDAO.save(drugList);
                        addNum++;
                    }
                } catch (Exception e) {
                    LOGGER.error("save or update drugListMatch error " + e.getMessage(), e);
                }
            }
        }

        //导入药品记录
        IImportExcelInfoService iImportExcelInfoService = AppContextHolder.getBean("opbase.importExcelInfoService", IImportExcelInfoService.class);

        ImportExcelInfoDTO importExcelInfoDTO = new ImportExcelInfoDTO();
        //导入药品记录
        importExcelInfoDTO.setFileName(originalFilename);
        importExcelInfoDTO.setExcelType(14);
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
        LOGGER.info(operator + "结束 readDrugExcel 方法" + System.currentTimeMillis() + "当前进程=" + Thread.currentThread().getName());
        result.put("code", 200);
        return result;
    }

    /**
     * 获取单元格值（字符串）
     *
     * @param cell
     * @return
     */
    private String getStrFromCell(Cell cell) {
        if (cell == null) {
            return null;
        }
        //读取数据前设置单元格类型
        cell.setCellType(CellType.STRING);
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
     * 判断该机构是否关联省平台（包括互联网医院）供前端调用
     *
     * @param organId
     * @return
     */
    @RpcService
    public List<ImportDrugRecord> findImportDrugRecordByOrganId(Integer organId) {
        return importDrugRecordDAO.findImportDrugRecordByOrganId(organId);
    }


    /**
     * @param detail
     * @param config
     * @param drugsEnterpriseId
     * @return
     */
    @RpcService
    public Map<String, Integer> syncOrganDrugDataToSaleDrugList(OrganDrugList detail, DrugsEnterpriseConfig config, Integer drugsEnterpriseId) {
        Integer addNum = 0;
        Integer updateNum = 0;
        Integer falseNum = 0;
        Map<String, Integer> map = new HashMap<>();
        if (ObjectUtils.isEmpty(config.getEnable_drug_syncType())) {
            throw new DAOException(DAOException.VALUE_NEEDED, "未找到该药企[数据同步类型]配置数据!");
        }
        String[] strings = config.getEnable_drug_syncType().split(",");
        //同步类型
        List<String> syncTypeList = new ArrayList<String>(Arrays.asList(strings));
        List<SaleDrugList> byOrganIdAndDrugCode = Lists.newArrayList();

        //药企药品编码保存字段
        switch (config.getSyncSaleDrugCodeType()) {
            case 1:
                byOrganIdAndDrugCode = saleDrugListDAO.findByOrganIdAndDrugCode(drugsEnterpriseId, detail.getOrganDrugCode());
                break;
            case 2:
                byOrganIdAndDrugCode = saleDrugListDAO.findByOrganIdAndDrugCode(drugsEnterpriseId, detail.getDrugId().toString());
                break;
            case 3:
                if (!ObjectUtils.isEmpty(detail.getMedicalDrugCode())) {
                    byOrganIdAndDrugCode = saleDrugListDAO.findByOrganIdAndDrugCode(drugsEnterpriseId, detail.getMedicalDrugCode());
                }
                break;
            case 4:
                if (!ObjectUtils.isEmpty(detail.getProducerCode())) {
                    byOrganIdAndDrugCode = saleDrugListDAO.findByOrganIdAndDrugCode(drugsEnterpriseId, detail.getProducerCode());
                }
                break;
            default:
                break;
        }
        SaleDrugList byDrugIdAndOrganId = saleDrugListDAO.getByDrugIdAndOrganId(detail.getDrugId(), drugsEnterpriseId);
        try {
            if (!CollectionUtils.isEmpty(byOrganIdAndDrugCode)) {
                if (syncTypeList.indexOf("3") != -1) {
                    if (detail.getStatus().equals(0)) {
                        for (SaleDrugList saleDrugList : byOrganIdAndDrugCode) {
                            boolean isAllow = isAllowDealBySyncDataRange(config.getDelSyncDataRange(), config.getDelSyncDrugType(), detail, drugsEnterpriseId);
                            if (isAllow) {
                                saleDrugList.setStatus(0);
                                saleDrugListDAO.update(saleDrugList);
                                LOGGER.info("syncOrganDrugDataToSaleDrugList 删除" + detail.getDrugName() + " 药企Id=[{}] 药企药品=[{}]  机构药品=[{}]", drugsEnterpriseId, JSONUtils.toString(saleDrugList), JSONUtils.toString(detail));
                            }
                        }
                        //deleteNum++;
                    }
                }
                //TODO liumin
                if (syncTypeList.indexOf("2") != -1) {
                    if (detail.getStatus().equals(1)) {
                        boolean isAllow = isAllowDealBySyncDataRange(config.getUpdateSyncDataRange(), config.getUpdateSyncDrugType(), detail, drugsEnterpriseId);
                        if (isAllow) {
                            List<SaleDrugListSyncField> saleDrugListSyncFieldList = saleDrugListSyncFieldDAO.findByDrugsenterpriseIdAndType(drugsEnterpriseId, "2");

                            SaleDrugList saleDrugList1 = byOrganIdAndDrugCode.get(0);
                            //没有找到配置项，同步所有字段
                            if (CollectionUtils.isEmpty(saleDrugListSyncFieldList)) {
                                saleDrugList1 = obtainSaleDrugListByDetailForUpdate(saleDrugList1, detail, config);

                            } else {
                                Map<String, SaleDrugListSyncField> saleDrugListSyncFieldMap = saleDrugListSyncFieldList.stream().collect(Collectors.toMap(SaleDrugListSyncField::getFieldCode, Function.identity()));
                                for (int i = 0; i < saleDrugListSyncFieldList.size(); i++) {
                                    SaleDrugListSyncField saleDrugListSyncField = saleDrugListSyncFieldList.get(i);
                                    //默认为同步（字段没勾选是否配置或者配置了同步或者表里不存在此跳配置）
                                    if (("price".equals(saleDrugListSyncField.getFieldCode()) && !"0".equals(saleDrugListSyncField.getIsSync()))
                                            || saleDrugListSyncFieldMap.get("price") == null) {
                                        saleDrugList1.setPrice(detail.getSalePrice());
                                    }
                                    if (("drugSpec".equals(saleDrugListSyncField.getFieldCode()) && !"0".equals(saleDrugListSyncField.getIsSync()))
                                            || saleDrugListSyncFieldMap.get("drugSpec") == null) {
                                        saleDrugList1.setDrugSpec(detail.getDrugSpec());
                                    }

                                    if (("drugName".equals(saleDrugListSyncField.getFieldCode()) && !"0".equals(saleDrugListSyncField.getIsSync()))
                                            || saleDrugListSyncFieldMap.get("drugName") == null) {
                                        saleDrugList1.setDrugName(detail.getDrugName());
                                    }

                                    if (("saleName".equals(saleDrugListSyncField.getFieldCode()) && (!"0".equals(saleDrugListSyncField.getIsSync())))
                                            || saleDrugListSyncFieldMap.get("saleName") == null) {
                                        saleDrugList1.setSaleName(detail.getSaleName());
                                    }
                                    if (("status".equals(saleDrugListSyncField.getFieldCode()) && !"0".equals(saleDrugListSyncField.getIsSync()))
                                            || saleDrugListSyncFieldMap.get("status") == null) {
                                        saleDrugList1.setStatus(detail.getStatus());//无效的药品不会变成有效的
                                    }
                                }
                                ;
                                saleDrugList1.setDrugId(detail.getDrugId());
                                saleDrugList1.setLastModify(new Date());
                                switch (config.getSyncSaleDrugCodeType()) {
                                    case 1:
                                        saleDrugList1.setOrganDrugCode(detail.getOrganDrugCode());
                                        saleDrugList1.setSaleDrugCode(detail.getOrganDrugCode());
                                        break;
                                    case 2:
                                        saleDrugList1.setOrganDrugCode(detail.getDrugId().toString());
                                        saleDrugList1.setSaleDrugCode(detail.getDrugId().toString());
                                        break;
                                    case 3:
                                        saleDrugList1.setOrganDrugCode(detail.getMedicalDrugCode());
                                        saleDrugList1.setSaleDrugCode(detail.getMedicalDrugCode());
                                        break;
                                    case 4:
                                        saleDrugList1.setOrganDrugCode(detail.getProducerCode());
                                        saleDrugList1.setSaleDrugCode(detail.getProducerCode());
                                        break;
                                    default:
                                        break;
                                }
                            }
                            SaleDrugList update = saleDrugListDAO.update(saleDrugList1);
                            DataSyncDTO dataSyncDTO = convertDataSyn(update, drugsEnterpriseId, 2, null, 2, detail);
                            List<DataSyncDTO> syncDTOList = Lists.newArrayList();
                            syncDTOList.add(dataSyncDTO);
                            dataSyncLogService.addDataSyncLog("2", syncDTOList);
                            //dataSyncLog(drugsEnterpriseId,update,1,detail);
                            LOGGER.info("syncOrganDrugDataToSaleDrugList 更新 " + update.getDrugName() + " 药企Id=[{}] 药企药品=[{}]  机构药品=[{}]", drugsEnterpriseId, JSONUtils.toString(update), JSONUtils.toString(detail));
                            updateNum++;
                        }
                    }
                }
            } else if (byDrugIdAndOrganId == null) {
                if (syncTypeList.indexOf("1") != -1 && detail.getStatus().equals(1)) {
                    boolean isAllow = isAllowDealBySyncDataRange(config.getAddSyncDataRange(), config.getAddSyncDrugType(), detail, drugsEnterpriseId);
                    if (isAllow) {
                        SaleDrugList saleDrugList = new SaleDrugList();
                        map = obtainSaleDrugListByDetailForAdd(saleDrugList, detail, config, map);
                        SaleDrugList save = saleDrugListDAO.save(saleDrugList);
                        DataSyncDTO dataSyncDTO = convertDataSyn(save, drugsEnterpriseId, 1, null, 1, detail);
                        List<DataSyncDTO> syncDTOList = Lists.newArrayList();
                        syncDTOList.add(dataSyncDTO);
                        dataSyncLogService.addDataSyncLog("2", syncDTOList);
                        //dataSyncLog(drugsEnterpriseId,save,2,detail);
                        LOGGER.info("syncOrganDrugDataToSaleDrugList 新增 " + save.getDrugName() + " 药企Id=[{}] 药企药品=[{}]  机构药品=[{}]", drugsEnterpriseId, JSONUtils.toString(save), JSONUtils.toString(detail));
                        addNum++;
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.info("syncOrganDrugDataToSaleDrugList error", e);
        }
        map.put("addNum", addNum);
        map.put("updateNum", updateNum);
        map.put("falseNum", falseNum);
        return map;
    }

    /**
     * @param saleDrugList1
     * @param detail
     * @param config
     * @return
     */
    private SaleDrugList obtainSaleDrugListByDetailForUpdate(SaleDrugList saleDrugList1, OrganDrugList detail, DrugsEnterpriseConfig config) {
        saleDrugList1.setPrice(detail.getSalePrice());
        saleDrugList1.setDrugId(detail.getDrugId());
        saleDrugList1.setDrugName(detail.getDrugName());
        saleDrugList1.setSaleName(detail.getSaleName());
        saleDrugList1.setDrugSpec(detail.getDrugSpec());
        saleDrugList1.setStatus(detail.getStatus());
        saleDrugList1.setLastModify(new Date());
        switch (config.getSyncSaleDrugCodeType()) {
            case 1:
                saleDrugList1.setOrganDrugCode(detail.getOrganDrugCode());
                saleDrugList1.setSaleDrugCode(detail.getOrganDrugCode());
                break;
            case 2:
                saleDrugList1.setOrganDrugCode(detail.getDrugId().toString());
                saleDrugList1.setSaleDrugCode(detail.getDrugId().toString());
                break;
            case 3:
                saleDrugList1.setOrganDrugCode(detail.getMedicalDrugCode());
                saleDrugList1.setSaleDrugCode(detail.getMedicalDrugCode());
                break;
            case 4:
                saleDrugList1.setOrganDrugCode(detail.getProducerCode());
                saleDrugList1.setSaleDrugCode(detail.getProducerCode());
                break;
            default:
                break;
        }
        return saleDrugList1;
    }

    /**
     * @param saleDrugList1
     * @param detail
     * @param config
     * @param map
     * @return
     */
    private Map<String, Integer> obtainSaleDrugListByDetailForAdd(SaleDrugList saleDrugList, OrganDrugList detail, DrugsEnterpriseConfig config, Map<String, Integer> map) {

        saleDrugList.setDrugId(detail.getDrugId());
        saleDrugList.setDrugName(detail.getDrugName());
        saleDrugList.setSaleName(detail.getSaleName());
        saleDrugList.setDrugSpec(detail.getDrugSpec());
        saleDrugList.setOrganId(config.getDrugsenterpriseId());
        saleDrugList.setStatus(1);
        saleDrugList.setPrice(detail.getSalePrice());
        switch (config.getSyncSaleDrugCodeType()) {
            case 1:
                saleDrugList.setOrganDrugCode(detail.getOrganDrugCode());
                break;
            case 2:
                saleDrugList.setOrganDrugCode(detail.getDrugId().toString());
                break;
            case 3:
                if (!ObjectUtils.isEmpty(detail.getMedicalDrugCode())) {
                    saleDrugList.setOrganDrugCode(detail.getMedicalDrugCode());
                } else {
                    map.put("addNum", 0);
                    map.put("updateNum", 0);
                    map.put("falseNum", 1);
                    return map;
                }
                break;
            case 4:
                if (!ObjectUtils.isEmpty(detail.getProducerCode())) {
                    saleDrugList.setOrganDrugCode(detail.getProducerCode());
                } else {
                    map.put("addNum", 0);
                    map.put("updateNum", 0);
                    map.put("falseNum", 1);
                    return map;
                }
                break;
            default:
                break;
        }
        saleDrugList.setSaleDrugCode(saleDrugList.getOrganDrugCode());
        saleDrugList.setInventory(new BigDecimal(100));
        saleDrugList.setCreateDt(new Date());
        saleDrugList.setLastModify(new Date());
        return map;
    }

    /**
     * @param obj，要获取字段的对象
     * @param propertyName，要获取字段名
     * @return 返回字段的值
     */
    public static Object get(Object obj, String name) {
        Object value = null;
        // 利用反射获取属性值
        Class c = obj.getClass();
        try {
            Field field = c.getDeclaredField(name);
            field.setAccessible(true);
            value = field.get(obj);
            field.setAccessible(false);
        } catch (NoSuchFieldException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (SecurityException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return value;

    }

    /**
     * 根据同步数据范围判断能否同步数据
     *
     * @param syncDataRange
     * @param syncDrugType
     * @param detail
     * @param drugsEnterpriseId
     * @return
     */
    @LogRecord
    private boolean isAllowDealBySyncDataRange(Integer syncDataRange, String syncDrugType, OrganDrugList detail, Integer drugsEnterpriseId) {
        boolean isAllow = false;
        if (syncDataRange == 1) {
            //同步数据范围 配送药企
            if (!ObjectUtils.isEmpty(detail.getDrugsEnterpriseIds())) {
                String[] drugsEnterpriseIdsStr = detail.getDrugsEnterpriseIds().split(",");
                List<String> drugsEnterpriseIdsList = new ArrayList<String>(Arrays.asList(drugsEnterpriseIdsStr));
                if (drugsEnterpriseIdsList.indexOf(drugsEnterpriseId.toString()) != -1) {
                    isAllow = true;
                }
            }
        } else if (syncDataRange == 2) {
            //同步数据范围 药品类型
            if (ObjectUtils.isEmpty(syncDrugType)) {
                throw new DAOException(DAOException.VALUE_NEEDED, "未找到该药企[同步药品类型]配置数据!");
            }
            String[] syncDrugTypeStr = syncDrugType.split(",");
            List<String> syncDrugTypeList = new ArrayList<String>(Arrays.asList(syncDrugTypeStr));
            //1西药 2中成药 3中药
            if (syncDrugTypeList.indexOf("1") != -1 && drugListDAO.get(detail.getDrugId()).getDrugType() == 1
                    || syncDrugTypeList.indexOf("2") != -1 && drugListDAO.get(detail.getDrugId()).getDrugType() == 2
                    || syncDrugTypeList.indexOf("3") != -1 && drugListDAO.get(detail.getDrugId()).getDrugType() == 3) {
                isAllow = true;
            }
        }
        return isAllow;
    }

    public DataSyncDTO convertDataSyn(SaleDrugList drug, Integer organId, Integer status, Exception e, Integer operType, OrganDrugList detail) {

        DataSyncDTO dataSyncDTO = new DataSyncDTO();
        dataSyncDTO.setType(2);
        dataSyncDTO.setOrganId(organId);
        if (!ObjectUtils.isEmpty(drug)) {
            Map<String, Object> param = new HashedMap();
            BeanUtils.map(drug, param);
            DrugListDAO dao = getDAO(DrugListDAO.class);
            if (!ObjectUtils.isEmpty(drug.getDrugId())) {
                DrugList drugList = dao.get(drug.getDrugId());
                if (!ObjectUtils.isEmpty(drugList)) {
                    param.put("drugType", drugList.getDrugType());
                    param.put("drugClass", drugList.getDrugClass());
                    if (!ObjectUtils.isEmpty(detail)) {
                        param.put("organId", detail.getOrganId());
                        param.put("SaleOrganId", drug.getOrganId());
                        param.put("organDrugId", detail.getOrganDrugId());
                    }
                    param.put("drugForm", drugList.getDrugForm());
                    param.put("producer", drugList.getProducer());
                }
            }
            dataSyncDTO.setReqMsg(JSONUtils.toString(param));
        }
        dataSyncDTO.setStatus(status);
        if (e != null) {
            dataSyncDTO.setRespMsg(e.getMessage());
        } else {
            dataSyncDTO.setRespMsg("成功");
        }
        dataSyncDTO.setOperType(operType);
        dataSyncDTO.setSyncTime(new Date());

        return dataSyncDTO;
    }


    public long timeDifference(String date) throws ParseException {
        SimpleDateFormat myFmt2 = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        Date date1 = myFmt2.parse(date);
        String date2 = myFmt2.format(new Date());
        Date date3 = myFmt2.parse(date2);
        long diff = date3.getTime() - date1.getTime();
        long minutes = diff / (1000 * 60);
        return minutes;
    }


    /**
     * 从缓存中实时获取同步情况
     *
     * @param drugsEnterpriseId
     * @return
     * @throws ParseException
     */
    @RpcService
    public Map<String, Object> getOrganDrugSyncData(Integer drugsEnterpriseId) throws ParseException {
        return (Map<String, Object>) redisClient.get(KEY_THE_DRUG_SYNC + drugsEnterpriseId.toString());
    }

    /**
     * 从缓存中实时获取同步情况 ()
     *
     * @param drugsEnterpriseId
     * @return
     * @throws ParseException
     */
    @RpcService
    public Map<String, Object> getOrganDrugSyncDataRedis(Integer drugsEnterpriseId) throws ParseException {
        Map<String, Object> maps = Maps.newHashMap();
        Map<String, Object> map = (Map<String, Object>) redisClient.get(KEY_THE_DRUG_SYNC + drugsEnterpriseId.toString());
        maps.put("Date", map.get("Date"));
        return maps;
    }


    /**
     * 从缓存中删除异常同步情况
     *
     * @param drugsEnterpriseId
     * @return
     * @throws ParseException
     */
    @RpcService
    public void deleteOrganDrugSyncData(Integer drugsEnterpriseId) {
        redisClient.del(KEY_THE_DRUG_SYNC + drugsEnterpriseId.toString());
    }


    /**
     * 从缓存中删除异常同步情况
     *
     * @param drugsEnterpriseId
     * @return
     * @throws ParseException
     */
    @RpcService
    public Long getTimeByOrganId(Integer drugsEnterpriseId) throws ParseException {
        long minutes = 0L;
        Map<String, Object> hget = (Map<String, Object>) redisClient.get(KEY_THE_DRUG_SYNC + drugsEnterpriseId.toString());
        if (hget != null) {
            Integer status = (Integer) hget.get("Status");
            String date = (String) hget.get("Date");
            minutes = timeDifference(date);
        }
        return minutes;
    }

    /**
     * 药企同步机构药品
     * organDrugSync saleDrugInfoSynMovement saleDrugInfoSynMovementT 整这么多相同的干啥
     *
     * @param drugsEnterpriseId
     * @return
     * @throws ParseException
     */
    @RpcService(timeout = 6000)
    @LogRecord
    public Map<String, Object> saleDrugInfoSynMovement(Integer drugsEnterpriseId) throws ParseException {

        if (ObjectUtils.isEmpty(drugsEnterpriseId)) {
            throw new DAOException(DAOException.VALUE_NEEDED, "drugsenterpriseId is required!");
        }
        DrugsEnterprise drugsEnterprise = drugsEnterpriseDAO.get(drugsEnterpriseId);
        if (ObjectUtils.isEmpty(drugsEnterprise)) {
            throw new DAOException(DAOException.VALUE_NEEDED, "未找到该药企" + drugsEnterpriseId);
        }
        UserRoleToken urt = UserRoleToken.getCurrent();
        IBusActionLogService busActionLogService = AppDomainContext.getBean("opbase.busActionLogService", IBusActionLogService.class);
        busActionLogService.recordBusinessLogRpcNew("药企药品管理", "", "SaleDrugList", "【" + urt.getUserName() + "】调用 药企药品目录-》手动同步【" + drugsEnterprise.getName()
                + "】", drugsEnterprise.getName());
        Map<String, Object> hget = (Map<String, Object>) redisClient.get(KEY_THE_DRUG_SYNC + drugsEnterpriseId.toString());
        if (hget != null) {
            Integer status = (Integer) hget.get("Status");
            String date = (String) hget.get("Date");
            long minutes = timeDifference(date);
            if (minutes < 10L) {
                throw new DAOException(DAOException.VALUE_NEEDED, "距离上次手动同步未超过10分钟，请稍后再尝试数据同步!");
            }
        }
        DrugsEnterpriseConfigService bean = AppContextHolder.getBean("eh.drugsEnterpriseConfigService", DrugsEnterpriseConfigService.class);
        DrugsEnterpriseConfig config = bean.getConfigByDrugsenterpriseId(drugsEnterpriseId);
        if (ObjectUtils.isEmpty(config)) {
            throw new DAOException(DAOException.VALUE_NEEDED, "未找到该药企配置数据!");
        }
        if (config.getEnable_drug_sync() == 0) {
            throw new DAOException(DAOException.VALUE_NEEDED, "请先确认 基础数据-药品目录-药企药品目录-同步设置-【药企药品是否支持同步】已开启，再尝试进行同步!");
        }
        String organIds = config.getOrganId();
        if (ObjectUtils.isEmpty(organIds)) {
            throw new DAOException(DAOException.VALUE_NEEDED, "该药企[" + drugsEnterprise.getName() + "]未找到关联机构!");
        }
        SimpleDateFormat myFmt2 = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        Map<String, Object> map = Maps.newHashMap();
        map.put("Date", myFmt2.format(new Date()));
        map.put("Status", 0);
        redisClient.del(KEY_THE_DRUG_SYNC + drugsEnterpriseId.toString());
        redisClient.set(KEY_THE_DRUG_SYNC + drugsEnterpriseId.toString(), map);

        RecipeBusiThreadPool.execute(new Runnable() {
            @Override
            public void run() {
                long start = System.currentTimeMillis();
                Integer updateNum = 0;
                Integer addNum = 0;
                Integer falseNum = 0;
                Integer total = 0;
                Integer deleteNum = 0;
                String[] organIdArr = organIds.split(",");
                for (int i = 0; i < organIdArr.length; i++) {
                    if (StringUtils.isEmpty(organIdArr[i])) {
                        continue;
                    }
                    Integer organId = Integer.parseInt(organIdArr[i]);
                    List<OrganDrugList> details = organDrugListDAO.findOrganDrugByOrganId(organId);
                    if (config.getSyncDataSource() == 1) {
                        //数据来源 关联管理机构
                        //获取药企关联机构药品目录
                        if (!ObjectUtils.isEmpty(details)) {
                            total = details.size();
                            for (OrganDrugList detail : details) {
                                Map<String, Integer> stringIntegerMap = syncOrganDrugDataToSaleDrugList(detail, config, drugsEnterpriseId);
                                LOGGER.info("syncSaleOrganDrug药企药品数据同步 配送 " + detail.getDrugName() + " 药企Id=[{}] drug=[{}]", drugsEnterpriseId, JSONUtils.toString(detail));
                                addNum = addNum + stringIntegerMap.get("addNum");
                                updateNum = updateNum + stringIntegerMap.get("updateNum");
                                falseNum = falseNum + stringIntegerMap.get("falseNum");
                            }
                        }
                    }

                    String[] strings = config.getEnable_drug_syncType().split(",");
                    List<String> syncTypeList = new ArrayList<String>(Arrays.asList(strings));
                    Map<Integer, OrganDrugList> drugMap = Maps.newHashMap();
                    //删除药品同步 （机构没有，药企有）
                    if (syncTypeList.indexOf("3") != -1) {
                        if (config.getDelSyncDataRange() == 1) {
                            String drugsEnterprise = "%" + drugsEnterpriseId.toString() + "%";
                            List<OrganDrugList> drugLists = organDrugListDAO.findOrganDrugByOrganIdAndDrugsEnterpriseId(organId, drugsEnterprise);
                            drugMap = drugLists.stream().collect(Collectors.toMap(OrganDrugList::getDrugId, a -> a, (k1, k2) -> k1));
                        } else {
                            drugMap = details.stream().collect(Collectors.toMap(OrganDrugList::getDrugId, a -> a, (k1, k2) -> k1));
                        }
                        List<SaleDrugList> saleDrugListsByOrganId = saleDrugListDAO.findSaleDrugListsByOrganId(drugsEnterpriseId);
                        String delSyncDrugType = config.getDelSyncDrugType();
                        if (!ObjectUtils.isEmpty(saleDrugListsByOrganId) && !ObjectUtils.isEmpty(delSyncDrugType)) {
                            for (SaleDrugList saleDrugList : saleDrugListsByOrganId) {
                                try {
                                    OrganDrugList organDrug = drugMap.get(saleDrugList.getDrugId());
                                    if (ObjectUtils.isEmpty(organDrug)) {
                                        if (config.getDelSyncDataRange() == 1) {
                                            saleDrugListDAO.remove(saleDrugList.getOrganDrugId());
                                        } else {
                                            //（还需判断药品类型）
                                            String[] syncDrugTypeStr = delSyncDrugType.split(",");
                                            List<String> syncDrugTypeList = new ArrayList<String>(Arrays.asList(syncDrugTypeStr));
                                            //1西药 2中成药 3中药
                                            if (syncDrugTypeList.indexOf("1") != -1 && drugListDAO.get(saleDrugList.getDrugId()).getDrugType() == 1
                                                    || syncDrugTypeList.indexOf("2") != -1 && drugListDAO.get(saleDrugList.getDrugId()).getDrugType() == 2
                                                    || syncDrugTypeList.indexOf("3") != -1 && drugListDAO.get(saleDrugList.getDrugId()).getDrugType() == 3) {
                                                saleDrugListDAO.remove(saleDrugList.getOrganDrugId());
                                            }
                                        }

                                        deleteNum++;
                                    }
                                } catch (Exception e) {
                                    LOGGER.info("error", e);
                                }

                            }
                        }
                    }
                }


                map.put("addNum", addNum);
                map.put("updateNum", updateNum);
                map.put("falseNum", falseNum);
                map.put("deleteNum", deleteNum);
                map.put("total", total);
                map.put("Date", myFmt2.format(new Date()));
                map.put("Status", 1);
                redisClient.del(KEY_THE_DRUG_SYNC + drugsEnterpriseId.toString());
                redisClient.set(KEY_THE_DRUG_SYNC + drugsEnterpriseId.toString(), map);
                long elapsedTime = System.currentTimeMillis() - start;
                LOGGER.info("RecipeBusiThreadPool saleDrugInfoSynMovement ES-推送药品 执行时间:{}.", elapsedTime);
            }
        });

        return map;
    }

    /**
     * 药企药品手动同步
     * 前端说没有在调用 是不是以前的老方法 为啥要搞成这样。。。定时任务？
     *
     * @param drugsEnterpriseId
     * @return
     */
    @RpcService(timeout = 6000)
    @LogRecord
    public Map<String, Object> saleDrugInfoSynMovementT(Integer drugsEnterpriseId) throws ParseException {
        if (ObjectUtils.isEmpty(drugsEnterpriseId)) {
            throw new DAOException(DAOException.VALUE_NEEDED, "drugsenterpriseId is required!");
        }
        DrugsEnterprise drugsEnterprise = drugsEnterpriseDAO.get(drugsEnterpriseId);
        if (ObjectUtils.isEmpty(drugsEnterprise)) {
            throw new DAOException(DAOException.VALUE_NEEDED, "未找到该药企" + drugsEnterpriseId);
        }
        Map<String, Object> hget = (Map<String, Object>) redisClient.get(KEY_THE_DRUG_SYNC + drugsEnterpriseId.toString());
        if (hget != null) {
            Integer status = (Integer) hget.get("Status");
            String date = (String) hget.get("Date");
            long minutes = timeDifference(date);
            if (minutes < 10L) {
                throw new DAOException(DAOException.VALUE_NEEDED, "距离上次手动同步未超过10分钟，请稍后再尝试数据同步!");
            }
        }
        DrugsEnterpriseConfigService bean = AppContextHolder.getBean("eh.drugsEnterpriseConfigService", DrugsEnterpriseConfigService.class);
        DrugsEnterpriseConfig config = bean.getConfigByDrugsenterpriseId(drugsEnterpriseId);
        if (ObjectUtils.isEmpty(config)) {
            throw new DAOException(DAOException.VALUE_NEEDED, "未找到该药企配置数据!");
        }
        if (config.getEnable_drug_sync() == 0) {
            throw new DAOException(DAOException.VALUE_NEEDED, "请先确认 基础数据-药品目录-药企药品目录-同步设置-【药企药品是否支持同步】已开启，再尝试进行同步!");
        }
        String organIds = config.getOrganId();
        if (ObjectUtils.isEmpty(organIds)) {
            throw new DAOException(DAOException.VALUE_NEEDED, "该药企[" + drugsEnterprise.getName() + "]未找到关联机构!");
        }
        OrganService organService = AppDomainContext.getBean("basic.organService", OrganService.class);
//        OrganDTO byOrganId = organService.getByOrganId(organId);
        //查询起始下标
        Integer updateNum = 0;
        Integer addNum = 0;
        Integer falseNum = 0;
        Integer deleteNum = 0;
        List<OrganDrugInfoTO> addList = Lists.newArrayList();
        List<OrganDrugInfoTO> updateList = Lists.newArrayList();
        Integer total = 0;
        Map<String, Object> map = Maps.newHashMap();
        SimpleDateFormat myFmt2 = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        String[] organIdArr = organIds.split(",");
        for (int i = 0; i < organIdArr.length; i++) {
            if (StringUtils.isEmpty(organIdArr[i])) {
                continue;
            }
            Integer organId = Integer.parseInt(organIdArr[i]);
            List<OrganDrugList> details = organDrugListDAO.findOrganDrugByOrganId(organId);
            if (config.getSyncDataSource() == 1) {
                //数据来源 关联管理机构
                //获取药企关联机构药品目录
                if (!ObjectUtils.isEmpty(details)) {
                    total = details.size();
                    try {
                        for (OrganDrugList detail : details) {
                            Map<String, Integer> stringIntegerMap = syncOrganDrugDataToSaleDrugList(detail, config, drugsEnterpriseId);
                        }
                    } catch (DAOException e) {
                        LOGGER.info("syncSaleOrganDrug error", e);
                    }
                }
            }
            String[] strings = config.getEnable_drug_syncType().split(",");
            List<String> syncTypeList = new ArrayList<String>(Arrays.asList(strings));
            Map<Integer, OrganDrugList> drugMap = Maps.newHashMap();

            if (syncTypeList.indexOf("3") != -1) {
                if (config.getDelSyncDataRange() == 1) {
                    String drugsEnt = "%" + drugsEnterpriseId.toString() + "%";
                    List<OrganDrugList> drugLists = organDrugListDAO.findOrganDrugByOrganIdAndDrugsEnterpriseId(organId, drugsEnt);
                    drugMap = drugLists.stream().collect(Collectors.toMap(OrganDrugList::getDrugId, a -> a, (k1, k2) -> k1));
                } else {
                    drugMap = details.stream().collect(Collectors.toMap(OrganDrugList::getDrugId, a -> a, (k1, k2) -> k1));
                }
                List<SaleDrugList> saleDrugListsByOrganId = saleDrugListDAO.findSaleDrugListsByOrganId(drugsEnterpriseId);
                String delSyncDrugType = config.getDelSyncDrugType();
                if (!ObjectUtils.isEmpty(saleDrugListsByOrganId) && !ObjectUtils.isEmpty(delSyncDrugType)) {
                    for (SaleDrugList saleDrugList : saleDrugListsByOrganId) {
                        try {
                            OrganDrugList organDrug = drugMap.get(saleDrugList.getDrugId());
                            if (ObjectUtils.isEmpty(organDrug)) {
                                if (config.getDelSyncDataRange() == 1) {
                                    saleDrugListDAO.remove(saleDrugList.getOrganDrugId());
                                } else {
                                    //（还需判断药品类型）
                                    String[] syncDrugTypeStr = delSyncDrugType.split(",");
                                    List<String> syncDrugTypeList = new ArrayList<String>(Arrays.asList(syncDrugTypeStr));
                                    //1西药 2中成药 3中药
                                    if (syncDrugTypeList.indexOf("1") != -1 && drugListDAO.get(saleDrugList.getDrugId()).getDrugType() == 1
                                            || syncDrugTypeList.indexOf("2") != -1 && drugListDAO.get(saleDrugList.getDrugId()).getDrugType() == 2
                                            || syncDrugTypeList.indexOf("3") != -1 && drugListDAO.get(saleDrugList.getDrugId()).getDrugType() == 3) {
                                        saleDrugListDAO.remove(saleDrugList.getOrganDrugId());
                                    }
                                }
                                deleteNum++;
                            }
                        } catch (Exception e) {
                            LOGGER.info("error", e);
                        }

                    }
                }
            }
        }

        map.put("addNum", addNum);
        map.put("updateNum", updateNum);
        map.put("deleteNum", deleteNum);
        map.put("falseNum", 0);
        map.put("total", total);
//        map.put("organName", byOrganId.getName());
        map.put("Date", myFmt2.format(new Date()));
        map.put("Status", 1);
        redisClient.del(KEY_THE_DRUG_SYNC + drugsEnterpriseId.toString());
        redisClient.set(KEY_THE_DRUG_SYNC + drugsEnterpriseId.toString(), map);
        return map;
    }


}

