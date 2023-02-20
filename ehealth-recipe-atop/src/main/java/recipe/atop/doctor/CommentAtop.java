package recipe.atop.doctor;

import com.alibaba.fastjson.JSON;
import com.ngari.recipe.comment.model.RecipeCommentTO;
import com.ngari.recipe.entity.comment.RecipeComment;
import ctd.account.UserRoleToken;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.atop.BaseAtop;
import recipe.core.api.IRecipeCommentService;
import recipe.util.ObjectCopyUtils;
import recipe.util.ValidateUtil;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * @Description
 * @Author yzl
 * @Date 2023-02-20
 */
@RpcBean("commentAtop")
public class CommentAtop extends BaseAtop {
    private static final Logger logger = LoggerFactory.getLogger(CommentAtop.class);

    @Resource
    IRecipeCommentService recipeCommentService;

    @RpcService
    public Integer addRecipeComment(RecipeCommentTO recipeCommentTO) {
        logger.info("addRecipeComment recipeCommentTO = {}", JSON.toJSONString(recipeCommentTO));
        validateAtop(recipeCommentTO, recipeCommentTO.getRecipeId(), recipeCommentTO.getCommentResultCode());
        UserRoleToken urt = UserRoleToken.getCurrent();
        logger.info("addRecipeComment urt = {}", JSON.toJSONString(urt));
        if (Objects.nonNull(recipeCommentTO.getId())) {
            recipeCommentTO.setId(null);
        }
        RecipeComment recipeComment = ObjectCopyUtils.convert(recipeCommentTO, RecipeComment.class);
        recipeComment.setCreateDate(new Date());
        recipeComment.setLastModify(new Date());
        return recipeCommentService.addRecipeComment(recipeComment);
    }

    @RpcService
    public RecipeCommentTO getRecipeCommentByRecipeId(Integer recipeId) {
        logger.info("getRecipeCommentByRecipeId recipeId={}", recipeId);
        return recipeCommentService.getRecipeCommentByRecipeId(recipeId);
    }
}
