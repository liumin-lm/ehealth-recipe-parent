package recipe.text;

import com.ngari.base.esign.model.CoOrdinateVO;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.caNew.pdf.CreatePdfFactory;
import recipe.core.api.greenroom.ITextService;
import recipe.manager.RedisManager;

import java.util.Collections;

/**
 * 用于postman 后门接口调用
 *
 * @author fuzi
 */
public class TextService implements ITextService {
    @Autowired
    private CreatePdfFactory createPdfFactory;
    @Autowired
    private RedisManager redisManager;

    @Override
    public void coOrdinate(Integer recipeId, CoOrdinateVO ordinateVO) {
        redisManager.coOrdinate(recipeId, Collections.singletonList(ordinateVO));
    }

    @Override
    public void updateAddressPdfExecute(Integer recipeId) {
        createPdfFactory.updateAddressPdfExecute(recipeId);
    }


}
