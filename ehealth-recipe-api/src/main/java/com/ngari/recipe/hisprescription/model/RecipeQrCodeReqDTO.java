package com.ngari.recipe.hisprescription.model;

import java.io.Serializable;

/**
 * created by shiyuping on 2019/11/7
 */
public class RecipeQrCodeReqDTO implements Serializable {
    private static final long serialVersionUID = -609891834517740099L;
    private String qrInfo;
    private Integer organId;
    private String clientType;
    private String appId;

    public String getQrInfo() {
        return qrInfo;
    }

    public void setQrInfo(String qrInfo) {
        this.qrInfo = qrInfo;
    }

    public Integer getOrganId() {
        return organId;
    }

    public void setOrganId(Integer organId) {
        this.organId = organId;
    }

    public String getClientType() {
        return clientType;
    }

    public void setClientType(String clientType) {
        this.clientType = clientType;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }
}
