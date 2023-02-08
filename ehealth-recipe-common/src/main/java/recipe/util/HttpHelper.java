package recipe.util;

import com.alibaba.fastjson.JSON;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * @author yuyun
 */
public class HttpHelper {
    private static final Logger logger = LoggerFactory.getLogger(HttpHelper.class);

    public static String doPost(String uri, String jsonStr, String token) throws IOException {
        logger.info("HttpHelper doPost uri:{},jsonStr:{}", uri, jsonStr);
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(uri);
        if (StringUtils.isNotEmpty(token)) {
            httpPost.setHeader("Authorization", token);
        }
        HttpEntity entity = new StringEntity(jsonStr, ContentType.APPLICATION_JSON);
        httpPost.setEntity(entity);
        CloseableHttpResponse response2 = httpclient.execute(httpPost);
        try {
            HttpEntity entity2 = response2.getEntity();
            String result = EntityUtils.toString(entity2);
            EntityUtils.consume(entity2);
            logger.info("HttpHelper doPost result:{}", result);
            return result;
        } finally {
            response2.close();
        }
    }

    public static int getStatusCode(String uri, String jsonStr, String token) throws Exception{
        logger.info("HttpHelper getStatusCode uri:{},jsonStr:{}", uri, jsonStr);
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(uri);
        if (StringUtils.isNotEmpty(token)) {
            httpPost.setHeader("Authorization", token);
        }
        HttpEntity entity = new StringEntity(jsonStr, ContentType.APPLICATION_JSON);
        httpPost.setEntity(entity);
        CloseableHttpResponse response = httpclient.execute(httpPost);
        logger.info("HttpHelper getStatusCode response:{}", JSON.toJSONString(response));
        return response.getStatusLine().getStatusCode();
    }

    public static String doGet(String uri, String token) throws Exception{
        logger.info("HttpHelper doGet uri:{},token:{}", uri, token);
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet(uri);
        httpGet.setHeader("Content-Type", "application/json");
        if (StringUtils.isNotEmpty(token)) {
            httpGet.setHeader("Authorization", token);
        }
        CloseableHttpResponse response = httpClient.execute(httpGet);
        HttpEntity httpEntity = response.getEntity();
        String responseStr = EntityUtils.toString(httpEntity);
        logger.info("HttpHelper doGet responseStr:{}", responseStr);
        return responseStr;
    }
}
