package recipe.drugsenterprise.bean.yd.model;

import ctd.schema.annotation.Desensitizations;
import ctd.schema.annotation.DesensitizationsType;
import recipe.drugsenterprise.bean.yd.utils.GsonUtils;

import java.util.List;

public class RecipeVo implements JsonAware {

    private String recipeno;
    private String caseno;
    private String hiscardno;
    private String patientname;
    private String idnumber;
    @Desensitizations(type = DesensitizationsType.MOBILE)
    private String mobile;
    private String outhospno;
    private String empsex;
    private String age;
    private String birthdate;
    private String visitdate;
    private String patienttypename;
    private String medicarecategname;
    private String signalsourcetypename;
    private String registdeptcode;
    private String registdeptname;
    private String hospital;
    private String registdrcode;
    private String registdrname;
    private String recipebegindate;
    private String recipeenddate;
    private String contactname;
    private String contactaddr;
    private String contactphone;
    private String country;//国家
    private String province;//省
    private String city;//市
    private String disrict;//区
    @Desensitizations(type = DesensitizationsType.ADDRESS)
    private String address;//地址
    private String paydate;
    private String paystatus;
    private String storeno;
    private String diagcode;
    private String diagname;
    private String patientno;
    private List<RecipeDtlVo> detaillist;

    //17:5253
//星期二
//2019年9月24日  新增
    private String recipe_source_flag;    //处方来源类型 1：住院, 2：门诊
    private String leave_hospital_date;   //出院时间
    private String hospitalization_department;//患者住院科室名称
    private String hospitalization_no;//住院号
    private String hospitalization_bedno;//住院床号
    private String consignorNo;

    private String doctorName;


//end


    public String getDoctorName() {
        return doctorName;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }

    public String getConsignorNo() {
        return consignorNo;
    }

    public void setConsignorNo(String consignorNo) {
        this.consignorNo = consignorNo;
    }

    public String getRecipe_source_flag() {
        return recipe_source_flag;
    }

    public void setRecipe_source_flag(String recipe_source_flag) {
        this.recipe_source_flag = recipe_source_flag;
    }

    public String getLeave_hospital_date() {
        return leave_hospital_date;
    }

    public void setLeave_hospital_date(String leave_hospital_date) {
        this.leave_hospital_date = leave_hospital_date;
    }

    public String getHospitalization_department() {
        return hospitalization_department;
    }

    public void setHospitalization_department(String hospitalization_department) {
        this.hospitalization_department = hospitalization_department;
    }

    public String getHospitalization_no() {
        return hospitalization_no;
    }

    public void setHospitalization_no(String hospitalization_no) {
        this.hospitalization_no = hospitalization_no;
    }

    public String getHospitalization_bedno() {
        return hospitalization_bedno;
    }

    public void setHospitalization_bedno(String hospitalization_bedno) {
        this.hospitalization_bedno = hospitalization_bedno;
    }

    public String getRecipeno() {
        return recipeno;
    }

    public void setRecipeno(String recipeno) {
        this.recipeno = recipeno;
    }

    public String getCaseno() {
        return caseno;
    }

    public void setCaseno(String caseno) {
        this.caseno = caseno;
    }

    public String getHiscardno() {
        return hiscardno;
    }

    public void setHiscardno(String hiscardno) {
        this.hiscardno = hiscardno;
    }

    public String getPatientname() {
        return patientname;
    }

    public void setPatientname(String patientname) {
        this.patientname = patientname;
    }

    public String getIdnumber() {
        return idnumber;
    }

    public void setIdnumber(String idnumber) {
        this.idnumber = idnumber;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getOuthospno() {
        return outhospno;
    }

    public void setOuthospno(String outhospno) {
        this.outhospno = outhospno;
    }

    public String getEmpsex() {
        return empsex;
    }

    public void setEmpsex(String empsex) {
        this.empsex = empsex;
    }

    public String getAge() {
        return age;
    }

    public void setAge(String age) {
        this.age = age;
    }

    public String getBirthdate() {
        return birthdate;
    }

    public void setBirthdate(String birthdate) {
        this.birthdate = birthdate;
    }

    public String getVisitdate() {
        return visitdate;
    }

    public void setVisitdate(String visitdate) {
        this.visitdate = visitdate;
    }

    public String getPatienttypename() {
        return patienttypename;
    }

    public void setPatienttypename(String patienttypename) {
        this.patienttypename = patienttypename;
    }

    public String getMedicarecategname() {
        return medicarecategname;
    }

    public void setMedicarecategname(String medicarecategname) {
        this.medicarecategname = medicarecategname;
    }

    public String getSignalsourcetypename() {
        return signalsourcetypename;
    }

    public void setSignalsourcetypename(String signalsourcetypename) {
        this.signalsourcetypename = signalsourcetypename;
    }

    public String getRegistdeptcode() {
        return registdeptcode;
    }

    public void setRegistdeptcode(String registdeptcode) {
        this.registdeptcode = registdeptcode;
    }

    public String getRegistdeptname() {
        return registdeptname;
    }

    public void setRegistdeptname(String registdeptname) {
        this.registdeptname = registdeptname;
    }

    public String getHospital() {
        return hospital;
    }

    public void setHospital(String hospital) {
        this.hospital = hospital;
    }

    public String getRegistdrcode() {
        return registdrcode;
    }

    public void setRegistdrcode(String registdrcode) {
        this.registdrcode = registdrcode;
    }

    public String getRegistdrname() {
        return registdrname;
    }

    public void setRegistdrname(String registdrname) {
        this.registdrname = registdrname;
    }

    public String getRecipebegindate() {
        return recipebegindate;
    }

    public void setRecipebegindate(String recipebegindate) {
        this.recipebegindate = recipebegindate;
    }

    public String getRecipeenddate() {
        return recipeenddate;
    }

    public void setRecipeenddate(String recipeenddate) {
        this.recipeenddate = recipeenddate;
    }

    public String getContactname() {
        return contactname;
    }

    public void setContactname(String contactname) {
        this.contactname = contactname;
    }

    public String getContactaddr() {
        return contactaddr;
    }

    public void setContactaddr(String contactaddr) {
        this.contactaddr = contactaddr;
    }

    public String getContactphone() {
        return contactphone;
    }

    public void setContactphone(String contactphone) {
        this.contactphone = contactphone;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getDisrict() {
        return disrict;
    }

    public void setDisrict(String disrict) {
        this.disrict = disrict;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPaydate() {
        return paydate;
    }

    public void setPaydate(String paydate) {
        this.paydate = paydate;
    }

    public String getPaystatus() {
        return paystatus;
    }

    public void setPaystatus(String paystatus) {
        this.paystatus = paystatus;
    }

    public String getStoreno() {
        return storeno;
    }

    public void setStoreno(String storeno) {
        this.storeno = storeno;
    }

    public String getDiagcode() {
        return diagcode;
    }

    public void setDiagcode(String diagcode) {
        this.diagcode = diagcode;
    }

    public String getDiagname() {
        return diagname;
    }

    public void setDiagname(String diagname) {
        this.diagname = diagname;
    }

    public String getPatientno() {
        return patientno;
    }

    public void setPatientno(String patientno) {
        this.patientno = patientno;
    }

    public List<RecipeDtlVo> getDetaillist() {
        return detaillist;
    }

    public void setDetaillist(List<RecipeDtlVo> detaillist) {
        this.detaillist = detaillist;
    }

    public String toJSONString() {
        return GsonUtils.toJson(this);
    }
}
