package recipe.atop.greenroom;

import ctd.util.annotation.RpcBean;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.core.api.greenroom.ITextService;

/**
 * 用于postman 后门接口调用
 * @author fuzi
 */
@RpcBean(value = "textGmAtop")
public class TextGmAtop {
    @Autowired
    private ITextService textBusinessService;

    public void updateAddressPdfExecute(Integer recipeId) {
        textBusinessService.updateAddressPdfExecute(recipeId);
    }
}
