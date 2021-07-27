package recipe.service;

import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.ngari.base.property.service.IConfigurationCenterUtilsService;
import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.base.PatientBaseInfo;
import com.ngari.his.patient.mode.PatientQueryRequestTO;
import com.ngari.his.patient.service.IPatientHisService;
import com.ngari.his.recipe.mode.ChronicDiseaseListReqTO;
import com.ngari.his.recipe.mode.ChronicDiseaseListResTO;
import com.ngari.his.recipe.mode.PatientChronicDiseaseRes;
import com.ngari.his.recipe.mode.PatientDiagnoseTO;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.patient.service.PatientService;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.common.RecipeResultBean;
import com.ngari.recipe.drugsenterprise.model.DepDetailBean;
import com.ngari.recipe.drugsenterprise.model.DepListBean;
import com.ngari.recipe.entity.*;
import com.ngari.recipe.recipe.model.RankShiftList;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import com.ngari.revisit.RevisitAPI;
import com.ngari.revisit.common.model.RevisitExDTO;
import com.ngari.revisit.common.service.IRevisitExService;
import ctd.controller.exception.ControllerException;
import ctd.dictionary.DictionaryController;
import ctd.persistence.DAOFactory;
import ctd.persistence.exception.DAOException;
import ctd.util.AppContextHolder;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.ApplicationUtils;
import recipe.bean.DrugEnterpriseResult;
import recipe.constant.*;
import recipe.dao.ChronicDiseaseDAO;
import recipe.dao.RecipeDAO;
import recipe.dao.RecipeDetailDAO;
import recipe.dao.SaleDrugListDAO;
import recipe.drugsenterprise.RemoteDrugEnterpriseService;
import recipe.hisservice.RecipeToHisService;
import recipe.service.common.RecipeCacheService;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 患者端服务
 * company: ngarihealth
 *
 * @author: 0184/yu_yun
 * @date:2017/6/30.
 */
@RpcBean("recipePatientService")
public class RecipePatientService extends RecipeBaseService {

    /**
     * LOGGER
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(RecipePatientService.class);
    @Autowired
    private ChronicDiseaseDAO chronicDiseaseDAO;
    private String msg;

    /**
     * 根据取药方式过滤药企
     *
     * @param recipeIds
     * @param payModes
     * @return
     */
    @RpcService
    public RecipeResultBean filterSupportDepList(List<Integer> recipeIds, final List<Integer> payModes) {
        RecipeResultBean result = this.findSupportDepList(1, recipeIds);
        RecipeResultBean backResult = RecipeResultBean.getSuccess();
        backResult.setCode(result.getCode());
        backResult.setMsg(result.getMsg());
        Object depListObj = result.getObject();
        DepListBean newBean = new DepListBean();
        if (null != depListObj && depListObj instanceof DepListBean) {
            DepListBean depListBean = (DepListBean) depListObj;
            newBean.setList(Lists.newArrayList(Collections2.filter(depListBean.getList(), new Predicate<DepDetailBean>() {
                @Override
                public boolean apply(@Nullable DepDetailBean input) {
                    if (payModes.contains(input.getPayMode())) {
                        return true;
                    }
                    return false;
                }
            })));
        }
        backResult.setObject(newBean);

        return backResult;
    }

    /**
     * 获取供应商列表
     *
     * @param findDetail 1:表示获取详情，0：表示判断是否需要展示供应商具体列表-开处方时查询库存
     * @param recipeIds
     */
    @RpcService
    public RecipeResultBean findSupportDepList(int findDetail, List<Integer> recipeIds) {
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        RecipeDetailDAO detailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
        RecipeService recipeService = ApplicationUtils.getRecipeService(RecipeService.class);
        RemoteDrugEnterpriseService remoteDrugService = ApplicationUtils.getRecipeService(RemoteDrugEnterpriseService.class);
        RecipeCacheService cacheService = ApplicationUtils.getRecipeService(RecipeCacheService.class);

        RecipeResultBean resultBean = RecipeResultBean.getSuccess();
        List<Recipe> recipeList = recipeDAO.findByRecipeIds(recipeIds);
        if (CollectionUtils.isEmpty(recipeList)) {
            resultBean.setCode(RecipeResultBean.FAIL);
            resultBean.setMsg("处方不存在");
            return resultBean;
        }

        DepListBean depListBean = new DepListBean();
        Integer organId = recipeList.get(0).getClinicOrgan();
        BigDecimal totalMoney = BigDecimal.ZERO;
        for (Recipe recipe : recipeList) {
            if (!recipe.getClinicOrgan().equals(organId)) {
                resultBean.setCode(RecipeResultBean.FAIL);
                resultBean.setMsg("选择处方的机构不一致，请重新选择");
                return resultBean;
            }

            totalMoney = totalMoney.add(recipe.getTotalMoney());
        }

        List<DrugsEnterprise> depList = recipeService.findSupportDepList(recipeIds, organId, null, false, null);
        LOGGER.info("findSupportDepList recipeIds={}, 匹配到药企数量[{}]", JSONUtils.toString(recipeIds), depList.size());
        if (CollectionUtils.isNotEmpty(depList)) {
            //设置默认值
            depListBean.setSigle(true);
            //只需要查询是否存在多个供应商
            if (0 == findDetail && depList.size() > 1) {
                depListBean.setSigle(false);
                resultBean.setObject(depListBean);
                //开处方阶段---判断药企库存这里就返回了
                return resultBean;
            }

            //该详情数据包含了所有处方的详情，可能存在同一种药品数据
            List<Recipedetail> details = detailDAO.findByRecipeIds(recipeIds);
            List<Recipedetail> backDetails = new ArrayList<>(details.size());
            Map<Integer, Double> drugIdCountRel = Maps.newHashMap();
            Recipedetail backDetail;
            for (Recipedetail recipedetail : details) {
                Integer drugId = recipedetail.getDrugId();
                if (drugIdCountRel.containsKey(drugId)) {
                    drugIdCountRel.put(drugId, drugIdCountRel.get(recipedetail.getDrugId()) + recipedetail.getUseTotalDose());
                } else {
                    backDetail = new Recipedetail();
                    backDetail.setDrugId(recipedetail.getDrugId());
                    backDetail.setDrugName(recipedetail.getDrugName());
                    backDetail.setDrugUnit(recipedetail.getDrugUnit());
                    backDetail.setDrugSpec(recipedetail.getDrugSpec());
                    backDetail.setUseDoseUnit(recipedetail.getUseDoseUnit());
                    backDetails.add(backDetail);
                    drugIdCountRel.put(drugId, recipedetail.getUseTotalDose());
                }
            }

            //判断是否需要展示供应商详情列表，如果遇上钥世圈的药企，则都展示供应商列表
            List<DepDetailBean> depDetailList = new ArrayList<>();
            for (DrugsEnterprise dep : depList) {
                //钥世圈需要从接口获取支持药店列表
                if (DrugEnterpriseConstant.COMPANY_YSQ.equals(dep.getCallSys()) || DrugEnterpriseConstant.COMPANY_PHARMACY.equals(dep.getCallSys()) || DrugEnterpriseConstant.COMPANY_ZFB.equals(dep.getCallSys())) {
                    //需要从接口获取药店列表
                    DrugEnterpriseResult drugEnterpriseResult = remoteDrugService.findSupportDep(recipeIds, null, dep);
                    if (DrugEnterpriseResult.SUCCESS.equals(drugEnterpriseResult.getCode())) {
                        Object listObj = drugEnterpriseResult.getObject();
                        if (null != listObj && listObj instanceof List) {
                            List<DepDetailBean> ysqList = (List) listObj;
                            for (DepDetailBean d : ysqList) {
                                d.setDepId(dep.getId());
                            }
                            depDetailList.addAll(ysqList);
                        }
                        //设置样式
                        resultBean.setStyle(drugEnterpriseResult.getStyle());
                    }
                } else {
                    parseDrugsEnterprise(dep, totalMoney, depDetailList);
                    //如果是价格自定义的药企，则需要设置单独价格
                    SaleDrugListDAO saleDrugListDAO = DAOFactory.getDAO(SaleDrugListDAO.class);
                    List<Integer> drugIds = Lists.newArrayList(drugIdCountRel.keySet());
                    if (Integer.valueOf(0).equals(dep.getSettlementMode()) && (RecipeBussConstant.DEP_SUPPORT_ONLINE.equals(dep.getPayModeSupport()) || RecipeBussConstant.DEP_SUPPORT_ALL.equals(dep.getPayModeSupport()))) {
                        List<SaleDrugList> saleDrugLists = saleDrugListDAO.findByOrganIdAndDrugIds(dep.getId(), drugIds);
                        if (CollectionUtils.isNotEmpty(saleDrugLists)) {
                            BigDecimal total = BigDecimal.ZERO;
                            try {
                                for (SaleDrugList saleDrug : saleDrugLists) {
                                    //保留3位小数
                                    total = total.add(saleDrug.getPrice().multiply(new BigDecimal(drugIdCountRel.get(saleDrug.getDrugId()))).divide(BigDecimal.ONE, 3, RoundingMode.UP));
                                }
                            } catch (Exception e) {
                                LOGGER.error("findSupportDepList 重新计算药企ID为[{}]的结算价格出错. drugIds={}", dep.getId(), JSONUtils.toString(drugIds), e);
                                //此处应该要把出错的药企从返回列表中剔除
                                depDetailList.remove(depDetailList.size() - 1);
                                continue;
                            }

                            //重置药企处方价格
                            for (DepDetailBean depDetailBean : depDetailList) {
                                if (depDetailBean.getDepId().equals(dep.getId())) {
                                    depDetailBean.setRecipeFee(total);
                                    break;
                                }
                            }
                        }
                    }

                }

                //只是查询的话减少处理量
                if (0 == findDetail && depDetailList.size() > 1) {
                    depListBean.setSigle(false);
                    break;
                }
            }

            //有可能钥世圈支持配送，实际从接口处没有获取到药店
            if (CollectionUtils.isEmpty(depDetailList)) {
                resultBean.setCode(RecipeResultBean.FAIL);
                resultBean.setMsg("很抱歉，当前库存不足无法购买，请联系客服：" + cacheService.getParam(ParameterConstant.KEY_CUSTOMER_TEL, RecipeSystemConstant.CUSTOMER_TEL));
                return resultBean;
            }

            depListBean.setList(depDetailList);
            resultBean.setObject(depListBean);
            //只需要查询是否存在多个供应商， 就不需要设置其他额外信息
            if (0 == findDetail) {
                return resultBean;
            }

            if (depDetailList.size() > 1) {
                depListBean.setSigle(false);
            }

            //重置药品数量
            for (Recipedetail recipedetail : backDetails) {
                recipedetail.setUseTotalDose(drugIdCountRel.get(recipedetail.getDrugId()));
            }
            depListBean.setDetails(ObjectCopyUtils.convert(backDetails, RecipeDetailBean.class));
            //患者处方取药方式提示
            if (recipeIds.size() <= 1) {
                depListBean.setRecipeGetModeTip(RecipeServiceSub.getRecipeGetModeTip(recipeList.get(0)));
            }

        } else {
            resultBean.setCode(RecipeResultBean.FAIL);
            resultBean.setMsg("很抱歉，未能匹配到可以支持的药企，请联系客服：" + cacheService.getParam(ParameterConstant.KEY_CUSTOMER_TEL, RecipeSystemConstant.CUSTOMER_TEL));
        }

        return resultBean;
    }



    /**
     * 查询处方无库存药企药品信息
     *
     * @param recipeId
     * @return
     */
    @RpcService
    public RecipeResultBean findUnSupportDepList(Integer recipeId) {
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        RecipeService recipeService = ApplicationUtils.getRecipeService(RecipeService.class);

        RecipeResultBean resultBean = RecipeResultBean.getSuccess();
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        if (recipe == null) {
            resultBean.setCode(RecipeResultBean.FAIL);
            resultBean.setMsg("处方不存在");
            return resultBean;
        }

        Integer organId = recipe.getClinicOrgan();
        List<DrugEnterpriseResult> unDepList = recipeService.findUnSupportDepList(recipeId, organId);
        LOGGER.info("findUnSupportDepList recipeId={}, 无库存药企信息[{}]", JSONUtils.toString(recipeId), JSONObject.toJSONString(unDepList));
        if (CollectionUtils.isNotEmpty(unDepList)) {
            resultBean.setCode(RecipeResultBean.FAIL);
            List<String> drugList = new ArrayList<>();
            for (DrugEnterpriseResult result : unDepList){
                List<String> list = (List<String>) result.getObject();
                if (CollectionUtils.isNotEmpty(list)){
                    drugList.addAll((list));
                    break;
                }
            }
            for (DrugEnterpriseResult result : unDepList){
                List<String> list = (List<String>) result.getObject();
                // 有药品名称取交集
                if (CollectionUtils.isNotEmpty(list)){
                    drugList.retainAll(list);
                }else {
                    // 有一个不能返回具体无库存药品，不展示药品名称，返回药品信息为空
                    LOGGER.info("findUnSupportDepList recipeId={}, 药企未返回具体无库存药品信息[{}]", JSONUtils.toString(recipeId), JSONObject.toJSONString(result));
                    resultBean.setObject(null);
                    break;
                }
            }
            // 仅各药企库存不足药品是包含关系才展示，即交集不为空，且交集结果至少是某一个药企无库存药品
            if (CollectionUtils.isNotEmpty(drugList)){
                Collections.sort(drugList);
                String retainStr = drugList.toString();
                Boolean showDrug = false;
                for (DrugEnterpriseResult result : unDepList){
                    List<String> list = (List<String>) result.getObject();
                    Collections.sort(list);
                    String listStr = list.toString();
                    if (retainStr.equals(listStr)){
                        showDrug = true;
                        break;
                    }
                }
                if (showDrug){
                    resultBean.setObject(drugList);
                }else {
                    resultBean.setObject(null);
                }
            }else {
                resultBean.setObject(null);
            }
        }
        LOGGER.info("findUnSupportDepList recipeId={}, 无库存药企药品信息取交集结果[{}]", JSONUtils.toString(recipeId), JSONObject.toJSONString(resultBean));
        return resultBean;
    }
    /**
     * 查询处方无库存药企药品信息
     *
     * @param recipeId
     * @return
     */
    @RpcService
    public RecipeResultBean findUnSupportDepList(Integer recipeId,List<DrugEnterpriseResult> unDepList) {
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);


        RecipeResultBean resultBean = RecipeResultBean.getSuccess();
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        if (recipe == null) {
            resultBean.setCode(RecipeResultBean.FAIL);
            resultBean.setMsg("处方不存在");
            return resultBean;
        }

        if (CollectionUtils.isNotEmpty(unDepList)) {
            resultBean.setCode(RecipeResultBean.FAIL);
            List<String> drugList = new ArrayList<>();
            for (DrugEnterpriseResult result : unDepList){
                List<String> list = (List<String>) result.getObject();
                if (CollectionUtils.isNotEmpty(list)){
                    drugList.addAll((list));
                    break;
                }
            }
            for (DrugEnterpriseResult result : unDepList){
                List<String> list = (List<String>) result.getObject();
                // 有药品名称取交集
                if (CollectionUtils.isNotEmpty(list)){
                    drugList.retainAll(list);
                }else {
                    // 有一个不能返回具体无库存药品，不展示药品名称，返回药品信息为空
                    LOGGER.info("findUnSupportDepList recipeId={}, 药企未返回具体无库存药品信息[{}]", JSONUtils.toString(recipeId), JSONObject.toJSONString(result));
                    resultBean.setObject(null);
                    break;
                }
            }
            // 仅各药企库存不足药品是包含关系才展示，即交集不为空，且交集结果至少是某一个药企无库存药品
            if (CollectionUtils.isNotEmpty(drugList)){
                Collections.sort(drugList);
                String retainStr = drugList.toString();
                Boolean showDrug = false;
                for (DrugEnterpriseResult result : unDepList){
                    List<String> list = (List<String>) result.getObject();
                    Collections.sort(list);
                    String listStr = list.toString();
                    if (retainStr.equals(listStr)){
                        showDrug = true;
                        break;
                    }
                }
                if (showDrug){
                    resultBean.setObject(drugList);
                }else {
                    resultBean.setObject(null);
                }
            }else {
                resultBean.setObject(null);
            }
        }
        LOGGER.info("findUnSupportDepList recipeId={}, 无库存药企药品信息取交集结果[{}]", JSONUtils.toString(recipeId), JSONObject.toJSONString(resultBean));
        return resultBean;
    }

    private void parseDrugsEnterprise(DrugsEnterprise dep, BigDecimal totalMoney, List<DepDetailBean> depDetailList) {
        RecipeCacheService cacheService = ApplicationUtils.getRecipeService(RecipeCacheService.class);

        DepDetailBean depDetailBean = new DepDetailBean();
        depDetailBean.setDepId(dep.getId());
        depDetailBean.setDepName(dep.getName());
        depDetailBean.setRecipeFee(totalMoney);
        Integer supportMode = dep.getPayModeSupport();
        String giveModeText = "";
        List<Integer> payModeList = new ArrayList<>();
        //配送模式支持 0:不支持 1:线上付款 2:货到付款 3:药店取药 8:货到付款和药店取药 9:都支持 7配送到家和药店取药
        if (RecipeBussConstant.DEP_SUPPORT_ONLINE.equals(supportMode)) {
            payModeList.add(RecipeBussConstant.PAYMODE_ONLINE);
            payModeList.add(RecipeBussConstant.DEP_SUPPORT_ONLINE_TFDS);
            giveModeText = "配送到家";
            //无法配送时间文案提示
            depDetailBean.setUnSendTitle(cacheService.getParam(ParameterConstant.KEY_RECIPE_UNSEND_TIP));
        } else if (RecipeBussConstant.DEP_SUPPORT_COD.equals(supportMode)) {
            payModeList.add(RecipeBussConstant.PAYMODE_COD);
            giveModeText = "配送到家";
        } else if (RecipeBussConstant.DEP_SUPPORT_TFDS.equals(supportMode)) {
            payModeList.add(RecipeBussConstant.PAYMODE_TFDS);
            payModeList.add(RecipeBussConstant.DEP_SUPPORT_ONLINE_TFDS);
        } else if (RecipeBussConstant.DEP_SUPPORT_COD_TFDS.equals(supportMode)) {
            payModeList.add(RecipeBussConstant.PAYMODE_COD);
            payModeList.add(RecipeBussConstant.PAYMODE_TFDS);
        } else if (RecipeBussConstant.DEP_SUPPORT_ALL.equals(supportMode)) {
            payModeList.add(RecipeBussConstant.PAYMODE_ONLINE);
            payModeList.add(RecipeBussConstant.PAYMODE_COD);
            payModeList.add(RecipeBussConstant.PAYMODE_TFDS);
            payModeList.add(RecipeBussConstant.DEP_SUPPORT_ONLINE_TFDS);
            //无法配送时间文案提示
            depDetailBean.setUnSendTitle(cacheService.getParam(ParameterConstant.KEY_RECIPE_UNSEND_TIP));
        } else if (RecipeBussConstant.DEP_SUPPORT_ONLINE_TFDS.equals(supportMode)) {
            //配送到家
            payModeList.add(RecipeBussConstant.PAYMODE_ONLINE);
            //药店取药
            payModeList.add(RecipeBussConstant.PAYMODE_TFDS);
        } else if (RecipeBussConstant.DEP_SUPPORT_UNKNOW.equals(supportMode)) {
            payModeList.add(RecipeBussConstant.DEP_SUPPORT_UNKNOW);
        }
        if (CollectionUtils.isNotEmpty(payModeList)) {
            depDetailBean.setPayMode(payModeList.get(0));
        }
        depDetailBean.setGiveModeText(giveModeText);
        depDetailList.add(depDetailBean);
    }

    /**
     * 获取患者特慢病病种列表
     *
     * @return
     */
    @RpcService
    public List<ChronicDiseaseListResTO> findPatientChronicDiseaseList(Integer organId, String mpiId) {
        LOGGER.info("findPatientChronicDiseaseList organId={},mpiId={}", organId, mpiId);
        PatientService patientService = ApplicationUtils.getBasicService(PatientService.class);
        PatientDTO patientDTO = patientService.get(mpiId);
        if (patientDTO == null) {
            throw new DAOException(609, "找不到该患者");
        }
        List<ChronicDiseaseListResTO> list = Lists.newArrayList();
        IConfigurationCenterUtilsService configurationService = ApplicationUtils.getBaseService(IConfigurationCenterUtilsService.class);
        Integer diseaseType = (Integer) configurationService.getConfiguration(organId, "recipeChooseChronicDisease");
        if (3 == diseaseType) {
            List<ChronicDisease> chronicDiseaseList = chronicDiseaseDAO.findChronicDiseasesByOrganId(diseaseType.toString());
            list = ObjectCopyUtils.convert(chronicDiseaseList, ChronicDiseaseListResTO.class);
            return list;
        } else {
            RecipeToHisService service = AppContextHolder.getBean("recipeToHisService", RecipeToHisService.class);
            ChronicDiseaseListReqTO req = new ChronicDiseaseListReqTO();
            PatientBaseInfo patientBaseInfo = new PatientBaseInfo();
            patientBaseInfo.setPatientName(patientDTO.getPatientName());
            patientBaseInfo.setCertificate(patientDTO.getCertificate());
            patientBaseInfo.setCertificateType(patientDTO.getCertificateType());
            req.setPatient(patientBaseInfo);
            req.setOrganId(organId);
            HisResponseTO<PatientChronicDiseaseRes> res = service.findPatientChronicDiseaseList(req);
            if (res != null && !("200".equals(res.getMsgCode()))) {
                String msg = "接口异常";
                if (StringUtils.isNotEmpty(res.getMsg())) {
                    msg = msg + ":" + res.getMsg();
                }
                throw new DAOException(609, msg);
            }
            if (res == null || res.getData() == null) {
                return list;
            }
            return res.getData().getChronicDiseaseListResTOs();
        }
    }

    /**
     * 获取患者特慢病病种列表
     *
     * @return
     */
    @RpcService
    public Map<String,Object> findPatientChronicDiseaseListNew(Integer organId, String mpiId) {
        LOGGER.info("findPatientChronicDiseaseListNew organId={},mpiId={}", organId, mpiId);
        Map<String,Object> result = Maps.newHashMap();
        PatientService patientService = ApplicationUtils.getBasicService(PatientService.class);
        PatientDTO patientDTO = patientService.get(mpiId);
        if (patientDTO == null) {
            throw new DAOException(609, "找不到该患者");
        }
        List<ChronicDiseaseListResTO> list = Lists.newArrayList();
        IConfigurationCenterUtilsService configurationService = ApplicationUtils.getBaseService(IConfigurationCenterUtilsService.class);
        // { "id": 1, "text": "无" , "locked": true},
        //        { "id": 2, "text": "特慢病病种" },
        //        { "id": 3, "text": "重症病种" },
        //        { "id": 4, "text": "慢病病种" },
        //        { "id": 5, "text": "重医大附属二院" }
        Integer diseaseType = (Integer) configurationService.getConfiguration(organId, "recipeChooseChronicDisease");
        result.put("recipeChooseChronicDisease",diseaseType);
        if (3 == diseaseType) {
            List<ChronicDisease> chronicDiseaseList = chronicDiseaseDAO.findChronicDiseasesByOrganId(diseaseType.toString());
            list = ObjectCopyUtils.convert(chronicDiseaseList, ChronicDiseaseListResTO.class);
        } else {
            if (1 == diseaseType){
                result.put("chronicDiseaseList",list);
                return result;
            }
            RecipeToHisService service = AppContextHolder.getBean("recipeToHisService", RecipeToHisService.class);
            ChronicDiseaseListReqTO req = new ChronicDiseaseListReqTO();
            PatientBaseInfo patientBaseInfo = new PatientBaseInfo();
            patientBaseInfo.setPatientName(patientDTO.getPatientName());
            patientBaseInfo.setCertificate(patientDTO.getCertificate());
            patientBaseInfo.setCertificateType(patientDTO.getCertificateType());
            req.setPatient(patientBaseInfo);
            req.setOrganId(organId);
            HisResponseTO<PatientChronicDiseaseRes> res = service.findPatientChronicDiseaseList(req);
            if (res != null && !("200".equals(res.getMsgCode()))) {
                String msg = "接口异常";
                if (StringUtils.isNotEmpty(res.getMsg())) {
                    msg = msg + ":" + res.getMsg();
                }
                throw new DAOException(609, msg);
            }
            if (res != null && res.getData() != null) {
                list = res.getData().getChronicDiseaseListResTOs();
                try {
                    if (CollectionUtils.isNotEmpty(list)&& (5 == diseaseType)){
                        //第一层
                        List<RankShiftList> rankShiftList = Lists.newArrayList();
                        Map<String, List<ChronicDiseaseListResTO>> flagMap = list.stream().collect(Collectors.groupingBy(ChronicDiseaseListResTO::getChronicDiseaseFlag));
                        Map<String, String> codeNameMap = list.stream().collect(Collectors.toMap(ChronicDiseaseListResTO::getChronicDiseaseCode, ChronicDiseaseListResTO::getChronicDiseaseName, (k1, k2) -> k1));
                        flagMap.forEach((k,v)->{
                            RankShiftList rank = new RankShiftList();
                            rank.setChronicDiseaseFlag(k);
                            try {
                                rank.setChronicDiseaseFlagText(DictionaryController.instance().get("eh.cdr.dictionary.ChronicDiseaseFlag").getText(k));
                            } catch (ControllerException e) {
                                LOGGER.error("findPatientChronicDiseaseListNew error",e);
                            }
                            //第二层
                            Map<String, List<ChronicDiseaseListResTO>> codeMap = v.stream().collect(Collectors.groupingBy(ChronicDiseaseListResTO::getChronicDiseaseCode));
                            List<RankShiftList> rankShiftList1 = Lists.newArrayList();
                            codeMap.forEach((k1,k2)->{
                                RankShiftList rank1 = new RankShiftList();
                                rank1.setChronicDiseaseCode(k1);
                                rank1.setChronicDiseaseName(codeNameMap.get(k1));
                                //第三层
                                List<RankShiftList> rankShiftList2 = Lists.newArrayList();
                                for (ChronicDiseaseListResTO resTO:k2){
                                    if (StringUtils.isNotEmpty(resTO.getComplication())){
                                        RankShiftList rankShift2 = new RankShiftList();
                                        rankShift2.setComplication(resTO.getComplication());
                                        rankShiftList2.add(rankShift2);
                                    }
                                }
                                rank1.setRankShiftList(rankShiftList2);
                                rankShiftList1.add(rank1);
                            });
                            rank.setRankShiftList(rankShiftList1);
                            rankShiftList.add(rank);
                        });
                        result.put("rankShiftList",rankShiftList);
                    }
                } catch (Exception e) {
                    LOGGER.error("findPatientChronicDiseaseListNew error",e);
                }
                result.put("medicalType",res.getData().getPatientType());
                result.put("medicalTypeText",res.getData().getPatientTypeText());
            }
        }
        result.put("chronicDiseaseList",list);
        return result;
    }

    /**
     * 获取患者诊断比较结果
     * 过期 已经迁移到医疗协同项目
     *
     * @return
     */
    @RpcService
    @Deprecated
    public void findPatientDiagnose(PatientDiagnoseTO request) {
        LOGGER.info("findPatientDiagnose request={}", JSONUtils.toString(request));
        PatientService patientService = ApplicationUtils.getBasicService(PatientService.class);
        PatientDTO patientDTO = patientService.get(request.getMpi());
        if (null == patientDTO) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "找不到该患者");
        }
        PatientBaseInfo patientBaseInfo = new PatientBaseInfo();
        BeanUtils.copyProperties(patientDTO, patientBaseInfo);
        patientBaseInfo.setMpi(patientDTO.getMpiId());
        request.setPatient(patientBaseInfo);
        RecipeToHisService service = AppContextHolder.getBean("recipeToHisService", RecipeToHisService.class);
        service.findPatientDiagnose(request);
    }

    /**
     * 查询线下患者信息接口
     *
     * @return
     */
    @RpcService
    public PatientQueryRequestTO queryPatientForHis(Integer organId, String mpiId) {
        LOGGER.info("queryPatientForHis organId={},mpiId={}", organId, mpiId);
        PatientService patientService = ApplicationUtils.getBasicService(PatientService.class);
        IPatientHisService iPatientHisService = AppContextHolder.getBean("his.iPatientHisService", IPatientHisService.class);
        PatientDTO patient = patientService.get(mpiId);
        if (patient == null) {
            throw new DAOException(609, "找不到该患者");
        }
        try {
            PatientQueryRequestTO req = new PatientQueryRequestTO();
            req.setOrgan(organId);
            req.setPatientName(patient.getPatientName());
            req.setCertificateType(patient.getCertificateType());
            req.setCertificate(patient.getCertificate());
            LOGGER.info("queryPatientForHis req={}", JSONUtils.toString(req));
            HisResponseTO<PatientQueryRequestTO> res = iPatientHisService.queryPatient(req);
            LOGGER.info("queryPatientForHis res={}", JSONUtils.toString(res));
            if (res != null && !("200".equals(res.getMsgCode()))) {
                String msg = "查患者信息接口异常";
                if (StringUtils.isNotEmpty(res.getMsg())) {
                    msg = msg + ":" + res.getMsg();
                }
                throw new DAOException(609, msg);
            }
            if (res == null){
                throw new DAOException(609, "查不到患者线下信息");
            }
            return res.getData();
        } catch (Exception e) {
            LOGGER.error("queryPatientForHis error", e);
            throw new DAOException(609, "查患者信息异常:"+e.getMessage());
        }
    }

    /**
     * 查询线下患者信息接口
     *
     * @return
     */
    @RpcService
    public PatientQueryRequestTO queryPatientForHisV1(Integer organId, String mpiId, Integer clinicId) {
        LOGGER.info("queryPatientForHis organId={},mpiId={}", organId, mpiId);
        PatientService patientService = ApplicationUtils.getBasicService(PatientService.class);
        IPatientHisService iPatientHisService = AppContextHolder.getBean("his.iPatientHisService", IPatientHisService.class);
        PatientDTO patient = patientService.get(mpiId);
        if (patient == null) {
            throw new DAOException(609, "找不到该患者");
        }
        try {
            PatientQueryRequestTO req = new PatientQueryRequestTO();
            req.setOrgan(organId);
            req.setPatientName(patient.getPatientName());
            req.setCertificateType(patient.getCertificateType());
            req.setCertificate(patient.getCertificate());
            //添加复诊ID
            IRevisitExService consultExService = RevisitAPI.getService(IRevisitExService.class);
            if (clinicId != null) {
                RevisitExDTO consultExDTO = consultExService.getByConsultId(clinicId);
                if (consultExDTO != null) {
                    req.setPatientID(consultExDTO.getPatId());
                }
            }
            LOGGER.info("queryPatientForHis req={}", JSONUtils.toString(req));
            HisResponseTO<PatientQueryRequestTO> res = iPatientHisService.queryPatient(req);
            LOGGER.info("queryPatientForHis res={}", JSONUtils.toString(res));
            if (res != null && !("200".equals(res.getMsgCode()))) {
                String msg = "查患者信息接口异常";
                if (StringUtils.isNotEmpty(res.getMsg())) {
                    msg = msg + ":" + res.getMsg();
                }
                throw new DAOException(609, msg);
            }
            if (res == null){
                throw new DAOException(609, "查不到患者线下信息");
            }
            PatientQueryRequestTO patientQueryRequestTO=res.getData();
            patientQueryRequestTO.setCardID(null);
            patientQueryRequestTO.setCertificate(null);
            patientQueryRequestTO.setGuardianCertificate(null);
            patientQueryRequestTO.setMobile(null);
            LOGGER.info("queryPatientForHis res:{}",JSONUtils.toString(patientQueryRequestTO));
            return patientQueryRequestTO;
        } catch (Exception e) {
            LOGGER.error("queryPatientForHis error", e);
            throw new DAOException(609, "查患者信息异常:"+e.getMessage());
        }
    }
}
