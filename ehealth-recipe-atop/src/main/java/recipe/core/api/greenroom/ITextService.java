package recipe.core.api.greenroom;


import com.ngari.base.esign.model.CoOrdinateVO;
import com.ngari.recipe.vo.FastRecipeAndDetailResVO;
import com.ngari.recipe.vo.FastRecipeReqVO;

/**
 * 用于postman 后门接口调用
 *
 * @author fuzi
 */
public interface ITextService {
    /**
     * 手动录入坐标缓存
     *
     * @param recipeId
     */
    void coOrdinate(Integer recipeId, CoOrdinateVO ordinateVO);

    /**
     * 支付成功后修改pdf 添加收货人信息/煎法
     *
     * @param recipeId
     */
    void updateAddressPdfExecute(Integer recipeId);

    /**
     * 获取咨询数据
     *
     * @param consultId
     */
    void getConsult(Integer consultId);

    /**
     * 快捷购药获取模板数据
     *
     * @param fastRecipeReqVO
     * @return
     */
    FastRecipeAndDetailResVO getFastRecipeJson(FastRecipeReqVO fastRecipeReqVO);

    void generateRecipePdf(Integer recipeId);

    void signFileByte(String organSealId);
}
