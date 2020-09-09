package recipe.drugsenterprise;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.ngari.patient.dto.OrganDTO;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.patient.service.*;
import com.ngari.recipe.drugsenterprise.model.DrugsDataBean;
import com.ngari.recipe.entity.*;
import com.ngari.recipe.hisprescription.model.HospitalRecipeDTO;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import ctd.persistence.DAOFactory;
import ctd.util.JSONUtils;
import org.apache.axis.client.Call;
import org.apache.axis.client.Service;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.bean.DrugEnterpriseResult;
import recipe.constant.DrugEnterpriseConstant;
import recipe.dao.*;
import recipe.drugsenterprise.bean.*;
import recipe.util.DateConversion;
import recipe.util.MapValueUtil;

import javax.xml.namespace.QName;
import java.math.BigDecimal;
import java.net.URL;
import java.util.*;

/**
 * 上海国药对接服务
 * company: ngarihealth
 *
 * @author: 0184/yu_yun
 * @date:2017/3/7.
 */
public class CommonSHRemoteService extends AccessDrugEnterpriseService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommonSHRemoteService.class);
    private static String NAME_SPACE = "http://tempuri.org/";
    private static String RESULT_SUCCESS = "0";
    private static String RESULT_FAIL = "1";


    @Override
    public void tokenUpdateImpl(DrugsEnterprise drugsEnterprise) {

    }

    public void test(Integer recipeId){
        List<Integer> recipeIds = Arrays.asList(recipeId);
        DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        DrugsEnterprise enterprise = drugsEnterpriseDAO.getById(236);
        getDistributionZt(recipeIds.get(0).toString(), enterprise);
    }

    //国药签名
    public Map<String,String> getSignature(String appId, String token)
    {
        long milli = System.currentTimeMillis() + 8*3600*1000;
        long ticks = (milli*10000)+621355968000000000L;
        int r = (new Random()).nextInt(10000);
        String nonce = String.valueOf(r);
        String[] ArrTmp = { appId, token,String.valueOf(ticks) ,nonce};
        Arrays.sort(ArrTmp,String.CASE_INSENSITIVE_ORDER);
        String tmpStr = String.join("", ArrTmp);
        tmpStr = DigestUtils.sha1Hex(tmpStr);//SHA1加¨®密¨¹
        Map<String,String> result = new HashMap<String,String>();
        result.put("accesstoken",tmpStr);
        result.put("timestamp",String.valueOf(ticks));
        result.put("nonce",nonce);
        return result;
    }

    @Override
    public DrugEnterpriseResult pushRecipeInfo(List<Integer> recipeIds, DrugsEnterprise enterprise) {
        DrugEnterpriseResult result = DrugEnterpriseResult.getSuccess();
        String depName=enterprise.getName();
        if (CollectionUtils.isEmpty(recipeIds)) {
            result.setMsg("处方ID参数为空");
            result.setCode(DrugEnterpriseResult.FAIL);
            return result;
        }

        //最终发给药企的json数据
        Map<String, Object> sendInfo = new HashMap<>(1);
        List<CommonSHRecipeInfoDto> commonSHRecipeInfoDto = getCommonSHRecipeInfo(recipeIds, enterprise);
        Map<String, Object> res= new HashMap<>(1);
        res.put("master",commonSHRecipeInfoDto);
        sendInfo.put("Content", res);
        sendInfo.put("MsgType","QZ001");
        sendInfo.put("DataType","Json");
        Date date = new Date(System.currentTimeMillis());
        sendInfo.put("CreateTime",DateConversion.getDateFormatter(date,DateConversion.DEFAULT_DATE_TIME));
        String sendInfoStr = JSON.toJSONString(sendInfo, new SerializerFeature[]
                { SerializerFeature.WriteMapNullValue, SerializerFeature.WriteNullStringAsEmpty });
        String methodName = "TransData";
        //LOGGER.info("发送[{}][{}]内容：{}", depName, methodName, sendInfoStr);

        //发送药企信息
        String appId = enterprise.getUserId();
        String tocken = enterprise.getToken();
        sendAndDealResult(enterprise, methodName, sendInfoStr, result, appId, tocken);
        return result;
    }
    private List<CommonSHRecipeInfoDto> getCommonSHRecipeInfo(List<Integer> recipeIds, DrugsEnterprise enterprise) {
        LOGGER.info("CommonSHRemoteService.getCommonSHRecipeInfo 获取上海国药推送处方信息:{}.", JSONUtils.toString(recipeIds));
        DrugEnterpriseResult result = DrugEnterpriseResult.getFail();
        PatientService patientService = BasicAPI.getService(PatientService.class);
        DoctorService doctorService = BasicAPI.getService(DoctorService.class);
        DepartmentService departmentService = BasicAPI.getService(DepartmentService.class);
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        RecipeOrderDAO recipeOrderDAO = DAOFactory.getDAO(RecipeOrderDAO.class);
        RecipeDetailDAO recipeDetailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
        DrugListDAO drugListDAO = DAOFactory.getDAO(DrugListDAO.class);
        Integer depId = enterprise.getId();
        String depName = enterprise.getName();
        //药品信息MAP，减少DB查询
        List<CommonSHRecipeInfoDto> recipeInfoList = new ArrayList<CommonSHRecipeInfoDto>();
        //每个处方的json数据
        Map<String, Object> recipeMap;
        for (Integer recipeId : recipeIds) {
            Recipe nowRecipe = recipeDAO.getByRecipeId(recipeId);
            List<Recipedetail> recipedetails = recipeDetailDAO.findByRecipeId(nowRecipe.getRecipeId());
            if (null == nowRecipe) {
                LOGGER.error("getCommonSHRecipeInfo ID为" + recipeId + "的处方不存在");
                continue;
            }
            //订单信息
            RecipeOrder recipeOrder = recipeOrderDAO.getByOrderCode(nowRecipe.getOrderCode());
            //患者信息
            PatientDTO patientDTO = patientService.get(nowRecipe.getMpiid());
            //医院信息
            OrganService organService = BasicAPI.getService(OrganService.class);
            OrganDTO organ = organService.getByOrganId(nowRecipe.getClinicOrgan());
            SaleDrugListDAO saleDrugListDAO = DAOFactory.getDAO(SaleDrugListDAO.class);
            for(Recipedetail recipedetail:recipedetails){
                SaleDrugList saleDrugList = saleDrugListDAO.getByDrugIdAndOrganId(recipedetail.getDrugId(), enterprise.getId());
                CommonSHRecipeInfoDto commonSHRecipeInfoDto=new CommonSHRecipeInfoDto();
                commonSHRecipeInfoDto.setPRESCRIPTNO(nowRecipe.getRecipeId().toString());
                commonSHRecipeInfoDto.setPRESCRIPTNOSEQ(recipedetail.getRecipeDetailId().toString());
                commonSHRecipeInfoDto.setHOSCODE(organ.getMinkeUnitID());
                commonSHRecipeInfoDto.setHOSNAME(organ.getName());
                commonSHRecipeInfoDto.setHEALCARD(patientDTO.getIdcard());
                commonSHRecipeInfoDto.setIDCARDNO(patientDTO.getIdcard());
                commonSHRecipeInfoDto.setENDOWMENT(patientDTO.getIdcard());
                commonSHRecipeInfoDto.setMEDICALNO(patientDTO.getIdcard());
                commonSHRecipeInfoDto.setARMYNO("");
                commonSHRecipeInfoDto.setSTUDENTNO("");
                commonSHRecipeInfoDto.setRESIDENCE("");
                commonSHRecipeInfoDto.setBILLDATE(DateConversion.getDateFormatter(nowRecipe.getCreateDate(),DateConversion.DEFAULT_DATE_TIME));
                commonSHRecipeInfoDto.setPATIENTNAME(patientDTO.getPatientName());
                if(StringUtils.equals(patientDTO.getPatientSex(),"男"))
                {
                    commonSHRecipeInfoDto.setGENDER("1");
                }else{
                    commonSHRecipeInfoDto.setGENDER("0");
                }
                commonSHRecipeInfoDto.setPATIENTADDRESS(patientDTO.getAddress());
                commonSHRecipeInfoDto.setPATIENTPHONE(patientDTO.getMobile());
                commonSHRecipeInfoDto.setGOODS(saleDrugList.getOrganDrugCode());
                commonSHRecipeInfoDto.setGOODSNAME(saleDrugList.getDrugName());
                commonSHRecipeInfoDto.setGNAME(saleDrugList.getSaleName());
                commonSHRecipeInfoDto.setSPEC(saleDrugList.getDrugSpec());
                commonSHRecipeInfoDto.setMSUNITNO(recipedetail.getDrugUnit());
                commonSHRecipeInfoDto.setPRONAME(drugListDAO.getById(saleDrugList.getDrugId()).getProducer());
                commonSHRecipeInfoDto.setHOSQTY(nowRecipe.getCopyNum());
                commonSHRecipeInfoDto.setHOSPRICE(saleDrugList.getPrice());
                commonSHRecipeInfoDto.setSUMVALUE(nowRecipe.getTotalMoney());
                commonSHRecipeInfoDto.setMETHODDESC(nowRecipe.getTcmUsePathways() + " " + nowRecipe.getTcmUsingRate());
                commonSHRecipeInfoDto.setHOSDEPTCODE("1");
                commonSHRecipeInfoDto.setHOSDEPTNAME("中国");
                commonSHRecipeInfoDto.setPATTERN("1");
                commonSHRecipeInfoDto.setREMARK(nowRecipe.getRecipeMemo());
                commonSHRecipeInfoDto.setRTLPRC(saleDrugList.getPrice());
                commonSHRecipeInfoDto.setAGE(patientDTO.getAge());
                commonSHRecipeInfoDto.setOTHERSUPPLIERS("0");
                commonSHRecipeInfoDto.setSALTYPE("0");
                recipeInfoList.add(commonSHRecipeInfoDto);
            }
        }
        return recipeInfoList;
    }
    @Override
    public DrugEnterpriseResult pushRecipe(HospitalRecipeDTO hospitalRecipeDTO, DrugsEnterprise enterprise) {
        return DrugEnterpriseResult.getSuccess();
    }

    @Override
    public String getDrugInventory(Integer drugId, DrugsEnterprise drugsEnterprise, Integer organId) {
        SaleDrugListDAO saleDrugListDAO = DAOFactory.getDAO(SaleDrugListDAO.class);
        SaleDrugList saleDrugList = saleDrugListDAO.getByDrugIdAndOrganId(drugId, drugsEnterprise.getId());
        if (saleDrugList.getInventory().intValue() == 1) {
            return "有库存";
        } else {
            return "无库存";
        }

    }

    @Override
    public List<String> getDrugInventoryForApp(DrugsDataBean drugsDataBean, DrugsEnterprise drugsEnterprise, Integer flag) {
        return null;
    }

    @Override
    public DrugEnterpriseResult scanStock(Integer recipeId, DrugsEnterprise enterprise) {
        LOGGER.info("CommonSHRemoteService.scanStock:处方ID为{}.", recipeId);
        DrugEnterpriseResult result = DrugEnterpriseResult.getSuccess();
        SaleDrugListDAO saleDrugListDAO = DAOFactory.getDAO(SaleDrugListDAO.class);
        RecipeDetailDAO recipeDetailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
        List<Recipedetail> recipedetails = recipeDetailDAO.findByRecipeId(recipeId);
        for (Recipedetail recipeDetail : recipedetails) {
            SaleDrugList saleDrugList = saleDrugListDAO.getByDrugIdAndOrganId(recipeDetail.getDrugId(), enterprise.getId());
            if (saleDrugList.getInventory().intValue() == 0) {
                result.setCode(DrugEnterpriseResult.FAIL);
                return result;
            }
        }
        return result;

        /*String depName = enterprise.getName();
        try{
            //查询当前处方信息
            RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
            Recipe nowRecipe = recipeDAO.get(recipeId);
            //查询当前处方下详情信息
            RecipeDetailDAO detailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
            List<Recipedetail> detailList = detailDAO.findByRecipeId(nowRecipe.getRecipeId());
            Map<Integer, DetailDrugGroup> drugGroup = getDetailGroup(detailList);
            SaleDrugList saleDrug = null;
            //遍历处方，判断当处方的所有的药品的库存量都够的话判断为库存足够
            boolean checkScan = true;

            //最终发给药企的json数据
            Map<String, Object> sendInfo = new HashMap<>(1);
            List<CommonSHScanStockReqDto> commonSHScanStockReqDto = new  ArrayList<CommonSHScanStockReqDto>();

            Iterator<Integer> iter = drugGroup.keySet().iterator();
            while(iter.hasNext()){
                Integer key=iter.next();
                DetailDrugGroup value = drugGroup.get(key);
                CommonSHScanStockReqDto commonSHScanStockReq = new CommonSHScanStockReqDto();
                saleDrug = saleDrugListDAO.getByDrugIdAndOrganId(key, enterprise.getId());
                commonSHScanStockReq.setGoods(saleDrug.getOrganDrugCode());
                commonSHScanStockReq.setPageIndex(1);
                commonSHScanStockReq.setPageSize(200);
                commonSHScanStockReqDto.add(commonSHScanStockReq);
            }

            Map<String, Object> res= new HashMap<>(1);
            res.put("master",commonSHScanStockReqDto);
            sendInfo.put("Content", res);
            sendInfo.put("MsgType","QZ004");
            sendInfo.put("DataType","Json");
            Date date = new Date(System.currentTimeMillis());
            sendInfo.put("CreateTime",DateConversion.getDateFormatter(date,DateConversion.DEFAULT_DATE_TIME));
            String sendInfoStr = JSON.toJSONString(sendInfo, new SerializerFeature[]
                    { SerializerFeature.WriteMapNullValue, SerializerFeature.WriteNullStringAsEmpty });
            String methodName = "TransData";
            LOGGER.info("发送[{}][{}]内容：{}", depName, methodName, sendInfoStr);

            //发送药企信息
            String appId = enterprise.getUserId();
            String tocken = enterprise.getToken();
            String resultJson = sendAndDealResult(enterprise, methodName, sendInfoStr, result, appId, tocken);
            JSONObject jsonObject = JSONObject.parseObject(resultJson);
            String transId = jsonObject.getString("transId");
            if(StringUtils.isEmpty(resultJson))
            {
                result.setMsg("库存信息下载失败");
            }else if(StringUtils.equals(jsonObject.getString("Success"),"false")){
                result.setMsg("库存信息下载失败");
            }
            else {
                jsonObject = JSONObject.parseObject(jsonObject.getString("Content"));
                JSONArray jsonArray = (JSONArray)JSONArray.parse(jsonObject.getString("master"));
                for(int i = 0;i < jsonArray.size();i++){
                    jsonObject = (JSONObject) jsonArray.get(i);
                    String isstock = jsonObject.getString("isstock");
                    if(StringUtils.equals(isstock,"0"))
                    {
                        checkScan = false;
                    }
                    DrugEnterpriseResult confirmResult = getScanStockConfirm(transId,jsonObject.getString("goods"),enterprise);
                    if(StringUtils.equals(confirmResult.getMsg(),"fail"))
                    {
                        result.setCode(DrugEnterpriseResult.FAIL);
                        result.setMsg("库存下载确认反馈失败");
                        break;
                    }
                    result.setCode(DrugEnterpriseResult.SUCCESS);
                    result.setMsg("库存充足");
                }
                if(!checkScan){
                    getFailResult(result, "当前药企下药品库存不够");
                } else {
                    result.setMsg("调用[" + enterprise.getName() + "][ scanStock ]结果返回成功,有库存,处方单ID:"+recipeId+".");
                }
            }
        }catch (Exception e){
            getFailResult(result, "当前药企下没有药店的药品库存足够");
            LOGGER.info("CommonSHRemoteService.scanStock:处方ID为{},{}.", recipeId, e.getMessage());
        }*/
    }

    /**
     * 获取当前所有药品库存
     * @param depId
     * @return
     */

    public DrugEnterpriseResult scanStockAll(Integer depId) {
        DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        DrugsEnterprise enterprise = drugsEnterpriseDAO.getById(depId);
        DrugEnterpriseResult result = DrugEnterpriseResult.getSuccess();
        String depName = enterprise.getName();
        try{

            boolean checkScan = true;

            //最终发给药企的json数据
            Map<String, Object> sendInfo = new HashMap<>(1);
            List<CommonSHScanStockReqDto> commonSHScanStockReqDto = new  ArrayList<CommonSHScanStockReqDto>();
            CommonSHScanStockReqDto commonSHScanStockReq = new CommonSHScanStockReqDto();
            commonSHScanStockReq.setGoods("");
            commonSHScanStockReq.setPageIndex(1);
            commonSHScanStockReq.setPageSize(200);
            commonSHScanStockReqDto.add(commonSHScanStockReq);
            Map<String, Object> res= new HashMap<>(1);
            res.put("master",commonSHScanStockReqDto);
            sendInfo.put("Content", res);
            sendInfo.put("MsgType","QZ004");
            sendInfo.put("DataType","Json");
            Date date = new Date(System.currentTimeMillis());
            sendInfo.put("CreateTime",DateConversion.getDateFormatter(date,DateConversion.DEFAULT_DATE_TIME));
            String sendInfoStr = JSON.toJSONString(sendInfo, new SerializerFeature[]
                    { SerializerFeature.WriteMapNullValue, SerializerFeature.WriteNullStringAsEmpty });
            String methodName = "TransData";
            //LOGGER.info("发送[{}][{}]内容：{}", depName, methodName, sendInfoStr);

            //发送药企信息
            String appId = enterprise.getUserId();
            String tocken = enterprise.getToken();
            String resultJson = sendAndDealResult(enterprise, methodName, sendInfoStr, result, appId, tocken);
            //LOGGER.info("scanStockAll resultJson:{}.", resultJson);
            JSONObject jsonObject = JSONObject.parseObject(resultJson);
            String transId = jsonObject.getString("transId");
            if(StringUtils.isEmpty(resultJson))
            {
                result.setMsg("库存信息下载失败");
            }else if(StringUtils.equals(jsonObject.getString("Success"),"false")){
                result.setMsg("库存信息下载失败");
            }
            else {
                jsonObject = JSONObject.parseObject(jsonObject.getString("Content"));
                JSONArray jsonArray = (JSONArray)JSONArray.parse(jsonObject.getString("master"));
                for(int i = 0;i < jsonArray.size();i++){
                    jsonObject = (JSONObject) jsonArray.get(i);
                    String isstock = jsonObject.getString("isstock");
                    if(StringUtils.equals(isstock,"0"))
                    {
                        checkScan = false;
                    }
                    DrugEnterpriseResult confirmResult = getScanStockConfirm(transId,"",enterprise);
                    if(StringUtils.equals(confirmResult.getMsg(),"fail"))
                    {
                        result.setCode(DrugEnterpriseResult.FAIL);
                        result.setMsg("库存下载确认反馈失败");
                        break;
                    }
                }
                if(!checkScan){
                    getFailResult(result, "当前药企下无药品库存");
                } else {
                    result.setMsg("调用[" + enterprise.getName() + "][ scanStockAll ]结果返回成功,有库存");
                }
            }
        }catch (Exception e){
            LOGGER.error("当前药企下没有药品库存",e);
            getFailResult(result, "当前药企下没有药品库存");
        }
        return result;
    }

    /**
     * @method  getDetailGroup
     * @description 将处方详情中的根据药品分组，叠加需求量
     * @date: 2019/7/10
     * @author: JRK
     * @param detailList 处方详情
     * @return java.util.Map<java.lang.Integer,recipe.drugsenterprise.bean.DetailDrugGroup>
     *     详情药品需求量分组
     */
    private Map<Integer, DetailDrugGroup> getDetailGroup(List<Recipedetail> detailList) {
        Map<Integer, DetailDrugGroup> result = new HashMap<>();
        DetailDrugGroup nowDetailDrugGroup;
        //遍历处方详情，通过drugId判断，相同药品下的需求量叠加
        for (Recipedetail recipedetail : detailList) {
            nowDetailDrugGroup = result.get(recipedetail.getDrugId());
            if(null == nowDetailDrugGroup){
                DetailDrugGroup newDetailDrugGroup = new DetailDrugGroup();
                result.put(recipedetail.getDrugId(), newDetailDrugGroup);
                newDetailDrugGroup.setDrugId(recipedetail.getDrugId());
                newDetailDrugGroup.setRecipeDetailId(recipedetail.getRecipeDetailId());
                newDetailDrugGroup.setSumUsage(recipedetail.getUseTotalDose());
            }else{
                //叠加需求量
                nowDetailDrugGroup.setSumUsage(nowDetailDrugGroup.getSumUsage()+ recipedetail.getUseTotalDose());
                result.put(recipedetail.getDrugId(), nowDetailDrugGroup);
            }
        }
        return result;
    }

    @Override
    public DrugEnterpriseResult syncEnterpriseDrug(DrugsEnterprise enterprise, List<Integer> drugIdList) {
        LOGGER.info("CommonSHRemoteService.syncEnterpriseDrug:药企ID为{}.", enterprise.getId());
        DrugEnterpriseResult result = DrugEnterpriseResult.getSuccess();
        SaleDrugListDAO saleDrugListDAO = DAOFactory.getDAO(SaleDrugListDAO.class);
        String depName = enterprise.getName();
        String transId = "";
        try{
            //最终发给药企的json数据
            Map<String, Object> sendInfo = new HashMap<>(1);
            List<CommonSHScanStockReqDto> commonSHScanStockReqDto = new  ArrayList<CommonSHScanStockReqDto>();
            int pageIndex = 1;
            while (true){
                CommonSHScanStockReqDto commonSHScanStockReq = new CommonSHScanStockReqDto();
                commonSHScanStockReq.setGoods("");
                commonSHScanStockReq.setPageIndex(pageIndex);
                commonSHScanStockReq.setPageSize(200);
                commonSHScanStockReqDto.add(commonSHScanStockReq);
                Map<String, Object> res= new HashMap<>(1);
                res.put("master",commonSHScanStockReqDto);
                sendInfo.put("Content", res);
                sendInfo.put("MsgType","QZ004");
                sendInfo.put("DataType","Json");
                Date date = new Date(System.currentTimeMillis());
                sendInfo.put("CreateTime",DateConversion.getDateFormatter(date,DateConversion.DEFAULT_DATE_TIME));
                String sendInfoStr = JSON.toJSONString(sendInfo, new SerializerFeature[]
                        { SerializerFeature.WriteMapNullValue, SerializerFeature.WriteNullStringAsEmpty });
                String methodName = "TransData";
                //LOGGER.info("发送[{}][{}]内容：{}", depName, methodName, sendInfoStr);

                //发送药企信息
                String appId = enterprise.getUserId();
                String tocken = enterprise.getToken();
                String resultJson = sendAndDealResult(enterprise, methodName, sendInfoStr, result, appId, tocken);
                JSONObject jsonObject = JSONObject.parseObject(resultJson);
                transId = jsonObject.getString("TransId");
                if(StringUtils.isEmpty(resultJson))
                {
                    result.setMsg("库存信息下载失败");
                }else if(StringUtils.equals(jsonObject.getString("Success"),"false")){
                    result.setMsg("库存信息下载失败");
                }
                else {
                    jsonObject = JSONObject.parseObject(jsonObject.getString("Content"));
                    pageIndex = jsonObject.getInteger("nextpage");
                    jsonObject = JSONObject.parseObject(jsonObject.getString("data"));
                    JSONArray jsonArray = (JSONArray)JSONArray.parse(jsonObject.getString("master"));
                    for(int i = 0;i < jsonArray.size();i++){
                        SaleDrugList saleDrug = new SaleDrugList();
                        jsonObject = (JSONObject) jsonArray.get(i);
                        String isstock = jsonObject.getString("isstock");
                        saleDrug = saleDrugListDAO.getByOrganIdAndDrugCode(enterprise.getId(), jsonObject.getString("goods"));
                        if(saleDrug == null)
                        {
                            SaleDrugList saleDrug2 = new SaleDrugList();
                            saleDrug2.setOrganDrugCode(jsonObject.getString("goods"));
                            saleDrug2.setDrugSpec(jsonObject.getString("spec"));
                            saleDrug2.setDrugName(jsonObject.getString("goodsname"));
                            saleDrug2.setSaleName(jsonObject.getString("gname"));
                            saleDrug2.setPrice(new BigDecimal(jsonObject.getString("LPRC")));
                            if(StringUtils.equals(jsonObject.getString("isstock"),"0"))
                            {
                                saleDrug2.setInventory(new BigDecimal("0"));
                            }else {
                                saleDrug2.setInventory(new BigDecimal("1"));
                            }
                            saleDrug2.setOrganId(enterprise.getId());
                            saleDrug2.setStatus(1);
                            saleDrug2.setCreateDt(DateConversion.getFormatDate(date,DateConversion.DEFAULT_DATE_TIME));
                            saleDrug2.setLastModify(DateConversion.getFormatDate(date,DateConversion.DEFAULT_DATE_TIME));
                            saleDrugListDAO.save(saleDrug2);
                            result.setMsg("调用[" + enterprise.getName() + "][ syncEnterpriseDrug ]结果返回成功,数据插入成功,药品ID:"+jsonObject.getString("goods")+".");
                        }else {
                            saleDrug.setOrganDrugCode(jsonObject.getString("goods"));
                            saleDrug.setDrugSpec(jsonObject.getString("spec"));
                            saleDrug.setDrugName(jsonObject.getString("goodsname"));
                            saleDrug.setSaleName(jsonObject.getString("gname"));
                            saleDrug.setPrice(new BigDecimal(jsonObject.getString("LPRC")));
                            if(StringUtils.equals(jsonObject.getString("isstock"),"0"))
                            {
                                saleDrug.setInventory(new BigDecimal("0"));
                            }else {
                                saleDrug.setInventory(new BigDecimal("1"));
                            }
                            saleDrug.setOrganId(enterprise.getId());
                            saleDrug.setStatus(1);
                            saleDrug.setLastModify(DateConversion.getFormatDate(date,DateConversion.DEFAULT_DATE_TIME));
                            saleDrugListDAO.update(saleDrug);
                            result.setMsg("调用[" + enterprise.getName() + "][ syncEnterpriseDrug ]结果返回成功,数据更新成功,药品ID:"+jsonObject.getString("goods")+".");
                        }
                    }
                    getScanStockConfirm(transId,"",enterprise);
                    if(pageIndex == 0)
                    {
                        break;
                    }
                }
            }

        }catch (Exception e){
            getFailResult(result, "当前药企下药品库存不够");
            LOGGER.error("CommonSHRemoteService.syncEnterpriseDrug:药企ID为{},{}.", enterprise.getId(), e.getMessage(),e);
        }
        return result;
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
        return DrugEnterpriseConstant.COMPANY_COMMON_SH;
    }
    /**
     * 获取wsdl调用客户端
     *
     * @param drugsEnterprise
     * @param method
     * @return
     */
    protected Call getCall(DrugsEnterprise drugsEnterprise, String method,String appId,String tocken) throws Exception {
        String wsdlUrl = drugsEnterprise.getBusinessUrl();
        Map<String,String> access_tocken = getSignature(appId,tocken);
        String nameSpaceUri = NAME_SPACE + method;
        String url = wsdlUrl + method + "?appId="+appId+"&token="+tocken+access_tocken+"&data=";
        //System.out.print(url);
        Call call = null;
        try {
            Service s = new Service();
            call = (Call) s.createCall();
            if (null != call) {
                //单位毫秒
                call.setTimeout(20000);
                call.setTargetEndpointAddress(new URL(wsdlUrl));
                call.setOperationName(new QName(NAME_SPACE, method));
                call.setSOAPActionURI(url);
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
    private String sendAndDealResult(DrugsEnterprise drugsEnterprise, String method, String sendInfoStr, DrugEnterpriseResult result,String appId,String tocken) {
        String drugEpName = drugsEnterprise.getName();
        String resultObj = null;
        try {
            String wsdlUrl = drugsEnterprise.getBusinessUrl();
            Map<String,String> access_tocken = getSignature(appId,tocken);
            String url = wsdlUrl + method;
            JSONObject jsonObject = (JSONObject)JSONObject.parse(sendInfoStr);
            HttpPost post = new HttpPost(url);
            List<BasicNameValuePair> param = new ArrayList<BasicNameValuePair>();
            param.add(new BasicNameValuePair("appId",appId));
            param.add(new BasicNameValuePair("timestamp",access_tocken.get("timestamp")));
            param.add(new BasicNameValuePair("nonce",access_tocken.get("nonce")));
            param.add(new BasicNameValuePair("accesstoken",access_tocken.get("accesstoken")));
            param.add(new BasicNameValuePair("data",sendInfoStr));

            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(param,"UTF-8");
            entity.setContentType("application/x-www-form-urlencoded");
            post.setEntity(entity);
            CloseableHttpClient httpClient = HttpClientBuilder.create().build();
            HttpResponse response = httpClient.execute(post);
            resultObj = EntityUtils.toString(response.getEntity(),"UTF-8");

            if (null != resultObj && resultObj instanceof String) {
                    LOGGER.info("调用[{}][{}]结果返回={}", drugEpName, method, resultObj); } else {
                    LOGGER.error("调用[{}][{}]结果返回为空", drugEpName, method);
                    result.setMsg(drugEpName + "接口返回结果为空");
                    result.setCode(DrugEnterpriseResult.FAIL);
            }

        } catch (Exception e) {
            resultObj = null;
            LOGGER.error(drugEpName + " post method[{}] error ", method, e);
            result.setMsg(drugEpName + "接口调用出错");
            result.setCode(DrugEnterpriseResult.FAIL);
        }

        if (StringUtils.isNotEmpty(resultObj)) {
            Map resultMap = JSONUtils.parse(resultObj, Map.class);
            String resCode = MapValueUtil.getString(resultMap, "CODE");
            if (RESULT_SUCCESS.equals(resCode)) {
                LOGGER.info("CommonSHRemoteService.sendAndDealResult 调用成功，{}");
            } else {
                result.setMsg("调用[" + drugEpName + "][" + method + "]失败.error:" + MapValueUtil.getString(resultMap, "message"));
                result.setCode(DrugEnterpriseResult.FAIL);
            }
        } else {
            result.setMsg(drugEpName + "接口调用返回为空");
            result.setCode(DrugEnterpriseResult.FAIL);
        }
        return resultObj;
    }

    /**
     * 获取配送信息
     * @return
     */
    public DrugEnterpriseResult getDistributionZt(String recipeId, DrugsEnterprise enterprise){
        DrugEnterpriseResult result = DrugEnterpriseResult.getSuccess();
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        Recipe nowRecipe = recipeDAO.getByRecipeId(Integer.parseInt(recipeId));
        String depName=enterprise.getName();
        if (StringUtils.isEmpty(recipeId)) {
            result.setMsg("处方ID参数为空");
            result.setCode(DrugEnterpriseResult.FAIL);
            return result;
        }

        //最终发给药企的json数据
        Map<String, Object> sendInfo = new HashMap<>(1);
        List<CommonSHDistributionReqDto> commonSHDistributionReqDto = new  ArrayList<CommonSHDistributionReqDto>();
        CommonSHDistributionReqDto commonSHDistribution = new CommonSHDistributionReqDto();
        commonSHDistribution.setBillno(recipeId);
        commonSHDistribution.setPageIndex(1);
        commonSHDistribution.setPageSize(200);
        commonSHDistribution.setStrdate(DateConversion.formatDate(nowRecipe.getCreateDate()));
        commonSHDistribution.setEnddate(DateConversion.formatDate(new Date(System.currentTimeMillis())));
        commonSHDistributionReqDto.add(commonSHDistribution);

        Map<String, Object> res= new HashMap<>(1);
        res.put("master",commonSHDistributionReqDto);
        sendInfo.put("Content", res);
        sendInfo.put("MsgType","QZ002");
        sendInfo.put("DataType","Json");
        Date date = new Date(System.currentTimeMillis());
        sendInfo.put("CreateTime",DateConversion.getDateFormatter(date,DateConversion.DEFAULT_DATE_TIME));
        String sendInfoStr = JSON.toJSONString(sendInfo, new SerializerFeature[]
                { SerializerFeature.WriteMapNullValue, SerializerFeature.WriteNullStringAsEmpty });
        String methodName = "TransData";
        //LOGGER.info("发送[{}][{}]内容：{}", depName, methodName, sendInfoStr);

        //发送药企信息
        String appId = enterprise.getUserId();
        String tocken = enterprise.getToken();
        String resultJson = sendAndDealResult(enterprise, methodName, sendInfoStr, result, appId, tocken);

        Map<String, String> resultMap = getCommonSHDistribution(resultJson, recipeId);
        DrugEnterpriseResult confirmResult = getDistributionZtConfirm(resultMap.get("transId"), resultMap.get("pkid"), enterprise);
        if (StringUtils.equals(confirmResult.getMsg(), "success")) {
            result.setMsg(resultMap.get("ddzt"));
        } else {
            result.setMsg("配送信息下载确认反馈失败");
            result.setCode(DrugEnterpriseResult.FAIL);
        }

        return result;
    }

    private Map<String,String> getCommonSHDistribution(String resultJson,String recipeId) {
        LOGGER.info("CommonSHRemoteService.getCommonSHDistribution 获取上海国药配送信息:{}.", JSONUtils.toString(recipeId));
        Map<String,String> map = new HashMap<String,String>();
        JSONObject jsonObject = JSONObject.parseObject(resultJson);
        String ddzt = "";
        String transId = "";
        String pkId = "";
        if(StringUtils.equals(jsonObject.getString("Success"),"true"))
        {
            jsonObject = JSONObject.parseObject(jsonObject.getString("Content"));
            JSONArray jsonArray = (JSONArray)JSONArray.parse(jsonObject.getString("master"));
            jsonObject = (JSONObject)jsonArray.get(0);
            ddzt = jsonObject.getString("ddzt");
            transId = jsonObject.getString("transId");
            pkId = jsonObject.getString("pkid");
            map.put("ddzt",ddzt);
            map.put("transId",transId);
            map.put("pkid",pkId);
        }else{
            LOGGER.info("CommonSHRemoteService.getCommonSHDistribution 获取上海国药配送信息失败.", JSONUtils.toString(recipeId));
        }
        return map;
    }

    /**
     * 配送信息下载确认反馈
     * @return
     */
    public DrugEnterpriseResult getDistributionZtConfirm(String transId,String pkId,DrugsEnterprise enterprise){
        DrugEnterpriseResult result = DrugEnterpriseResult.getSuccess();
        String depName=enterprise.getName();
        if (StringUtils.isEmpty(transId)) {
            result.setMsg("配送信息确认反馈接口批处理参数为空");
            result.setCode(DrugEnterpriseResult.FAIL);
            return result;
        }

        //最终发给药企的json数据
        Map<String, Object> sendInfo = new HashMap<>(1);
        List<CommonSHDistributionConfirmReqDto> commonSHDistributionConfirmReqDto = new  ArrayList<CommonSHDistributionConfirmReqDto>();
        CommonSHDistributionConfirmReqDto commonSHDistributionConfirm = new CommonSHDistributionConfirmReqDto();
        commonSHDistributionConfirm.setITEMID(pkId);
        commonSHDistributionConfirm.setTransId(transId);
        commonSHDistributionConfirmReqDto.add(commonSHDistributionConfirm);

        Map<String, Object> res= new HashMap<>(1);
        res.put("master",commonSHDistributionConfirmReqDto);
        sendInfo.put("Content", res);
        sendInfo.put("MsgType","QZ003");
        sendInfo.put("DataType","Json");
        Date date = new Date(System.currentTimeMillis());
        sendInfo.put("CreateTime",DateConversion.getDateFormatter(date,DateConversion.DEFAULT_DATE_TIME));
        String sendInfoStr = JSON.toJSONString(sendInfo, new SerializerFeature[]
                { SerializerFeature.WriteMapNullValue, SerializerFeature.WriteNullStringAsEmpty });
        String methodName = "TransData";
        //LOGGER.info("发送[{}][{}]内容：{}", depName, methodName, sendInfoStr);

        //发送药企信息
        String appId = enterprise.getUserId();
        String tocken = enterprise.getToken();
        String resultJson = sendAndDealResult(enterprise, methodName, sendInfoStr, result, appId, tocken);
        JSONObject jsonObject = JSONObject.parseObject(resultJson);
        if(StringUtils.equals(jsonObject.getString("Success"),"true"))
        {
            result.setMsg("success");
        }else{
            result.setMsg("fail");
        }
        return result;
    }

    /**
     * 库存信息下载确认反馈
     * @return
     */
    public DrugEnterpriseResult getScanStockConfirm(String transId,String goods,DrugsEnterprise enterprise){
        DrugEnterpriseResult result = DrugEnterpriseResult.getSuccess();
        String depName=enterprise.getName();
        if (StringUtils.isEmpty(transId)) {
            result.setMsg("库存信息确认反馈接口批处理参数为空");
            result.setCode(DrugEnterpriseResult.FAIL);
            return result;
        }

        //最终发给药企的json数据
        Map<String, Object> sendInfo = new HashMap<>(1);
        List<CommonSHScanStockConfirmReqDto> commonSHScanStockConfirmReq = new  ArrayList<CommonSHScanStockConfirmReqDto>();
        CommonSHScanStockConfirmReqDto commonSHScanStockConfirmReqDto = new CommonSHScanStockConfirmReqDto();
        commonSHScanStockConfirmReqDto.setITEMID(goods);
        commonSHScanStockConfirmReqDto.setTransId(transId);
        commonSHScanStockConfirmReq.add(commonSHScanStockConfirmReqDto);

        Map<String, Object> res= new HashMap<>(1);
        res.put("master",commonSHScanStockConfirmReq);
        sendInfo.put("Content", res);
        sendInfo.put("MsgType","QZ005");
        sendInfo.put("DataType","Json");
        Date date = new Date(System.currentTimeMillis());
        sendInfo.put("CreateTime",DateConversion.getDateFormatter(date,DateConversion.DEFAULT_DATE_TIME));
        String sendInfoStr = JSON.toJSONString(sendInfo, new SerializerFeature[]
                { SerializerFeature.WriteMapNullValue, SerializerFeature.WriteNullStringAsEmpty });
        String methodName = "TransData";
        //LOGGER.info("发送[{}][{}]内容：{}", depName, methodName, sendInfoStr);

        //发送药企信息
        String appId = enterprise.getUserId();
        String tocken = enterprise.getToken();
        String resultJson = sendAndDealResult(enterprise, methodName, sendInfoStr, result, appId, tocken);
        JSONObject jsonObject = JSONObject.parseObject(resultJson);
        if(StringUtils.equals(jsonObject.getString("Success"),"true"))
        {
            result.setMsg("success");
        }else{
            result.setMsg("fail");
        }
        return result;
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
