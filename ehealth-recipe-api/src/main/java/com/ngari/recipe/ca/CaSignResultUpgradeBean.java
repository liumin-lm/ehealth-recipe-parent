package com.ngari.recipe.ca;

import java.util.Map;

public class CaSignResultUpgradeBean implements java.io.Serializable {

    private static final long serialVersionUID = -4241163973791401433L;

    /**
     * 签章后的pdfbase64文件
     */
    private String pdfBase64;
    /**
     * 签名的时间戳
     */
    private String signDate;
    /**
     * 电子签名值
     */
    private String signCode;

    private String fileId;

    private Integer msgCode;

    private String msg;

    //当前ca关联的处方id
    private Integer bussId;

    //添加字段ca结果（-1: 当前ca操作未结束；0：当前ca已结束，结果失败; 1：当前ca已结束，结果成功）
    //date 20200617
    private Integer resultStatus;

    //证书
    private String certificate;

    //手写签名
    private String signPicture;

    //e签保返回
    private Map<String, Object> esignResponseMap;

    //当前ca关联的处方业务类型
    private Integer busstype;

    // CA类型
    private String caType;

    // 签名原文
    private String signText;

    private Integer signDoctor;

    public String getPdfBase64() {
        return pdfBase64;
    }

    public void setPdfBase64(String pdfBase64) {
        this.pdfBase64 = pdfBase64;
    }

    public String getSignDate() {
        return signDate;
    }

    public void setSignDate(String signDate) {
        this.signDate = signDate;
    }

    public String getSignCode() {
        return signCode;
    }

    public void setSignCode(String signCode) {
        this.signCode = signCode;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public Integer getMsgCode() {
        return msgCode;
    }

    public void setMsgCode(Integer msgCode) {
        this.msgCode = msgCode;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public Integer getBussId() {
        return bussId;
    }

    public void setBussId(Integer bussId) {
        this.bussId = bussId;
    }

    public Integer getResultStatus() {
        return resultStatus;
    }

    public void setResultStatus(Integer resultStatus) {
        this.resultStatus = resultStatus;
    }

    public String getCertificate() {
        return certificate;
    }

    public void setCertificate(String certificate) {
        this.certificate = certificate;
    }

    public String getSignPicture() {
        return signPicture;
    }

    public void setSignPicture(String signPicture) {
        this.signPicture = signPicture;
    }

    public Map<String, Object> getEsignResponseMap() {
        return esignResponseMap;
    }

    public void setEsignResponseMap(Map<String, Object> esignResponseMap) {
        this.esignResponseMap = esignResponseMap;
    }

    public Integer getBusstype() {
        return busstype;
    }

    public void setBusstype(Integer busstype) {
        this.busstype = busstype;
    }

    public String getCaType() {
        return caType;
    }

    public void setCaType(String caType) {
        this.caType = caType;
    }

    public String getSignText() {
        return signText;
    }

    public void setSignText(String signText) {
        this.signText = signText;
    }

    public Integer getSignDoctor() {
        return signDoctor;
    }

    public void setSignDoctor(Integer signDoctor) {
        this.signDoctor = signDoctor;
    }
}
