package recipe.audit.auditmode;

import com.alibaba.fastjson.JSON;
import com.ngari.base.property.service.IConfigurationCenterUtilsService;
import com.ngari.home.asyn.model.BussCreateEvent;
import com.ngari.home.asyn.service.IAsynDoBussService;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.common.RecipeResultBean;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeExtend;
import com.ngari.recipe.entity.RecipeOrder;
import com.ngari.recipe.entity.Recipedetail;
import com.ngari.recipe.recipe.model.RecipeBean;
import ctd.persistence.DAOFactory;
import ctd.util.AppContextHolder;
import eh.base.constant.BussTypeConstant;
import eh.cdr.constant.RecipeStatusConstant;
import eh.recipeaudit.api.IAuditMedicinesService;
import eh.recipeaudit.api.IRecipeAuditService;
import eh.recipeaudit.model.recipe.RecipeDTO;
import eh.recipeaudit.model.recipe.RecipeDetailDTO;
import eh.recipeaudit.util.RecipeAuditAPI;
import eh.wxpay.constant.PayConstant;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.ApplicationUtils;
import recipe.client.RecipeAuditClient;
import recipe.constant.RecipeBussConstant;
import recipe.constant.RecipeSystemConstant;
import recipe.dao.RecipeDAO;
import recipe.dao.RecipeDetailDAO;
import recipe.dao.RecipeExtendDAO;
import recipe.dao.RecipeOrderDAO;
import recipe.drugsenterprise.RemoteDrugEnterpriseService;
import recipe.manager.RecipeManager;
import recipe.service.RecipeLogService;
import recipe.service.RecipeMsgService;
import recipe.service.RecipeService;
import recipe.service.RecipeServiceSub;
import recipe.thread.RecipeBusiThreadPool;
import recipe.thread.UpdateWaterPrintRecipePdfRunable;
import recipe.util.MapValueUtil;

import java.util.List;
import java.util.Map;

import static ctd.persistence.DAOFactory.getDAO;

/**
 * created by shiyuping on 2019/9/3
 */
public abstract class AbstractAuidtMode implements IAuditMode {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractAuidtMode.class);

    @Override
    public void afterHisCallBackChange(Integer status, Recipe recipe, String memo) {
        RecipeDetailDAO detailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
        //发送卡片
        RecipeServiceSub.sendRecipeTagToPatient(recipe, detailDAO.findByRecipeId(recipe.getRecipeId()), null, true);
        saveStatusAndSendMsg(status, recipe, memo);
        //异步添加水印
        RecipeBusiThreadPool.execute(new UpdateWaterPrintRecipePdfRunable(recipe.getRecipeId()));
    }

    private void saveStatusAndSendMsg(Integer status, Recipe recipe, String memo) {
        //生成文件成功后再去更新处方状态
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        Recipe byRecipeId = recipeDAO.getByRecipeId(recipe.getRecipeId());
        //处方签名中 点击撤销按钮 如果处方单状态处于已取消 则不走下面逻辑
        if (9 == byRecipeId.getStatus()) {
            LOGGER.info("saveStatusAndSendMsg 处方单已经撤销,recipeid:{}", recipe.getRecipeId());
            return;
        }
        recipeDAO.updateRecipeInfoByRecipeId(recipe.getRecipeId(), status, null);
        //日志记录
        RecipeLogService.saveRecipeLog(recipe.getRecipeId(), recipe.getStatus(), status, memo);
        //平台处方进行消息发送等操作
        if (1 != recipe.getFromflag()) {
            return;
        }
        //发送消息--待审核或者待处理消息
        RecipeMsgService.batchSendMsg(recipe.getRecipeId(), status);
        boolean flag = this.judgeRecipeAutoCheck(recipe.getRecipeId(), recipe.getClinicOrgan());
        //线下审方不推送药师消息
        if (new Integer(1).equals(recipe.getCheckMode()) && !flag && RecipeStatusConstant.READY_CHECK_YS == status
                && RecipeBussConstant.RECIPEMODE_NGARIHEALTH.equals(recipe.getRecipeMode())) {
            //增加药师首页待处理任务---创建任务
            RecipeBean recipeBean = ObjectCopyUtils.convert(recipe, RecipeBean.class);
            LOGGER.info("AbstractAuidtMode saveStatusAndSendMsg recipeId:{},recipeBean:{}", recipe.getRecipeId(), JSON.toJSONString(recipeBean));
            ApplicationUtils.getBaseService(IAsynDoBussService.class).fireEvent(new BussCreateEvent(recipeBean, BussTypeConstant.RECIPE));
        }
        //保存至电子病历
        RecipeService recipeService = ApplicationUtils.getRecipeService(RecipeService.class);
        recipeService.saveRecipeDocIndex(recipe);
    }

    @Override
    public int afterAuditRecipeChange() {
        return RecipeStatusConstant.CHECK_PASS_YS;
    }

    @Override
    public void afterCheckPassYs(Recipe recipe) {
    }

    @Override
    public void afterCheckNotPassYs(Recipe recipe) {
    }

    @Override
    public void afterPayChange(Boolean saveFlag, Recipe recipe, RecipeResultBean result, Map<String, Object> attrMap) {
        //默认待处理
        Integer status = RecipeStatusConstant.CHECK_PASS;
        Integer giveMode = null == MapValueUtil.getInteger(attrMap, "giveMode") ? recipe.getGiveMode() : MapValueUtil.getInteger(attrMap, "giveMode");
        Integer payFlag = MapValueUtil.getInteger(attrMap, "payFlag");
        // 获取paymode
        RecipeOrderDAO orderDAO = DAOFactory.getDAO(RecipeOrderDAO.class);
        RecipeOrder byOrderCode = orderDAO.getByOrderCode(recipe.getOrderCode());
        Integer payMode = byOrderCode.getPayMode();
        //根据传入的方式来处理, 因为供应商列表，钥世圈提供的有可能是多种方式都支持，当时这2个值是保存为null的
        if (saveFlag) {
            attrMap.put("chooseFlag", 1);
            String memo = "";
            if (RecipeBussConstant.GIVEMODE_SEND_TO_HOME.equals(giveMode)) {
                if (RecipeBussConstant.PAYMODE_ONLINE.equals(payMode)) {
                    //线上支付
                    if (new Integer(PayConstant.PAY_FLAG_PAY_SUCCESS).equals(payFlag)) {
                        //配送到家-线上支付
                        memo = "配送到家-线上支付成功";
                    } else {
                        memo = "配送到家-线上支付失败";
                    }
                } else if (RecipeBussConstant.PAYMODE_COD.equals(payMode)) {
                    memo = "货到付款-待配送";
                }
            } else if (RecipeBussConstant.GIVEMODE_TFDS.equals(giveMode)) {
                memo = "药店取药-待取药";
            }
            //记录日志
            RecipeLogService.saveRecipeLog(recipe.getRecipeId(), RecipeStatusConstant.CHECK_PASS, status, memo);
        } else {
            attrMap.put("chooseFlag", 0);
            if (RecipeBussConstant.FROMFLAG_HIS_USE.equals(recipe.getFromflag())) {
                status = recipe.getStatus();
            }
        }
        updateRecipeInfoByRecipeId(recipe.getRecipeId(), status, attrMap, result);
        LOGGER.info("AbstractAuidtMode.afterPayChange saveFlag:{}, payFlag:{}.", saveFlag, payFlag);
        if (saveFlag && new Integer(PayConstant.PAY_FLAG_PAY_SUCCESS).equals(payFlag)) {
            //处方推送到药企
            RemoteDrugEnterpriseService remoteDrugEnterpriseService = ApplicationUtils.getRecipeService(RemoteDrugEnterpriseService.class);
            remoteDrugEnterpriseService.pushSingleRecipeInfo(recipe.getRecipeId());
        }

    }

    protected void updateRecipeInfoByRecipeId(Integer recipeId, Integer status, Map<String, Object> attrMap, RecipeResultBean result) {
        try {
            RecipeDAO recipeDAO = getDAO(RecipeDAO.class);
            boolean flag = recipeDAO.updateRecipeInfoByRecipeId(recipeId, status, attrMap);
            if (flag) {
                result.setMsg(RecipeSystemConstant.SUCCESS);
            } else {
                result.setCode(RecipeResultBean.FAIL);
                result.setError("更新处方失败");
            }
        } catch (Exception e) {
            result.setCode(RecipeResultBean.FAIL);
            result.setError("更新处方失败，" + e.getMessage());
            LOGGER.error("更新处方失败", e);
        }
    }

    /**
     * 线下审方
     *
     * @param recipe
     */
    protected void recipeAudit(Recipe recipe) {
        LOGGER.info("AbstractAuidtMode recipeAudit recipe={}",JSON.toJSONString(recipe));
        try {
            Integer recipeId = recipe.getRecipeId();
            //处方信息 AND 病历信息重新拉去
            RecipeManager recipeManager = AppContextHolder.getBean("recipeManager", RecipeManager.class);
            Recipe recipeManagBean = recipeManager.getRecipeById(recipeId);
            RecipeDTO recipeDTO = ObjectCopyUtils.convert(recipeManagBean, RecipeDTO.class);
            //查詢处方扩展 获取对应的挂号序号
            RecipeExtendDAO recipeExtendDAO = DAOFactory.getDAO(RecipeExtendDAO.class);
            RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipeId);
            recipeDTO.setRegisterId(recipeExtend.getRegisterID());
            //处方信息详情
            RecipeDetailDAO recipeDetailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
            List<Recipedetail> recipeDetails = recipeDetailDAO.findByRecipeId(recipeId);
            List<RecipeDetailDTO> recipeDetailBeans = ObjectCopyUtils.convert(recipeDetails, RecipeDetailDTO.class);
            IRecipeAuditService recipeAuditService = RecipeAuditAPI.getService(IRecipeAuditService.class, "recipeAuditServiceImpl");
            LOGGER.info("AbstractAuidtMode recipeAudit recipeDTO={} recipeDetailBeans={}",JSON.toJSONString(recipeDTO),JSON.toJSONString(recipeDetailBeans));
            if (recipeDTO.getCheckMode().equals(3)) {
                recipeAuditService.winningRecipeAudit(recipeDTO, recipeDetailBeans);
            } else {
                recipeAuditService.offlineRecipeAudit(recipeDTO, recipeDetailBeans);
            }

        } catch (Exception e) {
            LOGGER.error("recipeAudit.error", e);
        }
    }


    /**
     * @param recipeId
     * @desc 执行具体的三方 只能是三方审核模式下
     */
    protected void doAutoRecipe(Integer recipeId) {
        RecipeManager recipeManager = AppContextHolder.getBean("recipeManager", RecipeManager.class);
        Recipe recipe = recipeManager.getRecipeById(recipeId);
        RecipeDetailDAO recipeDetailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
        List<Recipedetail> recipeDetails = recipeDetailDAO.findByRecipeId(recipeId);
        RecipeAuditClient recipeAuditClient = AppContextHolder.getBean("recipeAuditClient", RecipeAuditClient.class);
        recipeAuditClient.analysis(recipe, null, recipeDetails);
    }

    protected boolean judgeRecipeAutoCheck(Integer recipeId, Integer organId) {
        LOGGER.info("judgeRecipeAutoCheck recipe={}", recipeId);
        try {
            IConfigurationCenterUtilsService iConfigService = ApplicationUtils.getBaseService(IConfigurationCenterUtilsService.class);
            Boolean invokeRecipeAnalysis = (Boolean) iConfigService.getConfiguration(organId, "InvokeRecipeAnalysis");
            Integer intellectJudicialFlag = (Integer) iConfigService.getConfiguration(organId, "intellectJudicialFlag");
            String autoRecipecheckLevel = (String) iConfigService.getConfiguration(organId, "autoRecipecheckLevel");
            String defaultRecipecheckDoctor = (String) iConfigService.getConfiguration(organId, "defaultRecipecheckDoctor");
            if (invokeRecipeAnalysis && intellectJudicialFlag == 1 && StringUtils.isNotEmpty(defaultRecipecheckDoctor) && StringUtils.isNotEmpty(autoRecipecheckLevel)) {
                String[] levels = autoRecipecheckLevel.split(",");
                Integer minLevel = Integer.valueOf(levels[0]);
                Integer maxLevel = Integer.valueOf(levels[1]);
                IAuditMedicinesService iAuditMedicinesService = AppContextHolder.getBean("recipeaudit.remoteAuditMedicinesService", IAuditMedicinesService.class);
                Map<Integer, Integer> maxLevelMap = iAuditMedicinesService.queryRecipeMaxLevel(recipeId);
                Integer dbMaxLevel = maxLevelMap.get(recipeId);
                if (dbMaxLevel == null || (minLevel.intValue() <= dbMaxLevel.intValue() && dbMaxLevel.intValue() <= maxLevel.intValue())) {
                    LOGGER.info("满足自动审方条件，已拦截，不推送药师消息，recipeId ={}", recipeId);
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            LOGGER.error("judgeRecipeAutoCheck error recipe={}", recipeId, e);
            return false;
        }
    }


    /**
     * @return
     */
    protected Boolean threeRecipeAutoCheck(Integer recipeId, Integer organId) {
        LOGGER.info("threeRecipeAutoCheck recipe={}", recipeId);
        try {
            IConfigurationCenterUtilsService iConfigService = ApplicationUtils.getBaseService(IConfigurationCenterUtilsService.class);
            Integer intellectJudicialFlag = (Integer) iConfigService.getConfiguration(organId, "intellectJudicialFlag");
            String autoRecipecheckLevel = (String) iConfigService.getConfiguration(organId, "autoRecipecheckLevel");
            String defaultRecipecheckDoctor = (String) iConfigService.getConfiguration(organId, "defaultRecipecheckDoctor");
            if (intellectJudicialFlag == 3 && StringUtils.isNotEmpty(defaultRecipecheckDoctor) && StringUtils.isNotEmpty(autoRecipecheckLevel)) {
                // 这个只是一个范围判断
                return true;
            }
            return false;
        } catch (Exception e) {
            LOGGER.error("threeRecipeAutoCheck error recipe={}", recipeId, e);
            return false;
        }
    }
}
