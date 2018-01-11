package recipe.bean;

import com.google.common.collect.Maps;
import com.ngari.base.patient.model.HealthCardBean;
import com.ngari.base.patient.model.PatientBean;
import com.ngari.recipe.entity.DrugList;
import com.ngari.recipe.entity.OrganDrugList;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.Recipedetail;
import ctd.persistence.DAOFactory;
import ctd.schema.exception.ValidateException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import recipe.constant.RecipeBussConstant;
import recipe.dao.DrugListDAO;
import recipe.dao.OrganDrugListDAO;
import recipe.util.ChinaIDNumberUtil;
import recipe.util.DateConversion;
import recipe.util.LocalStringUtil;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * 第三方医院处方对象
 * company: ngarihealth
 * @author: 0184/yu_yun
 * date:2017/4/17.
 */
public class HospitalRecipeBean implements Serializable {

    private static final long serialVersionUID = 8115518792159620416L;

    /**
     * 配送方式 药店取药 2
     */
    private static final String DELIVERYTYPE_TFDS = "2";

    /**
     * 支付标识 已支付 1
     */
    private static final String ISPAY_TURE = "1";

    /**
     * 卡类型 医保卡 2
     */
    private static final String CARD_TYPE_YBK = "2";

    /**
     * 机构组织代码（平台约定）
     */
    private String organID;

    /**
     * 源机构编号
     */
    private String originOrganID;

    /**
     * HIS处方号
     */
    private String recipeNo;

    /**
     * 源处方编号
     */
    private String originRecipeNo;

    /**
     * 院区代码 A001、 A002
     */
    private String hoscode;

    /**
     * 卡类型（1医院就诊卡  2医保卡3 医院病历号）
     */
    private String cardType;

    /**
     * 卡(病历)号码
     */
    private String cardNo;

    /**
     * 患者身份证
     */
    private String certID;

    /**
     * 患者姓名
     */
    private String patientName;

    /**
     * 是否监护人标记，0: 非监护人，1:监护人
     */
    private String guardianFlag;

    /**
     * (guardianFlag=1)时需要监护人姓名
     */
    private String guardianName;

    /**
     * (guardianFlag=1)时需要患者出生日期 (2000-01-01)
     */
    private String patientBirthDay;

    /**
     * (guardianFlag=1)时需要患者性别1:男  2:女
     */
    private String patientSex;

    /**
     * 手机号码
     */
    private String mobile;

    /**
     * 处方医生工号
     */
    private String doctorID;

    /**
     * 开单科室（挂号科室）
     */
    private String deptID;

    /**
     * 处方审核医生工号
     */
    private String auditDoctor;

    /**
     * 处方类型 1西药 2成药 3草药
     */
    private String recipeType;

    /**
     * 处方日期
     */
    private String datein;

    /**
     * 处方开始日期
     */
    private String startDate;

    /**
     * 处方结束日期
     */
    private String endDate;

    /**
     * 是否已经支付1支付 0未支付
     */
    private String isPay;

    /**
     * 配送方式 0医院药房取药 1物流配送 2药店取药
     */
    private String deliveryType;

    /**
     * ICD诊断码
     */
    private String icdCode;

    /**
     * ICD名称
     */
    private String icdName;

    /**
     * 取药的下级医院ID
     */
    private String subOrganId;

    /**
     * 简要病史(病史摘要)
     */
    private String diseasesHistory;

    /**
     * 处方天数
     */
    private String valueDays;

    /**
     * 处方状态 1 已支付 2已发药 7已接收  8已取消
     */
    private String recipeStatus;

    /**
     * 处方列表数据集
     */
    private List<HospitalRecipeDetailBean> orderList;

    private List<String> recipeCodeList;

    /**
     * 是否监护人标记 监护人 1
     */
    public static final String GUARDIAN_FLAG = "1";

    public HospitalRecipeBean() {
    }

    public Recipe convertNgariRecipe() {
        Recipe recipe = new Recipe();
        //此处接收到的是上级医院的处方，需要设置处方来源信息
        recipe.setOriginClinicOrgan(Integer.parseInt(this.organID));
        recipe.setClinicOrgan(Integer.parseInt(this.subOrganId));
        recipe.setRecipeType(Integer.parseInt(this.recipeType));
        recipe.setOriginRecipeCode(this.recipeNo);
        Date createDate = DateTime.now().toDate();
        if (StringUtils.isNotEmpty(this.datein)) {
            createDate = DateConversion.parseDate(this.datein, DateConversion.DEFAULT_DATE_TIME);
        }
        recipe.setSignDate(createDate);
        recipe.setCreateDate(createDate);
        recipe.setLastModify(createDate);
        recipe.setOrganDiseaseId(this.icdCode);
        recipe.setOrganDiseaseName(this.icdName);
        recipe.setRecipeMemo(this.diseasesHistory);
        recipe.setValueDays((null == this.valueDays) ? 1 : Integer.parseInt(this.valueDays));

        //默认2 药店取药
        if (DELIVERYTYPE_TFDS.equals(this.deliveryType)) {
            recipe.setGiveMode(RecipeBussConstant.GIVEMODE_TFDS);
            recipe.setPayMode(RecipeBussConstant.PAYMODE_TFDS);
        } else {
            recipe.setGiveMode(RecipeBussConstant.GIVEMODE_TO_HOS);
            recipe.setPayMode(RecipeBussConstant.PAYMODE_TO_HOS);
        }

        if (ISPAY_TURE.equals(this.isPay)) {
            recipe.setPayFlag(1);
            recipe.setPayDate(createDate);
        }
        recipe.setChooseFlag(1);
        //从医院HIS获取的处方
        recipe.setFromflag(0);
        return recipe;
    }

    public List<Recipedetail> convertNgariDetail() {
        OrganDrugListDAO organDrugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);
        DrugListDAO drugListDAO = DAOFactory.getDAO(DrugListDAO.class);

        List<HospitalRecipeDetailBean> hosDetailList = this.orderList;
        List<Recipedetail> recipeDetails = new ArrayList<>(hosDetailList.size());
        List<String> organDrugCode = new ArrayList<>(hosDetailList.size());
        for (HospitalRecipeDetailBean hosDetail : hosDetailList) {
            organDrugCode.add(hosDetail.getDrcode());
        }
        //从base_organdruglist获取平台药品ID数据
        List<OrganDrugList> organDrugList = organDrugListDAO.findByOrganIdAndDrugCodes(Integer.parseInt(this.organID), organDrugCode);
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
                List<OrganDrugList> subOrganDrugList = organDrugListDAO.findByOrganIdAndDrugIds(Integer.parseInt(this.subOrganId), drugIds);
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
                    for (HospitalRecipeDetailBean hosDetail : hosDetailList) {
                        drug = drugRel.get(hosDetail.getDrcode());
                        if (null != drug) {
                            recipedetail = new Recipedetail();
                            recipedetail.setStatus(1);
                            recipedetail.setCreateDt(now);
                            recipedetail.setLastModify(now);
                            recipedetail.setDrugId(drug.getDrugId());
                            recipedetail.setOrganDrugCode(hosDetail.getDrcode());
                            recipedetail.setMemo(hosDetail.getRemark());
                            recipedetail.setDrugGroup(hosDetail.getDrugGroup());
                            recipedetail.setDrugName(StringUtils.defaultString(hosDetail.getDrname(), drug.getDrugName()));
                            recipedetail.setDrugSpec(StringUtils.defaultString(hosDetail.getDrmodel(), drug.getDrugSpec()));
                            recipedetail.setDrugUnit(StringUtils.defaultString(hosDetail.getPackUnit(), drug.getUnit()));
                            recipedetail.setUseDoseUnit(StringUtils.defaultString(hosDetail.getDrunit(), drug.getUseDoseUnit()));
                            recipedetail.setDosageUnit(hosDetail.getDosageUnit());
                            recipedetail.setDefaultUseDose(drug.getUseDose());
                            recipedetail.setUseDose((null == hosDetail.getDosage()) ? drug.getUseDose() : Double.parseDouble(hosDetail.getDosage()));
                            recipedetail.setUseDays((null == hosDetail.getUseDays()) ? 1 : Integer.parseInt(hosDetail.getUseDays()));
                            recipedetail.setUseTotalDose((null == hosDetail.getDosageTotal()) ? 1 : Double.parseDouble(hosDetail.getDosageTotal()));
                            recipedetail.setPack((null == hosDetail.getPack()) ? drug.getPack() : Integer.parseInt(hosDetail.getPack()));
                            //用法
                            recipedetail.setUsePathways(StringUtils.defaultString(hosDetail.getAdmission(), drug.getUsePathways()));
                            //频次
                            recipedetail.setUsingRate(StringUtils.defaultString(hosDetail.getFrequency(), drug.getUsingRate()));

                            //设置价格
                            BigDecimal salePrice = codePriceRel.get(hosDetail.getDrcode());
                            if (null != salePrice) {
                                recipedetail.setSalePrice(salePrice);
                                BigDecimal drugCost = salePrice.multiply(new BigDecimal(recipedetail.getUseTotalDose()))
                                        .divide(BigDecimal.ONE, 3, RoundingMode.UP);
                                recipedetail.setDrugCost(drugCost);
                            }

                            recipeDetails.add(recipedetail);
                        }
                    }
                }
            }
        }

        return recipeDetails;
    }

    public PatientBean convertNgariPatient() {
        PatientBean patient = new PatientBean();
        patient.setPatientName(this.patientName);
        patient.setIdcard(this.certID);
        if (GUARDIAN_FLAG.equals(this.guardianFlag)) {
            //监护人模式
            patient.setGuardianFlag(true);
            patient.setGuardianName(this.guardianName);
            patient.setBirthday(DateConversion.parseDate(this.patientBirthDay, DateConversion.YYYY_MM_DD));
            patient.setPatientSex(this.getPatientSex());
        } else {
            patient.setGuardianFlag(false);
            //设置患者生日，性别
            try {
                String idCard18 = ChinaIDNumberUtil.convert15To18(this.certID);
                patient.setBirthday(ChinaIDNumberUtil.getBirthFromIDNumber(idCard18));
                patient.setPatientSex(ChinaIDNumberUtil.getSexFromIDNumber(idCard18));
            } catch (ValidateException e) {
                e.printStackTrace();
            }
        }
        patient.setMobile(this.mobile);
        if (StringUtils.isNotEmpty(this.cardNo)) {
            HealthCardBean card = new HealthCardBean();
            //卡类型（1医院就诊卡  2医保卡 3医院病历号）
            //卡类型字典需要对照
            if (CARD_TYPE_YBK.equals(this.cardType)) {
                //医保卡
                card.setCardType("2");
            } else {
                //就诊卡
                card.setCardType("3");
            }
            card.setCardId(this.cardNo);
            card.setCardOrgan(Integer.parseInt(this.subOrganId));
            patient.setHealthCards(Collections.singletonList(card));
            //默认自费
            patient.setPatientType("1");
        }

        return patient;
    }

    /**
     * 解析平台数据转换为医院处方对象
     *
     * @param recipe
     * @param details
     * @param patient
     */
    public RecipeResultBean parseRecipe(Recipe recipe, List<Recipedetail> details, PatientBean patient, List<HealthCardBean> cards) {
        RecipeResultBean result = RecipeResultBean.getSuccess();
        if (CollectionUtils.isEmpty(details)) {
            result.setCode(RecipeResultBean.FAIL);
            result.setMsg("处方详情为空");
            return result;
        }
        this.organID = LocalStringUtil.toString(recipe.getClinicOrgan());
        this.originOrganID = LocalStringUtil.toString(recipe.getOriginClinicOrgan());
        this.recipeNo = LocalStringUtil.toString(recipe.getRecipeCode());
        this.originRecipeNo = LocalStringUtil.toString(recipe.getOriginRecipeCode());
        this.certID = LocalStringUtil.toString(patient.getCertificate());
        this.patientName = LocalStringUtil.toString(patient.getPatientName());
        this.mobile = patient.getMobile();
        this.recipeType = LocalStringUtil.toString(recipe.getRecipeType());
        this.datein = new DateTime(recipe.getSignDate()).toString(DateConversion.DEFAULT_DATE_TIME);
        this.icdCode = recipe.getOrganDiseaseId();
        this.icdName = recipe.getOrganDiseaseName();
        this.diseasesHistory = recipe.getRecipeMemo();
        this.valueDays = LocalStringUtil.toString(recipe.getValueDays());
        this.isPay = (Integer.valueOf(1).equals(recipe.getPayFlag())) ? "1" : "0";
        //0医院药房取药 1物流配送 2药店取药
        if (RecipeBussConstant.PAYMODE_TFDS.equals(recipe.getPayMode())) {
            this.deliveryType = "2";
        } else {
            this.deliveryType = "1";
        }

        //就诊卡处理
        if (CollectionUtils.isNotEmpty(cards)) {
            HealthCardBean card = cards.get(0);
            if (CARD_TYPE_YBK.equals(card.getCardType())) {
                this.cardType = "2";
            } else {
                this.cardType = "1";
            }
            this.cardNo = card.getCardId();
        }

        List<HospitalRecipeDetailBean> hosDetailList = new ArrayList<>(details.size());
        HospitalRecipeDetailBean hosDetail;
        for (Recipedetail detail : details) {
            hosDetail = new HospitalRecipeDetailBean();
            hosDetail.setDrcode(detail.getOrganDrugCode());
            hosDetail.setDrname(detail.getDrugName());
            hosDetail.setDrugGroup(detail.getDrugGroup());
            hosDetail.setDrmodel(detail.getDrugSpec());
            hosDetail.setPack(detail.getPack().toString());
            hosDetail.setPackUnit(detail.getDrugUnit());
            hosDetail.setAdmission(detail.getUsePathways());
            hosDetail.setFrequency(detail.getUsingRate());
            hosDetail.setDosage(detail.getUseDose().toString());
            hosDetail.setDrunit(detail.getUseDoseUnit());
            hosDetail.setDosageUnit(detail.getDosageUnit());
            hosDetail.setDosageTotal(detail.getUseTotalDose().toString());
            hosDetail.setUseDays(detail.getUseDays().toString());
            hosDetail.setRemark(detail.getMemo());

            hosDetailList.add(hosDetail);
        }
        this.orderList = hosDetailList;

        return result;
    }

    public String getOrganID() {
        return organID;
    }

    public void setOrganID(String organID) {
        this.organID = organID;
    }

    public String getOriginOrganID() {
        return originOrganID;
    }

    public void setOriginOrganID(String originOrganID) {
        this.originOrganID = originOrganID;
    }

    public String getRecipeNo() {
        return recipeNo;
    }

    public void setRecipeNo(String recipeNo) {
        this.recipeNo = recipeNo;
    }

    public String getOriginRecipeNo() {
        return originRecipeNo;
    }

    public void setOriginRecipeNo(String originRecipeNo) {
        this.originRecipeNo = originRecipeNo;
    }

    public String getHoscode() {
        return hoscode;
    }

    public void setHoscode(String hoscode) {
        this.hoscode = hoscode;
    }

    public String getCardType() {
        return cardType;
    }

    public void setCardType(String cardType) {
        this.cardType = cardType;
    }

    public String getCardNo() {
        return cardNo;
    }

    public void setCardNo(String cardNo) {
        this.cardNo = cardNo;
    }

    public String getCertID() {
        return certID;
    }

    public void setCertID(String certID) {
        this.certID = certID;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getGuardianName() {
        return guardianName;
    }

    public void setGuardianName(String guardianName) {
        this.guardianName = guardianName;
    }

    public String getGuardianFlag() {
        return guardianFlag;
    }

    public void setGuardianFlag(String guardianFlag) {
        this.guardianFlag = guardianFlag;
    }

    public String getPatientBirthDay() {
        return patientBirthDay;
    }

    public void setPatientBirthDay(String patientBirthDay) {
        this.patientBirthDay = patientBirthDay;
    }

    public String getPatientSex() {
        return patientSex;
    }

    public void setPatientSex(String patientSex) {
        this.patientSex = patientSex;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getDoctorID() {
        return doctorID;
    }

    public void setDoctorID(String doctorID) {
        this.doctorID = doctorID;
    }

    public String getDeptID() {
        return deptID;
    }

    public void setDeptID(String deptID) {
        this.deptID = deptID;
    }

    public String getAuditDoctor() {
        return auditDoctor;
    }

    public void setAuditDoctor(String auditDoctor) {
        this.auditDoctor = auditDoctor;
    }

    public String getRecipeType() {
        return recipeType;
    }

    public void setRecipeType(String recipeType) {
        this.recipeType = recipeType;
    }

    public String getDatein() {
        return datein;
    }

    public void setDatein(String datein) {
        this.datein = datein;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public String getIsPay() {
        return isPay;
    }

    public void setIsPay(String isPay) {
        this.isPay = isPay;
    }

    public String getDeliveryType() {
        return deliveryType;
    }

    public void setDeliveryType(String deliveryType) {
        this.deliveryType = deliveryType;
    }

    public String getIcdCode() {
        return icdCode;
    }

    public void setIcdCode(String icdCode) {
        this.icdCode = icdCode;
    }

    public String getIcdName() {
        return icdName;
    }

    public void setIcdName(String icdName) {
        this.icdName = icdName;
    }

    public String getSubOrganId() {
        return subOrganId;
    }

    public void setSubOrganId(String subOrganId) {
        this.subOrganId = subOrganId;
    }

    public String getDiseasesHistory() {
        return diseasesHistory;
    }

    public void setDiseasesHistory(String diseasesHistory) {
        this.diseasesHistory = diseasesHistory;
    }

    public String getValueDays() {
        return valueDays;
    }

    public void setValueDays(String valueDays) {
        this.valueDays = valueDays;
    }

    public String getRecipeStatus() {
        return recipeStatus;
    }

    public void setRecipeStatus(String recipeStatus) {
        this.recipeStatus = recipeStatus;
    }

    public List<HospitalRecipeDetailBean> getOrderList() {
        return orderList;
    }

    public void setOrderList(List<HospitalRecipeDetailBean> orderList) {
        this.orderList = orderList;
    }

    public List<String> getRecipeCodeList() {
        return recipeCodeList;
    }

    public void setRecipeCodeList(List<String> recipeCodeList) {
        this.recipeCodeList = recipeCodeList;
    }

}




