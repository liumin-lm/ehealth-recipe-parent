package recipe.operation;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.ngari.base.organ.model.OrganBean;
import com.ngari.base.organ.service.IOrganService;
import com.ngari.base.patient.model.PatientBean;
import com.ngari.patient.dto.AppointDepartDTO;
import com.ngari.patient.dto.DoctorDTO;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.patient.service.DoctorService;
import com.ngari.patient.service.PatientService;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.drugsenterprise.model.DrugsEnterpriseBean;
import com.ngari.recipe.dto.ApothecaryDTO;
import com.ngari.recipe.entity.*;
import com.ngari.recipe.recipe.model.GuardianBean;
import com.ngari.recipe.recipe.model.RecipeBean;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import com.ngari.recipe.recipeorder.model.RecipeOrderBean;
import ctd.account.UserRoleToken;
import ctd.controller.exception.ControllerException;
import ctd.dictionary.DictionaryController;
import ctd.persistence.DAOFactory;
import ctd.persistence.exception.DAOException;
import ctd.schema.exception.ValidateException;
import ctd.util.BeanUtils;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import eh.recipeaudit.api.IRecipeAuditService;
import eh.recipeaudit.api.IRecipeCheckService;
import eh.recipeaudit.model.AuditMedicinesBean;
import eh.recipeaudit.model.Intelligent.PAWebRecipeDangerBean;
import eh.recipeaudit.model.RecipeCheckBean;
import eh.recipeaudit.util.RecipeAuditAPI;
import lombok.extern.java.Log;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.util.Args;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.ApplicationUtils;
import recipe.aop.LogRecord;
import recipe.audit.service.PrescriptionService;
import recipe.bussutil.AESUtils;
import recipe.client.DoctorClient;
import recipe.client.RecipeAuditClient;
import recipe.constant.*;
import recipe.dao.*;
import recipe.manager.ButtonManager;
import recipe.manager.DepartManager;
import recipe.service.RecipeService;
import recipe.util.ByteUtils;
import recipe.util.ChinaIDNumberUtil;
import recipe.util.DateConversion;
import recipe.vo.second.ApothecaryVO;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author wzc
 * @date 2020-10-27 14:27
 * @desc 运营平台处方服务
 */
@RpcBean("operationPlatformRecipeService")
public class OperationPlatformRecipeService {

    /**
     * LOGGER
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(OperationPlatformRecipeService.class);
    @Autowired
    private RecipeAuditClient recipeAuditClient;

    /**
     * 审核平台 获取处方单详情
     *
     * @param recipeId
     * @return
     */
    @RpcService
    @LogRecord
    public Map<String, Object> findRecipeAndDetailsAndCheckByIdEncrypt(String recipeId, Integer doctorId) {
        return recipeAuditClient.findRecipeAndDetailsAndCheckByIdEncrypt(recipeId,doctorId);
    }

    /**
     * 审核平台 获取处方单详情
     *
     * @param recipeId
     * @return
     */
    @RpcService
    @LogRecord
    public Map<String, Object> findRecipeAndDetailsAndCheckById(int recipeId, Integer checkerId) {
        return recipeAuditClient.findRecipeAndDetailsAndCheckById(recipeId,checkerId);
    }

    /**
     * 脱敏身份证号
     *
     * @param idCard
     * @return
     */
    private String hideIdCard(String idCard) {
        return ByteUtils.hideIdCard(idCard);
    }

    /**
     * chuwei
     * 前端页面调用该接口查询是否存在待审核的处方单
     *
     * @param organ 审核机构
     * @return
     */
    @RpcService
    @LogRecord
    public boolean existUncheckedRecipe(int organ) {
        RecipeDAO rDao = DAOFactory.getDAO(RecipeDAO.class);
        boolean bResult = rDao.checkIsExistUncheckedRecipe(organ);
        return bResult;
    }


    /**
     * 获取药师能审核的机构
     *
     * @param doctorId 药师ID
     * @return
     */
    @RpcService
    @LogRecord
    public List<OrganBean> findCheckOrganList(Integer doctorId) {
        return recipeAuditClient.findCheckOrganList(doctorId);
    }

    /**
     * 判断登录用户能否审核机构下的处方
     *
     * @param recipeId
     * @param doctorId
     */
    @LogRecord
    public void checkUserIsChemistByDoctorId(Integer recipeId, Integer doctorId) {
        recipeAuditClient.checkUserIsChemistByDoctorId(recipeId,doctorId);
    }

    /**
     * 获取抢单状态和自动解锁时间
     *
     * @param map
     * @return
     */
    @RpcService
    @LogRecord
    public Map<String, Object> getGrabOrderStatusAndLimitTime(Map<String, Object> map) {
        return recipeAuditClient.getGrabOrderStatusAndLimitTime(map);
    }
}
