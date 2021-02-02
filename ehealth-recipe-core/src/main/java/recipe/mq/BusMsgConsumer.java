package recipe.mq;

import com.google.common.collect.ImmutableMap;
import com.ngari.common.dto.TempMsgType;
import com.ngari.home.asyn.model.BussCancelEvent;
import com.ngari.home.asyn.service.IAsynDoBussService;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeOrder;
import ctd.net.broadcast.MQHelper;
import ctd.net.broadcast.MQSubscriber;
import ctd.net.broadcast.Observer;
import ctd.util.AppContextHolder;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import eh.cdr.constant.OrderStatusConstant;
import eh.msg.constant.MqConstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.ApplicationUtils;
import recipe.constant.*;
import recipe.dao.RecipeDAO;
import recipe.dao.RecipeOrderDAO;
import recipe.service.*;
import recipe.serviceprovider.recipe.service.RemoteRecipeService;
import recipe.thread.PushRecipeToRegulationCallable;
import recipe.thread.RecipeBusiThreadPool;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

import static ctd.persistence.DAOFactory.getDAO;

@RpcBean
public class BusMsgConsumer {

    /**
     * logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(BusMsgConsumer.class);

    /**
     * 订阅消息
     */
    @PostConstruct
    @RpcService
    public void busRecipeMsgConsumer() {
        //该对象不可删除，此处需要初始化
        OnsConfig onsConfig = (OnsConfig) AppContextHolder.getBean("onsConfig");
        if (!OnsConfig.onsSwitch) {
            LOGGER.info("the onsSwitch is set off, consumer not subscribe.");
            return;
        }
        LOGGER.info("busRecipeMsgConsumer start");

        MQSubscriber subscriber = MQHelper.getMqSubscriber();
        /**
         * basic相关消息
         */
        subscriber.attach(OnsConfig.basicInfoTopic, new Observer<TempMsgType>() {
            @Override
            public void onMessage(TempMsgType tMsg) {
                LOGGER.info("basicInfoTopic msg[{}]", JSONUtils.toString(tMsg));
                invalidPatient(tMsg);
            }
        });

        /**
         * 接收HIS消息处理
         */
        subscriber.attach(OnsConfig.hisCdrinfo, MqConstant.HIS_CDRINFO_TAG_TO_PLATFORM,
                new RecipeStatusFromHisObserver());

        /**
         * 接收药品修改消息
         */
        subscriber.attach(OnsConfig.dbModifyTopic, "base_druglist||base_organdruglist",
                new DrugSyncObserver());

        /**
         * 接收电子病历删除发送
         */
        subscriber.attach(OnsConfig.emrRecipe, "emrDeleted_recipe", new MqEmrRecipeServer());

        /**
         * 接收处方失效延迟消息
         */
        subscriber.attach(OnsConfig.recipeDelayTopic, RecipeSystemConstant.RECIPE_INVALID_TOPIC_TAG, new Observer<String>() {
                    @Override
                    public void onMessage(String msg) {
                        LOGGER.info("recipeInvalidMsgConsumer msg[{}]", msg);
                        handleRecipeInvalidMsg(msg);
                    }

                }
        );

        /*
        subscriber.attach(OnsConfig.hisCdrinfo, "recipeMedicalInfoFromHis",
                new RecipeMedicalInfoFromHisObserver());*/

    }

    private void handleRecipeInvalidMsg(String msg) {
        RecipeOrderDAO orderDAO = getDAO(RecipeOrderDAO.class);
        RecipeDAO recipeDAO = getDAO(RecipeDAO.class);
        RecipeOrderService orderService = ApplicationUtils.getRecipeService(RecipeOrderService.class);
        RecipeHisService hisService = ApplicationUtils.getRecipeService(RecipeHisService.class);

        Recipe recipe = recipeDAO.getByRecipeId(Integer.parseInt(msg));
        //过滤掉流转到扁鹊处方流转平台的处方
        if (recipe == null || RecipeServiceSub.isBQEnterpriseBydepId(recipe.getEnterpriseId())) {
            return;
        }
        //向药企推送处方过期的通知
        RecipeService.sendDrugEnterproseMsg(recipe);
        StringBuilder memo = new StringBuilder();
        memo.delete(0, memo.length());
        int recipeId = recipe.getRecipeId();
        //相应订单处理
        RecipeOrder order = orderDAO.getOrderByRecipeId(recipeId);
        orderService.cancelOrder(order, OrderStatusConstant.CANCEL_AUTO, true);
        if (recipe.getFromflag().equals(RecipeBussConstant.FROMFLAG_HIS_USE)) {
            if (null != order) {
                orderDAO.updateByOrdeCode(order.getOrderCode(), ImmutableMap.of("cancelReason", "患者未在规定时间内支付，该处方单已失效"));
            }
            //发送超时取消消息
            //${sendOrgan}：抱歉，您的处方单由于超过失效时间未处理，处方单已失效。如有疑问，请联系开方医生或拨打${customerTel}联系小纳。
            RecipeMsgService.sendRecipeMsg(RecipeMsgEnum.RECIPE_CANCEL_4HIS, recipe);
        }
        // 获取处方状态：未支付/未处理
        Integer status = RecipeService.getStatus(recipe);
        //变更处方状态
        if (status != null){
            recipeDAO.updateRecipeInfoByRecipeId(recipeId, status, ImmutableMap.of("chooseFlag", 1));
        }
        RecipeMsgService.batchSendMsg(recipe, status);
        if (RecipeBussConstant.RECIPEMODE_NGARIHEALTH.equals(recipe.getRecipeMode())) {
            //药师首页待处理任务---取消未结束任务
            ApplicationUtils.getBaseService(IAsynDoBussService.class).fireEvent(new BussCancelEvent(recipeId, BussTypeConstant.RECIPE));
        }

        if (RecipeStatusConstant.NO_PAY == status) {
            memo.append("已取消,超过失效时间未支付");
        } else if (RecipeStatusConstant.NO_OPERATOR == status) {
            memo.append("已取消,超过失效时间未操作");
        } else {
            memo.append("未知状态:" + status);
        }
        if (RecipeStatusConstant.NO_PAY == status) {
            //未支付，三天后自动取消后，优惠券自动释放
            RecipeCouponService recipeCouponService = ApplicationUtils.getRecipeService(RecipeCouponService.class);
            recipeCouponService.unuseCouponByRecipeId(recipeId);
        }
        //推送处方到监管平台
        RecipeBusiThreadPool.submit(new PushRecipeToRegulationCallable(recipe.getRecipeId(), 1));
        //HIS消息发送
        boolean succFlag = hisService.recipeStatusUpdate(recipeId);
        if (succFlag) {
            memo.append(",HIS推送成功");
        } else {
            memo.append(",HIS推送失败");
        }
        //保存处方状态变更日志
        RecipeLogService.saveRecipeLog(recipeId, RecipeStatusConstant.CHECK_PASS, status, memo.toString());
        //修改cdr_his_recipe status为已处理
        List<Recipe> recipeList = new ArrayList<>();
        recipeList.add(recipe);
        orderService.updateHisRecieStatus(recipeList);
    }

    @RpcService
    public void invalidPatient(TempMsgType tMsg) {
        if (MsgTypeEnum.DELETE_PATIENT.equals(tMsg.getMsgType())) {
            RemoteRecipeService remoteRecipeService = ApplicationUtils.getRecipeService(RemoteRecipeService.class);
            remoteRecipeService.synPatientStatusToRecipe(tMsg.getMsgContent());
        }
    }
}
