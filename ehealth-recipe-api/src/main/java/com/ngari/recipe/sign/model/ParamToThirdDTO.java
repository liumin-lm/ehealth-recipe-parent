package com.ngari.recipe.sign.model;

/**
 * @ClassName ParamToThirdDTO
 * @Description
 * @Author maoLy
 * @Date 2020/6/10
 **/
public class ParamToThirdDTO {

    private String jobNumber;
    private String thirdParty;
    private String signMsg;
    private String cretMsg;
    private String bussNo;
    private String organizeCode;

    public String getJobNumber() {
        return jobNumber;
    }

    public void setJobNumber(String jobNumber) {
        this.jobNumber = jobNumber;
    }

    public String getThirdParty() {
        return thirdParty;
    }

    public void setThirdParty(String thirdParty) {
        this.thirdParty = thirdParty;
    }

    public String getSignMsg() {
        return signMsg;
    }

    public void setSignMsg(String signMsg) {
        this.signMsg = signMsg;
    }

    public String getCretMsg() {
        return cretMsg;
    }

    public void setCretMsg(String cretMsg) {
        this.cretMsg = cretMsg;
    }

    public String getBussNo() {
        return bussNo;
    }

    public void setBussNo(String bussNo) {
        this.bussNo = bussNo;
    }

    public String getOrganizeCode() {
        return organizeCode;
    }

    public void setOrganizeCode(String organizeCode) {
        this.organizeCode = organizeCode;
    }
}
