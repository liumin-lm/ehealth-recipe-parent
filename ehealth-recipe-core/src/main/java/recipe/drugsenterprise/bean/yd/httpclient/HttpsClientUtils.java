package recipe.drugsenterprise.bean.yd.httpclient;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * HTTP(S)请求客户端
 */
public class HttpsClientUtils extends HttpClientBase{

    private static BasicHeader CONTENT_TYPE_JSON_UTF8 = new BasicHeader("content-type","application/json;charset=UTF-8");
    private static BasicHeader CONTENT_TYPE_FORMURLENCODED_UTF8 = new BasicHeader("content-type","application/x-www-form-urlencoded;charset=UTF-8");
    private static Charset DEFAULT_CHARSET_UTF8 = Charset.forName("UTF-8");

    private static Logger logger = LoggerFactory.getLogger(HttpsClientUtils.class.getName());

    public static String doPost(String url,String jsonData) throws IOException{
        return doPost(url,jsonData,null);
    }

    public static String doPost(String url,String jsonData,Map<String,String> extendHeaders) throws IOException{
        RequestBuilder requestBuilder = requestBuilder(Method.POST,extendHeaders);
        HttpUriRequest reqMethod = requestBuilder.setUri(url)
                .setEntity(new StringEntity(jsonData,DEFAULT_CHARSET_UTF8))
                .build();
        return execute(reqMethod);
    }

    public static String doGet(String url,Map<String, String> paramsMap,Map<String,String> extendHeaders) throws IOException{
        List<BasicNameValuePair> nameValuePairs = new ArrayList<BasicNameValuePair>();
        if(paramsMap != null){
            for(Map.Entry<String,String> param :paramsMap.entrySet()){
                nameValuePairs.add(new BasicNameValuePair(param.getKey(),param.getValue()));
            }
        }
        RequestBuilder requestBuilder = requestBuilder(Method.GET,extendHeaders);
        HttpUriRequest reqMethod = requestBuilder.setUri(url)
                .addParameters(nameValuePairs.toArray(new BasicNameValuePair[nameValuePairs.size()]))
                .build();
        return execute(reqMethod);
    }

    private static RequestBuilder requestBuilder(Method method, Map<String,String> extendHeaders){
        RequestBuilder requestBuilder = null;
        switch (method){
            case GET:
                requestBuilder = RequestBuilder.get()
                        .addHeader(CONTENT_TYPE_FORMURLENCODED_UTF8)
                        .setConfig(requestConfig())
                        .setCharset(DEFAULT_CHARSET_UTF8);
                break;
            case POST:
                requestBuilder = RequestBuilder.post()
                        .addHeader(CONTENT_TYPE_JSON_UTF8)
                        .setConfig(requestConfig())
                        .setCharset(DEFAULT_CHARSET_UTF8);
                break;
        }
        if(extendHeaders != null){
            for(Map.Entry<String,String> entry : extendHeaders.entrySet()){
                if(entry.getKey() != null && !"content-type".equalsIgnoreCase(entry.getKey())){
                    requestBuilder.addHeader(new BasicHeader(entry.getKey(),entry.getValue()));
                }
            }
        }
        return requestBuilder;
    }

    private static String execute(HttpUriRequest reqMethod) throws IOException{
        String result = null;
        CloseableHttpResponse response = null;
        HttpEntity entity = null;
        try {
            CloseableHttpClient httpClient = HttpClientBase.getHttpClient();
            response = httpClient.execute(reqMethod);
            int statusCode = response.getStatusLine().getStatusCode();
            entity = response.getEntity();
            result = EntityUtils.toString(entity,DEFAULT_CHARSET_UTF8);
            if(statusCode == 200){
                return result;
            }
            logger.error("数据接口请求错误，返回值：{} 返回数据：{}",statusCode,result);
        } catch (IOException e) {
            logger.error("数据接口请求异常!",e);
            throw e;
        }finally {
            if(response != null) {
                try {
                    response.close();
                } catch (IOException e) {
                }
            }
            EntityUtils.consumeQuietly(entity);
        }
        return null;
    }

    public enum Method{
        POST,
        GET
    }

}