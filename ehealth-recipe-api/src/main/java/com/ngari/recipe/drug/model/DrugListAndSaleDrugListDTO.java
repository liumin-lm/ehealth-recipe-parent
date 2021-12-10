package com.ngari.recipe.drug.model;


import ctd.schema.annotation.Schema;

import java.io.Serializable;

/**
 * company: ngarihealth
 *
 * @author: 0184/yu_yun
 */
@Schema
public class DrugListAndSaleDrugListDTO implements Serializable {

    private static final long serialVersionUID = 170058770800651017L;

    private DrugListBean drugList;
    private SaleDrugListDTO saleDrugList;
    /**机构是否已有药品关联该平台药品*/
    private Boolean canAssociated ;

    /**改药企对应的机构Id*/
    private Integer organId ;
    public DrugListAndSaleDrugListDTO() {
    }

    public DrugListBean getDrugList() {
        return drugList;
    }

    public void setDrugList(DrugListBean drugList) {
        this.drugList = drugList;
    }

    public SaleDrugListDTO getSaleDrugList() {
        return saleDrugList;
    }

    public void setSaleDrugList(SaleDrugListDTO saleDrugList) {
        this.saleDrugList = saleDrugList;
    }

    public Boolean getCanAssociated() {
        return canAssociated;
    }

    public void setCanAssociated(Boolean canAssociated) {
        this.canAssociated = canAssociated;
    }

    public Integer getOrganId() {
        return organId;
    }

    public void setOrganId(Integer organId) {
        this.organId = organId;
    }
}

