package recipe.text;

import com.ngari.base.esign.model.CoOrdinateVO;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.Recipedetail;
import com.ngari.recipe.vo.FastRecipeAndDetailResVO;
import com.ngari.recipe.vo.FastRecipeDetailVO;
import com.ngari.recipe.vo.FastRecipeReqVO;
import com.ngari.recipe.vo.FastRecipeResVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.caNew.pdf.CreatePdfFactory;
import recipe.client.ConsultClient;
import recipe.core.api.greenroom.ITextService;
import recipe.dao.RecipeDAO;
import recipe.dao.RecipeDetailDAO;
import recipe.manager.RedisManager;
import recipe.util.ObjectCopyUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 用于postman 后门接口调用
 *
 * @author fuzi
 */
@Service
public class TextService implements ITextService {
    @Autowired
    private CreatePdfFactory createPdfFactory;
    @Autowired
    private RedisManager redisManager;
    @Autowired
    private ConsultClient consultClient;
    @Autowired
    private RecipeDAO recipeDAO;
    @Autowired
    private RecipeDetailDAO recipeDetailDAO;

    @Override
    public void coOrdinate(Integer recipeId, CoOrdinateVO ordinateVO) {
        redisManager.coOrdinate(recipeId, Collections.singletonList(ordinateVO));
    }

    @Override
    public void updateAddressPdfExecute(Integer recipeId) {
        createPdfFactory.updateAddressPdfExecute(recipeId);
    }

    @Override
    public void getConsult(Integer consultId) {
        consultClient.getConsult(consultId);
    }

    @Override
    public FastRecipeAndDetailResVO getFastRecipeJson(FastRecipeReqVO fastRecipeReqVO) {
        Recipe recipe = recipeDAO.getByRecipeId(fastRecipeReqVO.getRecipeId());
        FastRecipeAndDetailResVO fastRecipeAndDetailResVO = new FastRecipeAndDetailResVO();
        FastRecipeResVO fastRecipeResVO = new FastRecipeResVO();
        ObjectCopyUtils.copyProperties(fastRecipeResVO, recipe);
        fastRecipeAndDetailResVO.setFastRecipeResVO(fastRecipeResVO);
        List<FastRecipeDetailVO> fastRecipeDetailList = new ArrayList<>();
        List<Recipedetail> recipeDetailList = recipeDetailDAO.findByRecipeId(fastRecipeReqVO.getRecipeId());
        recipeDetailList.forEach(recipeDetail -> {
            FastRecipeDetailVO fastRecipeDetailVO = new FastRecipeDetailVO();
            ObjectCopyUtils.copyProperties(fastRecipeDetailVO, recipeDetail);
            fastRecipeDetailList.add(fastRecipeDetailVO);
        });
        fastRecipeAndDetailResVO.setFastRecipeDetailList(fastRecipeDetailList);
        fastRecipeAndDetailResVO.setTitle(fastRecipeReqVO.getTitle());
        fastRecipeAndDetailResVO.setBackgroundImg(fastRecipeReqVO.getBackgroundImg());
        fastRecipeAndDetailResVO.setIntroduce(fastRecipeReqVO.getIntroduce());
        return fastRecipeAndDetailResVO;
    }
}
