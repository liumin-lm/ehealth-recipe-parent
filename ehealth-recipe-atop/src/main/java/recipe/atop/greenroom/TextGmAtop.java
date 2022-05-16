package recipe.atop.greenroom;

import com.alibaba.fastjson.JSON;
import com.ngari.base.esign.model.CoOrdinateVO;
import com.ngari.recipe.vo.FastRecipeAndDetailResVO;
import com.ngari.recipe.vo.FastRecipeReqVO;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.core.api.IDrugBusinessService;
import recipe.core.api.greenroom.ITextService;
import recipe.util.DictionaryUtil;

import javax.annotation.Resource;

/**
 * 用于postman 后门接口调用
 *
 * @author fuzi
 */
@RpcBean(value = "textGmAtop")
public class TextGmAtop {
    @Autowired
    private ITextService textBusinessService;
    @Resource
    private IDrugBusinessService drugBusinessService;

    @RpcService
    public void coOrdinate(Integer recipeId, CoOrdinateVO ordinateVO) {
        textBusinessService.coOrdinate(recipeId, ordinateVO);
    }

    @RpcService
    public void updateAddressPdfExecute(Integer recipeId) {
        textBusinessService.updateAddressPdfExecute(recipeId);
    }

    @RpcService
    public void getConsult(Integer consultId) {
        textBusinessService.getConsult(consultId);
    }

    @RpcService
    public String getDictionary(String classId, String key) {
        return DictionaryUtil.getDictionary(classId, key);
    }

    @RpcService
    public FastRecipeAndDetailResVO getFastRecipeJson(FastRecipeReqVO fastRecipeReqVO){
        return textBusinessService.getFastRecipeJson(fastRecipeReqVO);
    }

    @RpcService(timeout = 3600)
    public void queryRemindRecipe(String dateTime){
        drugBusinessService.queryRemindRecipe(dateTime);
    }
}
