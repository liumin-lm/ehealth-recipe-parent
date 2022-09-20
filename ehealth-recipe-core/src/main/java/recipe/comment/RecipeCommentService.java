package recipe.comment;

import com.google.common.collect.Lists;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.comment.model.RecipeCommentTO;
import com.ngari.recipe.comment.service.IRecipeCommentService;
import com.ngari.recipe.entity.comment.RecipeComment;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.dao.comment.RecipeCommentDAO;

import java.util.Date;
import java.util.List;

@RpcBean
public class RecipeCommentService implements IRecipeCommentService {

    @Autowired
    private RecipeCommentDAO recipeCommentDAO;

    @RpcService
    public void saveRecipeComment(Integer recipeId, String commentResult, String commentReamrk){
        RecipeComment recipeComment = new RecipeComment();
        recipeComment.setRecipeId(recipeId);
        recipeComment.setCommentResult(commentResult);
        recipeComment.setCommentRemark(commentReamrk);
        Date now = new Date();
        recipeComment.setCreateDate(now);
        recipeComment.setLastmodify(now);
        recipeCommentDAO.save(recipeComment);
    }

    @RpcService
    public RecipeComment getRecipeCommentByRecipeId(Integer recipeId){
        List<RecipeComment> list = recipeCommentDAO.findRecipeCommentByRecipeId(recipeId);
        if (list != null && list.size() > 0) {
            return list.get(0);
        }
        return null;
    }

    @Override
    public List<RecipeCommentTO> findCommentByRecipeIds(List<Integer> recipeIds) {
        if(recipeIds==null || recipeIds.size()==0){
            return Lists.newArrayList();
        }
        List<RecipeComment> list = recipeCommentDAO.findCommentByRecipeIds(recipeIds);
        return  ObjectCopyUtils.convert(list, RecipeCommentTO.class);
    }
}
