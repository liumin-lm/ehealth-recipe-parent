package recipe.business;

import com.alibaba.fastjson.JSON;
import com.ngari.recipe.dto.RecipeOrderRefundReqDTO;
import com.ngari.recipe.entity.DrugsEnterprise;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeExtend;
import com.ngari.recipe.entity.RecipeOrder;
import ctd.persistence.bean.QueryResult;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.core.api.greenroom.IRecipeOrderRefundService;
import recipe.dao.DrugsEnterpriseDAO;
import recipe.dao.RecipeDAO;
import recipe.dao.RecipeExtendDAO;
import recipe.dao.RecipeOrderDAO;
import recipe.enumerate.status.PayModeEnum;
import recipe.manager.OrderManager;
import recipe.util.DateConversion;
import recipe.util.ObjectCopyUtils;
import recipe.vo.greenroom.RecipeOrderRefundPageVO;
import recipe.vo.greenroom.RecipeOrderRefundReqVO;
import recipe.vo.greenroom.RecipeOrderRefundVO;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 退费查询接口调用
 *
 * @author ys
 */
@Service
public class RecipeOrderRefundService implements IRecipeOrderRefundService {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private RecipeOrderDAO recipeOrderDAO;
    @Autowired
    private RecipeDAO recipeDAO;
    @Autowired
    private RecipeExtendDAO recipeExtendDAO;
    @Autowired
    private DrugsEnterpriseDAO drugsEnterpriseDAO;
    @Autowired
    private OrderManager orderManager;

    @Override
    public RecipeOrderRefundPageVO findRefundRecipeOrder(RecipeOrderRefundReqVO recipeOrderRefundReqVO) {
        RecipeOrderRefundPageVO recipeOrderRefundPageVO = new RecipeOrderRefundPageVO();
        Date beginDate = DateConversion.parseDate(recipeOrderRefundReqVO.getBeginTime(), DateConversion.DEFAULT_DATE_TIME);
        Date endDate = DateConversion.parseDate(recipeOrderRefundReqVO.getEndTime(), DateConversion.DEFAULT_DATE_TIME);
        RecipeOrderRefundReqDTO recipeOrderRefundReqDTO = ObjectCopyUtils.convert(recipeOrderRefundReqVO, RecipeOrderRefundReqDTO.class);
        recipeOrderRefundReqDTO.setBeginTime(beginDate);
        recipeOrderRefundReqDTO.setEndTime(endDate);
        QueryResult<RecipeOrder> recipeOrderQueryResult = orderManager.findRefundRecipeOrder(recipeOrderRefundReqDTO);
        logger.info("RecipeOrderRefundService findRefundRecipeOrder recipeOrderQueryResult:{}", JSON.toJSONString(recipeOrderQueryResult));
        if (CollectionUtils.isEmpty(recipeOrderQueryResult.getItems())) {
            return recipeOrderRefundPageVO;
        }
        List<RecipeOrder> recipeOrderList = recipeOrderQueryResult.getItems();
        long total = recipeOrderQueryResult.getTotal();
        if (null != new Long(total)) {
            recipeOrderRefundPageVO.setTotal(new Long(total).intValue());
        }
        List<String> orderCodeList = recipeOrderList.stream().map(RecipeOrder::getOrderCode).collect(Collectors.toList());
        List<Integer> depIdList = recipeOrderList.stream().map(RecipeOrder::getEnterpriseId).collect(Collectors.toList());
        List<Recipe> recipeList = recipeDAO.findByOrderCode(orderCodeList);
        Map<String, Recipe> recipeOrderCodeMap = recipeList.stream().collect(Collectors.toMap(Recipe::getOrderCode,a->a,(k1,k2)->k1));
        List<Integer> recipeIdList = recipeList.stream().map(Recipe::getRecipeId).collect(Collectors.toList());
        List<RecipeExtend> recipeExtendList = recipeExtendDAO.queryRecipeExtendByRecipeIds(recipeIdList);
        Map<Integer, RecipeExtend> recipeExtendMap = recipeExtendList.stream().collect(Collectors.toMap(RecipeExtend::getRecipeId, a->a,(k1, k2)->k1));
        List<DrugsEnterprise> drugsEnterpriseList = drugsEnterpriseDAO.findByIdIn(depIdList);
        Map<Integer, DrugsEnterprise> drugsEnterpriseMap = drugsEnterpriseList.stream().collect(Collectors.toMap(DrugsEnterprise::getId,a->a,(k1,k2)->k1));
        List<RecipeOrderRefundVO> recipeOrderRefundVOList = new ArrayList<>();

        recipeOrderList.forEach(recipeOrder -> {
            RecipeOrderRefundVO recipeOrderRefundVO = new RecipeOrderRefundVO();
            recipeOrderRefundVO.setOrderCode(recipeOrder.getOrderCode());
            recipeOrderRefundVO.setActualPrice(recipeOrder.getActualPrice());
            recipeOrderRefundVO.setCreateTime(DateConversion.getDateFormatter(recipeOrder.getCreateTime(), DateConversion.DEFAULT_DATE_TIME));
            if (null != recipeOrder.getEnterpriseId()) {
                if (StringUtils.isNotEmpty(recipeOrder.getDrugStoreName())) {
                    recipeOrderRefundVO.setDepName(recipeOrder.getDrugStoreName());
                } else {
                    recipeOrderRefundVO.setDepName(drugsEnterpriseMap.get(recipeOrder.getEnterpriseId()).getName());
                }
            }
            recipeOrderRefundVO.setPatientName(recipeOrderCodeMap.get(recipeOrder.getOrderCode()).getPatientName());
            recipeOrderRefundVO.setPayModeText(PayModeEnum.getPayModeEnumName(recipeOrder.getPayMode()));
            recipeOrderRefundVO.setGiveModeText(recipeOrder.getGiveModeText());
            recipeOrderRefundVOList.add(recipeOrderRefundVO);
        });
        recipeOrderRefundPageVO.setRecipeOrderRefundVOList(recipeOrderRefundVOList);
        recipeOrderRefundPageVO.setStart(recipeOrderRefundReqVO.getStart());
        recipeOrderRefundPageVO.setLimit(recipeOrderRefundReqVO.getLimit());
        return recipeOrderRefundPageVO;
    }
}
