package recipe.client;

import com.github.rholder.retry.*;
import com.google.common.base.Predicates;
import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.recipe.mode.PayNotifyReqTO;
import com.ngari.his.recipe.service.IRecipeHisService;
import com.ngari.platform.recipe.CashSettleResultReqTo;
import ctd.persistence.exception.DAOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.util.JsonUtil;

import javax.annotation.Resource;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * @description： his结算 client
 * @author： whf
 * @date： 2022-06-28 13:54
 */
@Service
public class RecipeSettleClient extends BaseClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(RecipeSettleClient.class);
    @Autowired
    private IConfigurationClient configurationClient;
    @Resource
    private IRecipeHisService hisService;

    /**
     * 结算重试
     * @param req
     * @return
     */
    public HisResponseTO retrySettle(PayNotifyReqTO req) {
        // 根据机构配置获取是否调用结算反查接口
        Boolean isRetrySettle = configurationClient.getValueBooleanCatch(Integer.valueOf(req.getOrganID()), "isRetrySettle", false);
        if (!isRetrySettle) {
            LOGGER.info("三次后还是异常");
            throw new DAOException(609, "重试三次后还是接口异常");
        }
        CashSettleResultReqTo cashSettleResultReqTo = CashSettleResultReqTo.builder().orderCode(req.getOrderCode()).organId(Integer.valueOf(req.getOrganID())).recipeCode(req.getRecipeNoS()).build();
        Retryer<HisResponseTO> retryer = RetryerBuilder.<HisResponseTO>newBuilder()
                //抛出指定异常重试
                .retryIfExceptionOfType(Exception.class)
                .retryIfResult(e -> "99".equals(e.getMsgCode()))
                //停止重试策略
                .withStopStrategy(StopStrategies.stopAfterAttempt(2))
                //每次等待重试时间间隔
                .withWaitStrategy(WaitStrategies.fixedWait(60000, TimeUnit.MILLISECONDS))
                .build();

        HisResponseTO hisResponse = new HisResponseTO();
        try {
            hisResponse = retryer.call(() -> {
                    LOGGER.info("RecipeSettleClient.retrySettle retry cashSettleResultReqTo={}", JsonUtil.toString(cashSettleResultReqTo));
                    HisResponseTO hisResponseTO = hisService.cashSettleResult(cashSettleResultReqTo);
                    LOGGER.info("RecipeSettleClient.retrySettle hisResponseTO={}", JsonUtil.toString(hisResponseTO));
                    return hisResponseTO;
                });
        } catch (Exception e) {
            hisResponse.setMsgCode("200");
            return hisResponse;
        }

        return hisResponse;
    }
}
