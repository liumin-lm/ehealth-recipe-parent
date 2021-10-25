package recipe.factoryManager.button;

import com.ngari.recipe.dto.GiveModeShowButtonDTO;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeExtend;

/**
 * todo 老代码迁移 新写代码类似结构需谨慎使用
 * @author yinsheng
 * @date 2020\12\3 0003 20:04
 */
public interface IGiveModeBase {
    /**
     * 通过机构ID从运营平台获取购药方式的基本配置项
     * @param organId 机构ID
     * @return        运营平台的配置项
     */
    GiveModeShowButtonDTO getGiveModeSettingFromYypt(Integer organId);

    /**
     * 设置平台和互联网关于按钮个性化显示
     * @param giveModeShowButtonVO
     * @param recipe
     * @param recipeExtend
     */
    void setSpecialItem(GiveModeShowButtonDTO giveModeShowButtonVO, Recipe recipe, RecipeExtend recipeExtend);

    /**
     * 获取按钮对象
     *
     * @param recipe
     * @return
     */
    GiveModeShowButtonDTO getShowButton(Recipe recipe);

    GiveModeShowButtonDTO getShowButtonV1(Recipe recipe);

}
