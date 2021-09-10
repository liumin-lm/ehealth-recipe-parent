package recipe.caNew;

import com.alibaba.fastjson.JSON;
import com.ngari.base.BaseAPI;
import com.ngari.base.property.service.IConfigurationCenterUtilsService;
import com.ngari.consult.ConsultAPI;
import com.ngari.consult.process.service.IRecipeOnLineConsultService;
import com.ngari.patient.service.BasicAPI;
import com.ngari.patient.service.PatientService;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.common.RecipeResultBean;
import com.ngari.recipe.entity.DrugsEnterprise;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.Recipedetail;
import com.ngari.recipe.recipe.constant.RecipeDistributionFlagEnum;
import com.ngari.recipe.recipe.model.RecipeBean;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import com.ngari.revisit.RevisitAPI;
import com.ngari.revisit.process.service.IRecipeOnLineRevisitService;
import ctd.persistence.DAOFactory;
import ctd.util.AppContextHolder;
import ctd.util.JSONUtils;
import eh.wxpay.constant.PayConstant;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.ApplicationUtils;
import recipe.audit.auditmode.AuditModeContext;
import recipe.bean.DrugEnterpriseResult;
import recipe.constant.RecipeBussConstant;
import recipe.constant.RecipeStatusConstant;
import recipe.dao.OrganAndDrugsepRelationDAO;
import recipe.dao.RecipeDAO;
import recipe.drugsenterprise.RemoteDrugEnterpriseService;
import recipe.service.DrugDistributionService;
import recipe.service.RecipeHisService;
import recipe.service.RecipeService;
import recipe.service.RecipeServiceSub;
import recipe.thread.PushRecipeToHisCallable;
import recipe.thread.PushRecipeToRegulationCallable;
import recipe.thread.RecipeBusiThreadPool;

import java.util.*;

import static ctd.persistence.DAOFactory.getDAO;

//JRK
//将CA流程上的特异点抽象出来
public abstract class AbstractCaProcessType {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractCaProcessType.class);

    private static final Integer CA_OLD_TYPE = new Integer(0);

    private static final Integer CA_NEW_TYPE = new Integer(1);

    private static final Integer CA_BEFORE = new Integer(0);

    private static final Integer CA_AFTER = new Integer(1);
    //我们将开方的流程拆开：
    //1.保存处方（公共操作）=》2.CA签名前操作=》3.CA签名后操作
    //因为拿到CA结果的时机不同，流程3中：前置是在推his前拿到的，所以在拿到结果后需要将处方做推his的相关操作;
    //后置则是在his新增回调的时候返回CA结果的，所以拿到结果会需要将处方向下流

    public static AbstractCaProcessType getCaProcessFactory(Integer organId){
        //根据机构配置的CA模式获取具体的实现
        IConfigurationCenterUtilsService configService = BaseAPI.getService(IConfigurationCenterUtilsService.class);
        //添加按钮配置项key
        Object caFromHisCallBackOrder = configService.getConfiguration(organId, "CAFromHisCallBackOrder");
        //先给个默认值
        Integer CAType = CA_BEFORE;
        if(null != caFromHisCallBackOrder){
            CAType = Integer.parseInt(caFromHisCallBackOrder.toString());
        }
        AbstractCaProcessType beanFactory = CARecipeTypeEnum.getCaProcessType(CAType);
        if(null != beanFactory){
            return beanFactory;
        }else{
            LOGGER.warn("当前CA实现为空，默认实现后置！");
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

        Map<String, Object> rMap = new HashMap<>();
        rMap.put("signResult", true);
        rMap.put("recipeId", recipeBean.getRecipeId());

        //先将处方状态设置成【医院确认中】
        RecipeDAO recipeDAO = getDAO(RecipeDAO.class);
        recipeDAO.updateRecipeInfoByRecipeId(recipeBean.getRecipeId(), RecipeStatusConstant.CHECKING_HOS, null);

        RecipeService recipeService = ApplicationUtils.getRecipeService(RecipeService.class);
        //前置签名，CA后操作，通过CA的结果做判断，通过则将处方推his
        //HIS消息发送--异步处理
        RecipeBusiThreadPool.submit(new PushRecipeToHisCallable(recipeBean.getRecipeId()));

        //非可使用省医保的处方立即发送处方卡片，使用省医保的处方需要在药师审核通过后显示
        if (!recipeBean.canMedicalPay()) {
            //发送卡片
            Recipe recipe = ObjectCopyUtils.convert(recipeBean, Recipe.class);
            List<Recipedetail> details = ObjectCopyUtils.convert(detailBeanList, Recipedetail.class);
            RecipeServiceSub.sendRecipeTagToPatient(recipe, details, rMap, false);
        }
        //个性化医院特殊处理，开完处方模拟his成功返回数据（假如前置机不提供默认返回数据）
        recipeService.doHisReturnSuccessForOrgan(recipeBean, rMap);


        LOGGER.info("AbstractCaProcessType recipeHisResultBeforeCAFunction end recipeBean={}", JSON.toJSONString(recipeBean));
     }
    

    public void recipeHisResultAfterCAFunction(Integer recipeId){
        RecipeDAO recipeDAO = getDAO(RecipeDAO.class);
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        if(null == recipe){
            LOGGER.warn("当前处方{}信息为空！", recipeId);
            return;
        }

        String recipeMode = recipe.getRecipeMode();
        Integer status = RecipeStatusConstant.CHECK_PASS;

        String memo = "";
        Integer CANewOldWay = CA_OLD_TYPE;
        IConfigurationCenterUtilsService configService = BaseAPI.getService(IConfigurationCenterUtilsService.class);
        Object caProcessType = configService.getConfiguration(recipe.getClinicOrgan(), "CAProcessType");
        if(null != caProcessType){
            CANewOldWay = Integer.parseInt(caProcessType.toString());
        }
        //兼容新老版本，日志
        if(CA_OLD_TYPE.equals(CANewOldWay)){
            memo = "HIS审核返回：写入his成功，审核通过";
        }else{
            memo = "HIS审核返回：写入his成功，审核通过---CA后置操作完成回调";
        }
        //其他平台处方状态不变
        if (0 == recipe.getFromflag()) {
            status = recipe.getStatus();
            //兼容新老版本，日志
            if(CA_OLD_TYPE.equals(CANewOldWay)){
                memo = "HIS审核返回：写入his成功(其他平台处方)";
            }else{
                memo = "HIS审核返回：写入his成功(其他平台处方)---CA后置操作完成回调";
            }
        }

        //TODO 根据审方模式改变状态
        //设置处方签名成功后的处方的状态
        //修改后这些CA签名后进行的所有处方流转的操作只有【后置】ca才会触发
        AuditModeContext auditModeContext = AppContextHolder.getBean("auditModeContext", AuditModeContext.class);
        auditModeContext.getAuditModes(recipe.getReviewType()).afterHisCallBackChange(status, recipe, memo);
        if (RecipeBussConstant.RECIPEMODE_NGARIHEALTH.equals(recipeMode)) {
            //配送处方标记 1:只能配送 更改处方取药方式
            if (RecipeDistributionFlagEnum.DRUGS_HAVE.getType().equals(recipe.getDistributionFlag())) {
                try {
                    RecipeHisService hisService = ApplicationUtils.getRecipeService(RecipeHisService.class);
                    RecipeResultBean result1 = hisService.recipeDrugTake(recipe.getRecipeId(), PayConstant.PAY_FLAG_NOT_PAY, null);
                    if (RecipeResultBean.FAIL.equals(result1.getCode())) {
                        LOGGER.warn("retryDoctorSignCheck recipeId=[{}]更改取药方式失败，error=[{}]", recipe.getRecipeId(), result1.getError());
                        //不能影响流程去掉异常
                        /*throw new DAOException(ErrorCode.SERVICE_ERROR, "更改取药方式失败，错误:" + result1.getError());*/
                    }
                } catch (Exception e) {
                    LOGGER.warn("retryDoctorSignCheck recipeId=[{}]更改取药方式异常", recipe.getRecipeId(), e);
                }
            }
        }
        //2019/5/16 互联网模式--- 医生开完处方之后聊天界面系统消息提示
        if (RecipeBussConstant.RECIPEMODE_ZJJGPT.equals(recipeMode)) {
            Integer consultId = recipe.getClinicId();
            if (null != consultId) {
                try {
                    if (RecipeBussConstant.BUSS_SOURCE_FZ.equals(recipe.getBussSource())) {
                        IRecipeOnLineRevisitService recipeOnLineConsultService = RevisitAPI.getService(IRecipeOnLineRevisitService.class);
                        recipeOnLineConsultService.sendRecipeMsg(consultId, 3);
                    } else if (RecipeBussConstant.BUSS_SOURCE_WZ.equals(recipe.getBussSource())) {
                        IRecipeOnLineConsultService recipeOnLineConsultService = ConsultAPI.getService(IRecipeOnLineConsultService.class);
                        recipeOnLineConsultService.sendRecipeMsg(consultId, 3);
                    }
                } catch (Exception e) {
                    LOGGER.error("retryDoctorSignCheck sendRecipeMsg error, type:3, consultId:{}, error:{}", consultId, e);
                }

            }
        }
        //推送处方到监管平台
        RecipeBusiThreadPool.submit(new PushRecipeToRegulationCallable(Collections.singletonList(recipeId), 1));

        //将原先互联网回调修改处方的推送的逻辑移到这里
        //判断是否是阿里药企，是阿里大药房就推送处方给药企
        OrganAndDrugsepRelationDAO organAndDrugsepRelationDAO = DAOFactory.getDAO(OrganAndDrugsepRelationDAO.class);
        List<DrugsEnterprise> drugsEnterprises = organAndDrugsepRelationDAO.findDrugsEnterpriseByOrganIdAndStatus(recipe.getClinicOrgan(), 1);
        if (CollectionUtils.isEmpty(drugsEnterprises)) {
            return;
        }
        DrugsEnterprise drugsEnterprise = drugsEnterprises.get(0);
        if ("aldyf".equals(drugsEnterprise.getCallSys())) {
            //判断用户是否已鉴权
            if (StringUtils.isNotEmpty(recipe.getRequestMpiId())) {
                DrugDistributionService drugDistributionService = ApplicationUtils.getRecipeService(DrugDistributionService.class);
                PatientService patientService = BasicAPI.getService(PatientService.class);
                String loginId = patientService.getLoginIdByMpiId(recipe.getRequestMpiId());
                if (drugDistributionService.authorization(loginId)) {
                    //推送阿里处方推片和信息
                    if (null == drugsEnterprise) {
                        LOGGER.warn("updateRecipeStatus aldyf 药企不存在");
                    }
                    RemoteDrugEnterpriseService remoteDrugEnterpriseService = ApplicationUtils.getRecipeService(RemoteDrugEnterpriseService.class);
                    DrugEnterpriseResult deptResult =
                            remoteDrugEnterpriseService.pushSingleRecipeInfoWithDepId(recipeId, drugsEnterprise.getId());
                    LOGGER.info("updateRecipeStatus 推送药企处方，result={}", JSONUtils.toString(deptResult));
                }
            }
        }
    }
}