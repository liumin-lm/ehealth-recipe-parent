package recipe.atop.greenroom;

import com.ngari.base.esign.model.CoOrdinateVO;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.core.api.greenroom.ITextService;
import recipe.util.DictionaryUtil;

/**
 * 用于postman 后门接口调用
 *
 * @author fuzi
 */
@RpcBean(value = "textGmAtop")
public class TextGmAtop {
    @Autowired
    private ITextService textBusinessService;

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


    public String getDictionary(String classId, String key) {
        //"eh.base.dictionary.Gender"
        return DictionaryUtil.getDictionary(classId, key);

    }
}
