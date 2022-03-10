package recipe.core.api.greenroom;


/**
 * 用于postman 后门接口调用
 * @author fuzi
 */
public interface ITextService {
    /**
     * 支付成功后修改pdf 添加收货人信息/煎法
     *
     * @param recipeId
     */
    void updateAddressPdfExecute(Integer recipeId);
}
