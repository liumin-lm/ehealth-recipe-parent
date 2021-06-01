package com.ngari.recipe.drug.model;

/**
 * 当搜索药品时，需要去医院查询对应药品库存。
 * 实现该接口指明医院药品查询条件以及和医院库存返回药品匹配的条件。
 * 并根据匹配条件查询从医院库存返回药品找到对应物品，并设置药品搜索结果的库存。
 * <p>注意：目前通过前置机开发人员确认：只需上送机构（医院）药品编码OrganDrugCode，
 * 针对一个药品编码OrganDrugCode返回>1条不同规格的药品时，参照前置机校验库存的做法：
 * 匹配时也只需按机构（医院）药品编码去匹配，不考虑同种药品的不同规格
 * （前置机做药品库存是否充足时也是不考虑规格的）</>
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
     * 机构(医院)药品编码
     * @return 机构(医院)药品编码
     */
    String getOrganDrugCode();

}
