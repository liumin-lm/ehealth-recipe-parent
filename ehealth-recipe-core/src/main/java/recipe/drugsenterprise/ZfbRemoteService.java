package recipe.drugsenterprise;

import com.ngari.recipe.entity.DrugsEnterprise;
import com.ngari.recipe.entity.Recipe;
import ctd.persistence.DAOFactory;
import ctd.util.JSONUtils;
import org.apache.commons.collections.CollectionUtils;
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
import recipe.bean.DrugEnterpriseResult;
import recipe.dao.DrugsEnterpriseDAO;
import recipe.dao.RecipeDAO;
import recipe.drugsenterprise.bean.ZfbTokenRequest;
import recipe.drugsenterprise.bean.ZfbTokenResponse;
import recipe.util.RSAUtil;

import java.io.IOException;
import java.util.Calendar;
import java.util.List;

/**
 * @author： 0184/yu_yun
 * @date： 2018/9/18
 * @description： 支付宝配送商实现
 * @version： 1.0
 */
public class ZfbRemoteService extends AccessDrugEnterpriseService {

    /**
     * logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ZfbRemoteService.class);

    @Override
    public void tokenUpdateImpl(DrugsEnterprise drugsEnterprise) {
        DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        String depName = drugsEnterprise.getName();
        Integer depId = drugsEnterprise.getId();
        // 创建默认的httpClient实例.
        CloseableHttpClient httpclient = HttpClients.createDefault();
        try {
            if (-1 != drugsEnterprise.getAuthenUrl().indexOf("http:")) {
                HttpPost httpPost = new HttpPost(drugsEnterprise.getAuthenUrl());
                //组装请求参数
                String sign = RSAUtil.privateEncrypt(RSAUtil.getAppid() + Calendar.getInstance().getTimeInMillis(),
                        RSAUtil.getPrivateKey());
                ZfbTokenRequest request = new ZfbTokenRequest();
                request.setSign(sign);
                request.setAppid(RSAUtil.getAppid());
                StringEntity requestEntity = new StringEntity(JSONUtils.toString(request), ContentType.APPLICATION_JSON);
                httpPost.setEntity(requestEntity);

                //获取响应消息
                CloseableHttpResponse response = httpclient.execute(httpPost);
                HttpEntity httpEntity = response.getEntity();
                String responseStr = EntityUtils.toString(httpEntity);
                LOGGER.info("[{}][{}]token更新返回:{}", depId, depName, responseStr);
                ZfbTokenResponse zfbResponse = JSONUtils.parse(responseStr, ZfbTokenResponse.class);
                if("0".equals(zfbResponse.getCode())) {
                    //成功
                    drugsEnterpriseDAO.updateTokenById(depId, zfbResponse.getToken());
                }else{
                    //失败
                    LOGGER.info("[{}][{}]token更新失败:{}", depId, depName, zfbResponse.getMsg());
                }
                //关闭 HttpEntity 输入流
                EntityUtils.consume(httpEntity);
                response.close();
                httpclient.close();
            }
        } catch (Exception e) {
            LOGGER.warn("[{}][{}]更新异常。", depId, depName, e);
        } finally {
            try {
                httpclient.close();
            } catch (IOException e) {
//                e.printStackTrace();
            }
        }
    }

    @Override
    public DrugEnterpriseResult pushRecipeInfo(List<Integer> recipeIds, DrugsEnterprise enterprise) {
        DrugEnterpriseResult result = DrugEnterpriseResult.getSuccess();
        if (CollectionUtils.isEmpty(recipeIds)) {
            result.setMsg("处方ID参数为空");
            result.setCode(DrugEnterpriseResult.FAIL);
            return result;
        }

        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        String drugEpName = enterprise.getName();
        Integer depId = enterprise.getId();
        List<Recipe> recipeList = recipeDAO.findByRecipeIds(recipeIds);
        if(CollectionUtils.isNotEmpty(recipeList)) {
            Recipe dbRecipe = recipeList.get(0);


        }


        return null;
    }

    @Override
    public DrugEnterpriseResult scanStock(Integer recipeId, DrugsEnterprise drugsEnterprise) {
        return null;
    }

    @Override
    public DrugEnterpriseResult syncEnterpriseDrug(DrugsEnterprise drugsEnterprise, List<Integer> drugIdList) {
        return null;
    }

    @Override
    public DrugEnterpriseResult pushCheckResult(Integer recipeId, Integer checkFlag, DrugsEnterprise enterprise) {
        return null;
    }

    @Override
    public DrugEnterpriseResult findSupportDep(List<Integer> recipeIds, DrugsEnterprise enterprise) {
        return null;
    }

    @Override
    public String getDrugEnterpriseCallSys() {
        return null;
    }
}
