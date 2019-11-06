package recipe.third;

import ctd.util.annotation.RpcService;

import java.util.Map;

/**
 * created by shiyuping on 2019/11/5
 */
public interface IWXPushMessService {
    /**
     * 普通客服消息
     * @param appId
     * @param openId
     * @param msgContent
     * @return
     */
    @RpcService
    String sendCustomerMsg(String appId, String openId, String msgContent);

    /**
     * 推送模板消息
     * @param appId
     * @param templateId 模板id
     * @param openId
     * @param url 跳转链接
     * @param data 模板数据
     * @return
     */
    @RpcService
    Map pushTemplateMessage(String appId, String templateId, String openId, String url, Map<String, Object> data);

}
