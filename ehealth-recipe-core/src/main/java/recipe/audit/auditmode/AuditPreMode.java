package recipe.audit.auditmode;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Maps;
import com.ngari.consult.ConsultAPI;
import com.ngari.consult.common.model.ConsultExDTO;
import com.ngari.consult.common.service.IConsultExService;
import com.ngari.his.recipe.mode.NotifyPharAuditTO;
import com.ngari.his.recipe.service.IRecipeHisService;
import com.ngari.home.asyn.model.BussCreateEvent;
import com.ngari.home.asyn.service.IAsynDoBussService;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.recipe.model.RecipeBean;
import com.ngari.revisit.RevisitAPI;
import com.ngari.revisit.common.model.RevisitExDTO;
import com.ngari.revisit.common.service.IRevisitExService;
import ctd.persistence.DAOFactory;
import ctd.spring.AppDomainContext;
import ctd.util.AppContextHolder;
import ctd.util.JSONUtils;
import eh.base.constant.BussTypeConstant;
import eh.recipeaudit.api.IRecipeAuditService;
import eh.recipeaudit.model.recipe.RecipeDTO;
import eh.recipeaudit.util.RecipeAuditAPI;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.ApplicationUtils;
import recipe.client.DocIndexClient;
import recipe.constant.RecipeBussConstant;
import recipe.constant.RecipeStatusConstant;
import recipe.constant.ReviewTypeConstant;
import recipe.dao.RecipeDAO;
import recipe.dao.RecipeDetailDAO;
import recipe.enumerate.type.DocIndexShowEnum;
import recipe.manager.EnterpriseManager;
import recipe.service.RecipeLogService;
import recipe.service.RecipeMsgService;
import recipe.service.RecipeService;
import recipe.service.RecipeServiceSub;
import recipe.thread.RecipeBusiThreadPool;
import recipe.thread.UpdateWaterPrintRecipePdfRunable;

import java.util.Map;

import static ctd.persistence.DAOFactory.getDAO;

/**
 * created by shiyuping on 2019/8/15
 * 审方前置
 */
@AuditMode(ReviewTypeConstant.Pre_AuditMode)
public class AuditPreMode extends AbstractAuidtMode {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuditPreMode.class);

    @Override
    public int afterAuditRecipeChange() {
        return RecipeStatusConstant.CHECK_PASS;
    }

    @Override
    public void afterCheckPassYs(Recipe recipe) {
        LOGGER.info("AuditPreMode afterCheckPassYs recipeId:{}.", recipe.getRecipeId());
        RecipeDetailDAO detailDAO = getDAO(RecipeDetailDAO.class);
        Integer recipeId = recipe.getRecipeId();
        String recipeMode = recipe.getRecipeMode();
        EnterpriseManager enterpriseManager = AppContextHolder.getBean("enterpriseManager", EnterpriseManager.class);
        //药师审方后推送给前置机（扁鹊）
        enterpriseManager.pushRecipeForThird(recipe, 0);
        //正常平台处方
        if (RecipeBussConstant.FROMFLAG_PLATFORM.equals(recipe.getFromflag())) {
            //审核通过只有互联网发
            if (RecipeBussConstant.RECIPEMODE_ZJJGPT.equals(recipeMode)) {
                RecipeServiceSub.sendRecipeTagToPatient(recipe, detailDAO.findByRecipeId(recipeId), null, true);
                //向患者推送处方消息
                RecipeMsgService.batchSendMsg(recipe, RecipeStatusConstant.CHECK_PASS);
            } else {
                //平台前置发送审核通过消息 /向患者推送处方消息 处方通知您有一张处方单需要处理，请及时查看。
                RecipeMsgService.batchSendMsg(recipe, RecipeStatusConstant.CHECK_PASS_YS);
            }
        }
        // 病历处方-状态修改成显示
        DocIndexClient docIndexClient = AppContextHolder.getBean("docIndexClient", DocIndexClient.class);
        docIndexClient.updateStatusByBussIdBussType(recipe.getRecipeId(), DocIndexShowEnum.SHOW.getCode());
        RecipeLogService.saveRecipeLog(recipe.getRecipeId(), recipe.getStatus(), recipe.getStatus(), "审核通过处理完成");
    }

    @Override
    public void afterHisCallBackChange(Integer status, Recipe recipe, String memo) {
        //处方签名中 点击撤销按钮 如果处方单状态处于已取消 则不走下面逻辑
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        Recipe byRecipeId = recipeDAO.getByRecipeId(recipe.getRecipeId());
        if (byRecipeId.getStatus() == 9) {
            LOGGER.info("afterHisCallBackChange 处方单已经撤销,recipeid:{}", recipe.getRecipeId());
            return;
        }
        if (status == RecipeStatusConstant.CHECK_PASS) {
            status = RecipeStatusConstant.READY_CHECK_YS;
        }
        // 平台模式前置需要发送卡片 待审核只有平台发
        if (RecipeBussConstant.RECIPEMODE_NGARIHEALTH.equals(recipe.getRecipeMode())) {
            RecipeDetailDAO detailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
            RecipeServiceSub.sendRecipeTagToPatient(recipe, detailDAO.findByRecipeId(recipe.getRecipeId()), null, true);
        }
        //生成文件成功后再去更新处方状态
        if (recipeDAO.getByRecipeId(recipe.getRecipeId()).getStatus() == 9) {
            LOGGER.info("afterHisCallBackChange 处方单已经撤销再次判断,recipeid:{}", recipe.getRecipeId());
            return;
        }
        recipeDAO.updateRecipeInfoByRecipeId(recipe.getRecipeId(), status, null);
        //更新审方checkFlag为待审核
        Map<String, Object> attrMap = Maps.newHashMap();
        attrMap.put("checkFlag", 0);
        recipeDAO.updateRecipeInfoByRecipeId(recipe.getRecipeId(), attrMap);
        LOGGER.info("checkFlag {} 更新为待审核", recipe.getRecipeId());
        //日志记录
        RecipeLogService.saveRecipeLog(recipe.getRecipeId(), recipe.getStatus(), status, memo);
        //发送消息
        sendMsg(status, recipe);
        Integer checkMode = recipe.getCheckMode();
        // 是不是三方合理用药
        boolean flag = super.threeRecipeAutoCheck(recipe.getRecipeId(), recipe.getClinicOrgan());
        LOGGER.info("第三方智能审方flag:{}", flag);
        if (!new Integer(1).equals(checkMode)) {
            if (new Integer(2).equals(checkMode)) {
                //针对his审方的模式,先在此处处理,推送消息给前置机,让前置机取轮询HIS获取审方结果
                IRecipeAuditService recipeAuditService = RecipeAuditAPI.getService(IRecipeAuditService.class, "recipeAuditServiceImpl");
                RecipeDTO recipeBean = ObjectCopyUtils.convert(recipe, RecipeDTO.class);
                recipeAuditService.sendCheckRecipeInfo(recipeBean);
            } else if (new Integer(5).equals(checkMode)) {
                this.notifyPharAudit(byRecipeId);
            } else {
                super.recipeAudit(recipe);
            }
        } else if (flag) {
            LOGGER.info("第三方智能审方start");
            super.doAutoRecipe(recipe.getRecipeId());
            LOGGER.info("第三方智能审方end");
        }
        //异步添加水印
        RecipeBusiThreadPool.execute(new UpdateWaterPrintRecipePdfRunable(recipe.getRecipeId()));
    }

    /**
     * 美康在用
     *
     * @param recipe
     * @return
     */
    private Boolean notifyPharAudit(Recipe recipe) {
        LOGGER.info("notifyPharAudit start recipe={}", JSONUtils.toString(recipe));
        NotifyPharAuditTO request = new NotifyPharAuditTO();
        String registerNo = "";
        String cardNo = "";
        Integer recipeId = recipe.getRecipeId();
        Integer organId = recipe.getClinicOrgan();
        if (null != recipe.getClinicId()) {
            if (RecipeBussConstant.BUSS_SOURCE_FZ.equals(recipe.getBussSource())) {
                IRevisitExService iRevisitExService = RevisitAPI.getService(IRevisitExService.class);
                RevisitExDTO revisitExDTO = iRevisitExService.getByConsultId(recipe.getClinicId());
                LOGGER.info("notifyPharAudit revisitExDTO:{}", JSONUtils.toString(revisitExDTO));
                iRevisitExService.updateRecipeIdByConsultId(recipe.getClinicId(), recipe.getRecipeId());
                if (null != revisitExDTO) {
                    if (StringUtils.isNotEmpty(revisitExDTO.getRegisterNo())) {
                        registerNo = revisitExDTO.getRegisterNo();
                    }
                    if (StringUtils.isNotEmpty(revisitExDTO.getCardId()) && StringUtils.isNotEmpty(revisitExDTO.getCardType())) {
                        cardNo = revisitExDTO.getCardId();
                    }
                }
            } else if (RecipeBussConstant.BUSS_SOURCE_WZ.equals(recipe.getBussSource())) {
                IConsultExService exService = ConsultAPI.getService(IConsultExService.class);
                ConsultExDTO consultExDTO = exService.getByConsultId(recipe.getClinicId());
                LOGGER.info("notifyPharAudit consultExDTO:{}", JSONUtils.toString(consultExDTO));
                exService.updateRecipeIdByConsultId(recipe.getClinicId(), recipe.getRecipeId());
                if (null != consultExDTO) {
                    if (StringUtils.isNotEmpty(consultExDTO.getRegisterNo())) {
                        registerNo = consultExDTO.getRegisterNo();
                    }
                    if (StringUtils.isNotEmpty(consultExDTO.getCardId()) && StringUtils.isNotEmpty(consultExDTO.getCardType())) {
                        cardNo = consultExDTO.getCardId();
                    }
                }
            }
        }
        request.setPatCode(cardNo);
        request.setInHospNo(registerNo);
        request.setVisitCode(String.valueOf(recipeId));
        request.setOrganID(String.valueOf(organId));
        IRecipeHisService hisService = AppDomainContext.getBean("his.iRecipeHisService", IRecipeHisService.class);
        LOGGER.info("notifyPharAudit request={}", JSONUtils.toString(request));
        Boolean response = false;
        try {
            response = hisService.notifyPharAudit(request);
            LOGGER.info("notifyPharAudit response={}", JSONUtils.toString(response));
        } catch (Exception e) {
            LOGGER.error("notifyPharAudit error request:{}", JSONUtils.toString(request), e);
        }
        LOGGER.info("notifyPharAudit finsh");
        return response;
    }


    private void sendMsg(Integer status, Recipe recipe) {
        //平台处方进行消息发送等操作
        if (1 == recipe.getFromflag()) {
            Integer checkMode = recipe.getCheckMode();
            //发送消息--待审核消息
            RecipeMsgService.batchSendMsg(recipe.getRecipeId(), status);
            boolean flag = super.judgeRecipeAutoCheck(recipe.getRecipeId(), recipe.getClinicOrgan());
            boolean threeflag = super.threeRecipeAutoCheck(recipe.getRecipeId(), recipe.getClinicOrgan());
            //平台审方途径下才发消息  满足自动审方的不推送
            LOGGER.info("sendMsg:判断:{}", (status == RecipeStatusConstant.READY_CHECK_YS && new Integer(1).equals(checkMode) && !(flag || threeflag)));
            if (status == RecipeStatusConstant.READY_CHECK_YS && new Integer(1).equals(checkMode) && !(flag || threeflag)) {
                if (RecipeBussConstant.RECIPEMODE_NGARIHEALTH.equals(recipe.getRecipeMode())) {
                    //增加药师首页待处理任务---创建任务
                    RecipeBean recipeBean = ObjectCopyUtils.convert(recipe, RecipeBean.class);
                    LOGGER.info("AuditPreMode sendMsg recipeId:{},recipeBean:{}", recipe.getRecipeId(), JSON.toJSONString(recipeBean));
                    ApplicationUtils.getBaseService(IAsynDoBussService.class).fireEvent(new BussCreateEvent(recipeBean, BussTypeConstant.RECIPE));
                }
            }
            //保存至电子病历
            RecipeService recipeService = ApplicationUtils.getRecipeService(RecipeService.class);
            recipeService.saveRecipeDocIndex(recipe);
        }
    }

}
