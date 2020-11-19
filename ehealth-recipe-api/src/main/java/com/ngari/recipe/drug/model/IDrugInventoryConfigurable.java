package com.ngari.recipe.drug.model;

import java.util.List;

/**
 * @author Zhou Wenfei
 * date 2020/11/18 20:56
 */
public interface IDrugInventoryConfigurable {

    /**
     * 设置药品库存
     * @param inventories
     */
    void setInventories(List<DrugInventoryInfo> inventories);
}
