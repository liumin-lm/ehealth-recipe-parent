package recipe.atop.doctor;

import com.alibaba.fastjson.JSON;
import com.ngari.recipe.comment.model.RecipeCommentTO;
import com.ngari.recipe.entity.comment.RecipeComment;
import ctd.account.UserRoleToken;
import ctd.persistence.exception.DAOException;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.atop.BaseAtop;
import com.ngari.recipe.comment.service.IRecipeCommentService;
import recipe.util.ObjectCopyUtils;

import javax.annotation.Resource;
import java.util.Date;
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
        validateAtop(recipeCommentTO, recipeCommentTO.getRecipeId());
        UserRoleToken urt = UserRoleToken.getCurrent();
        logger.info("addRecipeComment urt = {}", JSON.toJSONString(urt));
        if (Objects.isNull(urt)) {
            throw new DAOException("未获取到点评用户信息！");
        }
        if (Objects.isNull(recipeCommentTO.getCommentResultCode()) || StringUtils.isEmpty(recipeCommentTO.getCommentResult())) {
            throw new DAOException("请选择点评结果！");
        }
        if (Objects.nonNull(recipeCommentTO.getId())) {
            recipeCommentTO.setId(null);
        }
        recipeCommentTO.setCommentUserName(urt.getUserName());
        recipeCommentTO.setCommentUserUrt(urt.getId().toString());
        recipeCommentTO.setCommentUserType(urt.getRoleId());
        recipeCommentTO.setCreateDate(new Date());
        recipeCommentTO.setLastModify(new Date());
        return recipeCommentService.addRecipeComment(recipeCommentTO);
    }

    @RpcService
    public RecipeCommentTO getRecipeCommentByRecipeId(Integer recipeId) {
        return recipeCommentService.getRecipeCommentByRecipeId(recipeId);
    }
}
