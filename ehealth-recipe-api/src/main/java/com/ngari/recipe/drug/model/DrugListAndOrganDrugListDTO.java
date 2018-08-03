package com.ngari.recipe.drug.model;


import ctd.schema.annotation.Schema;

import java.io.Serializable;

/**
 * company: ngarihealth
 *
 * @author: 0184/yu_yun
 */
@Schema
public class DrugListAndOrganDrugListDTO implements Serializable {

    private static final long serialVersionUID = -7789104163606842050L;

    private DrugListBean drugList;
    private OrganDrugListDTO organDrugList;

    public DrugListAndOrganDrugListDTO() {
    }


    public DrugListBean getDrugList() {
        return drugList;
    }

    public void setDrugList(DrugListBean drugList) {
        this.drugList = drugList;
    }

    public OrganDrugListDTO getOrganDrugList() {
        return organDrugList;
    }

    public void setOrganDrugList(OrganDrugListDTO organDrugList) {
        this.organDrugList = organDrugList;
    }
}
