package recipe.drugsenterprise;

import com.google.common.collect.ImmutableMap;
import com.ngari.patient.dto.OrganDTO;
import com.ngari.patient.service.OrganService;
import com.ngari.recipe.drugsenterprise.model.DrugsDataBean;
import com.ngari.recipe.dto.DrugStockAmountDTO;
import com.ngari.recipe.entity.DrugsEnterprise;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeOrder;
import com.ngari.recipe.entity.Recipedetail;
import com.ngari.recipe.hisprescription.model.HospitalRecipeDTO;
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
import org.springframework.beans.factory.annotation.Autowired;
import recipe.ApplicationUtils;
import recipe.bean.DrugEnterpriseResult;
import recipe.constant.DrugEnterpriseConstant;
import recipe.constant.RecipeMsgEnum;
import recipe.constant.RecipeStatusConstant;
import recipe.dao.DrugsEnterpriseDAO;
import recipe.dao.RecipeDAO;
import recipe.dao.RecipeOrderDAO;
import recipe.service.RecipeLogService;
import recipe.service.RecipeMsgService;
import recipe.service.RecipeOrderService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author yinsheng
 * @date 2020\11\4 0004 14:35
 */
@RpcBean(value = "ysqnRemoteService")
public class YsqnRemoteService extends AccessDrugEnterpriseService {

    private static final Logger LOGGER = LoggerFactory.getLogger(YsqnRemoteService.class);

    @Autowired
    private YsqRemoteService ysqRemoteService;

    @Override
    public void tokenUpdateImpl(DrugsEnterprise drugsEnterprise) {

    }

    @RpcService
    public void test (List<Integer> recipeIds, Integer depId) {
        DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        DrugsEnterprise drugsEnterprise = drugsEnterpriseDAO.getById(depId);
        pushRecipeInfo(recipeIds, drugsEnterprise);
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
        RecipeOrderDAO recipeOrderDAO = DAOFactory.getDAO(RecipeOrderDAO.class);
        Recipe recipe = recipeDAO.getByRecipeId(recipeIds.get(0));
        RecipeOrder recipeOrder = recipeOrderDAO.getByOrderCode(recipe.getOrderCode());
        if (new Integer(1).equals(recipeOrder.getPushFlag())) {
            //表示已经推送
            return result;
        }
        OrganDTO organDTO = organService.getByOrganId(recipe.getClinicOrgan());
        List<Map<String, Object>> recipeInfoList = ysqRemoteService.getYsqRecipeInfo(recipeIds, hosInteriorSupportFlag, enterprise);
        //最终发给药企的json数据
        Map<String, Object> sendInfo = new HashMap<>();
        String[] userId = enterprise.getUserId().split("\\|");
        sendInfo.put("APPKEY", userId[0]);
        sendInfo.put("APPSECRET", enterprise.getPassword());
        sendInfo.put("SOURCECODE", organDTO.getOrganizeCode());
        sendInfo.put("TITLES", recipeInfoList);
        try {
            //发送http请求获取
            CloseableHttpClient httpclient = HttpClients.createDefault();
            //生成post请求
            HttpPost httpPost = new HttpPost(enterprise.getAuthenUrl());
            httpPost.setHeader("Content-Type", "application/json");
            LOGGER.info("YsqnRemoteService.pushRecipeInfo sendInfo:{}.", JSONUtils.toString(sendInfo));
            //将请求参数转成json
            StringEntity requestEntity = new StringEntity(JSONUtils.toString(sendInfo), ContentType.APPLICATION_JSON);
            httpPost.setEntity(requestEntity);
            //获取响应消息
            CloseableHttpResponse response = httpclient.execute(httpPost);
            HttpEntity responseEntity = response.getEntity();
            String responseStr =  EntityUtils.toString(responseEntity);
            LOGGER.info("YsqnRemoteService.pushRecipeInfo responseStr:{}.", responseStr);
            Map resultMap = JSONUtils.parse(responseStr, Map.class);
            RecipeOrderService orderService = ApplicationUtils.getRecipeService(RecipeOrderService.class);
            if (resultMap != null && (boolean)resultMap.get("SUCCESS")) {
                String message = (String)resultMap.get("MESSAGE");
                if (DrugEnterpriseResult.SUCCESS.equals(result.getCode())) {
                    recipeDAO.updatePushFlagByRecipeId(recipeIds);
                    orderService.updateOrderInfo(recipeOrderDAO.getOrderCodeByRecipeIdWithoutCheck(recipeIds.get(0)), ImmutableMap.of("pushFlag", 1), null);
                    RecipeLogService.saveRecipeLog(recipeIds.get(0), RecipeStatusConstant.CHECK_PASS, RecipeStatusConstant.CHECK_PASS, "药企推送成功:" + enterprise.getName() + message);
                    for (Integer recipeId : recipeIds) {
                        //推送审核结果
                        pushCheckResult(recipeId, 1, enterprise);
                    }
                    if (new Integer(3).equals(enterprise.getExpressFeePayWay())){
                        //推送处方运费待支付消息提醒
                        RecipeMsgService.sendRecipeMsg(RecipeMsgEnum.RECIPE_EXPRESSFEE_REMIND_NOPAY,recipeIds.get(0));
                    }
                } else {
                    for (Integer recipeId : recipeIds) {
                        orderService.updateOrderInfo(recipeOrderDAO.getOrderCodeByRecipeIdWithoutCheck(recipeId), ImmutableMap.of("pushFlag", -1), null);
                        RecipeLogService.saveRecipeLog(recipeId, RecipeStatusConstant.CHECK_PASS, RecipeStatusConstant.CHECK_PASS, "推送处方失败,药企：" + enterprise.getName() + message + ",错误：" + result.getMsg());
                    }
                    //当前钥世圈没有在线支付的情况
                    result.setMsg("推送处方失败，" + result.getMsg());
                    result.setCode(DrugEnterpriseResult.FAIL);
                }
            } else {
                String message = (String)resultMap.get("MESSAGE");
                orderService.updateOrderInfo(recipeOrderDAO.getOrderCodeByRecipeIdWithoutCheck(recipeIds.get(0)), ImmutableMap.of("pushFlag", -1), null);
                RecipeLogService.saveRecipeLog(recipeIds.get(0), RecipeStatusConstant.CHECK_PASS, RecipeStatusConstant.CHECK_PASS, "推送处方失败,药企：" + enterprise.getName() + message + ",错误：" + result.getMsg());
                //当前钥世圈没有在线支付的情况
                result.setMsg("推送处方失败，" + result.getMsg());
                result.setCode(DrugEnterpriseResult.FAIL);
            }
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
    public DrugStockAmountDTO scanEnterpriseDrugStock(Recipe recipe, DrugsEnterprise drugsEnterprise, List<Recipedetail> recipeDetails) {
        return ysqRemoteService.scanEnterpriseDrugStock(recipe, drugsEnterprise, recipeDetails);
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
