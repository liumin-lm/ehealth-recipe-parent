package recipe.service;

import com.alibaba.fastjson.JSON;
import com.ngari.recipe.dto.ApothecaryDTO;
import com.ngari.recipe.dto.SkipThirdBean;
import com.ngari.recipe.entity.ConfigStatusCheck;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeOrder;
import com.ngari.recipe.recipe.model.SkipThirdReqVO;
import com.ngari.recipe.vo.ResultBean;
import com.ngari.recipe.vo.UpdateOrderStatusVO;
import ctd.persistence.exception.DAOException;
import ctd.util.JSONUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.ApplicationUtils;
import recipe.bean.DrugEnterpriseResult;
import recipe.caNew.pdf.CreatePdfFactory;
import recipe.client.DoctorClient;
import recipe.client.IConfigurationClient;
import recipe.constant.ErrorCode;
import recipe.core.api.IRecipeOrderService;
import recipe.dao.ConfigStatusCheckDAO;
import recipe.dao.RecipeDAO;
import recipe.dao.RecipeOrderDAO;
import recipe.factory.status.givemodefactory.GiveModeProxy;
import recipe.givemode.business.GiveModeTextEnum;
import recipe.manager.OrderManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 处方订单处理实现类 （新增）
 *
 * @author fuzi
 */
@Service
public class RecipeOrderTwoService implements IRecipeOrderService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    @Autowired
    private RecipeDAO recipeDAO;
    @Autowired
    private RecipeOrderDAO recipeOrderDAO;
    @Autowired
    private ConfigStatusCheckDAO configStatusCheckDAO;
    @Autowired
    private GiveModeProxy giveModeProxy;
    @Autowired
    private DoctorClient doctorClient;
    @Autowired
    private CreatePdfFactory createPdfFactory;
    @Autowired
    private IConfigurationClient configurationClient;
    @Autowired
    private RecipeServiceSub recipeServiceSub;
    @Autowired
    private OrderManager orderManager;

    @Override
    public ResultBean updateRecipeGiveUser(Integer recipeId, Integer giveUser) {
        ResultBean result = ResultBean.serviceError("参数错误");
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        if (null == recipe) {
            return result;
        }
        if (StringUtils.isEmpty(recipe.getOrderCode())) {
            return result;
        }
        try {
            //更新订单表字段 兼容老版本
            ApothecaryDTO apothecaryDTO = doctorClient.getGiveUser(recipe);
            recipeOrderDAO.updateApothecaryByOrderId(recipe.getOrderCode(), apothecaryDTO.getGiveUserName(), apothecaryDTO.getGiveUserIdCardCleartext());
            logger.info("RecipeOrderTwoService updateRecipeGiveUser OrderCode{}, apothecaryVO:{} ", recipe.getOrderCode(), JSONUtils.toString(apothecaryDTO));
        } catch (Exception e) {
            logger.error("RecipeOrderTwoService updateRecipeGiveUser ", e);
        }

        //更新pdf
        recipe.setGiveUser(giveUser.toString());
        createPdfFactory.updateGiveUser(recipe);
        return ResultBean.succeed();
    }


    @Override
    public ResultBean updateRecipeOrderStatus(UpdateOrderStatusVO orderStatus) {
        logger.info("RecipeOrderTwoService updateRecipeOrderStatus orderStatus = {}", JSON.toJSONString(orderStatus));
        ResultBean result = ResultBean.serviceError("参数错误");
        Recipe recipe = recipeDAO.getByRecipeId(orderStatus.getRecipeId());
        if (null == recipe || StringUtils.isEmpty(recipe.getOrderCode())) {
            return result;
        }
        RecipeOrder recipeOrder = recipeOrderDAO.getByOrderCode(recipe.getOrderCode());
        //校验订单状态可否流转
        List<ConfigStatusCheck> statusList = configStatusCheckDAO.findByLocationAndSource(recipe.getGiveMode(), recipeOrder.getStatus());
        boolean status = statusList.stream().anyMatch(a -> a.getTarget().equals(orderStatus.getTargetRecipeOrderStatus()));
        result = ResultBean.succeed();
        if (!status) {
            updateOrderStatus(orderStatus);
            return result;
        }
        //工厂代理处理 按照购药方式 修改订单信息
        orderStatus.setSourceRecipeOrderStatus(recipeOrder.getStatus());
        orderStatus.setOrderId(recipeOrder.getOrderId());
        orderStatus.setSourceRecipeStatus(recipe.getStatus());
        giveModeProxy.updateOrderByGiveMode(recipe.getGiveMode(), orderStatus);
        logger.info("RecipeOrderTwoService updateRecipeOrderStatus result = {}", JSON.toJSONString(result));
        return result;
    }

    /**
     * todo 方法需要优化 原方法需要删除
     *
     * @param skipThirdReqVO
     */
    @Override
    public void uploadRecipeInfoToThird(SkipThirdReqVO skipThirdReqVO) {
        logger.info("RecipeOrderService uploadRecipeInfoToThird skipThirdReqVO:{}.", JSONUtils.toString(skipThirdReqVO));
        Boolean pushToHisAfterChoose = configurationClient.getValueBooleanCatch(skipThirdReqVO.getOrganId(), "pushToHisAfterChoose", false);
        if (!pushToHisAfterChoose) {
            return;
        }
        List<Recipe> recipes = recipeDAO.findByRecipeIds(skipThirdReqVO.getRecipeIds());
        //将处方上传到第三方
        recipes.forEach(recipe -> {
            recipe.setGiveMode(GiveModeTextEnum.getGiveMode(skipThirdReqVO.getGiveMode()));
            DrugEnterpriseResult result = recipeServiceSub.pushRecipeForThird(recipe, 1);
            logger.info("RecipeOrderService uploadRecipeInfoToThird result:{}.", JSONUtils.toString(result));
            if (new Integer(0).equals(result.getCode())) {
                //表示上传失败
                throw new DAOException(ErrorCode.SERVICE_ERROR, result.getMsg());
            }
        });
    }


    @Override
    public SkipThirdBean getThirdUrl(Integer recipeId) {
        return orderManager.getThirdUrl(recipeId);
    }

    /**
     * todo 需要修改成 新模式
     * 不在新增逻辑内的状态流转 走老方法
     *
     * @param orderStatus
     */
    private void updateOrderStatus(UpdateOrderStatusVO orderStatus) {
        RecipeOrderService recipeOrderService = ApplicationUtils.getRecipeService(RecipeOrderService.class);
        Map<String, Object> attrMap = new HashMap<>();
        attrMap.put("status", orderStatus.getTargetRecipeOrderStatus());
        recipeOrderService.updateOrderStatus(orderStatus.getRecipeId(), attrMap);
    }

}
