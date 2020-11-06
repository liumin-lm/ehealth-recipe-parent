package recipe.drugsenterprise;

import com.ngari.patient.dto.OrganDTO;
import com.ngari.patient.service.OrganService;
import com.ngari.recipe.drugsenterprise.model.DrugsDataBean;
import com.ngari.recipe.entity.DrugsEnterprise;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.hisprescription.model.HospitalRecipeDTO;
import ctd.persistence.DAOFactory;
import ctd.util.JSONUtils;
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
import org.springframework.beans.factory.annotation.Autowired;
import recipe.ApplicationUtils;
import recipe.bean.DrugEnterpriseResult;
import recipe.constant.DrugEnterpriseConstant;
import recipe.dao.RecipeDAO;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author yinsheng
 * @date 2020\11\4 0004 14:35
 */
public class YsqnRemoteService extends AccessDrugEnterpriseService {

    private static final Logger LOGGER = LoggerFactory.getLogger(YsqnRemoteService.class);

    private static final String requestHeadJsonKey = "Content-Type";

    private static final String requestHeadJsonValue = "application/json";

    private static final String requestHeadPowerKey = "Authorization";

    @Autowired
    private YsqRemoteService ysqRemoteService;

    @Override
    public void tokenUpdateImpl(DrugsEnterprise drugsEnterprise) {

    }

    @Override
    public DrugEnterpriseResult pushRecipeInfo(List<Integer> recipeIds, DrugsEnterprise enterprise) {
        DrugEnterpriseResult result = DrugEnterpriseResult.getSuccess();
        Integer hosInteriorSupport = enterprise.getHosInteriorSupport();
        Boolean hosInteriorSupportFlag = true;
        if (hosInteriorSupport != null && hosInteriorSupport == 1) {
            //为补充库存
            hosInteriorSupportFlag = false;
        }
        OrganService organService = ApplicationUtils.getBasicService(OrganService.class);
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        Recipe recipe = recipeDAO.getByRecipeId(recipeIds.get(0));
        OrganDTO organDTO = organService.getByOrganId(recipe.getClinicOrgan());
        List<Map<String, Object>> recipeInfoList = ysqRemoteService.getYsqRecipeInfo(recipeIds, hosInteriorSupportFlag, enterprise);
        //最终发给药企的json数据
        Map<String, Object> sendInfo = new HashMap<>();
        sendInfo.put("APPKEY", enterprise.getUserId());
        sendInfo.put("APPSECRET", enterprise.getPassword());
        sendInfo.put("SOURCECODE", organDTO.getOrganizeCode());
        sendInfo.put("TITLES", recipeInfoList);
        try {
            //发送http请求获取
            CloseableHttpClient httpclient = HttpClients.createDefault();
            //生成post请求
            HttpPost httpPost = new HttpPost(enterprise.getToken());
            httpPost.setHeader("Content-Type", "application/json");
            //将请求参数转成json
            StringEntity requestEntity = new StringEntity(JSONUtils.toString(sendInfo), ContentType.APPLICATION_JSON);
            httpPost.setEntity(requestEntity);
            //获取响应消息
            CloseableHttpResponse response = httpclient.execute(httpPost);
            HttpEntity responseEntity = response.getEntity();
            String responseStr =  EntityUtils.toString(responseEntity);
            LOGGER.info("YsqnRemoteService.pushRecipeInfo responseStr:{}.", responseStr);

        } catch (Exception e) {
            LOGGER.error("YsqnRemoteService.pushRecipeInfo error msg:{}.", e.getMessage(), e);
        }
        return result;
    }

    @Override
    public DrugEnterpriseResult pushRecipe(HospitalRecipeDTO hospitalRecipeDTO, DrugsEnterprise enterprise) {
        return ysqRemoteService.pushRecipe(hospitalRecipeDTO, enterprise);
    }

    @Override
    public String getDrugInventory(Integer drugId, DrugsEnterprise drugsEnterprise, Integer organId) {
        return ysqRemoteService.getDrugInventory(drugId, drugsEnterprise, organId);
    }

    @Override
    public List<String> getDrugInventoryForApp(DrugsDataBean drugsDataBean, DrugsEnterprise drugsEnterprise, Integer flag) {
        return ysqRemoteService.getDrugInventoryForApp(drugsDataBean, drugsEnterprise, flag);
    }

    @Override
    public DrugEnterpriseResult scanStock(Integer recipeId, DrugsEnterprise drugsEnterprise) {
        return ysqRemoteService.scanStock(recipeId, drugsEnterprise);
    }

    @Override
    public DrugEnterpriseResult syncEnterpriseDrug(DrugsEnterprise drugsEnterprise, List<Integer> drugIdList) {
        return ysqRemoteService.syncEnterpriseDrug(drugsEnterprise, drugIdList);
    }

    @Override
    public DrugEnterpriseResult pushCheckResult(Integer recipeId, Integer checkFlag, DrugsEnterprise enterprise) {
        return ysqRemoteService.pushCheckResult(recipeId, checkFlag, enterprise);
    }

    @Override
    public DrugEnterpriseResult findSupportDep(List<Integer> recipeIds, Map ext, DrugsEnterprise enterprise) {
        return ysqRemoteService.findSupportDep(recipeIds, ext, enterprise);
    }

    @Override
    public String getDrugEnterpriseCallSys() {
        return DrugEnterpriseConstant.COMPANY_YSQN;
    }
}
