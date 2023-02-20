package recipe.dao.comment;

import com.ngari.recipe.entity.comment.RecipeComment;
import ctd.persistence.annotation.DAOMethod;
import ctd.persistence.annotation.DAOParam;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.util.annotation.RpcSupportDAO;

import java.util.List;

@RpcSupportDAO
public abstract class RecipeCommentDAO extends HibernateSupportDelegateDAO<RecipeComment> {

    public RecipeCommentDAO() {
        super();
        this.setEntityName(RecipeComment.class.getName());
        this.setKeyField("id");
    }

    @DAOMethod(sql = " from RecipeComment where recipeId=:recipeId order by id desc")
    public abstract List<RecipeComment> findRecipeCommentByRecipeId(@DAOParam("recipeId") Integer recipeId);

    @DAOMethod(sql = " from RecipeComment where recipeId in (:recipeIds) order by id desc", limit = 0)
    public abstract List<RecipeComment> findCommentByRecipeIds(@DAOParam("recipeIds") List<Integer> recipeIds);

}
