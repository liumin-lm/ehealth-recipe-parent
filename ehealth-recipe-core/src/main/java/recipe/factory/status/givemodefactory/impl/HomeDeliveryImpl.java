package recipe.factory.status.givemodefactory.impl;

import com.ngari.base.patient.model.PatientBean;
import com.ngari.base.patient.service.IPatientService;
import com.ngari.his.recipe.mode.DrugTakeChangeReqTO;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeOrder;
import com.ngari.recipe.entity.Recipedetail;
import com.ngari.recipe.vo.UpdateOrderStatusVO;
import ctd.controller.exception.ControllerException;
import ctd.dictionary.DictionaryController;
import ctd.persistence.DAOFactory;
import ctd.util.AppContextHolder;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.constant.RecipeStatusConstant;
import recipe.dao.RecipeDetailDAO;
import recipe.factory.status.constant.GiveModeEnum;
import recipe.hisservice.HisRequestInit;
import recipe.hisservice.RecipeToHisService;
import recipe.service.RecipeLogService;
import recipe.thread.RecipeBusiThreadPool;

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

    @Override
    public Integer getGiveMode() {
        return GiveModeEnum.GIVE_MODE_HOME_DELIVERY.getType();
    }

    @Override
    public void updateStatus(UpdateOrderStatusVO orderStatus) {
        orderStatus.setSender("system");
        orderStatus.setTargetRecipeStatus(RecipeStatusConstant.WAIT_SEND);
        RecipeOrder recipeOrder = new RecipeOrder(orderStatus.getOrderId());
        if (null != orderStatus.getLogisticsCompany()) {
            recipeOrder.setLogisticsCompany(orderStatus.getLogisticsCompany());
        }
        if (StringUtils.isNotEmpty(orderStatus.getTrackingNumber())) {
            recipeOrder.setTrackingNumber(orderStatus.getTrackingNumber());
        }
        Recipe recipe = recipeOrderStatusProxy.updateOrderByStatus(orderStatus, recipeOrder);
        //记录日志
        String company = orderStatus.getLogisticsCompany().toString();
        try {
            company = DictionaryController.instance().get("eh.cdr.dictionary.LogisticsCompany").getText(orderStatus.getLogisticsCompany());
        } catch (ControllerException e) {
            logger.error("toSend get logisticsCompany error. logisticsCompany={}", orderStatus.getLogisticsCompany(), e);
        }
        RecipeLogService.saveRecipeLog(orderStatus.getRecipeId(), orderStatus.getSourceRecipeOrderStatus()
                , orderStatus.getTargetRecipeOrderStatus(), "配送中,配送人：" + orderStatus.getSender() +
                        ",快递公司：" + company + ",快递单号：" + orderStatus.getTrackingNumber());

        //将快递公司快递单号信息用更新配送方式接口更新至his
        if (null == recipe || StringUtils.isEmpty(recipe.getMpiid())) {
            return;
        }
        if (null != orderStatus.getLogisticsCompany() && StringUtils.isNotEmpty(orderStatus.getTrackingNumber())) {
            RecipeBusiThreadPool.submit(() -> {
                RecipeDetailDAO recipeDetailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
                RecipeToHisService service = AppContextHolder.getBean("recipeToHisService", RecipeToHisService.class);
                List<Recipedetail> details = recipeDetailDAO.findByRecipeId(recipe.getRecipeId());
                PatientBean patientBean = patientService.get(recipe.getMpiid());
                DrugTakeChangeReqTO request = HisRequestInit.initDrugTakeChangeReqTO(recipe, details, patientBean, null);
                service.drugTakeChange(request);
                return null;
            });
        }
    }
}
