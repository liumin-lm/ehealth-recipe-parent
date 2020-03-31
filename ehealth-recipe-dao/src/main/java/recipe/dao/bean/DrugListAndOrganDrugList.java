package recipe.dao.bean;


import com.ngari.recipe.drug.model.DepSaleDrugInfo;
import com.ngari.recipe.entity.DrugList;
import com.ngari.recipe.entity.OrganDrugList;

import java.util.List;

/**
 * company: ngarihealth
 * @author: 0184/yu_yun
 */
public class DrugListAndOrganDrugList {
    private DrugList drugList;
    private OrganDrugList organDrugList;
    /**药品药企配送信息--药企名-药企编码*/
    private List<DepSaleDrugInfo> depSaleDrugInfos;
    /**是否可配送*/
    private Boolean canDrugSend;

    public DrugListAndOrganDrugList() {
    }

    public List<DepSaleDrugInfo> getDepSaleDrugInfos() {
        return depSaleDrugInfos;
    }

    public void setDepSaleDrugInfos(List<DepSaleDrugInfo> depSaleDrugInfos) {
        this.depSaleDrugInfos = depSaleDrugInfos;
    }

    public DrugListAndOrganDrugList(DrugList drugList, OrganDrugList organDrugList) {
        this.drugList = drugList;
        this.organDrugList = organDrugList;
    }

    public DrugList getDrugList() {
        return drugList;
    }

    public void setDrugList(DrugList drugList) {
        this.drugList = drugList;
    }

    public OrganDrugList getOrganDrugList() {
        return organDrugList;
    }

    public void setOrganDrugList(OrganDrugList organDrugList) {
        this.organDrugList = organDrugList;
    }

    public Boolean getCanDrugSend() {
        return canDrugSend;
    }

    public void setCanDrugSend(Boolean canDrugSend) {
        this.canDrugSend = canDrugSend;
    }
}
