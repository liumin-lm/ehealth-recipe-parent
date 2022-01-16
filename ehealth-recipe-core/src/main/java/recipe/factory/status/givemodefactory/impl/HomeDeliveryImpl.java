package recipe.factory.status.givemodefactory.impl;

import com.ngari.base.patient.model.PatientBean;
import com.ngari.base.patient.service.IPatientService;
import com.ngari.his.recipe.mode.DrugTakeChangeReqTO;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeOrder;
import com.ngari.recipe.entity.Recipedetail;
import com.ngari.recipe.vo.UpdateOrderStatusVO;
import ctd.persistence.exception.DAOException;
import ctd.util.AppContextHolder;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.ApplicationUtils;
import recipe.common.response.CommonResponse;
import recipe.constant.ErrorCode;
import recipe.constant.RecipeBussConstant;
import recipe.constant.RecipeMsgEnum;
import recipe.constant.RecipeStatusConstant;
import recipe.dao.RecipeDetailDAO;
import recipe.drugsenterprise.ThirdEnterpriseCallService;
import recipe.enumerate.status.GiveModeEnum;
import recipe.enumerate.status.RecipeOrderStatusEnum;
import recipe.enumerate.status.RecipeStatusEnum;
import recipe.hisservice.HisRequestInit;
import recipe.hisservice.RecipeToHisService;
import recipe.hisservice.syncdata.HisSyncSupervisionService;
import recipe.manager.OrderManager;
import recipe.service.RecipeLogService;
import recipe.service.RecipeMsgService;
import recipe.thread.RecipeBusiThreadPool;
import recipe.util.DictionaryUtil;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

/**
 * 配送到家
 *
 * @author fuzi
 */
@Service
public class HomeDeliveryImpl extends AbstractGiveMode {
    @Autowired
    private IPatientService patientService;

    @Resource
    private RecipeDetailDAO recipeDetailDAO;

    @Autowired
    private OrderManager orderManager;

    @Override
    public Integer getGiveMode() {
        return GiveModeEnum.GIVE_MODE_HOME_DELIVERY.getType();
    }

    @Override
    public void updateStatus(UpdateOrderStatusVO orderStatus) {
        if (null != orderStatus.getLogisticsCompany() || StringUtils.isNotEmpty(orderStatus.getTrackingNumber())) {
            //检查运营人员维护的运单号是否已经存在
            List<RecipeOrder> recipeOrders = recipeOrderDAO.findByLogisticsCompanyAndTrackingNumber(orderStatus.getOrderId(), orderStatus.getLogisticsCompany(), orderStatus.getTrackingNumber());
            if (CollectionUtils.isNotEmpty(recipeOrders)) {
                throw new DAOException(ErrorCode.SERVICE_ERROR, "运单号重复");
            }
        }
        orderStatus.setSender("system");
        RecipeOrder recipeOrder = new RecipeOrder(orderStatus.getOrderId());
        Recipe recipe = super.getRecipe(orderStatus.getRecipeId());
        //如果是货到付款还要更新付款时间和付款状态
        if (RecipeBussConstant.PAYMODE_OFFLINE.equals(recipeOrder.getPayMode())) {
            Date date = new Date();
            recipeOrder.setPayTime(date);
            recipe.setPayDate(date);
            recipe.setPayFlag(1);
        }
        recipeOrderStatusProxy.updateOrderByStatus(orderStatus, recipeOrder, recipe);
    }


    @Override
    public void updateStatusAfter(UpdateOrderStatusVO orderStatus) {
        Integer recipeId = orderStatus.getRecipeId();
        Recipe recipe = getRecipe(recipeId);
        if (RecipeOrderStatusEnum.ORDER_STATUS_DONE.getType().equals(orderStatus.getTargetRecipeOrderStatus())) {
            //HIS消息发送
            RecipeMsgService.batchSendMsg(recipe, RecipeStatusConstant.PATIENT_REACHPAY_FINISH);
            //监管平台上传配送信息(配送到家-处方完成)
            RecipeBusiThreadPool.execute(() -> {
                HisSyncSupervisionService hisSyncService = ApplicationUtils.getRecipeService(HisSyncSupervisionService.class);
                CommonResponse response = hisSyncService.uploadFinishMedicine(recipeId);
                RecipeLogService.saveRecipeLog(recipeId, recipe.getStatus(), RecipeStatusEnum.RECIPE_STATUS_FINISH.getType(),
                        "监管平台配送信息[完成]上传code" + response.getCode() + ",msg:" + response.getMsg());
            });
        }

        if (null != orderStatus.getLogisticsCompany() || StringUtils.isNotBlank(orderStatus.getTrackingNumber())) {
            RecipeOrder recipeOrder = orderManager.getRecipeOrderById(orderStatus.getOrderId());
            orderManager.updateOrderLogisticsInfo(orderStatus.getOrderId(), orderStatus.getLogisticsCompany(), orderStatus.getTrackingNumber());
            //同步运单信息至基础服务
            ThirdEnterpriseCallService.sendLogisticsInfoToBase(orderStatus.getRecipeId(), orderStatus.getLogisticsCompany() + "", orderStatus.getTrackingNumber());
            if (StringUtils.isEmpty(recipeOrder.getTrackingNumber())) {
                //更新快递信息后，发送消息
                RecipeMsgService.batchSendMsg(orderStatus.getRecipeId(), RecipeMsgEnum.EXPRESSINFO_REMIND.getStatus());
            }
            //将快递公司快递单号信息用更新配送方式接口更新至his
            if (StringUtils.isEmpty(recipe.getMpiid())) {
                return;
            }
            RecipeBusiThreadPool.execute(() -> {
                RecipeToHisService service = AppContextHolder.getBean("recipeToHisService", RecipeToHisService.class);
                List<Recipedetail> details = recipeDetailDAO.findByRecipeId(recipe.getRecipeId());
                PatientBean patientBean = patientService.get(recipe.getMpiid());
                DrugTakeChangeReqTO request = HisRequestInit.initDrugTakeChangeReqTO(recipe, details, patientBean, null);
                service.drugTakeChange(request);
            });
            //记录日志
            String company = DictionaryUtil.getDictionary("eh.cdr.dictionary.LogisticsCompany", orderStatus.getLogisticsCompany());
            RecipeLogService.saveRecipeLog(orderStatus.getRecipeId(), orderStatus.getSourceRecipeOrderStatus()
                    , orderStatus.getTargetRecipeOrderStatus(), "配送中,配送人：" + orderStatus.getSender() +
                            ",快递公司：" + company + ",快递单号：" + orderStatus.getTrackingNumber());
        }
    }
}
