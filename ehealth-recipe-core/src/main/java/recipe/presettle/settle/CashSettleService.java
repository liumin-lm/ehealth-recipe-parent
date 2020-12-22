package recipe.presettle.settle;

import com.ngari.his.recipe.mode.PayNotifyReqTO;
import com.ngari.his.recipe.mode.PayNotifyResTO;
import com.ngari.his.recipe.service.IRecipeHisService;
import com.ngari.recipe.common.RecipeResultBean;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.Recipedetail;
import ctd.util.JSONUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import recipe.service.HisCallBackService;
import recipe.service.RecipeLogService;

import javax.annotation.Resource;

/**
 * created by shiyuping on 2020/12/7
 *
 * @author shiyuping
 */
@Service
public class CashSettleService implements IRecipeSettleService {
    private static final Logger LOGGER = LoggerFactory.getLogger(CashSettleService.class);
    @Resource
    IRecipeHisService hisService;

    @Override
    public PayNotifyResTO recipeSettle(PayNotifyReqTO req) {
        //IRecipeHisService hisService = AppDomainContext.getBean("his.iRecipeHisService", IRecipeHisService.class);
        LOGGER.info("payNotify request={}", JSONUtils.toString(req));
        PayNotifyResTO response = hisService.payNotify(req);
        LOGGER.info("payNotify response={}", JSONUtils.toString(response));
        return response;

    }

    @Override
    public void doRecipeSettleResponse(PayNotifyResTO response, Recipe recipe, RecipeResultBean result) {
        if (response == null){
            //非医保结算前置机返回null可能未对接接口
            LOGGER.error("payNotify 前置机未返回null，可能未对接结算接口");
            return;
        }
        if (response.getMsgCode() == 0) {
            //结算成功
            if (response.getData() != null) {
                Recipedetail detail = new Recipedetail();
                detail.setPatientInvoiceNo(response.getData().getInvoiceNo());
                detail.setPharmNo(response.getData().getWindows());
                HisCallBackService.havePaySuccess(recipe.getRecipeId(), detail);
            } else {
                HisCallBackService.havePaySuccess(recipe.getRecipeId(), null);
            }

        } else if (response.getMsgCode() != 0) {
            //前置机返回结算失败
            if (result != null){
                result.setCode(RecipeResultBean.FAIL);
                if (response != null && response.getMsg() != null) {
                    result.setError(response.getMsg());
                } else {
                    result.setError("由于医院接口异常，支付失败，建议您稍后重新支付。");
                }
            }
            HisCallBackService.havePayFail(recipe.getRecipeId());
            RecipeLogService.saveRecipeLog(recipe.getRecipeId(), recipe.getStatus(), recipe.getStatus(), "支付完成结算失败，his返回原因：" + response.getMsg());
        }
    }
}
