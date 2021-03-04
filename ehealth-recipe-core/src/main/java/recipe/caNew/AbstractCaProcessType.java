package recipe.caNew;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.ImmutableMap;
import com.ngari.base.BaseAPI;
import com.ngari.base.property.service.IConfigurationCenterUtilsService;
import com.ngari.base.scratchable.service.IScratchableService;
import com.ngari.consult.ConsultAPI;
import com.ngari.consult.process.service.IRecipeOnLineConsultService;
import com.ngari.patient.service.BasicAPI;
import com.ngari.patient.service.PatientService;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.common.RecipeResultBean;
import com.ngari.recipe.entity.DrugsEnterprise;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.Recipedetail;
import com.ngari.recipe.recipe.model.RecipeBean;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import com.ngari.revisit.RevisitAPI;
import com.ngari.revisit.process.service.IRecipeOnLineRevisitService;
import ctd.persistence.DAOFactory;
import ctd.persistence.exception.DAOException;
import ctd.util.AppContextHolder;
import ctd.util.JSONUtils;
import eh.entity.base.Scratchable;
import eh.wxpay.constant.PayConstant;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.ApplicationUtils;
import recipe.audit.auditmode.AuditModeContext;
import recipe.bean.DrugEnterpriseResult;
import recipe.bussutil.CreateRecipePdfUtil;
import recipe.constant.ErrorCode;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
            if (Integer.valueOf(1).equals(recipe.getDistributionFlag())) {
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
        RecipeBusiThreadPool.submit(new PushRecipeToRegulationCallable(recipe.getRecipeId(), 1));

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

    /**
     * 新版本前置CA his回调之后给处方pdf添加处方号和患者病历号
     * @param recipeId
     */
    public static void addRecipeCodeAndPatientForRecipePdf(Integer recipeId){
        try {
            RecipeDAO recipeDAO = getDAO(RecipeDAO.class);
            Recipe recipe=recipeDAO.getByRecipeId(recipeId);
            if(recipe==null){
                return;
            }
            String newPdf = null;
            String key = "SignFile";
            String recipeCode="";
            //模块一的总大小
            int moduleOneSize=0;
            //根据模块一的大小和每个字段的位置，计算字段的坐标
            Map<String,String> positionMap=new HashMap<>();

//            IConfigurationCenterUtilsService configService = ApplicationUtils.getBaseService(IConfigurationCenterUtilsService.class);
//            Object recipeNumber = configService.getConfiguration(recipe.getClinicOrgan(), "recipeNumber");
//            LOGGER.info("addRecipeCodeAndPatientForRecipePdf  recipeId={},recipeNumber={}", recipeId, recipeNumber);
//            if (null == recipeNumber ||StringUtils.isEmpty(recipeNumber.toString())) {
//                return;
//            }
            //{ "id": 1, "text": "平台处方单号" , "locked": true},{ "id": 2, "text": "his处方单号" }
//            if(Integer.parseInt(recipeNumber.toString()) ==1){
//                recipeCode=recipeId.toString();
//            }else{
//                recipeCode=recipe.getRecipeCode();
//            }
            //获取model one 配置，根据配置判断是否配置了字段（暂时按固定格式）
            IScratchableService scratchableService  = AppContextHolder.getBean("eh.scratchableService", IScratchableService.class);
            Map<String, Object> labelMap = scratchableService.findRecipeListDetail(recipe.getClinicOrgan().toString());
            if (org.springframework.util.CollectionUtils.isEmpty(labelMap)) {
                throw new DAOException(ErrorCode.SERVICE_ERROR, "运营平台配置为空");
            }
            List<Scratchable> moduleOne = (List<Scratchable>) labelMap.get("moduleOne");
            if (!org.springframework.util.CollectionUtils.isEmpty(moduleOne) ) {
                moduleOneSize=moduleOne.size();
                int i=0;
                for(Scratchable scratchable:moduleOne){
                    i++;
                    //position**   字段存在模块一的位置（用于计算替换的位置）
                    if("recipe.recipeCode".equals(scratchable.getBoxLink().trim())&&"处方单号".equals(scratchable.getBoxTxt().trim())){
                        recipeCode=recipe.getRecipeCode();
                        positionMap.put("recipeCodeName",scratchable.getBoxTxt().trim());
                        positionMap.put("positionRecipeCode",String.valueOf(i));
                        continue;
                    }
                    if("recipe.recipeId".equals(scratchable.getBoxLink().trim())&&"处方单号".equals(scratchable.getBoxTxt().trim())){
                        positionMap.put("recipeIdName",scratchable.getBoxTxt().trim());
                        positionMap.put("positionRecipeId",String.valueOf(i));
                        continue;
                    }
                    if("recipe.patientID".equals(scratchable.getBoxLink().trim())&&"病历号".equals(scratchable.getBoxTxt().trim())){
                        positionMap.put("patientIdName",scratchable.getBoxTxt().trim());
                        positionMap.put("positionPatientId",String.valueOf(i));
                        continue;
                    }

                }
                positionMap.put("moduleOneSize",String.valueOf(moduleOneSize));
            }

            //获取抬头配置
            List<Scratchable> moduleFive = (List<Scratchable>) labelMap.get("moduleFive");
            if (!org.springframework.util.CollectionUtils.isEmpty(moduleFive) ) {
                for(Scratchable scratchable:moduleFive){
                    //获取条形码配置的值
                    if("条形码".equals(scratchable.getBoxTxt().trim())){
                        positionMap.put("positionBarCode","1");
                        //如果条形码配置成病历
                        if("recipe.patientID".equals(scratchable.getBoxLink().trim())){
                            positionMap.put("barCodeValue",recipe.getPatientID().trim());
                        }else if("recipe.recipeCode".equals(scratchable.getBoxLink().trim())){
                            positionMap.put("barCodeValue",recipe.getRecipeCode());
                        }else{
                            positionMap.put("barCodeValue","");
                        }
                    }
                    break;
                }
            }
            newPdf= CreateRecipePdfUtil.generateRecipeCodeAndPatientIdForRecipePdf(recipe.getSignFile(),recipeCode,recipeId,recipe.getPatientID(),positionMap);
            LOGGER.info("addRecipeCodeAndPatientForRecipePdf  recipeId={},newPdf={}", recipeId, newPdf);
            newPdf=CreateRecipePdfUtil.generateBarCodeInRecipePdf(newPdf,positionMap);
            LOGGER.info("addRecipeCodeAndPatientForRecipePdf 条形码 recipeId={},newPdf={}", recipeId, newPdf);
            if (StringUtils.isNotEmpty(newPdf) && StringUtils.isNotEmpty(key)) {
                recipeDAO.updateRecipeInfoByRecipeId(recipeId, ImmutableMap.of(key, newPdf));
            }
        } catch (Exception e) {
            LOGGER.error("addRecipeCodeAndPatientForRecipePdf error recipeId={},e={}", recipeId, e);
        }
    }

}