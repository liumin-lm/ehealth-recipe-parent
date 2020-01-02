package recipe.drugsenterprise;

import com.alibaba.fastjson.JSONObject;
import com.ngari.recipe.entity.DrugsEnterprise;
import com.ngari.recipe.entity.SaleDrugList;
import ctd.persistence.DAOFactory;
import ctd.util.JSONUtils;
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
import recipe.dao.DrugsEnterpriseDAO;
import recipe.dao.RecipeParameterDao;
import recipe.dao.SaleDrugListDAO;
import recipe.drugsenterprise.bean.StoreInventoryResponse;
import recipe.drugsenterprise.bean.YueyResponse;
import recipe.util.DigestUtil;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * 药企信息变更服务,比如库存变更,价格变动
 * 此定时更新不再使用
 * @author yinsheng
 * @date 2019\11\13 0013 15:34
 */
@RpcBean("enterpriseInfoChangeService")
public class EnterpriseInfoChangeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnterpriseInfoChangeService.class);

    @RpcService
    public void updateStoreInventory(){
        /*try{
            RecipeParameterDao recipeParameterDao = DAOFactory.getDAO(RecipeParameterDao.class);
            SaleDrugListDAO saleDrugListDAO = DAOFactory.getDAO(SaleDrugListDAO.class);
            DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
            String time = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
            String roledata = recipeParameterDao.getByName("yy-roledata");
            String password = recipeParameterDao.getByName("yy-password");
            String roleidStr = recipeParameterDao.getByName("yy-roleid");
            Long roleid = Long.parseLong(roleidStr);
            String url = recipeParameterDao.getByName("yy-url");
            String sign = DigestUtil.encodeMD5(roledata + password + time);
            String param = String.format("?timestamp=%s&roledata=%s&sign=%s", time, roledata, sign);
            String data = "{\"intfaceStatus\":{\"roleid\":" + roleid + "}}";
            CloseableHttpClient httpClient = HttpClients.createDefault();
            String path = url + param;
            HttpPost httpPost = new HttpPost(path);
            StringEntity requestEntry = new StringEntity(data, ContentType.APPLICATION_JSON);
            httpPost.setEntity(requestEntry);
            //获取响应消息
            CloseableHttpResponse response = httpClient.execute(httpPost);
            HttpEntity httpEntity = response.getEntity();
            String responseStr = EntityUtils.toString(httpEntity);
            LOGGER.info("EnterpriseInfoChangeService-updateStoreInventory responseStr:{}.", responseStr);
            YueyResponse storeResponses = JSONUtils.parse(responseStr, YueyResponse.class);
            if (storeResponses != null && "200".equals(storeResponses.getCode())) {
                for (StoreInventoryResponse storeInventoryResponse : storeResponses.getData()) {
                    String hwarecode = storeInventoryResponse.getHwarecode();
                    double storeqty = storeInventoryResponse.getStoreqty();
                    List<DrugsEnterprise> drugsEnterprises = drugsEnterpriseDAO.findAllDrugsEnterpriseByName("岳阳-钥世圈");
                    SaleDrugList saleDrugList = saleDrugListDAO.getByOrganIdAndDrugCode(drugsEnterprises.get(0).getId(), hwarecode);
                    if (saleDrugList != null) {
                        if (saleDrugList.getInventory() != null && saleDrugList.getInventory().doubleValue() != storeqty) {
                            saleDrugList.setInventory(new BigDecimal(storeqty));
                            saleDrugListDAO.update(saleDrugList);
                        }
                    }
                }
            }
        }catch (Exception e){
            LOGGER.info("EnterpriseInfoChangeService-updateStoreInventory 更新药店药品库存失败.");
        }*/
    }
}
