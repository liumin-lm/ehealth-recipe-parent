//package recipe.recipecheck;
//
//import com.google.common.collect.Maps;
//import com.ngari.base.organconfig.service.IOrganConfigService;
//import com.ngari.home.asyn.model.BussFinishEvent;
//import com.ngari.home.asyn.service.IAsynDoBussService;
//import com.ngari.recipe.entity.DrugsEnterprise;
//import com.ngari.recipe.entity.Recipe;
//import ctd.persistence.DAOFactory;
//import ctd.persistence.exception.DAOException;
//import ctd.util.annotation.RpcBean;
//import ctd.util.annotation.RpcService;
//import ctd.util.event.GlobalEventExecFactory;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import recipe.ApplicationUtils;
//import recipe.audit.auditmode.AuditModeContext;
//import recipe.bean.CheckYsInfoBean;
//import recipe.constant.BussTypeConstant;
//import recipe.constant.RecipeBussConstant;
//import recipe.constant.RecipeMsgEnum;
//import recipe.constant.RecipecCheckStatusConstant;
//import recipe.dao.OrganAndDrugsepRelationDAO;
//import recipe.dao.RecipeDAO;
//import recipe.drugsenterprise.RemoteDrugEnterpriseService;
//import recipe.service.*;
//import recipe.thread.PushRecipeToRegulationCallable;
//import recipe.thread.RecipeBusiThreadPool;
//import recipe.util.MapValueUtil;
//
//import javax.annotation.Resource;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
///**
// * created by shiyuping on 2020/3/17
// * 第三方处方审核结果通知接口
// */
//
//@RpcBean("platRecipeCheckService")
//public class PlatRecipeCheckService implements IRecipeCheckService{
//    /**
//     * LOGGER
//     */
//    private static final Logger LOGGER = LoggerFactory.getLogger(PlatRecipeCheckService.class);
//
//    @Resource
//    private AuditModeContext auditModeContext;
//
//
//    /**
//     * 保存平台处方通知审核结果
//     *
//     * @param paramMap 包含以下属性
//     *                 int         recipeId 处方ID
//     *                 int        checkOrgan  检查机构
//     *                 int        checker    检查人员
//     *                 int        result  1:审核通过 0-通过失败
//     *                 String     failMemo 备注
//     * @return boolean
//     */
//    @Override
//    @RpcService
//    public Map<String, Object> saveCheckResult(Map<String, Object> paramMap) {
//        Map<String, Object> resMap = Maps.newHashMap();
////
////        Integer recipeId = MapValueUtil.getInteger(paramMap, "recipeId");
////        Integer result = MapValueUtil.getInteger(paramMap, "result");
////        if (null == recipeId || null == result) {
////            throw new DAOException(DAOException.VALUE_NEEDED, "params are needed");
////        }
////
////        RecipeService recipeService = ApplicationUtils.getRecipeService(RecipeService.class);
////        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
////
////        //审核处方单（药师相关数据处理）
////        CheckYsInfoBean resultBean = recipeService.reviewRecipe(paramMap);
////        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
////
////        Map<String, Object> resMap = Maps.newHashMap();
////        resMap.put("result", resultBean.isRs());
////        resMap.put("recipeId", recipeId);
////        //把审核结果再返回前端 0:未审核 1:通过 2:不通过
////        resMap.put("check", (1 == result) ? 1 : 2);
////
////        //date 20200507
////        //将签名从不那个审核处方的逻辑中拆分出来
////        recipeService.retryPharmacistSignCheck(recipeId, recipe.getChecker(), resMap);
////        //签名失败，设置审核结果为失败
////        RecipeBusiThreadPool.submit(new PushRecipeToRegulationCallable(recipe.getRecipeId(),2));
//        return resMap;
//    }
//
//
//    public void doAfterCheckNotPassYs(Recipe recipe) {
//        boolean secondsignflag = RecipeServiceSub.canSecondAudit(recipe.getClinicOrgan());
//        /*IOrganConfigService iOrganConfigService = ApplicationUtils.getBaseService(IOrganConfigService.class);
//        boolean secondsignflag = iOrganConfigService.getEnableSecondsignByOrganId(recipe.getClinicOrgan());*/
//        //不支持二次签名的机构直接执行后续操作
//        if (!secondsignflag) {
//            //一次审核不通过的需要将优惠券释放
//            RecipeCouponService recipeCouponService = ApplicationUtils.getRecipeService(RecipeCouponService.class);
//            recipeCouponService.unuseCouponByRecipeId(recipe.getRecipeId());
//            //TODO 根据审方模式改变
//            auditModeContext.getAuditModes(recipe.getReviewType()).afterCheckNotPassYs(recipe);
//            //HIS消息发送
//            //审核不通过 往his更新状态（已取消）
//            RecipeHisService hisService = ApplicationUtils.getRecipeService(RecipeHisService.class);
//            hisService.recipeStatusUpdate(recipe.getRecipeId());
//            //记录日志
//            RecipeLogService.saveRecipeLog(recipe.getRecipeId(), recipe.getStatus(), recipe.getStatus(), "审核不通过处理完成");
//        }else{
//            //需要二次审核，这里是一次审核不通过的流程
//            //需要将处方的审核状态设置成一次审核不通过的状态
//            Map<String, Object> updateMap = new HashMap<>();
//            RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
//            updateMap.put("checkStatus", RecipecCheckStatusConstant.First_Check_No_Pass);
//            recipeDAO.updateRecipeInfoByRecipeId(recipe.getRecipeId(), updateMap);
//        }
//        //由于支持二次签名的机构第一次审方不通过时医生收不到消息。所以将审核不通过推送消息放这里处理
//        sendCheckNotPassYsMsg(recipe);
//    }
//
//    private void sendCheckNotPassYsMsg(Recipe recipe) {
//        RecipeDAO rDao = DAOFactory.getDAO(RecipeDAO.class);
//        if (null == recipe) {
//            return;
//        }
//        recipe = rDao.get(recipe.getRecipeId());
//        if (RecipeBussConstant.FROMFLAG_HIS_USE.equals(recipe.getFromflag())) {
//            //发送审核不成功消息
//            //${sendOrgan}：抱歉，您的处方未通过药师审核。如有收取费用，款项将为您退回，预计1-5个工作日到账。如有疑问，请联系开方医生或拨打${customerTel}联系小纳。
//            RecipeMsgService.sendRecipeMsg(RecipeMsgEnum.RECIPE_YS_CHECKNOTPASS_4HIS, recipe);
//            //date 2019/10/10
//            //添加判断 一次审核不通过不需要向患者发送消息
//        }else if (RecipeBussConstant.FROMFLAG_PLATFORM.equals(recipe.getFromflag())){
//            //发送审核不成功消息
//            //处方审核不通过通知您的处方单审核不通过，如有疑问，请联系开方医生
//            RecipeMsgService.batchSendMsg(recipe, eh.cdr.constant.RecipeStatusConstant.CHECK_NOT_PASSYS_REACHPAY);
//        }
//    }
//}
