package recipe.drugsenterprise.bean;

import ctd.schema.annotation.Schema;

import java.math.BigDecimal;

/**
 * company: ngarihealth
 * @author: 0184/yu_yun
 * date:2017/7/3.
 */
@Schema
public class YnsScanStockBean {

    /**
     * 药品编码
     */
    private String inventory;

    public String isInventory() {
        return inventory;
    }

    public void setInventory(String inventory) {
        this.inventory = inventory;
    }
}
