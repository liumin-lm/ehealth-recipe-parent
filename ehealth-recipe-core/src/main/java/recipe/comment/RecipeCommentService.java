package recipe.comment;

import com.google.common.collect.Lists;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.platform.regulation.mode.QueryRegulationUnitReq;
import com.ngari.recipe.comment.model.RecipeCommentTO;
import com.ngari.recipe.comment.model.RegulationRecipeCommentBean;
import com.ngari.recipe.entity.comment.RecipeComment;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import com.ngari.recipe.comment.service.IRecipeCommentService;
import recipe.dao.comment.RecipeCommentDAO;

import java.util.Date;
import java.util.List;

@RpcBean
public class RecipeCommentService implements IRecipeCommentService {

    @Autowired
    private RecipeCommentDAO recipeCommentDAO;

    @Deprecated
    @RpcService
    public void saveRecipeComment(Integer recipeId, String commentResult, String commentRemrk) {
        RecipeComment recipeComment = new RecipeComment();
        recipeComment.setRecipeId(recipeId);
        recipeComment.setCommentResult(commentResult);
        recipeComment.setCommentRemark(commentRemrk);
        Date now = new Date();
        recipeComment.setCreateDate(now);
        recipeComment.setLastModify(now);
        recipeCommentDAO.save(recipeComment);
    }

    @Override
    @RpcService
    public RecipeCommentTO getRecipeCommentByRecipeId(Integer recipeId) {
        List<RecipeComment> recipeCommentList = recipeCommentDAO.findRecipeCommentByRecipeId(recipeId);
        if (CollectionUtils.isNotEmpty(recipeCommentList)) {
            return ObjectCopyUtils.convert(recipeCommentList.get(0), RecipeCommentTO.class);
        }
        return null;
    }

    @Override
    public List<RegulationRecipeCommentBean> queryRegulationRecipeComment(QueryRegulationUnitReq req) {
        return Lists.newArrayList();
    }

    @Override
    public List<RecipeCommentTO> findCommentByRecipeIds(List<Integer> recipeIds) {
        if (CollectionUtils.isEmpty(recipeIds)) {
            return Lists.newArrayList();
        }
        List<RecipeComment> list = recipeCommentDAO.findCommentByRecipeIds(recipeIds);
        return ObjectCopyUtils.convert(list, RecipeCommentTO.class);
    }

    @Override
    public Integer addRecipeComment(RecipeCommentTO recipeCommentTO) {
        RecipeComment recipeComment = ObjectCopyUtils.convert(recipeCommentTO, RecipeComment.class);
        return recipeCommentDAO.save(recipeComment).getId();
    }
}
