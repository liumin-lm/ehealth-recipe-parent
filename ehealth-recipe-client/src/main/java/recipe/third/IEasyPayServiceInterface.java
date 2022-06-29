package recipe.third;

import ctd.util.annotation.RpcService;
import easypay.entity.vo.param.CommonParam;

/**
 * @Description
 * @Author 刘敏
 * @Date 2021-06-27
 */
public interface IEasyPayServiceInterface {

    @RpcService
    String gateWay(CommonParam commonParam);


}
