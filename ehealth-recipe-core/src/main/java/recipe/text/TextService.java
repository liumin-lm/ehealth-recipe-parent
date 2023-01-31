package recipe.text;

import com.ngari.base.esign.model.CoOrdinateVO;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeExtend;
import com.ngari.recipe.entity.Recipedetail;
import com.ngari.recipe.vo.*;
import ctd.persistence.exception.DAOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.bussutil.CreateRecipePdfUtil;
import recipe.caNew.pdf.CreatePdfFactory;
import recipe.client.ConsultClient;
import recipe.constant.ErrorCode;
import recipe.core.api.greenroom.ITextService;
import recipe.dao.RecipeDAO;
import recipe.dao.RecipeDetailDAO;
import recipe.dao.RecipeExtendDAO;
import recipe.enumerate.type.RecipeTypeEnum;
import recipe.manager.RedisManager;
import recipe.thread.RecipeBusiThreadPool;
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
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
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
    @Autowired
    private RecipeExtendDAO recipeExtendDAO;

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
        if (RecipeTypeEnum.RECIPETYPE_TCM.getType().equals(recipe.getRecipeType())) {
            FastRecipeExtend fastRecipeExtend = new FastRecipeExtend();
            RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipe.getRecipeId());
            ObjectCopyUtils.copyProperties(fastRecipeExtend, recipeExtend);
            fastRecipeResVO.setRecipeExtend(fastRecipeExtend);
        }
        fastRecipeAndDetailResVO.setRecipeBean(fastRecipeResVO);
        List<FastRecipeDetailVO> fastRecipeDetailList = new ArrayList<>();
        List<Recipedetail> recipeDetailList = recipeDetailDAO.findByRecipeId(fastRecipeReqVO.getRecipeId());
        recipeDetailList.forEach(recipeDetail -> {
            FastRecipeDetailVO fastRecipeDetailVO = new FastRecipeDetailVO();
            ObjectCopyUtils.copyProperties(fastRecipeDetailVO, recipeDetail);
            fastRecipeDetailVO.setType(1);
            fastRecipeDetailList.add(fastRecipeDetailVO);
        });
        fastRecipeAndDetailResVO.setDetailBeanList(fastRecipeDetailList);
        fastRecipeAndDetailResVO.setTitle(fastRecipeReqVO.getTitle());
        fastRecipeAndDetailResVO.setBackgroundImg(fastRecipeReqVO.getBackgroundImg());
        fastRecipeAndDetailResVO.setIntroduce(fastRecipeReqVO.getIntroduce());
        return fastRecipeAndDetailResVO;
    }

    @Override
    public void generateRecipePdf(Integer recipeId) {
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        try {
            createPdfFactory.generateRecipePdf(recipe);
        } catch (Exception e) {
            logger.info("TextService generateRecipePdf ", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        }
    }

    @Override
    public void signFileByte(String organSealId) {
        for (int i = 0; i < 10; i++) {
            int finalI = i;
            RecipeBusiThreadPool.execute(() -> {
                long start = System.currentTimeMillis();
                logger.info("TextService signFileByte i={} start= {}", finalI, start);
                byte[] b = null;
                try {
                    b = CreateRecipePdfUtil.signFileByte(organSealId);
                } catch (Exception e) {
                    logger.error("TextService signFileByte error  i={}", finalI, e);
                } finally {
                    Long end = System.currentTimeMillis() - start;
                    if (null != b) {
                        logger.info("TextService signFileByte i={} b= {},end={}", finalI, b.length, end);
                    } else {
                        logger.info("TextService signFileByte i={} end={}", finalI, end);
                    }
                }
            });
        }
    }
}
