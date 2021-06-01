package recipe.comment;

import com.ngari.recipe.entity.comment.RecipeComment;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.dao.comment.RecipeCommentDAO;

import java.util.Date;
import java.util.List;

@RpcBean
public class RecipeCommentService {

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
}
