package recipe.third;

import ctd.util.annotation.RpcService;

import java.util.Map;

/**
 * Created by zhangx on 2016/4/17.
 */
public interface IWXServiceInterface {

    /**
     * 组装单页地址
     *
     * @param appId
     * @param paramsMap
     * @return
     */
    @RpcService
    public String getSinglePageUrl(String appId, Map<String, String> paramsMap);

}
