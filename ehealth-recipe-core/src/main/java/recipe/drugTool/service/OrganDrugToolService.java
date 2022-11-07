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
import ctd.util.FileAuth;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import recipe.aop.LogRecord;
import recipe.bussutil.ExcelUtil;
import recipe.constant.DrugMatchConstant;
import recipe.dao.*;
import recipe.manager.RecipeManager;
import recipe.third.IFileDownloadService;
import recipe.thread.RecipeBusiThreadPool;
import recipe.vo.greenroom.ImportDrugRecordVO;

import javax.annotation.Resource;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * liumin
 * 机构药品业务处理类
 */
@RpcBean(value = "organDrugToolService")
public class OrganDrugToolService implements IOrganDrugToolService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DrugToolService.class);

    @Resource
    private ImportDrugRecordDAO importDrugRecordDAO;

    @Resource
    private ImportDrugRecordMsgDAO importDrugRecordMsgDAO;

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

    @Autowired
    private RecipeManager  recipeManager;

    private static final String SUFFIX_2003 = ".xls";
    private static final String SUFFIX_2007 = ".xlsx";

    @Override
    @LogRecord
    public Map<String, Object> readDrugExcel(Integer id) {
        Map<String, Object> result = Maps.newHashMap();
        result.put("code", 200);
        RecipeBusiThreadPool.execute(() -> {
            ImportDrugRecord importDrugRecord=importDrugRecordDAO.get(id);
            boolean checkResult=checkReadDrugExcel(importDrugRecord);
            String fileId=importDrugRecord.getFileId();
            if(!checkResult){return ;}
            String operator=importDrugRecord.getImportOperator();
            // TODO 数据处理?? 为么取不到
//        String filePath=recipeManager.getRecipeSignFileUrl(fileId,3600);
//        LOGGER.info("filePath:{}",filePath);
            //行下标
            AtomicReference<Integer> rowIndex= new AtomicReference<>(0);
            //总条数
            AtomicReference<Integer> total = new AtomicReference<>(0);
            ExcelUtil excelUtil = new ExcelUtil(cells -> {
                //输出每一行的内容
                LOGGER.info("rowIndex:{},cells:{}",rowIndex,cells);
                DrugListMatch drug = new DrugListMatch();
                StringBuilder validMsg = new StringBuilder();
                obtainDrugListMatchFromReadExcel(cells, rowIndex.get(),importDrugRecord,drug,validMsg);
                if (validMsg.length() > 1) {
                    failProcess(importDrugRecord,rowIndex,validMsg);
                } else {
                    successProcess(importDrugRecord,rowIndex,drug,operator);
                }
                rowIndex.getAndSet(rowIndex.get() + 1);
                total.getAndSet(total.get() + 1);
            });

            try {
                excelUtil.load( new ByteArrayInputStream(fileDownloadService.downloadAsByte(fileId))).parse();
            } catch (Exception e) {
                e.printStackTrace();
            }
            //导入记录更新
            importDrugRecord.setFailNum(total.get()<1?0:total.get()-1 - importDrugRecord.getAddNum() - importDrugRecord.getUpdateNum() - importDrugRecord.getBankNumber());
            if(importDrugRecord.getFailNum()>0){
                importDrugRecord.setStatus(3);
            }else{
                importDrugRecord.setFailNum(0);
                importDrugRecord.setStatus(1);
            }
            importDrugRecordDAO.update(importDrugRecord);
            LOGGER.info(operator + "结束 readDrugExcel 方法" + System.currentTimeMillis() + "当前进程=" + Thread.currentThread().getName());
        });
        LOGGER.info("result={}",JSONUtils.toString(result));
        return result;
    }

    /**
     * 数据校验
     * @param importDrugRecord
     * @param
     */
    private boolean checkReadDrugExcel(ImportDrugRecord importDrugRecord) {
        Map<String, Object> result = Maps.newHashMap();
        if(importDrugRecord==null){
            saveImportErr("导入失败，原因是文件不存在",importDrugRecord);
            return false;
        }
        String operator=importDrugRecord.getImportOperator();
        String fileId=importDrugRecord.getFileId();
        String fileName=importDrugRecord.getFileName();
        LOGGER.info(operator + "开始 readDrugExcel 方法" + System.currentTimeMillis() + "当前进程=" + Thread.currentThread().getName());
        if (fileName.endsWith(SUFFIX_2003)) {
            LOGGER.info("readDrugExcel SUFFIX_2003");
        } else if (fileName.endsWith(SUFFIX_2007)) {
            LOGGER.info("readDrugExcel SUFFIX_2007");
        } else {
            saveImportErr("上传文件格式有问题",importDrugRecord);
            return false;
        }
        if (StringUtils.isEmpty(operator)) {
            saveImportErr("operator is required",importDrugRecord);
            return false;
        }
        return true;
    }

    /**
     * 成功处理
     * @param importDrugRecord
     * @param rowIndex
     * @param drug
     * @param operator
     */
    private void successProcess(ImportDrugRecord importDrugRecord, AtomicReference<Integer> rowIndex, DrugListMatch drug, String operator) {
        if(new Integer("0").equals(rowIndex.get()) || drug==null ||StringUtils.isEmpty(drug.getOrganDrugCode()))return;
        try {
            drugToolService.AutoMatch(drug);
            boolean isUpdateSuccess=drugListMatchDAO.updateDrugListMatch(drug);
            if(isUpdateSuccess){
                //更新
                List<DrugListMatch> drugListMatchesDb = drugListMatchDAO.findDataByOrganDrugCodenew(drug.getOrganDrugCode(), drug.getSourceOrgan());
                for (DrugListMatch drugListMatch : drugListMatchesDb) {
                    try {
                        drugToolService.automaticDrugMatch(drugListMatch, operator);
                        drugListMatch.setStatus(DrugMatchConstant.ALREADY_MATCH);
                        drugListMatchDAO.updateDrugListMatch(drugListMatch);
                        importDrugRecord.setUpdateNum(importDrugRecord.getUpdateNum()+1);
                    } catch (Exception e) {

                        LOGGER.error("readDrugExcel.updateMatchAutomatic fail,", e);
                    }
                }
            }else{
                //新增
                try {
                    if(StringUtils.isEmpty(drug.getOrganDrugCode())){
                        return;
                    }
                    DrugListMatch save = drugListMatchDAO.save(drug);
                    drugToolService.automaticDrugMatch(save, operator);
                } catch (Exception e) {
                    LOGGER.error("readDrugExcel.updateMatchAutomatic fail,", e);
                }
                importDrugRecord.setAddNum(importDrugRecord.getAddNum()+1);
            }

        } catch (Exception e) {
            LOGGER.error("save or update drugListMatch error " + e.getMessage(), e);
        }
    }

    /**
     * 失败处理
     * @param importDrugRecord
     * @param rowIndex
     * @param validMsg
     */
    private void failProcess(ImportDrugRecord importDrugRecord, AtomicReference<Integer> rowIndex, StringBuilder validMsg) {
        ImportDrugRecordMsg importDrugRecordMsg=new ImportDrugRecordMsg();
        Integer errorLocaction=rowIndex.get()+1;
        importDrugRecordMsg.setErrLocaction("第" + errorLocaction + "行");
        importDrugRecordMsg.setErrMsg(validMsg.substring(0, validMsg.length() - 1) );
        importDrugRecordMsg.setImportDrugRecordId(importDrugRecord.getRecordId());
        importDrugRecordMsgDAO.save(importDrugRecordMsg);
        importDrugRecord.setFailNum(importDrugRecord.getFailNum()+1);
    }

    /**
     *
     * @param errMsg
     * @param importDrugRecord
     */
    private void saveImportErr(String errMsg, ImportDrugRecord importDrugRecord) {
        ImportDrugRecordMsg importDrugRecordMsg=new ImportDrugRecordMsg();
        importDrugRecordMsg.setErrMsg(errMsg );
        importDrugRecordMsg.setImportDrugRecordId(importDrugRecord.getRecordId());
        importDrugRecordMsgDAO.save(importDrugRecordMsg);
        importDrugRecord.setStatus(3);
        importDrugRecordDAO.save(importDrugRecord);
    }

    @Override
    public Integer saveImportDrugRecord(ImportDrugRecordVO param) {
        ImportDrugRecord importDrugRecord= ObjectCopyUtils.convert(param,ImportDrugRecord.class);
        importDrugRecord=importDrugRecordDAO.save(importDrugRecord);
        return importDrugRecord.getRecordId();
    }

    /**
     * 数据校验并将表格行数据转换成DrugListMatch
     * @param cells
     * @param
     * @param rowIndex
     * @return
     */
    public DrugListMatch obtainDrugListMatchFromReadExcel(List<String>  cells,int rowIndex
            ,ImportDrugRecord importDrugRecord,DrugListMatch drug,StringBuilder validMsg ){
        Integer organId=importDrugRecord.getOrganId();
        String operator=importDrugRecord.getImportOperator();

        //循环获得每个行
        boolean flag = false;
        if(null != cells){
            for(String cell : cells){
                if (!StringUtils.isEmpty(cell) ) {
                    //有数据
                    flag = true;
                    break;
                }
            }
        }
        //如果为空行的数据，则空行数+1
        if(!flag) {
            importDrugRecord.setBankNumber(importDrugRecord.getBankNumber()+1);
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
                    validMsg.append("模板有误，请确认！").append(";");
                    return drug;
                }

            }
            /*try{*/
            try {
                if (StringUtils.isNotEmpty(getStrFromCell(cells.get(0)))) {
                    drug.setRegulationDrugCode(getStrFromCell(cells.get(0)));
                }
            } catch (Exception e) {
                LOGGER.error("监管药品编码," + e.getMessage(), e);
                validMsg.append("监管药品编码有误").append(";");
            }

            try {
                if (StringUtils.isEmpty(getStrFromCell(cells.get(2)))) {
                    validMsg.append("机构唯一索引不能为空").append(";");
                }
                drug.setOrganDrugCode(getStrFromCell(cells.get(2)));
            } catch (Exception e) {
                LOGGER.error("机构唯一索引有误 ," + e.getMessage(), e);
                validMsg.append("机构唯一索引有误").append(";");
            }


            try {
                if (StringUtils.isNotEmpty(getStrFromCell(cells.get(3)))) {
                    drug.setDrugItemCode(getStrFromCell(cells.get(3)));
                }
            } catch (Exception e) {
                LOGGER.error("药品编码 ," + e.getMessage(), e);
                validMsg.append("药品编码").append(";");
            }


            try {
                if (StringUtils.isNotEmpty(getStrFromCell(cells.get(1)))) {
                    OrganDrugList byDrugIdAndOrganId = organDrugListDAO.getByDrugIdAndOrganId(Integer.parseInt(getStrFromCell(cells.get(1)).trim()), organId);
                    if (!StringUtils.isEmpty(getStrFromCell(cells.get(2)))) {
                        if (!ObjectUtils.isEmpty(byDrugIdAndOrganId)) {
                            if (!byDrugIdAndOrganId.getOrganDrugCode().equals(getStrFromCell(cells.get(2)))) {
                                validMsg.append("机构已存在药品关联该平台药品").append(";");
                            }
                        }
                    }
                    drug.setPlatformDrugId(Integer.parseInt(getStrFromCell(cells.get(1)).trim()));
                }
            } catch (Exception e) {
                LOGGER.error("平台药品编码有误 ," + e.getMessage(), e);
                validMsg.append("平台药品编码有误").append(";");
            }

            try {
                if (StringUtils.isEmpty(getStrFromCell(cells.get(4)))) {
                    validMsg.append("【药品名】未填写").append(";");
                }
                drug.setDrugName(getStrFromCell(cells.get(4)));
            } catch (Exception e) {
                LOGGER.error("药品名有误 ," + e.getMessage(), e);
                validMsg.append("药品名有误").append(";");
            }
            try {
                if (StringUtils.isEmpty(getStrFromCell(cells.get(5)))) {
                    validMsg.append("【商品名】未填写").append(";");
                }
                drug.setSaleName(getStrFromCell(cells.get(5)));
            } catch (Exception e) {
                LOGGER.error("商品名有误 ," + e.getMessage(), e);
                validMsg.append("商品名有误").append(";");
            }

            try {
                if (StringUtils.isNotEmpty(getStrFromCell(cells.get(6)))) {
                    drug.setChemicalName(getStrFromCell(cells.get(6)));
                }
            } catch (Exception e) {
                LOGGER.error("药品化学名有误," + e.getMessage(), e);
                validMsg.append("药品化学名有误").append(";");
            }

            try {
                if (StringUtils.isEmpty(getStrFromCell(cells.get(7)))) {
                    validMsg.append("【院内检索码】未填写").append(";");
                }
                drug.setRetrievalCode(getStrFromCell(cells.get(7)));
            } catch (Exception e) {
                LOGGER.error("药品院内检索码有误 ," + e.getMessage(), e);
                validMsg.append("药品院内检索码有误").append(";");
            }
           /* try {
                if (StringUtils.isNotEmpty(getStrFromCell(cells.get(7)))) {
                    if ("有效".equals(getStrFromCell(cells.get(7)).trim())){
                        drug.setStatus(1);
                    }
                }
            } catch (Exception e) {
                LOGGER.error("药品状态有误 ," + e.getMessage(), e);
                validMsg.append("药品状态有误").append(";");
            }*/
            try {
                if (StringUtils.isEmpty(getStrFromCell(cells.get(9)))) {
                    validMsg.append("【药品类型】未填写").append(";");
                }
                if (("中药").equals(getStrFromCell(cells.get(9)))) {
                    drug.setDrugType(3);
                } else if (("中成药").equals(getStrFromCell(cells.get(9)))) {
                    drug.setDrugType(2);
                } else if (("西药").equals(getStrFromCell(cells.get(9)))) {
                    drug.setDrugType(1);
                } else {
                    validMsg.append("药品类型格式错误").append(";");
                }
            } catch (Exception e) {
                LOGGER.error("药品类型有误 ," + e.getMessage(), e);
                validMsg.append("药品类型有误").append(";");
            }
            try {
                if (!StringUtils.isEmpty(getStrFromCell(cells.get(10)))) {
                    drug.setDrugForm(getStrFromCell(cells.get(10)));
                }
            } catch (Exception e) {
                LOGGER.error("药品剂型有误 ," + e.getMessage(), e);
                validMsg.append("药品剂型有误").append(";");
            }
            try {
                if (StringUtils.isEmpty(getStrFromCell(cells.get(11)))) {
                    validMsg.append("【药品规格/单位】未填写").append(";");
                }
                drug.setDrugSpec(getStrFromCell(cells.get(11)));
            } catch (Exception e) {
                LOGGER.error("药品规格/单位有误 ," + e.getMessage(), e);
                validMsg.append("药品规格/单位有误").append(";");
            }
            try {
                if (StringUtils.isNotEmpty(getStrFromCell(cells.get(12)))) {
                    drug.setUseDoseSmallestUnit(getStrFromCell(cells.get(12)));
                }
            } catch (Exception e) {
                LOGGER.error("药品最小单位有误 ," + e.getMessage(), e);
                validMsg.append("药品最小单位有误").append(";");
            }
            try {
                if (StringUtils.isEmpty(getStrFromCell(cells.get(13)))) {
                    //中药不需要设置
                    if (!(new Integer(3).equals(drug.getDrugType()))) {
                        validMsg.append("【包装数量(转换系数)】未填写").append(";");
                    } else {
                        drug.setPack(1);
                    }
                } else {
                    drug.setPack(Integer.parseInt(getStrFromCell(cells.get(13))));
                }
            } catch (Exception e) {
                LOGGER.error("包装数量(转换系数)有误 ," + e.getMessage(), e);
                validMsg.append("包装数量(转换系数)有误").append(";");
            }
            try {
                if (StringUtils.isEmpty(getStrFromCell(cells.get(14)))) {
                    //中药不需要设置
                    if (!(new Integer(3).equals(drug.getDrugType()))) {
                        validMsg.append("【单次剂量(规格单位)】未填写").append(";");
                    }

                } else {
                    drug.setUseDose(Double.parseDouble(getStrFromCell(cells.get(14))));
                }
            } catch (Exception e) {
                LOGGER.error("单次剂量(规格单位)有误 ," + e.getMessage(), e);
                validMsg.append("单次剂量(规格单位)有误").append(";");
            }
            try {
                if (StringUtils.isEmpty(getStrFromCell(cells.get(15)))) {
                    //中药不需要设置
                    if (!(new Integer(3).equals(drug.getDrugType()))) {
                        validMsg.append("【规格单位】未填写").append(";");
                    }

                } else {
                    drug.setUseDoseUnit(getStrFromCell(cells.get(15)));
                }
            } catch (Exception e) {
                LOGGER.error("规格单位有误 ," + e.getMessage(), e);
                validMsg.append("规格单位有误").append(";");
            }
            try {
                if (StringUtils.isNotEmpty(getStrFromCell(cells.get(16)))) {
                    drug.setRecommendedUseDose(Double.parseDouble(getStrFromCell(cells.get(16))));
                }
            } catch (Exception e) {
                LOGGER.error("默认单次剂量(规格单位)有误 ," + e.getMessage(), e);
                validMsg.append("默认单次剂量(规格单位)有误").append(";");
            }
            try {
                if (StringUtils.isEmpty(getStrFromCell(cells.get(17)))) {
                    //中药不需要设置
                    if (!(new Integer(3).equals(drug.getDrugType()))) {
                        validMsg.append("【包装单位】未填写").append(";");
                    }
                }
                drug.setUnit(getStrFromCell(cells.get(17)));
            } catch (Exception e) {
                LOGGER.error("药品包装单位有误 ," + e.getMessage(), e);
                validMsg.append("药品包装单位有误").append(";");
            }
            try {
                if (StringUtils.isEmpty(getStrFromCell(cells.get(18)))) {
                    validMsg.append("【单价】未填写").append(";");
                }
                String priceCell = getStrFromCell(cells.get(18));
                drug.setPrice(new BigDecimal(priceCell));
            } catch (Exception e) {
                LOGGER.error("药品单价有误 ," + e.getMessage(), e);
                validMsg.append("药品单价有误").append(";");
            }
            try {
                if (StringUtils.isEmpty(getStrFromCell(cells.get(19)))) {
                    //中药不需要设置
                    if (!(new Integer(3).equals(drug.getDrugType()))) {
                        validMsg.append("【生产厂家】未填写").append(";");
                    }
                }
                drug.setProducer(getStrFromCell(cells.get(19)));
            } catch (Exception e) {
                LOGGER.error("药品生产厂家有误 ," + e.getMessage(), e);
                validMsg.append("药品生产厂家有误").append(";");
            }
            try {
                if (StringUtils.isNotEmpty(getStrFromCell(cells.get(20)))) {
                    drug.setDrugManfCode(getStrFromCell(cells.get(20)));
                }
            } catch (Exception e) {
                LOGGER.error("药品产地编码有误 ," + e.getMessage(), e);
                validMsg.append("药品产地编码有误").append(";");
            }
            try {
                if (getStrFromCell(cells.get(21)) != null) {
                    String strFromCell = getStrFromCell(cells.get(21));
                    StringBuilder ss = new StringBuilder();
                    String[] split = strFromCell.split(",");
                    for (int i = 0; i < split.length; i++) {
                        Integer idByPharmacyName = pharmacyTcmDAO.getIdByPharmacyNameAndOrganId(split[i], organId);
                        if (idByPharmacyName == null) {
                            validMsg.append("药房名称有误").append(";");
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
                            validMsg.append("平台未找到该配送药企" + split[i] + "").append(";");
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
                            validMsg.append("平台未找到该用药频次").append(";");
                        } else {
                            drug.setUsingRateId(usingRateDTO.getId().toString());
                        }

                    } else {
                        UsingRateDTO usingRateDTOByOrganAndKey = bean.findUsingRateDTOByOrganAndKey(organId, getStrFromCell(cells.get(23)));
                        if (ObjectUtils.isEmpty(usingRateDTOByOrganAndKey)) {
                            validMsg.append("机构未找到该用药频次").append(";");
                        } else {
                            drug.setUsingRateId(usingRateDTOByOrganAndKey.getId().toString());
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.error("用药频次有误 ," + e.getMessage(), e);
                validMsg.append("用药频次有误").append(";");
            }

            try {
                if (StringUtils.isNotEmpty(getStrFromCell(cells.get(25)))) {
                    IUsePathwaysService bean = AppContextHolder.getBean("basic.usePathwaysService", IUsePathwaysService.class);
                    List<UsePathwaysDTO> allUsePathwaysByOrganId = bean.findAllUsePathwaysByOrganId(organId);
                    if (ObjectUtils.isEmpty(allUsePathwaysByOrganId)) {
                        UsePathwaysDTO usePathwaysDTO = bean.findUsePathwaysByOrganAndKey(0, getStrFromCell(cells.get(25)));
                        if (ObjectUtils.isEmpty(usePathwaysDTO)) {
                            validMsg.append("平台未找到该用药途径").append(";");
                        } else {
                            drug.setUsePathwaysId(usePathwaysDTO.getId().toString());
                        }

                    } else {
                        UsePathwaysDTO usePathwaysDTO = bean.findUsePathwaysByOrganAndKey(organId, getStrFromCell(cells.get(25)));
                        if (ObjectUtils.isEmpty(usePathwaysDTO)) {
                            validMsg.append("机构未找到该用药途径").append(";");
                        } else {
                            drug.setUsePathwaysId(usePathwaysDTO.getId().toString());
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.error("用药途径有误 ," + e.getMessage(), e);
                validMsg.append("用药途径有误").append(";");
            }


            try {
                if (!StringUtils.isEmpty(getStrFromCell(cells.get(27)))) {
                    if ((new Integer(3).equals(drug.getDrugType()))) {
                        DrugEntrust byOrganIdAndDrugEntrustName = drugEntrustDAO.getByOrganIdAndDrugEntrustName(organId, getStrFromCell(cells.get(27)));
                        if (byOrganIdAndDrugEntrustName != null) {
                            drug.setDrugEntrust(byOrganIdAndDrugEntrustName.getDrugEntrustName());
                        } else {
                            validMsg.append("中药药品字典未找到该嘱托").append(";");
                        }
                    } else {
                        drug.setDrugEntrust(getStrFromCell(cells.get(27)));
                    }
                }
            } catch (Exception e) {
                LOGGER.error("药品嘱托有误 ," + e.getMessage(), e);
                validMsg.append("药品嘱托有误").append(";");
            }
            try {
                if (StringUtils.isNotEmpty(getStrFromCell(cells.get(28)))) {
                    drug.setIndicationsDeclare(getStrFromCell(cells.get(28)));
                }
            } catch (Exception e) {
                LOGGER.error("药品适应症说明有误 ," + e.getMessage(), e);
                validMsg.append("药品适应症说明有误").append(";");
            }
            try {
                if (StringUtils.isNotEmpty(getStrFromCell(cells.get(29)))) {
                    drug.setPackingMaterials(getStrFromCell(cells.get(29)));
                }
            } catch (Exception e) {
                LOGGER.error("药品包装材料有误 ," + e.getMessage(), e);
                validMsg.append("药品包装材料有误").append(";");
            }

            //中药不需要设置
            if (!(new Integer(3).equals(drug.getDrugType()))) {
                try {
                    if (("是").equals(getStrFromCell(cells.get(30)))) {
                        drug.setBaseDrug(1);
                    } else if (("否").equals(getStrFromCell(cells.get(30)))) {
                        drug.setBaseDrug(0);
                    } else {
                        drug.setBaseDrug(0);
//                        validMsg.append("是否基药格式不正确").append(";");
                    }

                } catch (Exception e) {
                    LOGGER.error("是否基药有误 ," + e.getMessage(), e);
                    validMsg.append("是否基药有误").append(";");
                }
            }else{
                if (("是").equals(getStrFromCell(cells.get(30)))) {
                    drug.setBaseDrug(1);
                } else if (("否").equals(getStrFromCell(cells.get(30)))) {
                    drug.setBaseDrug(0);
                }
            }
            try {
                if (!StringUtils.isEmpty(getStrFromCell(cells.get(31)))) {
                    if (("是").equals(getStrFromCell(cells.get(31)))) {
                        drug.setMedicalInsuranceControl(true);
                    } else if (("否").equals(getStrFromCell(cells.get(31)))) {
                        drug.setMedicalInsuranceControl(false);
                    } else {
                        validMsg.append("医保控制格式错误").append(";");
                    }
                }
            } catch (Exception e) {
                LOGGER.error("医保控制有误 ," + e.getMessage(), e);
                validMsg.append("医保控制有误").append(";");
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
                        validMsg.append("单复方格式错误").append(";");
                    }
                }
            } catch (Exception e) {
                LOGGER.error("单复方有误 ," + e.getMessage(), e);
                validMsg.append("单复方有误").append(";");
            }

            try {
                if (StringUtils.isNotEmpty(getStrFromCell(cells.get(33)))) {
                    drug.setMedicalDrugCode(getStrFromCell(cells.get(33)));
                }
            } catch (Exception e) {
                LOGGER.error("药品医保药品编码有误 ," + e.getMessage(), e);
                validMsg.append("药品医保药品编码有误").append(";");
            }
            try {
                if (StringUtils.isNotEmpty(getStrFromCell(cells.get(34)))) {
                    drug.setMedicalDrugFormCode(getStrFromCell(cells.get(34)));
                }
            } catch (Exception e) {
                LOGGER.error("药品医保剂型代码有误 ," + e.getMessage(), e);
                validMsg.append("药品医保剂型代码有误").append(";");
            }
            try {
                if (StringUtils.isNotEmpty(getStrFromCell(cells.get(35)))) {
                    drug.setHisFormCode(getStrFromCell(cells.get(35)));
                }
            } catch (Exception e) {
                LOGGER.error("药品HIS剂型代码有误 ," + e.getMessage(), e);
                validMsg.append("药品HIS剂型代码有误").append(";");
            }
            try {
                if (StringUtils.isNotEmpty(getStrFromCell(cells.get(36)))) {
                    drug.setLicenseNumber(getStrFromCell(cells.get(36)));
                }
            } catch (Exception e) {
                LOGGER.error("药品国药准字有误 ," + e.getMessage(), e);
                validMsg.append("药品国药准字有误").append(";");
            }
            try {
                if (!StringUtils.isEmpty(getStrFromCell(cells.get(37)))) {
                    String strFromCell = getStrFromCell(cells.get(37));
                    StringBuilder ss = new StringBuilder();
                    //对中文逗号进行校验
                    if(!strFromCell.contains("，")){
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
                    }else{
                        validMsg.append("适用业务有误").append(";");
                    }
                } else {
                    validMsg.append("【适用业务】未填写").append(";");
                }
            } catch (Exception e) {
                LOGGER.error("适用业务有误," + e.getMessage(), e);
                validMsg.append("适用业务有误").append(";");
            }

            try {
                if (StringUtils.isNotEmpty(getStrFromCell(cells.get(38)))) {
                    if (("是").equals(getStrFromCell(cells.get(38)))) {
                        drug.setTargetedDrugType(1);
                    } else if (("否").equals(getStrFromCell(cells.get(38)))) {
                        drug.setTargetedDrugType(0);
                    } else {
                        validMsg.append("是否靶向药格式错误").append(";");
                    }
                }
            } catch (Exception e) {
                LOGGER.error("是否靶向药有误 ," + e.getMessage(), e);
                validMsg.append("是否靶向药有误").append(";");
            }
            //TODO 最后一个字段空列问题
            try {
                if (StringUtils.isNotEmpty(getStrFromCell(cells.get(39)))) {
                    drug.setSmallestSaleMultiple(Integer.parseInt(getStrFromCell(cells.get(39)).trim()));
                }
            } catch (Exception e) {
                LOGGER.error("最小销售倍数有误 ," + e.getMessage(), e);
                validMsg.append("最小销售倍数有误").append(";");
            }

            try {
                if (StringUtils.isNotEmpty(getStrFromCell(cells.get(40)))) {
                    if (("是").equals(getStrFromCell(cells.get(40)))) {
                        drug.setUnavailable(1);
                    } else if (("否").equals(getStrFromCell(cells.get(40)))) {
                        drug.setUnavailable(0);
                    } else {
                        validMsg.append("不可在线开具有误").append(";");
                    }
                }else{
                    drug.setUnavailable(0);
                }
            } catch (Exception e) {
                LOGGER.error("不可在线开具有误," + e.getMessage(), e);
                validMsg.append("不可在线开具有误").append(";");
            }

            try {
                if (StringUtils.isNotEmpty(getStrFromCell(cells.get(41)))) {
                    if (("是").equals(getStrFromCell(cells.get(41)))) {
                        drug.setPsychotropicDrugFlag(1);
                    } else if (("否").equals(getStrFromCell(cells.get(41)))) {
                        drug.setPsychotropicDrugFlag(0);
                    } else {
                        validMsg.append("是否精神药物格式错误").append(";");
                    }
                }
            } catch (Exception e) {
                LOGGER.error("是否精神药物有误 ," + e.getMessage(), e);
                validMsg.append("是否精神药物有误").append(";");
            }

            try {
                if (StringUtils.isNotEmpty(getStrFromCell(cells.get(42)))) {
                    if (("是").equals(getStrFromCell(cells.get(42)))) {
                        drug.setNarcoticDrugFlag(1);
                    } else if (("否").equals(getStrFromCell(cells.get(42)))) {
                        drug.setNarcoticDrugFlag(0);
                    } else {
                        validMsg.append("是否麻醉药物格式错误").append(";");
                    }
                }
            } catch (Exception e) {
                LOGGER.error("是否麻醉药物有误 ," + e.getMessage(), e);
                validMsg.append("是否麻醉药物有误").append(";");
            }

            try {
                if (StringUtils.isNotEmpty(getStrFromCell(cells.get(43)))) {
                    if (("是").equals(getStrFromCell(cells.get(43)))) {
                        drug.setToxicDrugFlag(1);
                    } else if (("否").equals(getStrFromCell(cells.get(43)))) {
                        drug.setToxicDrugFlag(0);
                    } else {
                        validMsg.append("是否毒性药物格式错误").append(";");
                    }
                }
            } catch (Exception e) {
                LOGGER.error("是否毒性药物有误 ," + e.getMessage(), e);
                validMsg.append("是否毒性药物有误").append(";");
            }

            try {
                if (StringUtils.isNotEmpty(getStrFromCell(cells.get(44)))) {
                    if (("是").equals(getStrFromCell(cells.get(44)))) {
                        drug.setRadioActivityDrugFlag(1);
                    } else if (("否").equals(getStrFromCell(cells.get(44)))) {
                        drug.setRadioActivityDrugFlag(0);
                    } else {
                        validMsg.append("是否放射性药物格式错误").append(";");
                    }
                }
            } catch (Exception e) {
                LOGGER.error("是否放射性药物有误 ," + e.getMessage(), e);
                validMsg.append("是否放射性药物有误").append(";");
            }

            try {
                if (StringUtils.isNotEmpty(getStrFromCell(cells.get(45)))) {
                    if (("是").equals(getStrFromCell(cells.get(45)))) {
                        drug.setSpecialUseAntibioticDrugFlag(1);
                    } else if (("否").equals(getStrFromCell(cells.get(45)))) {
                        drug.setSpecialUseAntibioticDrugFlag(0);
                    } else {
                        validMsg.append("是否特殊使用级抗生素药物格式错误").append(";");
                    }
                }
            } catch (Exception e) {
                LOGGER.error("是否特殊使用级抗生素药物有误 ," + e.getMessage(), e);
                validMsg.append("是否特殊使用级抗生素药物有误").append(";");
            }

            try {
                if (StringUtils.isNotEmpty(getStrFromCell(cells.get(46)))) {
                    if (("无").equals(getStrFromCell(cells.get(46)))) {
                        drug.setAntibioticsDrugLevel(0);
                    } else if (("1级").equals(getStrFromCell(cells.get(46)))) {
                        drug.setAntibioticsDrugLevel(1);
                    } else if (("2级").equals(getStrFromCell(cells.get(46)))) {
                        drug.setAntibioticsDrugLevel(2);
                    } else if (("3级").equals(getStrFromCell(cells.get(46)))) {
                        drug.setAntibioticsDrugLevel(3);
                    } else {
                        validMsg.append("抗菌素药物等级格式错误").append(";");
                    }
                }
            } catch (Exception e) {
                LOGGER.error("抗菌素药物等级有误 ," + e.getMessage(), e);
                validMsg.append("抗菌素药物等级有误").append(";");
            }

            try {
                if (StringUtils.isNotEmpty(getStrFromCell(cells.get(47)))) {
                    if (("是").equals(getStrFromCell(cells.get(47)))) {
                        drug.setAntiTumorDrugFlag(1);
                    } else if (("否").equals(getStrFromCell(cells.get(47)))) {
                        drug.setAntiTumorDrugFlag(0);
                    } else {
                        validMsg.append("是否抗肿瘤药物格式错误").append(";");
                    }
                }
            } catch (Exception e) {
                LOGGER.error("是否抗肿瘤药物有误 ," + e.getMessage(), e);
                validMsg.append("是否抗肿瘤药物有误").append(";");
            }

            try {
                if(new Integer(1).equals(drug.getAntiTumorDrugFlag()) && StringUtils.isEmpty(getStrFromCell(cells.get(48)))){
                        validMsg.append("抗肿瘤药物等级未填写").append(";");
                }else{
                    if (StringUtils.isNotEmpty(getStrFromCell(cells.get(48)))) {
                        if (("普通级").equals(getStrFromCell(cells.get(48)))) {
                            drug.setAntiTumorDrugLevel(1);
                        } else if (("限制级").equals(getStrFromCell(cells.get(48)))) {
                            drug.setAntiTumorDrugLevel(2);
                        } else {
                            validMsg.append("抗肿瘤药物等级格式错误").append(";");
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.error("抗肿瘤药物等级有误 ," + e.getMessage(), e);
                validMsg.append("抗肿瘤药物等级有误").append(";");
            }

            try {
                if (StringUtils.isNotEmpty(getStrFromCell(cells.get(49)))) {
                    drug.setUnitHisCode(getStrFromCell(cells.get(49)));
                }
            } catch (Exception e) {
                LOGGER.error("最小售药单位HIS编码有误 ," + e.getMessage(), e);
                validMsg.append("最小售药单位HIS编码有误").append(";");
            }

            try {
                if (StringUtils.isNotEmpty(getStrFromCell(cells.get(50)))) {
                    drug.setUseDoseUnitHisCode(getStrFromCell(cells.get(50)));
                }
            } catch (Exception e) {
                LOGGER.error("规格单位HIS编码有误 ," + e.getMessage(), e);
                validMsg.append("规格单位HIS编码有误").append(";");
            }

            try {
                if (StringUtils.isNotEmpty(getStrFromCell(cells.get(51)))) {
                    drug.setUseDoseSmallestUnitHisCode(getStrFromCell(cells.get(51)));
                }
            } catch (Exception e) {
                LOGGER.error("药品最小规格包装单位HIS编码有误 ," + e.getMessage(), e);
                validMsg.append("药品最小规格包装单位HIS编码有误").append(";");
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

