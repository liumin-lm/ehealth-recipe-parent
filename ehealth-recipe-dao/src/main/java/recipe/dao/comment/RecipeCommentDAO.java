package recipe.dao.comment;

import com.ngari.recipe.entity.comment.RecipeComment;
import com.ngari.recipe.entity.sign.SignDoctorCaInfo;
import ctd.persistence.annotation.DAOMethod;
import ctd.persistence.annotation.DAOParam;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.util.annotation.RpcSupportDAO;

import java.util.List;

@RpcSupportDAO
public abstract class RecipeCommentDAO extends HibernateSupportDelegateDAO<RecipeComment> {

    public RecipeCommentDAO(){
        super();
        this.setEntityName(RecipeComment.class.getName());
        this.setKeyField("id");
    }

    @DAOMethod(sql = " from RecipeComment where recipeId=:recipeId")
    public abstract List<RecipeComment> findRecipeCommentByRecipeId(@DAOParam("recipeId")Integer recipeId);
}
