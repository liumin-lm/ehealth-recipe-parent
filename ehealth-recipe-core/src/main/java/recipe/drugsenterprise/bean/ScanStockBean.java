package recipe.drugsenterprise.bean;

import ctd.schema.annotation.Schema;

/**
 * company: ngarihealth
 * @author: 0184/yu_yun
 * date:2017/7/3.
 */
@Schema
public class ScanStockBean {

    /**
     * 药品编码
     */
    private String drugCode;
    /**
     * 有无库存
     */
    private String inventory;

    public String getDrugCode() {
        return drugCode;
    }

    public void setDrugCode(String drugCode) {
        this.drugCode = drugCode;
    }

    public String getInventory() {
        return inventory;
    }

    public void setInventory(String inventory) {
        this.inventory = inventory;
    }
}
