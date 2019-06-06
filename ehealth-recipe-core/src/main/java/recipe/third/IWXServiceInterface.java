package recipe.third;

import ctd.util.annotation.RpcService;

import java.util.Map;

/**
 * @author yuyun
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

    /**
     * 获取微信地址
     * @return
     */
    @RpcService
    public String urlJoin();
}
