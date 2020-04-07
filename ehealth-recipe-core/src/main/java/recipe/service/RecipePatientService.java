package recipe.service;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.base.PatientBaseInfo;
import com.ngari.his.recipe.mode.ChronicDiseaseListReqTO;
import com.ngari.his.recipe.mode.ChronicDiseaseListResTO;
import com.ngari.his.recipe.mode.MedicInsurSettleApplyResTO;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.patient.service.PatientService;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.common.RecipeResultBean;
import com.ngari.recipe.drugsenterprise.model.DepDetailBean;
import com.ngari.recipe.drugsenterprise.model.DepListBean;
import com.ngari.recipe.entity.DrugsEnterprise;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.Recipedetail;
import com.ngari.recipe.entity.SaleDrugList;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import ctd.persistence.DAOFactory;
import ctd.persistence.exception.DAOException;
import ctd.util.AppContextHolder;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.ApplicationUtils;
import recipe.bean.DrugEnterpriseResult;
import recipe.constant.DrugEnterpriseConstant;
import recipe.constant.ParameterConstant;
import recipe.constant.RecipeBussConstant;
import recipe.constant.RecipeSystemConstant;
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
import java.util.List;
import java.util.Map;

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

    /**
     * 根据取药方式过滤药企
     * @param recipeIds
     * @param payModes
     * @return
     */
    @RpcService
    public RecipeResultBean filterSupportDepList(List<Integer> recipeIds, final List<Integer> payModes){
        RecipeResultBean result = this.findSupportDepList(1, recipeIds);
        RecipeResultBean backResult = RecipeResultBean.getSuccess();
        backResult.setCode(result.getCode());
        backResult.setMsg(result.getMsg());
        Object depListObj = result.getObject();
        DepListBean newBean = new DepListBean();
        if(null != depListObj && depListObj instanceof DepListBean){
            DepListBean depListBean = (DepListBean)depListObj;
            newBean.setList(Lists.newArrayList(Collections2.filter(depListBean.getList(), new Predicate<DepDetailBean>() {
                @Override
                public boolean apply(@Nullable DepDetailBean input) {
                    if(payModes.contains(input.getPayMode())){
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
                if (DrugEnterpriseConstant.COMPANY_YSQ.equals(dep.getCallSys()) ||
                        DrugEnterpriseConstant.COMPANY_PHARMACY.equals(dep.getCallSys())
                        || DrugEnterpriseConstant.COMPANY_ZFB.equals(dep.getCallSys())) {
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
                    if (Integer.valueOf(0).equals(dep.getSettlementMode()) &&
                            (RecipeBussConstant.DEP_SUPPORT_ONLINE.equals(dep.getPayModeSupport())
                                    || RecipeBussConstant.DEP_SUPPORT_ALL.equals(dep.getPayModeSupport()))) {
                        List<SaleDrugList> saleDrugLists = saleDrugListDAO.findByOrganIdAndDrugIds(dep.getId(), drugIds);
                        if (CollectionUtils.isNotEmpty(saleDrugLists)) {
                            BigDecimal total = BigDecimal.ZERO;
                            try {
                                for (SaleDrugList saleDrug : saleDrugLists) {
                                    //保留3位小数
                                    total = total.add(saleDrug.getPrice().multiply(new BigDecimal(drugIdCountRel.get(saleDrug.getDrugId())))
                                            .divide(BigDecimal.ONE, 3, RoundingMode.UP));
                                }
                            } catch (Exception e) {
                                LOGGER.warn("findSupportDepList 重新计算药企ID为[{}]的结算价格出错. drugIds={}", dep.getId(),
                                        JSONUtils.toString(drugIds), e);
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
                resultBean.setMsg("很抱歉，当前库存不足无法购买，请联系客服：" +
                        cacheService.getParam(ParameterConstant.KEY_CUSTOMER_TEL, RecipeSystemConstant.CUSTOMER_TEL));
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
            resultBean.setMsg("很抱歉，未能匹配到可以支持的药企，请联系客服：" +
                    cacheService.getParam(ParameterConstant.KEY_CUSTOMER_TEL, RecipeSystemConstant.CUSTOMER_TEL));
        }

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
        }else if (RecipeBussConstant.DEP_SUPPORT_ONLINE_TFDS.equals(supportMode)){
            //配送到家
            payModeList.add(RecipeBussConstant.PAYMODE_ONLINE);
            //药店取药
            payModeList.add(RecipeBussConstant.PAYMODE_TFDS);
        } else if (RecipeBussConstant.DEP_SUPPORT_UNKNOW.equals(supportMode)){
            payModeList.add(RecipeBussConstant.DEP_SUPPORT_UNKNOW);
        }
        if (CollectionUtils.isNotEmpty(payModeList)){
            depDetailBean.setPayMode(payModeList.get(0));
        }
        depDetailBean.setGiveModeText(giveModeText);
        depDetailList.add(depDetailBean);
    }

    /**
     * 获取患者特慢病病种列表
     * @return
     */
    @RpcService
    public List<ChronicDiseaseListResTO> findPatientChronicDiseaseList(Integer organId,String mpiId){
        LOGGER.info("findPatientChronicDiseaseList organId={},mpiId={}",organId,mpiId);
        PatientService patientService = ApplicationUtils.getBasicService(PatientService.class);
        PatientDTO patientDTO = patientService.get(mpiId);
        if (patientDTO == null){
            throw new DAOException(609, "找不到该患者");
        }
        RecipeToHisService service = AppContextHolder.getBean("recipeToHisService", RecipeToHisService.class);
        ChronicDiseaseListReqTO req = new ChronicDiseaseListReqTO();
        PatientBaseInfo patientBaseInfo = new PatientBaseInfo();
        patientBaseInfo.setPatientName(patientDTO.getPatientName());
        patientBaseInfo.setCertificate(patientDTO.getCertificate());
        patientBaseInfo.setCertificateType(patientDTO.getCertificateType());
        req.setPatient(patientBaseInfo);
        req.setOrganId(organId);
        List<ChronicDiseaseListResTO> list = Lists.newArrayList();
        HisResponseTO<List<ChronicDiseaseListResTO>> res = service.findPatientChronicDiseaseList(req);
        if (res == null){
            return list;
        }
        return res.getData();
    }
}
