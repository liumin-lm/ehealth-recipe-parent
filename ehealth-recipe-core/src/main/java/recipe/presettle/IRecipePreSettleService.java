package recipe.presettle;

import java.util.Map;

/**
 * created by shiyuping on 2020/11/27
 * 处方预结算接口
 * @author shiyuping
 */
public interface IRecipePreSettleService {

    /**
     * 处方预结算方法
     * @param recipeId 处方id
     * @param extInfo 扩展参数
     * @return
     */
    Map<String, Object> recipePreSettle(Integer recipeId,Map<String, Object> extInfo);
}
