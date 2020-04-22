package recipe.thread;

import com.google.common.collect.Lists;
import com.ngari.recipe.audit.model.AuditMedicineIssueDTO;
import com.ngari.recipe.audit.model.AuditMedicinesDTO;
import com.ngari.recipe.entity.AuditMedicineIssue;
import com.ngari.recipe.recipe.model.RecipeBean;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import ctd.persistence.DAOFactory;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.ApplicationUtils;
import recipe.audit.bean.AutoAuditResult;
import recipe.audit.bean.Issue;
import recipe.audit.bean.PAWebMedicines;
import recipe.audit.bean.PAWebRecipeDanger;
import recipe.audit.service.PrescriptionService;
import recipe.dao.AuditMedicineIssueDAO;
import recipe.dao.AuditMedicinesDAO;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author： 0184/yu_yun
 * @date： 2018/11/27
 * @description： 保存审方信息
 * @version： 1.0
 */
public class SaveAutoReviewRunable implements Runnable {

    /**
     * logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(SaveAutoReviewRunable.class);

    private RecipeBean recipe;

    private List<RecipeDetailBean> details;

    public SaveAutoReviewRunable(RecipeBean recipe, List<RecipeDetailBean> details) {
        this.recipe = recipe;
        this.details = details;
    }

    @Override
    public void run() {
        Integer recipeId = this.recipe.getRecipeId();
        LOGGER.info("SaveAutoReview start. recipeId={}", recipeId);
        PrescriptionService prescriptionService = ApplicationUtils.getRecipeService(PrescriptionService.class);
        AuditMedicinesDAO auditMedicinesDAO = DAOFactory.getDAO(AuditMedicinesDAO.class);
        AuditMedicineIssueDAO auditMedicineIssueDAO = DAOFactory.getDAO(AuditMedicineIssueDAO.class);
        AutoAuditResult autoAuditResult = prescriptionService.analysis(recipe, details);
        List<AuditMedicinesDTO> auditMedicinesList = Lists.newArrayList();
        List<PAWebMedicines> paResultList = autoAuditResult.getMedicines();
        List<PAWebRecipeDanger> recipeDangers = autoAuditResult.getRecipeDangers();
        if (CollectionUtils.isEmpty(paResultList)) {
            AuditMedicinesDTO auditMedicinesDTO = new AuditMedicinesDTO();
            auditMedicinesDTO.setRecipeId(recipeId);
            auditMedicinesDTO.setRemark(autoAuditResult.getMsg());
            auditMedicinesList.add(auditMedicinesDTO);
            auditMedicinesDAO.save(recipeId, auditMedicinesList);
        } else if(CollectionUtils.isNotEmpty(paResultList)) {
            AuditMedicinesDTO auditMedicinesDTO;
            List<Issue> issueList;
            List<AuditMedicineIssueDTO> auditMedicineIssues;
            AuditMedicineIssueDTO auditIssueDTO;
            for (PAWebMedicines paMedicine : paResultList) {
                auditMedicinesDTO = new AuditMedicinesDTO();
                auditMedicinesDTO.setRecipeId(recipeId);
                auditMedicinesDTO.setCode(paMedicine.getCode());
                auditMedicinesDTO.setName(paMedicine.getName());
                issueList = paMedicine.getIssues();
                if (CollectionUtils.isNotEmpty(issueList)) {
                    auditMedicineIssues = new ArrayList<>(issueList.size());
                    for (Issue issue : issueList) {
                        auditIssueDTO = new AuditMedicineIssueDTO();
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
            auditMedicinesDAO.save(recipeId, auditMedicinesList);
        } else if (CollectionUtils.isNotEmpty(recipeDangers)) {
            recipeDangers.forEach(item->{
                AuditMedicineIssue auditMedicineIssue = new AuditMedicineIssue();
                auditMedicineIssue.setRecipeId(recipeId);
                auditMedicineIssue.setLvl(item.getDangerType());
                auditMedicineIssue.setLvlCode(item.getDangerLevel());
                auditMedicineIssue.setDetail(item.getDangerDesc());
                auditMedicineIssue.setTitle(item.getDangerDrug());
                auditMedicineIssue.setCreateTime(new Date());
                auditMedicineIssue.setLastModify(new Date());
                auditMedicineIssue.setLogicalDeleted(0);
                auditMedicineIssueDAO.save(auditMedicineIssue);
            });
        }
        LOGGER.info("SaveAutoReview finish. recipeId={}", recipeId);
    }

}
