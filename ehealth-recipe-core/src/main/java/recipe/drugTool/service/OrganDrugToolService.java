package recipe.drugTool.service;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.ngari.patient.dto.OrganDTO;
import com.ngari.patient.dto.UsePathwaysDTO;
import com.ngari.patient.dto.UsingRateDTO;
import com.ngari.patient.service.IUsePathwaysService;
import com.ngari.patient.service.IUsingRateService;
import com.ngari.patient.service.OrganService;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.drugTool.service.IOrganDrugToolService;
import com.ngari.recipe.entity.*;
import ctd.persistence.DAOFactory;
import ctd.spring.AppDomainContext;
import ctd.util.AppContextHolder;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ObjectUtils;
import recipe.aop.LogRecord;
import recipe.bussutil.ExcelUtil;
import recipe.constant.DrugMatchConstant;
import recipe.dao.*;
import recipe.third.IFileDownloadService;
import recipe.vo.greenroom.ImportDrugRecordVO;

import javax.annotation.Resource;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * created by renfuhao on 2020/6/11
 */
@RpcBean(value = "organDrugToolService")
public class OrganDrugToolService implements IOrganDrugToolService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DrugToolService.class);

    @Resource
    private ImportDrugRecordDAO importDrugRecordDAO;

    @Autowired
    private IFileDownloadService fileDownloadService;

    @Resource
    private OrganDrugListDAO organDrugListDAO;

    @Resource
    private PharmacyTcmDAO pharmacyTcmDAO;

    @Autowired
    private DrugsEnterpriseDAO drugsEnterpriseDAO;

    @Autowired
    private DrugToolService drugToolService;

    @Autowired
    private DrugListMatchDAO drugListMatchDAO;

    @Autowired
    private DrugEntrustDAO drugEntrustDAO;

    private static final String SUFFIX_2003 = ".xls";
    private static final String SUFFIX_2007 = ".xlsx";

    @Override
    @LogRecord
    public Map<String, Object> readDrugExcel(Integer id) {
        Map<String, Object> result = Maps.newHashMap();
        ImportDrugRecord importDrugRecord=importDrugRecordDAO.get(id);
        if(importDrugRecord==null){
            result.put("code", 609);
            result.put("msg", "导入失败，原因是文件不存在");
            return result;
        }
        String operator=importDrugRecord.getImportOperator();
        String fileId=importDrugRecord.getFileId();
        Integer organId=importDrugRecord.getOrganId();
        String fileName=importDrugRecord.getFileName();
        LOGGER.info(operator + "开始 readDrugExcel 方法" + System.currentTimeMillis() + "当前进程=" + Thread.currentThread().getName());
        //数据处理
        byte[] buf=fileDownloadService.downloadAsByte(fileId);
        int bankNumber = 0;
        DrugListMatch drug;
        AtomicReference<Integer> rowIndex= new AtomicReference<>(0);
        AtomicReference<Integer> total = new AtomicReference<>(0);
        List<String> errDrugListMatchList = Lists.newArrayList();
        Integer addNum = 0;
        Integer updateNum = 0;
        Integer failNum = 0;
        ExcelUtil excelUtil = new ExcelUtil(cells -> {
            System.out.println("cells:"+cells); //直接输出每一行的内容
            obtainReadExcelDrugListMatch(cells,bankNumber, rowIndex.get(),result,organId,operator,errDrugListMatchList,addNum,updateNum,failNum);
            rowIndex.getAndSet(rowIndex.get() + 1);
            total.getAndSet(total.get() + 1);
        });
        InputStream is = new ByteArrayInputStream(buf);
        if (fileName.endsWith(SUFFIX_2003)) {
            LOGGER.info("readDrugExcel SUFFIX_2003");
        } else if (fileName.endsWith(SUFFIX_2007)) {
            //使用InputStream需要将所有内容缓冲到内存中，这会占用空间并占用时间
            //当数据量过大时，这里会非常耗时
            LOGGER.info("readDrugExcel SUFFIX_2007");
        } else {
            result.put("code", 609);
            result.put("msg", "上传文件格式有问题");
            return result;
        }
        if (StringUtils.isEmpty(operator)) {
            result.put("code", 609);
            result.put("msg", "operator is required");
            return result;
        }
//        int length = buf.length;
//        LOGGER.info("readDrugExcel byte[] length=" + length);
//        int max = 1343518;
//        //控制导入数据量
//        if (max <= length) {
//            result.put("code", 609);
//            result.put("msg", "超过7000条数据,请分批导入");
//            return result;
//        }
        LOGGER.info("机构药品目录导入数据校验开始,文件名={},organId={},operator={}", fileName, organId, operator);
        try {
            excelUtil.load(is).parse();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InvalidFormatException e) {
            e.printStackTrace();
        }


//        Integer total = sheet.getLastRowNum();
//        if (total == null || total <= 0) {
//            result.put("code", 609);
//            result.put("msg", "data is required");
//            return result;
//        }

        LOGGER.info("机构药品目录导入数据校验errorMsg:{},文件名={},organId={},operator={}", JSONUtils.toString(errDrugListMatchList),fileName, organId, operator);
        LOGGER.info("机构药品目录导入数据校验结束,文件名={},organId={},operator={}", fileName, organId, operator);
        //更新药品记录
        importDrugRecord.setAddNum(addNum);
        importDrugRecord.setUpdateNum(updateNum);
        importDrugRecord.setFailNum(failNum);
        if (errDrugListMatchList.size() > 0) {
            result.put("code", 609);
            result.put("msg", errDrugListMatchList);
            result.put("addNum", addNum);
            result.put("updateNum", updateNum);
            result.put("failNum", failNum);
            //TODO
            importDrugRecord.setErrMsg(JSONUtils.toString(errDrugListMatchList.stream().limit(600).collect(Collectors.toList())));
            importDrugRecordDAO.save(importDrugRecord);
            return result;
        }
        importDrugRecordDAO.save(importDrugRecord);

        result.put("addNum", addNum);
        result.put("updateNum", updateNum);
        result.put("failNum", total.get() - addNum - updateNum - bankNumber);
        LOGGER.info(operator + "结束 readDrugExcel 方法" + System.currentTimeMillis() + "当前进程=" + Thread.currentThread().getName());
        result.put("code", 200);
        LOGGER.info("result={}",JSONUtils.toString(result));
        return result;
    }

    @Override
    public Integer saveImportDrugRecord(ImportDrugRecordVO param) {
        ImportDrugRecord importDrugRecord= ObjectCopyUtils.convert(param,ImportDrugRecord.class);
        importDrugRecord=importDrugRecordDAO.save(importDrugRecord);
        return importDrugRecord.getRecordId();
    }

    /**
     * 表格行数据转换成DrugListMatch
     * @param cells
     * @param bankNumber
     * @param rowIndex
     * @param result
     * @return
     */
    public DrugListMatch obtainReadExcelDrugListMatch(List<String>  cells,int bankNumber,int rowIndex, Map<String, Object> result,Integer organId
            ,String operator,List<String> errDrugListMatchList,Integer addNum,Integer updateNum,Integer failNum){
        DrugListMatch drug = new DrugListMatch();
        //循环获得每个行
        boolean flag = false;
        if(null != cells){
            for(String cell : cells){
                if (cell == null) {
                    flag = true;
                    break;
                }
            }
        }
        //如果为空行的数据，则空行数+1
        if(!flag) {
            bankNumber +=1;
        }
        //否则正常读取数据
        else {
            // 判断是否是模板
            if (rowIndex == 0) {
                String drugCode = getStrFromCell(cells.get(0));
                String drugName = getStrFromCell(cells.get(1));
                String retrievalCode = getStrFromCell(cells.get(32));
                if ("监管药品编码".equals(drugCode) && "平台药品编码".equals(drugName) && "单复方".equals(retrievalCode)) {
                    return drug;
                } else {
                    result.put("code", 609);
                    result.put("msg", "模板有误，请确认！");
                    return drug;
                }

            }
            drug = new DrugListMatch();
            StringBuilder errMsg = new StringBuilder();
            /*try{*/
            try {
                if (StringUtils.isNotEmpty(getStrFromCell(cells.get(0)))) {
                    drug.setRegulationDrugCode(getStrFromCell(cells.get(0)));
                }
            } catch (Exception e) {
                LOGGER.error("监管药品编码," + e.getMessage(), e);
                errMsg.append("监管药品编码有误").append(";");
            }

            try {
                if (StringUtils.isEmpty(getStrFromCell(cells.get(2)))) {
                    errMsg.append("机构唯一索引不能为空").append(";");
                }
                drug.setOrganDrugCode(getStrFromCell(cells.get(2)));
            } catch (Exception e) {
                LOGGER.error("机构唯一索引有误 ," + e.getMessage(), e);
                errMsg.append("机构唯一索引有误").append(";");
            }


            try {
                if (StringUtils.isNotEmpty(getStrFromCell(cells.get(3)))) {
                    drug.setDrugItemCode(getStrFromCell(cells.get(3)));
                }
            } catch (Exception e) {
                LOGGER.error("药品编码 ," + e.getMessage(), e);
                errMsg.append("药品编码").append(";");
            }


            try {
                if (StringUtils.isNotEmpty(getStrFromCell(cells.get(1)))) {
                    OrganDrugList byDrugIdAndOrganId = organDrugListDAO.getByDrugIdAndOrganId(Integer.parseInt(getStrFromCell(cells.get(1)).trim()), organId);
                    if (!StringUtils.isEmpty(getStrFromCell(cells.get(2)))) {
                        if (!ObjectUtils.isEmpty(byDrugIdAndOrganId)) {
                            if (!byDrugIdAndOrganId.getOrganDrugCode().equals(getStrFromCell(cells.get(2)))) {
                                errMsg.append("机构已存在药品关联该平台药品").append(";");
                            }
                        }
                    }
                    drug.setPlatformDrugId(Integer.parseInt(getStrFromCell(cells.get(1)).trim()));
                }
            } catch (Exception e) {
                LOGGER.error("平台药品编码有误 ," + e.getMessage(), e);
                errMsg.append("平台药品编码有误").append(";");
            }

            try {
                if (StringUtils.isEmpty(getStrFromCell(cells.get(4)))) {
                    errMsg.append("【药品名】未填写").append(";");
                }
                drug.setDrugName(getStrFromCell(cells.get(4)));
            } catch (Exception e) {
                LOGGER.error("药品名有误 ," + e.getMessage(), e);
                errMsg.append("药品名有误").append(";");
            }
            try {
                drug.setSaleName(getStrFromCell(cells.get(5)));
            } catch (Exception e) {
                LOGGER.error("药品商品名有误 ," + e.getMessage(), e);
                errMsg.append("药品商品名有误").append(";");
            }

            try {
                if (StringUtils.isNotEmpty(getStrFromCell(cells.get(6)))) {
                    drug.setChemicalName(getStrFromCell(cells.get(6)));
                }
            } catch (Exception e) {
                LOGGER.error("药品化学名有误," + e.getMessage(), e);
                errMsg.append("药品化学名有误").append(";");
            }

            try {
                if (StringUtils.isEmpty(getStrFromCell(cells.get(7)))) {
                    errMsg.append("【院内检索码】未填写").append(";");
                }
                drug.setRetrievalCode(getStrFromCell(cells.get(7)));
            } catch (Exception e) {
                LOGGER.error("药品院内检索码有误 ," + e.getMessage(), e);
                errMsg.append("药品院内检索码有误").append(";");
            }
           /* try {
                if (StringUtils.isNotEmpty(getStrFromCell(cells.get(7)))) {
                    if ("有效".equals(getStrFromCell(cells.get(7)).trim())){
                        drug.setStatus(1);
                    }
                }
            } catch (Exception e) {
                LOGGER.error("药品状态有误 ," + e.getMessage(), e);
                errMsg.append("药品状态有误").append(";");
            }*/
            try {
                if (StringUtils.isEmpty(getStrFromCell(cells.get(9)))) {
                    errMsg.append("【药品类型】未填写").append(";");
                }
                if (("中药").equals(getStrFromCell(cells.get(9)))) {
                    drug.setDrugType(3);
                } else if (("中成药").equals(getStrFromCell(cells.get(9)))) {
                    drug.setDrugType(2);
                } else if (("西药").equals(getStrFromCell(cells.get(9)))) {
                    drug.setDrugType(1);
                } else {
                    errMsg.append("药品类型格式错误").append(";");
                }
            } catch (Exception e) {
                LOGGER.error("药品类型有误 ," + e.getMessage(), e);
                errMsg.append("药品类型有误").append(";");
            }
            try {
                if (!StringUtils.isEmpty(getStrFromCell(cells.get(10)))) {
                    drug.setDrugForm(getStrFromCell(cells.get(10)));
                }
            } catch (Exception e) {
                LOGGER.error("药品剂型有误 ," + e.getMessage(), e);
                errMsg.append("药品剂型有误").append(";");
            }
            try {
                if (StringUtils.isEmpty(getStrFromCell(cells.get(11)))) {
                    errMsg.append("【药品规格/单位】未填写").append(";");
                }
                drug.setDrugSpec(getStrFromCell(cells.get(11)));
            } catch (Exception e) {
                LOGGER.error("药品规格/单位有误 ," + e.getMessage(), e);
                errMsg.append("药品规格/单位有误").append(";");
            }
            try {
                if (StringUtils.isNotEmpty(getStrFromCell(cells.get(12)))) {
                    drug.setUseDoseSmallestUnit(getStrFromCell(cells.get(12)));
                }
            } catch (Exception e) {
                LOGGER.error("药品最小单位有误 ," + e.getMessage(), e);
                errMsg.append("药品最小单位有误").append(";");
            }
            try {
                if (StringUtils.isEmpty(getStrFromCell(cells.get(13)))) {
                    //中药不需要设置
                    if (!(new Integer(3).equals(drug.getDrugType()))) {
                        errMsg.append("【包装数量(转换系数)】未填写").append(";");
                    } else {
                        drug.setPack(1);
                    }
                } else {
                    drug.setPack(Integer.parseInt(getStrFromCell(cells.get(13))));
                }
            } catch (Exception e) {
                LOGGER.error("包装数量(转换系数)有误 ," + e.getMessage(), e);
                errMsg.append("包装数量(转换系数)有误").append(";");
            }
            try {
                if (StringUtils.isEmpty(getStrFromCell(cells.get(14)))) {
                    //中药不需要设置
                    if (!(new Integer(3).equals(drug.getDrugType()))) {
                        errMsg.append("【单次剂量(规格单位)】未填写").append(";");
                    }

                } else {
                    drug.setUseDose(Double.parseDouble(getStrFromCell(cells.get(14))));
                }
            } catch (Exception e) {
                LOGGER.error("单次剂量(规格单位)有误 ," + e.getMessage(), e);
                errMsg.append("单次剂量(规格单位)有误").append(";");
            }
            try {
                if (StringUtils.isEmpty(getStrFromCell(cells.get(15)))) {
                    //中药不需要设置
                    if (!(new Integer(3).equals(drug.getDrugType()))) {
                        errMsg.append("【规格单位】未填写").append(";");
                    }

                } else {
                    drug.setUseDoseUnit(getStrFromCell(cells.get(15)));
                }
            } catch (Exception e) {
                LOGGER.error("规格单位有误 ," + e.getMessage(), e);
                errMsg.append("规格单位有误").append(";");
            }
            try {
                if (StringUtils.isNotEmpty(getStrFromCell(cells.get(16)))) {
                    drug.setRecommendedUseDose(Double.parseDouble(getStrFromCell(cells.get(16))));
                }
            } catch (Exception e) {
                LOGGER.error("默认单次剂量(规格单位)有误 ," + e.getMessage(), e);
                errMsg.append("默认单次剂量(规格单位)有误").append(";");
            }
            try {
                if (StringUtils.isEmpty(getStrFromCell(cells.get(17)))) {
                    //中药不需要设置
                    if (!(new Integer(3).equals(drug.getDrugType()))) {
                        errMsg.append("【包装单位】未填写").append(";");
                    }
                }
                drug.setUnit(getStrFromCell(cells.get(17)));
            } catch (Exception e) {
                LOGGER.error("药品包装单位有误 ," + e.getMessage(), e);
                errMsg.append("药品包装单位有误").append(";");
            }
            try {
                if (StringUtils.isEmpty(getStrFromCell(cells.get(18)))) {
                    errMsg.append("【单价】未填写").append(";");
                }
                String priceCell = getStrFromCell(cells.get(18));
                drug.setPrice(new BigDecimal(priceCell));
            } catch (Exception e) {
                LOGGER.error("药品单价有误 ," + e.getMessage(), e);
                errMsg.append("药品单价有误").append(";");
            }
            try {
                if (StringUtils.isEmpty(getStrFromCell(cells.get(19)))) {
                    //中药不需要设置
                    if (!(new Integer(3).equals(drug.getDrugType()))) {
                        errMsg.append("【生产厂家】未填写").append(";");
                    }
                }
                drug.setProducer(getStrFromCell(cells.get(19)));
            } catch (Exception e) {
                LOGGER.error("药品生产厂家有误 ," + e.getMessage(), e);
                errMsg.append("药品生产厂家有误").append(";");
            }
            try {
                if (StringUtils.isNotEmpty(getStrFromCell(cells.get(20)))) {
                    drug.setDrugManfCode(getStrFromCell(cells.get(20)));
                }
            } catch (Exception e) {
                LOGGER.error("药品产地编码有误 ," + e.getMessage(), e);
                errMsg.append("药品产地编码有误").append(";");
            }
            try {
                if (getStrFromCell(cells.get(21)) != null) {
                    String strFromCell = getStrFromCell(cells.get(21));
                    StringBuilder ss = new StringBuilder();
                    String[] split = strFromCell.split(",");
                    for (int i = 0; i < split.length; i++) {
                        Integer idByPharmacyName = pharmacyTcmDAO.getIdByPharmacyNameAndOrganId(split[i], organId);
                        if (idByPharmacyName == null) {
                            errMsg.append("药房名称有误").append(";");
                        } else {
                            if (i != split.length - 1) {
                                ss.append(idByPharmacyName.toString() + ",");
                            } else {
                                ss.append(idByPharmacyName.toString());
                            }
                        }
                    }
                    drug.setPharmacy(ss.toString());
                }
            } catch (Exception e) {
                LOGGER.error("药房名称有误 ," + e.getMessage(), e);
            }

            try {
                if (getStrFromCell(cells.get(22)) != null) {
                    String strFromCell = getStrFromCell(cells.get(22));
                    StringBuilder ss = new StringBuilder();
                    String[] split = strFromCell.split(",");
                    for (int i = 0; i < split.length; i++) {
                        DrugsEnterprise byEnterpriseCode = drugsEnterpriseDAO.getByEnterpriseCode(split[i], organId);
                        if (byEnterpriseCode == null) {
                            errMsg.append("平台未找到该配送药企" + split[i] + "").append(";");
                        } else {
                            if (i != split.length - 1) {
                                ss.append(byEnterpriseCode.getId().toString() + ",");
                            } else {
                                ss.append(byEnterpriseCode.getId().toString());
                            }
                        }
                    }
                    drug.setDrugsEnterpriseIds(ss.toString());
                }
            } catch (Exception e) {
                LOGGER.error("配送药企有误 ," + e.getMessage(), e);
            }

            try {
                if (StringUtils.isNotEmpty(getStrFromCell(cells.get(23)))) {
                    IUsingRateService bean = AppContextHolder.getBean("basic.usingRateService", IUsingRateService.class);
                    List<UsingRateDTO> allusingRateByOrganId = bean.findAllusingRateByOrganId(organId);
                    if (ObjectUtils.isEmpty(allusingRateByOrganId)) {
                        UsingRateDTO usingRateDTO = bean.findUsingRateDTOByOrganAndKey(0, getStrFromCell(cells.get(23)));
                        if (ObjectUtils.isEmpty(usingRateDTO)) {
                            errMsg.append("平台未找到该用药频次").append(";");
                        } else {
                            drug.setUsingRateId(usingRateDTO.getId().toString());
                        }

                    } else {
                        UsingRateDTO usingRateDTOByOrganAndKey = bean.findUsingRateDTOByOrganAndKey(organId, getStrFromCell(cells.get(23)));
                        if (ObjectUtils.isEmpty(usingRateDTOByOrganAndKey)) {
                            errMsg.append("机构未找到该用药频次").append(";");
                        } else {
                            drug.setUsingRateId(usingRateDTOByOrganAndKey.getId().toString());
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.error("用药频次有误 ," + e.getMessage(), e);
                errMsg.append("用药频次有误").append(";");
            }

            try {
                if (StringUtils.isNotEmpty(getStrFromCell(cells.get(25)))) {
                    IUsePathwaysService bean = AppContextHolder.getBean("basic.usePathwaysService", IUsePathwaysService.class);
                    List<UsePathwaysDTO> allUsePathwaysByOrganId = bean.findAllUsePathwaysByOrganId(organId);
                    if (ObjectUtils.isEmpty(allUsePathwaysByOrganId)) {
                        UsePathwaysDTO usePathwaysDTO = bean.findUsePathwaysByOrganAndKey(0, getStrFromCell(cells.get(25)));
                        if (ObjectUtils.isEmpty(usePathwaysDTO)) {
                            errMsg.append("平台未找到该用药途径").append(";");
                        } else {
                            drug.setUsePathwaysId(usePathwaysDTO.getId().toString());
                        }

                    } else {
                        UsePathwaysDTO usePathwaysDTO = bean.findUsePathwaysByOrganAndKey(organId, getStrFromCell(cells.get(25)));
                        if (ObjectUtils.isEmpty(usePathwaysDTO)) {
                            errMsg.append("机构未找到该用药途径").append(";");
                        } else {
                            drug.setUsePathwaysId(usePathwaysDTO.getId().toString());
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.error("用药途径有误 ," + e.getMessage(), e);
                errMsg.append("用药途径有误").append(";");
            }


            try {
                if (!StringUtils.isEmpty(getStrFromCell(cells.get(27)))) {
                    if ((new Integer(3).equals(drug.getDrugType()))) {
                        DrugEntrust byOrganIdAndDrugEntrustName = drugEntrustDAO.getByOrganIdAndDrugEntrustName(organId, getStrFromCell(cells.get(27)));
                        if (byOrganIdAndDrugEntrustName != null) {
                            drug.setDrugEntrust(byOrganIdAndDrugEntrustName.getDrugEntrustName().toString());
                        } else {
                            errMsg.append("中药药品字典未找到该嘱托").append(";");
                        }
                    } else {
                        drug.setDrugEntrust(getStrFromCell(cells.get(27)));
                    }
                }
            } catch (Exception e) {
                LOGGER.error("药品嘱托有误 ," + e.getMessage(), e);
                errMsg.append("药品嘱托有误").append(";");
            }
            try {
                if (StringUtils.isNotEmpty(getStrFromCell(cells.get(28)))) {
                    drug.setIndicationsDeclare(getStrFromCell(cells.get(28)));
                }
            } catch (Exception e) {
                LOGGER.error("药品适应症说明有误 ," + e.getMessage(), e);
                errMsg.append("药品适应症说明有误").append(";");
            }
            try {
                if (StringUtils.isNotEmpty(getStrFromCell(cells.get(29)))) {
                    drug.setPackingMaterials(getStrFromCell(cells.get(29)));
                }
            } catch (Exception e) {
                LOGGER.error("药品包装材料有误 ," + e.getMessage(), e);
                errMsg.append("药品包装材料有误").append(";");
            }

            //中药不需要设置
            if (!(new Integer(3).equals(drug.getDrugType()))) {
                try {
                    if (("是").equals(getStrFromCell(cells.get(30)))) {
                        drug.setBaseDrug(1);
                    } else if (("否").equals(getStrFromCell(cells.get(30)))) {
                        drug.setBaseDrug(0);
                    } else {
                        errMsg.append("是否基药格式不正确").append(";");
                    }

                } catch (Exception e) {
                    LOGGER.error("是否基药有误 ," + e.getMessage(), e);
                    errMsg.append("是否基药有误").append(";");
                }
            }
            try {
                if (!StringUtils.isEmpty(getStrFromCell(cells.get(31)))) {
                    if (("是").equals(getStrFromCell(cells.get(31)))) {
                        drug.setMedicalInsuranceControl(true);
                    } else if (("否").equals(getStrFromCell(cells.get(31)))) {
                        drug.setMedicalInsuranceControl(false);
                    } else {
                        errMsg.append("医保控制格式错误").append(";");
                    }
                }
            } catch (Exception e) {
                LOGGER.error("医保控制有误 ," + e.getMessage(), e);
                errMsg.append("医保控制有误").append(";");
            }

            try {
                if (!StringUtils.isEmpty(getStrFromCell(cells.get(32)))) {
                    if (("单复方可报").equals(getStrFromCell(cells.get(32)))) {
                        drug.setUnilateralCompound(0);
                    } else if (("单方不可报，复方可报").equals(getStrFromCell(cells.get(32)))) {
                        drug.setUnilateralCompound(1);
                    } else if (("单复方均不可报").equals(getStrFromCell(cells.get(32)))) {
                        drug.setUnilateralCompound(2);
                    } else {
                        errMsg.append("单复方格式错误").append(";");
                    }
                }
            } catch (Exception e) {
                LOGGER.error("单复方有误 ," + e.getMessage(), e);
                errMsg.append("单复方有误").append(";");
            }

            try {
                if (StringUtils.isNotEmpty(getStrFromCell(cells.get(33)))) {
                    drug.setMedicalDrugCode(getStrFromCell(cells.get(33)));
                }
            } catch (Exception e) {
                LOGGER.error("药品医保药品编码有误 ," + e.getMessage(), e);
                errMsg.append("药品医保药品编码有误").append(";");
            }
            try {
                if (StringUtils.isNotEmpty(getStrFromCell(cells.get(34)))) {
                    drug.setMedicalDrugFormCode(getStrFromCell(cells.get(34)));
                }
            } catch (Exception e) {
                LOGGER.error("药品医保剂型代码有误 ," + e.getMessage(), e);
                errMsg.append("药品医保剂型代码有误").append(";");
            }
            try {
                if (StringUtils.isNotEmpty(getStrFromCell(cells.get(35)))) {
                    drug.setHisFormCode(getStrFromCell(cells.get(35)));
                }
            } catch (Exception e) {
                LOGGER.error("药品HIS剂型代码有误 ," + e.getMessage(), e);
                errMsg.append("药品HIS剂型代码有误").append(";");
            }
            try {
                if (StringUtils.isNotEmpty(getStrFromCell(cells.get(36)))) {
                    drug.setLicenseNumber(getStrFromCell(cells.get(36)));
                }
            } catch (Exception e) {
                LOGGER.error("药品国药准字有误 ," + e.getMessage(), e);
                errMsg.append("药品国药准字有误").append(";");
            }
            try {
                if (!StringUtils.isEmpty(getStrFromCell(cells.get(37)))) {
                    String strFromCell = getStrFromCell(cells.get(37));
                    StringBuilder ss = new StringBuilder();
                    String[] split = strFromCell.split(",");
                    for (int i = 0; i < split.length; i++) {
                        String applyBusiness = "";
                        if ("药品处方".equals(split[i])) {
                            applyBusiness = "1";
                        } else if ("诊疗处方".equals(split[i])) {
                            applyBusiness = "2";
                        } else {
                            break;
                        }
                        if (i != split.length - 1) {
                            ss.append(applyBusiness + ",");
                        } else {
                            ss.append(applyBusiness);
                        }
                    }
                    drug.setApplyBusiness(ss.toString());
                } else {
                    errMsg.append("【适用业务】未填写").append(";");
                }
            } catch (Exception e) {
                LOGGER.error("适用业务有误," + e.getMessage(), e);
                errMsg.append("适用业务有误").append(";");
            }

            try {
                if (StringUtils.isNotEmpty(getStrFromCell(cells.get(38)))) {
                    if (("是").equals(getStrFromCell(cells.get(38)))) {
                        drug.setTargetedDrugType(1);
                    } else if (("否").equals(getStrFromCell(cells.get(38)))) {
                        drug.setTargetedDrugType(0);
                    } else {
                        errMsg.append("是否靶向药格式错误").append(";");
                    }
                }
            } catch (Exception e) {
                LOGGER.error("是否靶向药有误 ," + e.getMessage(), e);
                errMsg.append("是否靶向药有误").append(";");
            }

            try {
                if (StringUtils.isNotEmpty(getStrFromCell(cells.get(39)))) {
                    drug.setSmallestSaleMultiple(Integer.parseInt(getStrFromCell(cells.get(39)).trim()));
                }
            } catch (Exception e) {
                LOGGER.error("SmallestSaleMultiple ," + e.getMessage(), e);
                errMsg.append("SmallestSaleMultiple").append(";");
            }

            if (!ObjectUtils.isEmpty(organId)) {
                DrugSourcesDAO dao = DAOFactory.getDAO(DrugSourcesDAO.class);
                List<DrugSources> byDrugSourcesId = dao.findByDrugSourcesId(organId);
                if (ObjectUtils.isEmpty(byDrugSourcesId)) {
                    OrganService bean = AppDomainContext.getBean("basic.organService", OrganService.class);
                    OrganDTO byOrganId = bean.getByOrganId(organId);
                    DrugSources saveData = new DrugSources();
                    saveData.setDrugSourcesId(byOrganId.getOrganId());
                    saveData.setDrugSourcesName(byOrganId.getName());
                    DrugSources save = dao.save(saveData);
                    drug.setSourceOrgan(save.getDrugSourcesId());
                } else {
                    drug.setSourceOrgan(organId);
                }
            }
            drug.setStatus(DrugMatchConstant.UNMATCH);
            drug.setOperator(operator);
            drug.setDrugSource(0);
            if (errMsg.length() > 1) {
                int showNum = rowIndex + 1;
                String error = ("【第" + showNum + "行】" + errMsg.substring(0, errMsg.length() - 1) + "\n");
                errDrugListMatchList.add(error);
                failNum++;
            } else {
                try {
                    drugToolService.AutoMatch(drug);
                    boolean isSuccess = drugListMatchDAO.updateData(drug);
                    if (!isSuccess) {
                        //自动匹配功能暂无法提供
                        DrugListMatch save = drugListMatchDAO.save(drug);
                        try {
                            drugToolService.automaticDrugMatch(save, operator);
                        } catch (Exception e) {
                            LOGGER.error("readDrugExcel.updateMatchAutomatic fail,", e);
                        }
                        addNum++;
                    } else {
                        List<DrugListMatch> dataByOrganDrugCode = drugListMatchDAO.findDataByOrganDrugCodenew(drug.getOrganDrugCode(), drug.getSourceOrgan());
                        if (dataByOrganDrugCode != null && dataByOrganDrugCode.size() > 0) {
                            for (DrugListMatch drugListMatch : dataByOrganDrugCode) {
                                try {
                                    drugToolService.automaticDrugMatch(drugListMatch, operator);
                                    drugListMatch.setStatus(DrugMatchConstant.ALREADY_MATCH);
                                    drugListMatchDAO.updateData(drugListMatch);
                                } catch (Exception e) {
                                    LOGGER.error("readDrugExcel.updateMatchAutomatic fail,", e);
                                }
                            }
                        }
                        updateNum++;
                    }
                } catch (Exception e) {
                    LOGGER.error("save or update drugListMatch error " + e.getMessage(), e);
                }
            }


            rowIndex++;

        }



        return drug;
    }

    /**
     * 获取单元格值（字符串）
     *
     * @param str
     * @return
     */
    private String getStrFromCell(String str) {
        if(StringUtils.isEmpty(str)){
            return null;
        }
        if (str != null) {
            str = str.trim();
            if (StringUtils.isEmpty(str)) {
                str = null;
            }
        }
        return str;
    }


}

