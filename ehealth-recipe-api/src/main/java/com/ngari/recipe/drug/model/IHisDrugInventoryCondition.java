package com.ngari.recipe.drug.model;

/**
 * @author Zhou Wenfei
 * date 2020/11/18 20:56
 */
public interface IHisDrugInventoryCondition {
    /**
     * 药品id
     * @return 药品id
     */
    Integer getDrugId();

    /**
     * 药品机构编码
     * @return 药品机构编码
     */
    String getOrganDrugCode();

}
