package recipe.dao.bean;


import com.ngari.recipe.entity.DrugList;
import com.ngari.recipe.entity.SaleDrugList;

/**
 * company: ngarihealth
 * @author: 0184/yu_yun
 */
public class DrugListAndSaleDrugList {
    private DrugList drugList;
    private SaleDrugList saleDrugList;

    public DrugListAndSaleDrugList() {
    }

    public DrugListAndSaleDrugList(DrugList drugList, SaleDrugList saleDrugList) {
        this.drugList = drugList;
        this.saleDrugList = saleDrugList;
    }

    public DrugList getDrugList() {
        return drugList;
    }

    public void setDrugList(DrugList drugList) {
        this.drugList = drugList;
    }

    public SaleDrugList getSaleDrugList() {
        return saleDrugList;
    }

    public void setSaleDrugList(SaleDrugList saleDrugList) {
        this.saleDrugList = saleDrugList;
    }
}

