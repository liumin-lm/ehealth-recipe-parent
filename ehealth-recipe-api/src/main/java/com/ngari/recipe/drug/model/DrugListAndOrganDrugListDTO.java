package com.ngari.recipe.drug.model;


import ctd.schema.annotation.Schema;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

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
    /**药品药企配送信息--药企名-药企编码*/
    private List<DepSaleDrugInfo> depSaleDrugInfos;
    /**是否可配送*/
    private Boolean canDrugSend;

    /**机构是否已有药品关联该平台药品*/
    private Boolean canAssociated ;

    public List<DepSaleDrugInfo> getDepSaleDrugInfos() {
        return depSaleDrugInfos;
    }

    public void setDepSaleDrugInfos(List<DepSaleDrugInfo> depSaleDrugInfos) {
        this.depSaleDrugInfos = depSaleDrugInfos;
    }

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

    public Boolean getCanDrugSend() {
        return canDrugSend;
    }

    public void setCanDrugSend(Boolean canDrugSend) {
        this.canDrugSend = canDrugSend;
    }

    public Boolean getCanAssociated() {
        return canAssociated;
    }

    public void setCanAssociated(Boolean canAssociated) {
        this.canAssociated = canAssociated;
    }
}
