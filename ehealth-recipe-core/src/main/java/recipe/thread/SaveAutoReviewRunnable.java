package recipe.thread;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.ngari.recipe.recipe.model.RecipeBean;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import ctd.util.AppContextHolder;
import eh.recipeaudit.api.IAuditMedicinesService;
import eh.recipeaudit.model.AuditMedicineIssueBean;
import eh.recipeaudit.model.AuditMedicinesBean;
import eh.recipeaudit.model.Intelligent.AutoAuditResultBean;
import eh.recipeaudit.model.Intelligent.IssueBean;
import eh.recipeaudit.model.Intelligent.PAWebMedicinesBean;
import eh.recipeaudit.model.Intelligent.PAWebRecipeDangerBean;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.ApplicationUtils;
import recipe.audit.service.PrescriptionService;

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
        long start = System.currentTimeMillis();
        LOGGER.info("SaveAutoReviewRunnable start. recipe={}", JSON.toJSONString(recipe));
        try{
            Integer recipeId = recipe.getRecipeId();
            PrescriptionService prescriptionService = ApplicationUtils.getRecipeService(PrescriptionService.class);
            IAuditMedicinesService iAuditMedicinesService = AppContextHolder.getBean("recipeaudit.remoteAuditMedicinesService", IAuditMedicinesService.class);
            AutoAuditResultBean autoAuditResult = prescriptionService.analysis(recipe, details);
            List<AuditMedicinesBean> auditMedicinesList = Lists.newArrayList();
            List<PAWebMedicinesBean> paResultList = autoAuditResult.getMedicines();
            List<PAWebRecipeDangerBean> recipeDangers = autoAuditResult.getRecipeDangers();
            LOGGER.info("SaveAutoReviewRunnable paResultList:{},paResultList:{}", JSON.toJSONString(paResultList), JSON.toJSONString(recipeDangers));
            if (CollectionUtils.isNotEmpty(recipeDangers)) {
                recipeDangers.forEach(item -> {
                    AuditMedicineIssueBean auditMedicineIssue = new AuditMedicineIssueBean();
                    auditMedicineIssue.setRecipeId(recipeId);
                    auditMedicineIssue.setLvl(item.getDangerType());
                    auditMedicineIssue.setLvlCode(item.getDangerLevel());
                    auditMedicineIssue.setDetail(item.getDangerDesc());
                    auditMedicineIssue.setTitle(item.getDangerDrug());
                    auditMedicineIssue.setCreateTime(new Date());
                    auditMedicineIssue.setLastModify(new Date());
                    auditMedicineIssue.setDetailUrl(item.getDetailUrl());
                    auditMedicineIssue.setLogicalDeleted(0);
                    iAuditMedicinesService.saveAuditMedicineIssue(auditMedicineIssue);
                });
            }
            if (CollectionUtils.isEmpty(paResultList)) {
                AuditMedicinesBean auditMedicinesDTO = new AuditMedicinesBean();
                auditMedicinesDTO.setRecipeId(recipeId);
                auditMedicinesDTO.setRemark(StringUtils.isNotEmpty(autoAuditResult.getMsg())?autoAuditResult.getMsg():"系统预审未发现处方问题");
                auditMedicinesList.add(auditMedicinesDTO);
                iAuditMedicinesService.saveAuditMedicines(recipeId, auditMedicinesList);
            } else if (CollectionUtils.isNotEmpty(paResultList)) {
                AuditMedicinesBean auditMedicinesDTO;
                List<IssueBean> issueList;
                List<AuditMedicineIssueBean> auditMedicineIssues;
                AuditMedicineIssueBean auditIssueDTO;
                for (PAWebMedicinesBean paMedicine : paResultList) {
                    auditMedicinesDTO = new AuditMedicinesBean();
                    auditMedicinesDTO.setRecipeId(recipeId);
                    auditMedicinesDTO.setCode(paMedicine.getCode());
                    auditMedicinesDTO.setName(paMedicine.getName());
                    auditMedicinesDTO.setRemark(StringUtils.isNotEmpty(autoAuditResult.getMsg())?autoAuditResult.getMsg():"系统预审未发现处方问题");
                    issueList = paMedicine.getIssues();
                    if (CollectionUtils.isNotEmpty(issueList)) {
                        auditMedicineIssues = new ArrayList<>(issueList.size());
                        for (IssueBean issue : issueList) {
                            auditIssueDTO = new AuditMedicineIssueBean();
                            auditIssueDTO.setDetail(issue.getDetail());
                            auditIssueDTO.setLvl(issue.getLvl());
                            auditIssueDTO.setLvlCode(issue.getLvlCode());
                            auditIssueDTO.setTitle(issue.getTitle());
                            auditMedicineIssues.add(auditIssueDTO);
                        }
                        auditMedicinesDTO.setAuditMedicineIssues(auditMedicineIssues);
                    }
                    auditMedicinesList.add(auditMedicinesDTO);
                }
                iAuditMedicinesService.saveAuditMedicines(recipeId, auditMedicinesList);
            }
        }catch (Exception e){
            LOGGER.error("SaveAutoReviewRunnable error,recipe={}", JSON.toJSONString(recipe), e);
        } finally {
            long elapsedTime = System.currentTimeMillis() - start;
            LOGGER.info("RecipeBusiThreadPool SaveAutoReviewRunnable 保存智能审方 执行时间:{}ms.", elapsedTime);
        }
        LOGGER.info("SaveAutoReviewRunnable finish. recipe={}", JSON.toJSONString(recipe));
    }

}
