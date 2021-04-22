package recipe.ca.vo;

import java.util.Map;

public class CaSignResultVo {

    /**
     * 签章后的pdfbase64文件
     */
    private String pdfBase64;
    /**
     * 签名的时间戳
     */
    private String signCADate;
    /**
     * 电子签名值
     */
    private String signRecipeCode;

    private String fileId;

    private Integer code;

    private String msg;

    //当前ca关联的处方id
    private Integer recipeId;

    //添加字段ca结果（-1: 当前ca操作未结束；0：当前ca已结束，结果失败; 1：当前ca已结束，结果成功）
    //date 20200617
    private Integer resultCode;

    //证书
    private String certificate;

    //手写签名
    private String signPicture;

    //e签保返回
    private Map<String, Object> esignResponseMap;

    //当前ca关联的处方业务类型
    private Integer bussType;

    //签名医生
    private Integer signDoctor;

    public Integer getBussType() {
        return bussType;
    }

    public void setBussType(Integer bussType) {
        this.bussType = bussType;
    }

    public Map<String, Object> getEsignResponseMap() {
        return esignResponseMap;
    }

    public void setEsignResponseMap(Map<String, Object> esignResponseMap) {
        this.esignResponseMap = esignResponseMap;
    }

    public Integer getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(Integer recipeId) {
        this.recipeId = recipeId;
    }

    public Integer getResultCode() {
        return resultCode;
    }

    public void setResultCode(Integer resultCode) {
        this.resultCode = resultCode;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getPdfBase64() {
        return pdfBase64;
    }

    public void setPdfBase64(String pdfBase64) {
        this.pdfBase64 = pdfBase64;
    }

    public String getSignCADate() {
        return signCADate;
    }

    public void setSignCADate(String signCADate) {
        this.signCADate = signCADate;
    }

    public String getSignRecipeCode() {
        return signRecipeCode;
    }

    public void setSignRecipeCode(String signRecipeCode) {
        this.signRecipeCode = signRecipeCode;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
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

    public Integer getSignDoctor() {
        return signDoctor;
    }

    public void setSignDoctor(Integer signDoctor) {
        this.signDoctor = signDoctor;
    }
}
