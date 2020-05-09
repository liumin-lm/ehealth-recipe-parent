/*
package recipe.drugsenterprise;

import com.ngari.recipe.entity.*;
import com.ngari.recipe.hisprescription.model.HospitalRecipeDTO;
import ctd.persistence.DAOFactory;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import groovy.util.logging.Slf4j;
import org.apache.axis.Constants;
import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import org.apache.axis.encoding.XMLType;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.bean.DrugEnterpriseResult;
import recipe.constant.DrugEnterpriseConstant;
import recipe.dao.*;
import recipe.drugsenterprise.bean.YnsPharmacyAndStockRequest;
import recipe.drugsenterprise.bean.yd.httpclient.HttpsClientUtils;
import recipe.service.RecipeListService;
import recipe.util.MapValueUtil;

import javax.xml.namespace.QName;
import javax.xml.rpc.ParameterMode;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

*/
/**
 * @author yinsheng
 * @date 2020\5\8 0008 15:08
 *//*

@RpcBean
public class LmgyRemoteService extends AccessDrugEnterpriseService {

    RecipeDetailDAO recipeDetailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);

    SaleDrugListDAO saleDrugListDAO = DAOFactory.getDAO(SaleDrugListDAO.class);

    RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);

    private static final Logger LOGGER = LoggerFactory.getLogger(LmgyRemoteService.class);

    private static String NAME_SPACE = "http://tempuri.org/";

    private static String method="ngarihealth_checkstock";

    private static String RESULT_SUCCESS = "0";


    @Override
    public void tokenUpdateImpl(DrugsEnterprise drugsEnterprise) {

    }

    @Override
    public DrugEnterpriseResult pushRecipeInfo(List<Integer> recipeIds, DrugsEnterprise enterprise) {
        return null;
    }

    @Override
    public DrugEnterpriseResult pushRecipe(HospitalRecipeDTO hospitalRecipeDTO, DrugsEnterprise enterprise) {
        return null;
    }

    @Override
    public String getDrugInventory(Integer drugId, DrugsEnterprise drugsEnterprise, Integer organId) {
        return null;
    }

    @Override
    public DrugEnterpriseResult scanStock(Integer recipeId, DrugsEnterprise drugsEnterprise) {
        LOGGER.info("LmgyRemoteService.scanStock:[{}]", JSONUtils.toString(recipeId));
        String drugEpName = drugsEnterprise.getName();
        DrugEnterpriseResult result = DrugEnterpriseResult.getSuccess();
        Map<String,Object> param=new HashMap<>();
        Recipe recipe=recipeDAO.getByRecipeId(recipeId);
        if(recipe==null) return  new DrugEnterpriseResult(0);
        LOGGER.info("LmgyRemoteService.scanStock:[{}][{}]请求临沐国药调用库存校验，通过recipeid{}未获取到处方", drugsEnterprise.getId(), drugsEnterprise.getName(),recipeId);
        List<Recipedetail> recipedetails = recipeDetailDAO.findByRecipeId(recipeId);
        if(recipedetails==null||recipedetails.size()==0) return  new DrugEnterpriseResult(0);
        LOGGER.info("LmgyRemoteService.scanStock:[{}][{}]请求临沐国药调用库存校验，通过recipeid{}未获取到处方详情", drugsEnterprise.getId(), drugsEnterprise.getName(),recipeId);
        List<Map<String,Object>> paramList=new ArrayList<>();
        for (Recipedetail recipedetail : recipedetails) {
            //获取oraanDrugCode
            Map<String,Object> paramMap=new HashMap<>();
            List<Integer> drugIds=new ArrayList<>();
            drugIds.add(recipedetail.getDrugId());
            int organid=recipe.getClinicOrgan();
            List<SaleDrugList> saleDrugLists= saleDrugListDAO.findByOrganIdAndDrugIds(organid,drugIds);
            if(saleDrugLists==null||saleDrugLists.size()==0) return  new DrugEnterpriseResult(0);
            LOGGER.info("LmgyRemoteService.scanStock:[{}][{}]请求临沐国药调用库存校验，请求参数drugCode通过saleDrugListDAO.findByOrganIdAndDrugIds(organid{},drugIds{})未获取到值", drugsEnterprise.getId(), drugsEnterprise.getName(),organid,drugIds);
            paramMap.put("drugCode",saleDrugLists.get(0).getOrganDrugCode());
            paramMap.put("total",recipedetail.getUseTotalDose());
            paramMap.put("unit",recipedetail.getDrugUnit());
            paramList.add(paramMap);
        }
        param.put("accesstoken",drugsEnterprise.getToken());
        param.put("account",drugsEnterprise.getAccount());
        param.put("password",drugsEnterprise.getPassword());
        param.put("drugList",paramList);

        //请求临沐国药
        String requestStr = JSONUtils.toString(param);
        //String requestStr = "{\"accesstoken\":\"710F2F0A5778728AAB04EB3ED1842758\",\"account\":\"ngarihealth\",\"password\":\"202CB962AC59075B964B07152D234B70\",\"drugList\":[{\"drugCode\":\"z1211\",\"total\":\"3\",\"unit\":\"  盒\"},{\"drugCode\":\"z1212\",\"total\":\"3\",\"unit\":\"盒\"}]}";

        LOGGER.info("LmgyRemoteService.scanStock:[{}][{}]请求临沐国药调用库存校验，请求内容：{}", drugsEnterprise.getId(), drugsEnterprise.getName(), requestStr);

        String resultJson = null;
        try {
            Call call = getCall(drugsEnterprise, method);
            if (null != call) {
                call.addParameter(new QName(NAME_SPACE, "input"), Constants.XSD_STRING, ParameterMode.IN);
                call.setReturnType(Constants.XSD_STRING);
                Object resultObj;
                Object[] paramWsdl = {requestStr};
                resultObj = call.invoke(paramWsdl);
                if (null != resultObj && resultObj instanceof String) {
                    resultJson = resultObj.toString();
                    LOGGER.info("LmgyRemoteService.scanStock:[{}][{}]请求临沐国药调用库存校验，获取响应getBody消息：{}", drugsEnterprise.getId(), drugsEnterprise.getName(), resultJson);
                } else {
                    LOGGER.info("LmgyRemoteService.scanStock:[{}][{}]请求临沐国药调用库存校验，获取响应getBody消息空", drugsEnterprise.getId(), drugsEnterprise.getName());
                    result.setMsg(drugEpName + "接口返回结果为空");
                    result.setCode(DrugEnterpriseResult.FAIL);
                }
            }
        } catch (Exception e) {
            LOGGER.error(drugEpName + " invoke method[{}] error ", method, e);
            result.setMsg(drugEpName + "接口调用出错");
            result.setCode(DrugEnterpriseResult.FAIL);
        }
        //resultJson="{\"Code\":\"0\",\"Content\":\"数据已返回！\",\"Table\":[{\"drugcode\":\"z1211\",\"inventory\":\"0\"},{\"drugcode\":\"z1212\",\"inventory\":\"1\"}]}";
        if (StringUtils.isNotEmpty(resultJson)) {
            Map resultMap = JSONUtils.parse(resultJson, Map.class);
            String resCode = MapValueUtil.getString(resultMap, "Code");
            if (!RESULT_SUCCESS.equals(resCode)) {
                result.setMsg("调用[" + drugEpName + "][" + method + "]失败.error:" + MapValueUtil.getString(resultMap, "MSG"));
                result.setCode(DrugEnterpriseResult.FAIL);
                return result;
            }
            int code=1;//是否有库存 0：无  1：有
            List<Map<String, Object>> drugList = MapValueUtil.getList(resultMap, "Table");
            //一个处方对应多个药品，只要其中一个药品没库存就返回没库存 code:1
            if (CollectionUtils.isNotEmpty(drugList) && drugList.size() > 0) {
                for (Map<String, Object> drugBean : drugList) {
                    String inventory = MapValueUtil.getObject(drugBean, "inventory").toString();
                    if ("0".equals(inventory)) {
                        code=DrugEnterpriseResult.FAIL;
                        break;
                    }
                }
            } else {
                code=DrugEnterpriseResult.FAIL;
            }
            //result.setMsg(JSONUtils.toString(drugList));
            result.setCode(code);
        }
        return result;
    }

    */
/**
     * @method  getFailResult
     * @description 失败操作的结果对象
     * @date: 2019/7/10
     * @author: JRK
     * @param result 返回的结果集对象
     * @param msg 失败提示的信息
     * @return
     *//*

    private void getFailResult(DrugEnterpriseResult result, String msg) {
        result.setMsg(msg);
        result.setCode(DrugEnterpriseResult.FAIL);
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
    public DrugEnterpriseResult findSupportDep(List<Integer> recipeIds, Map ext, DrugsEnterprise enterprise) {
        return null;
    }

    @Override
    public String getDrugEnterpriseCallSys() {
        return DrugEnterpriseConstant.COMPANY_LMGY;
    }

    */
/**
     * 获取wsdl调用客户端
     *
     * @param drugsEnterprise
     * @param method
     * @return
     *//*

    protected Call getCall(DrugsEnterprise drugsEnterprise, String method) throws Exception {
        String wsdlUrl = drugsEnterprise.getBusinessUrl();
        String nameSpaceUri = NAME_SPACE + method;
        Call call = null;
        try {
            Service s = new Service();
            call = (Call) s.createCall();
            if (null != call) {
                //单位毫秒
                call.setTimeout(20000);
                call.setTargetEndpointAddress(new URL(wsdlUrl));
                call.setOperationName(new QName(NAME_SPACE, method));
                call.setSOAPActionURI(nameSpaceUri);
            }
        } catch (Exception e) {
            call = null;
            LOGGER.error("create call error. wsdlUrl={}, nameSpaceUri={}", wsdlUrl, nameSpaceUri, e);
        } finally {
            if(null == call){
                LOGGER.error("create call error finally. wsdlUrl={}, nameSpaceUri={}", wsdlUrl, nameSpaceUri);
            }
        }

        return call;
    }


}
*/
