package recipe.serviceprovider.his.service;

import com.ngari.base.BaseAPI;
import com.ngari.base.hisservice.service.IRecipeHisService;
import com.ngari.bus.hosrelation.model.HosrelationBean;
import com.ngari.bus.hosrelation.service.IHosrelationService;
import com.ngari.recipe.common.RecipeCommonReqTO;
import com.ngari.recipe.common.RecipeCommonResTO;
import com.ngari.recipe.his.service.IRecipeToHisService;
import ctd.spring.AppDomainContext;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import recipe.constant.BusTypeEnum;

/**
 * 处方相关对接HIS服务
 * company: ngarihealth
 * author: 0184/yu_yun
 * date:2017/9/12.
 */
@RpcBean("remoteRecipeToHisService")
public class RemoteRecipeToHisService implements IRecipeToHisService {

    @RpcService
    @Override
    public RecipeCommonResTO canVisit(RecipeCommonReqTO request) {
        IRecipeHisService hisService = AppDomainContext.getBean("his.iRecipeHisService", IRecipeHisService.class);


    }

    @RpcService
    @Override
    public RecipeCommonResTO visitRegist(RecipeCommonReqTO request) {

    }

    @RpcService
    @Override
    public boolean queryVisitStatus(Integer consultId) {
        IHosrelationService hosrelationService = BaseAPI.getService(IHosrelationService.class);
        HosrelationBean hosrelationBean = hosrelationService.getByBusIdAndBusType(consultId, BusTypeEnum.CONSULT.getId());

        return true;
    }
}
