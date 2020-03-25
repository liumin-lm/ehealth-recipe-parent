package recipe.drugsenterprise.bean;

import ctd.schema.annotation.Schema;

import java.util.List;

/**
 * company: ngarihealth
 * @author: 0184/yu_yun
 * date:2017/7/3.
 */
@Schema
public class LxScanStockBean {
    private List drugList;
    public List getDrugList() {
        return drugList;
    }

    public void setDrugList(List drugList) {
        this.drugList = drugList;
    }
}
