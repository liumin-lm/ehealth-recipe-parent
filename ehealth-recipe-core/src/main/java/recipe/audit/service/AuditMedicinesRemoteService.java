package recipe.audit.service;

import com.ngari.recipe.audit.model.AuditMedicinesDTO;
import com.ngari.recipe.audit.service.IAuditMedicinesService;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import recipe.ApplicationUtils;
import recipe.service.RecipeService;

import java.util.List;

/**
 * 运营平台获取审方信息服务
 * created by shiyuping on 2018/11/26
 */
@RpcBean(value = "auditMedicinesService")
public class AuditMedicinesRemoteService implements IAuditMedicinesService {

    @Override
    @RpcService
    public List<AuditMedicinesDTO> getAuditmedicinesResult(int recipeId) {
        RecipeService recipeService = ApplicationUtils.getRecipeService(RecipeService.class);
        return recipeService.getAuditMedicineIssuesByRecipeId(recipeId);
    }
}
