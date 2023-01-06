package recipe.caNew;

import com.alibaba.fastjson.JSON;
import com.ngari.consult.ConsultAPI;
import com.ngari.consult.process.service.IRecipeOnLineConsultService;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.common.RecipeResultBean;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.Recipedetail;
import com.ngari.recipe.recipe.model.RecipeBean;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import com.ngari.revisit.RevisitAPI;
import com.ngari.revisit.process.service.IRecipeOnLineRevisitService;
import ctd.util.AppContextHolder;
import eh.wxpay.constant.PayConstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.ApplicationUtils;
import recipe.audit.auditmode.AuditModeContext;
import recipe.client.DocIndexClient;
import recipe.client.IConfigurationClient;
import recipe.constant.JKHBConstant;
import recipe.constant.RecipeBussConstant;
import recipe.enumerate.status.RecipeStatusEnum;
import recipe.enumerate.type.RecipeDistributionFlagEnum;
import recipe.manager.CaManager;
import recipe.manager.RecipeManager;
import recipe.manager.StateManager;
import recipe.service.RecipeHisService;
import recipe.service.RecipeServiceSub;
import recipe.thread.PushRecipeToHisCallable;
import recipe.thread.PushRecipeToRegulationCallable;
import recipe.thread.RecipeBusiThreadPool;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//JRK
//将CA流程上的特异点抽象出来
public abstract class AbstractCaProcessType {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractCaProcessType.class);
    protected static IConfigurationClient configurationClient = AppContextHolder.getBean("IConfigurationClient", IConfigurationClient.class);
    protected static CaManager caManager = AppContextHolder.getBean("caManager", CaManager.class);
    protected static StateManager stateManager = AppContextHolder.getBean("stateManager", StateManager.class);
    protected static DocIndexClient docIndexClient = AppContextHolder.getBean("docIndexClient", DocIndexClient.class);
    protected static RecipeManager recipeManager = AppContextHolder.getBean("recipeManager", RecipeManager.class);

    private static final Integer CA_BEFORE = new Integer(0);

    //我们将开方的流程拆开：
    //1.保存处方（公共操作）=》2.CA签名前操作=》3.CA签名后操作
    //因为拿到CA结果的时机不同，流程3中：前置是在推his前拿到的，所以在拿到结果后需要将处方做推his的相关操作;
    //后置则是在his新增回调的时候返回CA结果的，所以拿到结果会需要将处方向下流

    public static AbstractCaProcessType getCaProcessFactory(Integer organId) {
        //根据机构配置的CA模式获取具体的实现
        Integer frontOrBack = configurationClient.getValueCatchReturnInteger(organId, "CAFromHisCallBackOrder", CA_BEFORE);
        AbstractCaProcessType beanFactory = CARecipeTypeEnum.getCaProcessType(frontOrBack);
        if (null != beanFactory) {
            return beanFactory;
        } else {
            LOGGER.error("当前CA实现为空，默认实现后置！organId={}", organId);
            return new CaAfterProcessType();
        }

    }

    //CA签名前的处方操作
    public abstract void signCABeforeRecipeFunction(RecipeBean recipeBean, List<RecipeDetailBean> detailBeanList);

    //CA签名后的处方操作
    public abstract void signCAAfterRecipeCallBackFunction(RecipeBean recipeBean, List<RecipeDetailBean> detailBeanList);

    //his新增回调处方，调用generateRecipePdfAndSign方法触发CA
    public abstract RecipeResultBean hisCallBackCARecipeFunction(Integer recipeId);

    public void recipeHisResultBeforeCAFunction(RecipeBean recipeBean, List<RecipeDetailBean> detailBeanList) {
        LOGGER.info("AbstractCaProcessType recipeHisResultBeforeCAFunction start recipeBean={}", JSON.toJSONString(recipeBean));
        //前置签名，CA后操作，通过CA的结果做判断，通过则将处方推his
        //HIS消息发送--异步处理
        RecipeBusiThreadPool.execute(new PushRecipeToHisCallable(recipeBean.getRecipeId()));
        //非可使用省医保的处方立即发送处方卡片，使用省医保的处方需要在药师审核通过后显示
        try {
            if (!recipeBean.canMedicalPay()) {
                //发送卡片
                Recipe recipe = ObjectCopyUtils.convert(recipeBean, Recipe.class);
                List<Recipedetail> details = ObjectCopyUtils.convert(detailBeanList, Recipedetail.class);
                Map<String, Object> rMap = new HashMap<>();
                rMap.put("signResult", true);
                rMap.put("recipeId", recipeBean.getRecipeId());
                RecipeServiceSub.sendRecipeTagToPatient(recipe, details, rMap, false);
            }
        } catch (Exception e) {
            LOGGER.warn("AbstractCaProcessType recipeHisResultBeforeCAFunction sendRecipeTagToPatient{}", recipeBean.getRecipeId(), e);
        }
        LOGGER.info("AbstractCaProcessType recipeHisResultBeforeCAFunction end recipeBean={}", JSON.toJSONString(recipeBean));
    }


    /**
     * 签名成功后续操作
     *
     * @param recipe
     * @param memo
     */
    public void caComplete(Recipe recipe, String memo) {
        //设置处方签名成功后的处方的状态
        Integer status = RecipeStatusEnum.RECIPE_STATUS_CHECK_PASS.getType();
        //根据审方模式改变状态
        AuditModeContext auditModeContext = AppContextHolder.getBean("auditModeContext", AuditModeContext.class);
        auditModeContext.getAuditModes(recipe.getReviewType()).afterHisCallBackChange(status, recipe, memo);
        RecipeBusiThreadPool.execute(() -> {
            if(null==recipe.getReviewType()||"0".equals(recipe.getReviewType())){
                recipeManager.addRecipeNotify(recipe.getRecipeId(), JKHBConstant.NO_PAY);
            }
        });
        //配送处方标记 1:只能配送 更改处方取药方式
        if (RecipeBussConstant.RECIPEMODE_NGARIHEALTH.equals(recipe.getRecipeMode())
                && RecipeDistributionFlagEnum.DRUGS_HAVE.getType().equals(recipe.getDistributionFlag())) {
            try {
                RecipeHisService hisService = ApplicationUtils.getRecipeService(RecipeHisService.class);
                hisService.recipeDrugTake(recipe.getRecipeId(), PayConstant.PAY_FLAG_NOT_PAY, null);
            } catch (Exception e) {
                LOGGER.warn("retryDoctorSignCheck recipeId=[{}]更改取药方式异常", recipe.getRecipeId(), e);
            }
        }
        //互联网模式--- 医生开完处方之后聊天界面系统消息提示
        if (RecipeBussConstant.RECIPEMODE_ZJJGPT.equals(recipe.getRecipeMode()) && null != recipe.getClinicId()) {
            try {
                if (RecipeBussConstant.BUSS_SOURCE_FZ.equals(recipe.getBussSource())) {
                    IRecipeOnLineRevisitService recipeOnLineConsultService = RevisitAPI.getService(IRecipeOnLineRevisitService.class);
                    recipeOnLineConsultService.sendRecipeMsg(recipe.getClinicId(), 3);
                } else if (RecipeBussConstant.BUSS_SOURCE_WZ.equals(recipe.getBussSource())) {
                    IRecipeOnLineConsultService recipeOnLineConsultService = ConsultAPI.getService(IRecipeOnLineConsultService.class);
                    recipeOnLineConsultService.sendRecipeMsg(recipe.getClinicId(), 3);
                }
            } catch (Exception e) {
                LOGGER.error("retryDoctorSignCheck sendRecipeMsg error, type:3, consultId:{}, error:{}", recipe.getClinicId(), e);
            }
        }
        //推送处方到监管平台
        RecipeBusiThreadPool.submit(new PushRecipeToRegulationCallable(Collections.singletonList(recipe.getRecipeId()), 1));
        //保存电子病历
        docIndexClient.saveRecipeDocIndex(recipe);
    }
}