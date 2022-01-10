package recipe.client;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.entity.*;
import ctd.persistence.exception.DAOException;
import ctd.util.JSONUtils;
import eh.recipeaudit.api.IAuditMedicinesService;
import eh.recipeaudit.api.IRecipeAuditService;
import eh.recipeaudit.api.IRecipeCheckDetailService;
import eh.recipeaudit.api.IRecipeCheckService;
import eh.recipeaudit.model.AuditMedicineIssueBean;
import eh.recipeaudit.model.AuditMedicinesBean;
import eh.recipeaudit.model.Intelligent.AutoAuditResultBean;
import eh.recipeaudit.model.Intelligent.PAWebRecipeDangerBean;
import eh.recipeaudit.model.RecipeCheckBean;
import eh.recipeaudit.model.RecipeCheckDetailBean;
import eh.recipeaudit.model.recipe.RecipeDTO;
import eh.recipeaudit.model.recipe.RecipeDetailDTO;
import eh.recipeaudit.model.recipe.RecipeExtendDTO;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 审方相关服务
 *
 * @Author liumin
 * @Date 2021/7/22 下午2:26
 * @Description
 */
@Service
public class RecipeAuditClient extends BaseClient {
    private static final Integer CA_FAIL = 0;
    @Autowired
    private IRecipeCheckService recipeCheckService;

    @Autowired
    private IRecipeAuditService recipeAuditService;

    @Autowired
    private IAuditMedicinesService iAuditMedicinesService;

    @Autowired
    private IRecipeCheckDetailService recipeCheckDetailService;

    /**
     * 通过处方号获取审方信息
     *
     * @param recipeId
     * @return
     */
    public RecipeCheckBean getByRecipeId(Integer recipeId) {
        logger.info("RecipeAuditClient getByRecipeId param recipeId:{}", recipeId);
        RecipeCheckBean recipeCheck = recipeCheckService.getByRecipeId(recipeId);
        logger.info("RecipeAuditClient getByRecipeId res recipeCheck:{} ", JSONUtils.toString(recipeCheck));
        return recipeCheck;
    }

    /**
     * 获取审核不通过详情
     *
     * @param recipeId
     * @return
     */
    public List<Map<String, Object>> getCheckNotPassDetail(Integer recipeId) {
        logger.info("RecipeAuditClient getCheckNotPassDetail param recipeId:{}", recipeId);
        List<Map<String, Object>> mapList = recipeAuditService.getCheckNotPassDetail(recipeId);
        logger.info("RecipeAuditClient getCheckNotPassDetail res mapList:{}", JSONUtils.toString(mapList));
        return mapList;
    }

    /**
     * 通过处方号获取智能审方结果
     *
     * @param recipeId
     * @return
     */
    public List<AuditMedicinesBean> getAuditMedicineIssuesByRecipeId(int recipeId) {
        logger.info("RecipeAuditClient getAuditMedicineIssuesByRecipeId recipeId:{}", recipeId);
        List<AuditMedicinesBean> list = Lists.newArrayList();
        List<AuditMedicinesBean> medicines = iAuditMedicinesService.findMedicinesByRecipeId(recipeId);
        if (CollectionUtils.isEmpty(medicines)) {
            return list;
        }

        list = ObjectCopyUtils.convert(medicines, AuditMedicinesBean.class);
        List<AuditMedicineIssueBean> issues = iAuditMedicinesService.findIssueByRecipeId(recipeId);
        if (CollectionUtils.isEmpty(issues)) {
            return list;
        }
        for (AuditMedicinesBean auditMedicinesDTO : list) {
            List<AuditMedicineIssueBean> issueList = issues.stream().filter(a -> auditMedicinesDTO.getId().equals(a.getMedicineId())).collect(Collectors.toList());
            auditMedicinesDTO.setAuditMedicineIssues(issueList);
        }
        logger.info("RecipeAuditClient getAuditMedicineIssuesByRecipeId list:{}", JSON.toJSONString(list));
        return list;
    }

    public AutoAuditResultBean analysis(Recipe recipe, RecipeExtend recipeExtend, List<Recipedetail> recipedetails) {
        if (null == recipe) {
            throw new DAOException("处方不存在");
        }
        RecipeDTO recipeDTO = ObjectCopyUtils.convert(recipe, RecipeDTO.class);
        recipeDTO.setRecipeExtend(ObjectCopyUtils.convert(recipeExtend, RecipeExtendDTO.class));
        List<RecipeDetailDTO> recipeDetails = ObjectCopyUtils.convert(recipedetails, RecipeDetailDTO.class);
        AutoAuditResultBean resultBean = recipeAuditService.analysis(recipeDTO, recipeDetails);
        logger.info("RecipeAuditClient analysis resultBean:{}", JSON.toJSONString(resultBean));
        return resultBean;
    }

    /**
     * 返回处方分析数据
     *
     * @param recipeId
     * @return
     */
    public List<PAWebRecipeDangerBean> PAWebRecipeDanger(int recipeId) {
        logger.info("RecipeAuditClient PAWebRecipeDanger recipeId:{}", recipeId);
        List<eh.recipeaudit.model.AuditMedicineIssueBean> auditMedicineIssues = iAuditMedicinesService.findIssueByRecipeId(recipeId);
        List<PAWebRecipeDangerBean> recipeDangers = new ArrayList<>();
        if (CollectionUtils.isEmpty(auditMedicineIssues)) {
            return recipeDangers;
        }
        List<AuditMedicineIssueBean> resultMedicineIssues = auditMedicineIssues.stream().filter(a -> null == a.getMedicineId()).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(resultMedicineIssues)) {
            return recipeDangers;
        }
        resultMedicineIssues.forEach(item -> {
            PAWebRecipeDangerBean recipeDanger = new PAWebRecipeDangerBean();
            recipeDanger.setDangerDesc(item.getDetail());
            recipeDanger.setDangerDrug(item.getTitle());
            recipeDanger.setDangerLevel(item.getLvlCode());
            recipeDanger.setDangerType(item.getLvl());
            recipeDanger.setDetailUrl(item.getDetailUrl());
            recipeDangers.add(recipeDanger);
        });
        logger.info("RecipeAuditClient PAWebRecipeDanger recipeDangers:{}", JSON.toJSONString(recipeDangers));
        return recipeDangers;
    }

    /**
     * 判断药师的ca流程是否开启
     *
     * @param recipeId
     * @return
     */
    public Boolean isShowCheckCA(Integer recipeId) {
        RecipeCheckBean recipeCheckBean = getNowCheckResultByRecipeId(recipeId);
        if (recipeCheckBean != null && CA_FAIL.equals(recipeCheckBean.getIsCheckCA())) {
            return false;
        }
        return true;
    }

    /**
     * 当前处方已有审核通过中无审核不通过记录
     *
     * @param recipe
     * @param recipeLogs
     * @return
     */
    public boolean isShowChecker(Recipe recipe, List<RecipeLog> recipeLogs) {
        logger.info("RecipeAuditClient isShowChecker recipe:{}, recipeLogs:{}", JSON.toJSONString(recipe), JSON.toJSONString(recipeLogs));
        RecipeCheckBean recipeCheckBean = getNowCheckResultByRecipeId(recipe.getRecipeId());
        if (recipe.getCheckMode() != null && recipe.getCheckMode() == 2) {
            return false;
        }
        //判断是否是通过的
        if (null == recipeCheckBean) {
            return false;
        }
        //判断有没有不通过的记录，没有就说明是直接审核通过的
        if (null != recipeCheckBean.getCheckStatus() && 1 == recipeCheckBean.getCheckStatus() && CollectionUtils.isEmpty(recipeLogs)) {
            return true;
        }
        return false;
    }

    /**
     * 审核通过审核不通过
     *
     * @param recipeId
     * @return
     */
    public RecipeCheckBean getNowCheckResultByRecipeId(Integer recipeId) {
        logger.info("RecipeAuditClient getNowCheckResultByRecipeId recipeId:{}", recipeId);
        RecipeCheckBean recipeCheckBean = recipeCheckService.getNowCheckResultByRecipeId(recipeId);
        logger.info("RecipeAuditClient getNowCheckResultByRecipeId recipeCheckBean:{}", JSON.toJSONString(recipeCheckBean));
        return recipeCheckBean;
    }

    public List<RecipeCheckDetail> findByCheckId(Integer checkId) {
        logger.info("RecipeAuditClient findByCheckId checkId:{}", checkId);
        List<RecipeCheckDetailBean> recipeCheckDetailBeans = recipeCheckDetailService.findByCheckId(checkId);
        logger.info("RecipeAuditClient findByCheckId recipeCheckDetailBeans:{}", JSON.toJSONString(recipeCheckDetailBeans));
        List<RecipeCheckDetail> recipeCheckDetails = ObjectCopyUtils.convert(recipeCheckDetailBeans, RecipeCheckDetail.class);
        return recipeCheckDetails;
    }

}
