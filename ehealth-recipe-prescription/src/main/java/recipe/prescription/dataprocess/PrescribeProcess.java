package recipe.prescription.dataprocess;

import com.google.common.collect.Maps;
import com.ngari.recipe.entity.DrugList;
import com.ngari.recipe.entity.OrganDrugList;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.Recipedetail;
import ctd.persistence.DAOFactory;
import ctd.util.JSONUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.common.CommonConstant;
import recipe.common.ResponseUtils;
import recipe.constant.RecipeBussConstant;
import recipe.constant.RecipeStatusConstant;
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
     * logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(PrescribeProcess.class);

    /**
     * 支付标识 已支付 1
     */
    private static final String ISPAY_TURE = "1";

    /**
     * 转换处方数据
     *
     * @param hospitalRecipeDTO
     * @return
     */
    public static Recipe convertNgariRecipe(HospitalRecipeDTO hospitalRecipeDTO) {
        try {
            Recipe recipe = new Recipe();
            recipe.setClinicId((StringUtils.isEmpty(hospitalRecipeDTO.getClinicId()) ?
                    null : Integer.parseInt(hospitalRecipeDTO.getClinicId())));
            recipe.setPatientName(hospitalRecipeDTO.getPatientName());
            recipe.setDoctorName(hospitalRecipeDTO.getDoctorName());
            recipe.setPatientID(hospitalRecipeDTO.getPatientNumber());
//        recipe.setOriginClinicOrgan(Integer.parseInt(hospitalRecipeDTO.getClinicOrgan()));
            recipe.setClinicOrgan(Integer.parseInt(hospitalRecipeDTO.getClinicOrgan()));
            recipe.setRecipeType(Integer.parseInt(hospitalRecipeDTO.getRecipeType()));
//        recipe.setOriginRecipeCode(hospitalRecipeDTO.getRecipeCode());
            recipe.setRecipeCode(hospitalRecipeDTO.getRecipeCode());
            Date createDate = DateConversion.parseDate(hospitalRecipeDTO.getCreateDate(), DateConversion.DEFAULT_DATE_TIME);
            recipe.setSignDate(createDate);
            recipe.setCreateDate(createDate);
            recipe.setLastModify(createDate);
            recipe.setOrganDiseaseId(hospitalRecipeDTO.getOrganDiseaseId());
            recipe.setOrganDiseaseName(hospitalRecipeDTO.getOrganDiseaseName());
            recipe.setRecipeMemo(hospitalRecipeDTO.getRecipeMemo());
            recipe.setMemo(hospitalRecipeDTO.getMemo());
            recipe.setTotalMoney(StringUtils.isEmpty(hospitalRecipeDTO.getRecipeFee()) ?
                    BigDecimal.ZERO : new BigDecimal(hospitalRecipeDTO.getRecipeFee()));
            recipe.setActualPrice(StringUtils.isEmpty(hospitalRecipeDTO.getRecipeFee()) ?
                    BigDecimal.ZERO : new BigDecimal(hospitalRecipeDTO.getRecipeFee()));

            //处方审核信息
            if (StringUtils.isNotEmpty(hospitalRecipeDTO.getCheckerNumber())) {
                recipe.setCheckOrgan(StringUtils.isEmpty(hospitalRecipeDTO.getCheckOrgan()) ?
                        null : Integer.parseInt(hospitalRecipeDTO.getCheckOrgan()));
                recipe.setCheckerTel(hospitalRecipeDTO.getCheckerTel());
                recipe.setCheckFailMemo(hospitalRecipeDTO.getCheckFailMemo());
                recipe.setSupplementaryMemo(hospitalRecipeDTO.getSupplementaryMemo());
                if (StringUtils.isEmpty(hospitalRecipeDTO.getCheckDate())) {
                    recipe.setCheckDate(createDate);
                    recipe.setCheckDateYs(createDate);
                } else {
                    try {
                        Date checkDate = DateConversion.parseDate(hospitalRecipeDTO.getCheckDate(),
                                DateConversion.DEFAULT_DATE_TIME);
                        recipe.setCheckDate(checkDate);
                        recipe.setCheckDateYs(checkDate);
                    } catch (Exception e) {
                        recipe.setCheckDate(createDate);
                        recipe.setCheckDateYs(createDate);
                    }
                }
            }

            recipe.setPayMode(StringUtils.isEmpty(hospitalRecipeDTO.getPayMode()) ?
                    RecipeBussConstant.PAYMODE_TO_HOS : Integer.parseInt(hospitalRecipeDTO.getPayMode()));
            recipe.setGiveMode(StringUtils.isEmpty(hospitalRecipeDTO.getGiveMode()) ?
                    RecipeBussConstant.GIVEMODE_TO_HOS : Integer.parseInt(hospitalRecipeDTO.getGiveMode()));
            recipe.setGiveUser(hospitalRecipeDTO.getGiveUser());

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
            recipe.setMedicalPayFlag(StringUtils.isEmpty(hospitalRecipeDTO.getMedicalPayFlag()) ?
                    0 : Integer.parseInt(hospitalRecipeDTO.getMedicalPayFlag()));
            recipe.setDistributionFlag(StringUtils.isEmpty(hospitalRecipeDTO.getDistributionFlag()) ?
                    1 : Integer.parseInt(hospitalRecipeDTO.getDistributionFlag()));
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
            recipe.setStatus(StringUtils.isEmpty(hospitalRecipeDTO.getStatus()) ?
                    RecipeStatusConstant.CHECK_PASS : Integer.parseInt(hospitalRecipeDTO.getStatus()));
            return recipe;
        } catch (Exception e) {
            LOG.warn("convertNgariRecipe error. recipeCode={}", hospitalRecipeDTO.getRecipeCode(), e);
        }

        return null;
    }

    /**
     * 转换详情数据
     *
     * @param hospitalRecipeDTO
     * @return
     */
    public static List<Recipedetail> convertNgariDetail(HospitalRecipeDTO hospitalRecipeDTO) {
        OrganDrugListDAO organDrugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);
        DrugListDAO drugListDAO = DAOFactory.getDAO(DrugListDAO.class);

        List<HospitalDrugDTO> hosDetailList = hospitalRecipeDTO.getDrugList();
        List<Recipedetail> recipeDetails = new ArrayList<>(hosDetailList.size());
        List<String> organDrugCode = new ArrayList<>(hosDetailList.size());
        for (HospitalDrugDTO hosDetail : hosDetailList) {
            if (StringUtils.isEmpty(hosDetail.getDrugCode())) {
                LOG.warn("convertNgariDetail 存在药品编码为空的数据. hosDetail={}", JSONUtils.toString(hosDetail));
                organDrugCode.clear();
                break;
            }
            organDrugCode.add(hosDetail.getDrugCode());
        }

        if (CollectionUtils.isEmpty(organDrugCode)) {
            return recipeDetails;
        }
        Integer recipeType = Integer.parseInt(hospitalRecipeDTO.getRecipeType());
        boolean isTcmType = (RecipeBussConstant.RECIPETYPE_TCM.equals(recipeType)
                || RecipeBussConstant.RECIPETYPE_HP.equals(recipeType)) ? true : false;

        //从base_organdruglist获取平台药品ID数据
        List<OrganDrugList> organDrugList = organDrugListDAO.findByOrganIdAndDrugCodes(
                Integer.parseInt(hospitalRecipeDTO.getClinicOrgan()), organDrugCode);
        if (CollectionUtils.isNotEmpty(organDrugList) && organDrugCode.size() == organDrugList.size()) {
            if (CollectionUtils.isNotEmpty(organDrugList)) {
                List<Integer> drugIds = new ArrayList<>(organDrugList.size());
                //某机构内，药品编码与药品ID关系
                Map<Integer, String> codeIdRel = Maps.newHashMap();
                for (OrganDrugList organDrug : organDrugList) {
                    drugIds.add(organDrug.getDrugId());
                    codeIdRel.put(organDrug.getDrugId(), organDrug.getOrganDrugCode());
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
                            recipedetail.setDrugSpec(drug.getDrugSpec());
                            recipedetail.setDrugUnit(drug.getUnit());
                            recipedetail.setUseDoseUnit(drug.getUseDoseUnit());
                            recipedetail.setDefaultUseDose(drug.getUseDose());
                            recipedetail.setPack(drug.getPack());
                            recipedetail.setOrganDrugCode(hosDetail.getDrugCode());
                            recipedetail.setPharmNo(hosDetail.getPharmNo());
                            recipedetail.setMemo(hosDetail.getMemo());
                            recipedetail.setDrugName(StringUtils.defaultString(hosDetail.getDrugName(),
                                    drug.getSaleName()));
                            recipedetail.setUseDose(StringUtils.isEmpty(hosDetail.getUseDose()) ?
                                    drug.getUseDose() : Double.parseDouble(hosDetail.getUseDose()));
                            recipedetail.setUseDays(StringUtils.isEmpty(hosDetail.getUesDays()) ?
                                    1 : Integer.parseInt(hosDetail.getUesDays()));
                            recipedetail.setUseTotalDose(StringUtils.isEmpty(hosDetail.getTotal()) ?
                                    1 : Double.parseDouble(hosDetail.getTotal()));

                            //中药或者膏方
                            if (isTcmType) {
                                //用法
                                recipedetail.setUsePathways(StringUtils.defaultString(hospitalRecipeDTO.getTcmUsePathways(),
                                        drug.getUsePathways()));
                                //频次
                                recipedetail.setUsingRate(StringUtils.defaultString(hospitalRecipeDTO.getTcmUsingRate(),
                                        drug.getUsingRate()));
                            } else {
                                //用法
                                recipedetail.setUsePathways(StringUtils.defaultString(hosDetail.getUsePathways(),
                                        drug.getUsePathways()));
                                //频次
                                recipedetail.setUsingRate(StringUtils.defaultString(hosDetail.getUsingRate(),
                                        drug.getUsingRate()));
                            }

                            //设置价格
                            recipedetail.setSalePrice(StringUtils.isEmpty(hosDetail.getDrugFee()) ?
                                    BigDecimal.ZERO : new BigDecimal(hosDetail.getDrugFee()));
                            recipedetail.setDrugCost(StringUtils.isEmpty(hosDetail.getDrugTotalFee()) ?
                                    BigDecimal.ZERO : new BigDecimal(hosDetail.getDrugTotalFee()));

                            recipeDetails.add(recipedetail);
                        }
                    }
                }
            }
        } else {
            LOG.warn("convertNgariDetail 存在未录入或未打开的药品. organDrugCode={}, organDrugList.size={}",
                    JSONUtils.toString(organDrugCode), organDrugList.size());
            return recipeDetails;
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

            if (StringUtils.isEmpty(recipe.getDoctorName())) {
                result.setMsg("开方医生名字为空");
                return result;
            }

            if (StringUtils.isEmpty(recipe.getCertificate())) {
                result.setMsg("患者身份证信息为空");
                return result;
            }

            if (StringUtils.isEmpty(recipe.getPatientName())) {
                result.setMsg("患者名字为空");
                return result;
            }

            if (CollectionUtils.isEmpty(recipe.getDrugList())) {
                result.setMsg("处方详情数据为空");
                return result;
            }

            if (StringUtils.isEmpty(recipe.getCreateDate())) {
                result.setMsg("处方创建时间为空");
                return result;
            } else {
                Date cDate = DateConversion.parseDate(recipe.getCreateDate(), DateConversion.DEFAULT_DATE_TIME);
                if (null == cDate) {
                    //格式为 yyyy-MM-dd HH:mm:ss
                    result.setMsg("处方创建时间格式错误");
                    return result;
                }
            }
        }

        result.setCode(CommonConstant.SUCCESS);
        return result;
    }
}
