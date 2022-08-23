package recipe.mq;

import com.alibaba.fastjson.JSONArray;
import com.ngari.recipe.recipe.model.RecipeOutpatientPaymentDTO;
import ctd.net.broadcast.Observer;
import ctd.util.AppContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.common.OnsConfig;
import recipe.core.api.IRecipeBusinessService;


/**
 * @description： 门诊缴费支付回调
 * @author： whf
 * @date： 2022-08-19 16:48
 */
public class RecipeOutpatientPaymentConsumer implements Observer<String> {
    private static final Logger LOGGER = LoggerFactory.getLogger(RecipeOutpatientPaymentConsumer.class);
    @Override
    public void onMessage(String message) {
        LOGGER.info("RecipeStatusFromHisObserver onMessage topic={}, message={}", OnsConfig.settleStateSyncRecipeTag, message);
        RecipeOutpatientPaymentDTO recipeOutpatientPaymentDTO = JSONArray.parseObject(message, RecipeOutpatientPaymentDTO.class);
        IRecipeBusinessService recipeBusinessService = AppContextHolder.getBean("recipeBusinessService", IRecipeBusinessService.class);
        recipeBusinessService.recipeOutpatientPaymentCallback(recipeOutpatientPaymentDTO);
    }
}
