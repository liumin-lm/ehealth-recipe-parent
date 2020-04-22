package recipe.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import ctd.mvc.support.HttpClientUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @ClassName AppSiganatureUtils
 * @Description 上海益药宝电商平台签名认证工具类，由接口提供方提供
 * @Author maoLy
 * @Date 2020/4/15
 **/
public class AppSiganatureUtils {
    private static String SAPARATOR="@$@";
    public static String createSiganature(String data, String appid,String appSecret,long timestamp) {
        String plain=appid+SAPARATOR+appSecret+SAPARATOR+data+SAPARATOR+timestamp+SAPARATOR+appSecret+SAPARATOR+appid;
        String siganature=encrypt(plain,"SHA-512");
        return siganature;
    }


    private static String encrypt(String strSrc, String encName) {
        MessageDigest md = null;
        String strDes = null;

        byte[] bt=null;
        try {
            bt = strSrc.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e1) {
            e1.printStackTrace();
        }
        try {
            if (encName == null || encName.equals("")) {
                encName = "SHA-256";
            }
            md = MessageDigest.getInstance(encName);
            md.update(bt);
            strDes = bytes2Hex(md.digest()); // to HexString
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
        return strDes;
    }

    private static String bytes2Hex(byte[] bts) {
        String des = "";
        String tmp = null;
        for (int i = 0; i < bts.length; i++) {
            tmp = (Integer.toHexString(bts[i] & 0xFF));
            if (tmp.length() == 1) {
                des += "0";
            }
            des += tmp;
        }
        return des;
    }
    public static void main(String[] args) throws Exception{
        final String APP_ID = "137438310108214011";
        final String APP_SECRET = "496d1e71-ad22-4c31-a896-3fe55a9286bb";
        long timestamp = System.currentTimeMillis(); // 生成签名时间戳
        String url = "https://apitest.yiyaogo.com/logisticsService/fetchLogisticsProcess";
        Map<String, Object> params = new HashMap<>();
        params.put("prescripNo","ngari87913");
        params.put("hospitalName","上海市中医医院（上海中医药大学附属市中医医院）");

        String json = JSONObject.toJSONString(params);
        HttpPost method = new HttpPost(url);
        method.addHeader("ACCESS_APPID", APP_ID);
        method.addHeader("ACCESS_TIMESTAMP", String.valueOf(timestamp));
        method.addHeader("ACCESS_SIGANATURE", createSiganature(json, APP_ID, APP_SECRET,
                timestamp));
        method.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
        HttpClient httpClient = HttpClientUtils.getHttpClient();
        HttpResponse httpResponse = httpClient.execute(method);
        HttpEntity entity = httpResponse.getEntity();
        String response = EntityUtils.toString(entity);
        JSONObject jsonObject = JSON.parseObject(response);
        Boolean success = jsonObject.getBoolean("success");
        JSONArray jsonArray = jsonObject.getJSONArray("result");
        //List<ShsyTrace> list = jsonArray.toJavaList(ShsyTrace.class);
        System.out.println(response);
        System.out.println(success);
        System.out.println(jsonArray);
        //System.out.println(list);

    }
}
