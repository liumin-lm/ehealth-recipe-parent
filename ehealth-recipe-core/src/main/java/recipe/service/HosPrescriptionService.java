package recipe.service;

import com.google.common.collect.Maps;
import com.ngari.recipe.common.RecipeCommonResTO;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.hisprescription.model.HosRecipeResult;
import com.ngari.recipe.hisprescription.model.HospitalRecipeDTO;
import com.ngari.recipe.hisprescription.service.IHosPrescriptionService;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.ApplicationUtils;
import recipe.bean.OrderCreateResult;
import recipe.bean.RecipeResultBean;
import recipe.common.CommonConstant;
import recipe.constant.OrderStatusConstant;
import recipe.constant.RecipeBussConstant;
import recipe.prescription.PrescribeService;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;

/**
 * 对接第三方医院服务
 * company: ngarihealth
 *
 * @author: 0184/yu_yun
 * @date:2017/4/17.
 */
@RpcBean("remoteHosPrescriptionService")
public class HosPrescriptionService implements IHosPrescriptionService {

    /**
     * logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(HosPrescriptionService.class);

    /**
     * 接收第三方处方
     *
     * @param hospitalRecipeList 医院处方
     * @return 结果
     */
    @Override
    @RpcService
    public HosRecipeResult createPrescription(HospitalRecipeDTO hospitalRecipeDTO) {
        PrescribeService prescribeService = ApplicationUtils.getRecipeService(PrescribeService.class);
//        String recipeInfo = "{ \"clinicId\": \"1\", \"certificateType \": \"1\", \"certificate \": \"330624198808141671\",  \"patientTel \": \"17706521132\", \"patientName\": \"张三\",\"patientNumber \": \"P100\", \"clinicOrgan\": \"1\", \"recipeCode\": \"CF001001\", \"recipeType\": \"1\",  \"doctorNumber\": \"0020\", \"doctorName\": \"测试doc\",\"createDate\": \"2018-03-22 10:40:30\", \"recipeFee\": \"100.01\", \"actualFee\": \"105.02\", \"couponFee\": \"0.00\", \"expressFee\": \"5.01\",\"decoctionFee\": \"0.00\",\"medicalFee\": \"0.00\", \"orderTotalFee\": \"105.02\", \"organDiseaseName\": \"A8888\", \"organDiseaseId\": \"感冒\", \"payMode\": \"3\", \"giveMode\": \"2\",\"giveUser\": \"测试发药\",\"status\": \"2\", \"memo\": \"诊断备注\", \"medicalPayFlag\": \"0\", \"distributionFlag\": \"0\", \"recipeMemo\": \"处方备注\", \"tcmUsePathways\": \"\",\"tcmUsingRate\": \"\",  \"tcmNum \": \"\",  \"takeMedicine\": \"\",  \"drugList\": [{ \"drugCode\": \"111001402\",   \"drugName\": \"头孢\", \"total\": \"2\",  \"useDose\": \"0.1\",\"drugFee\": \"50.005\", \"medicalFee\": \"0\", \"drugTotalFee\": \"100.01\", \"uesDays\": \"3\",  \"pharmNo\": \"8\", \"usingRate\": \"qid\",\"usePathways\": \"po\", \"memo\": \"药品使用备注\"}]}";
        HosRecipeResult result = prescribeService.createPrescription(hospitalRecipeDTO);
        if (CommonConstant.SUCCESS.equals(result.getCode())) {
            RecipeOrderService orderService = ApplicationUtils.getRecipeService(RecipeOrderService.class);
            RecipeService recipeService = ApplicationUtils.getRecipeService(RecipeService.class);

            Recipe recipe = result.getRecipe();
            HospitalRecipeDTO hospitalRecipe = result.getHospitalRecipe();
            Integer recipeId = result.getRecipeId();
            //已支付的处方不需要创建订单
            if (1 == recipe.getPayFlag() && RecipeBussConstant.PAYMODE_TO_HOS.equals(recipe.getPayMode())) {
                result.setRecipe(null);
                result.setHospitalRecipe(null);
                return result;
            }

            //创建订单
            Map<String, String> orderMap = Maps.newHashMap();
            orderMap.put("operMpiId", recipe.getMpiid());
            //PayWayEnum.UNKNOW
            orderMap.put("payway", "-1");
            orderMap.put("payMode", recipe.getPayMode().toString());
            orderMap.put("decoctionFlag", "0");
            orderMap.put("gfFeeFlag", "0");
            orderMap.put("calculateFee", "0");
            OrderCreateResult orderCreateResult = orderService.createOrder(
                    Collections.singletonList(recipeId), orderMap, 1);
            if (null != orderCreateResult && OrderCreateResult.SUCCESS.equals(orderCreateResult.getCode())) {
                try {
                    //更新订单数据
                    Map<String, Object> orderAttr = Maps.newHashMap();
                    orderAttr.put("status", OrderStatusConstant.READY_PAY);
                    orderAttr.put("effective", 1);
                    orderAttr.put("payFlag", recipe.getPayFlag());
                    orderAttr.put("registerFee", BigDecimal.ZERO);
                    orderAttr.put("recipeFee", recipe.getTotalMoney());
                    orderAttr.put("expressFee", StringUtils.isEmpty(hospitalRecipe.getExpressFee()) ?
                            BigDecimal.ZERO : new BigDecimal(hospitalRecipe.getExpressFee()));
                    orderAttr.put("decoctionFee", StringUtils.isEmpty(hospitalRecipe.getDecoctionFee()) ?
                            BigDecimal.ZERO : new BigDecimal(hospitalRecipe.getDecoctionFee()));
                    orderAttr.put("couponFee", StringUtils.isEmpty(hospitalRecipe.getCouponFee()) ?
                            BigDecimal.ZERO : new BigDecimal(hospitalRecipe.getCouponFee()));
                    orderAttr.put("totalFee", StringUtils.isEmpty(hospitalRecipe.getOrderTotalFee()) ?
                            BigDecimal.ZERO : new BigDecimal(hospitalRecipe.getOrderTotalFee()));
                    orderAttr.put("actualPrice", StringUtils.isEmpty(hospitalRecipe.getActualFee()) ?
                            0d : new BigDecimal(hospitalRecipe.getActualFee()).doubleValue());

                    RecipeResultBean resultBean = orderService.updateOrderInfo(
                            orderCreateResult.getOrderCode(), orderAttr, null);
                    LOG.info("createPrescription 订单更新 orderCode={}, result={}",
                            orderCreateResult.getOrderCode(), JSONUtils.toString(resultBean));
                } catch (Exception e) {
                    LOG.warn("createPrescription 订单更新失败. recipeId={}, orderCode={}",
                            recipeId, orderCreateResult.getOrderCode(), e);
                    //删除处方
                    recipeService.delRecipeForce(recipeId);
                    result.setCode(RecipeCommonResTO.FAIL);
                    result.setMsg("处方[" + result.getRecipeCode() + "]订单更新失败");
                }
            } else {
                LOG.warn("createPrescription 创建订单失败. recipeId={}, result={}",
                        recipeId, JSONUtils.toString(orderCreateResult));
                //删除处方
                recipeService.delRecipeForce(recipeId);
                result.setCode(RecipeCommonResTO.FAIL);
                result.setMsg("处方[" + result.getRecipeCode() + "]订单创建失败, 原因：" + orderCreateResult.getMsg());
            }
        }

        result.setRecipe(null);
        result.setHospitalRecipe(null);
        return result;
    }

    /**
     * 撤销处方 (上级医院调用)
     *
     * @param hospitalRecipe 医院处方
     * @return 结果
     */
//    @RpcService
//    public HosRecipeResult platformRecipeCancel(HospitalRecipeBean hospitalRecipe) {
//        //传入数据校验
//        HosRecipeResult result = validateHospitalRecipe(hospitalRecipe, CANCEL_FLAG);
//        if (HosRecipeResult.FAIL.equals(result.getMsgCode())) {
//            return result;
//        }
//
//        //撤销原处方
//        RecipeOrderService orderService = ApplicationUtils.getRecipeService(RecipeOrderService.class);
//        RecipeHisService hisService = ApplicationUtils.getRecipeService(RecipeHisService.class);
//        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
//
//        List<String> recipeCodeList = hospitalRecipe.getRecipeCodeList();
//        HosRecipeResult subResult;
//        List<HosRecipeResult> failList = Lists.newArrayList();
//        for (String recipeCode : recipeCodeList) {
//            subResult = HosRecipeResult.getSuccess();
//            Recipe recipe = recipeDAO.getByOriginRecipeCodeAndOriginClinicOrgan(recipeCode, Integer.parseInt(hospitalRecipe.getOrganID()));
//            if (null != recipe) {
//                Integer recipeId = recipe.getRecipeId();
//                //发送HIS消息
//                //最后以为是HIS处方状态，2表示取消
//                boolean succFlag = hisService.recipeStatusUpdateWithOrganId(recipeId, Integer.parseInt(hospitalRecipe.getSubOrganId()), "2");
//                if (succFlag) {
//                    succFlag = recipeDAO.updateRecipeInfoByRecipeId(recipeId, RecipeStatusConstant.DELETE, ImmutableMap.of("recipeCode", ""));
//                    if (succFlag) {
//                        RecipeResultBean orderResult = orderService.cancelOrderByRecipeId(recipeId, OrderStatusConstant.CANCEL_AUTO);
//                        if (RecipeResultBean.SUCCESS.equals(orderResult.getCode())) {
//                            RecipeLogService.saveRecipeLog(recipeId, recipe.getStatus(), RecipeStatusConstant.DELETE, "收到撤销处方消息，撤销成功");
//                        } else {
//                            subResult.setMsgCode(HosRecipeResult.FAIL);
//                            subResult.setMsg("平台订单更新失败");
//                        }
//                    } else {
//                        subResult.setMsgCode(HosRecipeResult.FAIL);
//                        subResult.setMsg("平台处方更新失败");
//                    }
//                } else {
//                    subResult.setMsgCode(HosRecipeResult.FAIL);
//                    subResult.setMsg("下级医院撤销处方失败，请联系下级医院处理完毕后再试");
//                }
//            } else {
//                subResult.setMsgCode(HosRecipeResult.FAIL);
//                subResult.setMsg("平台无法找到该处方");
//            }
//
//            if (HosRecipeResult.FAIL.equals(subResult.getMsgCode())) {
//                failList.add(subResult);
//            }
//        }
//
//        //批量数据结果返回结构不明确
//
//        return result;
//    }

    /**
     * 医院处方状态修改 (下级医院调用)
     *
     * @param hospitalRecipe 医院处方
     * @return 结果
     */
//    @RpcService
//    public HosRecipeResult platformRecipeStatusUpdate(HospitalRecipeBean hospitalRecipe) {
//        //传入数据校验
//        HosRecipeResult result = validateHospitalRecipe(hospitalRecipe, UPDATE_FLAG);
//        if (HosRecipeResult.FAIL.equals(result.getMsgCode())) {
//            return result;
//        }
//
//        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
//        Recipe recipe = recipeDAO.getByOriginRecipeCodeAndOriginClinicOrgan(hospitalRecipe.getOriginRecipeNo(),
//                Integer.parseInt(hospitalRecipe.getOriginOrganID()));
//        if (null == recipe) {
//            result.setMsgCode(HosRecipeResult.FAIL);
//            result.setMsg("处方不存在");
//            return result;
//        }
//
//        ThirdEnterpriseCallService takeDrugService = ApplicationUtils.getRecipeService(ThirdEnterpriseCallService.class, "takeDrugService");
//
//        //处方状态
//        //0已接受 1已支付 2已发药 8已取消
//        String recipeStatus = hospitalRecipe.getRecipeStatus();
//        String accept = "0";
//        String pay = "1";
//        String send = "2";
//        String cancel = "8";
//        if (accept.equals(recipeStatus)) {
//            Map<String, Object> attrMap = Maps.newHashMap();
//            attrMap.put("recipeCode", hospitalRecipe.getRecipeNo());
//            recipeDAO.updateRecipeInfoByRecipeId(recipe.getRecipeId(), attrMap);
//        } else if (cancel.equals(recipeStatus) || send.equals(recipeStatus)) {
//            Map<String, Object> paramMap = Maps.newHashMap();
//            if (send.equals(recipeStatus)) {
//                paramMap.put("result", "1");
//            } else {
//                paramMap.put("result", "0");
//                paramMap.put("reason", "处方取消");
//            }
//            paramMap.put("organId", hospitalRecipe.getOrganID());
//            paramMap.put("recipeCode", hospitalRecipe.getRecipeNo());
//            paramMap.put("sendDate", DateTime.now().toString(DateConversion.DEFAULT_DATE_TIME));
//            ThirdResultBean backMap = takeDrugService.recordDrugStoreResult(paramMap);
//            if (null != backMap && ThirdEnterpriseCallService.REQUEST_OK.equals(backMap.getCode())) {
//                //成功
//                LOGGER.info("platformRecipeStatusUpdate 处理成功. recipeStatus=[{}]", recipeStatus);
//            } else {
//                result.setMsgCode(HosRecipeResult.FAIL);
//                result.setMsg("平台处方状态更新失败,原因：" + backMap.getMsg());
//            }
//        }
//
//        return result;
//    }
//
//    /**
//     * 查询处方 (下级医院调用)
//     *
//     * @param seachBean 查询条件
//     * @return
//     */
//    @RpcService
//    public HosRecipeResult platformRecipeSearch(HospitalSearchBean seachBean) {
//        //传入数据校验
//        List<SqlOperInfo> searchAttr = new ArrayList<>();
//        HosRecipeResult result = validateHospitalSearch(seachBean, searchAttr);
//        if (HosRecipeResult.FAIL.equals(result.getMsgCode())) {
//            return result;
//        }
//
//        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
//        RecipeDetailDAO recipeDetailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
//
//        List<HospitalRecipeBean> backList = new ArrayList<>();
//        PatientBean patient = iPatientService.getByIdCard(seachBean.getCertID());
//        if (null == patient) {
//            result.setMsgCode(HosRecipeResult.FAIL);
//            result.setMsg("没有该患者的处方单");
//            return result;
//        } else {
//            if (patient.getPatientName().equals(seachBean.getPatientName())) {
//                searchAttr.add(new SqlOperInfo("mpiid", patient.getMpiId()));
//            } else {
//                result.setMsgCode(HosRecipeResult.FAIL);
//                result.setMsg("患者姓名与平台不匹配");
//                return result;
//            }
//        }
//        searchAttr.add(new SqlOperInfo("fromflag", 0));
//        if (StringUtils.isNotEmpty(seachBean.getDoctorID())) {
//            EmploymentBean employment = iEmploymentService.getByJobNumberAndOrganId(seachBean.getDoctorID(), Integer.parseInt(seachBean.getOrganID()));
//            if (null != employment) {
//                searchAttr.add(new SqlOperInfo("doctor", employment.getDoctorId()));
//            } else {
//                result.setMsgCode(HosRecipeResult.FAIL);
//                result.setMsg("平台无法找到医生执业点");
//                return result;
//            }
//        }
//        List<Recipe> dbList = recipeDAO.findRecipeListWithConditions(searchAttr);
//        if (CollectionUtils.isNotEmpty(dbList)) {
//            HospitalRecipeBean hospitalRecipeBean;
//            for (Recipe recipe : dbList) {
//                hospitalRecipeBean = new HospitalRecipeBean();
//                try {
//                    RecipeResultBean result1 = hospitalRecipeBean.parseRecipe(recipe, recipeDetailDAO.findByRecipeId(recipe.getRecipeId()),
//                            patient, iPatientService.findAllHealthCard(recipe.getMpiid(), recipe.getClinicOrgan()));
//                    if (RecipeResultBean.SUCCESS.equals(result1.getCode())) {
//                        backList.add(hospitalRecipeBean);
//                    }
//                } catch (Exception e) {
//                    e.printStackTrace();
//                    LOGGER.error("platformRecipeSearch 解析平台处方出错. recipeId=[{}]", recipe.getRecipeId());
//                }
//            }
//        }
//
//        result.setData(backList);
//        return result;
//    }
//
//    /**
//     * 发送下级医院处方
//     *
//     * @param recipeId   处方ID
//     * @param subOrganId 下级机构ID
//     * @return 结果
//     */
//    @RpcService
//    public boolean sendSubOrganHisRecipe(Integer recipeId, String subOrganId) {
//        if (null != recipeId && StringUtils.isNotEmpty(subOrganId)) {
//            RecipeHisService hisService = ApplicationUtils.getRecipeService(RecipeHisService.class);
//            Integer subOrgan = Integer.parseInt(subOrganId);
//            //发送HIS消息
//            //发送下级医院处方
//            hisService.recipeSendHis(recipeId, subOrgan);
//            return true;
//        }
//
//        return false;
//    }


}
