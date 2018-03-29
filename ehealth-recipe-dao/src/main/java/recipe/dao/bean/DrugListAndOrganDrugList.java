package recipe.dao.bean;


import com.ngari.recipe.entity.DrugList;
import com.ngari.recipe.entity.OrganDrugList;

/**
 * company: ngarihealth
 * @author: 0184/yu_yun
 */
public class DrugListAndOrganDrugList {
    private DrugList drugList;
    private OrganDrugList organDrugList;

    public DrugListAndOrganDrugList() {
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
}
