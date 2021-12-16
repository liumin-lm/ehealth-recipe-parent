package recipe.business;

import com.alibaba.fastjson.JSON;
import com.ngari.common.dto.CheckRequestCommonOrderItemDTO;
import com.ngari.common.dto.CheckRequestCommonOrderPageDTO;
import com.ngari.common.dto.SyncOrderVO;
import com.ngari.patient.service.BasicAPI;
import com.ngari.patient.service.PatientService;
import com.ngari.recipe.dto.ApothecaryDTO;
import com.ngari.recipe.dto.RecipeFeeDTO;
import com.ngari.recipe.dto.RecipeOrderDto;
import com.ngari.recipe.dto.SkipThirdDTO;
import com.ngari.recipe.entity.ConfigStatusCheck;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeOrder;
import com.ngari.recipe.recipe.model.SkipThirdReqVO;
import com.ngari.recipe.vo.UpdateOrderStatusVO;
import ctd.persistence.bean.QueryResult;
import ctd.util.JSONUtils;
import eh.entity.bus.pay.BusTypeEnum;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.ApplicationUtils;
import recipe.caNew.pdf.CreatePdfFactory;
import recipe.client.DoctorClient;
import recipe.core.api.patient.IRecipeOrderBusinessService;
import recipe.dao.ConfigStatusCheckDAO;
import recipe.dao.RecipeDAO;
import recipe.dao.RecipeOrderDAO;
import recipe.enumerate.type.GiveModeTextEnum;
import recipe.factory.status.givemodefactory.GiveModeProxy;
import recipe.manager.EnterpriseManager;
import recipe.manager.OrderManager;
import recipe.service.RecipeOrderService;
import recipe.vo.ResultBean;
import recipe.vo.second.RecipeOrderVO;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 处方订单处理实现类 （新增）
 *
 * @author fuzi
 */
@Service
public class RecipeOrderBusinessService implements IRecipeOrderBusinessService {
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
    private OrderManager orderManager;
    @Autowired
    private EnterpriseManager enterpriseManager;

    @Override
    public ResultBean updateRecipeGiveUser(Integer recipeId, Integer giveUser) {
        ResultBean result = ResultBean.serviceError("参数错误");
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        if (null == recipe) {
            return result;
        }
        recipe.setGiveUser(giveUser.toString());
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
        Recipe recipeUpdate = new Recipe();
        recipeUpdate.setGiveUser(recipe.getGiveUser());
        recipeUpdate.setRecipeId(recipe.getRecipeId());
        recipeDAO.updateNonNullFieldByPrimaryKey(recipeUpdate);
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
    public SkipThirdDTO uploadRecipeInfoToThird(SkipThirdReqVO skipThirdReqVO) {
        return enterpriseManager.uploadRecipeInfoToThird(skipThirdReqVO.getOrganId(), skipThirdReqVO.getGiveMode(), skipThirdReqVO.getRecipeIds());
    }


    /**
     * 获取第三方跳转链接
     * TODO 七月大版本将会去掉bqEnterprise标志,会对此处代码进行重构,由于涉及改动较大,本次小版本不做处理
     *
     * @param skipThirdReqVO
     * @return
     */
    @Override
    public SkipThirdDTO getSkipUrl(SkipThirdReqVO skipThirdReqVO) {
        return orderManager.getThirdUrl(skipThirdReqVO.getRecipeIds().get(0), GiveModeTextEnum.getGiveMode(skipThirdReqVO.getGiveMode()));
    }

    @Override
    public List<RecipeFeeDTO> findRecipeOrderDetailFee(String orderCode) {
        return orderManager.findRecipeOrderDetailFee(orderCode);
    }

    @Override
    public RecipeOrderDto getRecipeOrderByBusId(Integer orderId) {
        return orderManager.getRecipeOrderByBusId(orderId);
    }

    @Override
    public CheckRequestCommonOrderPageDTO getRecipePageForCommonOrder(SyncOrderVO request) {
        logger.info("getRecipePageForCommonOrder param ={}", JSON.toJSONString(request));
        CheckRequestCommonOrderPageDTO pageDTO = new CheckRequestCommonOrderPageDTO();
        if (request.getPage() == null || request.getSize() == null) {
            return pageDTO;
        }
        Integer start = (request.getPage() - 1) * request.getSize();
        Integer limit = request.getSize();
        QueryResult<RecipeOrder> queryResult = recipeOrderDAO.queryPageForCommonOrder(request.getStartDate(),
                request.getEndDate(), start, limit);
        if (queryResult == null) {
            return pageDTO;
        }
        if (CollectionUtils.isEmpty(queryResult.getItems())) {
            return pageDTO;
        }
        pageDTO.setTotal(Integer.parseInt(String.valueOf(queryResult.getTotal())));
        pageDTO.setPage(request.getPage());
        pageDTO.setSize(request.getSize());
        List<CheckRequestCommonOrderItemDTO> order = new ArrayList<>();
        PatientService patientService = BasicAPI.getService(PatientService.class);
        for (RecipeOrder recipeOrder : queryResult.getItems()) {
            CheckRequestCommonOrderItemDTO orderItem = new CheckRequestCommonOrderItemDTO();
            String userId = patientService.getLoginIdByMpiId(recipeOrder.getMpiId());
            orderItem.setUserId(userId);
            orderItem.setMpiId(recipeOrder.getMpiId());
            orderItem.setBusType(BusTypeEnum.RECIPE.getCode());
            orderItem.setBusId(recipeOrder.getOrderId());
            orderItem.setBusStatus(recipeOrder.getStatus());
            orderItem.setBusDate(recipeOrder.getCreateTime());
            orderItem.setCreateDate(recipeOrder.getCreateTime());
            orderItem.setLastModify(recipeOrder.getLastModifyTime());
            order.add(orderItem);
        }
        pageDTO.setOrder(order);
        logger.info("getRecipePageForCommonOrder result ={}", JSON.toJSONString(pageDTO));
        return pageDTO;
    }

    /**
     * 患者提交订单时更新pdf
     *
     * @param recipeId
     */
    @Override
    public void updatePdfForSubmitOrderAfter(Integer recipeId) {
        createPdfFactory.updateCodePdfExecute(recipeId);
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
