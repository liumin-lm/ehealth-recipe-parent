package recipe.mq;

import com.google.common.collect.ImmutableMap;
import com.ngari.home.asyn.model.BussCancelEvent;
import com.ngari.home.asyn.service.IAsynDoBussService;
import com.ngari.recipe.entity.DrugsEnterprise;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeOrder;
import ctd.net.broadcast.MQHelper;
import ctd.net.broadcast.MQSubscriber;
import ctd.net.broadcast.Observer;
import ctd.persistence.DAOFactory;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import eh.cdr.constant.OrderStatusConstant;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.ApplicationUtils;
import recipe.bean.DrugEnterpriseResult;
import recipe.constant.*;
import recipe.dao.OrganAndDrugsepRelationDAO;
import recipe.dao.RecipeDAO;
import recipe.dao.RecipeOrderDAO;
import recipe.drugsenterprise.AccessDrugEnterpriseService;
import recipe.drugsenterprise.RemoteDrugEnterpriseService;
import recipe.service.*;
import recipe.thread.PushRecipeToRegulationCallable;
import recipe.thread.RecipeBusiThreadPool;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

import static ctd.persistence.DAOFactory.getDAO;

/**
 * @author Created by liuxiaofeng on 2021/1/26 0026.
 *         处方失效时间非当天24点且小于24小时失效消息消费
 */
@RpcBean
public class RecipeInvalidMsgConsumer {

    /**
     * logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(BusMsgConsumer.class);

    /**
     * 订阅消息
     */
    @PostConstruct
    @RpcService
    public void recipeInvalidMsgConsumer() {
        LOGGER.info("recipeInvalidMsgConsumer,topic={},tag={}", OnsConfig.recipeDelayTopic, RecipeSystemConstant.RECIPE_INVALID_TOPIC_TAG);
        if (!OnsConfig.onsSwitch) {
            LOGGER.error("recipeInvalidMsgConsumer，the onsSwitch is set off, consumer not subscribe.");
            return;
        }

        MQSubscriber subscriber = MQHelper.getMqSubscriber();
        subscriber.attach(OnsConfig.recipeDelayTopic, RecipeSystemConstant.RECIPE_INVALID_TOPIC_TAG, new Observer<String>() {
                    @Override
                    public void onMessage(String msg) {
                        LOGGER.info("recipeInvalidMsgConsumer msg[{}]", msg);
                        RecipeOrderDAO orderDAO = getDAO(RecipeOrderDAO.class);
                        RecipeDAO recipeDAO = getDAO(RecipeDAO.class);
                        RecipeOrderService orderService = ApplicationUtils.getRecipeService(RecipeOrderService.class);
                        RecipeHisService hisService = ApplicationUtils.getRecipeService(RecipeHisService.class);

                        Recipe recipe = recipeDAO.getByRecipeId(Integer.parseInt(msg));
                        //过滤掉流转到扁鹊处方流转平台的处方
                        if (RecipeServiceSub.isBQEnterpriseBydepId(recipe.getEnterpriseId())) {
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
                        Integer status = RecipeService.getStatus(recipeDAO, recipe, recipeId);
                        //变更处方状态
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

                }
        );

    }


}
