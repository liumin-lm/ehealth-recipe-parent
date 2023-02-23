package recipe.comment;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.ngari.his.regulation.entity.RegulationNotifyDataReq;
import com.ngari.his.regulation.service.IRegulationService;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.platform.regulation.mode.QueryRegulationUnitReq;
import com.ngari.recipe.comment.model.RecipeCommentTO;
import com.ngari.recipe.comment.model.RegulationRecipeCommentBean;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.comment.RecipeComment;
import ctd.spring.AppDomainContext;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import com.ngari.recipe.comment.service.IRecipeCommentService;
import recipe.dao.RecipeDAO;
import recipe.dao.comment.RecipeCommentDAO;
import recipe.presettle.condition.DoctorForceCashHandler;

import java.util.Date;
import java.util.List;

@RpcBean
public class RecipeCommentService implements IRecipeCommentService {
    private static final Logger logger = LoggerFactory.getLogger(DoctorForceCashHandler.class);

    @Autowired
    private RecipeCommentDAO recipeCommentDAO;

    @Autowired
    private RecipeDAO recipeDAO;


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
        Recipe recipe = recipeDAO.getByRecipeId(recipeComment.getRecipeId());

        Integer id = recipeCommentDAO.save(recipeComment).getId();
        IRegulationService iRegulationService = AppDomainContext.getBean("his.regulationService", IRegulationService.class);
        RegulationNotifyDataReq req = new RegulationNotifyDataReq();
        req.setBussId(id.toString());
        req.setBussType("recipeComment");
        req.setNotifyTime(System.currentTimeMillis() - 1000);
        req.setOrganId(recipe.getClinicOrgan());
        logger.info("addRecipeComment notifyData req = {}", JSON.toJSONString(req));
        iRegulationService.notifyData(recipe.getClinicOrgan(), req);
        return id;
    }
}
