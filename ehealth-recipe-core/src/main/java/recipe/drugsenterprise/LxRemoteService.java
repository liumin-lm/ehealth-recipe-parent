package recipe.drugsenterprise;

import com.ngari.patient.dto.OrganDTO;
import com.ngari.patient.service.BasicAPI;
import com.ngari.patient.service.OrganService;
import com.ngari.recipe.drugsenterprise.model.DepDetailBean;
import com.ngari.recipe.drugsenterprise.model.DrugsDataBean;
import com.ngari.recipe.drugsenterprise.model.Position;
import com.ngari.recipe.dto.DrugInfoDTO;
import com.ngari.recipe.dto.DrugStockAmountDTO;
import com.ngari.recipe.entity.*;
import com.ngari.recipe.hisprescription.model.HospitalRecipeDTO;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import ctd.persistence.DAOFactory;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.bean.DrugEnterpriseResult;
import recipe.constant.DrugEnterpriseConstant;
import recipe.dao.*;
import recipe.drugsenterprise.bean.HdDrugRequestData;
import recipe.drugsenterprise.bean.HdPosition;
import recipe.drugsenterprise.bean.YnsPharmacyAndStockRequest;
import recipe.drugsenterprise.bean.yd.httpclient.HttpsClientUtils;
import recipe.util.MapValueUtil;

import java.util.*;
import java.util.stream.Collectors;

/**
* @Description: LxRemoteService 类（或接口）是 对接云南省医药服务接口
* @Author: JRK
* @Date: 2019/7/8
*/
@RpcBean("lxRemoteService")
public class LxRemoteService extends AccessDrugEnterpriseService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LxRemoteService.class);

    private static final String searchMapRANGE = "range";
    private static final String searchMapLatitude = "latitude";
    private static final String searchMapLongitude = "longitude";
    private static final String drugRecipeTotal = "0";
    private static final String requestSuccessCode = "000";
    private static final String requestHeadJsonValue = "application/json";
    @Autowired
    private DrugListDAO drugListDAO;
    @Autowired
    private SaleDrugListDAO saleDrugListDAO;
    @Override
    public void tokenUpdateImpl(DrugsEnterprise drugsEnterprise) {

    }

    @Override
    public DrugEnterpriseResult pushRecipeInfo(List<Integer> recipeIds, DrugsEnterprise enterprise) {
        return DrugEnterpriseResult.getSuccess();
    }

    @Override
    public DrugEnterpriseResult pushRecipe(HospitalRecipeDTO hospitalRecipeDTO, DrugsEnterprise enterprise) {
        return DrugEnterpriseResult.getSuccess();
    }

    @RpcService
    public DrugStockAmountDTO test(){
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        RecipeDetailDAO recipeDetailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
        Recipe recipe = recipeDAO.getByRecipeId(136534);
        List<Recipedetail> recipedetails = recipeDetailDAO.findByRecipeId(136534);
        DrugsEnterprise drugsEnterprise = new DrugsEnterprise();
        drugsEnterprise.setId(3052);
        drugsEnterprise.setUserId("dplx5274155892");
        drugsEnterprise.setPassword("329a4ff3a17c7a280bb2b6175");
        drugsEnterprise.setToken("747e9801-111b-400a-a3fa-3986698df417");
        drugsEnterprise.setAuthenUrl("http://rx.eastpharm.com:8082/prescription/oauth/token");
        drugsEnterprise.setBusinessUrl("http://305l5120d3.wicp.vip/dp-ws/");
        return scanEnterpriseDrugStock(recipe,drugsEnterprise,recipedetails);
    }

    @Override
    public DrugStockAmountDTO scanEnterpriseDrugStock(Recipe recipe, DrugsEnterprise drugsEnterprise, List<Recipedetail> recipeDetails) {
        DrugStockAmountDTO drugStockAmountDTO = new DrugStockAmountDTO();
        List<Integer> drugList = recipeDetails.stream().map(Recipedetail::getDrugId).distinct().collect(Collectors.toList());
        List<SaleDrugList> saleDrugLists = saleDrugListDAO.findByOrganIdAndDrugIds(drugsEnterprise.getId(), drugList);
        Map<Integer,SaleDrugList> saleDrugListMap = saleDrugLists.stream().collect(Collectors.toMap(SaleDrugList::getDrugId,a->a,(k1,k2)->k1));
        List<Map<String, String>> result = findAllDrugInventory(drugsEnterprise, recipeDetails, drugList, saleDrugListMap);
        Map<String,String> inventoryMap = new HashMap<>();
        try {
            if (CollectionUtils.isNotEmpty(result)) {
                for (Map<String, String> drugBean : result) {
                    String inventory = MapValueUtil.getString(drugBean, "inventory");
                    String drugCode = MapValueUtil.getString(drugBean, "drugCode");
                    if ("true".equals(inventory)) {
                        inventoryMap.put(drugCode, "有库存");
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("scanEnterpriseDrugStock error", e);
        }
        drugStockAmountDTO.setResult(true);
        List<DrugInfoDTO> drugInfoList = new LinkedList<>();
        recipeDetails.forEach(recipeDetail -> {
            DrugInfoDTO drugInfoDTO = new DrugInfoDTO();
            BeanUtils.copyProperties(recipeDetail, drugInfoDTO);
            SaleDrugList saleDrugList = saleDrugListMap.get(recipeDetail.getDrugId());
            if (null != saleDrugList && saleDrugList.getStatus() == 1) {
                drugInfoDTO.setStock("有库存".equals(inventoryMap.get(saleDrugList.getOrganDrugCode())));
                drugInfoDTO.setStockAmountChin(drugInfoDTO.getStock()?"有库存":"无库存");
            } else {
                drugInfoDTO.setStock(false);
                drugInfoDTO.setStockAmountChin("无库存");
            }
            drugInfoList.add(drugInfoDTO);
        });
        List<String> noDrugNames = drugInfoList.stream().filter(drugInfoDTO -> !drugInfoDTO.getStock()).map(DrugInfoDTO::getDrugName).collect(Collectors.toList());
        drugStockAmountDTO.setResult(true);
        if (CollectionUtils.isNotEmpty(noDrugNames)) {
            drugStockAmountDTO.setNotDrugNames(noDrugNames);
            drugStockAmountDTO.setResult(false);
        }
        drugStockAmountDTO.setDrugInfoList(drugInfoList);
        return drugStockAmountDTO;
    }

    private List<Map<String, String>> findAllDrugInventory(DrugsEnterprise drugsEnterprise, List<Recipedetail> recipeDetails, List<Integer> drugList, Map<Integer, SaleDrugList> saleDrugListMap){
        List<DrugList> drugLists = drugListDAO.findByDrugIds(drugList);
        Map<Integer, String> unitMap = drugLists.stream().collect(Collectors.toMap(DrugList::getDrugId,DrugList::getUnit));
        List<HdDrugRequestData> requestList = new ArrayList<>();
        recipeDetails.forEach(recipeDetail -> {
            HdDrugRequestData drugBean = new HdDrugRequestData();
            SaleDrugList saleDrugList = saleDrugListMap.get(recipeDetail.getDrugId());
            if (null != saleDrugList && StringUtils.isNotEmpty(saleDrugList.getOrganDrugCode())) {
                drugBean.setDrugCode(saleDrugList.getOrganDrugCode());
            } else {
                drugBean.setDrugCode(recipeDetail.getDrugId().toString());
            }
            drugBean.setTotal(recipeDetail.getUseTotalDose().toString());
            drugBean.setUnit(unitMap.get(recipeDetail.getDrugId()));
            requestList.add(drugBean);
        });
        Map<String, List<HdDrugRequestData>> map = new HashMap<>();
        map.put("drugList", requestList);
        String requestJson = JSONUtils.toString(map);
        LOGGER.info("findAllDrugInventory requestJson:{}", requestJson);
        Map<String, String> extendHeaders = new HashMap<String, String>();
        extendHeaders.put("Content-Type", requestHeadJsonValue);
        extendHeaders.put("X-Token", getToken(drugsEnterprise));
        try{
            String stockData = HttpsClientUtils.doPost(drugsEnterprise.getBusinessUrl() + "sys/drugstore/checkstock", requestJson, extendHeaders);
            LOGGER.info("findAllDrugInventory stockData:{}", stockData);
            Map resultMap = JSONUtils.parse(stockData, Map.class);
            if (requestSuccessCode.equals(MapValueUtil.getString(resultMap, "code"))) {
                List<Map<String, String>> result = MapValueUtil.getList(resultMap, "drugList");
                return result;
            }
        }catch (Exception e){
            LOGGER.error("findAllDrugInventory error", e);
        }
        return null;
    }

    private String getToken(DrugsEnterprise drugsEnterprise){
        String loginUrl = "sys/login";
        Map<String, String> loginBody = new HashMap<>();
        loginBody.put("username", drugsEnterprise.getUserId());
        loginBody.put("password", drugsEnterprise.getPassword());
        LOGGER.info("getToken loginBody:{}", JSONUtils.toString(loginBody));
        try{
            String tokenData = HttpsClientUtils.doPost(drugsEnterprise.getBusinessUrl() + loginUrl, JSONUtils.toString(loginBody));
            LOGGER.info("getToken tokenData:{}.", JSONUtils.toString(tokenData));
            Map tokenMap = JSONUtils.parse(tokenData, Map.class);
            if (requestSuccessCode.equals(MapValueUtil.getString(tokenMap, "code"))) {
                String token = MapValueUtil.getString(tokenMap, "token");
                LOGGER.info("getToken token:{}.", token);
                return token;
            }
        }catch (Exception e){
            LOGGER.error("getToken error", e);
        }
        return "";
    }

    @Override
    public String getDrugInventory(Integer drugId, DrugsEnterprise drugsEnterprise, Integer organId) {
        RecipeParameterDao recipeParameterDao = DAOFactory.getDAO(RecipeParameterDao.class);
        String loginUrl=recipeParameterDao.getByName("lx-login-url");
        String checkstockUrl=recipeParameterDao.getByName("lx-checkstock-url");
        SaleDrugListDAO saleDrugListDAO = DAOFactory.getDAO(SaleDrugListDAO.class);
        OrganDrugListDAO organDrugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);
        try {
            Map<String, String> loginBody = new HashMap<String, String>();
            loginBody.put("username", drugsEnterprise.getUserId());
            loginBody.put("password", drugsEnterprise.getPassword());
            String requestStr = JSONUtils.toString(loginBody);
            LOGGER.info("LxRemoteService.scanStock:[{}][{}]根据用户名和密码获取Token请求，请求内容：{}", drugsEnterprise.getId(), drugsEnterprise.getName(), requestStr);
            String tokenData = HttpsClientUtils.doPost(drugsEnterprise.getBusinessUrl() + loginUrl, requestStr);
            LOGGER.info("LxRemoteService.scanStock:[{}][{}]根据用户名和密码获取Token请求，获取响应消息：{}", drugsEnterprise.getId(), drugsEnterprise.getName(), JSONUtils.toString(tokenData));
            Map tokenMap = JSONUtils.parse(tokenData, Map.class);
            if (requestSuccessCode.equals(MapValueUtil.getString(tokenMap, "code"))) {
                String token = MapValueUtil.getString(tokenMap, "token");
                Map<String, String> extendHeaders = new HashMap<String, String>();
                extendHeaders.put("Content-Type", requestHeadJsonValue);
                extendHeaders.put("X-Token", token);
                /**
                 * {"drugList":[{"drugCode":"SPH00001154","total":"1","unit":"盒"}]}
                 */
                SaleDrugList saleDrugList = saleDrugListDAO.getByDrugIdAndOrganId(drugId, drugsEnterprise.getId());
                List<OrganDrugList> organDrugLists = organDrugListDAO.findByDrugIdAndOrganId(drugId, organId);
                List<HdDrugRequestData> list = new ArrayList<>();
                if (saleDrugList != null) {
                    HdDrugRequestData drugBean = new HdDrugRequestData();
                    drugBean.setDrugCode(saleDrugList.getOrganDrugCode());
                    drugBean.setTotal("5");
                    if (CollectionUtils.isNotEmpty(organDrugLists)) {
                        drugBean.setUnit(organDrugLists.get(0).getUnit());
                    }
                    list.add(drugBean);
                }
                Map<String, List<HdDrugRequestData>> map = new HashMap<>();
                map.put("drugList", list);
                String requestParames = JSONUtils.toString(map);
                String stockData = HttpsClientUtils.doPost(drugsEnterprise.getBusinessUrl() + checkstockUrl, requestParames, extendHeaders);
                LOGGER.info("LxRemoteService.scanStock:[{}][{}]获取药企库存查询请求，获取响应getBody消息：{}", drugsEnterprise.getId(), drugsEnterprise.getName(), stockData);
                Map resultMap = JSONUtils.parse(stockData, Map.class);
                if (requestSuccessCode.equals(MapValueUtil.getString(resultMap, "code"))) {
                    List<Map<String, Object>> drugList = MapValueUtil.getList(resultMap, "drugList");
                    if (CollectionUtils.isNotEmpty(drugList) && drugList.size() > 0) {
                        for (Map<String, Object> drugBean : drugList) {
                            String inventory = MapValueUtil.getObject(drugBean, "inventory").toString();
                            if ("false".equals(inventory)) {
                                return "无库存";
                            } else {
                                return "有库存";
                            }
                        }
                    } else {
                        return "无库存";
                    }
                }
            }
        }catch (Exception e){
            LOGGER.info("getDrugInventory error:{}.", e.getMessage(), e);
            return "无库存";
        }
        return "暂不支持库存查询";
    }

    @Override
    public List<String> getDrugInventoryForApp(DrugsDataBean drugsDataBean, DrugsEnterprise drugsEnterprise, Integer flag) {
        return null;
    }

    @RpcService
    public void test(Integer recipeId){
        List<Integer> recipeIds = Arrays.asList(recipeId);
        DrugsEnterpriseDAO enterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        DrugsEnterprise drugsEnterprise = enterpriseDAO.getById(242);
        Map ext=new HashMap();
        ext.put("longitude","34.975056");
        ext.put("latitude","118.269448");
        ext.put("range","20");
//        findSupportDep(recipeIds,ext,drugsEnterprise);
        scanStock(recipeId,drugsEnterprise);
    }
    @Override
    public DrugEnterpriseResult scanStock(Integer recipeId, DrugsEnterprise drugsEnterprise) {
        LOGGER.info("LxRemoteService.scanStock:[{}]", JSONUtils.toString(recipeId));
        DrugEnterpriseResult result = DrugEnterpriseResult.getSuccess();
        RecipeParameterDao recipeParameterDao = DAOFactory.getDAO(RecipeParameterDao.class);
        String loginUrl=recipeParameterDao.getByName("lx-login-url");
        String checkstockUrl=recipeParameterDao.getByName("lx-checkstock-url");
        try {
            Map<String,String> loginBody=new HashMap<String,String>();
            loginBody.put("username",drugsEnterprise.getUserId());
            loginBody.put("password",drugsEnterprise.getPassword());
            String requestStr = JSONUtils.toString(loginBody);
            LOGGER.info("LxRemoteService.scanStock:[{}][{}]根据用户名和密码获取Token请求，请求内容：{}", drugsEnterprise.getId(), drugsEnterprise.getName(), requestStr);
            String tokenData = HttpsClientUtils.doPost(drugsEnterprise.getBusinessUrl()+loginUrl, requestStr);
            LOGGER.info("LxRemoteService.scanStock:[{}][{}]根据用户名和密码获取Token请求，获取响应消息：{}", drugsEnterprise.getId(), drugsEnterprise.getName(), JSONUtils.toString(tokenData));
            Map tokenMap = JSONUtils.parse(tokenData, Map.class);
            if(requestSuccessCode.equals(MapValueUtil.getString(tokenMap, "code"))) {
                String token = MapValueUtil.getString(tokenMap, "token");
                Map<String,String> extendHeaders=new HashMap<String,String>();
                extendHeaders.put("Content-Type",requestHeadJsonValue);
                extendHeaders.put("X-Token",token);
                YnsPharmacyAndStockRequest stockRequest=findScanStockBussReq(result,recipeId, drugsEnterprise);
                if (stockRequest == null) {
                    getFailResult(result, "库存不足");
                    return result;
                }
                String bodyStr = JSONUtils.toString(stockRequest);

                ////根据处方信息发送药企库存查询请求，判断有药店是否满足库存
                LOGGER.info("LxRemoteService.scanStock:[{}][{}]根据处方信息发送药企库存查询请求，请求内容：{}", drugsEnterprise.getId(), drugsEnterprise.getName(), bodyStr);
                String stockData = HttpsClientUtils.doPost(drugsEnterprise.getBusinessUrl()+checkstockUrl, bodyStr,extendHeaders);
                LOGGER.info("LxRemoteService.scanStock:[{}][{}]获取药企库存查询请求，获取响应getBody消息：{}", drugsEnterprise.getId(), drugsEnterprise.getName(), stockData);
                Map resultMap = JSONUtils.parse(stockData, Map.class);
                if (requestSuccessCode.equals(MapValueUtil.getString(resultMap, "code"))) {
                    List<Map<String, Object>> drugList = MapValueUtil.getList(resultMap,"drugList");
                    if (CollectionUtils.isNotEmpty(drugList) && drugList.size() > 0) {
                        for (Map<String, Object> drugBean : drugList) {
                            String inventory = MapValueUtil.getObject(drugBean, "inventory").toString();
                            if ("false".equals(inventory)) {
                                getFailResult(result, "当前药企下没有药店的药品库存足够");
                                return result;
                            }
                        }
                        LOGGER.info("LxRemoteService.findSupportDep:[{}][{}]获取药品库存请求，返回前端result消息：{}", drugsEnterprise.getId(), drugsEnterprise.getName(), JSONUtils.toString(result));
                    } else {
                        getFailResult(result, "当前药企下没有药店的药品库存足够");
                    }
                    LOGGER.info("LxRemoteService.findSupportDep:[{}][{}]获取药店列表请求，返回前端result消息：{}", drugsEnterprise.getId(), drugsEnterprise.getName(), JSONUtils.toString(result));
                }else{
                    getFailResult(result, "当前药企下没有药店的药品库存足够");
                }
                LOGGER.info("LxRemoteService-scanStock 药店取药药品库存:{}.", JSONUtils.toString(result));
            }else{
                result.setCode(DrugEnterpriseResult.FAIL);
                result.setMsg("获取Token失败");
                LOGGER.error("LxRemoteService.findSupportDep: msg [{}][{}]登录罗欣药企获取Token：{}",drugsEnterprise.getId(), drugsEnterprise.getName(), "获取Token失败");
                getFailResult(result, "获取Token失败");
                return result;
            }
        } catch (Exception e) {
                result.setCode(DrugEnterpriseResult.FAIL);
                result.setMsg(e.getMessage());
                LOGGER.error("LxRemoteService.scanStock:[{}][{}]获取药品库存异常：{}",drugsEnterprise.getId(), drugsEnterprise.getName(), e.getMessage(),e);
                getFailResult(result,  e.getMessage());
                return result;
            } finally {
                try {
                } catch (Exception e) {
                    result.setCode(DrugEnterpriseResult.FAIL);
                    result.setMsg(e.getMessage());
                    getFailResult(result,  e.getMessage());
                    LOGGER.error("LxRemoteService.scanStock:http请求资源关闭异常: {}！", e.getMessage(),e);
                    return result;
                }
            }
            return result;
    }
    private YnsPharmacyAndStockRequest findScanStockBussReq(DrugEnterpriseResult result ,Integer recipeId, DrugsEnterprise drugsEnterprise){
        LOGGER.info("LxRemoteService.findScanStockBussReq:[{}][{}]获取药企库存查询请求，请求内容：{}", drugsEnterprise.getId(), drugsEnterprise.getName(), recipeId);
        //查询当前处方信息
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        Recipe nowRecipe = recipeDAO.get(recipeId);
        //查询当前处方下详情信息
        RecipeDetailDAO detailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
        List<Recipedetail> detailList = detailDAO.findByRecipeId(nowRecipe.getRecipeId());

        //根据药品请求华东旗下的所有可用药店，当有一个可用说明库存是足够的
        Map<String, HdDrugRequestData> resultMap = new HashMap<>();
        List<HdDrugRequestData> drugRequestDataList = getDrugRequestList(resultMap, detailList, drugsEnterprise, result);
        if(DrugEnterpriseResult.FAIL == result.getCode()) return null;
        YnsPharmacyAndStockRequest hdPharmacyAndStockRequest = new YnsPharmacyAndStockRequest();
        hdPharmacyAndStockRequest.setDrugList(drugRequestDataList);
        return hdPharmacyAndStockRequest;
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
        LOGGER.info("LxRemoteService.findSupportDep:[{}]", JSONUtils.toString(recipeIds));
        DrugEnterpriseResult result = DrugEnterpriseResult.getSuccess();
        RecipeParameterDao recipeParameterDao = DAOFactory.getDAO(RecipeParameterDao.class);
        String loginUrl=recipeParameterDao.getByName("lx-login-url");
        String storelistUrl=recipeParameterDao.getByName("lx-storelist-url");
        try {
            Map<String,String> loginBody=new HashMap<String,String>();
            loginBody.put("username",enterprise.getUserId());
            loginBody.put("password",enterprise.getPassword());
            String requestStr = JSONUtils.toString(loginBody);
            LOGGER.info("LxRemoteService.findSupportDep:[{}][{}]根据用户名和密码获取Token请求，请求内容：{}", enterprise.getId(), enterprise.getName(), requestStr);
            String tokenData = HttpsClientUtils.doPost(enterprise.getBusinessUrl()+loginUrl, requestStr);
            LOGGER.info("LxRemoteService.findSupportDep:[{}][{}]根据用户名和密码获取Token请求，获取响应消息：{}", enterprise.getId(), enterprise.getName(), JSONUtils.toString(tokenData));
            Map tokenMap = JSONUtils.parse(tokenData, Map.class);
            if(requestSuccessCode.equals(MapValueUtil.getString(tokenMap, "code"))) {
                String token=MapValueUtil.getString(tokenMap, "token");
                Map<String,String> extendHeaders=new HashMap<String,String>();
                extendHeaders.put("Content-Type",requestHeadJsonValue);
                extendHeaders.put("X-Token",token);
                YnsPharmacyAndStockRequest hdPharmacyAndStockRequest=findSupportDepBussReq(result,recipeIds,ext,enterprise);
                String bodyStr = JSONUtils.toString(hdPharmacyAndStockRequest);

                ////根据处方信息发送药企库存查询请求，判断有药店是否满足库存
                LOGGER.info("LxRemoteService.scanStock:[{}][{}]根据处方信息发送药企库存列表请求，请求内容：{}", enterprise.getId(), enterprise.getName(), bodyStr);
                String stockData = HttpsClientUtils.doPost(enterprise.getBusinessUrl()+storelistUrl, bodyStr,extendHeaders);
                LOGGER.info("LxRemoteService.scanStock:[{}][{}]获取药企列表查询请求，获取响应getBody消息：{}", enterprise.getId(), enterprise.getName(), JSONUtils.toString(stockData));
                Map resultMap = JSONUtils.parse(stockData, Map.class);
                if(requestSuccessCode.equals(MapValueUtil.getString(resultMap, "code"))) {
                    //接口返回的结果
                    List<Map<String, Object>> ynsStoreBeans = MapValueUtil.getList(resultMap, "list");
                    //数据封装成页面展示数据
                    List<DepDetailBean> list=new ArrayList<>();
                    DepDetailBean detailBean;
                    for (Map<String, Object> ynsStoreBean : ynsStoreBeans) {
                        LOGGER.info("LxRemoteService.findSupportDep lxStoreBean:{}.", JSONUtils.toString(ynsStoreBean));
                        detailBean=new DepDetailBean();
                        detailBean.setPharmacyCode(MapValueUtil.getString(ynsStoreBean, "pharmacyCode"));
                        detailBean.setDepName(MapValueUtil.getString(ynsStoreBean, "pharmacyName"));
                        detailBean.setAddress(MapValueUtil.getString(ynsStoreBean, "address"));
                        detailBean.setDistance(Double.parseDouble(MapValueUtil.getString(ynsStoreBean, "distance")));
                        LOGGER.info("LxRemoteService.findSupportDep pharmacyCode:{}.",MapValueUtil.getString(ynsStoreBean, "pharmacyCode") );
                        Map position=  (Map)MapValueUtil.getObject(ynsStoreBean, "position");
                        LOGGER.info("LxRemoteService.findSupportDep position:{}.", JSONUtils.toString(position));
                        //Map mp = JSONUtils.parse(position, Map.class);
                        //LOGGER.info("LxRemoteService.findSupportDep mp:{}.", JSONUtils.toString(position));
                        Position postion=new Position();
                        postion.setLatitude(Double.parseDouble(MapValueUtil.getString(position, "latitude")));
                        postion.setLongitude(Double.parseDouble(MapValueUtil.getString(position, "longitude")));
                        detailBean.setPosition(postion);
                        list.add(detailBean);
                    }
                    result.setObject(list);
                    LOGGER.info("LxRemoteService.findSupportDep:[{}][{}]获取药店列表请求，返回前端result消息：{}", enterprise.getId(), enterprise.getName(), JSONUtils.toString(result));
                }else {
                    String responseData = MapValueUtil.getString(resultMap, "msg");
                    result.setCode(DrugEnterpriseResult.FAIL);
                    result.setMsg(responseData);
                    LOGGER.error("LxRemoteService.findSupportDep: msg [{}][{}]获取药店列表异常：{}",enterprise.getId(), enterprise.getName(), responseData);
                    getFailResult(result, responseData);
                    return null;
                }
            }else{
                result.setCode(DrugEnterpriseResult.FAIL);
                result.setMsg("获取Token失败");
                LOGGER.error("LxRemoteService.findSupportDep: msg [{}][{}]登录罗欣药企获取Token：{}",enterprise.getId(), enterprise.getName(), "获取Token失败");
                getFailResult(result, "获取Token失败");
                return null;
            }

        } catch (Exception e) {
            result.setCode(DrugEnterpriseResult.FAIL);
            result.setMsg(e.getMessage());
            LOGGER.error("LxRemoteService.findSupportDep:[{}][{}]获取药店列表异常：{}",enterprise.getId(), enterprise.getName(), e.getMessage(),e);
            getFailResult(result,  e.getMessage());
        } finally {
            try {
            } catch (Exception e) {
                result.setCode(DrugEnterpriseResult.FAIL);
                result.setMsg(e.getMessage());
                getFailResult(result,  e.getMessage());
                LOGGER.error("LxRemoteService.findSupportDep:http请求资源关闭异常: {}！", e.getMessage(),e);
            }
        }
        return result;
    }
    private YnsPharmacyAndStockRequest findSupportDepBussReq(DrugEnterpriseResult result ,List<Integer> recipeIds, Map ext, DrugsEnterprise enterprise){
        LOGGER.info("LxRemoteService.findSupportDepBussReq:[{}][{}]获取药店列表请求，请求内容：{}", enterprise.getId(), enterprise.getName(), recipeIds);
        RecipeDetailDAO detailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
        //现在默认只有一个处方单
        List<Recipedetail> detailList = detailDAO.findByRecipeId(recipeIds.get(0));
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        if(CollectionUtils.isEmpty(detailList)){
            LOGGER.warn("LxRemoteService.findSupportDep:处方单{}的细节信息为空", recipeIds.get(0));

        }
        Recipe nowRecipe = recipeDAO.get(recipeIds.get(0));
        if (null == nowRecipe) {
            LOGGER.warn("LxRemoteService.findSupportDep:处方单{}不存在", recipeIds.get(0));

        }
        OrganService organService = BasicAPI.getService(OrganService.class);
        OrganDTO organ = organService.getByOrganId(nowRecipe.getClinicOrgan());
        if(null == organ){
            LOGGER.warn("LxRemoteService.findSupportDep:处方ID为{},对应的开处方机构不存在.", nowRecipe.getRecipeId());
        }
        //根据请求返回的药店列表，获取对应药店下药品需求的总价格
        Map<String, HdDrugRequestData> resultMap = new HashMap<>();
        //首先组装请求对象
        YnsPharmacyAndStockRequest hdPharmacyAndStockRequest = new YnsPharmacyAndStockRequest();
        LOGGER.info("LxRemoteService.findSupportDepBussReq:[{}][{}]获取药店列表请求，请求内容：{}", enterprise.getId(), enterprise.getName(), "判断药店的坐标地址是否正确");
        if (ext != null && null!= ext.get(searchMapRANGE) && null!= ext.get(searchMapLongitude) && null!= ext.get(searchMapLatitude)) {
            List<HdDrugRequestData> drugRequestList = getDrugRequestList(resultMap, detailList, enterprise, result);
            if(DrugEnterpriseResult.FAIL == result.getCode()) return null;
            hdPharmacyAndStockRequest.setDrugList(drugRequestList);
            hdPharmacyAndStockRequest.setRange("50");
            hdPharmacyAndStockRequest.setPosition(new HdPosition(MapValueUtil.getString(ext, searchMapLongitude), MapValueUtil.getString(ext, searchMapLatitude)));

        }else{
            //LOGGER.warn("LxRemoteService.findSupportDep:请求的搜索参数不健全" );
            //配送到家的信息
            List<HdDrugRequestData> drugRequestList = getDrugRequestList(resultMap, detailList, enterprise, result);
            hdPharmacyAndStockRequest.setDrugList(drugRequestList);
        }

        return hdPharmacyAndStockRequest;
    }

    /**
     * @method  getDrugRequestList
     * @description 获取请求药店下药品信息接口下的药品总量（根据药品的code分组）的list
     * @date: 2019/7/25
     * @author: JRK
     * @param detailList 处方下的详情列表
     * @param  finalResult 请求的最终结果
     * @return java.util.List<recipe.drugsenterprise.bean.HdDrugRequestData>请求药店下药品信息列表
     */
    private List<HdDrugRequestData> getDrugRequestList(Map<String, HdDrugRequestData> result, List<Recipedetail> detailList, DrugsEnterprise drugsEnterprise, DrugEnterpriseResult finalResult) {
        LOGGER.info("LxRemoteService.getDrugRequestList:[{}][{}]获取请求药店下药品信息接口下的药品总量（根据药品的code分组）的list：{}", drugsEnterprise.getId(), drugsEnterprise.getName(), JSONUtils.toString(detailList));
        HdDrugRequestData hdDrugRequestData;
        Double sum;
        SaleDrugListDAO saleDrugListDAO = DAOFactory.getDAO(SaleDrugListDAO.class);
        SaleDrugList saleDrug;
        //遍历处方详情，通过drugId判断，相同药品下的需求量叠加
        for (Recipedetail recipedetail : detailList) {
            //这里的药品是对接到药企下的所以取的是saleDrugList的药品标识
            saleDrug = saleDrugListDAO.getByDrugIdAndOrganId(recipedetail.getDrugId(), drugsEnterprise.getId());
            if(null == saleDrug){
                LOGGER.warn("HdRemoteService.pushRecipeInfo:药品id:{},药企id:{}的药企药品信息不存在",
                        recipedetail.getDrugId(), drugsEnterprise.getId());
                getFailResult(finalResult, "对接的药品信息为空");
                return new ArrayList<>();
            }
            hdDrugRequestData = result.get(saleDrug.getOrganDrugCode());

            if(null == hdDrugRequestData){
                HdDrugRequestData newHdDrugRequestData1 = new HdDrugRequestData();
                result.put(saleDrug.getOrganDrugCode(), newHdDrugRequestData1);
                newHdDrugRequestData1.setDrugCode(saleDrug.getOrganDrugCode());
                newHdDrugRequestData1.setTotal(null == recipedetail.getUseTotalDose() ? drugRecipeTotal : String.valueOf(new Double(recipedetail.getUseTotalDose()).intValue()));
                newHdDrugRequestData1.setUnit(recipedetail.getDrugUnit());
                result.put(saleDrug.getOrganDrugCode(), newHdDrugRequestData1);
            }else{
                //叠加需求量
                sum = Double.parseDouble(hdDrugRequestData.getTotal()) + recipedetail.getUseTotalDose();
                hdDrugRequestData.setTotal(String.valueOf(sum.intValue()));
                result.put(saleDrug.getOrganDrugCode(), hdDrugRequestData);
            }
        }
        //将叠加好总量的药品分组转成list
        return new ArrayList<HdDrugRequestData>(result.values());
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
    public String getDrugEnterpriseCallSys() {
        return DrugEnterpriseConstant.COMPANY_LY;
    }

}