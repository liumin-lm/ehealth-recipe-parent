package recipe.retry;

import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.recipe.mode.PayNotifyReqTO;
import com.ngari.his.recipe.mode.PayNotifyResTO;
import com.ngari.his.recipe.service.IRecipeHisService;
import com.ngari.platform.recipe.CashSettleResultReqTo;
import ctd.persistence.exception.DAOException;
import ctd.util.JSONUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.client.IConfigurationClient;
import recipe.presettle.settle.IRecipeSettleService;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * created by shiyuping on 2020/12/9
 *
 * @author shiyuping
 */
@Service
public class RecipeRetryService {
    private static final Logger LOGGER = LoggerFactory.getLogger(RecipeRetryService.class);

    @Autowired
    private IConfigurationClient configurationClient;
    @Resource
    private IRecipeHisService hisService;


    /**
     * 结算异常处理----补偿机制--异常将重试三次
     *
     * @param settleService
     * @param req
     * @return
     */
    public PayNotifyResTO doRecipeSettle(IRecipeSettleService settleService, PayNotifyReqTO req) {
        LOGGER.info("RecipeSettleRetryService.doRecipeSettle start req:{}", JSONUtils.toString(req));
        Retryer<PayNotifyResTO> retryer = RetryerBuilder.<PayNotifyResTO>newBuilder()
                //抛出指定异常重试
                .retryIfExceptionOfType(Exception.class)
                //停止重试策略
                .withStopStrategy(StopStrategies.stopAfterAttempt(3))
                //每次等待重试时间间隔
                .withWaitStrategy(WaitStrategies.fixedWait(300, TimeUnit.MILLISECONDS))
                .build();
        PayNotifyResTO resTO;
        try {
            resTO = retryer.call(() -> {
                LOGGER.info("RecipeSettleRetryService.doRecipeSettle retry");
                return settleService.recipeSettle(req);
            });
        } catch (Exception e) {
            LOGGER.info("三次后还是异常");
            throw new DAOException(609, "重试三次后还是接口异常");
        }
        return resTO;
    }


    private PayNotifyResTO retrySettle(PayNotifyReqTO req) throws Exception {
        // 根据机构配置获取是否调用结算反查接口
        Boolean isRetrySettle = configurationClient.getValueBooleanCatch(Integer.valueOf(req.getOrganID()), "isRetrySettle", false);
        if (!isRetrySettle) {
            LOGGER.info("三次后还是异常");
            throw new DAOException(609, "重试三次后还是接口异常");
        }
        CashSettleResultReqTo cashSettleResultReqTo = CashSettleResultReqTo.builder().orderCode(req.getOrderCode()).organId(Integer.valueOf(req.getOrganID())).recipeCode(req.getRecipeCodeS()).build();
        Retryer<HisResponseTO> retryer = RetryerBuilder.<HisResponseTO>newBuilder()
                //抛出指定异常重试
                .retryIfExceptionOfType(Exception.class)
                //停止重试策略
                .withStopStrategy(StopStrategies.stopAfterAttempt(2))
                //每次等待重试时间间隔
                .withWaitStrategy(WaitStrategies.fixedWait(1, TimeUnit.SECONDS))
                .build();


        PayNotifyResTO resTO = new PayNotifyResTO();
        resTO.setMsgCode(0);
        try {

            resTO = retryer.call(() -> {
                LOGGER.info("RecipeSettleRetryService.retrySettle retry");
                HisResponseTO hisResponseTO = hisService.cashSettleResult(cashSettleResultReqTo);
                PayNotifyResTO resTO1 = new PayNotifyResTO();
                if("-1".equals(hisResponseTO.getMsgCode())){

                    resTO1.setMsgCode(-1);
                }

                return resTO1;
            });
        } catch (Exception e) {
            LOGGER.info("RecipeSettleRetryService.retrySettle retry Exception OrderCode={}",req.getOrderCode());
        }
        return resTO;
    }
}
