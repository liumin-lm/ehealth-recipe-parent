package recipe.drugsenterprise;

import com.ngari.recipe.drugsenterprise.model.DepDetailBean;
import com.ngari.recipe.drugsenterprise.model.DrugsDataBean;
import com.ngari.recipe.drugsenterprise.model.Position;
import com.ngari.recipe.dto.DrugStockAmountDTO;
import com.ngari.recipe.entity.DrugsEnterprise;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.Recipedetail;
import com.ngari.recipe.entity.SaleDrugList;
import com.ngari.recipe.hisprescription.model.HospitalRecipeDTO;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import ctd.persistence.DAOFactory;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.axis.Constants;
import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.bean.DrugEnterpriseResult;
import recipe.constant.DrugEnterpriseConstant;
import recipe.dao.RecipeDAO;
import recipe.dao.RecipeDetailDAO;
import recipe.dao.SaleDrugListDAO;
import recipe.util.MapValueUtil;

import javax.xml.namespace.QName;
import javax.xml.rpc.ParameterMode;
import java.math.BigDecimal;
import java.net.URL;
import java.util.*;

/**
 * @author yinsheng
 * @date 2020\5\8 0008 15:08
 */
@RpcBean
public class LmgyRemoteService extends AccessDrugEnterpriseService {

    RecipeDetailDAO recipeDetailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);

    SaleDrugListDAO saleDrugListDAO = DAOFactory.getDAO(SaleDrugListDAO.class);

    RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);

    private static final Logger LOGGER = LoggerFactory.getLogger(LmgyRemoteService.class);

    private static String NAME_SPACE = "http://tempuri.org/";

    private static String RESULT_SUCCESS = "0";

    private static final String searchMapRANGE = "range";
    private static final String searchMapLatitude = "latitude";
    private static final String searchMapLongitude = "longitude";


    @Override
    public void tokenUpdateImpl(DrugsEnterprise drugsEnterprise) {

    }

    @Override
    public DrugEnterpriseResult pushRecipeInfo(List<Integer> recipeIds, DrugsEnterprise enterprise) {
        return DrugEnterpriseResult.getSuccess();
    }

    @RpcService
    @Override
    public DrugEnterpriseResult pushRecipe(HospitalRecipeDTO hospitalRecipeDTO, DrugsEnterprise enterprise) {
        return DrugEnterpriseResult.getSuccess();
    }

    @Override
    public String getDrugInventory(Integer drugId, DrugsEnterprise drugsEnterprise, Integer organId) {
        return "暂不支持库存查询";
    }

    @Override
    public List<String> getDrugInventoryForApp(DrugsDataBean drugsDataBean, DrugsEnterprise drugsEnterprise, Integer flag) {
        return null;
    }

    @Override
    public DrugStockAmountDTO scanEnterpriseDrugStock(Recipe recipe, DrugsEnterprise drugsEnterprise, List<Recipedetail> recipeDetails) {
        DrugStockAmountDTO drugStockAmountDTO = new DrugStockAmountDTO();
        if (null != recipe && null != recipe.getRecipeId()) {
            DrugEnterpriseResult drugEnterpriseResult = scanStock(recipe.getRecipeId(), drugsEnterprise);
            if (DrugEnterpriseResult.SUCCESS.equals(drugEnterpriseResult.getCode())) {
                drugStockAmountDTO.setResult(true);
            } else {
                drugStockAmountDTO.setResult(false);
            }
            return drugStockAmountDTO;
        }
        return super.scanEnterpriseDrugStock(recipe, drugsEnterprise, recipeDetails);
    }

    public DrugEnterpriseResult scanStock(Integer recipeId, DrugsEnterprise drugsEnterprise) {
        LOGGER.info("LmgyRemoteService.scanStock:[{}]", JSONUtils.toString(recipeId));
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
            SaleDrugList saleDrugList = saleDrugListDAO.getByDrugIdAndOrganId(recipedetail.getDrugId(), drugsEnterprise.getId());
            if(saleDrugList == null) return  DrugEnterpriseResult.getFail();
            LOGGER.info("LmgyRemoteService.scanStock:请求临沐国药调用库存校验，drugId{}", recipedetail.getDrugId());
            paramMap.put("drugCode",saleDrugList.getOrganDrugCode());
            paramMap.put("total",recipedetail.getUseTotalDose());
            paramMap.put("unit",recipedetail.getDrugUnit());
            paramList.add(paramMap);
        }
        param.put("accesstoken",drugsEnterprise.getToken());
        param.put("account",drugsEnterprise.getUserId());
        param.put("password",drugsEnterprise.getPassword());
        param.put("drugList",paramList);

        //表示为药店取药
        DrugEnterpriseResult drugEnterpriseResult = this.findSupportDep(Arrays.asList(recipeId), null, drugsEnterprise);
        if (drugEnterpriseResult.getCode() == 1 && drugEnterpriseResult.getObject() != null) {
            List<DepDetailBean> list = (List<DepDetailBean>)drugEnterpriseResult.getObject();
            for (DepDetailBean depDetailBean : list) {
                String pharmacyCode = depDetailBean.getPharmacyCode();
                param.put("hisid",pharmacyCode);
                result.setCode(DrugEnterpriseResult.SUCCESS);
                if (getScanResult(drugsEnterprise, result, param).getCode() == 1){
                    return result;
                }
            }
        }
        return result;
    }

    private DrugEnterpriseResult getScanResult(DrugsEnterprise drugsEnterprise, DrugEnterpriseResult result, Map<String, Object> param) {
        //请求临沐国药
        String requestStr = JSONUtils.toString(param);
        //String requestStr = "{\"accesstoken\":\"710F2F0A5778728AAB04EB3ED1842758\",\"account\":\"ngarihealth\",\"password\":\"202CB962AC59075B964B07152D234B70\",\"drugList\":[{\"drugCode\":\"z1211\",\"total\":\"3\",\"unit\":\"  盒\"},{\"drugCode\":\"z1212\",\"total\":\"3\",\"unit\":\"盒\"}]}";

        LOGGER.info("LmgyRemoteService.scanStock:[{}][{}]请求临沐国药调用库存校验，请求内容：{}", drugsEnterprise.getId(), drugsEnterprise.getName(), requestStr);
        String method = "ngarihealth_checkstock";
        String resultJson = getResultString(drugsEnterprise, result, requestStr, method);
        //resultJson="{\"Code\":\"0\",\"Content\":\"数据已返回！\",\"Table\":[{\"drugcode\":\"z1211\",\"inventory\":\"0\"},{\"drugcode\":\"z1212\",\"inventory\":\"1\"}]}";
        try{
            if (StringUtils.isNotEmpty(resultJson)) {
                Map resultMap = JSONUtils.parse(resultJson, Map.class);
                String resCode = MapValueUtil.getString(resultMap, "Code");
                if (!RESULT_SUCCESS.equals(resCode)) {
                    result.setMsg("调用[" + drugsEnterprise.getName() + "][" + method + "]失败.error:" + MapValueUtil.getString(resultMap, "MSG"));
                    result.setCode(DrugEnterpriseResult.FAIL);
                    return result;
                }
                int code=1;//是否有库存 0：无  1：有
                List<Map<String, Object>> drugList = MapValueUtil.getList(resultMap, "Table");
                //一个处方对应多个药品，只要其中一个药品没库存就返回没库存 code:1
                if (CollectionUtils.isNotEmpty(drugList) && drugList.size() > 0) {
                    for (Map<String, Object> drugBean : drugList) {
                        String inventory = MapValueUtil.getObject(drugBean, "inventory").toString();
                        if ("false".equals(inventory)) {
                            code = DrugEnterpriseResult.FAIL;
                            break;
                        }
                    }
                } else {
                    code = DrugEnterpriseResult.FAIL;
                }
                result.setCode(code);
            } else{
                result.setCode(DrugEnterpriseResult.FAIL);
            }
        }catch(Exception e){
            result.setCode(DrugEnterpriseResult.FAIL);
            LOGGER.error("getScanResult:异常: {}", e.getMessage(),e);
        }
        return result;
    }

    private String getResultString(DrugsEnterprise drugsEnterprise, DrugEnterpriseResult result, String requestStr, String method) {
        LOGGER.info("getResultString requestStr:{}.", requestStr);
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
                    LOGGER.info("LmgyRemoteService.getResultString:[{}][{}]请求临沐国药调用库存校验，获取响应getBody消息：{}", drugsEnterprise.getId(), drugsEnterprise.getName(), resultJson);
                } else {
                    LOGGER.info("LmgyRemoteService.getResultString:[{}][{}]请求临沐国药调用库存校验，获取响应getBody消息空", drugsEnterprise.getId(), drugsEnterprise.getName());
                    result.setMsg(drugsEnterprise.getName() + "接口返回结果为空");
                    result.setCode(DrugEnterpriseResult.FAIL);
                }
            }
        } catch (Exception e) {
            LOGGER.error(drugsEnterprise.getName() + " invoke method[{}] error ", method, e);
            result.setMsg(drugsEnterprise.getName() + "接口调用出错");
            result.setCode(DrugEnterpriseResult.FAIL);
        }
        return resultJson;
    }

    /**
     * @method  getFailResult
     * @description 失败操作的结果对象
     * @date: 2019/7/10
     * @author: JRK
     * @param result 返回的结果集对象
     * @param msg 失败提示的信息
     * @return
     */
    private void getFailResult(DrugEnterpriseResult result, String msg) {
        result.setMsg(msg);
        result.setCode(DrugEnterpriseResult.FAIL);
    }

    @Override
    public DrugEnterpriseResult syncEnterpriseDrug(DrugsEnterprise drugsEnterprise, List<Integer> drugIdList) {
        return DrugEnterpriseResult.getSuccess();
    }

    @Override
    public DrugEnterpriseResult pushCheckResult(Integer recipeId, Integer checkFlag, DrugsEnterprise enterprise) {
        return DrugEnterpriseResult.getSuccess();
    }

    @Override
    public DrugEnterpriseResult findSupportDep(List<Integer> recipeIds, Map ext, DrugsEnterprise enterprise) {
        LOGGER.info("LmgyRemoteService.findSupportDep:[{}]", JSONUtils.toString(recipeIds.get(0)));
        //查询药店列表
        String method = "ngarihealth_shoplist";
        Integer recipeId = recipeIds.get(0);
        String drugEpName = enterprise.getName();
        DrugEnterpriseResult result = DrugEnterpriseResult.getSuccess();
        Map<String,Object> param=new HashMap<>();
        Recipe recipe=recipeDAO.getByRecipeId(recipeIds.get(0));
        if(recipe==null) return  new DrugEnterpriseResult(0);
        LOGGER.info("LmgyRemoteService.findSupportDep:[{}][{}]请求临沐国药查询药店列表，通过recipeid{}未获取到处方", enterprise.getId(), drugEpName, recipeId);
        List<Recipedetail> recipedetails = recipeDetailDAO.findByRecipeId(recipeId);
        if(recipedetails==null||recipedetails.size()==0) return  new DrugEnterpriseResult(0);
        LOGGER.info("LmgyRemoteService.findSupportDep:[{}][{}]请求临沐国药查询药店列表，通过recipeid{}未获取到处方详情", enterprise.getId(), drugEpName, recipeId);
        List<Map<String,Object>> paramList=new ArrayList<>();
        for (Recipedetail recipedetail : recipedetails) {
            //获取oraanDrugCode
            Map<String,Object> paramMap=new HashMap<>();
            SaleDrugList saleDrugList = saleDrugListDAO.getByDrugIdAndOrganId(recipedetail.getDrugId(), enterprise.getId());
            if(saleDrugList == null) return  DrugEnterpriseResult.getFail();
            LOGGER.info("LmgyRemoteService.findSupportDep:请求临沐国药查询药店列表，drugId{}", recipedetail.getDrugId());
            paramMap.put("drugCode",saleDrugList.getOrganDrugCode());
            paramMap.put("total",recipedetail.getUseTotalDose());
            paramMap.put("unit",recipedetail.getDrugUnit());
            paramList.add(paramMap);
        }
        param.put("accesstoken", enterprise.getToken());
        param.put("account", enterprise.getUserId());
        param.put("password", enterprise.getPassword());
        param.put("drugList", paramList);
        if (ext != null && null != ext.get(searchMapRANGE) && null != ext.get(searchMapLongitude) && null != ext.get(searchMapLatitude)) {
            param.put("longitude", MapValueUtil.getString(ext, searchMapLongitude));
            param.put("latitude", MapValueUtil.getString(ext, searchMapLatitude));
            if (enterprise.getSort() == 99) {
                param.put("range", "2000");
            } else {
                param.put("range", MapValueUtil.getString(ext, searchMapRANGE));
            }
        } else {
            //医生开处方
            param.put("longitude", "117.085419");
            param.put("latitude", "36.632060");
            param.put("range", "1000");
        }

        //请求临沐国药
        String requestStr = JSONUtils.toString(param);
        String resultJson = getResultString(enterprise, result, requestStr, method);
        if (StringUtils.isNotEmpty(resultJson)) {
            Map resultMap = JSONUtils.parse(resultJson, Map.class);
            String resCode = MapValueUtil.getString(resultMap, "Code");
            if (!RESULT_SUCCESS.equals(resCode)) {
                result.setMsg("调用[" + drugEpName + "][" + method + "]失败.error:" + MapValueUtil.getString(resultMap, "MSG"));
                result.setCode(DrugEnterpriseResult.FAIL);
                return result;
            }
            int code = 1;
            List<Map<String, Object>> storeBeanList = MapValueUtil.getList(resultMap, "Table");
            //一个处方对应多个药品，只要其中一个药品没库存就返回没库存 code:1
            DepDetailBean detailBean;
            List<DepDetailBean> list = new ArrayList<>();
            if (CollectionUtils.isNotEmpty(storeBeanList) && storeBeanList.size() > 0) {
                for (Map<String, Object> storeBean : storeBeanList) {
                    detailBean = new DepDetailBean();
                    detailBean.setPharmacyCode(MapValueUtil.getString(storeBean, "pharmacycode"));
                    detailBean.setDepName(MapValueUtil.getString(storeBean, "pharmacyname"));
                    detailBean.setBelongDepName(enterprise.getName());
                    detailBean.setAddress(MapValueUtil.getString(storeBean, "address"));
                    if (StringUtils.isNotEmpty(MapValueUtil.getString(storeBean, "recipefee"))) {
                        detailBean.setRecipeFee(new BigDecimal(MapValueUtil.getString(storeBean, "recipefee")));
                    }
                    detailBean.setDistance(Double.parseDouble(MapValueUtil.getString(storeBean, "distance")));
                    Position postion = new Position();
                    postion.setLatitude(Double.parseDouble(MapValueUtil.getString(storeBean, "latitude")));
                    postion.setLongitude(Double.parseDouble(MapValueUtil.getString(storeBean, "longitude")));
                    detailBean.setPosition(postion);
                    list.add(detailBean);
                }
            } else {
                code = DrugEnterpriseResult.FAIL;
            }
            result.setObject(list);
            result.setCode(code);
        }
        return result;
    }

    @Override
    public String getDrugEnterpriseCallSys() {
        return DrugEnterpriseConstant.COMPANY_LMGY;
    }

    /**
     * 获取wsdl调用客户端
     *
     * @param drugsEnterprise
     * @param method
     * @return
     */
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
