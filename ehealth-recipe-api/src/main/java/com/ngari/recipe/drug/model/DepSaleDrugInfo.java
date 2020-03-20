package com.ngari.recipe.drug.model;

import java.io.Serializable;

/**
 * created by shiyuping on 2020/3/20
 */
public class DepSaleDrugInfo implements Serializable {
    private static final long serialVersionUID = -7133021932958904909L;
    private String drugEnterpriseName;
    private String saleDrugName;

    public String getDrugEnterpriseName() {
        return drugEnterpriseName;
    }

    public void setDrugEnterpriseName(String drugEnterpriseName) {
        this.drugEnterpriseName = drugEnterpriseName;
    }

    public String getSaleDrugName() {
        return saleDrugName;
    }

    public void setSaleDrugName(String saleDrugName) {
        this.saleDrugName = saleDrugName;
    }
}
