package recipe.text;

import org.springframework.beans.factory.annotation.Autowired;
import recipe.caNew.pdf.CreatePdfFactory;
import recipe.core.api.greenroom.ITextService;

/**
 * 用于postman 后门接口调用
 * @author fuzi
 */
public class TextService implements ITextService {
    @Autowired
    private CreatePdfFactory createPdfFactory;
    @Override
    public void updateAddressPdfExecute(Integer recipeId) {
        createPdfFactory.updateAddressPdfExecute(recipeId);
    }
}
