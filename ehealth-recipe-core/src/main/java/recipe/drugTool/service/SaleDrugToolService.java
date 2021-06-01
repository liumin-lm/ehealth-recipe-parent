package recipe.drugTool.service;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.ngari.opbase.xls.mode.ImportExcelInfoDTO;
import com.ngari.opbase.xls.service.IImportExcelInfoService;
import com.ngari.recipe.drugTool.service.ISaleDrugToolService;
import com.ngari.recipe.entity.DrugList;
import com.ngari.recipe.entity.ImportDrugRecord;
import com.ngari.recipe.entity.SaleDrugList;
import ctd.util.AppContextHolder;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.io.output.StringBuilderWriter;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.dao.DrugListDAO;
import recipe.dao.ImportDrugRecordDAO;
import recipe.dao.OrganDrugListDAO;
import recipe.dao.SaleDrugListDAO;
import recipe.service.OrganDrugListService;
import recipe.util.RedisClient;

import javax.annotation.Resource;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * created by renfuhao on 2020/6/11
 */
@RpcBean(value = "saleDrugToolService")
public class SaleDrugToolService implements ISaleDrugToolService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SaleDrugToolService.class);
    private static final String SUFFIX_2003 = ".xls";
    private static final String SUFFIX_2007 = ".xlsx";

    @Resource
    private DrugListDAO drugListDAO;

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
                drug.setOrganDrugCode(getStrFromCell(row.getCell(0)));
            } catch (Exception e) {
                LOGGER.error("药品编号有误 ," + e.getMessage(),e);
                errMsg.append("药品编号有误").append(";");
            }



            try {
                if (StringUtils.isEmpty(getStrFromCell(row.getCell(2)))) {
                    if (StringUtils.isEmpty(getStrFromCell(row.getCell(31)))) {
                        errMsg.append("平台药品编号不能为空").append(";");
                    }
                    DrugList byId = drugListDAO.getById(Integer.parseInt(getStrFromCell(row.getCell(31))));
                    if (byId != null){
                        drug.setDrugName(byId.getDrugName());
                    }
                }else {
                    drug.setDrugName(getStrFromCell(row.getCell(2)));
                }
            } catch (Exception e) {
                LOGGER.error("药品名有误 ," + e.getMessage(),e);
                errMsg.append("药品名有误").append(";");
            }

            try {
                if (StringUtils.isEmpty(getStrFromCell(row.getCell(3)))) {
                    if (StringUtils.isEmpty(getStrFromCell(row.getCell(31)))) {
                        errMsg.append("平台药品编号不能为空").append(";");
                    }
                    DrugList byId = drugListDAO.getById(Integer.parseInt(getStrFromCell(row.getCell(31))));
                    if (byId != null){
                        drug.setDrugName(byId.getSaleName());
                    }
                }else {
                    drug.setSaleName(getStrFromCell(row.getCell(3)));
                }
            } catch (Exception e) {
                LOGGER.error("药品商品名有误 ," + e.getMessage(),e);
                errMsg.append("药品商品名有误").append(";");
            }

            try {
                    drug.setDrugSpec(getStrFromCell(row.getCell(6)));
            } catch (Exception e) {
                LOGGER.error("药品规格有误 ," + e.getMessage(),e);
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
                LOGGER.error("平台药品编号有误 ," + e.getMessage(),e);
                errMsg.append("平台药品编号有误").append(";");
            }

            try {
                if (StringUtils.isEmpty(getStrFromCell(row.getCell(20)))) {
                    errMsg.append("价格不能为空").append(";");
                }
                    drug.setPrice(BigDecimal.valueOf(Double.parseDouble(getStrFromCell(row.getCell(20)))));
                    drug.setRate(0.00);
                    drug.setRatePrice(Double.parseDouble(getStrFromCell(row.getCell(20))));
            } catch (Exception e) {
                LOGGER.error("价格有误 ," + e.getMessage(),e);
                errMsg.append("价格有误").append(";");
            }

            if (!drugListDAO.exist(drug.getDrugId())) {
                errMsg.append("平台药品编号错误！").append(";");
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
}

