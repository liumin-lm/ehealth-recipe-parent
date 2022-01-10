package recipe.audit.service;

import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.audit.model.AuditMedicinesDTO;
import com.ngari.recipe.audit.service.IAuditMedicinesService;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import eh.recipeaudit.model.AuditMedicinesBean;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.client.RecipeAuditClient;

import java.util.List;

/**
 * 运营平台获取审方信息服务 服务在用 新方法不再此类新增
 * created by shiyuping on 2018/11/26
 */
@RpcBean(value = "auditMedicinesService")
@Deprecated
public class AuditMedicinesRemoteService implements IAuditMedicinesService {
    @Autowired
    private RecipeAuditClient recipeAuditClient;

    @Override
    @RpcService
    public List<AuditMedicinesDTO> getAuditmedicinesResult(int recipeId) {
        List<AuditMedicinesBean> auditMedicines = recipeAuditClient.getAuditMedicineIssuesByRecipeId(recipeId);
        return ObjectCopyUtils.convert(auditMedicines, AuditMedicinesDTO.class);
    }

}
