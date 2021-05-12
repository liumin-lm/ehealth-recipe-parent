package recipe.service.afterpay;

import com.ngari.recipe.common.RecipeResultBean;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeOrder;
import ctd.util.JSONUtils;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.constant.PayConstant;

import java.util.List;

/**
 * 支付后的业务处理
 * @author yinsheng
 * @date 2021\4\12 0012 17:26
 */
@Service
public class AfterPayBusService{

    private static final Logger LOGGER = LoggerFactory.getLogger(AfterPayBusService.class);

    @Autowired
    private HealthCardService healthCardService;
    @Autowired
    private KeepAccountService keepAccountService;
    @Autowired
    private PaySendMsgService paySendMsgService;

    public void handle(RecipeResultBean result, RecipeOrder recipeOrder, List<Recipe> recipes, Integer payFlag) {
        LOGGER.info("AfterPayBusService handle recipeOrder:{}.", JSONUtils.toString(recipeOrder));
        if (CollectionUtils.isEmpty(recipes)) {
            LOGGER.info("AfterPayBusService handle 处方列表为空,recipes:{}.", JSONUtils.toString(recipes));
            return;
        }
        if (recipeOrder != null && PayConstant.PAY_FLAG_PAY_SUCCESS == payFlag && RecipeResultBean.SUCCESS.equals(result.getCode())) {
            //上传记账信息
            keepAccountService.uploadKeepAccount(recipeOrder, recipes);
            //发送支付后消息
            paySendMsgService.sendPayMsg(recipeOrder, recipes);
        }
        //上传健康卡
        healthCardService.uploadHealthCard(recipes);
    }
}
