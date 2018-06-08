package recipe.serviceprovider.his.service;

import com.ngari.base.BaseAPI;
import com.ngari.bus.hosrelation.model.HosrelationBean;
import com.ngari.bus.hosrelation.service.IHosrelationService;
import com.ngari.recipe.common.RecipeCommonReqTO;
import com.ngari.recipe.his.service.IRecipeHisService;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import recipe.constant.BusTypeEnum;

/**
 * 处方相关对接HIS服务
 * company: ngarihealth
 * author: 0184/yu_yun
 * date:2017/9/12.
 */
@RpcBean("remoteRecipeHisService")
public class RemoteRecipeHisService implements IRecipeHisService {

    @RpcService
    @Override
    public void canVisit(RecipeCommonReqTO request) {

    }

    @RpcService
    @Override
    public void visitRegist(RecipeCommonReqTO request) {

    }

    @RpcService
    @Override
    public void cancelVisit(RecipeCommonReqTO request) {

    }

    @RpcService
    @Override
    public boolean queryVisitStatus(Integer consultId) {
        IHosrelationService hosrelationService = BaseAPI.getService(IHosrelationService.class);
        HosrelationBean hosrelationBean = hosrelationService.getByBusIdAndBusType(consultId, BusTypeEnum.CONSULT.getId());

        return true;
    }
}
