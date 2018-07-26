package com.ngari.recipe.drug.model;


import java.io.Serializable;

/**
 * company: ngarihealth
 *
 * @author: 0184/yu_yun
 */
public class DrugListAndSaleDrugListDTO implements Serializable {

    private static final long serialVersionUID = 170058770800651017L;

    private DrugListBean drugList;
    private SaleDrugListDTO saleDrugList;

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
}

