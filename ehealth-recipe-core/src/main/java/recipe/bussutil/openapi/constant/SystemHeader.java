
package recipe.bussutil.openapi.constant;

/**
 * 系统HTTP头常量
 */
public class SystemHeader {
    //签名Header
    public static final String X_CA_SIGNATURE = "X-Ca-Signature";
    //所有参与签名的Header
    public static final String X_CA_SIGNATURE_HEADERS = "X-Ca-Signature-Headers";
    //请求时间戳
    public static final String X_CA_TIMESTAMP = "X-Ca-Timestamp";
    //请求放重放Nonce,15分钟内保持唯一,建议使用UUID
    public static final String X_CA_NONCE = "X-Ca-Nonce";
    //APP KEY
    public static final String X_CA_KEY = "X-Ca-Key";
    //服务id
    public static final String X_SERVICE_ID = "X-Service-Id";
    //具体method
    public static final String X_SERVICE_METHOD = "X-Service-Method";
    //MD5加密的请求体信息
    public static final String X_CONTENT_MD5 = "X-Content-MD5";

    //MD5加密的请求体信息
    public static final String X_CA_REQUESTID = "X-Ca-RequestId";
    //API网关会将服务端的StringToSign放到HTTP应答的Header中返回到客户端，Key为：X-Ca-Error-Message
    public static final String X_CA_ERROR_MESSAGE = "X-Ca-Error-Message";
}
