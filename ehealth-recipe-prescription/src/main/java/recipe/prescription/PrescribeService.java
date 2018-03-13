package recipe.prescription;

import ctd.persistence.DAOFactory;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.common.CommonConstant;
import recipe.common.ResponseUtils;
import recipe.dao.RecipeDAO;
import recipe.prescription.bean.HosRecipeResult;
import recipe.prescription.bean.HospitalRecipeDTO;

import java.util.List;

/**
 * @author： 0184/yu_yun
 * @date： 2018/1/31
 * @description： 开方服务
 * @version： 1.0
 */
@RpcBean("prescribeService")
public class PrescribeService {

    /**
     * logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(PrescribeService.class);

    /**
     * 新增标识
     */
    private static final int ADD_FLAG = 1;

    /**
     * 撤销标识
     */
    private static final int CANCEL_FLAG = 2;

    /**
     * 更新标识
     */
    private static final int UPDATE_FLAG = 3;

    /**
     * 创建处方
     *
     * @param recipeInfo
     * @return
     */
    @RpcService
    public HosRecipeResult createPrescription(String recipeInfo) {
        if (StringUtils.isEmpty(recipeInfo)) {
            return ResponseUtils.getFailResponse(HosRecipeResult.class, "传入参数为空");
        }

        List<HospitalRecipeDTO> recipeList = null;
        try {
            recipeList = JSONUtils.parse(recipeInfo, List.class);
        } catch (Exception e) {
            LOG.error("createPrescription parse error. param={}", recipeInfo, e);
            return ResponseUtils.getFailResponse(HosRecipeResult.class, "解析出错");
        }

        if (CollectionUtils.isNotEmpty(recipeList)) {
            HosRecipeResult result = validateHospitalRecipe(recipeList, ADD_FLAG);
            for(HospitalRecipeDTO recipe : recipeList){
                String prefix = "处方[" + recipe.getRecipeCode() + "]:";
                RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
//                Recipe dbRecipe = recipeDAO.getByOriginRecipeCodeAndOriginClinicOrgan(hospitalRecipe.getRecipeNo(), Integer.parseInt(hospitalRecipe.getOrganID()));
//                if (null != dbRecipe) {
//                    result.setMsgCode(HosRecipeResult.FAIL);
//                    result.setMsg(prefix + "平台已存在该处方");
//                    break;
//                }
//
//                Recipe recipe = hospitalRecipe.convertNgariRecipe();
//                if (null != recipe) {
//                    ThirdEnterpriseCallService takeDrugService = ApplicationUtils.getRecipeService(ThirdEnterpriseCallService.class, "takeDrugService");
//                    Integer originClinicOrgan = recipe.getOriginClinicOrgan();
//                    String organName = iOrganService.getNameById(originClinicOrgan);
//                    if (StringUtils.isEmpty(organName)) {
//                        result.setMsgCode(HosRecipeResult.FAIL);
//                        result.setMsg(prefix + "平台未找到相关机构");
//                        break;
//                    }
//
//                    //设置医生信息(开方医生，审核医生)
//                    EmploymentBean employment = iEmploymentService.getByJobNumberAndOrganId(hospitalRecipe.getDoctorID(), originClinicOrgan);
//                    if (null != employment) {
//                        recipe.setDoctor(employment.getDoctorId());
//                        recipe.setDepart(employment.getDepartment());
//
//                        String ysJobNumber = hospitalRecipe.getAuditDoctor();
//                        if (StringUtils.isNotEmpty(ysJobNumber)) {
//                            EmploymentBean ysEmployment = iEmploymentService.getByJobNumberAndOrganId(ysJobNumber, originClinicOrgan);
//                            if (null != ysEmployment) {
//                                recipe.setChecker(ysEmployment.getDoctorId());
//                                recipe.setCheckOrgan(originClinicOrgan);
//                                recipe.setCheckDateYs(recipe.getSignDate());
//                            } else {
//                                LOGGER.error("platformRecipeCreate 审核医生在平台没有执业点.");
//                            }
//                        } else {
//                            LOGGER.error("platformRecipeCreate 审核医生工号(auditDoctor)为空.");
//                        }
//
//                        //设置患者信息
//                        try {
//                            PatientBean patient = iPatientService.getOrUpdate(hospitalRecipe.convertNgariPatient());
//                            if (null == patient) {
//                                result.setMsgCode(HosRecipeResult.FAIL);
//                                result.setMsg(prefix + "获取平台患者失败");
//                                break;
//                            } else {
//                                recipe.setMpiid(patient.getMpiId());
//                            }
//                        } catch (Exception e) {
//                            LOGGER.error("创建平台患者失败，e=" + e.getMessage());
//                            result.setMsgCode(HosRecipeResult.FAIL);
//                            result.setMsg(prefix + "创建平台患者失败");
//                            break;
//                        }
//
//                        //创建详情数据
//                        List<Recipedetail> details = hospitalRecipe.convertNgariDetail();
//                        if (CollectionUtils.isEmpty(details)) {
//                            result.setMsgCode(HosRecipeResult.FAIL);
//                            result.setMsg(prefix + "存在下级医院无法开具的药品");
//                            break;
//                        }
//
//                        //初始化处方状态为待处理
//                        recipe.setStatus(RecipeStatusConstant.CHECK_PASS);
//                        Integer recipeId = recipeService.saveRecipeDataForHos(recipe, details);
//                        if (null != recipeId) {
//                            //创建订单数据
//                            Map<String, String> orderMap = Maps.newHashMap();
//                            orderMap.put("operMpiId", recipe.getMpiid());
//                            //PayWayEnum.UNKNOW
//                            orderMap.put("payway", "-1");
//                            orderMap.put("payMode", recipe.getPayMode().toString());
//                            OrderCreateResult orderCreateResult = orderService.createOrder(Collections.singletonList(recipeId), orderMap, 1);
//                            if (null != orderCreateResult && OrderCreateResult.SUCCESS.equals(orderCreateResult.getCode())) {
//                                recipe.setOrderCode(orderCreateResult.getOrderCode());
//                                //到店取药流程处理
//                                if (RecipeBussConstant.PAYMODE_TFDS.equals(recipe.getPayMode())) {
//                                    OrganBean subOrgan = iOrganService.get(recipe.getClinicOrgan());
//                                    //到店取药则自动完成用户确认操作
//                                    Map<String, Object> paramMap = Maps.newHashMap();
//                                    paramMap.put("result", "1");
//                                    paramMap.put("recipeId", recipeId);
//                                    paramMap.put("drugstore", subOrgan.getName());
//                                    paramMap.put("drugstoreAddr", subOrgan.getAddress());
//                                    paramMap.put("wxUrl", "false");
//                                    ThirdResultBean backMap = takeDrugService.userConfirm(paramMap);
//                                    if (null != backMap && ThirdEnterpriseCallService.REQUEST_OK.equals(backMap.getCode())) {
//                                        //由于医院发过来的处方已药师审核通过，所以需要自动审核完成
//                                        boolean bl = recipeDAO.updateRecipeInfoByRecipeId(recipeId, RecipeStatusConstant.CHECK_PASS_YS, null);
//                                        if (bl) {
//                                            recipe.setStatus(RecipeStatusConstant.CHECK_PASS_YS);
//                                            //到店取药审核完成是带取药状态
//                                            RecipeResultBean orderChangeResult = orderService.updateOrderInfo(recipe.getOrderCode(), ImmutableMap.of("status", OrderStatusConstant.READY_GET_DRUG), null);
//                                            if (null != orderChangeResult && RecipeResultBean.SUCCESS.equals(orderChangeResult.getCode())) {
//                                                //发送下级医院处方
////                                                    sendSubOrganHisRecipe(recipeId, hospitalRecipe.getSubOrganId());
//                                                allRecipeId.add(recipeId);
//                                                LOGGER.info("platformRecipeCreate 接收医院处方成功，recipeId=" + recipeId);
//                                                RecipeLogService.saveRecipeLog(recipeId, recipe.getStatus(), recipe.getStatus(), "收到[" + organName + "]处方成功");
//                                            } else {
//                                                orderService.cancelOrderByCode(recipe.getOrderCode(), OrderStatusConstant.CANCEL_AUTO);
//                                                recipeService.delRecipeForce(recipeId);
//                                                result.setMsgCode(HosRecipeResult.FAIL);
//                                                result.setMsg(prefix + "修改订单状态失败");
//                                            }
//                                        } else {
//                                            orderService.cancelOrderByCode(recipe.getOrderCode(), OrderStatusConstant.CANCEL_AUTO);
//                                            recipeService.delRecipeForce(recipeId);
//                                            result.setMsgCode(HosRecipeResult.FAIL);
//                                            result.setMsg(prefix + "修改处方单状态失败");
//                                        }
//                                    } else {
//                                        result.setMsgCode(HosRecipeResult.FAIL);
//                                        result.setMsg(prefix + "订单用户确认失败,原因：" + backMap.getMsg());
//                                    }
//                                }
//                            } else {
//                                //删除处方
//                                recipeService.delRecipeForce(recipeId);
//                                result.setMsgCode(HosRecipeResult.FAIL);
//                                result.setMsg(prefix + "订单创建失败,原因：" + orderCreateResult.getMsg());
//                            }
//                        } else {
//                            result.setMsgCode(HosRecipeResult.FAIL);
//                            result.setMsg(prefix + "处方创建失败");
//                        }
//                    } else {
//                        result.setMsgCode(HosRecipeResult.FAIL);
//                        result.setMsg(prefix + "平台无法找到医生执业点");
//                    }
//                } else {
//                    result.setMsgCode(HosRecipeResult.FAIL);
//                    result.setMsg(prefix + "转换平台处方出错");
//                }
//
//                if (HosRecipeResult.FAIL.equals(result.getMsgCode())) {
//                    break;
//                }
            }

            return result;
        } else {
            LOG.error("createPrescription recipeList is empty. param={}", recipeInfo);
            return ResponseUtils.getFailResponse(HosRecipeResult.class, "未知错误-处方对象为空");
        }
    }

    /**
     * 校验医院处方信息
     *
     * @param obj 医院处方
     * @return 结果
     */
    private HosRecipeResult validateHospitalRecipe(List<HospitalRecipeDTO> recipeList, int flag) {
        HosRecipeResult result = ResponseUtils.getFailResponse(HosRecipeResult.class, null);
        if (ADD_FLAG == flag) {
            //新增
            HospitalRecipeDTO hospitalRecipe;
            StringBuilder prefix = new StringBuilder();
            for (int i = 0; i < recipeList.size(); i++) {
                hospitalRecipe = recipeList.get(i);
                prefix = prefix.append("处方[" + hospitalRecipe.getRecipeCode() + "]:");

                if (StringUtils.isEmpty(hospitalRecipe.getRecipeType())) {
                    result.setMsg(prefix + "处方类型为空");
                    return result;
                }

                if (StringUtils.isEmpty(hospitalRecipe.getClinicOrgan())) {
                    result.setMsg(prefix + "开方机构为空");
                    return result;
                }

                if (StringUtils.isEmpty(hospitalRecipe.getDoctorNumber())) {
                    result.setMsg(prefix + "开方医生工号为空");
                    return result;
                }

                if (StringUtils.isEmpty(hospitalRecipe.getCertificate())) {
                    result.setMsg(prefix + "患者身份证信息为空");
                    return result;
                }

                if (CollectionUtils.isEmpty(hospitalRecipe.getDrugList())) {
                    result.setMsg(prefix + "处方详情数据为空");
                    return result;
                }
            }
        }

        result.setCode(CommonConstant.SUCCESS);
        return result;
    }

}
