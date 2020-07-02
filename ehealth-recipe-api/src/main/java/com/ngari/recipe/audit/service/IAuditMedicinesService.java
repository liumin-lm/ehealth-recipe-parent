package com.ngari.recipe.audit.service;

import com.ngari.recipe.audit.model.AuditMedicinesDTO;
import ctd.util.annotation.RpcService;

import java.util.List;

/**
 * created by shiyuping on 2018/11/26
 */
public interface IAuditMedicinesService {

    @RpcService
    List<AuditMedicinesDTO> getAuditmedicinesResult(int recipeId);

    /**
     * 获取审核处方状态
     * @param reviewType
     * @return
     */
    @RpcService
    int getAuditStatusByReviewType(int reviewType);
}
