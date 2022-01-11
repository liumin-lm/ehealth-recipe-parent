package recipe.thread;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.ngari.recipe.recipe.model.RecipeBean;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import ctd.util.AppContextHolder;
import eh.recipeaudit.api.IAuditMedicinesService;
import eh.recipeaudit.api.IRecipeAuditService;
import eh.recipeaudit.model.AuditMedicineIssueBean;
import eh.recipeaudit.model.AuditMedicinesBean;
import eh.recipeaudit.model.Intelligent.AutoAuditResultBean;
import eh.recipeaudit.model.Intelligent.IssueBean;
import eh.recipeaudit.model.Intelligent.PAWebMedicinesBean;
import eh.recipeaudit.model.Intelligent.PAWebRecipeDangerBean;
import eh.recipeaudit.model.recipe.RecipeDTO;
import eh.recipeaudit.model.recipe.RecipeDetailDTO;
import eh.recipeaudit.util.RecipeAuditAPI;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.ApplicationUtils;
import recipe.audit.service.PrescriptionService;
import recipe.util.ObjectCopyUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author： 0184/yu_yun
 * @date： 2018/11/27
 * @description： 保存审方信息
 * @version： 1.0
 */
public class SaveAutoReviewRunnable implements Runnable {

    /**
     * logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(SaveAutoReviewRunnable.class);

    private RecipeBean recipe;

    private List<RecipeDetailBean> details;

    public SaveAutoReviewRunnable(RecipeBean recipe, List<RecipeDetailBean> details) {
        this.recipe = recipe;
        this.details = details;
    }

    @Override
    public void run() {
        RecipeDTO recipeDTO = ObjectCopyUtils.convert(recipe, RecipeDTO.class);
        List<RecipeDetailDTO> detailsList = ObjectCopyUtils.convert(details, RecipeDetailDTO.class);
        LOGGER.info("SaveAutoReviewRunnable start RecipeDTO={},detailsList={}",JSON.toJSONString(recipeDTO),JSON.toJSONString(detailsList));
        try{
            IRecipeAuditService recipeAuditService = RecipeAuditAPI.getService(IRecipeAuditService.class, "recipeAuditServiceImpl");
            recipeAuditService.saveAutoReview(recipeDTO,detailsList);
        }catch(Exception e){
            LOGGER.info("SaveAutoReviewRunnable recipe={} exception",JSON.toJSONString(recipe),e);
        }finally {
            LOGGER.info("SaveAutoReviewRunnable end recipe={}",JSON.toJSONString(recipe));
        }
    }

}
