package recipe.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.ngari.base.property.service.IConfigurationCenterUtilsService;
import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.base.PatientBaseInfo;
import com.ngari.his.ca.model.CaSealRequestTO;
import com.ngari.his.patient.mode.HisCardVO;
import com.ngari.his.patient.mode.PatientQueryRequestTO;
import com.ngari.his.recipe.mode.ChronicDiseaseListReqTO;
import com.ngari.his.recipe.mode.ChronicDiseaseListResTO;
import com.ngari.his.recipe.mode.PatientChronicDiseaseRes;
import com.ngari.intface.IJumperAuthorizationService;
import com.ngari.patient.dto.DoctorDTO;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.patient.service.PatientService;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.platform.recipe.MedicalInsuranceAuthResBean;
import com.ngari.platform.recipe.mode.MedicalInsuranceAuthInfoBean;
import com.ngari.recipe.basic.ds.PatientVO;
import com.ngari.recipe.common.RecipeResultBean;
import com.ngari.recipe.drugsenterprise.model.DepDetailBean;
import com.ngari.recipe.drugsenterprise.model.DepListBean;
import com.ngari.recipe.dto.GiveModeButtonDTO;
import com.ngari.recipe.dto.GiveModeShowButtonDTO;
import com.ngari.recipe.dto.PatientRecipeDetailReqDTO;
import com.ngari.recipe.dto.RecipeInfoDTO;
import com.ngari.recipe.entity.*;
import com.ngari.recipe.offlinetoonline.model.OfflineToOnlineReqVO;
import com.ngari.recipe.offlinetoonline.model.OfflineToOnlineResVO;
import com.ngari.recipe.recipe.model.GiveModeButtonBean;
import com.ngari.recipe.recipe.model.RankShiftList;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import com.ngari.recipe.vo.*;
import com.ngari.revisit.RevisitBean;
import com.ngari.revisit.common.model.RevisitExDTO;
import ctd.controller.exception.ControllerException;
import ctd.dictionary.DictionaryController;
import ctd.persistence.DAOFactory;
import ctd.persistence.exception.DAOException;
import ctd.util.AppContextHolder;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import eh.utils.BeanCopyUtils;
import eh.utils.ValidateUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.ApplicationUtils;
import recipe.aop.LogRecord;
import recipe.bean.DrugEnterpriseResult;
import recipe.bussutil.RecipeUtil;
import recipe.caNew.pdf.CreatePdfFactory;
import recipe.client.*;
import recipe.common.CommonConstant;
import recipe.constant.*;
import recipe.core.api.patient.IOfflineRecipeBusinessService;
import recipe.core.api.patient.IPatientBusinessService;
import recipe.dao.*;
import recipe.drugsenterprise.RemoteDrugEnterpriseService;
import recipe.enumerate.status.*;
import recipe.enumerate.type.*;
import recipe.factory.offlinetoonline.IOfflineToOnlineStrategy;
import recipe.factory.offlinetoonline.OfflineToOnlineFactory;
import recipe.hisservice.RecipeToHisService;
import recipe.manager.*;
import recipe.service.common.RecipeCacheService;
import recipe.util.DictionaryUtil;
import recipe.util.RedisClient;
import recipe.vo.doctor.RecipeInfoVO;
import recipe.vo.patient.PatientRecipeDetailForDetailResVO;
import recipe.vo.patient.PatientRecipeDetailReqVO;
import recipe.vo.patient.PatientRecipeDetailResVO;
import recipe.vo.patient.ReadyRecipeVO;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 患者端服务
 * company: ngarihealth
 *
 * @author: 0184/yu_yun
 * @date:2017/6/30.
 */
@RpcBean("recipePatientService")
public class RecipePatientService extends RecipeBaseService implements IPatientBusinessService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecipePatientService.class);
    @Autowired
    private ChronicDiseaseDAO chronicDiseaseDAO;
    @Autowired
    private PatientClient patientClient;
    @Autowired
    private RevisitClient revisitClient;
    @Autowired
    private RecipeManager recipeManager;
    @Autowired
    private OrganDrugListManager organDrugListManager;
    @Autowired
    private RecipeDetailManager recipeDetailManager;
    @Autowired
    private CreatePdfFactory createPdfFactory;
    @Autowired
    private IOfflineRecipeBusinessService offlineRecipeBusinessService;
    @Autowired
    private OperationClient operationClient;
    @Autowired
    private OrganDrugListDAO organDrugListDAO;
    @Autowired
    private IConfigurationClient configurationClient;
    @Autowired
    private DoctorClient doctorClient;
    @Autowired
    private IJumperAuthorizationService jumperAuthorizationService;
    @Autowired
    private EmrRecipeManager emrRecipeManager;
    @Autowired
    private RecipeDAO recipeDAO;
    @Autowired
    private RecipeOrderDAO recipeOrderDAO;
    @Autowired
    private StateManager stateManager;
    @Autowired
    private CaManager caManager;
    @Autowired
    private HisRecipeManager  hisRecipeManager;
    @Autowired
    private OfflineToOnlineFactory offlineToOnlineFactory;

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
                    return payModes.contains(input.getPayMode());
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
                    if (Integer.valueOf(0).equals(dep.getSettlementMode())) {
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
            patientBaseInfo.setPatientID(patientDTO.getPatId());
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
            list = res.getData().getChronicDiseaseListResTOs();
            HashMap<String, String> finalChronicDiseaseFlagMap = getRedisChronicDiseaseMap(organId);
            if (CollectionUtils.isNotEmpty(list) && finalChronicDiseaseFlagMap.size() > 0) {
                LOGGER.info("慢病信息转化前的list={}",JSON.toJSONString(list));
                list.forEach(
                        item -> {
                            String chronicDiseaseFlag = finalChronicDiseaseFlagMap.get(item.getChronicDiseaseFlag());
                            if(StringUtils.isNoneBlank(chronicDiseaseFlag)){
                                item.setChronicDiseaseFlag(chronicDiseaseFlag);
                            }
                        }
                );
                LOGGER.info("慢病信息转化后的list={}",JSON.toJSONString(list));
            }
            return list;
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
        //{ "id": 1, "text": "无" , "locked": true},
        //{ "id": 2, "text": "特慢病病种" },
        //{ "id": 3, "text": "重症病种" },
        //{ "id": 4, "text": "慢病病种" },
        //{ "id": 5, "text": "重医大附属二院" }
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
            patientBaseInfo.setPatientID(patientDTO.getPatId());
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
                HashMap<String, String> finalChronicDiseaseFlagMap = getRedisChronicDiseaseMap(organId);
                if (CollectionUtils.isNotEmpty(list) && finalChronicDiseaseFlagMap.size() > 0) {
                    LOGGER.info("慢病信息转化前的list={}",JSON.toJSONString(list));
                    list.forEach(
                            item -> {
                                String chronicDiseaseFlag = finalChronicDiseaseFlagMap.get(item.getChronicDiseaseFlag());
                                if(StringUtils.isNoneBlank(chronicDiseaseFlag)){
                                    item.setChronicDiseaseFlag(chronicDiseaseFlag);
                                }
                            }
                    );
                    LOGGER.info("慢病信息转化后的list={}",JSON.toJSONString(list));
                }
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
     * 查询线下患者信息接口
     *
     * @return
     */
    @RpcService
    public PatientQueryRequestTO queryPatientForHis(Integer organId, String mpiId) {
        LOGGER.info("queryPatientForHis organId={},mpiId={}", organId, mpiId);
        return queryPatientForHisV1(organId, mpiId, null);
    }

    /**
     * 查询线下患者信息接口
     *
     * @return
     */
    @RpcService
    public PatientQueryRequestTO queryPatientForHisV1(Integer organId, String mpiId, Integer clinicId) {
        LOGGER.info("queryPatientForHisV1 organId={},mpiId={}", organId, mpiId);

        PatientDTO patient = patientClient.getPatientBeanByMpiId(mpiId);
        if (patient == null) {
            throw new DAOException(609, "找不到该患者");
        }
        try {
            PatientQueryRequestTO req = new PatientQueryRequestTO();
            req.setOrgan(organId);
            req.setPatientName(patient.getPatientName());
            req.setCertificateType(patient.getCertificateType());
            req.setCertificate(patient.getCertificate());
            if (clinicId != null) {
                RevisitExDTO consultExDTO = revisitClient.getByClinicId(clinicId);
                if (consultExDTO != null) {
                    req.setPatientID(consultExDTO.getPatId());
                }
            }
            LOGGER.info("queryPatientForHisV1 req={}", JSONUtils.toString(req));
            PatientQueryRequestTO patientQueryRequestTO = patientClient.queryPatient(req);
            LOGGER.info("queryPatientForHisV1 patientQueryRequestTO={}", JSONUtils.toString(patientQueryRequestTO));
            if (null == patientQueryRequestTO || CollectionUtils.isEmpty(patientQueryRequestTO.getHisCards())) {
                return null;
            }
            List<HisCardVO> hisCardVOS = patientQueryRequestTO.getHisCards();
            //默认获取第一张卡类型
            if ("2".equals(hisCardVOS.get(0).getCardType())){
                patientQueryRequestTO.setMedicalType("2");
                patientQueryRequestTO.setMedicalTypeText("医保");
            } else {
                patientQueryRequestTO.setMedicalType("1");
                patientQueryRequestTO.setMedicalTypeText("自费");
            }
            LOGGER.info("queryPatientForHisV1 patientQueryRequestTO:{}.", JSONUtils.toString(patientQueryRequestTO));
            return patientQueryRequestTO;
        } catch (Exception e) {
            LOGGER.error("queryPatientForHisV1 error", e);
            throw new DAOException(609, "查患者信息异常:"+e.getMessage());
        }
    }

    /**
     * 校验当前就诊人是否有效 是否实名认证 就诊卡是否有效
     * @param outPatientReqVO 当前就诊人信息
     * @return 枚举值
     */
    @Override
    public Integer checkCurrentPatient(OutPatientReqVO outPatientReqVO){
        LOGGER.info("OutPatientRecipeService checkCurrentPatient outPatientReqVO:{}.", JSON.toJSONString(outPatientReqVO));
        PatientDTO patientDTO = patientClient.getPatientBeanByMpiId(outPatientReqVO.getMpiId());
        if (null == patientDTO || !new Integer(1).equals(patientDTO.getStatus())) {
            return CheckPatientEnum.CHECK_PATIENT_PATIENT.getType();
        }
        if (!new Integer(1).equals(patientDTO.getAuthStatus())) {
            return CheckPatientEnum.CHECK_PATIENT_NOAUTH.getType();
        }
        Collection result = patientClient.findHealthCard(outPatientReqVO.getMpiId());
        if (null == result || (StringUtils.isNotEmpty(outPatientReqVO.getCardID()) && !result.contains(outPatientReqVO.getCardID()))) {
            return CheckPatientEnum.CHECK_PATIENT_CARDDEL.getType();
        }
        return CheckPatientEnum.CHECK_PATIENT_NORMAL.getType();
    }

    @Override
    public MedicalInsuranceAuthResVO medicalInsuranceAuth(MedicalInsuranceAuthInfoVO medicalInsuranceAuthInfoVO) {
        String mpiId = medicalInsuranceAuthInfoVO.getMpiId();
        com.ngari.patient.dto.PatientDTO patientDTO = patientClient.getPatientBeanByMpiId(mpiId);
        LOGGER.info("medicalInsuranceAuth patientDTO:{}", JSON.toJSONString(patientDTO));
        MedicalInsuranceAuthInfoBean medicalInsuranceAuthInfoBean = new MedicalInsuranceAuthInfoBean();
        medicalInsuranceAuthInfoBean.setCallUrl(medicalInsuranceAuthInfoVO.getCallUrl());
        medicalInsuranceAuthInfoBean.setMpiId(medicalInsuranceAuthInfoVO.getMpiId());
        medicalInsuranceAuthInfoBean.setUserName(patientDTO.getPatientName());
        medicalInsuranceAuthInfoBean.setOrganId(medicalInsuranceAuthInfoVO.getOrganId());
        String openId = patientClient.getTid();
        medicalInsuranceAuthInfoBean.setChnlUserId(openId);
        Map<String, String> map = new HashMap<>();
        map.put("cid", medicalInsuranceAuthInfoVO.getRecipeId()+"");
        map.put("module", "recipeDetail");
        map.put("organId", medicalInsuranceAuthInfoVO.getOrganId()+"");
        String callUrl = jumperAuthorizationService.getThirdCallBackUrlCommon(map);
        medicalInsuranceAuthInfoBean.setCallUrl(callUrl);
        if (null != patientDTO.getCertificateType()) {
            medicalInsuranceAuthInfoBean.setIdType(patientDTO.getCertificateType()+"");
            medicalInsuranceAuthInfoBean.setIdNo(patientDTO.getCertificate());
        } else {
            medicalInsuranceAuthInfoBean.setIdType("01");
            medicalInsuranceAuthInfoBean.setIdNo(patientDTO.getCardId());
        }
        MedicalInsuranceAuthResBean medicalInsuranceAuthResBean = patientClient.medicalInsuranceAuth(medicalInsuranceAuthInfoBean);
        return ObjectCopyUtils.convert(medicalInsuranceAuthResBean, MedicalInsuranceAuthResVO.class);
    }

    /**
     * 根据mpiId获取患者信息
     *
     * @param mpiId 患者唯一号
     * @return 患者信息
     */
    @Override
    public PatientDTO getPatientDTOByMpiID(String mpiId) {
        return patientClient.getPatientBeanByMpiId(mpiId);
    }

    @Override
    public Map<String, com.ngari.recipe.dto.PatientDTO> findPatientByMpiIds(List<String> mpiIds) {
        return patientClient.findPatientMap(mpiIds);
    }


    /**
     * 获取患者医保信息
     *
     * @param patientInfoVO 患者信息
     * @return 医保类型相关
     */
    @Override
    public PatientMedicalTypeVO queryPatientMedicalType(PatientInfoVO patientInfoVO) {
        LOGGER.info("OutPatientRecipeService queryPatientMedicalType patientInfoVO:{}.", JSON.toJSONString(patientInfoVO));
        PatientMedicalTypeVO patientMedicalTypeVO = new PatientMedicalTypeVO("1", "自费");
        if (ValidateUtil.nullOrZeroInteger(patientInfoVO.getClinicId())){
            return patientMedicalTypeVO;
        }
        RevisitExDTO revisitExDTO = revisitClient.getByClinicId(patientInfoVO.getClinicId());
        if (null == revisitExDTO) {
            return patientMedicalTypeVO;
        }
        if (null != revisitExDTO.getMedicalFlag() && MedicalTypeEnum.MEDICAL_PAY.getType().equals(revisitExDTO.getMedicalFlag())) {
            return new PatientMedicalTypeVO("2", "医保");
        } else {
            return patientMedicalTypeVO;
        }
    }


    /**
     * 便捷购药开处方
     *
     * @param recipeInfoVO 处方信息
     * @return
     */
    @Override
    public Integer saveRecipe(RecipeInfoVO recipeInfoVO) {
        //数据校验
        validateData(recipeInfoVO);
        //保存处方
        com.ngari.recipe.dto.PatientDTO patientDTO = patientClient.getPatientDTO(recipeInfoVO.getRecipeBean().getMpiid());
        recipeInfoVO.getRecipeBean().setPatientName(patientDTO.getPatientName());
        recipeInfoVO.setPatientVO(ObjectCopyUtils.convert(patientDTO, PatientVO.class));
        Recipe recipe = ObjectCopyUtils.convert(recipeInfoVO.getRecipeBean(), Recipe.class);
        RecipeUtil.setDefaultData(recipe);
        recipe.setFastRecipeFlag(1);
        recipe.setAuditState(RecipeAuditStateEnum.PASS.getType());
        String ca = caManager.obtainFastRecipeCaParam(recipe);
        if(!CaConstant.ESIGN.equals(ca)){
            recipe.setStatus(RecipeStatusEnum.RECIPE_STATUS_SIGN_ING_CODE_DOC.getType());
            recipe.setChecker(null);
        }else{
            recipe.setStatus(RecipeStatusEnum.RECIPE_STATUS_CHECK_PASS.getType());
        }
        if (StringUtils.isEmpty(recipeInfoVO.getRecipeBean().getRecipeSupportGiveMode())) {
            //设置购药方式
            this.setRecipeSupportGiveMode(recipe);
        }
        recipe = recipeManager.saveRecipe(recipe);
        if(!CaConstant.ESIGN.equals(ca)){
            stateManager.updateRecipeState(recipe.getRecipeId(), RecipeStateEnum.PROCESS_STATE_ORDER, RecipeStateEnum.SUB_ORDER_READY_SUBMIT_ORDER);
        }else{
            stateManager.updateRecipeState(recipe.getRecipeId(), RecipeStateEnum.PROCESS_STATE_SUBMIT, RecipeStateEnum.SUB_SUBMIT_DOC_SIGN_ING);
        }
        //保存处方扩展
        if (null != recipeInfoVO.getRecipeExtendBean()) {
            RecipeExtend recipeExtend = ObjectCopyUtils.convert(recipeInfoVO.getRecipeExtendBean(), RecipeExtend.class);
            String cardNo = recipeManager.getCardNoByRecipe(recipe);
            if (StringUtils.isNotEmpty(cardNo)) {
                recipeExtend.setCardNo(cardNo);
            }
            recipeExtend.setRecipeBusinessType(RecipeBusinessTypeEnum.BUSINESS_RECIPE_REVISIT.getType());
            recipeManager.setRecipeInfoFromRevisit(recipe, recipeExtend);
            recipeManager.saveRecipe(recipe);
            recipeManager.saveRecipeExtend(recipeExtend, recipe);
        }

        //保存处方明细
        if (CollectionUtils.isNotEmpty(recipeInfoVO.getRecipeDetails())) {
            List<Recipedetail> details = ObjectCopyUtils.convert(recipeInfoVO.getRecipeDetails(), Recipedetail.class);
            recipeDetailManager.saveRecipeDetails(details, recipe);
        }
        RecipeLogService.saveRecipeLog(recipe.getRecipeId(), RecipeStatusEnum.RECIPE_STATUS_UNSIGNED.getType(), RecipeStatusEnum.RECIPE_STATUS_READY_CHECK_YS.getType(), "处方保存成功，等待药师审核");
        //保存审方信息
        recipeManager.saveRecipeCheck(recipe);
        RecipeLogService.saveRecipeLog(recipe.getRecipeId(), RecipeStatusEnum.RECIPE_STATUS_READY_CHECK_YS.getType(), RecipeStatusEnum.RECIPE_STATUS_CHECK_PASS.getType(), "药师审核通过，等待患者处理");
        try {
            //将处方写入HIS
            offlineRecipeBusinessService.pushRecipe(recipe.getRecipeId(), CommonConstant.RECIPE_PUSH_TYPE, CommonConstant.RECIPE_PATIENT_TYPE, null, null, null, 1);
            RecipeLogService.saveRecipeLog(recipe.getRecipeId(), RecipeStatusEnum.RECIPE_STATUS_CHECK_PASS.getType(), RecipeStatusEnum.RECIPE_STATUS_CHECK_PASS.getType(), "处方写入HIS成功");
        } catch (Exception e) {
            LOGGER.error("RecipePatientService pushRecipe error,recipeId:{}", recipe.getRecipeId(), e);
            //处方写入his失败
            recipe.setStatus(RecipeStatusEnum.RECIPE_STATUS_HIS_FAIL.getType());
            recipeManager.saveRecipe(recipe);
            stateManager.updateRecipeState(recipe.getRecipeId(), RecipeStateEnum.PROCESS_STATE_CANCELLATION, RecipeStateEnum.SUB_CANCELLATION_WRITE_HIS_NOT_ORDER);
        }
        try {
            //设置处方的失效时间
            RecipeService.handleRecipeInvalidTime(recipe.getClinicOrgan(), recipe.getRecipeId(), recipe.getSignDate());
            //更新诊断信息
            emrRecipeManager.updateDisease(recipe.getRecipeId());
        } catch (Exception e) {
            LOGGER.error("设置处方的失效时间出错 ", e);
        }
        return recipe.getRecipeId();
    }

    @Override
    public void fastRecipeCa(Integer recipeId) {
        Recipe recipe = recipeManager.getRecipeById(recipeId);
        LOGGER.info("fastRecipeCa esignRecipeCa recipe:{}", JSON.toJSONString(recipe));
        String ca = caManager.obtainFastRecipeCaParam(recipe);
        if (CaConstant.ESIGN.equals(ca)) {
            esignRecipeCa(recipeId);
        } else {
            fastRecipeOtherCa(recipe);
        }
    }

    private void fastRecipeOtherCa(Recipe recipe) {
        LOGGER.info("fastRecipeOtherCa param:{}", recipe.getRecipeId());
        CaSealRequestTO requestSealTO = createPdfFactory.queryPdfByte(recipe.getRecipeId(), true);
        RecipeServiceEsignExt.updateInitRecipePDF(true, recipe, requestSealTO.getPdfBase64Str());
        stateManager.updateDoctorSignState(recipe.getRecipeId(), SignEnum.SIGN_STATE_SUBMIT);
        caManager.oldCommonCASign(requestSealTO, recipe);
    }

    @Override
    @LogRecord
    public Integer esignRecipeCa(Integer recipeId) {
        try {
            Recipe recipe = recipeManager.getRecipeById(recipeId);
            LOGGER.info("esignRecipeCa recipe:{}", JSON.toJSONString(recipe));
            //1.医生签名
            stateManager.updateDoctorSignState(recipeId, SignEnum.SIGN_STATE_SUBMIT);
            Boolean doctorSignResult = createPdfFactory.queryPdfOssId(recipe);
            if (doctorSignResult) {
                stateManager.updateDoctorSignState(recipeId, SignEnum.SIGN_STATE_ORDER);
            } else {
                stateManager.updateDoctorSignState(recipeId, SignEnum.SIGN_STATE_AUDIT);
            }

            //2.药师签名
            stateManager.updateCheckerSignState(recipeId, SignEnum.SIGN_STATE_SUBMIT);
            Boolean checkerSignResult = createPdfFactory.updateCheckNamePdfESign(recipeId, null);
            //药师审核通过后，重新根据药师的pdf生成签名图片
            createPdfFactory.updatePdfToImg(recipe.getRecipeId(), SignImageTypeEnum.SIGN_IMAGE_TYPE_CHEMIST.getType());
            if (checkerSignResult) {
                stateManager.updateCheckerSignState(recipeId, SignEnum.SIGN_STATE_ORDER);
            } else {
                stateManager.updateCheckerSignState(recipeId, SignEnum.SIGN_STATE_AUDIT);
            }
        } catch (Exception e) {
            LOGGER.error("esignRecipeCa error", e);
        }
        return null;
    }



    /**
     * 处方开成功回写复诊更改处方id
     *
     * @param recipeId
     * @param clinicId
     */
    @Override
    public void updateRecipeIdByConsultId(Integer recipeId, Integer clinicId) {
        LOGGER.info("updateRecipeIdByConsultId recipeId:{},clinicId:{}", recipeId, clinicId);
        revisitClient.updateRecipeIdByConsultId(recipeId, clinicId);
    }

    /**
     * 是否有待处理处方
     * @param orderId
     * @return
     */
    @Override
    public ReadyRecipeVO getReadyRecipeFlag(Integer orderId) {
        ReadyRecipeVO readyRecipeVO = new ReadyRecipeVO();
        //获取该订单对应的处方
        RecipeOrder recipeOrder = recipeOrderDAO.getByOrderId(orderId);
        if (null == recipeOrder) {
            throw new DAOException("订单不存在");
        }
        String orderCode = recipeOrder.getOrderCode();
        List<Recipe> recipeList = recipeDAO.findByOrderCode(Arrays.asList(orderCode));
        if (CollectionUtils.isEmpty(recipeList)) {
            throw new DAOException("处方不存在");
        }
        //查询是否为线下处方单
        Recipe recipe = recipeList.get(0);
        if (null != recipe && RecipeSourceTypeEnum.OFFLINE_RECIPE.getType().equals(recipe.getRecipeSourceType())) {
            readyRecipeVO.setRecipeSource(2);
            readyRecipeVO.setHaveRecipe(false);
            return readyRecipeVO;
        }

        readyRecipeVO.setRecipeSource(1);
        String mpiId = recipeOrder.getMpiId();
        List<Recipe> recipes = recipeDAO.findRecipeByMpiId(mpiId);
        if (CollectionUtils.isNotEmpty(recipes)) {
            readyRecipeVO.setHaveRecipe(true);
        } else {
            readyRecipeVO.setHaveRecipe(false);
        }
        return readyRecipeVO;
    }

    @Override
    public PatientRecipeDetailResVO patientRecipeDetail(PatientRecipeDetailReqVO patientRecipeDetailReq) {
        RecipeInfoDTO recipe = null;
        Integer recipeBusType = 1;
        if (Objects.nonNull(patientRecipeDetailReq.getRecipeId())) {
             recipe = recipeManager.getRecipeInfoDTO(patientRecipeDetailReq.getRecipeId());
            Recipe recipeRecipe = recipe.getRecipe();
            if(Objects.isNull(recipe) && RecipeSourceTypeEnum.OFFLINE_RECIPE.equals(recipeRecipe.getRecipeSourceType()) && RecipeStateEnum.PROCESS_STATE_ORDER.getType().equals(recipeRecipe.getProcessState())){
                 // 线下转线上的处方 待下单的处方 进入详情页 需要查看处方有没有变动
                 IOfflineToOnlineStrategy offlineToOnlineStrategy = offlineToOnlineFactory.getFactoryService(OfflineToOnlineEnum.OFFLINE_TO_ONLINE_NO_PAY.getName());
                 OfflineToOnlineReqVO request = new OfflineToOnlineReqVO();
                 request.setMpiid(recipeRecipe.getMpiid());
                 request.setOrganId(recipeRecipe.getClinicOrgan());
                 request.setProcessState(recipeRecipe.getProcessState());
                 request.setCardId(recipe.getRecipeExtend().getCardNo());
                 request.setRecipeCode(recipeRecipe.getRecipeCode());
                 request.setStartTime(patientRecipeDetailReq.getStartTime());
                 request.setEndTime(patientRecipeDetailReq.getEndTime());

                OfflineToOnlineResVO offlineToOnlineResVO = offlineToOnlineStrategy.offlineToOnline(request);
                if (Objects.nonNull(offlineToOnlineResVO.getRecipe()) && Objects.nonNull(offlineToOnlineResVO.getRecipe().getRecipeId())) {
                    recipe = recipeManager.getRecipeInfoDTO(offlineToOnlineResVO.getRecipe().getRecipeId());
                }

            }
        }
        if(Objects.isNull(recipe) && StringUtils.isNotEmpty(patientRecipeDetailReq.getRecipeCode()) && Objects.nonNull(patientRecipeDetailReq.getOrganId())){
            // 获取线下处方
            recipe = hisRecipeManager.getHisRecipeInfoDTO(BeanCopyUtils.copyProperties(patientRecipeDetailReq, PatientRecipeDetailReqDTO::new));
            recipeBusType = 2;
        }
        if (Objects.isNull(recipe)){
            throw new DAOException("未查询到相关处方信息");
        }
        PatientRecipeDetailResVO patientRecipeDetailResVO = coverPatientRecipeDetailResVO(recipe,recipeBusType);
        return patientRecipeDetailResVO;
    }

    /**
     * 转换详情出参
     * @param recipe
     * @return
     */
    private PatientRecipeDetailResVO coverPatientRecipeDetailResVO(RecipeInfoDTO recipe,Integer recipeBusType) {
        PatientRecipeDetailResVO patientRecipeDetailResVO = new PatientRecipeDetailResVO();
        Recipe returnRecipe = recipe.getRecipe();
        BeanCopyUtils.copy(returnRecipe,patientRecipeDetailResVO);
        BeanCopyUtils.copy(recipe.getRecipeExtend(),patientRecipeDetailResVO);
        List<Recipedetail> recipeDetails = recipe.getRecipeDetails();
        Recipedetail recipedetail = recipeDetails.get(0);
        patientRecipeDetailResVO.setPharmacyId(recipedetail.getPharmacyId());
        patientRecipeDetailResVO.setPharmacyName(recipedetail.getPharmacyName());
        patientRecipeDetailResVO.setRecipeBusType(recipeBusType);
        List<String> hideRecipeDetail = configurationClient.getValueListCatch(returnRecipe.getClinicOrgan(), "hideRecipeDetail", null);
        LOGGER.info("hideRecipeDetail 药品类型：{} 需要隐方的类型:{}", returnRecipe.getRecipeType(), hideRecipeDetail);
        if (org.apache.commons.collections.CollectionUtils.isNotEmpty(hideRecipeDetail) && hideRecipeDetail.contains(returnRecipe.getRecipeType().toString()) && PayFlagEnum.NOPAY.getType().equals(returnRecipe.getPayFlag())) {
            patientRecipeDetailResVO.setIsHiddenRecipeDetail(true);
        }

        patientRecipeDetailResVO.setSubStateText(RecipeStateEnum.getRecipeStateEnum(returnRecipe.getSubState()).getName());
        if (Objects.nonNull(returnRecipe.getDepart())) {
            patientRecipeDetailResVO.setDepartName(DictionaryUtil.getDictionary("eh.base.dictionary.Depart", returnRecipe.getDepart()));
        }else {
            patientRecipeDetailResVO.setDepartName(returnRecipe.getAppointDepartName());
        }
        if (Objects.nonNull(returnRecipe.getDepart())) {
            patientRecipeDetailResVO.setDoctorName(DictionaryUtil.getDictionary("eh.base.dictionary.Doctor", returnRecipe.getDoctor()));
        }else {
            patientRecipeDetailResVO.setDoctorName(returnRecipe.getDoctorName());
        }

        Integer secrecyRecipe = 0;
        List<PatientRecipeDetailForDetailResVO> patientRecipeDetailForDetailResVOS = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(recipeDetails)) {
            List<String> organDrugCodes = recipeDetails.stream().map(Recipedetail::getOrganDrugCode).collect(Collectors.toList());
            List<OrganDrugList> organDrugList = organDrugListDAO.findByOrganIdAndDrugCodes(returnRecipe.getClinicOrgan(), organDrugCodes);
            if (CollectionUtils.isEmpty(organDrugList)) {
                organDrugList = new LinkedList<>();
            }
            Map<String, OrganDrugList> organDrugListMap = organDrugList.stream().collect(Collectors.toMap(k -> k.getOrganDrugCode() + k.getDrugId(), a -> a, (k1, k2) -> k1));

            for (Recipedetail recipeDetail : recipeDetails) {
                PatientRecipeDetailForDetailResVO patientRecipeDetailForDetailResVO = new PatientRecipeDetailForDetailResVO();
                BeanCopyUtils.copy(recipeDetail, patientRecipeDetailForDetailResVO);
                if (new Integer(3).equals(recipeDetail.getType())) {
                    secrecyRecipe = 1;
                }
                if (recipeDetail.getDrugMedicalFlag() != null && recipeDetail.getDrugMedicalFlag() >= 0) {
                    //医保限定药标识 0 否  1 是
                    patientRecipeDetailForDetailResVO.setMedicalInsuranceDrugFlag(1);
                } else {
                    patientRecipeDetailForDetailResVO.setMedicalInsuranceDrugFlag(0);
                }
                OrganDrugList organDrugList1 = organDrugListMap.get(recipeDetail.getOrganDrugCode() + recipedetail.getDrugId());
                if (Objects.nonNull(organDrugList1)) {
                    patientRecipeDetailForDetailResVO.setTargetedDrugType(organDrugList1.getTargetedDrugType());
                    patientRecipeDetailForDetailResVO.setAntiTumorDrugFlag(organDrugList1.getAntiTumorDrugFlag());
                    patientRecipeDetailForDetailResVO.setAntibioticsDrugLevel(organDrugList1.getAntiTumorDrugLevel());
                    patientRecipeDetailForDetailResVO.setNationalStandardDrugFlag(organDrugList1.getNationalStandardDrugFlag());
                }

                patientRecipeDetailForDetailResVOS.add(patientRecipeDetailForDetailResVO);
            }
        }
        patientRecipeDetailResVO.setRecipeDetail(patientRecipeDetailForDetailResVOS);
        patientRecipeDetailResVO.setSecrecyRecipe(secrecyRecipe);
        if (RecipeBusinessTypeEnum.BUSINESS_RECIPE_REVISIT.getType().equals(returnRecipe.getBussSource())) {
            RevisitExDTO revisitExDTO = revisitClient.getByClinicId(returnRecipe.getClinicId());
            if(Objects.nonNull(revisitExDTO) && Objects.nonNull(revisitExDTO.getOrderNumStartTime())){
                patientRecipeDetailResVO.setVisitTime(revisitExDTO.getOrderNumStartTime());
            }else {
                RevisitBean revisit = revisitClient.getRevisitByClinicId(returnRecipe.getClinicId());
                if(Objects.nonNull(revisit)) {
                    patientRecipeDetailResVO.setVisitTime(revisit.getPaymentDate());
                }
            }

        }
        boolean downConfig = getDownConfig(returnRecipe, recipe.getRecipeOrder());
        patientRecipeDetailResVO.setIsDownload(downConfig);
        GiveModeButtonBean showThirdOrder = getShowThirdOrder(returnRecipe);
        patientRecipeDetailResVO.setShowThirdOrder(showThirdOrder);
        String recipeInfoBottomText = configurationClient.getValueCatch(returnRecipe.getClinicOrgan(), "RecipeInfoBottomText", "");
        patientRecipeDetailResVO.setBottomText(recipeInfoBottomText);
        patientRecipeDetailResVO.setGiveModeText(recipe.getGiveModeText());
        patientRecipeDetailResVO.setShowText(recipe.getShowText());
        return patientRecipeDetailResVO;
    }

    /**
     * 获取跳转第三方查看订单信息
     * @param recipe
     * @return
     */
    private GiveModeButtonBean getShowThirdOrder(Recipe recipe) {
        DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        //设置第三方订单跳转的按钮
        Integer enterpriseId = recipe.getEnterpriseId();
        if (null == enterpriseId) {
            return null;
        }
        DrugsEnterprise drugsEnterprise = drugsEnterpriseDAO.getById(enterpriseId);
        if (null == drugsEnterprise.getOrderType() || new Integer(1).equals(drugsEnterprise.getOrderType())) {
            return null;
        }

        if (RecipeStateEnum.PROCESS_STATE_DISPENSING.getType().equals(recipe.getProcessState()) ||
                RecipeStateEnum.PROCESS_STATE_DISTRIBUTION.getType().equals(recipe.getProcessState()) ||
                RecipeStateEnum.PROCESS_STATE_DONE.getType().equals(recipe.getProcessState()) ||
                RecipeStateEnum.PROCESS_STATE_ORDER.getType().equals(recipe.getProcessState()) ||
                RecipeStateEnum.PROCESS_STATE_CANCELLATION.getType().equals(recipe.getProcessState()) ) {
            //orderType=0表示订单在第三方生成
            GiveModeButtonBean giveModeButton = new GiveModeButtonBean();
            giveModeButton.setButtonSkipType("3");
            giveModeButton.setShowButtonName("查看订单");
            giveModeButton.setShowButtonKey("supportThirdOrder");
            return giveModeButton;
        }
        return null;
    }

    /**
     * 获取是否展示下载处方笺
     * @param recipe
     * @param order
     * @return
     */
    public boolean getDownConfig(Recipe recipe, RecipeOrder order) {
        //互联网的不需要下载处方笺
        if (RecipeBussConstant.RECIPEMODE_ZJJGPT.equals(recipe.getRecipeMode())) {
            return false;
        }
        Boolean isDownload = false;
        //获取配置项
        Integer downloadPrescription = configurationClient.getValueCatchReturnInteger(recipe.getClinicOrgan(), "downloadPrescription", 0);
        //date 2020/1/9
        //逻辑修改成：如果是下载处方购药方式的，无需判断配不配置【患者展示下载处方笺】
        //非下载处方的购药方式，只有配置了【患者展示下载处方笺】才判断是否展示下载按钮
        if (RecipeBussConstant.GIVEMODE_DOWNLOAD_RECIPE.equals(recipe.getGiveMode())) {
            isDownload = canDown(recipe, order,Lists.newArrayList(RecipeStateEnum.PROCESS_STATE_ORDER.getType(),RecipeStateEnum.PROCESS_STATE_DONE.getType()), true);
        } else {
            if (null != downloadPrescription) {
                if (new Integer(1).equals(downloadPrescription)) {
                    ArrayList<Integer> list = Lists.newArrayList(RecipeStateEnum.PROCESS_STATE_ORDER.getType(), RecipeStateEnum.PROCESS_STATE_DONE.getType(), RecipeStateEnum.PROCESS_STATE_DISTRIBUTION.getType(), RecipeStateEnum.PROCESS_STATE_DISPENSING.getType());
                    isDownload = canDown(recipe, order, list, false);
                } else {
                    //没有配置则不会展示下载按钮
                    isDownload = false;
                }
            }
        }
        return isDownload;
    }

    /**
     * 获取是否下载处方
     * @param recipe
     * @param order
     * @param status
     * @param isDownLoad
     * @return
     */
    private boolean canDown(Recipe recipe, RecipeOrder order, List<Integer> status, Boolean isDownLoad) {
        boolean isDownload = false;
        //后置的时候判断处方的状态是一些状态的时候是展示按钮的
        if (ReviewTypeConstant.Postposition_Check == recipe.getReviewType()) {
            if (status.contains(recipe.getProcessState())) {
                isDownload = true;
            }
        } else if (ReviewTypeConstant.Not_Need_Check == recipe.getReviewType() && RecipeBussConstant.GIVEMODE_DOWNLOAD_RECIPE.equals(recipe.getGiveMode()) && !RecipeStateEnum.PROCESS_STATE_DONE.getType().equals( recipe.getProcessState())) {
            //这里当是不需审核，且选择的下载处方的购药方式的时候，没有产生订单，直接判断没有选定购药方式
            if (1 == recipe.getChooseFlag()) {
                isDownload = true;
            }
        } else {
            //如果实际金额为0则判断有没有关联ordercode，实际金额不为0则判断是否已经支付,展示下载处方签，
            //当下载处方购药时，已完成处方不展示下载处方签
            if (null != recipe.getOrderCode() && null != order && !(isDownLoad && RecipeStateEnum.PROCESS_STATE_DONE.getType().equals( recipe.getProcessState()))) {
                if (0 == order.getActualPrice() || (0 < order.getActualPrice() && 1 == recipe.getPayFlag())) {
                    isDownload = true;
                }

            }
        }
        return isDownload;
    }

    /**
     * @desc 做一个疾病类型的适配
     * @author 毛泽
     * @param organId
     * @return
     */
    private HashMap<String,String> getRedisChronicDiseaseMap(Integer organId){
        HashMap<String,String> chronicDiseaseFlagMap = new HashMap<>();
        try {
            RedisClient redisClient = AppContextHolder.getBean("redisClient", RedisClient.class);
            String ChronicDiseaseFlagStr = redisClient.get(CacheConstant.KEY_CHRONIC_DISEASE_FLAG+organId);
            if(StringUtils.isNoneBlank(ChronicDiseaseFlagStr)) {
                    chronicDiseaseFlagMap = JSON.parseObject(ChronicDiseaseFlagStr,HashMap.class);

            }
        } catch (Exception e) {
            LOGGER.error("getRedisChronicDiseaseMap error",e);
        }
        LOGGER.info("getRedisChronicDiseaseMap={}",JSON.toJSONString(chronicDiseaseFlagMap));
        return  chronicDiseaseFlagMap;
    }

    private void setRecipeSupportGiveMode(Recipe recipe){
        //从运营平台获取配置项
        GiveModeShowButtonDTO giveModeShowButtonDTO = operationClient.getGiveModeSettingFromYypt(recipe.getClinicOrgan());
        if (CollectionUtils.isEmpty(giveModeShowButtonDTO.getGiveModeButtons())) {
            return;
        }
        List<GiveModeButtonDTO> giveModeButtonDTOList = giveModeShowButtonDTO.getGiveModeButtons();
        LOGGER.info("setRecipeSupportGiveMode giveModeButtonDTOList:{}", JSON.toJSONString(giveModeButtonDTOList));
        StringBuilder recipeSupportGiveMode = new StringBuilder();
        giveModeButtonDTOList.forEach(giveModeButtonDTO -> {
            Integer giveMode = RecipeSupportGiveModeEnum.getGiveModeType(giveModeButtonDTO.getShowButtonKey());
            LOGGER.info("setRecipeSupportGiveMode giveMode:{}", giveMode);
            if (!new Integer(0).equals(giveMode) && !recipeSupportGiveMode.toString().contains(giveMode.toString())) {
                recipeSupportGiveMode.append(giveMode).append(",");
            }
            LOGGER.info("setRecipeSupportGiveMode recipeSupportGiveMode:{}", recipeSupportGiveMode.toString());
        });
        LOGGER.info("setRecipeSupportGiveMode recipeSupportGiveMode:{}", recipeSupportGiveMode.toString());
        recipeSupportGiveMode.deleteCharAt(recipeSupportGiveMode.lastIndexOf(","));
        recipe.setRecipeSupportGiveMode(recipeSupportGiveMode.toString());
    }

    private void validateData(RecipeInfoVO recipeInfoVO) {
        LOGGER.info("validateData recipeInfoVO:{}", JSON.toJSONString(recipeInfoVO));
        recipeInfoVO.getRecipeDetails().forEach(recipeDetailBean -> {
            Integer drugId = recipeDetailBean.getDrugId();
            Integer organId = recipeInfoVO.getRecipeBean().getClinicOrgan();
            String organDrugCode = recipeDetailBean.getOrganDrugCode();
            OrganDrugList organDrugList = organDrugListDAO.getByOrganIdAndOrganDrugCodeAndDrugId(organId, organDrugCode, drugId);
            if (null == organDrugList) {
                LOGGER.error("validateData organDrugName:{},organDrugCode:{}", recipeDetailBean.getDrugName(), recipeDetailBean.getOrganDrugCode());
                throw new DAOException(ErrorCode.SERVICE_ERROR, "药品" + recipeDetailBean.getDrugName() + "目录缺失无法开具");
            }
        });
        String ca=caManager.obtainFastRecipeCaParam(recipe.util.ObjectCopyUtils.convert(recipeInfoVO, Recipe.class));
        String fastRecipeChecker = configurationClient.getValueCatch(recipeInfoVO.getRecipeBean().getClinicOrgan(), "fastRecipeChecker", "");
        if (StringUtils.isEmpty(fastRecipeChecker)) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "没有指定审方药师");
        }
        if(CaConstant.ESIGN.equals(ca)){
            Integer checker = Integer.parseInt(fastRecipeChecker);
            DoctorDTO doctorDTO = doctorClient.getDoctor(checker);
            recipeInfoVO.getRecipeBean().setChecker(checker);
            recipeInfoVO.getRecipeBean().setCheckerText(doctorDTO.getName());
            recipeInfoVO.getRecipeBean().setCheckDate(new Date());
            recipeInfoVO.getRecipeBean().setCheckDateYs(new Date());
            recipeInfoVO.getRecipeBean().setCheckOrgan(doctorDTO.getOrgan());
            recipeInfoVO.getRecipeBean().setCheckFlag(1);

        }
    }
}
