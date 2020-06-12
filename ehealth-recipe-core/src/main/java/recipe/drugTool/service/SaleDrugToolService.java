package recipe.drugTool.service;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.ngari.recipe.drugTool.service.ISaleDrugToolService;
import com.ngari.recipe.entity.*;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.dao.*;
import recipe.service.OrganDrugListService;
import recipe.util.RedisClient;

import javax.annotation.Resource;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * created by renfuhao on 2020/6/11
 */
@RpcBean(value = "saleDrugToolService", mvc_authentication = false)
public class SaleDrugToolService implements ISaleDrugToolService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SaleDrugToolService.class);
    private static final String SUFFIX_2003 = ".xls";
    private static final String SUFFIX_2007 = ".xlsx";
    private double progress;
    private RedisClient redisClient = RedisClient.instance();

    @Resource
    private DrugListDAO drugListDAO;

    @Resource
    private SaleDrugListDAO saleDrugListDAO;

    @Resource
    private OrganDrugListDAO organDrugListDAO;

    @Resource
    private OrganDrugListService organDrugListService;

    @Resource
    private ImportDrugRecordDAO importDrugRecordDAO;


    private LoadingCache<String, List<DrugList>> drugListCache = CacheBuilder.newBuilder().expireAfterWrite(10, TimeUnit.MINUTES).build(new CacheLoader<String, List<DrugList>>() {
        @Override
        public List<DrugList> load(String str) throws Exception {
            return drugListDAO.findBySaleNameLike(str);
        }
    });

    @Override
    public synchronized Map<String, Object> readDrugExcel(byte[] buf, String originalFilename, int organId, String operator) {
        LOGGER.info(operator + "开始 readDrugExcel 方法" + System.currentTimeMillis() + "当前进程=" + Thread.currentThread().getName());
        progress = 0;
        String key = organId + operator;
        if (redisClient.exists(key)) {
            redisClient.del(key);
        }
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
            LOGGER.error("readDrugExcel error ," + e.getMessage());
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
                String retrievalCode = getStrFromCell(row.getCell(8));
                if ("药品名".equals(drugCode) && "商品名".equals(drugName) && "默认单次剂量".equals(retrievalCode)) {
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
                    errMsg.append("药企药品编码不能为空").append(";");
                }
                drug.setOrganDrugCode(getStrFromCell(row.getCell(1)));
            } catch (Exception e) {
                errMsg.append("药品编号有误").append(";");
            }



            try {
                drug.setDrugName(getStrFromCell(row.getCell(2)));
            } catch (Exception e) {
                errMsg.append("药品名有误").append(";");
            }

            try {
                drug.setSaleName(getStrFromCell(row.getCell(3)));
            } catch (Exception e) {
                errMsg.append("药品商品名有误").append(";");
            }

            try {
                    drug.setDrugSpec(getStrFromCell(row.getCell(6)));
            } catch (Exception e) {
                errMsg.append("药品规格有误").append(";");
            }
            if (!StringUtils.isEmpty(getStrFromCell(row.getCell(31)))) {
                SaleDrugList byOrganIdAndDrugId = saleDrugListDAO.getByOrganIdAndDrugId(organId, Integer.parseInt(getStrFromCell(row.getCell(31))));
                if (byOrganIdAndDrugId!=null){
                    errMsg.append("药品已存在").append(";");
                }
            }
            try {
                if (StringUtils.isEmpty(getStrFromCell(row.getCell(31)))) {
                    errMsg.append("平台药品编号不能为空").append(";");
                }
                drug.setDrugId(Integer.parseInt(getStrFromCell(row.getCell(31))));
            } catch (Exception e) {
                errMsg.append("平台药品编号有误").append(";");
            }

            try {
                if (StringUtils.isEmpty(getStrFromCell(row.getCell(20)))) {
                    errMsg.append("价格不能为空").append(";");
                }
                    drug.setPrice(BigDecimal.valueOf(Integer.parseInt(getStrFromCell(row.getCell(20)))));
                    drug.setRate(0.00);
                    drug.setRatePrice(Double.parseDouble(getStrFromCell(row.getCell(20))));
            } catch (Exception e) {
                errMsg.append("价格有误").append(";");
            }

            drug.setStatus(1);
            drug.setOrganId(organId);
            drug.setInventory(new BigDecimal(100));
            drug.setCreateDt(new Date());
            drug.setLastModify(new Date());

            if (errMsg.length() > 1) {
                int showNum = rowIndex + 1;
                String error = ("【第" + showNum + "行】" + errMsg.substring(0, errMsg.length() - 1));
                errDrugListMatchList.add(error);
            } else {
                drugLists.add(drug);
            }
            progress = new BigDecimal((float) rowIndex / total).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
            redisClient.set(organId + operator, progress * 100);
//                    progressMap.put(organId+operator,progress*100);
        }
        if (errDrugListMatchList.size()>0){
            //导入药品记录
            ImportDrugRecord importDrugRecord = new ImportDrugRecord();
            importDrugRecord.setFileName(originalFilename);
            importDrugRecord.setOrganId(organId);
            importDrugRecord.setAddNum(addNum);
            importDrugRecord.setUpdateNum(updateNum);
            importDrugRecord.setFailNum(total-addNum-updateNum);
            importDrugRecord.setImportOperator(operator);
            importDrugRecord.setErrMsg(JSONUtils.toString(errDrugListMatchList));
            importDrugRecordDAO.save(importDrugRecord);

            result.put("code", 609);
            result.put("msg", errDrugListMatchList);
            result.put("addNum",addNum);
            result.put("updateNum",updateNum);
            result.put("failNum",total-addNum-updateNum);
            LOGGER.info(operator + "结束 readDrugExcel 方法" + System.currentTimeMillis() + "当前进程=" + Thread.currentThread().getName());
            return result;

        }else {
            for (SaleDrugList drugList : drugLists) {
                try {
                    //自动匹配功能暂无法提供
                    saleDrugListDAO.save(drugList);
                    addNum++;

                } catch (Exception e) {
                    LOGGER.error("save or update drugListMatch error " + e.getMessage());
                }
            }
        }

        //导入药品记录
        ImportDrugRecord importDrugRecord = new ImportDrugRecord();
        importDrugRecord.setFileName(originalFilename);
        importDrugRecord.setOrganId(organId);
        importDrugRecord.setAddNum(addNum);
        importDrugRecord.setUpdateNum(updateNum);
        importDrugRecord.setFailNum(total-addNum-updateNum);
        importDrugRecord.setImportOperator(operator);
        /*if (errDrugListMatchList.size() > 0) {
            result.put("code", 609);
            result.put("msg", errDrugListMatchList);
            importDrugRecord.setErrMsg(JSONUtils.toString(errDrugListMatchList));
            return result;
        }*/
        importDrugRecordDAO.save(importDrugRecord);


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
}

