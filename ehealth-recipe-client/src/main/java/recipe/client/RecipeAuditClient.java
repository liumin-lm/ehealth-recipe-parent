package recipe.client;

import com.google.common.collect.Lists;
import com.ngari.patient.utils.ObjectCopyUtils;
import ctd.util.JSONUtils;
import eh.recipeaudit.api.IAuditMedicinesService;
import eh.recipeaudit.api.IRecipeAuditService;
import eh.recipeaudit.api.IRecipeCheckService;
import eh.recipeaudit.model.AuditMedicineIssueBean;
import eh.recipeaudit.model.AuditMedicinesBean;
import eh.recipeaudit.model.RecipeCheckBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 审方相关服务
 *
 * @Author liumin
 * @Date 2021/7/22 下午2:26
 * @Description
 */
@Service
public class RecipeAuditClient extends BaseClient {

    @Autowired
    private IRecipeCheckService recipeCheckService;

    @Autowired
    private IRecipeAuditService recipeAuditService;

    @Autowired
    IAuditMedicinesService iAuditMedicinesService;


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
        List<AuditMedicinesBean> medicines = iAuditMedicinesService.findMedicinesByRecipeId(recipeId);
        List<AuditMedicinesBean> list = Lists.newArrayList();
        if (medicines != null && medicines.size() > 0) {
            list = ObjectCopyUtils.convert(medicines, AuditMedicinesBean.class);
            List<AuditMedicineIssueBean> issues = iAuditMedicinesService.findIssueByRecipeId(recipeId);
            if (issues != null && issues.size() > 0) {
                List<AuditMedicineIssueBean> issueList;
                for (AuditMedicinesBean auditMedicinesDTO : list) {
                    issueList = Lists.newArrayList();
                    for (AuditMedicineIssueBean auditMedicineIssue : issues) {
                        if (null != auditMedicineIssue.getMedicineId() && auditMedicineIssue.getMedicineId().equals(auditMedicinesDTO.getId())) {
                            issueList.add(auditMedicineIssue);
                        }
                    }
                    auditMedicinesDTO.setAuditMedicineIssues(ObjectCopyUtils.convert(issueList, AuditMedicineIssueBean.class));
                }
            }
        }
        return list;
    }

}
