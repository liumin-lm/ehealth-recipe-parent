package recipe.drugTool.service;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.ngari.his.recipe.mode.OrganDrugInfoRequestTO;
import com.ngari.his.recipe.mode.OrganDrugInfoResponseTO;
import com.ngari.his.recipe.mode.OrganDrugInfoTO;
import com.ngari.his.recipe.service.IRecipeHisService;
import com.ngari.opbase.base.service.IBusActionLogService;
import com.ngari.opbase.log.mode.DataSyncDTO;
import com.ngari.opbase.log.service.IDataSyncLogService;
import com.ngari.opbase.xls.mode.ImportExcelInfoDTO;
import com.ngari.opbase.xls.service.IImportExcelInfoService;
import com.ngari.patient.dto.OrganConfigDTO;
import com.ngari.patient.dto.OrganDTO;
import com.ngari.patient.service.OrganService;
import com.ngari.recipe.drugTool.service.ISaleDrugToolService;
import com.ngari.recipe.entity.*;
import ctd.account.UserRoleToken;
import ctd.persistence.exception.DAOException;
import ctd.spring.AppDomainContext;
import ctd.util.AppContextHolder;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import ctd.util.exp.standard.IN;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.output.StringBuilderWriter;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ObjectUtils;
import recipe.ApplicationUtils;
import recipe.dao.*;
import recipe.service.DrugsEnterpriseConfigService;
import recipe.service.OrganDrugListService;
import recipe.service.RecipeHisService;
import recipe.thread.RecipeBusiThreadPool;
import recipe.util.RedisClient;

import javax.annotation.Resource;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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
            LOGGER.error("readDrugExcel error ," + e.getMessage(),e);
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
        List<SaleDrugList> drugLists=Lists.newArrayList();

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
                    if (ObjectUtils.isEmpty(drugList)){
                        errMsg.append("平台未找到该平台通用药品").append(";");
                    }
                    if (StringUtils.isNotEmpty(getStrFromCell(row.getCell(0)))) {
                        SaleDrugList byDrugIdAndOrganId = saleDrugListDAO.getByDrugIdAndOrganId(Integer.parseInt(getStrFromCell(row.getCell(1)).trim()), organId);
                        if (!ObjectUtils.isEmpty(byDrugIdAndOrganId)){
                            errMsg.append("药企已存在药品关联该平台药品").append(";");
                        }
                        drug.setDrugId(Integer.parseInt(getStrFromCell(row.getCell(0)).trim()));
                    }
                } catch (Exception e) {
                    LOGGER.error("平台药品编码有误 ," + e.getMessage(), e);
                    errMsg.append("平台药品编码有误").append(";");
                }

                try {
                    if (StringUtils.isEmpty(getStrFromCell(row.getCell(1)))) {
                        errMsg.append("【机构药品编码】未填写").append(";");
                    }
                    drug.setOrganDrugCode(getStrFromCell(row.getCell(1)));
                } catch (Exception e) {
                    LOGGER.error("机构药品编码有误 ," + e.getMessage(), e);
                    errMsg.append("机构药品编码有误").append(";");
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
                    if (!ObjectUtils.isEmpty(drugList)){
                        drug.setDrugSpec(drugList.getDrugSpec());
                    }
                }

            if (!ObjectUtils.isEmpty(drug.getRate())){
                drug.setRatePrice(drug.getPrice().doubleValue()*(1-drug.getRate()));
            }

            drug.setStatus(1);
            drug.setOrganId(organId);
            drug.setInventory(new BigDecimal(100));
            drug.setCreateDt(new Date());
            drug.setLastModify(new Date());

            if (errMsg.length() > 1) {
                int showNum = rowIndex + 1;
                String error = ("【第" + showNum + "行】" + errMsg.substring(0, errMsg.length() - 1)+"\n");
                errMsgAll.append(error);
                errDrugListMatchList.add(error);
            } else {
                drugLists.add(drug);
            }
        }
        if (errDrugListMatchList.size()>0){

            IImportExcelInfoService iImportExcelInfoService = AppContextHolder.getBean("opbase.importExcelInfoService", IImportExcelInfoService.class);

            ImportExcelInfoDTO importExcelInfoDTO=new ImportExcelInfoDTO();
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
            result.put("addNum",addNum);
            result.put("updateNum",updateNum);
            result.put("failNum",total-addNum-updateNum);
            result.put("ImportExcelInfoId",importExcelInfoDTO.getId());
            LOGGER.info(operator + "结束 readDrugExcel 方法" + System.currentTimeMillis() + "当前进程=" + Thread.currentThread().getName());
            return result;

        }else {
            for (SaleDrugList drugList : drugLists) {
                try {
                    //自动匹配功能暂无法提供
                    saleDrugListDAO.save(drugList);
                    addNum++;

                } catch (Exception e) {
                    LOGGER.error("save or update drugListMatch error " + e.getMessage(),e);
                }
            }
        }

        //导入药品记录
        IImportExcelInfoService iImportExcelInfoService = AppContextHolder.getBean("opbase.importExcelInfoService", IImportExcelInfoService.class);

        ImportExcelInfoDTO importExcelInfoDTO=new ImportExcelInfoDTO();
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
        result.put("ImportExcelInfoId",importExcelInfoDTO.getId());
        result.put("addNum",addNum);
        result.put("updateNum",updateNum);
        result.put("failNum",total-addNum-updateNum);
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
     * @param organId
     * @return
     */
    @RpcService
    public List<ImportDrugRecord> findImportDrugRecordByOrganId(Integer organId){
        return importDrugRecordDAO.findImportDrugRecordByOrganId(organId);
    }




    @RpcService
    public Map<String,Integer>   syncOrganDrugDataToSaleDrugList(OrganDrugList detail,DrugsEnterpriseConfig config, Integer drugsEnterpriseId) {
        Integer addNum=0;
        Integer updateNum=0;
        Integer falseNum = 0;
        Map<String,Integer> map=new HashMap<>();
        if (ObjectUtils.isEmpty(config.getEnable_drug_syncType())){
            throw new DAOException(DAOException.VALUE_NEEDED, "未找到该药企[数据同步类型]配置数据!");
        }
        String[] strings = config.getEnable_drug_syncType().split(",");
        List<String> syncTypeList = new ArrayList<String>(Arrays.asList(strings));
        List<SaleDrugList> byOrganIdAndDrugCode =Lists.newArrayList();

        switch (config.getSyncSaleDrugCodeType()) {
            case 1:
                byOrganIdAndDrugCode=saleDrugListDAO.findByOrganIdAndDrugCode(drugsEnterpriseId, detail.getOrganDrugCode());
                break;
            case 2:
                byOrganIdAndDrugCode=saleDrugListDAO.findByOrganIdAndDrugCode(drugsEnterpriseId, detail.getDrugId().toString());
                break;
            case 3:
                if (!ObjectUtils.isEmpty(detail.getMedicalDrugCode())){
                    byOrganIdAndDrugCode=saleDrugListDAO.findByOrganIdAndDrugCode(drugsEnterpriseId, detail.getMedicalDrugCode());
                }
                break;
            case 4:
                if (!ObjectUtils.isEmpty(detail.getProducerCode())){
                    byOrganIdAndDrugCode=saleDrugListDAO.findByOrganIdAndDrugCode(drugsEnterpriseId, detail.getProducerCode());
                }
                break;
            default:
                break;
        }
        SaleDrugList byDrugIdAndOrganId = saleDrugListDAO.getByDrugIdAndOrganId(detail.getDrugId(), drugsEnterpriseId);
        if (byOrganIdAndDrugCode != null && byOrganIdAndDrugCode.size()>0) {
            if (syncTypeList.indexOf("3")!=-1){
                if (detail.getStatus().equals(0)){
                    for (SaleDrugList saleDrugList : byOrganIdAndDrugCode) {
                        saleDrugListDAO.remove(saleDrugList);
                        LOGGER.info("syncOrganDrugDataToSaleDrugList 删除" + detail.getDrugName() + " 药企Id=[{}] 药企药品=[{}]  机构药品=[{}]", drugsEnterpriseId, JSONUtils.toString(saleDrugList),JSONUtils.toString(detail));
                    }
                    //deleteNum++;
                }
            }
            if (syncTypeList.indexOf("2")!=-1){
                if (detail.getStatus().equals(1)){
                    SaleDrugList saleDrugList1 = byOrganIdAndDrugCode.get(0);
                    saleDrugList1.setPrice(detail.getSalePrice());
                    saleDrugList1.setDrugId(detail.getDrugId());
                    saleDrugList1.setDrugName(detail.getDrugName());
                    saleDrugList1.setSaleName(detail.getSaleName());
                    saleDrugList1.setDrugSpec(detail.getDrugSpec());
                    saleDrugList1.setStatus(detail.getStatus());
                    saleDrugList1.setLastModify(new Date());
                    saleDrugList1.setOrganDrugCode(String.valueOf(detail.getOrganDrugCode()));
                    switch (config.getSyncSaleDrugCodeType()) {
                        case 1:
                            saleDrugList1.setSaleDrugCode(detail.getOrganDrugCode());
                            break;
                        case 2:
                            saleDrugList1.setSaleDrugCode(detail.getDrugId().toString());
                            break;
                        case 3:
                            saleDrugList1.setSaleDrugCode(detail.getMedicalDrugCode());
                            break;
                        case 4:
                            saleDrugList1.setSaleDrugCode(detail.getProducerCode());
                            break;
                        default:
                            break;
                    }
                    SaleDrugList update = saleDrugListDAO.update(saleDrugList1);
                    //dataSyncLog(drugsEnterpriseId,update,1,detail);
                    LOGGER.info("syncOrganDrugDataToSaleDrugList 更新 " + update.getDrugName() + " 药企Id=[{}] 药企药品=[{}]  机构药品=[{}]", drugsEnterpriseId, JSONUtils.toString(update),JSONUtils.toString(detail));
                    updateNum++;
                }
            }
        }else if (byDrugIdAndOrganId == null) {
            if (syncTypeList.indexOf("1")!=-1){
                if (detail.getStatus().equals(1)) {
                    SaleDrugList saleDrugList=new SaleDrugList();
                    saleDrugList.setDrugId(detail.getDrugId());
                    saleDrugList.setDrugName(detail.getDrugName());
                    saleDrugList.setSaleName(detail.getSaleName());
                    saleDrugList.setDrugSpec(detail.getDrugSpec());
                    saleDrugList.setOrganId(drugsEnterpriseId);
                    saleDrugList.setStatus(1);
                    saleDrugList.setPrice(detail.getSalePrice());
                    switch (config.getSyncSaleDrugCodeType()) {
                        case 1:
                            saleDrugList.setSaleDrugCode(detail.getOrganDrugCode());
                            break;
                        case 2:
                            saleDrugList.setSaleDrugCode(detail.getDrugId().toString());
                            break;
                        case 3:
                            if (!ObjectUtils.isEmpty(detail.getMedicalDrugCode())){
                                saleDrugList.setSaleDrugCode(detail.getMedicalDrugCode());
                            }else {
                                map.put("addNum",0);
                                map.put("updateNum",0);
                                map.put("falseNum",1);
                                return map;
                            }
                            break;
                        case 4:
                            if (!ObjectUtils.isEmpty(detail.getProducerCode())){
                                saleDrugList.setSaleDrugCode(detail.getProducerCode());
                            }else {
                                map.put("addNum",0);
                                map.put("updateNum",0);
                                map.put("falseNum",1);
                                return map;
                            }
                            break;
                        default:
                            break;
                    }
                    saleDrugList.setOrganDrugCode(String.valueOf(detail.getOrganDrugCode()));
                    saleDrugList.setInventory(new BigDecimal(100));
                    saleDrugList.setCreateDt(new Date());
                    saleDrugList.setLastModify(new Date());
                    SaleDrugList save = saleDrugListDAO.save(saleDrugList);
                    //dataSyncLog(drugsEnterpriseId,save,2,detail);
                    LOGGER.info("syncOrganDrugDataToSaleDrugList 新增 " + save.getDrugName() + " 药企Id=[{}] 药企药品=[{}]  机构药品=[{}]", drugsEnterpriseId, JSONUtils.toString(save),JSONUtils.toString(detail));
                    addNum++;
                }
            }
        }
        map.put("addNum",addNum);
        map.put("updateNum",updateNum);
        map.put("falseNum",falseNum);
        return map;
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

  public void dataSyncLog(Integer drugsEnterpriseId,SaleDrugList update,Integer status,OrganDrugList detail) {
      IDataSyncLogService dataSyncLogService = AppContextHolder.getBean("opbase.dataSyncLogService", IDataSyncLogService.class);
      DataSyncDTO dataSyncDTO=new DataSyncDTO();
      ArrayList<DataSyncDTO> list = Lists.newArrayList();
      dataSyncDTO.setOrganId(drugsEnterpriseId.toString());
      if (status==1){
          dataSyncDTO.setReqMsg(JSONUtils.toString(update));
          dataSyncDTO.setRespMsg("更新成功");
      }
      if (status==2){
          dataSyncDTO.setReqMsg(JSONUtils.toString(update));
          dataSyncDTO.setRespMsg("新增成功");
      }
      dataSyncDTO.setType("7");
      dataSyncDTO.setStatus("1");
      dataSyncDTO.setStatus("1");
      dataSyncDTO.setSyncTime(new Date());
      list.add(dataSyncDTO);
      dataSyncLogService.addDataSyncLog("7",list);
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
        Map<String, Object> maps=Maps.newHashMap();
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
     *
     * @param drugsEnterpriseId
     * @return
     * @throws ParseException
     */
    @RpcService(timeout = 6000)
    public Map<String, Object> saleDrugInfoSynMovement(Integer drugsEnterpriseId) throws ParseException {
        if (ObjectUtils.isEmpty(drugsEnterpriseId)){
            throw new DAOException(DAOException.VALUE_NEEDED, "drugsenterpriseId is required!");
        }
        DrugsEnterprise drugsEnterprise = drugsEnterpriseDAO.get(drugsEnterpriseId);
        if (ObjectUtils.isEmpty(drugsEnterprise)){
            throw new DAOException(DAOException.VALUE_NEEDED, "未找到该药企"+drugsEnterpriseId);
        }
        UserRoleToken urt = UserRoleToken.getCurrent();
        IBusActionLogService busActionLogService = AppDomainContext.getBean("opbase.busActionLogService", IBusActionLogService.class);
        busActionLogService.recordBusinessLogRpcNew("药企药品管理", "", "SaleDrugList", "【" + urt.getUserName() + "】调用 药企药品目录-》手动同步【" + drugsEnterprise.getName()
                +"】",drugsEnterprise.getName());
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
        if (ObjectUtils.isEmpty(config)){
            throw new DAOException(DAOException.VALUE_NEEDED, "未找到该药企配置数据!");
        }
        if (config.getEnable_drug_sync()==0){
            throw new DAOException(DAOException.VALUE_NEEDED, "请先确认 基础数据-药品目录-药企药品目录-同步设置-【药企药品是否支持同步】已开启，再尝试进行同步!");
        }
        Integer organId = drugsEnterprise.getOrganId();
        if (ObjectUtils.isEmpty(organId)){
            throw new DAOException(DAOException.VALUE_NEEDED, "该药企["+drugsEnterprise.getName()+"]未找到关联机构!");
        }
        OrganService organService = AppDomainContext.getBean("basic.organService", OrganService.class);
        OrganDTO byOrganId = organService.getByOrganId(organId);
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
                List<OrganDrugList> details = organDrugListDAO.findOrganDrugByOrganId(drugsEnterprise.getOrganId());
                if (config.getSyncDataSource() == 1) {
                    //数据来源 关联管理机构
                    //获取药企关联机构药品目录
                    if (!ObjectUtils.isEmpty(details)){
                        total = details.size();
                        for (OrganDrugList detail : details) {
                          if (config.getSyncDataRange() == 1) {
                              //同步数据范围 配送药企
                              if (!ObjectUtils.isEmpty(detail.getDrugsEnterpriseIds())) {
                                  String[] split = detail.getDrugsEnterpriseIds().split(",");
                                  List<String> userIdList = new ArrayList<String>(Arrays.asList(split));
                                  if (userIdList.indexOf(drugsEnterpriseId.toString()) != -1) {
                                      Map<String, Integer> stringIntegerMap = syncOrganDrugDataToSaleDrugList(detail, config, drugsEnterpriseId);
                                      LOGGER.info("syncSaleOrganDrug药企药品数据同步 配送 " + detail.getDrugName() + " 药企Id=[{}] drug=[{}]", drugsEnterpriseId, JSONUtils.toString(detail));
                                      addNum = addNum + stringIntegerMap.get("addNum");
                                      updateNum = updateNum + stringIntegerMap.get("updateNum");
                                      falseNum = falseNum + stringIntegerMap.get("falseNum");
                                  }
                              }
                          } else if (config.getSyncDataRange() == 2) {
                              //同步数据范围 药品类型
                              if (ObjectUtils.isEmpty(config.getSyncDrugType())) {
                                  throw new DAOException(DAOException.VALUE_NEEDED, "未找到该药企[同步药品类型]配置数据!");
                              }
                              if (ObjectUtils.isEmpty(config.getEnable_drug_syncType())) {
                                  throw new DAOException(DAOException.VALUE_NEEDED, "未找到该药企[数据同步类型]配置数据!");
                              }
                              String[] strings1 = config.getSyncDrugType().split(",");
                              List<String> syncDrugTypeList = new ArrayList<String>(Arrays.asList(strings1));
                              if (!ObjectUtils.isEmpty(drugListDAO.get(detail.getDrugId()))){
                                  Integer drugType = drugListDAO.get(detail.getDrugId()).getDrugType();
                                  if (!ObjectUtils.isEmpty(drugType)){
                                      //西药
                                      if (syncDrugTypeList.indexOf("1") != -1) {
                                          if (drugType == 1) {
                                              Map<String, Integer> stringIntegerMap = syncOrganDrugDataToSaleDrugList(detail, config, drugsEnterpriseId);
                                              LOGGER.info("syncSaleOrganDrug药企药品数据同步 西药 " + detail.getDrugName() + " 药企Id=[{}] drug=[{}]", drugsEnterpriseId, JSONUtils.toString(detail));
                                              addNum = addNum + stringIntegerMap.get("addNum");
                                              updateNum = updateNum + stringIntegerMap.get("updateNum");
                                              falseNum = falseNum + stringIntegerMap.get("falseNum");
                                          }
                                      }
                                      //中成药
                                      if (syncDrugTypeList.indexOf("2") != -1) {
                                          if (drugType == 2) {
                                              Map<String, Integer> stringIntegerMap = syncOrganDrugDataToSaleDrugList(detail, config, drugsEnterpriseId);
                                              LOGGER.info("syncSaleOrganDrug药企药品数据同步 中成药 " + detail.getDrugName() + " 药企Id=[{}] drug=[{}]", drugsEnterpriseId, JSONUtils.toString(detail));
                                              addNum = addNum + stringIntegerMap.get("addNum");
                                              updateNum = updateNum + stringIntegerMap.get("updateNum");
                                              falseNum = falseNum + stringIntegerMap.get("falseNum");
                                          }
                                      }
                                      //中药
                                      if (syncDrugTypeList.indexOf("3") != -1) {
                                          if (drugType == 3) {
                                              Map<String, Integer> stringIntegerMap = syncOrganDrugDataToSaleDrugList(detail, config, drugsEnterpriseId);
                                              LOGGER.info("syncSaleOrganDrug药企药品数据同步 中药 " + detail.getDrugName() + " 药企Id=[{}] drug=[{}]", drugsEnterpriseId, JSONUtils.toString(detail));
                                              addNum = addNum + stringIntegerMap.get("addNum");
                                              updateNum = updateNum + stringIntegerMap.get("updateNum");
                                              falseNum = falseNum + stringIntegerMap.get("falseNum");
                                          }
                                      }
                                  }
                              }
                              }
                          }
                      }
                    }
                String[] strings = config.getEnable_drug_syncType().split(",");
                List<String> syncTypeList = new ArrayList<String>(Arrays.asList(strings));
                Map<String, OrganDrugList> drugMap = Maps.newHashMap();
                if (syncTypeList.indexOf("3")!=-1){
                    if (config.getSyncDataRange()==1){
                        String drugsEnterprise="%"+drugsEnterpriseId.toString()+"%";
                        List<OrganDrugList> drugLists = organDrugListDAO.findOrganDrugByOrganIdAndDrugsEnterpriseId(organId, drugsEnterprise);
                        drugMap = drugLists.stream().collect(Collectors.toMap(OrganDrugList::getOrganDrugCode, a -> a, (k1, k2) -> k1));
                    }else {
                        drugMap = details.stream().collect(Collectors.toMap(OrganDrugList::getOrganDrugCode, a -> a, (k1, k2) -> k1));
                    }
                    List<SaleDrugList> saleDrugListsByOrganId = saleDrugListDAO.findSaleDrugListsByOrganId(drugsEnterpriseId);
                    if (!ObjectUtils.isEmpty(saleDrugListsByOrganId)){
                        for (SaleDrugList saleDrugList : saleDrugListsByOrganId) {
                            OrganDrugList organDrug = drugMap.get(saleDrugList.getOrganDrugCode());
                            if (ObjectUtils.isEmpty(organDrug)) {
                                saleDrugListDAO.remove(saleDrugList.getOrganDrugId());
                                deleteNum++;
                            }
                        }
                    }
                }


                map.put("addNum", addNum);
                map.put("updateNum", updateNum);
                map.put("falseNum", falseNum);
                map.put("deleteNum", deleteNum);
                map.put("total", total);
                map.put("organName", byOrganId.getName());
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
     *
     * @param drugsEnterpriseId
     * @return
     */
    @RpcService(timeout = 6000)
    public Map<String, Object> saleDrugInfoSynMovementT(Integer drugsEnterpriseId) throws ParseException {
        if (ObjectUtils.isEmpty(drugsEnterpriseId)){
            throw new DAOException(DAOException.VALUE_NEEDED, "drugsenterpriseId is required!");
        }
        DrugsEnterprise drugsEnterprise = drugsEnterpriseDAO.get(drugsEnterpriseId);
        if (ObjectUtils.isEmpty(drugsEnterprise)){
            throw new DAOException(DAOException.VALUE_NEEDED, "未找到该药企"+drugsEnterpriseId);
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
        RecipeHisService hisService = ApplicationUtils.getRecipeService(RecipeHisService.class);
        UserRoleToken urt = UserRoleToken.getCurrent();
        DrugsEnterpriseConfigService bean = AppContextHolder.getBean("eh.drugsEnterpriseConfigService", DrugsEnterpriseConfigService.class);
        DrugsEnterpriseConfig config = bean.getConfigByDrugsenterpriseId(drugsEnterpriseId);
        if (ObjectUtils.isEmpty(config)){
            throw new DAOException(DAOException.VALUE_NEEDED, "未找到该药企配置数据!");
        }
        if (config.getEnable_drug_sync()==0){
            throw new DAOException(DAOException.VALUE_NEEDED, "请先确认 基础数据-药品目录-药企药品目录-同步设置-【药企药品是否支持同步】已开启，再尝试进行同步!");
        }
        Integer organId = drugsEnterprise.getOrganId();
        if (ObjectUtils.isEmpty(organId)){
            throw new DAOException(DAOException.VALUE_NEEDED, "该药企["+drugsEnterprise.getName()+"]未找到关联机构!");
        }
        OrganService organService = AppDomainContext.getBean("basic.organService", OrganService.class);
        OrganDTO byOrganId = organService.getByOrganId(organId);
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
        List<OrganDrugList> details = organDrugListDAO.findOrganDrugByOrganId(drugsEnterprise.getOrganId());
        if (config.getSyncDataSource() == 1) {
            //数据来源 关联管理机构
            //获取药企关联机构药品目录
            if (!ObjectUtils.isEmpty(details)){
                total=details.size();
                try {
                    for (OrganDrugList detail : details) {
                        if (config.getSyncDataRange() == 1) {
                            //同步数据范围 配送药企
                            if (!ObjectUtils.isEmpty(detail.getDrugsEnterpriseIds())) {
                                String[] split = detail.getDrugsEnterpriseIds().split(",");
                                List<String> userIdList = new ArrayList<String>(Arrays.asList(split));
                                if (userIdList.indexOf(drugsEnterpriseId.toString()) != -1) {
                                    Map<String, Integer> stringIntegerMap = syncOrganDrugDataToSaleDrugList(detail, config, drugsEnterpriseId);
                                    LOGGER.info("syncSaleOrganDrug药企药品数据同步 配送 " + detail.getDrugName() + " 药企Id=[{}] drug=[{}]", drugsEnterpriseId, JSONUtils.toString(detail));
                                    addNum = addNum + stringIntegerMap.get("addNum");
                                    updateNum = updateNum + stringIntegerMap.get("updateNum");
                                    falseNum = falseNum + stringIntegerMap.get("falseNum");
                                }
                            }
                        } else if (config.getSyncDataRange() == 2) {
                            //同步数据范围 药品类型
                            if (ObjectUtils.isEmpty(config.getSyncDrugType())) {
                                throw new DAOException(DAOException.VALUE_NEEDED, "未找到该药企[同步药品类型]配置数据!");
                            }
                            if (ObjectUtils.isEmpty(config.getEnable_drug_syncType())) {
                                throw new DAOException(DAOException.VALUE_NEEDED, "未找到该药企[数据同步类型]配置数据!");
                            }
                            String[] strings1 = config.getSyncDrugType().split(",");
                            List<String> syncDrugTypeList = new ArrayList<String>(Arrays.asList(strings1));
                            if (!ObjectUtils.isEmpty(drugListDAO.get(detail.getDrugId()))){
                                Integer drugType = drugListDAO.get(detail.getDrugId()).getDrugType();
                                if (!ObjectUtils.isEmpty(drugType)){
                                    //西药
                                    if (syncDrugTypeList.indexOf("1") != -1) {
                                        if (drugType == 1) {
                                            Map<String, Integer> stringIntegerMap = syncOrganDrugDataToSaleDrugList(detail, config, drugsEnterpriseId);
                                            LOGGER.info("syncSaleOrganDrug药企药品数据同步 西药 " + detail.getDrugName() + " 药企Id=[{}] drug=[{}]", drugsEnterpriseId, JSONUtils.toString(detail));
                                            addNum = addNum + stringIntegerMap.get("addNum");
                                            updateNum = updateNum + stringIntegerMap.get("updateNum");
                                            falseNum = falseNum + stringIntegerMap.get("falseNum");
                                        }
                                    }
                                    //中成药
                                    if (syncDrugTypeList.indexOf("2") != -1) {
                                        if (drugType == 2) {
                                            Map<String, Integer> stringIntegerMap = syncOrganDrugDataToSaleDrugList(detail, config, drugsEnterpriseId);
                                            LOGGER.info("syncSaleOrganDrug药企药品数据同步 中成药 " + detail.getDrugName() + " 药企Id=[{}] drug=[{}]", drugsEnterpriseId, JSONUtils.toString(detail));
                                            addNum = addNum + stringIntegerMap.get("addNum");
                                            updateNum = updateNum + stringIntegerMap.get("updateNum");
                                            falseNum = falseNum + stringIntegerMap.get("falseNum");
                                        }
                                    }
                                    //中药
                                    if (syncDrugTypeList.indexOf("3") != -1) {
                                        if (drugType == 3) {
                                            Map<String, Integer> stringIntegerMap = syncOrganDrugDataToSaleDrugList(detail, config, drugsEnterpriseId);
                                            LOGGER.info("syncSaleOrganDrug药企药品数据同步 中药 " + detail.getDrugName() + " 药企Id=[{}] drug=[{}]", drugsEnterpriseId, JSONUtils.toString(detail));
                                            addNum = addNum + stringIntegerMap.get("addNum");
                                            updateNum = updateNum + stringIntegerMap.get("updateNum");
                                            falseNum = falseNum + stringIntegerMap.get("falseNum");
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (DAOException e) {
                    LOGGER.info("syncSaleOrganDrug error" ,e);
                }
            }
        }
        String[] strings = config.getEnable_drug_syncType().split(",");
        List<String> syncTypeList = new ArrayList<String>(Arrays.asList(strings));
        Map<String, OrganDrugList> drugMap = Maps.newHashMap();

        if (syncTypeList.indexOf("3")!=-1){
            if (config.getSyncDataRange()==1){
                String drugsEnt="%"+drugsEnterpriseId.toString()+"%";
                List<OrganDrugList> drugLists = organDrugListDAO.findOrganDrugByOrganIdAndDrugsEnterpriseId(organId, drugsEnt);
                drugMap = drugLists.stream().collect(Collectors.toMap(OrganDrugList::getOrganDrugCode, a -> a, (k1, k2) -> k1));
            }else {
                drugMap = details.stream().collect(Collectors.toMap(OrganDrugList::getOrganDrugCode, a -> a, (k1, k2) -> k1));
            }
            List<SaleDrugList> saleDrugListsByOrganId = saleDrugListDAO.findSaleDrugListsByOrganId(drugsEnterpriseId);
            if (!ObjectUtils.isEmpty(saleDrugListsByOrganId)){
                for (SaleDrugList saleDrugList : saleDrugListsByOrganId) {
                    OrganDrugList organDrug = drugMap.get(saleDrugList.getOrganDrugCode());
                    if (ObjectUtils.isEmpty(organDrug)) {
                        saleDrugListDAO.remove(saleDrugList.getOrganDrugId());
                        deleteNum++;
                    }
                }
            }
        }


        map.put("addNum", addNum);
        map.put("updateNum", updateNum);
        map.put("deleteNum", deleteNum);
        map.put("falseNum", 0);
        map.put("total", total);
        map.put("organName", byOrganId.getName());
        map.put("Date", myFmt2.format(new Date()));
        map.put("Status", 1);
        redisClient.del(KEY_THE_DRUG_SYNC + drugsEnterpriseId.toString());
        redisClient.set(KEY_THE_DRUG_SYNC + drugsEnterpriseId.toString(), map);
        return map;
    }



}

