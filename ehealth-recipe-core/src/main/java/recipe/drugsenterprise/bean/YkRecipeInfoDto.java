package recipe.drugsenterprise.bean;
/**
 * @Description: 对接英克推送处方中间对象
 * @Author: JRK
 * @Date: 2020/02/20
 */

import ctd.schema.annotation.Schema;

import java.io.Serializable;
import java.util.List;

@Schema
public class YkRecipeInfoDto implements Serializable {
    /**
     *门店ID
     */
    private String placepointid;
    /**
     *处方日期
     */
    private String credata;
    /**
     *处方编号
     */
    private String prescriptionnumber;
    /**
     *处方医院
     */
    private String prescriptionhospital;
    /**
     *处方医生
     */
    private String prescriptiondoctor;
    /**
     *临床诊断
     * 非必填
     */
    private String diagnosis;
    /**
     *患者姓名
     * 非必填
     */
    private String insidername;
    /**
     *患者性别
     * 非必填
     */
    private String insidersex;
    /**
     *患者年龄
     * 非必填
     */
    private String insiderage;
    /**
     *出生日期
     *  非必填
     */
    private String birthday;
    /**
     *身份证号
     *  非必填
     */
    private String id_card;
    /**
     *民族
     * 非必填
     */
    private String nation;
    /**
     *地址
     * 非必填
     */
    private String address;
    /**
     *用法用量
     * 非必填
     */
    private String usagedosage;
    /**
     *用药禁忌
     * 非必填
     */
    private String contraindication;
    /**
     *医保卡号
     */
    private String medicard;
    /**
     *会员卡号
     */
    private int insidercardno;
    /**
     *重症病种
     */
    private int diseaseid;
    /**
     *接口处方类型
     */
    private int prescriptiontype;
    /**
     *处方来源平台
     */
    private int sourceplatform;
    /**
     *处方总单号
     */
    private int prescriptionid;
    /**
     *接口处方状态
     */
    private String usestates;
    /**
     *收货人姓名
     */
    private String consigneename;
    /**
     *收货人手机
     */
    private String consigneetel;
    /**
     *收货地址-省市
     */
    private String consigpcity;
    /**
     *收货地址-街道地址
     */
    private String consigdstreet;

    /**
     * 药品详情
     */
    private List<YkDrugDto> goods_list;

    public String getPlacepointid() {
        return placepointid;
    }

    public void setPlacepointid(String placepointid) {
        this.placepointid = placepointid;
    }

    public String getCredata() {
        return credata;
    }

    public void setCredata(String credata) {
        this.credata = credata;
    }

    public String getPrescriptionnumber() {
        return prescriptionnumber;
    }

    public void setPrescriptionnumber(String prescriptionnumber) {
        this.prescriptionnumber = prescriptionnumber;
    }

    public String getPrescriptionhospital() {
        return prescriptionhospital;
    }

    public void setPrescriptionhospital(String prescriptionhospital) {
        this.prescriptionhospital = prescriptionhospital;
    }

    public String getPrescriptiondoctor() {
        return prescriptiondoctor;
    }

    public void setPrescriptiondoctor(String prescriptiondoctor) {
        this.prescriptiondoctor = prescriptiondoctor;
    }

    public String getDiagnosis() {
        return diagnosis;
    }

    public void setDiagnosis(String diagnosis) {
        this.diagnosis = diagnosis;
    }

    public String getInsidername() {
        return insidername;
    }

    public void setInsidername(String insidername) {
        this.insidername = insidername;
    }

    public String getInsidersex() {
        return insidersex;
    }

    public void setInsidersex(String insidersex) {
        this.insidersex = insidersex;
    }

    public String getInsiderage() {
        return insiderage;
    }

    public void setInsiderage(String insiderage) {
        this.insiderage = insiderage;
    }

    public String getBirthday() {
        return birthday;
    }

    public void setBirthday(String birthday) {
        this.birthday = birthday;
    }

    public String getId_card() {
        return id_card;
    }

    public void setId_card(String id_card) {
        this.id_card = id_card;
    }

    public String getNation() {
        return nation;
    }

    public void setNation(String nation) {
        this.nation = nation;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getUsagedosage() {
        return usagedosage;
    }

    public void setUsagedosage(String usagedosage) {
        this.usagedosage = usagedosage;
    }

    public String getContraindication() {
        return contraindication;
    }

    public void setContraindication(String contraindication) {
        this.contraindication = contraindication;
    }

    public String getMedicard() {
        return medicard;
    }

    public void setMedicard(String medicard) {
        this.medicard = medicard;
    }

    public int getInsidercardno() {
        return insidercardno;
    }

    public void setInsidercardno(int insidercardno) {
        this.insidercardno = insidercardno;
    }

    public int getDiseaseid() {
        return diseaseid;
    }

    public void setDiseaseid(int diseaseid) {
        this.diseaseid = diseaseid;
    }

    public int getPrescriptiontype() {
        return prescriptiontype;
    }

    public void setPrescriptiontype(int prescriptiontype) {
        this.prescriptiontype = prescriptiontype;
    }

    public int getSourceplatform() {
        return sourceplatform;
    }

    public void setSourceplatform(int sourceplatform) {
        this.sourceplatform = sourceplatform;
    }

    public int getPrescriptionid() {
        return prescriptionid;
    }

    public void setPrescriptionid(int prescriptionid) {
        this.prescriptionid = prescriptionid;
    }

    public String getUsestates() {
        return usestates;
    }

    public void setUsestates(String usestates) {
        this.usestates = usestates;
    }

    public String getConsigneename() {
        return consigneename;
    }

    public void setConsigneename(String consigneename) {
        this.consigneename = consigneename;
    }

    public String getConsigneetel() {
        return consigneetel;
    }

    public void setConsigneetel(String consigneetel) {
        this.consigneetel = consigneetel;
    }

    public String getConsigpcity() {
        return consigpcity;
    }

    public void setConsigpcity(String consigpcity) {
        this.consigpcity = consigpcity;
    }

    public String getConsigdstreet() {
        return consigdstreet;
    }

    public void setConsigdstreet(String consigdstreet) {
        this.consigdstreet = consigdstreet;
    }

    public List<YkDrugDto> getGoods_list() {
        return goods_list;
    }

    public void setGoods_list(List<YkDrugDto> goods_list) {
        this.goods_list = goods_list;
    }
}
