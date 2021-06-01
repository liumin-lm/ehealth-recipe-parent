
package recipe.bussutil.openapi.util;


import ngari.openapi.constant.Constants;
import ngari.openapi.constant.SystemHeader;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.*;

/**
 * 签名工具
 */
public class SignUtil {
    /**
     * 需要参与签名的请求头
     */
    private static final List<String> SIGN_HEADER_LIST = Arrays.asList("X-Service-Id","X-Service-Method","X-Ca-Key",
            "X-Ca-Nonce","X-Ca-Timestamp","X-Content-MD5");
    /**
     * 计算签名
     *
     * @param secret APP密钥
     * @param headers 请求头参数
     * @return 签名后的字符串
     */
    public static String sign(String secret,
    							Map<String, String> headers) {
        try {
            Mac hmacSha256 = Mac.getInstance(Constants.HMAC_SHA256);
            byte[] keyBytes = secret.getBytes(Constants.ENCODING);
            hmacSha256.init(new SecretKeySpec(keyBytes, 0, keyBytes.length, Constants.HMAC_SHA256));
//            String contentMd5 = MessageDigestUtil.base64AndMD5(bodyContent);

            return new String(Base64.encodeBase64(
                    hmacSha256.doFinal((buildHeaders(headers, SIGN_HEADER_LIST))
                            .getBytes(Constants.ENCODING))),
                    Constants.ENCODING);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 构建待签名Http头
     *
     * @param headers 请求中所有的Http头
     * @param signHeaderList 自定义参与签名Header前缀
     * @return 待签名Http头
     */
    public static String buildHeaders(Map<String, String> headers, List<String> signHeaderList) {

    	List<String> sbList=new ArrayList<>();
    	if (null != signHeaderList) {
    		Collections.sort(signHeaderList);
    		if (null != headers) {
    			Map<String, String> sortMap = new TreeMap<String, String>();
    			sortMap.putAll(headers);
    			StringBuilder signHeadersStringBuilder = new StringBuilder();
    			for (Map.Entry<String, String> header : sortMap.entrySet()) {
                    if (isHeaderToSign(header.getKey(), signHeaderList)) {
                        StringBuilder sb = new StringBuilder();
                    	sb.append(header.getKey().toLowerCase());
                    	sb.append(Constants.SPE2);
                        if (!StringUtils.isBlank(header.getValue())) {
                        	sb.append(header.getValue());
                        }
                        sbList.add(sb.toString());
                        if (0 < signHeadersStringBuilder.length()) {
                        	signHeadersStringBuilder.append(Constants.SPE1);
                        }
                        signHeadersStringBuilder.append(header.getKey().toLowerCase());
                    }
                }
    			headers.put(SystemHeader.X_CA_SIGNATURE_HEADERS, signHeadersStringBuilder.toString());
    		}
    	}
        return StringUtils.join(sbList,Constants.SPE3);
    }

    /**
     * Http头是否参与签名 return
     */
    private static boolean isHeaderToSign(String headerName, List<String> signHeaderList) {
        if (StringUtils.isBlank(headerName)) {
            return false;
        }

        if (headerName.startsWith(Constants.CA_HEADER_TO_SIGN_PREFIX_SYSTEM)) {
            return true;
        }

        if (null != signHeaderList) {
            for (String signHeaderPrefix : signHeaderList) {
                if (headerName.equalsIgnoreCase(signHeaderPrefix)) {
                    return true;
                }
            }
        }

        return false;
    }

    public static void main(String[] args) {
        String secret = "1234567890123456";
        Map<String, String> headers = new HashMap<>();
        headers.put("X-Service-Id", "eh.organ");
        headers.put("X-Service-Method", "");
        headers.put("X-Ca-Key", "hello");
        headers.put("X-Ca-Timestamp", String.valueOf(System.currentTimeMillis()));
        headers.put("X-Ca-Nonce", UUID.randomUUID().toString());
        headers.put(SystemHeader.X_CONTENT_MD5, UUID.randomUUID().toString());
        System.out.println(sign(secret, headers));

    }
}
