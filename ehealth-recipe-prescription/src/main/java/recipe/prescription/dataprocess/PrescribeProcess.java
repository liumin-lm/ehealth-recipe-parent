package recipe.prescription.dataprocess;

import com.google.common.collect.Maps;
import com.ngari.recipe.entity.DrugList;
import com.ngari.recipe.entity.OrganDrugList;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.Recipedetail;
import ctd.persistence.DAOFactory;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import recipe.common.CommonConstant;
import recipe.common.ResponseUtils;
import recipe.dao.DrugListDAO;
import recipe.dao.OrganDrugListDAO;
import recipe.prescription.PrescribeService;
import recipe.prescription.bean.HosRecipeResult;
import recipe.prescription.bean.HospitalDrugDTO;
import recipe.prescription.bean.HospitalRecipeDTO;
import recipe.util.DateConversion;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author： 0184/yu_yun
 * @date： 2018/3/14
 * @description：
 * @version： 1.0
 */
public class PrescribeProcess {

    /**
     * 支付标识 已支付 1
     */
    private static final String ISPAY_TURE = "1";

    public static Recipe convertNgariRecipe(HospitalRecipeDTO hospitalRecipeDTO) {
        Recipe recipe = new Recipe();
        //此处接收到的是上级医院的处方，需要设置处方来源信息
        recipe.setOriginClinicOrgan(Integer.parseInt(hospitalRecipeDTO.getClinicOrgan()));
        recipe.setClinicOrgan(Integer.parseInt(hospitalRecipeDTO.getClinicOrgan()));
        recipe.setRecipeType(Integer.parseInt(hospitalRecipeDTO.getRecipeType()));
        recipe.setOriginRecipeCode(hospitalRecipeDTO.getRecipeCode());
        Date createDate = DateConversion.parseDate(hospitalRecipeDTO.getCreateDate(), DateConversion.DEFAULT_DATE_TIME);
        recipe.setSignDate(createDate);
        recipe.setCreateDate(createDate);
        recipe.setLastModify(createDate);
        recipe.setOrganDiseaseId(hospitalRecipeDTO.getOrganDiseaseId());
        recipe.setOrganDiseaseName(hospitalRecipeDTO.getOrganDiseaseName());
        recipe.setRecipeMemo(hospitalRecipeDTO.getRecipeMemo());

        recipe.setPayMode(Integer.parseInt(hospitalRecipeDTO.getPayMode()));
        recipe.setGiveMode(Integer.parseInt(hospitalRecipeDTO.getGiveMode()));

        if (StringUtils.isEmpty(hospitalRecipeDTO.getPayFlag())) {
            recipe.setPayFlag(0);
        } else {
            if (ISPAY_TURE.equals(hospitalRecipeDTO.getPayFlag())) {
                recipe.setPayFlag(1);
                recipe.setPayDate(createDate);
            } else {
                recipe.setPayFlag(0);
            }
        }
        recipe.setChooseFlag(1);
        //从医院HIS获取的处方
        recipe.setFromflag(0);
        return recipe;
    }

    public static List<Recipedetail> convertNgariDetail(HospitalRecipeDTO hospitalRecipeDTO) {
        OrganDrugListDAO organDrugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);
        DrugListDAO drugListDAO = DAOFactory.getDAO(DrugListDAO.class);

        List<HospitalDrugDTO> hosDetailList = hospitalRecipeDTO.getDrugList();
        List<Recipedetail> recipeDetails = new ArrayList<>(hosDetailList.size());
        List<String> organDrugCode = new ArrayList<>(hosDetailList.size());
        for (HospitalDrugDTO hosDetail : hosDetailList) {
            organDrugCode.add(hosDetail.getDrugCode());
        }
        //从base_organdruglist获取平台药品ID数据
        List<OrganDrugList> organDrugList = organDrugListDAO.findByOrganIdAndDrugCodes(
                Integer.parseInt(hospitalRecipeDTO.getClinicOrgan()), organDrugCode);
        if (CollectionUtils.isNotEmpty(organDrugList)) {
            List<Integer> drugIds = new ArrayList<>(organDrugList.size());
            //某机构内，药品编码与药品ID关系
            Map<Integer, String> codeIdRel = Maps.newHashMap();
            //某机构内，药品编码与药品单价关系
            Map<String, BigDecimal> codePriceRel = Maps.newHashMap();
            for (OrganDrugList organDrug : organDrugList) {
                drugIds.add(organDrug.getDrugId());
                codeIdRel.put(organDrug.getDrugId(), organDrug.getOrganDrugCode());
                codePriceRel.put(organDrug.getOrganDrugCode(), organDrug.getSalePrice());
            }

            if (CollectionUtils.isNotEmpty(drugIds)) {
                List<OrganDrugList> subOrganDrugList = organDrugListDAO.findByOrganIdAndDrugIds(
                        Integer.parseInt(hospitalRecipeDTO.getClinicOrgan()), drugIds);
                if (drugIds.size() != subOrganDrugList.size()) {
                    return recipeDetails;
                }

                //从base_druglist获取药品具体数据
                List<DrugList> drugList = drugListDAO.findByDrugIds(drugIds);
                if (CollectionUtils.isNotEmpty(drugList)) {
                    //某机构内，药品编码与药品详情关系
                    Map<String, DrugList> drugRel = Maps.newHashMap();
                    for (DrugList drug : drugList) {
                        String organDrugCode1 = codeIdRel.get(drug.getDrugId());
                        if (StringUtils.isNotEmpty(organDrugCode1)) {
                            drugRel.put(organDrugCode1, drug);
                        }
                    }

                    Recipedetail recipedetail;
                    DrugList drug;
                    Date now = DateTime.now().toDate();
                    for (HospitalDrugDTO hosDetail : hosDetailList) {
                        drug = drugRel.get(hosDetail.getDrugCode());
                        if (null != drug) {
                            recipedetail = new Recipedetail();
                            recipedetail.setStatus(1);
                            recipedetail.setCreateDt(now);
                            recipedetail.setLastModify(now);
                            recipedetail.setDrugId(drug.getDrugId());
                            recipedetail.setOrganDrugCode(hosDetail.getDrugCode());
                            recipedetail.setMemo(hosDetail.getMemo());
//                            recipedetail.setDrugName(StringUtils.defaultString("", drug.getDrugName()));
//                            recipedetail.setDrugSpec(StringUtils.defaultString(hosDetail.getDrmodel(), drug.getDrugSpec()));
//                            recipedetail.setDrugUnit(StringUtils.defaultString(hosDetail.getPackUnit(), drug.getUnit()));
//                            recipedetail.setUseDoseUnit(StringUtils.defaultString(hosDetail.getDrunit(), drug.getUseDoseUnit()));
//                            recipedetail.setDosageUnit(hosDetail.getDosageUnit());
//                            recipedetail.setDefaultUseDose(drug.getUseDose());
//                            recipedetail.setUseDose((null == hosDetail.getDosage()) ? drug.getUseDose() : Double.parseDouble(hosDetail.getDosage()));
//                            recipedetail.setUseDays((null == hosDetail.getUseDays()) ? 1 : Integer.parseInt(hosDetail.getUseDays()));
//                            recipedetail.setUseTotalDose((null == hosDetail.getDosageTotal()) ? 1 : Double.parseDouble(hosDetail.getDosageTotal()));
//                            recipedetail.setPack((null == hosDetail.getPack()) ? drug.getPack() : Integer.parseInt(hosDetail.getPack()));
//                            //用法
//                            recipedetail.setUsePathways(StringUtils.defaultString(hosDetail.getAdmission(), drug.getUsePathways()));
//                            //频次
//                            recipedetail.setUsingRate(StringUtils.defaultString(hosDetail.getFrequency(), drug.getUsingRate()));
//
//                            //设置价格
//                            BigDecimal salePrice = codePriceRel.get(hosDetail.getDrcode());
//                            if (null != salePrice) {
//                                recipedetail.setSalePrice(salePrice);
//                                BigDecimal drugCost = salePrice.multiply(new BigDecimal(recipedetail.getUseTotalDose()))
//                                        .divide(BigDecimal.ONE, 3, RoundingMode.UP);
//                                recipedetail.setDrugCost(drugCost);
//                            }
//
//                            recipeDetails.add(recipedetail);
                        }
                    }
                }
            }
        }

        return recipeDetails;
    }

//    public static PatientBean convertNgariPatient() {
//        PatientBean patient = new PatientBean();
//        patient.setPatientName(this.patientName);
//        patient.setIdcard(this.certID);
//        if (GUARDIAN_FLAG.equals(this.guardianFlag)) {
//            //监护人模式
//            patient.setGuardianFlag(true);
//            patient.setGuardianName(this.guardianName);
//            patient.setBirthday(DateConversion.parseDate(this.patientBirthDay, DateConversion.YYYY_MM_DD));
//            patient.setPatientSex(this.getPatientSex());
//        } else {
//            patient.setGuardianFlag(false);
//            //设置患者生日，性别
//            try {
//                String idCard18 = ChinaIDNumberUtil.convert15To18(this.certID);
//                patient.setBirthday(ChinaIDNumberUtil.getBirthFromIDNumber(idCard18));
//                patient.setPatientSex(ChinaIDNumberUtil.getSexFromIDNumber(idCard18));
//            } catch (ValidateException e) {
//                e.printStackTrace();
//            }
//        }
//        patient.setMobile(this.mobile);
//        if (StringUtils.isNotEmpty(this.cardNo)) {
//            HealthCardBean card = new HealthCardBean();
//            //卡类型（1医院就诊卡  2医保卡 3医院病历号）
//            //卡类型字典需要对照
//            if (CARD_TYPE_YBK.equals(this.cardType)) {
//                //医保卡
//                card.setCardType("2");
//            } else {
//                //就诊卡
//                card.setCardType("3");
//            }
//            card.setCardId(this.cardNo);
//            card.setCardOrgan(Integer.parseInt(this.subOrganId));
//            patient.setHealthCards(Collections.singletonList(card));
//            //默认自费
//            patient.setPatientType("1");
//        }
//
//        return patient;
//    }

    /**
     * 校验医院处方信息
     *
     * @param obj 医院处方
     * @return 结果
     */
    public static HosRecipeResult validateHospitalRecipe(HospitalRecipeDTO recipe, int flag) {
        HosRecipeResult result = ResponseUtils.getFailResponse(HosRecipeResult.class, null);
        if (PrescribeService.ADD_FLAG == flag) {
            //新增
            if (StringUtils.isEmpty(recipe.getRecipeCode())) {
                result.setMsg("处方编号为空");
                return result;
            }

            if (StringUtils.isEmpty(recipe.getRecipeType())) {
                result.setMsg("处方类型为空");
                return result;
            }

            if (StringUtils.isEmpty(recipe.getClinicOrgan())) {
                result.setMsg("开方机构为空");
                return result;
            }

            if (StringUtils.isEmpty(recipe.getDoctorNumber())) {
                result.setMsg("开方医生工号为空");
                return result;
            }

            if (StringUtils.isEmpty(recipe.getCertificate())) {
                result.setMsg("患者身份证信息为空");
                return result;
            }

            if (CollectionUtils.isEmpty(recipe.getDrugList())) {
                result.setMsg("处方详情数据为空");
                return result;
            }
        }

        result.setCode(CommonConstant.SUCCESS);
        return result;
    }
}
