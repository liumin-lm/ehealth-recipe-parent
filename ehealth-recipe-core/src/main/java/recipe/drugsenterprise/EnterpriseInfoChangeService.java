package recipe.drugsenterprise;

import com.alibaba.fastjson.JSONObject;
import ctd.persistence.DAOFactory;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.dao.RecipeParameterDao;
import recipe.drugsenterprise.bean.StoreInventoryResponse;
import recipe.util.DigestUtil;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * 药企信息变更服务,比如库存变更,价格变动
 * @author yinsheng
 * @date 2019\11\13 0013 15:34
 */
@RpcBean("enterpriseInfoChangeService")
public class EnterpriseInfoChangeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnterpriseInfoChangeService.class);

    @RpcService
    public void updateStoreInventory(){
        try{
            RecipeParameterDao recipeParameterDao = DAOFactory.getDAO(RecipeParameterDao.class);
            String time = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
            String roledata = recipeParameterDao.getByName("yy-roledata");
            String password = recipeParameterDao.getByName("yy-password");
            String url = recipeParameterDao.getByName("yy-url");
            String sign = DigestUtil.encodeMD5(roledata + password + time);
            String param = String.format("?timestamp=%s&roledata=%s&sign=%s", time, roledata, sign);
            String data = "{\"intfaceStatus\":{\"roleid\":1220001}}";
            CloseableHttpClient httpClient = HttpClients.createDefault();
            String path = url + param;
            HttpPost httpPost = new HttpPost(path);
            StringEntity requestEntry = new StringEntity(data, ContentType.APPLICATION_JSON);
            httpPost.setEntity(requestEntry);
            //获取响应消息
            CloseableHttpResponse response = httpClient.execute(httpPost);
            HttpEntity httpEntity = response.getEntity();
            String responseStr = EntityUtils.toString(httpEntity);
            List<StoreInventoryResponse> storeResponses = JSONObject.parseArray(responseStr, StoreInventoryResponse.class);
            LOGGER.info("EnterpriseInfoChangeService-updateStoreInventory storeResponses:{}.", storeResponses);
            if (storeResponses != null && storeResponses.size() > 0) {

            }
        }catch (Exception e){
            LOGGER.info("EnterpriseInfoChangeService-updateStoreInventory 更新药店药品库存失败.");
        }
    }
}
