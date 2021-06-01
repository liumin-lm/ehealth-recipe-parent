package recipe.drugsenterprise;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.ngari.patient.dto.*;
import com.ngari.patient.service.*;
import com.ngari.recipe.drugsenterprise.model.DrugsDataBean;
import com.ngari.recipe.entity.*;
import com.ngari.recipe.hisprescription.model.HospitalRecipeDTO;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import ctd.controller.exception.ControllerException;
import ctd.dictionary.DictionaryController;
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
import recipe.dao.*;
import recipe.drugsenterprise.bean.YkDrugDto;
import recipe.drugsenterprise.bean.YkRecipeInfoDto;
import recipe.drugsenterprise.bean.YkRecipeListInfoDto;
import recipe.util.DateConversion;
import recipe.util.MapValueUtil;

import javax.xml.namespace.QName;
import javax.xml.rpc.ParameterMode;
import java.net.URL;
import java.util.*;

/**
 * 英克对接服务
 * company: ngarihealth
 *
 * @author: 0184/yu_yun
 * @date:2017/3/7.
 */
@RpcBean("ykRemoteService")
public class YkRemoteService extends AccessDrugEnterpriseService {

    private static final Logger LOGGER = LoggerFactory.getLogger(YkRemoteService.class);
    private static String NAME_SPACE = "http://hbgksj.webservice.inca.com";
    private static String RESULT_SUCCESS = "0";
    private static String RESULT_FAIL = "1";


    @Override
    public void tokenUpdateImpl(DrugsEnterprise drugsEnterprise) {

    }
    @RpcService
    public void test(Integer recipeId){
        List<Integer> recipeIds = Arrays.asList(recipeId);
        DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        DrugsEnterprise enterprise = drugsEnterpriseDAO.getById(235);
        pushRecipeInfo(recipeIds, enterprise);
    }
    @Override
    public DrugEnterpriseResult pushRecipeInfo(List<Integer> recipeIds, DrugsEnterprise enterprise) {
        DrugEnterpriseResult result = DrugEnterpriseResult.getSuccess();
//        DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
//        DrugsEnterprise enterprise = drugsEnterpriseDAO.getByAccount("yk");
        String depName=enterprise.getName();
        if (CollectionUtils.isEmpty(recipeIds)) {
            result.setMsg("处方ID参数为空");
            result.setCode(DrugEnterpriseResult.FAIL);
            return result;
        }
        //最终发给药企的json数据
        Map<String, Object> sendInfo = new HashMap<>(1);
        List<YkRecipeInfoDto> ykRecipeListDto = getYkRecipeInfo(recipeIds, enterprise);
        Map<String, Object> res= new HashMap<>(1);
        res.put("wbcf_list",ykRecipeListDto);
        sendInfo.put("data", res);
        String sendInfoStr = JSON.toJSONString(sendInfo, new SerializerFeature[]
                { SerializerFeature.WriteMapNullValue, SerializerFeature.WriteNullStringAsEmpty });
        String methodName = "Wbcf_Service";
        LOGGER.info("发送[{}][{}]内容：{}", depName, methodName, sendInfoStr);

        //发送药企信息
        sendAndDealResult(enterprise, methodName, sendInfoStr, result);
        return result;
    }
    private List<YkRecipeInfoDto> getYkRecipeInfo(List<Integer> recipeIds, DrugsEnterprise enterprise) {
        LOGGER.info("YkRemoteService.getYkRecipeInfo 获取英克推送处方信息:{}.", JSONUtils.toString(recipeIds));
        DrugEnterpriseResult result = DrugEnterpriseResult.getFail();
        PatientService patientService = BasicAPI.getService(PatientService.class);
        DoctorService doctorService = BasicAPI.getService(DoctorService.class);
        DepartmentService departmentService = BasicAPI.getService(DepartmentService.class);
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        RecipeOrderDAO recipeOrderDAO = DAOFactory.getDAO(RecipeOrderDAO.class);
        RecipeDetailDAO recipeDetailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
        Integer depId = enterprise.getId();
        String depName = enterprise.getName();
        //药品信息MAP，减少DB查询
        List<YkRecipeInfoDto> recipeInfoList = new ArrayList<YkRecipeInfoDto>();
        //每个处方的json数据
        Map<String, Object> recipeMap;
        for (Integer recipeId : recipeIds) {
            Recipe nowRecipe = recipeDAO.getByRecipeId(recipeId);
            List<Recipedetail> recipedetails = recipeDetailDAO.findByRecipeId(nowRecipe.getRecipeId());
            if (null == nowRecipe) {
                LOGGER.error("getYkRecipeInfo ID为" + recipeId + "的处方不存在");
                continue;
            }
            //订单信息
            RecipeOrder recipeOrder = recipeOrderDAO.getByOrderCode(nowRecipe.getOrderCode());
            //科室信息
            DepartmentDTO department=departmentService.getById(nowRecipe.getDepart());
            //医生信息
            DoctorDTO doctor = doctorService.get(nowRecipe.getDoctor());
            //患者信息
            PatientDTO patientDTO = patientService.get(nowRecipe.getMpiid());
            if(null == doctor){
                LOGGER.warn("YtRemoteService.pushRecipeInfo:处方ID为{},绑定医生不存在.", nowRecipe.getRecipeId());
                getFailResult(result, "处方绑定医生不存在");
            }
            YkRecipeInfoDto ykRecipeInfoDto=new YkRecipeInfoDto();
            ykRecipeInfoDto.setAddress(patientDTO.getAddress());
            ykRecipeInfoDto.setBirthday(DateConversion.getDateFormatter(patientDTO.getBirthday(),DateConversion.YYYY_MM_DD));
            ykRecipeInfoDto.setConsigdstreet(StringUtils.isEmpty(recipeOrder.getAddress4()) ? "" : recipeOrder.getAddress4());
            ykRecipeInfoDto.setConsigneename(recipeOrder.getReceiver());
            ykRecipeInfoDto.setConsigneetel(recipeOrder.getRecMobile());
            ykRecipeInfoDto.setPrescriptionid(nowRecipe.getRecipeId());
            ykRecipeInfoDto.setConsigpcity(getCompleteAddress(recipeOrder));
            ykRecipeInfoDto.setContraindication("");
            ykRecipeInfoDto.setCredata(DateConversion.getDateFormatter(nowRecipe.getCreateDate(),DateConversion.DEFAULT_DATE_TIME));
            ykRecipeInfoDto.setDiagnosis(nowRecipe.getMemo());
            ykRecipeInfoDto.setDiseaseid(100);
            ykRecipeInfoDto.setId_card(patientDTO.getIdcard());
            ykRecipeInfoDto.setInsiderage(patientDTO.getAge().toString());
            ykRecipeInfoDto.setInsidercardno(1600000);
            ykRecipeInfoDto.setInsidername(patientDTO.getPatientName());
            ykRecipeInfoDto.setInsidersex(patientDTO.getPatientSex());
            ykRecipeInfoDto.setMedicard("");
            ykRecipeInfoDto.setNation(patientDTO.getNation());
            ykRecipeInfoDto.setPlacepointid("50");
            ykRecipeInfoDto.setPrescriptiondoctor(doctor.getName());
            ykRecipeInfoDto.setPrescriptionhospital(nowRecipe.getOrganName());
            ykRecipeInfoDto.setPrescriptionnumber(nowRecipe.getRecipeId().toString());
            ykRecipeInfoDto.setPrescriptiontype(2);
            ykRecipeInfoDto.setSourceplatform(3);
            ykRecipeInfoDto.setUsagedosage("");

            ykRecipeInfoDto.setUsestates("1");
            List<YkDrugDto> ykDrugDtoList = new ArrayList<YkDrugDto>();
            SaleDrugListDAO saleDrugListDAO = DAOFactory.getDAO(SaleDrugListDAO.class);
            for(Recipedetail recipedetail:recipedetails){
                YkDrugDto ykDrugDto=new YkDrugDto();
                SaleDrugList saleDrugList = saleDrugListDAO.getByDrugIdAndOrganId(recipedetail.getDrugId(), enterprise.getId());
                ykDrugDto.setGoods_id(Integer.parseInt(saleDrugList.getOrganDrugCode()));
                ykDrugDto.setGoods_num(recipedetail.getUseTotalDose().intValue());
                ykDrugDto.setPrescriptiondtlid(recipedetail.getRecipeDetailId());
                ykDrugDto.setPrescriptiontype(2);
                ykDrugDto.setSourceplatform(3);
                ykDrugDto.setUnitprice(recipedetail.getSalePrice().toString());
                ykDrugDto.setMoney(recipedetail.getSalePrice().toString());
                ykDrugDtoList.add(ykDrugDto);
            }
            ykRecipeInfoDto.setGoods_list(ykDrugDtoList);
            recipeInfoList.add(ykRecipeInfoDto);
        }
        return recipeInfoList;
    }
    @Override
    public DrugEnterpriseResult pushRecipe(HospitalRecipeDTO hospitalRecipeDTO, DrugsEnterprise enterprise) {
        return null;
    }

    @Override
    public String getDrugInventory(Integer drugId, DrugsEnterprise drugsEnterprise, Integer organId) {
        return "有库存";
    }

    @Override
    public List<String> getDrugInventoryForApp(DrugsDataBean drugsDataBean, DrugsEnterprise drugsEnterprise, Integer flag) {
        return null;
    }

    @Override
    public DrugEnterpriseResult scanStock(Integer recipeId, DrugsEnterprise drugsEnterprise) {
        return DrugEnterpriseResult.getSuccess();
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
        return DrugEnterpriseResult.getSuccess();
    }

    @Override
    public String getDrugEnterpriseCallSys() {
        return DrugEnterpriseConstant.COMPANY_YK;
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
    /**
     * 发送药企信息处理返回结果
     *
     * @param drugsEnterprise
     * @param method
     * @param sendInfoStr
     * @param result
     */
    private void sendAndDealResult(DrugsEnterprise drugsEnterprise, String method, String sendInfoStr, DrugEnterpriseResult result) {
        String drugEpName = drugsEnterprise.getName();
        String resultJson = null;
        try {
            Call call = getCall(drugsEnterprise, method);
            if (null != call) {
                Object resultObj;
                Object[] param = {sendInfoStr};
                resultObj = call.invoke(param);
                if (null != resultObj && resultObj instanceof String) {
                    resultJson = resultObj.toString();
                    LOGGER.info("调用[{}][{}]结果返回={}", drugEpName, method, resultJson);
                } else {
                    LOGGER.error("调用[{}][{}]结果返回为空", drugEpName, method);
                    result.setMsg(drugEpName + "接口返回结果为空");
                    result.setCode(DrugEnterpriseResult.FAIL);
                }
            }
        } catch (Exception e) {
            resultJson = null;
            LOGGER.error(drugEpName + " invoke method[{}] error ", method, e);
            result.setMsg(drugEpName + "接口调用出错");
            result.setCode(DrugEnterpriseResult.FAIL);
        }

        if (StringUtils.isNotEmpty(resultJson)) {
            Map resultMap = JSONUtils.parse(resultJson, Map.class);
            String resCode = MapValueUtil.getString(resultMap, "CODE");
            if (RESULT_SUCCESS.equals(resCode)) {
                LOGGER.info("YkRemoteService.sendAndDealResult 处方推送成功，{}");
            } else {
                result.setMsg("调用[" + drugEpName + "][" + method + "]失败.error:" + MapValueUtil.getString(resultMap, "message"));
                result.setCode(DrugEnterpriseResult.FAIL);
            }
        } else {
            result.setMsg(drugEpName + "接口调用返回为空");
            result.setCode(DrugEnterpriseResult.FAIL);
        }
    }
    /**
     * @method  getFailResult
     * @description 失败操作的结果对象
     * @date: 2020/2/20
     * @author: JRK
     * @param result 返回的结果集对象
     * @param msg 失败提示的信息
     * @return
     */
    private void getFailResult(DrugEnterpriseResult result, String msg) {
        result.setMsg(msg);
        result.setCode(DrugEnterpriseResult.FAIL);
    }
    /**
     * 生成完整地址
     *
     * @param order 订单
     * @return
     */
    public String getCompleteAddress(RecipeOrder order) {
        StringBuilder address = new StringBuilder();
        if (null != order) {
            this.getAddressDic(address, order.getAddress1());
            this.getAddressDic(address, order.getAddress2());
            this.getAddressDic(address, order.getAddress3());
            address.append(StringUtils.isEmpty(order.getAddress4()) ? "" : order.getAddress4());
        }
        return address.toString();
    }
}
