package com.ngari.recipe.drug.model;

import ctd.schema.annotation.Schema;

import java.io.Serializable;

/**
 * @author yinsheng
 * @date 2019\6\27 0027 11:00
 */
@Schema
public class UpDownDrugBean implements Serializable{
    private static final long serialVersionUID = -7380985078850529854L;

    private String organizeCode;

    private String organDrugCode;

    private Integer status;

    public String getOrganizeCode() {
        return organizeCode;
    }

    public void setOrganizeCode(String organizeCode) {
        this.organizeCode = organizeCode;
    }

    public String getOrganDrugCode() {
        return organDrugCode;
    }

    public void setOrganDrugCode(String organDrugCode) {
        this.organDrugCode = organDrugCode;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
