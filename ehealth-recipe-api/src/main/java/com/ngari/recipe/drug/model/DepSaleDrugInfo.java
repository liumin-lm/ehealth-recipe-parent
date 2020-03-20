package com.ngari.recipe.drug.model;

import java.io.Serializable;

/**
 * created by shiyuping on 2020/3/20
 */
public class DepSaleDrugInfo implements Serializable {
    private static final long serialVersionUID = -7133021932958904909L;
    private String drugEnterpriseName;
    private String saleDrugCode;

    public String getDrugEnterpriseName() {
        return drugEnterpriseName;
    }

    public void setDrugEnterpriseName(String drugEnterpriseName) {
        this.drugEnterpriseName = drugEnterpriseName;
    }

    public String getSaleDrugCode() {
        return saleDrugCode;
    }

    public void setSaleDrugCode(String saleDrugCode) {
        this.saleDrugCode = saleDrugCode;
    }
}
