package recipe.util;

import com.google.common.collect.Lists;
import ctd.util.JSONUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.constant.RecipeSystemConstant;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

/**
 * @author yuyun
 */
public class HttpHelper
{
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpHelper.class);

    public static String sendPostRequest(String url, Map<String, String> headers, String requestParamsJsonString) throws IOException {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(url);
        try {
            httpPost.addHeader(new BasicHeader("Content-Type","application/json;charset=utf-8"));
            if(headers!=null && headers.size()>0){
                Iterator<Map.Entry<String, String>> it = headers.entrySet().iterator();
                while(it.hasNext()){
                    Map.Entry<String, String> en = it.next();
                    httpPost.addHeader(new BasicHeader(en.getKey(), en.getValue()));
                }
            }
            HttpEntity entity = new StringEntity(requestParamsJsonString, ContentType.APPLICATION_JSON);
            httpPost.setEntity(entity);
            String result = null;
            CloseableHttpResponse response = httpClient.execute(httpPost);
            HttpEntity responseEntity = response.getEntity();
            if(responseEntity!=null){
                result = EntityUtils.toString(responseEntity, "UTF-8");
            }
            EntityUtils.consume(responseEntity);
            response.close();
            return result;
        } catch (IOException e) {
            LOGGER.error("sendPostRequest exception, error ", e);
            throw e;
        }
    }

    public static String getHttpResult(String urlvalue) {
        String inputLine = "";
        BufferedReader in = null;
        try {
            URL url = new URL(urlvalue);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            in = new BufferedReader(new InputStreamReader(urlConnection
                    .getInputStream(), RecipeSystemConstant.DEFAULT_CHARACTER_ENCODING));
            inputLine = in.readLine();
        } catch (Exception e) {
            LOGGER.error("getHttpResult"+e);
        }  finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException e) {
                LOGGER.error("error", e);
            }
        }

        return inputLine;
    }

    /**
     * 发送 application/json post请求
     */
    public static String doPost(String uri, String jsonStr) throws IOException {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(uri);

        HttpEntity entity = new StringEntity(jsonStr, ContentType.APPLICATION_JSON);
        httpPost.setEntity(entity);
        CloseableHttpResponse response2 = httpclient.execute(httpPost);
        try {
            HttpEntity entity2 = response2.getEntity();

            // do something useful with the response body
            // and ensure it is fully consumed
            //消耗掉response
            String result = EntityUtils.toString(entity2);
            EntityUtils.consume(entity2);
            return result;
        } finally {
            response2.close();
        }
    }

    public static String doPost(String uri, HashMap<String, Object> dataMap) throws ClientProtocolException, IOException {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(uri);
        //拼接参数
        List<NameValuePair> nvps = Lists.newArrayList();
        nvps.add(new BasicNameValuePair("username", "vip"));
        nvps.add(new BasicNameValuePair("password", "secret"));
        HttpEntity entity = new StringEntity(JSONUtils.toString(dataMap), ContentType.APPLICATION_JSON);
        httpPost.setEntity(entity);
        CloseableHttpResponse response2 = httpclient.execute(httpPost);
        try {
            LOGGER.info("doPost.response2.getStatusLine()="+response2.getStatusLine());
            HttpEntity entity2 = response2.getEntity();

            // do something useful with the response body
            // and ensure it is fully consumed
            //消耗掉response
            String result = EntityUtils.toString(entity2);
            EntityUtils.consume(entity2);
            return result;
        } finally {
            response2.close();
        }
    }

    public static String doGet(String uri) throws IOException {
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet(uri);
        // HttpEntity entity=new StringEntity(JSONUtils.toString(dataMap), ContentType.APPLICATION_JSON);
        CloseableHttpResponse response2 = httpclient.execute(httpGet);
        try {
            LOGGER.info("doGet.response2.getStatusLine()="+response2.getStatusLine());
            HttpEntity entity2 = response2.getEntity();

            // do something useful with the response body
            // and ensure it is fully consumed
            //消耗掉response
            String result = EntityUtils.toString(entity2);
            EntityUtils.consume(entity2);
            return result;
        } finally {
            response2.close();
        }
    }

    /**
     * 运营平台医生提现调用httpPost请求德科接口
     *
     * @param url
     * @param body
     * @return
     * @throws IOException
     */
    public static String httpsPost(String url, String body) throws IOException {
        HttpClient httpClient = HttpClients.createDefault();
        HttpPost post = new HttpPost(url);
        StringEntity postingString = new StringEntity(body, "UTF-8");
        post.setEntity(postingString);
        post.setHeader("Content-type", "application/json");
        HttpResponse response = httpClient.execute(post);
        String content = EntityUtils.toString(response.getEntity());
        return content;
    }

    /**
     * 运营平台调用德科支付结果查询接口查询打款结果明细
     *
     * @param url
     * @return
     * @throws IOException
     */
    public static Map<String, Object> httpsGet(String url) throws IOException {
        HttpClient httpClient = HttpClients.createDefault();
        HttpGet get = new HttpGet(url);
        HttpResponse response = httpClient.execute(get);
        HttpEntity entity = response.getEntity();
        String content = EntityUtils.toString(entity);
        Map<String, Object> map = JSONUtils.parse(content.toString(), Map.class);
        return map;
    }

}
