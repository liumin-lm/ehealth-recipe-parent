package recipe.logistics;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeOrder;
import com.sf.csim.express.service.CallExpressServiceTools;
import ctd.mvc.support.HttpClientUtils;
import ctd.persistence.DAOFactory;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import de.odysseus.staxon.json.JsonXMLConfig;
import de.odysseus.staxon.json.JsonXMLConfigBuilder;
import de.odysseus.staxon.json.JsonXMLInputFactory;
import de.odysseus.staxon.json.JsonXMLOutputFactory;
import de.odysseus.staxon.xml.util.PrettyXMLEventWriter;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.dao.RecipeDAO;
import recipe.dao.RecipeOrderDAO;
import recipe.dao.RecipeParameterDao;
import recipe.drugsenterprise.bean.EsbWebService;
import recipe.util.AppSiganatureUtils;
import recipe.util.DictionaryUtil;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.util.*;

/**
 * 快递鸟物流轨迹即时查询接口
 *
 * @技术QQ群: 456320272
 * @see: http://www.kdniao.com/YundanChaxunAPI.aspx
 * @copyright: 深圳市快金数据技术服务有限公司
 * <p>
 * DEMO中的电商ID与私钥仅限测试使用，正式环境请单独注册账号
 * 单日超过500单查询量，建议接入我方物流轨迹订阅推送接口
 * <p>
 * ID和Key请到官网申请：http://www.kdniao.com/ServiceApply.aspx
 */

@RpcBean(value = "logisticsService")
public class LogisticsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogisticsService.class);


    //电商ID
    private String EBusinessID = "1346011";
    //电商加密私钥，快递鸟提供，注意保管，不要泄漏
    private String AppKey = "60266f42-169b-4201-84dc-18fbf5659019";
    //请求url
    private String ReqURL = "http://api.kdniao.com/Ebusiness/EbusinessOrderHandle.aspx";

    /**
     * Json方式 查询订单物流轨迹
     *
     * @throws Exception
     */
    @RpcService
    public String getOrderTracesByJson(String expCode, String expNo) throws Exception {
        if (StringUtils.isEmpty(expCode) || StringUtils.isEmpty(expNo)) {
            LOGGER.warn("参数不正确 expCode={}, expNo={}", expCode, expNo);
            return "";
        }
        LOGGER.info("getOrderTracesByJson expCode={}, expNo={} ", expCode, expNo);
        if (StringUtils.equals("SF", expCode)) {
            //获取顺丰物流信息
            String result = getSfOrderTracesByJson(expCode, expNo);
            //根据公司业务处理返回的信息......
            LOGGER.info("getOrderTracesByJson result={}", result);
            return result;
        } else if(StringUtils.equals("SHSY", expCode)){
            //上海上药物流信息
            String result = getShsyTracesByJson(expCode,expNo);
            LOGGER.info("getOrderTracesByJson result={}", result);
            return result;
        } else {
            String requestData = "{'OrderCode':'','ShipperCode':'" + expCode + "','LogisticCode':'" + expNo + "'}";
            Map<String, String> params = new HashMap<String, String>();
            params.put("RequestData", urlEncoder(requestData, "UTF-8"));
            params.put("EBusinessID", EBusinessID);
            params.put("RequestType", "1002");
            String dataSign = encrypt(requestData, AppKey, "UTF-8");
            params.put("DataSign", urlEncoder(dataSign, "UTF-8"));
            params.put("DataType", "2");
            String result = sendPost(ReqURL, params);
            //根据公司业务处理返回的信息......
            LOGGER.info("getOrderTracesByJson result={}", result);
            return result;
        }

    }


    /**
     * 前端调用查询物流轨迹
     *
     * @param expCode
     * @param expNo
     * @return
     */
    @RpcService
    public List<LogisticsTrace> getOrderTraces(String expCode, String expNo) {
        List<LogisticsTrace> traceList = Lists.newArrayList();
        try {
            String traceStr = getOrderTracesByJson(expCode, expNo);
            if (StringUtils.isNotEmpty(traceStr)) {
                //分析数据
                LogisticsTraceResponse traceResponse = JSON.parseObject(traceStr, LogisticsTraceResponse.class);
                if (null != traceResponse) {
                    Collections.reverse(traceResponse.getTraces());
                    return traceResponse.getTraces();
                }
            }
        } catch (Exception e) {
            LOGGER.error("getOrderTraces error! expCode={}, expNo={}", expCode, expNo, e);
        }
        return traceList;
    }

    /**
     * MD5加密
     *
     * @param str     内容
     * @param charset 编码方式
     * @throws Exception
     */
    @SuppressWarnings("unused")
    private String MD5(String str, String charset) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(str.getBytes(charset));
        byte[] result = md.digest();
        StringBuffer sb = new StringBuffer(32);
        for (int i = 0; i < result.length; i++) {
            int val = result[i] & 0xff;
            if (val <= 0xf) {
                sb.append("0");
            }
            sb.append(Integer.toHexString(val));
        }
        return sb.toString().toLowerCase();
    }

    /**
     * base64编码
     *
     * @param str     内容
     * @param charset 编码方式
     * @throws UnsupportedEncodingException
     */
    private String base64(String str, String charset) throws UnsupportedEncodingException {
        String encoded = base64Encode(str.getBytes(charset));
        return encoded;
    }

    @SuppressWarnings("unused")
    private String urlEncoder(String str, String charset) throws UnsupportedEncodingException {
        String result = URLEncoder.encode(str, charset);
        return result;
    }

    /**
     * 电商Sign签名生成
     *
     * @param content  内容
     * @param keyValue Appkey
     * @param charset  编码方式
     * @return DataSign签名
     * @throws UnsupportedEncodingException ,Exception
     */
    @SuppressWarnings("unused")
    private String encrypt(String content, String keyValue, String charset) throws UnsupportedEncodingException, Exception {
        if (keyValue != null) {
            return base64(MD5(content + keyValue, charset), charset);
        }
        return base64(MD5(content, charset), charset);
    }

    /**
     * 向指定 URL 发送POST方法的请求
     *
     * @param url    发送请求的 URL
     * @param params 请求的参数集合
     * @return 远程资源的响应结果
     */
    @SuppressWarnings("unused")
    private String sendPost(String url, Map<String, String> params) {
        OutputStreamWriter out = null;
        BufferedReader in = null;
        StringBuilder result = new StringBuilder();
        try {
            URL realUrl = new URL(url);
            HttpURLConnection conn = (HttpURLConnection) realUrl.openConnection();
            // 发送POST请求必须设置如下两行
            conn.setDoOutput(true);
            conn.setDoInput(true);
            // POST方法
            conn.setRequestMethod("POST");
            // 设置通用的请求属性
            conn.setRequestProperty("accept", "*/*");
            conn.setRequestProperty("connection", "Keep-Alive");
            conn.setRequestProperty("user-agent",
                    "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.connect();
            // 获取URLConnection对象对应的输出流
            out = new OutputStreamWriter(conn.getOutputStream(), "UTF-8");
            // 发送请求参数
            if (params != null) {
                StringBuilder param = new StringBuilder();
                for (Map.Entry<String, String> entry : params.entrySet()) {
                    if (param.length() > 0) {
                        param.append("&");
                    }
                    param.append(entry.getKey());
                    param.append("=");
                    param.append(entry.getValue());
                    //System.out.println(entry.getKey()+":"+entry.getValue());
                }
                //System.out.println("param:"+param.toString());
                out.write(param.toString());
            }
            // flush输出流的缓冲
            out.flush();
            // 定义BufferedReader输入流来读取URL的响应
            in = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), "UTF-8"));
            String line;
            while ((line = in.readLine()) != null) {
                result.append(line);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        //使用finally块来关闭输出流、输入流
        finally {
            try {
                if (out != null) {
                    out.close();
                }
                if (in != null) {
                    in.close();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return result.toString();
    }


    private static char[] base64EncodeChars = new char[]{
            'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H',
            'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P',
            'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X',
            'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f',
            'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n',
            'o', 'p', 'q', 'r', 's', 't', 'u', 'v',
            'w', 'x', 'y', 'z', '0', '1', '2', '3',
            '4', '5', '6', '7', '8', '9', '+', '/'};

    public static String base64Encode(byte[] data) {
        StringBuffer sb = new StringBuffer();
        int len = data.length;
        int i = 0;
        int b1, b2, b3;
        while (i < len) {
            b1 = data[i++] & 0xff;
            if (i == len) {
                sb.append(base64EncodeChars[b1 >>> 2]);
                sb.append(base64EncodeChars[(b1 & 0x3) << 4]);
                sb.append("==");
                break;
            }
            b2 = data[i++] & 0xff;
            if (i == len) {
                sb.append(base64EncodeChars[b1 >>> 2]);
                sb.append(base64EncodeChars[((b1 & 0x03) << 4) | ((b2 & 0xf0) >>> 4)]);
                sb.append(base64EncodeChars[(b2 & 0x0f) << 2]);
                sb.append("=");
                break;
            }
            b3 = data[i++] & 0xff;
            sb.append(base64EncodeChars[b1 >>> 2]);
            sb.append(base64EncodeChars[((b1 & 0x03) << 4) | ((b2 & 0xf0) >>> 4)]);
            sb.append(base64EncodeChars[((b2 & 0x0f) << 2) | ((b3 & 0xc0) >>> 6)]);
            sb.append(base64EncodeChars[b3 & 0x3f]);
        }
        return sb.toString();
    }

    /**
     * 顺丰物流查询
     *
     * @param expCode
     * @param expNo
     * @return
     * @throws Exception
     */
    public String getSfOrderTracesByJson(String expCode, String expNo) throws Exception {
        LogisticsTraceResponse logisticsTraceResponse = new LogisticsTraceResponse();
        RecipeParameterDao recipeParameterDao = DAOFactory.getDAO(RecipeParameterDao.class);
        RecipeOrderDAO recipeOrderDAO = DAOFactory.getDAO(RecipeOrderDAO.class);
        String status = "logistics_sf_status";
        String statusCode = recipeParameterDao.getByName(status);
        if (StringUtils.equals("0", statusCode)) {
            return "";
        }
        RecipeOrder recipeOrder = recipeOrderDAO.getByTrackingNumber(expNo);
        if (recipeOrder == null) {
            return "";
        }
        String phone = recipeOrder.getRecMobile();
        String check_phoneNo = "";
        //取手机号后四位
        if (StringUtils.isNotEmpty(phone) && phone.length() > 4) {
            check_phoneNo = phone.substring(phone.length() - 4, phone.length());
        }
        String client_url = "logistics_sf_client_url";
        String client_code = "logistics_sf_client_code";
        String check_word = "logistics_sf_check_word";
        String clientCode = recipeParameterDao.getByName(client_code);
        String checkword = recipeParameterDao.getByName(check_word);
        String reqURL = recipeParameterDao.getByName(client_url);
        String reqXml = "<Request service='RouteService' lang='zh-CN'> <Head>NAGRI-HEALTH</Head> <Body> <RouteRequest tracking_type='1' method_type='1' tracking_number='" + expNo + "' check_phoneNo='" + check_phoneNo + "'/> </Body> </Request>";
        String myReqXML = reqXml.replace("NAGRI-HEALTH", clientCode);
        LOGGER.info("LogisticsService.getSfOrderTracesByJson:http请求物流信息入参:url={}-{}！", reqURL, myReqXML);
        CallExpressServiceTools client = CallExpressServiceTools.getInstance();
        String respXml = client.callSfExpressServiceByCSIM(reqURL, myReqXML, clientCode, checkword);
        LOGGER.info("LogisticsService.getSfOrderTracesByJson:http请求物流信息出参:url={}-{}！", reqURL, respXml);
        if (respXml != null) {
            JSONObject respObj = JSON.parseObject(xml2json(respXml));
            //判断物流信息查询是否成功
            if (StringUtils.equals("OK", respObj.getJSONObject("Response").getString("Head").trim()) && respObj.getJSONObject("Response").get("Body") != null) {
                List<LogisticsTrace> logisticsTraces = new ArrayList<LogisticsTrace>();
                JSONObject routeResponse = respObj.getJSONObject("Response").getJSONObject("Body").getJSONObject("RouteResponse");
                if (routeResponse != null) {
                    String routesInfo = routeResponse.getString("Route");
                    //判断当前节点是对象还是对象数组
                    if (checkJsonObj(routesInfo)) {
                        JSONObject route = routeResponse.getJSONObject("Route");
                        LogisticsTrace logisticsTrace = new LogisticsTrace();
                        logisticsTrace.setAcceptStation(route.getString("@accept_address") + ":" + route.getString("@remark"));
                        logisticsTrace.setAcceptTime(route.getString("@accept_time"));
                        logisticsTraces.add(logisticsTrace);
                    } else if (checkJsonArray(routesInfo)) {
                        JSONArray routes = routeResponse.getJSONArray("Route");
                        for (int j = 0; j < routes.size(); j++) {
                            JSONObject route = routes.getJSONObject(j);
                            LogisticsTrace logisticsTrace = new LogisticsTrace();
                            logisticsTrace.setAcceptStation(route.getString("@accept_address") + ":" + route.getString("@remark"));
                            logisticsTrace.setAcceptTime(route.getString("@accept_time"));
                            logisticsTraces.add(logisticsTrace);
                        }
                    }
                }
                LOGGER.info("LogisticsService.getSfOrderTracesByJson:http请求物流信息出参:url={}-{}！", reqURL, JSON.toJSONString(logisticsTraces));
                logisticsTraceResponse.setTraces(logisticsTraces);
                return JSON.toJSONString(logisticsTraceResponse);
            } else {
                return "";
            }
        } else {
            return "";
        }

    }

    public static boolean checkJsonObj(String json) {
        Object obj = JSON.parse(json);
        if (obj instanceof JSONObject) {
            return true;
        }
        return false;
    }

    public static boolean checkJsonArray(String json) {
        Object obj = JSON.parse(json);
        if (obj instanceof JSONArray) {
            return true;
        }
        return false;

    }

    /**
     * json string convert to xml string
     */
    public static String json2xml(String json) {
        StringReader input = new StringReader(json);
        StringWriter output = new StringWriter();
        JsonXMLConfig config = new JsonXMLConfigBuilder().multiplePI(false).repairingNamespaces(false).build();
        try {
            XMLEventReader reader = new JsonXMLInputFactory(config).createXMLEventReader(input);
            XMLEventWriter writer = XMLOutputFactory.newInstance().createXMLEventWriter(output);
            writer = new PrettyXMLEventWriter(writer);
            writer.add(reader);
            reader.close();
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                output.close();
                input.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (output.toString().length() >= 38) {//remove <?xml version="1.0" encoding="UTF-8"?>
            return output.toString().substring(39);
        }
        return output.toString();
    }

    /**
     * xml string convert to json string
     */
    public static String xml2json(String xml) {
        StringReader input = new StringReader(xml);
        StringWriter output = new StringWriter();
        JsonXMLConfig config = new JsonXMLConfigBuilder().autoArray(true).autoPrimitive(true).prettyPrint(true).build();
        try {
            XMLEventReader reader = XMLInputFactory.newInstance().createXMLEventReader(input);
            XMLEventWriter writer = new JsonXMLOutputFactory(config).createXMLEventWriter(output);
            writer.add(reader);
            reader.close();
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                output.close();
                input.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return output.toString();
    }

    private static boolean hasOrgan(String organ, String parames){
        if (StringUtils.isNotEmpty(parames)) {
            String[] organs = parames.split(",");
            for (String o : organs) {
                if (organ.equals(o)) {
                    return true;
                }
            }
        }
        return false;
    }
    /**
     * 根据物流code与物流单号查询物流轨迹（目前仅针对上海市中医院）
     * @param expCode
     * @param expNo
     * @return
     * @throws Exception
     */
    @RpcService
    public String getShsyTracesByJson(String expCode,String expNo) throws Exception{
        RecipeParameterDao recipeParameterDao = DAOFactory.getDAO(RecipeParameterDao.class);
        RecipeOrderDAO recipeOrderDAO = DAOFactory.getDAO(RecipeOrderDAO.class);
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);

        String item = DictionaryUtil.getKeyByValue("eh.cdr.dictionary.KuaiDiNiaoCode",expCode);
        String ebs_organ = recipeParameterDao.getByName("ebs_organ");
        Recipe recipe = new Recipe();
        if(StringUtils.isNotBlank(item)){
            Integer logisticsCompany = Integer.parseInt(item);
            String orderCode = recipeOrderDAO.getOrderCodeByLogisticsCompanyAndTrackingNumber(logisticsCompany,expNo);
            if(orderCode != null){
                List<Recipe> recipeList = recipeDAO.findRecipeListByOrderCode(orderCode);
                if(recipeList.size() > 0){
                    recipe = recipeList.get(0);
                }
            }
        }
        if (hasOrgan(recipe.getClinicOrgan().toString(), ebs_organ)) {
            String prescripNo = recipe.getRecipeCode();
            String hospitalName = recipeParameterDao.getByName(recipe.getClinicOrgan()+"_shyy-organname");
            Map<String, Object> params = new HashMap<>();
            params.put("prescripNo",prescripNo);
            params.put("hospitalName",hospitalName);
            params.put("prescribeDate","");
            String request = jsonToXml(params);
            EsbWebService xkyyHelper = new EsbWebService();
            Map<String, String> param=new HashMap<String, String>();
            String url = recipeParameterDao.getByName("logistics_shxk_url");
            param.put("url", url);
            String fetchLogisticsProcessMethod = "fetchLogisticsProcess";
            xkyyHelper.initConfig(param);
            String webServiceResult = xkyyHelper.HXCFZT(request, fetchLogisticsProcessMethod);
            LOGGER.info("getDrugInventory webServiceResult:{}. ", webServiceResult);
            Map maps = (Map)JSON.parse(webServiceResult);

            LogisticsTraceResponse logisticsTraceResponse = new LogisticsTraceResponse();
            Boolean success = (Boolean) maps.get("success");
            String code = (String) maps.get("code");
            List<LogisticsTrace> traces = new ArrayList<>();
            if("0".equals(code)){
                List<Map> list = (List)maps.get("result");
                if(list.size() > 0){
                    for(Map shsyTrace : list){
                        LogisticsTrace logisticsTrace = new LogisticsTrace();
                        logisticsTrace.setAcceptStation((String)shsyTrace.get("processRemark"));
                        logisticsTrace.setAcceptTime((String)shsyTrace.get("processTime"));
                        traces.add(logisticsTrace);
                    }
                }
            }
            logisticsTraceResponse.setSuccess(success);
            logisticsTraceResponse.setTraces(traces);
            logisticsTraceResponse.setLogisticCode(expNo);
            logisticsTraceResponse.setShipperCode(expCode);
            return JSON.toJSONString(logisticsTraceResponse);
        } else {
            String prescripNo = recipe.getRecipeCode();
            String hospitalName = recipeParameterDao.getByName(recipe.getClinicOrgan()+"_shyy-organname");
            String appId = recipeParameterDao.getByName(recipe.getClinicOrgan()+"_logistics_shsy_app_id");
            String appSecret = recipeParameterDao.getByName(recipe.getClinicOrgan()+"_logistics_shsy_app_secret");
            Map<String, Object> params = new HashMap<>();
            params.put("prescripNo",prescripNo);
            params.put("hospitalName",hospitalName);
            String json = JSONObject.toJSONString(params);
            LOGGER.info("上海上药物流信息查询，签名认证参数：APP_ID={},APP_SECRET={},json={}",appId,appSecret,json);
            long timestamp = System.currentTimeMillis();
            String url = recipeParameterDao.getByName(recipe.getClinicOrgan()+"_logistics_shsy_url");
            HttpPost method = new HttpPost(url);
            method.addHeader("ACCESS_APPID", appId);
            method.addHeader("ACCESS_TIMESTAMP", String.valueOf(timestamp));
            method.addHeader("ACCESS_SIGANATURE", AppSiganatureUtils.createSiganature(json, appId, appSecret,
                    timestamp));
            method.setEntity(new StringEntity(json, ContentType.APPLICATION_JSON));
            HttpClient httpClient = HttpClientUtils.getHttpClient();
            HttpResponse httpResponse = httpClient.execute(method);
            HttpEntity entity = httpResponse.getEntity();
            String response = EntityUtils.toString(entity);
            JSONObject jsonObject = JSON.parseObject(response);
            LogisticsTraceResponse logisticsTraceResponse = new LogisticsTraceResponse();
            Boolean success = jsonObject.getBoolean("success");
            String code = jsonObject.getString("code");
            List<LogisticsTrace> traces = new ArrayList<>();
            if("0".equals(code)){
                JSONArray jsonArray = jsonObject.getJSONArray("result");
                List<ShsyTrace> list = jsonArray.toJavaList(ShsyTrace.class);
                if(list.size() > 0){
                    for(ShsyTrace shsyTrace : list){
                        LogisticsTrace logisticsTrace = new LogisticsTrace();
                        logisticsTrace.setAcceptStation(shsyTrace.getProcessRemark());
                        logisticsTrace.setAcceptTime(shsyTrace.getProcessTime());
                        traces.add(logisticsTrace);
                    }
                }
            }
            logisticsTraceResponse.setSuccess(success);
            logisticsTraceResponse.setTraces(traces);
            logisticsTraceResponse.setLogisticCode(expNo);
            logisticsTraceResponse.setShipperCode(expCode);
            return JSON.toJSONString(logisticsTraceResponse);
        }

    }

    private String jsonToXml(Map<String, Object> params){
        StringBuilder result = new StringBuilder("<root><body><params>");
        if (params != null) {
            for (Map.Entry<String, Object> entry : params.entrySet()) {
                result.append("<").append(entry.getKey()).append(">").append(entry.getValue()).append("</").append(entry.getKey()).append(">");
            }
        }
        result.append("</params></body></root>");
        return result.toString();
    }
}
