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
        LOGGER.info("RecipeOutpatientPaymentConsumer onMessage topic={}, message={}", OnsConfig.paymentReportTopic, message);
        // {"recipeCodes":["22222333"],"organId":1,"hisSettlementNo":"1122221","tradeNo":"out_A001_166123514827527","outTradeNo":"out_A001_166123514827527","preSettleTotalAmount":3333,"isMedicalSettle":0,"settleMode":1,"payTime":"2022-08-23 17:56:16"}
        RecipeOutpatientPaymentDTO recipeOutpatientPaymentDTO = JSONArray.parseObject(message, RecipeOutpatientPaymentDTO.class);
        IRecipeBusinessService recipeBusinessService = AppContextHolder.getBean("recipeBusinessService", IRecipeBusinessService.class);
        recipeBusinessService.recipeOutpatientPaymentCallback(recipeOutpatientPaymentDTO);
    }
}
