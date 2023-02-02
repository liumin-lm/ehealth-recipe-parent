package recipe.atop.open;

import com.ngari.pay.api.service.bus.IBusPayService;
import ctd.util.annotation.RpcBean;
import easypay.entity.vo.param.bus.HlwTbParamReq;
import easypay.entity.vo.param.bus.MedicalPreSettleQueryReq;
import easypay.entity.vo.param.bus.SelfPreSettleQueryReq;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.core.api.patient.IRecipeOrderBusinessService;

/**
 * @description： 支付调用业务接口
 * @author： whf
 * @date： 2022-08-10 11:37
 */
@RpcBean("recipePayAtop")
public class RecipePayAtop implements IBusPayService {
    @Autowired
    private IRecipeOrderBusinessService recipeOrderService;

    @Override
    public MedicalPreSettleQueryReq medicalPreSettleQueryInfo(Integer busId) {
        return recipeOrderService.medicalPreSettleQueryInfo(busId);
    }

    @Override
    public SelfPreSettleQueryReq selfPreSettleQueryInfo(Integer busId) {
        return recipeOrderService.selfPreSettleQueryInfo(busId);
    }

    @Override
    public HlwTbParamReq getHlwYbInfo(Integer busId) {
        return recipeOrderService.getHlwYbInfo(busId);
    }
}
