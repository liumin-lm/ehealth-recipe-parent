package recipe.drugsenterprise.bean;
/**
 * @Description: 对接上海国药推送处方中间对象
 * @Author: JRK
 * @Date: 2020/02/20
 */

import ctd.schema.annotation.Schema;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Schema
public class CommonSHRecipeInfoDto implements Serializable {
    /**
     *处方ID
     */
    private String PRESCRIPTNO;
    /**
     *处方明细号
     */
    private String PRESCRIPTNOSEQ;
    /**
     *医院代码
     */
    private String HOSCODE;
    /**
     *医院名称
     */
    private String HOSNAME;
    /**
     *健康卡号
     */
    private String HEALCARD;
    /**
     *身份证号
     *非必填
     */
    private String IDCARDNO;
    /**
     *社保卡号
     *非必填
     */
    private String ENDOWMENT;
    /**
     *医保卡号
     */
    private String MEDICALNO;
    /**
     *军官证号
     * 非必填
     */
    private String ARMYNO;
    /**
     *学生证号
     * 非必填
     */
    private String STUDENTNO;
    /**
     *居住证号
     *  非必填
     */
    private String RESIDENCE;
    /**
     *处方日期
     */
    private String BILLDATE;
    /**
     *患者姓名
     */
    private String PATIENTNAME;
    /**
     *性别
     */
    private String GENDER;
    /**
     *患者地址
     */
    private String PATIENTADDRESS;
    /**
     *患者手机号
     */
    private String PATIENTPHONE;
    /**
     *药品编号
     */
    private String GOODS;
    /**
     *药品名称
     */
    private String GOODSNAME;
    /**
     *通用名
     */
    private String GNAME;
    /**
     *规格
     */
    private String SPEC;
    /**
     *包装单位
     */
    private String MSUNITNO;
    /**
     *生产厂家
     */
    private String PRONAME;
    /**
     *采购数量
     */
    private int HOSQTY;
    /**
     *采购单价
     */
    private BigDecimal HOSPRICE;
    /**
     *收费金额
     */
    private BigDecimal SUMVALUE;
    /**
     *用法用量
     */
    private String METHODDESC;
    /**
     *社区配送地址编码
     */
    private String HOSDEPTCODE;
    /**
     * 社区配送地址名称
     */
    private String HOSDEPTNAME;
    /**
     * 送货方式，配送点提货（自提）或送货上门标志（1：自提；0：送货上门)
     * 非必填
     */
    private String PATTERN;
    /**
     * 备注
     * 非必填
     */
    private String REMARK;
    /**
     * 零售价
     */
    private BigDecimal RTLPRC;
    /**
     * 年龄
     */
    private int AGE;
    /**
     * 是否有其他供应商的药品需要提货（0:没有   1:  有）
     * 非必填
     */
    private String OTHERSUPPLIERS;
    /**
     * 0  批发    1  零售
     * 非必填
     */
    private String SALTYPE;

    public String getPRESCRIPTNO() {
        return PRESCRIPTNO;
    }

    public void setPRESCRIPTNO(String PRESCRIPTNO) {
        this.PRESCRIPTNO = PRESCRIPTNO;
    }

    public String getHOSCODE() {
        return HOSCODE;
    }

    public void setHOSCODE(String HOSCODE) {
        this.HOSCODE = HOSCODE;
    }

    public String getHOSNAME() {
        return HOSNAME;
    }

    public void setHOSNAME(String HOSNAME) {
        this.HOSNAME = HOSNAME;
    }

    public String getHEALCARD() {
        return HEALCARD;
    }

    public void setHEALCARD(String HEALCARD) {
        this.HEALCARD = HEALCARD;
    }

    public String getIDCARDNO() {
        return IDCARDNO;
    }

    public void setIDCARDNO(String IDCARDNO) {
        this.IDCARDNO = IDCARDNO;
    }

    public String getENDOWMENT() {
        return ENDOWMENT;
    }

    public void setENDOWMENT(String ENDOWMENT) {
        this.ENDOWMENT = ENDOWMENT;
    }

    public String getMEDICALNO() {
        return MEDICALNO;
    }

    public void setMEDICALNO(String MEDICALNO) {
        this.MEDICALNO = MEDICALNO;
    }

    public String getARMYNO() {
        return ARMYNO;
    }

    public void setARMYNO(String ARMYNO) {
        this.ARMYNO = ARMYNO;
    }

    public String getSTUDENTNO() {
        return STUDENTNO;
    }

    public void setSTUDENTNO(String STUDENTNO) {
        this.STUDENTNO = STUDENTNO;
    }

    public String getRESIDENCE() {
        return RESIDENCE;
    }

    public void setRESIDENCE(String RESIDENCE) {
        this.RESIDENCE = RESIDENCE;
    }

    public String getBILLDATE() {
        return BILLDATE;
    }

    public void setBILLDATE(String BILLDATE) {
        this.BILLDATE = BILLDATE;
    }

    public String getPATIENTNAME() {
        return PATIENTNAME;
    }

    public void setPATIENTNAME(String PATIENTNAME) {
        this.PATIENTNAME = PATIENTNAME;
    }

    public String getGENDER() {
        return GENDER;
    }

    public void setGENDER(String GENDER) {
        this.GENDER = GENDER;
    }

    public String getPATIENTADDRESS() {
        return PATIENTADDRESS;
    }

    public void setPATIENTADDRESS(String PATIENTADDRESS) {
        this.PATIENTADDRESS = PATIENTADDRESS;
    }

    public String getPATIENTPHONE() {
        return PATIENTPHONE;
    }

    public void setPATIENTPHONE(String PATIENTPHONE) {
        this.PATIENTPHONE = PATIENTPHONE;
    }

    public String getGOODS() {
        return GOODS;
    }

    public void setGOODS(String GOODS) {
        this.GOODS = GOODS;
    }

    public String getGOODSNAME() {
        return GOODSNAME;
    }

    public void setGOODSNAME(String GOODSNAME) {
        this.GOODSNAME = GOODSNAME;
    }

    public String getGNAME() {
        return GNAME;
    }

    public void setGNAME(String GNAME) {
        this.GNAME = GNAME;
    }

    public String getSPEC() {
        return SPEC;
    }

    public void setSPEC(String SPEC) {
        this.SPEC = SPEC;
    }

    public String getMSUNITNO() {
        return MSUNITNO;
    }

    public void setMSUNITNO(String MSUNITNO) {
        this.MSUNITNO = MSUNITNO;
    }

    public String getPRONAME() {
        return PRONAME;
    }

    public void setPRONAME(String PRONAME) {
        this.PRONAME = PRONAME;
    }

    public int getHOSQTY() {
        return HOSQTY;
    }

    public void setHOSQTY(int HOSQTY) {
        this.HOSQTY = HOSQTY;
    }

    public BigDecimal getHOSPRICE() {
        return HOSPRICE;
    }

    public void setHOSPRICE(BigDecimal HOSPRICE) {
        this.HOSPRICE = HOSPRICE;
    }

    public BigDecimal getSUMVALUE() {
        return SUMVALUE;
    }

    public void setSUMVALUE(BigDecimal SUMVALUE) {
        this.SUMVALUE = SUMVALUE;
    }

    public String getMETHODDESC() {
        return METHODDESC;
    }

    public void setMETHODDESC(String METHODDESC) {
        this.METHODDESC = METHODDESC;
    }

    public String getHOSDEPTCODE() {
        return HOSDEPTCODE;
    }

    public void setHOSDEPTCODE(String HOSDEPTCODE) {
        this.HOSDEPTCODE = HOSDEPTCODE;
    }

    public String getHOSDEPTNAME() {
        return HOSDEPTNAME;
    }

    public void setHOSDEPTNAME(String HOSDEPTNAME) {
        this.HOSDEPTNAME = HOSDEPTNAME;
    }

    public String getPATTERN() {
        return PATTERN;
    }

    public void setPATTERN(String PATTERN) {
        this.PATTERN = PATTERN;
    }

    public String getREMARK() {
        return REMARK;
    }

    public void setREMARK(String REMARK) {
        this.REMARK = REMARK;
    }

    public BigDecimal getRTLPRC() {
        return RTLPRC;
    }

    public void setRTLPRC(BigDecimal RTLPRC) {
        this.RTLPRC = RTLPRC;
    }

    public int getAGE() {
        return AGE;
    }

    public void setAGE(int AGE) {
        this.AGE = AGE;
    }

    public String getOTHERSUPPLIERS() {
        return OTHERSUPPLIERS;
    }

    public void setOTHERSUPPLIERS(String OTHERSUPPLIERS) {
        this.OTHERSUPPLIERS = OTHERSUPPLIERS;
    }

    public String getSALTYPE() {
        return SALTYPE;
    }

    public void setSALTYPE(String SALTYPE) {
        this.SALTYPE = SALTYPE;
    }

    public String getPRESCRIPTNOSEQ() {
        return PRESCRIPTNOSEQ;
    }

    public void setPRESCRIPTNOSEQ(String PRESCRIPTNOSEQ) {
        this.PRESCRIPTNOSEQ = PRESCRIPTNOSEQ;
    }
}
