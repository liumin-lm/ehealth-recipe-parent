package recipe.core.api;

import com.ngari.recipe.entity.OrganDrugList;

/**
 * @author zgy
 * @date 2022/5/24 13:52
 */
public interface IOrganDrugBusinessService {

    /**
     * 添加机构药品列表销售策略
     * @param salesStrategy 销售策略
     */
    void addOrganDrugSalesStrategy(OrganDrugList organDrugList);

}
