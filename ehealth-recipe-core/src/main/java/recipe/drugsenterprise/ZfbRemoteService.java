package recipe.drugsenterprise;

import com.ngari.recipe.entity.DrugsEnterprise;
import ctd.persistence.DAOFactory;
import ctd.util.JSONUtils;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.bean.DrugEnterpriseResult;
import recipe.dao.DrugsEnterpriseDAO;
import recipe.util.RSAUtil;

import java.util.ArrayList;
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

        try {
            if (-1 != drugsEnterprise.getAuthenUrl().indexOf("http:")) {
                // 创建默认的httpClient实例.
                CloseableHttpClient httpclient = HttpClients.createDefault();
                HttpPost httpPost = new HttpPost(drugsEnterprise.getAuthenUrl());
                String sign = RSAUtil.privateEncrypt(RSAUtil.getAppid() + Calendar.getInstance().getTimeInMillis(), RSAUtil.getPrivateKey());
                System.out.println("sign:" + sign);
                // 创建参数队列
                List<NameValuePair> formparams = new ArrayList<NameValuePair>();
                formparams.add(new BasicNameValuePair("appid", RSAUtil.getAppid()));
                formparams.add(new BasicNameValuePair("sign", sign));
                UrlEncodedFormEntity uefEntity = new UrlEncodedFormEntity(formparams);
                httpPost.setEntity(uefEntity);
                CloseableHttpResponse response = httpclient.execute(httpPost);
                HttpEntity entity = response.getEntity();
                System.out.println(JSONUtils.toString("zfbrespones:" + JSONUtils.toString(entity)));
            }
        } catch (Exception e) {
            LOGGER.warn("[{}][{}]更新异常。", depId, depName, e);
        }
    }

    @Override
    public DrugEnterpriseResult pushRecipeInfo(List<Integer> recipeIds, DrugsEnterprise enterprise) {
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
