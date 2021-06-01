package recipe.drugsenterprise;

import com.ngari.patient.dto.OrganDTO;
import com.ngari.patient.service.BasicAPI;
import com.ngari.patient.service.OrganService;
import com.ngari.recipe.drugsenterprise.model.DepDetailBean;
import com.ngari.recipe.drugsenterprise.model.DrugsDataBean;
import com.ngari.recipe.drugsenterprise.model.Position;
import com.ngari.recipe.entity.*;
import com.ngari.recipe.hisprescription.model.HospitalRecipeDTO;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import ctd.persistence.DAOFactory;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import ngari.openapi.Client;
import ngari.openapi.Request;
import ngari.openapi.Response;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.bean.DrugEnterpriseResult;
import recipe.constant.DrugEnterpriseConstant;
import recipe.dao.*;
import recipe.drugsenterprise.bean.HdDrugRequestData;
import recipe.drugsenterprise.bean.HdPosition;
import recipe.drugsenterprise.bean.YnsPharmacyAndStockRequest;
import recipe.util.MapValueUtil;

import java.util.*;

/**
* @Description: YnsRemoteService 类（或接口）是 对接云南省医药服务接口
* @Author: JRK
* @Date: 2019/7/8
*/
@RpcBean("ynsRemoteService")
public class YnsRemoteService extends AccessDrugEnterpriseService {

    private static final Logger LOGGER = LoggerFactory.getLogger(YnsRemoteService.class);
    //如果开启加密，则必填
    private static final String encodingAesKey = "";
    private static final String searchMapRANGE = "range";
    private static final String searchMapLatitude = "latitude";
    private static final String searchMapLongitude = "longitude";
    private static final String drugRecipeTotal = "0";
    private static final String requestSuccessCode = "000";

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

    @Override
    public String getDrugInventory(Integer drugId, DrugsEnterprise drugsEnterprise, Integer organId) {
        RecipeParameterDao recipeParameterDao = DAOFactory.getDAO(RecipeParameterDao.class);
        String appKey=recipeParameterDao.getByName("ynsyy-key");
        String pharmacyStock=recipeParameterDao.getByName("ynsyy-pharmacyStockMethod");
        SaleDrugListDAO saleDrugListDAO = DAOFactory.getDAO(SaleDrugListDAO.class);
        try{
            Client client = new Client(drugsEnterprise.getBusinessUrl()+pharmacyStock, appKey, drugsEnterprise.getToken(), encodingAesKey);
            ////根据处方信息发送药企库存查询请求，判断有药店是否满足库存
            //X-Service-Id对应的值
            String serviceId = "CallYygsService";
            //X-Service-Method对应的值
            String method = recipeParameterDao.getByName("ynsyy-pharmacyStockMethod");
            SaleDrugList saleDrugList = saleDrugListDAO.getByDrugIdAndOrganId(drugId, drugsEnterprise.getId());

            List list = new ArrayList<>();
            YnsPharmacyAndStockRequest hdPharmacyAndStockRequest = new YnsPharmacyAndStockRequest();
            List<HdDrugRequestData> drugRequestDataList = new ArrayList<>();

            if (saleDrugList != null) {
                HdDrugRequestData drugBean = new HdDrugRequestData();
                drugBean.setDrugCode(saleDrugList.getOrganDrugCode());
                drugBean.setTotal("5");

                DrugListDAO drugListDAO = DAOFactory.getDAO(DrugListDAO.class);
                DrugList drugList = drugListDAO.getById(drugId);
                drugBean.setUnit(drugList.getUnit());
                drugRequestDataList.add(drugBean);
                hdPharmacyAndStockRequest.setDrugList(drugRequestDataList);
            }
            list.add(hdPharmacyAndStockRequest);
            LOGGER.info("getDrugInventory request:{}.", JSONUtils.toString(list));
            Request request =  new Request(serviceId,method,list);
            Response response = client.execute(request);
            LOGGER.info("getDrugInventory response:{}.", JSONUtils.toString(response));
            Map resultMap = JSONUtils.parse(response.getBody(), Map.class);
            if (requestSuccessCode.equals(MapValueUtil.getString(resultMap, "code"))) {
                String inventory = MapValueUtil.getObject(resultMap, "inventory").toString();
                if ("true".equals(inventory)) {
                    return "有库存";
                } else {
                    return "无库存";
                }
            }else{
                return "无库存";
            }
        }catch (Exception e){
            LOGGER.info("getDrugInventory error:{}.", e.getMessage(), e);
            return "无库存";
        }
    }

    @Override
    public List<String> getDrugInventoryForApp(DrugsDataBean drugsDataBean, DrugsEnterprise drugsEnterprise, Integer flag) {
        return null;
    }

    @RpcService
    public void test(Integer recipeId){
        List<Integer> recipeIds = Arrays.asList(recipeId);
        DrugsEnterpriseDAO enterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        DrugsEnterprise drugsEnterprise = enterpriseDAO.getById(238);
        Map ext=new HashMap();
        ext.put("longitude","31.2553210000");
        ext.put("latitude","121.4620020000");
        ext.put("range","20");
        getDrugInventory(749, drugsEnterprise, 11);
//        scanStock(recipeId,drugsEnterprise);
    }
    @Override
    public DrugEnterpriseResult scanStock(Integer recipeId, DrugsEnterprise drugsEnterprise) {
        LOGGER.info("YnsRemoteService.scanStock:[{}]", JSONUtils.toString(recipeId));
        DrugEnterpriseResult result = DrugEnterpriseResult.getSuccess();
        RecipeParameterDao recipeParameterDao = DAOFactory.getDAO(RecipeParameterDao.class);
        String appKey=recipeParameterDao.getByName("ynsyy-key");
        String pharmacyStock=recipeParameterDao.getByName("ynsyy-pharmacyStockMethod");
        try {
            Client client = new Client(drugsEnterprise.getBusinessUrl()+pharmacyStock, appKey, drugsEnterprise.getToken(), encodingAesKey);
            ////根据处方信息发送药企库存查询请求，判断有药店是否满足库存
            LOGGER.info("YnsRemoteService.scanStock:[{}][{}]根据处方信息发送药企库存查询请求，请求内容：{}", drugsEnterprise.getId(), drugsEnterprise.getName(), recipeId);
            Response response = client.execute(findScanStockBussReq(result,recipeId, drugsEnterprise));
            LOGGER.info("YnsRemoteService.scanStock:[{}][{}]获取药企库存查询请求，获取响应getBody消息：{}", drugsEnterprise.getId(), drugsEnterprise.getName(), response.getBody());
            Map resultMap = JSONUtils.parse(response.getBody(), Map.class);
            if (requestSuccessCode.equals(MapValueUtil.getString(resultMap, "code"))) {
                String inventory = MapValueUtil.getObject(resultMap, "inventory").toString();
                if ("true".equals(inventory)) {
                    result.setCode(DrugEnterpriseResult.SUCCESS);
                    return result;
                } else {
                    result.setCode(DrugEnterpriseResult.FAIL);
                    return result;
                }
            }else{
                getFailResult(result, "当前药企下没有药店的药品库存足够");
            }
            LOGGER.info("HdRemoteService-scanStock 药店取药药品库存:{}.", JSONUtils.toString(result.getObject()));
        } catch (Exception e) {
                result.setCode(DrugEnterpriseResult.FAIL);
                result.setMsg(e.getMessage());
                LOGGER.error("YnsRemoteService.scanStock:[{}][{}]获取药品库存异常：{}",drugsEnterprise.getId(), drugsEnterprise.getName(), e.getMessage(),e);
                getFailResult(result,  e.getMessage());
            } finally {
                try {
                } catch (Exception e) {
                    result.setCode(DrugEnterpriseResult.FAIL);
                    result.setMsg(e.getMessage());
                    getFailResult(result,  e.getMessage());
                    LOGGER.error("YnsRemoteService.scanStock:http请求资源关闭异常: {}！", e.getMessage(),e);
                }
            }
            return result;
    }
    private Request findScanStockBussReq(DrugEnterpriseResult result ,Integer recipeId, DrugsEnterprise drugsEnterprise){
        LOGGER.info("YnsRemoteService.findScanStockBussReq:[{}][{}]获取药企库存查询请求，请求内容：{}", drugsEnterprise.getId(), drugsEnterprise.getName(), recipeId);
        //入参赋值,注意最外层是个json数组
        List bodyList = new ArrayList<>();
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
        bodyList.add(hdPharmacyAndStockRequest);
        LOGGER.info("YnsRemoteService.findScanStockBussReq:获取药企库存查询请求药品数据:{}.", JSONUtils.toString(bodyList));
        //X-Service-Id对应的值
        String serviceId = "CallYygsService";
        //X-Service-Method对应的值
        RecipeParameterDao recipeParameterDao = DAOFactory.getDAO(RecipeParameterDao.class);
        String pharmacyStock=recipeParameterDao.getByName("ynsyy-pharmacyStockMethod");
        String method = pharmacyStock;
        return new Request(serviceId,method,bodyList);
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
        LOGGER.info("YnsRemoteService.findSupportDep:[{}]", JSONUtils.toString(recipeIds));
        DrugEnterpriseResult result = DrugEnterpriseResult.getSuccess();
        RecipeParameterDao recipeParameterDao = DAOFactory.getDAO(RecipeParameterDao.class);
        String appKey=recipeParameterDao.getByName("ynsyy-key");
        String pharmacy=recipeParameterDao.getByName("ynsyy-pharmacyMethod");
        try {
            Client client = new Client(enterprise.getBusinessUrl()+pharmacy,appKey,enterprise.getToken(),encodingAesKey);
            //调用相应的接口请求
            LOGGER.info("YnsRemoteService.findSupportDep:[{}][{}]获取药店列表请求，请求内容：{}", enterprise.getId(), enterprise.getName(), JSONUtils.toString(ext));
            Response response = client.execute(findSupportDepBussReq(result,recipeIds,ext,enterprise));
            LOGGER.info("YnsRemoteService.findSupportDep:[{}][{}]获取药店列表请求，获取响应getBody消息：{}", enterprise.getId(), enterprise.getName(), response.getBody());
            Map resultMap = JSONUtils.parse(response.getBody(), Map.class);
            if(requestSuccessCode.equals(MapValueUtil.getString(resultMap, "code"))) {
                //接口返回的结果
                List<Map<String, Object>> ynsStoreBeans = MapValueUtil.getList(resultMap, "list");
                //数据封装成页面展示数据
                List<DepDetailBean> list=new ArrayList<>();
                DepDetailBean detailBean;
                for (Map<String, Object> ynsStoreBean : ynsStoreBeans) {
                    LOGGER.info("YnsRemoteService.findSupportDep ynsStoreBean:{}.", JSONUtils.toString(ynsStoreBean));
                    detailBean=new DepDetailBean();
                    detailBean.setPharmacyCode(MapValueUtil.getString(ynsStoreBean, "pharmacyCode"));
                    detailBean.setDepName(MapValueUtil.getString(ynsStoreBean, "pharmacyName"));
                    detailBean.setAddress(MapValueUtil.getString(ynsStoreBean, "address"));
                    detailBean.setDistance(Double.parseDouble(MapValueUtil.getString(ynsStoreBean, "distance")));
                    LOGGER.info("YnsRemoteService.findSupportDep pharmacyCode:{}.",MapValueUtil.getString(ynsStoreBean, "pharmacyCode") );
                    Map position=  (Map)MapValueUtil.getObject(ynsStoreBean, "position");
                    LOGGER.info("YnsRemoteService.findSupportDep position:{}.", position);
                    //Map mp = JSONUtils.parse(position, Map.class);
                    LOGGER.info("YnsRemoteService.findSupportDep mp:{}.", JSONUtils.toString(position));
                    Position postion=new Position();
                    postion.setLatitude(Double.parseDouble(MapValueUtil.getString(position, "latitude")));
                    postion.setLongitude(Double.parseDouble(MapValueUtil.getString(position, "longitude")));
                    detailBean.setPosition(postion);
                    list.add(detailBean);
                }
                result.setObject(list);
                LOGGER.info("YnsRemoteService.findSupportDep:[{}][{}]获取药店列表请求，返回前端result消息：{}", enterprise.getId(), enterprise.getName(), JSONUtils.toString(result));
            }else {
                String responseData = MapValueUtil.getString(resultMap, "msg");
                result.setCode(DrugEnterpriseResult.FAIL);
                result.setMsg(responseData);
                LOGGER.error("YnsRemoteService.findSupportDep: msg [{}][{}]获取药店列表异常：{}",enterprise.getId(), enterprise.getName(), responseData);
                getFailResult(result, responseData);
                return result;
            }
        } catch (Exception e) {
            result.setCode(DrugEnterpriseResult.FAIL);
            result.setMsg(e.getMessage());
            LOGGER.error("YnsRemoteService.findSupportDep:[{}][{}]获取药店列表异常：{}",enterprise.getId(), enterprise.getName(), e.getMessage(),e);
            getFailResult(result,  e.getMessage());
        } finally {
            try {
            } catch (Exception e) {
                result.setCode(DrugEnterpriseResult.FAIL);
                result.setMsg(e.getMessage());
                getFailResult(result,  e.getMessage());
                LOGGER.error("YnsRemoteService.findSupportDep:http请求资源关闭异常: {}！", e.getMessage(),e);
            }
        }
        return result;
    }
    private Request findSupportDepBussReq(DrugEnterpriseResult result ,List<Integer> recipeIds, Map ext, DrugsEnterprise enterprise){
        LOGGER.info("YnsRemoteService.findSupportDepBussReq:[{}][{}]获取药店列表请求，请求内容：{}", enterprise.getId(), enterprise.getName(), recipeIds);
        //入参赋值,注意最外层是个json数组
        List bodyList = new ArrayList<>();
        RecipeDetailDAO detailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
        //现在默认只有一个处方单
        List<Recipedetail> detailList = detailDAO.findByRecipeId(recipeIds.get(0));
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        if(CollectionUtils.isEmpty(detailList)){
            LOGGER.warn("YnsRemoteService.findSupportDep:处方单{}的细节信息为空", recipeIds.get(0));

        }
        Recipe nowRecipe = recipeDAO.get(recipeIds.get(0));
        if (null == nowRecipe) {
            LOGGER.warn("YnsRemoteService.findSupportDep:处方单{}不存在", recipeIds.get(0));

        }
        OrganService organService = BasicAPI.getService(OrganService.class);
        OrganDTO organ = organService.getByOrganId(nowRecipe.getClinicOrgan());
        if(null == organ){
            LOGGER.warn("YnsRemoteService.findSupportDep:处方ID为{},对应的开处方机构不存在.", nowRecipe.getRecipeId());
        }
        //根据请求返回的药店列表，获取对应药店下药品需求的总价格
        Map<String, HdDrugRequestData> resultMap = new HashMap<>();
        //首先组装请求对象
        YnsPharmacyAndStockRequest hdPharmacyAndStockRequest = new YnsPharmacyAndStockRequest();
        LOGGER.info("YnsRemoteService.findSupportDepBussReq:[{}][{}]获取药店列表请求，请求内容：{}", enterprise.getId(), enterprise.getName(), "判断药店的坐标地址是否正确");
        if (ext != null && null!= ext.get(searchMapRANGE) && null!= ext.get(searchMapLongitude) && null!= ext.get(searchMapLatitude)) {
            List<HdDrugRequestData> drugRequestList = getDrugRequestList(resultMap, detailList, enterprise, result);
            if(DrugEnterpriseResult.FAIL == result.getCode()) return null;
            hdPharmacyAndStockRequest.setDrugList(drugRequestList);
            hdPharmacyAndStockRequest.setRange("50");
            hdPharmacyAndStockRequest.setPosition(new HdPosition(MapValueUtil.getString(ext, searchMapLongitude), MapValueUtil.getString(ext, searchMapLatitude)));

        }else{
            LOGGER.warn("HdRemoteService.findSupportDep:请求的搜索参数不健全" );
        }
        bodyList.add(hdPharmacyAndStockRequest);
        //X-Service-Id对应的值
        String serviceId = "CallYygsService";
        //X-Service-Method对应的值
        RecipeParameterDao recipeParameterDao = DAOFactory.getDAO(RecipeParameterDao.class);
        String pharmacy=recipeParameterDao.getByName("ynsyy-pharmacyMethod");
        String method = pharmacy;
        return new Request(serviceId,method,bodyList);
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
        LOGGER.info("YnsRemoteService.getDrugRequestList:[{}][{}]获取请求药店下药品信息接口下的药品总量（根据药品的code分组）的list：{}", drugsEnterprise.getId(), drugsEnterprise.getName(), JSONUtils.toString(detailList));
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
                return null;
            }
            hdDrugRequestData = result.get(saleDrug.getOrganDrugCode());

            if(null == hdDrugRequestData){
                HdDrugRequestData newHdDrugRequestData1 = new HdDrugRequestData();
                result.put(saleDrug.getOrganDrugCode(), newHdDrugRequestData1);
                newHdDrugRequestData1.setDrugCode(saleDrug.getOrganDrugCode());
                newHdDrugRequestData1.setTotal(null == recipedetail.getUseTotalDose() ? drugRecipeTotal : String.valueOf(new Double(recipedetail.getUseTotalDose()).intValue()));
                newHdDrugRequestData1.setUnit(recipedetail.getDrugUnit());
            }else{
                //叠加需求量
                sum = Double.parseDouble(hdDrugRequestData.getTotal()) + recipedetail.getUseTotalDose();
                hdDrugRequestData.setTotal(String.valueOf(sum.intValue()));
                result.put(saleDrug.getOrganDrugCode(), hdDrugRequestData);
            }
        }
        //将叠加好总量的药品分组转成list
        List<HdDrugRequestData> hdDrugRequestDataList = new ArrayList<HdDrugRequestData>(result.values());
        return hdDrugRequestDataList;
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
        return DrugEnterpriseConstant.COMPANY_YNS;
    }

}