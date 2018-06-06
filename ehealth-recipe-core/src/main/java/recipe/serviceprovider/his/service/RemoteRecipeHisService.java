package recipe.serviceprovider.his.service;

import com.ngari.base.hisservice.model.HisResTO;
import com.ngari.his.recipe.mode.*;
import com.ngari.recipe.common.RecipeCommonReqTO;
import com.ngari.recipe.his.service.IRecipeHisService;
import ctd.spring.AppDomainContext;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import recipe.hisservice.RecipeToHisService;

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
    public void queryVisitStatus(RecipeCommonReqTO request) {

    }
}
