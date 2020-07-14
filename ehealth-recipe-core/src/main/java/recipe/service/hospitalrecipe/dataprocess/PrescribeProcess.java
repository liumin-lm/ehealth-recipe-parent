package recipe.service.hospitalrecipe.dataprocess;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.ngari.base.property.service.IConfigurationCenterUtilsService;
import com.ngari.recipe.entity.DrugList;
import com.ngari.recipe.entity.OrganDrugList;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeOrder;
import com.ngari.recipe.hisprescription.model.HospitalDrugDTO;
import com.ngari.recipe.hisprescription.model.HospitalRecipeDTO;
import com.ngari.recipe.recipe.model.RecipeBean;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import ctd.persistence.DAOFactory;
import ctd.util.JSONUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.ApplicationUtils;
import recipe.bussutil.UsePathwaysFilter;
import recipe.bussutil.UsingRateFilter;
import recipe.constant.PayConstant;
import recipe.constant.RecipeBussConstant;
import recipe.constant.RecipeSystemConstant;
import recipe.constant.ReviewTypeConstant;
import recipe.dao.DrugListDAO;
import recipe.dao.OrganDrugListDAO;
import recipe.dao.RecipeOrderDAO;
import recipe.util.DateConversion;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author： 0184/yu_yun
 * @date： 2018/3/14
 * @description：
 * @version： 1.0
 */
public class PrescribeProcess {

    /**
     * logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(PrescribeProcess.class);

    /**
     * 转换处方数据
     *
     * @param hospitalRecipeDTO
     * @return
     */
    public static void convertNgariRecipe(RecipeBean recipe, HospitalRecipeDTO hospitalRecipeDTO) {
        recipe.setClinicId((StringUtils.isEmpty(hospitalRecipeDTO.getClinicId()) ?
                null : Integer.parseInt(hospitalRecipeDTO.getClinicId())));
        recipe.setPatientID(hospitalRecipeDTO.getPatientNumber());
        recipe.setRecipeCode(hospitalRecipeDTO.getRecipeCode());
        recipe.setRecipeType(Integer.parseInt(hospitalRecipeDTO.getRecipeType()));
        recipe.setDoctorName(hospitalRecipeDTO.getDoctorName());
        Date createDate = DateConversion.parseDate(hospitalRecipeDTO.getCreateDate(), DateConversion.DEFAULT_DATE_TIME);
        recipe.setSignDate(createDate);
        // 医院审核时间
        recipe.setCheckDate(createDate);
        recipe.setCreateDate(createDate);
        recipe.setLastModify(createDate);
        recipe.setTotalMoney(StringUtils.isEmpty(hospitalRecipeDTO.getRecipeFee()) ?
                BigDecimal.ZERO : new BigDecimal(hospitalRecipeDTO.getRecipeFee()));
        recipe.setActualPrice(StringUtils.isEmpty(hospitalRecipeDTO.getActualFee()) ?
                BigDecimal.ZERO : new BigDecimal(hospitalRecipeDTO.getActualFee()));
        // 多个诊断按“|”分隔
        recipe.setOrganDiseaseId(hospitalRecipeDTO.getOrganDiseaseId().replaceAll("\\|", RecipeSystemConstant.ORGAN_DISEASE_SPLIT));
        recipe.setOrganDiseaseName(hospitalRecipeDTO.getOrganDiseaseName().replaceAll("\\|", RecipeSystemConstant.ORGAN_DISEASE_SPLIT));
        recipe.setRecipeMemo(hospitalRecipeDTO.getRecipeMemo());
        recipe.setMemo(hospitalRecipeDTO.getMemo());
        //处方审核信息
        if (StringUtils.isNotEmpty(hospitalRecipeDTO.getCheckerNumber())) {
            recipe.setCheckOrgan(StringUtils.isEmpty(hospitalRecipeDTO.getCheckOrgan()) ?
                    null : Integer.parseInt(hospitalRecipeDTO.getCheckOrgan()));
            recipe.setCheckerTel(hospitalRecipeDTO.getCheckerTel());
            recipe.setCheckFailMemo(hospitalRecipeDTO.getCheckFailMemo());
            recipe.setSupplementaryMemo(hospitalRecipeDTO.getSupplementaryMemo());
            if (StringUtils.isNotEmpty(hospitalRecipeDTO.getCheckDate())) {
                Date checkDate = DateConversion.parseDate(hospitalRecipeDTO.getCheckDate(),
                        DateConversion.DEFAULT_DATE_TIME);
                recipe.setCheckDate(checkDate);
                recipe.setCheckDateYs(checkDate);
            }

        }

        recipe.setPayMode(StringUtils.isEmpty(hospitalRecipeDTO.getPayMode()) ?
                null : Integer.parseInt(hospitalRecipeDTO.getPayMode()));
        recipe.setGiveMode(StringUtils.isEmpty(hospitalRecipeDTO.getGiveMode()) ?
                null : Integer.parseInt(hospitalRecipeDTO.getGiveMode()));
        recipe.setGiveUser(hospitalRecipeDTO.getGiveUser());
        recipe.setPayFlag(StringUtils.isEmpty(hospitalRecipeDTO.getPayFlag()) ?
                PayConstant.PAY_FLAG_NOT_PAY : Integer.valueOf(hospitalRecipeDTO.getPayFlag()));

        recipe.setStatus(Integer.valueOf(hospitalRecipeDTO.getStatus()));
        recipe.setMedicalPayFlag(Integer.parseInt(hospitalRecipeDTO.getMedicalPayFlag()));
        recipe.setDistributionFlag(Integer.parseInt(hospitalRecipeDTO.getDistributionFlag()));
        if (1 == recipe.getDistributionFlag()) {
            recipe.setTakeMedicine(1);
        } else {
            recipe.setTakeMedicine(0);
        }
        //中药处理
        recipe.setCopyNum(StringUtils.isEmpty(hospitalRecipeDTO.getTcmNum()) ?
                1 : Integer.parseInt(hospitalRecipeDTO.getTcmNum()));
        //过期时间
        recipe.setValueDays(3);
        recipe.setPushFlag(0);
        recipe.setRemindFlag(0);
        recipe.setGiveFlag(0);
        recipe.setChooseFlag(0);
        recipe.setRecipeMode(RecipeBussConstant.RECIPEMODE_NGARIHEALTH);
        //设置运营平台设置的审方模式
        if (recipe.getReviewType() == null){
            try {
                IConfigurationCenterUtilsService configurationService = ApplicationUtils.getBaseService(IConfigurationCenterUtilsService.class);
                Integer reviewType = (Integer)configurationService.getConfiguration(recipe.getClinicOrgan(), "reviewType");
                if (reviewType == null){
                    //默认审方后置
                    recipe.setReviewType(ReviewTypeConstant.Postposition_Check);
                }else {
                    recipe.setReviewType(reviewType);
                }
            }catch (Exception e){
                LOG.error("获取运营平台审方方式配置异常",e);
                //默认审方后置
                recipe.setReviewType(ReviewTypeConstant.Postposition_Check);
            }
        }
    }

    /**
     * 转换详情数据
     *
     * @param hospitalRecipeDTO
     * @return
     */
    public static List<RecipeDetailBean> convertNgariDetail(HospitalRecipeDTO hospitalRecipeDTO) {
        OrganDrugListDAO organDrugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);
        DrugListDAO drugListDAO = DAOFactory.getDAO(DrugListDAO.class);

        //数据校验已在开始处理
        List<HospitalDrugDTO> hosDetailList = hospitalRecipeDTO.getDrugList();
        List<String> organDrugCodeList = Lists.newArrayList(Collections2.transform(hosDetailList, new Function<HospitalDrugDTO, String>() {
            @Nullable
            @Override
            public String apply(@Nullable HospitalDrugDTO input) {
                return input.getDrugCode();
            }
        }));

        List<RecipeDetailBean> recipeDetails = new ArrayList<>(hosDetailList.size());

        Integer clinicOrgan = Integer.parseInt(hospitalRecipeDTO.getClinicOrgan());
        //从base_organdruglist获取平台药品ID数据
        List<OrganDrugList> organDrugList = organDrugListDAO.findByOrganIdAndDrugCodes(clinicOrgan, organDrugCodeList);
        if (CollectionUtils.isNotEmpty(organDrugList) && organDrugCodeList.size() == organDrugList.size()) {
            //某机构内，药品编码与药品ID关系
            Map<Integer, String> codeIdRel = Maps.newHashMap();
            for (OrganDrugList organDrug : organDrugList) {
                codeIdRel.put(organDrug.getDrugId(), organDrug.getOrganDrugCode());
            }

            //从base_druglist获取药品具体数据
            List<DrugList> drugList = drugListDAO.findByDrugIds(Lists.newArrayList(codeIdRel.keySet()));
            if (CollectionUtils.isNotEmpty(drugList) && drugList.size() == organDrugCodeList.size()) {
                //某机构内，药品编码与药品详情关系
                Map<String, DrugList> drugRel = Maps.newHashMap();
                for (DrugList drug : drugList) {
                    drugRel.put(codeIdRel.get(drug.getDrugId()), drug);
                }
                //处理药品数据
                doRecipeDetail(drugRel,hospitalRecipeDTO,recipeDetails,hosDetailList);
            } else {
                LOG.warn("convertNgariDetail drugList数据与医院不匹配. organDrugCode={}, drugList.size={}",
                        JSONUtils.toString(organDrugCodeList), drugList.size());
            }
        } else {
            LOG.warn("convertNgariDetail 存在未录入或未打开的药品. organDrugCode={}, organDrugList.size={}",
                    JSONUtils.toString(organDrugCodeList), organDrugList.size());
        }

        return recipeDetails;
    }

    private static void doRecipeDetail(Map<String, DrugList> drugRel, HospitalRecipeDTO hospitalRecipeDTO, List<RecipeDetailBean> recipeDetails,List<HospitalDrugDTO> hosDetailList) {
        Integer recipeType = Integer.parseInt(hospitalRecipeDTO.getRecipeType());
        boolean isTcmType = (RecipeBussConstant.RECIPETYPE_TCM.equals(recipeType)
                || RecipeBussConstant.RECIPETYPE_HP.equals(recipeType)) ? true : false;

        Integer clinicOrgan = Integer.parseInt(hospitalRecipeDTO.getClinicOrgan());
        RecipeDetailBean recipedetail;
        DrugList drug;
        String usingRate;
        String usePathways;
        Date now = DateTime.now().toDate();
        for (HospitalDrugDTO hosDetail : hosDetailList) {
            drug = drugRel.get(hosDetail.getDrugCode());
            if (null != drug) {
                recipedetail = new RecipeDetailBean();
                recipedetail.setCreateDt(now);
                recipedetail.setLastModify(now);
                recipedetail.setDrugId(drug.getDrugId());
                recipedetail.setOrganDrugCode(hosDetail.getDrugCode());
                recipedetail.setDrugName(StringUtils.defaultString(hosDetail.getDrugName(),
                        drug.getSaleName()));
                recipedetail.setDrugSpec(StringUtils.defaultString(hosDetail.getSpecification(),
                        drug.getDrugSpec()));
                recipedetail.setUseTotalDose(StringUtils.isEmpty(hosDetail.getTotal()) ?
                        1 : Double.parseDouble(hosDetail.getTotal()));
                recipedetail.setUseDose(StringUtils.isEmpty(hosDetail.getUseDose()) ?
                        drug.getUseDose() : Double.parseDouble(hosDetail.getUseDose()));
                recipedetail.setUseDays(StringUtils.isEmpty(hosDetail.getUesDays()) ?
                        1 : Integer.parseInt(hosDetail.getUesDays()));
                //设置平台药品数据
                recipedetail.setPack(drug.getPack());
                recipedetail.setDrugUnit(drug.getUnit());
                recipedetail.setUseDoseUnit(drug.getUseDoseUnit());
                recipedetail.setDefaultUseDose(drug.getUseDose());
                recipedetail.setPharmNo(hosDetail.getPharmNo());
                recipedetail.setMemo(hosDetail.getMemo());
                recipedetail.setStatus(1);
                //中药或者膏方
                if (isTcmType) {
                    usingRate = UsingRateFilter.filter(clinicOrgan, hospitalRecipeDTO.getTcmUsingRate());
                    usePathways = UsePathwaysFilter.filter(clinicOrgan, hospitalRecipeDTO.getTcmUsePathways());
                    //用法
                    recipedetail.setUsePathways(StringUtils.defaultString(usePathways, drug.getUsePathways()));
                    //频次
                    recipedetail.setUsingRate(StringUtils.defaultString(usingRate, drug.getUsingRate()));
                } else {
                    //西药等如果医院不做处理需要进行字典转换
                    usingRate = UsingRateFilter.filter(clinicOrgan, hosDetail.getUsingRate());
                    usePathways = UsePathwaysFilter.filter(clinicOrgan, hosDetail.getUsePathways());
                    //用法
                    recipedetail.setUsePathways(StringUtils.defaultString(usePathways, drug.getUsePathways()));
                    //频次
                    recipedetail.setUsingRate(StringUtils.defaultString(usingRate, drug.getUsingRate()));
                }

                //设置价格
                recipedetail.setSalePrice(StringUtils.isEmpty(hosDetail.getDrugFee()) ?
                        BigDecimal.ZERO : new BigDecimal(hosDetail.getDrugFee()));
                recipedetail.setDrugCost(StringUtils.isEmpty(hosDetail.getDrugTotalFee()) ?
                        BigDecimal.ZERO : new BigDecimal(hosDetail.getDrugTotalFee()));

                //date 202000601
                //设置处方用药天数字符类型
                if(StringUtils.isEmpty(recipedetail.getUseDaysB())){

                    recipedetail.setUseDaysB(null != recipedetail.getUseDays() ? recipedetail.getUseDays().toString() : "0");

                }

                recipeDetails.add(recipedetail);
            }
        }
    }

    /**
     * 将平台处方转成医院处方格式
     *
     * @param dbRecipe
     * @return
     */
    public static HospitalRecipeDTO convertHospitalRecipe(Recipe dbRecipe) {
        HospitalRecipeDTO hospitalRecipeDTO = new HospitalRecipeDTO();
        hospitalRecipeDTO.setStatus(dbRecipe.getStatus().toString());
        hospitalRecipeDTO.setPatientName(dbRecipe.getPatientName());
        hospitalRecipeDTO.setDoctorName(dbRecipe.getDoctorName());
        hospitalRecipeDTO.setRecipeCode(dbRecipe.getRecipeCode());
        hospitalRecipeDTO.setOrganDiseaseId(dbRecipe.getOrganDiseaseId());
        hospitalRecipeDTO.setOrganDiseaseName(dbRecipe.getOrganDiseaseName());
        hospitalRecipeDTO.setPatientNumber(dbRecipe.getPatientID());
        hospitalRecipeDTO.setRecipeType(dbRecipe.getRecipeType().toString());
        hospitalRecipeDTO.setMemo(dbRecipe.getMemo());
        hospitalRecipeDTO.setRecipeMemo(dbRecipe.getRecipeMemo());

        //订单信息
        if (StringUtils.isNotEmpty(dbRecipe.getRecipeCode())) {
            RecipeOrderDAO orderDAO = DAOFactory.getDAO(RecipeOrderDAO.class);
            RecipeOrder order = orderDAO.getByOrderCode(dbRecipe.getOrderCode());
            if (null != order) {
                hospitalRecipeDTO.setActualFee(order.getActualPrice().toString());
                hospitalRecipeDTO.setCouponFee(order.getCouponFee().toPlainString());
                hospitalRecipeDTO.setDecoctionFee(order.getDecoctionFee().toPlainString());
                hospitalRecipeDTO.setExpressFee(order.getExpressFee().toPlainString());
                hospitalRecipeDTO.setOrderTotalFee(order.getTotalFee().toPlainString());
                hospitalRecipeDTO.setMedicalFee("0");
                hospitalRecipeDTO.setRecipeFee(order.getRecipeFee().toPlainString());

            }

        }

        return hospitalRecipeDTO;
    }

    //转换详情数据（武昌传的是平台drugId）
    public static List<RecipeDetailBean> convertNgariDetailForWuChang(HospitalRecipeDTO hospitalRecipeDTO) {
        DrugListDAO drugListDAO = DAOFactory.getDAO(DrugListDAO.class);
        //his传的药品数据
        List<HospitalDrugDTO> hosDetailList = hospitalRecipeDTO.getDrugList();
        List<RecipeDetailBean> recipeDetails = new ArrayList<>(hosDetailList.size());

        //从base_druglist获取药品具体数据
        List<Integer> list = hosDetailList.stream().map(HospitalDrugDTO::getDrugCode).map(Integer::parseInt).collect(Collectors.toList());
        List<DrugList> drugList = drugListDAO.findByDrugIds(list);
        //drugId -> DrugList
        Map<String, DrugList> drugRel = Maps.newHashMap();
        for (DrugList drug : drugList) {
            drugRel.put(drug.getDrugId().toString(), drug);
        }

        doRecipeDetail(drugRel,hospitalRecipeDTO,recipeDetails,hosDetailList);
        return recipeDetails;
    }
}
