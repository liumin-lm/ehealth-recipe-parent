package recipe.bussutil.openapi.util;

import ngari.openapi.JSONResponseBean;
import ngari.openapi.Request;
import ngari.openapi.Response;
import ngari.openapi.util.JSONUtils;
import ngari.openapi.util.MessageDigestUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.Map;

public class OpenApiUtils {
    public static Response post(Request request) throws Exception {
        return convert(doPost(request));
    }
    /**
     * post form
     *
     * @param request
     * @return
     * @throws Exception
     */
    private static HttpResponse doPost(Request request)
            throws Exception {
        HttpClient httpClient = wrapClient(request.getApiUrl());

        HttpPost httpPost = new HttpPost(request.getApiUrl());
        for (Map.Entry<String, String> e : request.getHeaders().entrySet()) {
            httpPost.addHeader(e.getKey(), e.getValue());
        }
        if (request.getBodys() != null||StringUtils.isNotEmpty(request.getStringBody())) {
            if(StringUtils.isNotEmpty(request.getStringBody())){
                httpPost.setEntity(new StringEntity(request.getStringBody(), ContentType.APPLICATION_JSON));
            }else {
                httpPost.setEntity(new StringEntity(ngari.openapi.util.JSONUtils.toString(request.getBodys()), ContentType.APPLICATION_JSON));
            }
        }
        return httpClient.execute(httpPost);
    }

    private static HttpClient wrapClient(String host) {
        HttpClient httpClient = new DefaultHttpClient();
        if (host.startsWith("https://")) {
            sslClient(httpClient);
        }
        return httpClient;
    }
    private static void sslClient(HttpClient httpClient) {
        try {
            SSLContext ctx = SSLContext.getInstance("TLS");
            X509TrustManager tm = new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
                public void checkClientTrusted(X509Certificate[] xcs, String str) {

                }
                public void checkServerTrusted(X509Certificate[] xcs, String str) {

                }
            };
            ctx.init(null, new TrustManager[] { tm }, null);
            SSLSocketFactory ssf = new SSLSocketFactory(ctx);
            ssf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
            ClientConnectionManager ccm = httpClient.getConnectionManager();
            SchemeRegistry registry = ccm.getSchemeRegistry();
            registry.register(new Scheme("https", 443, ssf));
        } catch (KeyManagementException ex) {
            throw new RuntimeException(ex);
        } catch (NoSuchAlgorithmException ex) {
            throw new RuntimeException(ex);
        }
    }
    private static Response convert(HttpResponse response) throws IOException {
       Response res = new Response();

        if (null != response) {
            res.setStatusCode(response.getStatusLine().getStatusCode());
            for (Header header : response.getAllHeaders()) {
                res.setHeader(header.getName(), MessageDigestUtil.iso88591ToUtf8(header.getValue()));
            }
            res.setContentType(res.getHeader("Content-Type"));
            res.setRequestId(res.getHeader("X-Ca-Request-Id"));
            res.setBody(EntityUtils.toString(response.getEntity()));
            res.setJsonResponseBean(JSONUtils.parse(res.getBody(), JSONResponseBean.class));
            res.setCaErrorMsg(res.getHeader("X-Ca-Error-Message"));

        } else {
            //服务器无回应
            res.setStatusCode(500);
            res.setErrorMessage("No Response");
        }

        return res;
    }
}
