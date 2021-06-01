
package recipe.bussutil.openapi;

import ngari.openapi.Request;
import ngari.openapi.Response;
import ngari.openapi.constant.SystemHeader;
import ngari.openapi.util.*;
import org.apache.commons.lang3.StringUtils;
import java.util.UUID;

/**
 * Client
 */
public class Client {

    private String apiUrl;
    private String appKey;
    private String appSecret;
    private String encodingAesKey;

    public Client(String apiUrl,String appKey, String appSecret, String encodingAesKey) {
        this.apiUrl=apiUrl;
        this.appKey = appKey;
        this.appSecret = appSecret;
        this.encodingAesKey = encodingAesKey;
    }

    /**
     * 发送请求
     *
     * @param request request对象
     * @return Response
     * @throws Exception
     */
    public Response execute(Request request) throws Exception {
        request.setApiUrl(this.apiUrl);
        request.setAppKey(this.appKey);
        request.setAppSecret(this.appSecret);
        request.setEncodingAesKey(this.encodingAesKey);
        request.addHeader(SystemHeader.X_CA_KEY,request.getAppKey());
        request.addHeader(SystemHeader.X_CA_NONCE, UUID.randomUUID().toString());
        request.addHeader(SystemHeader.X_CA_TIMESTAMP, String.valueOf(System.currentTimeMillis()));
        String jsonStr= JSONUtils.toString(request.getBodys());
        String encryptStr;
        if(StringUtils.isNotEmpty(request.getEncodingAesKey())){
            encryptStr = AESUtils.encrypt(jsonStr, request.getEncodingAesKey());
        }else {
            encryptStr =jsonStr;
        }
        String contentMd5 = MessageDigestUtil.base64AndMD5(encryptStr);
        request.addHeader(SystemHeader.X_CONTENT_MD5, contentMd5);
        String signature = SignUtil.sign(request.getAppSecret(), request.getHeaders());
        request.addHeader(SystemHeader.X_CA_SIGNATURE,signature);
        request.setStringBody(encryptStr);
    	return OpenApiUtils.post(request);
    }

    public static void main(String[] args) throws Exception {
        String body="[1]";
        String encryptStr = AESUtils.encrypt(body, "1234567890123456");
        String contentMd5 = MessageDigestUtil.base64AndMD5(encryptStr.getBytes());
        System.out.println(contentMd5);

    }
}
