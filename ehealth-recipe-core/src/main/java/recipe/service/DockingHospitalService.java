package recipe.service;

import com.google.common.collect.ImmutableMap;
import com.ngari.base.employment.model.EmploymentBean;
import com.ngari.base.employment.service.IEmploymentService;
import com.ngari.base.organ.model.OrganBean;
import com.ngari.base.organ.service.IOrganService;
import com.ngari.base.patient.model.PatientBean;
import com.ngari.base.patient.service.IPatientService;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.Recipedetail;
import ctd.persistence.DAOFactory;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.bean.*;
import recipe.constant.ConditionOperator;
import recipe.constant.OrderStatusConstant;
import recipe.constant.RecipeBussConstant;
import recipe.constant.RecipeStatusConstant;
import recipe.dao.RecipeDAO;
import recipe.dao.RecipeDetailDAO;
import recipe.drugsenterprise.ThirdEnterpriseCallService;
import recipe.util.ApplicationUtils;
import recipe.util.DateConversion;
import recipe.util.SqlOperInfo;

import java.util.*;

/**
 * 对接第三方医院服务
 * company: ngarihealth
 * author: 0184/yu_yun
 * date:2017/4/17.
 */
@RpcBean("dockingHospitalService")
public class DockingHospitalService {

    /**
     * logger
     */
    private static final Logger logger = LoggerFactory.getLogger(DockingHospitalService.class);

    private IOrganService iOrganService = ApplicationUtils.getBaseService(IOrganService.class);


    private IEmploymentService iEmploymentService =
            ApplicationUtils.getBaseService(IEmploymentService.class);

    private IPatientService iPatientService =
            ApplicationUtils.getBaseService(IPatientService.class);

    /**
     * 接收第三方处方 (上级医院调用)
     *
     * @param hospitalRecipeList 医院处方
     * @return 结果
     */
    @RpcService
    public HosRecipeResult platformRecipeCreate(List<HospitalRecipeBean> hospitalRecipeList) {
        //传入数据校验
        HosRecipeResult result = validateHospitalRecipe(hospitalRecipeList, 1);
        if (HosRecipeResult.FAIL.equals(result.getMsgCode())) {
            return result;
        }
        RecipeOrderService orderService = ApplicationUtils.getRecipeService(RecipeOrderService.class);
        RecipeService recipeService = ApplicationUtils.getRecipeService(RecipeService.class);

        List<Integer> allRecipeId = new ArrayList<>(hospitalRecipeList.size());
        for (HospitalRecipeBean hospitalRecipe : hospitalRecipeList) {
            String prefix = "处方号[" + hospitalRecipe.getRecipeNo() + "]：";
            RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
            Recipe dbRecipe = recipeDAO.getByOriginRecipeCodeAndOriginClinicOrgan(hospitalRecipe.getRecipeNo(), Integer.parseInt(hospitalRecipe.getOrganID()));
            if (null != dbRecipe) {
                result.setMsgCode(HosRecipeResult.FAIL);
                result.setMsg(prefix + "平台已存在该处方");
                break;
            }

            Recipe recipe = hospitalRecipe.convertNgariRecipe();
            if (null != recipe) {
                ThirdEnterpriseCallService takeDrugService = ApplicationUtils.getRecipeService(ThirdEnterpriseCallService.class, "takeDrugService");
                Integer originClinicOrgan = recipe.getOriginClinicOrgan();
                String organName = iOrganService.getNameById(originClinicOrgan);
                if (StringUtils.isEmpty(organName)) {
                    result.setMsgCode(HosRecipeResult.FAIL);
                    result.setMsg(prefix + "平台未找到相关机构");
                    break;
                }

                //设置医生信息(开方医生，审核医生)
                EmploymentBean employment = iEmploymentService.getByJobNumberAndOrganId(hospitalRecipe.getDoctorID(), originClinicOrgan);
                if (null != employment) {
                    recipe.setDoctor(employment.getDoctorId());
                    recipe.setDepart(employment.getDepartment());

                    String ysJobNumber = hospitalRecipe.getAuditDoctor();
                    if (StringUtils.isNotEmpty(ysJobNumber)) {
                        EmploymentBean ysEmployment = iEmploymentService.getByJobNumberAndOrganId(ysJobNumber, originClinicOrgan);
                        if (null != ysEmployment) {
                            recipe.setChecker(ysEmployment.getDoctorId());
                            recipe.setCheckOrgan(originClinicOrgan);
                            recipe.setCheckDateYs(recipe.getSignDate());
                        } else {
                            logger.error("platformRecipeCreate 审核医生在平台没有执业点.");
                        }
                    } else {
                        logger.error("platformRecipeCreate 审核医生工号(auditDoctor)为空.");
                    }

                    //设置患者信息
                    try {
                        PatientBean patient = iPatientService.getOrUpdate(hospitalRecipe.convertNgariPatient());
                        if (null == patient) {
                            result.setMsgCode(HosRecipeResult.FAIL);
                            result.setMsg(prefix + "获取平台患者失败");
                            break;
                        } else {
                            recipe.setMpiid(patient.getMpiId());
                        }
                    } catch (Exception e) {
                        logger.error("创建平台患者失败，e=" + e.getMessage());
                        result.setMsgCode(HosRecipeResult.FAIL);
                        result.setMsg(prefix + "创建平台患者失败");
                        break;
                    }

                    //创建详情数据
                    List<Recipedetail> details = hospitalRecipe.convertNgariDetail();
                    if (CollectionUtils.isEmpty(details)) {
                        result.setMsgCode(HosRecipeResult.FAIL);
                        result.setMsg(prefix + "存在下级医院无法开具的药品");
                        break;
                    }

                    //初始化处方状态为待处理
                    recipe.setStatus(RecipeStatusConstant.CHECK_PASS);
                    Integer recipeId = recipeService.saveRecipeDataForHos(recipe, details);
                    if (null != recipeId) {
                        //创建订单数据
                        Map<String, String> orderMap = new HashMap<>();
                        orderMap.put("operMpiId", recipe.getMpiid());
                        //PayWayEnum.UNKNOW
                        orderMap.put("payway", "-1");
                        orderMap.put("payMode", recipe.getPayMode().toString());
                        OrderCreateResult orderCreateResult = orderService.createOrder(Collections.singletonList(recipeId), orderMap, 1);
                        if (null != orderCreateResult && OrderCreateResult.SUCCESS.equals(orderCreateResult.getCode())) {
                            recipe.setOrderCode(orderCreateResult.getOrderCode());
                            //到店取药流程处理
                            if (RecipeBussConstant.PAYMODE_TFDS.equals(recipe.getPayMode())) {
                                OrganBean subOrgan = iOrganService.get(recipe.getClinicOrgan());
                                //到店取药则自动完成用户确认操作
                                Map<String, Object> paramMap = new HashMap<>();
                                paramMap.put("result", "1");
                                paramMap.put("recipeId", recipeId);
                                paramMap.put("drugstore", subOrgan.getName());
                                paramMap.put("drugstoreAddr", subOrgan.getAddress());
                                paramMap.put("wxUrl", "false");
                                ThirdResultBean backMap = takeDrugService.userConfirm(paramMap);
                                if (null != backMap && ThirdEnterpriseCallService.REQUEST_OK.equals(backMap.getCode())) {
                                    //由于医院发过来的处方已药师审核通过，所以需要自动审核完成
                                    boolean bl = recipeDAO.updateRecipeInfoByRecipeId(recipeId, RecipeStatusConstant.CHECK_PASS_YS, null);
                                    if (bl) {
                                        recipe.setStatus(RecipeStatusConstant.CHECK_PASS_YS);
                                        //到店取药审核完成是带取药状态
                                        RecipeResultBean orderChangeResult = orderService.updateOrderInfo(recipe.getOrderCode(), ImmutableMap.of("status", OrderStatusConstant.READY_GET_DRUG), null);
                                        if (null != orderChangeResult && RecipeResultBean.SUCCESS.equals(orderChangeResult.getCode())) {
                                            //发送下级医院处方
//                                                    sendSubOrganHisRecipe(recipeId, hospitalRecipe.getSubOrganId());
                                            allRecipeId.add(recipeId);
                                            logger.info("platformRecipeCreate 接收医院处方成功，recipeId=" + recipeId);
                                            RecipeLogService.saveRecipeLog(recipeId, recipe.getStatus(), recipe.getStatus(), "收到[" + organName + "]处方成功");
                                        } else {
                                            orderService.cancelOrderByCode(recipe.getOrderCode(), OrderStatusConstant.CANCEL_AUTO);
                                            recipeService.delRecipeForce(recipeId);
                                            result.setMsgCode(HosRecipeResult.FAIL);
                                            result.setMsg(prefix + "修改订单状态失败");
                                        }
                                    } else {
                                        orderService.cancelOrderByCode(recipe.getOrderCode(), OrderStatusConstant.CANCEL_AUTO);
                                        recipeService.delRecipeForce(recipeId);
                                        result.setMsgCode(HosRecipeResult.FAIL);
                                        result.setMsg(prefix + "修改处方单状态失败");
                                    }
                                } else {
                                    result.setMsgCode(HosRecipeResult.FAIL);
                                    result.setMsg(prefix + "订单用户确认失败,原因：" + backMap.getMsg());
                                }
                            }
                        } else {
                            //删除处方
                            recipeService.delRecipeForce(recipeId);
                            result.setMsgCode(HosRecipeResult.FAIL);
                            result.setMsg(prefix + "订单创建失败,原因：" + orderCreateResult.getMsg());
                        }
                    } else {
                        result.setMsgCode(HosRecipeResult.FAIL);
                        result.setMsg(prefix + "处方创建失败");
                    }
                } else {
                    result.setMsgCode(HosRecipeResult.FAIL);
                    result.setMsg(prefix + "平台无法找到医生执业点");
                }
            } else {
                result.setMsgCode(HosRecipeResult.FAIL);
                result.setMsg(prefix + "转换平台处方出错");
            }

            if (HosRecipeResult.FAIL.equals(result.getMsgCode())) {
                break;
            }
        }

        if (HosRecipeResult.FAIL.equals(result.getMsgCode())) {
            //处理之前保存过的处方
            if (CollectionUtils.isNotEmpty(allRecipeId)) {
                for (Integer recipeId : allRecipeId) {
                    orderService.cancelOrderByRecipeId(recipeId, OrderStatusConstant.CANCEL_AUTO);
                    recipeService.delRecipeForce(recipeId);
                }
            }
        }

        return result;
    }

    /**
     * 撤销处方 (上级医院调用)
     *
     * @param hospitalRecipe 医院处方
     * @return 结果
     */
    @RpcService
    public HosRecipeResult platformRecipeCancel(HospitalRecipeBean hospitalRecipe) {
        //传入数据校验
        HosRecipeResult result = validateHospitalRecipe(hospitalRecipe, 2);
        if (HosRecipeResult.FAIL.equals(result.getMsgCode())) {
            return result;
        }

        //撤销原处方
        RecipeOrderService orderService = ApplicationUtils.getRecipeService(RecipeOrderService.class);
        RecipeHisService hisService = ApplicationUtils.getRecipeService(RecipeHisService.class);
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);

        List<String> recipeCodeList = hospitalRecipe.getRecipeCodeList();
        HosRecipeResult subResult;
        List<HosRecipeResult> failList = new ArrayList<>(5);
        for (String recipeCode : recipeCodeList) {
            subResult = HosRecipeResult.getSuccess();
            Recipe recipe = recipeDAO.getByOriginRecipeCodeAndOriginClinicOrgan(recipeCode, Integer.parseInt(hospitalRecipe.getOrganID()));
            if (null != recipe) {
                Integer recipeId = recipe.getRecipeId();
                //发送HIS消息
                //最后以为是HIS处方状态，2表示取消
                boolean succFlag = hisService.recipeStatusUpdateWithOrganId(recipeId, Integer.parseInt(hospitalRecipe.getSubOrganId()), "2");
                if (succFlag) {
                    succFlag = recipeDAO.updateRecipeInfoByRecipeId(recipeId, RecipeStatusConstant.DELETE, ImmutableMap.of("recipeCode", ""));
                    if (succFlag) {
                        RecipeResultBean orderResult = orderService.cancelOrderByRecipeId(recipeId, OrderStatusConstant.CANCEL_AUTO);
                        if (RecipeResultBean.SUCCESS.equals(orderResult.getCode())) {
                            RecipeLogService.saveRecipeLog(recipeId, recipe.getStatus(), RecipeStatusConstant.DELETE, "收到撤销处方消息，撤销成功");
                        } else {
                            subResult.setMsgCode(HosRecipeResult.FAIL);
                            subResult.setMsg("平台订单更新失败");
                        }
                    } else {
                        subResult.setMsgCode(HosRecipeResult.FAIL);
                        subResult.setMsg("平台处方更新失败");
                    }
                } else {
                    subResult.setMsgCode(HosRecipeResult.FAIL);
                    subResult.setMsg("下级医院撤销处方失败，请联系下级医院处理完毕后再试");
                }
            } else {
                subResult.setMsgCode(HosRecipeResult.FAIL);
                subResult.setMsg("平台无法找到该处方");
            }

            if (HosRecipeResult.FAIL.equals(subResult.getMsgCode())) {
                failList.add(subResult);
            }
        }

        //TODO 批量数据结果返回结构不明确

        return result;
    }

    /**
     * 医院处方状态修改 (下级医院调用)
     *
     * @param hospitalRecipe 医院处方
     * @return 结果
     */
    @RpcService
    public HosRecipeResult platformRecipeStatusUpdate(HospitalRecipeBean hospitalRecipe) {
        //传入数据校验
        HosRecipeResult result = validateHospitalRecipe(hospitalRecipe, 3);
        if (HosRecipeResult.FAIL.equals(result.getMsgCode())) {
            return result;
        }

        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        Recipe recipe = recipeDAO.getByOriginRecipeCodeAndOriginClinicOrgan(hospitalRecipe.getOriginRecipeNo(),
                Integer.parseInt(hospitalRecipe.getOriginOrganID()));
        if (null == recipe) {
            result.setMsgCode(HosRecipeResult.FAIL);
            result.setMsg("处方不存在");
            return result;
        }

        ThirdEnterpriseCallService takeDrugService = ApplicationUtils.getRecipeService(ThirdEnterpriseCallService.class, "takeDrugService");

        //处方状态
        //0已接受 1已支付 2已发药 8已取消
        String recipeStatus = hospitalRecipe.getRecipeStatus();
        if ("0".equals(recipeStatus)) {
            Map<String, Object> _attrMap = new HashMap<>();
            _attrMap.put("recipeCode", hospitalRecipe.getRecipeNo());
            recipeDAO.updateRecipeInfoByRecipeId(recipe.getRecipeId(), _attrMap);
        } else if ("8".equals(recipeStatus) || "2".equals(recipeStatus)) {
            Map<String, Object> paramMap = new HashMap<>();
            if ("2".equals(recipeStatus)) {
                paramMap.put("result", "1");
            } else {
                paramMap.put("result", "0");
                paramMap.put("reason", "处方取消");
            }
            paramMap.put("organId", hospitalRecipe.getOrganID());
            paramMap.put("recipeCode", hospitalRecipe.getRecipeNo());
            paramMap.put("sendDate", DateTime.now().toString(DateConversion.DEFAULT_DATE_TIME));
            ThirdResultBean backMap = takeDrugService.recordDrugStoreResult(paramMap);
            if (null != backMap && ThirdEnterpriseCallService.REQUEST_OK.equals(backMap.getCode())) {
                //成功
                logger.info("platformRecipeStatusUpdate 处理成功. recipeStatus=[{}]", recipeStatus);
            } else {
                result.setMsgCode(HosRecipeResult.FAIL);
                result.setMsg("平台处方状态更新失败,原因：" + backMap.getMsg());
            }
        } else if ("1".equals(recipeStatus)) {

        }

        return result;
    }

    /**
     * 查询处方 (下级医院调用)
     *
     * @param seachBean 查询条件
     * @return
     */
    @RpcService
    public HosRecipeResult platformRecipeSearch(HospitalSearchBean seachBean) {
        //传入数据校验
        List<SqlOperInfo> searchAttr = new ArrayList<>();
        HosRecipeResult result = validateHospitalSearch(seachBean, searchAttr);
        if (HosRecipeResult.FAIL.equals(result.getMsgCode())) {
            return result;
        }

        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        RecipeDetailDAO recipeDetailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);

        List<HospitalRecipeBean> backList = new ArrayList<>();
        PatientBean patient = iPatientService.getByIdCard(seachBean.getCertID());
        if (null == patient) {
            result.setMsgCode(HosRecipeResult.FAIL);
            result.setMsg("没有该患者的处方单");
            return result;
        } else {
            if (patient.getPatientName().equals(seachBean.getPatientName())) {
                searchAttr.add(new SqlOperInfo("mpiid", patient.getMpiId()));
            } else {
                result.setMsgCode(HosRecipeResult.FAIL);
                result.setMsg("患者姓名与平台不匹配");
                return result;
            }
        }
        searchAttr.add(new SqlOperInfo("fromflag", 0));
        if (StringUtils.isNotEmpty(seachBean.getDoctorID())) {
            EmploymentBean employment = iEmploymentService.getByJobNumberAndOrganId(seachBean.getDoctorID(), Integer.parseInt(seachBean.getOrganID()));
            if (null != employment) {
                searchAttr.add(new SqlOperInfo("doctor", employment.getDoctorId()));
            } else {
                result.setMsgCode(HosRecipeResult.FAIL);
                result.setMsg("平台无法找到医生执业点");
                return result;
            }
        }
        List<Recipe> dbList = recipeDAO.findRecipeListWithConditions(searchAttr);
        if (CollectionUtils.isNotEmpty(dbList)) {
            HospitalRecipeBean hospitalRecipeBean;
            for (Recipe recipe : dbList) {
                hospitalRecipeBean = new HospitalRecipeBean();
                try {
                    RecipeResultBean _result = hospitalRecipeBean.parseRecipe(recipe, recipeDetailDAO.findByRecipeId(recipe.getRecipeId()),
                            patient, iPatientService.findAllHealthCard(recipe.getMpiid(), recipe.getClinicOrgan()));
                    if (RecipeResultBean.SUCCESS.equals(_result.getCode())) {
                        backList.add(hospitalRecipeBean);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    logger.error("platformRecipeSearch 解析平台处方出错. recipeId=[{}]", recipe.getRecipeId());
                }
            }
        }

        result.setData(backList);
        return result;
    }

    /**
     * 发送下级医院处方
     *
     * @param recipeId   处方ID
     * @param subOrganId 下级机构ID
     * @return 结果
     */
    @RpcService
    public boolean sendSubOrganHisRecipe(Integer recipeId, String subOrganId) {
        if (null != recipeId && StringUtils.isNotEmpty(subOrganId)) {
            RecipeHisService hisService = ApplicationUtils.getRecipeService(RecipeHisService.class);
            Integer subOrgan = Integer.parseInt(subOrganId);
            //发送HIS消息
            //TODO 发送下级医院处方
            hisService.recipeSendHis(recipeId, subOrgan);
            return true;
        }

        return false;
    }

    /**
     * 校验处方查询条件
     *
     * @param seachBean
     * @return
     */
    private HosRecipeResult validateHospitalSearch(HospitalSearchBean seachBean, List<SqlOperInfo> searchAttr) {
        HosRecipeResult result = HosRecipeResult.getFail();

        if (null == seachBean) {
            result.setMsg("传入参数为空");
            return result;
        }

        if (StringUtils.isEmpty(seachBean.getOrganID())) {
            result.setMsg("开方机构为空");
            return result;
        } else {
            searchAttr.add(new SqlOperInfo("clinicOrgan", Integer.valueOf(seachBean.getOrganID())));
        }

        if (StringUtils.isEmpty(seachBean.getCertID())) {
            result.setMsg("患者身份证信息为空");
            return result;
        }

        if (StringUtils.isEmpty(seachBean.getPatientName())) {
            result.setMsg("患者姓名信息为空");
            return result;
        }

        SqlOperInfo time = new SqlOperInfo("signDate", ConditionOperator.BETWEEN, "");
        if (StringUtils.isEmpty(seachBean.getStartDate())) {
            result.setMsg("开始时间为空");
            return result;
        } else {
            try {
                time.setValue(DateTimeFormat.forPattern(DateConversion.DEFAULT_DATE_TIME).parseDateTime(seachBean.getStartDate()).toDate());
            } catch (Exception e) {
                result.setMsg("开始时间格式错误，请使用 yyyy-MM-dd HH:mm:ss 格式");
                return result;
            }
        }

        if (StringUtils.isEmpty(seachBean.getEndDate())) {
            time.setExtValue(DateTime.now().toDate());
        } else {
            try {
                time.setExtValue(DateTimeFormat.forPattern(DateConversion.DEFAULT_DATE_TIME).parseDateTime(seachBean.getEndDate()).toDate());
            } catch (Exception e) {
                result.setMsg("结束时间格式错误，请使用 yyyy-MM-dd HH:mm:ss 格式");
                return result;
            }
        }
        searchAttr.add(time);

        result.setMsgCode(HosRecipeResult.SUCCESS);
        return result;
    }

    /**
     * 校验医院处方信息
     *
     * @param obj 医院处方
     * @return 结果
     */
    private HosRecipeResult validateHospitalRecipe(Object obj, int flag) {
        HosRecipeResult result = HosRecipeResult.getFail();

        if (null == obj) {
            result.setMsg("传入参数为空");
            return result;
        }

        if (1 == flag) {
            //新增
            List<HospitalRecipeBean> hospitalRecipeList = (List<HospitalRecipeBean>) obj;
            HospitalRecipeBean hospitalRecipe;
            String prefix = "处方";
            for (int i = 0; i < hospitalRecipeList.size(); i++) {
                hospitalRecipe = hospitalRecipeList.get(i);
                prefix = prefix + i + ":";
                if (StringUtils.isEmpty(hospitalRecipe.getRecipeType())) {
                    result.setMsg(prefix + "处方类型为空");
                    return result;
                }

                if (StringUtils.isEmpty(hospitalRecipe.getRecipeNo())) {
                    result.setMsg(prefix + "处方编号为空");
                    return result;
                }

                if (StringUtils.isEmpty(hospitalRecipe.getOrganID())) {
                    result.setMsg(prefix + "开方机构为空");
                    return result;
                }

                if (StringUtils.isEmpty(hospitalRecipe.getDoctorID())) {
                    result.setMsg(prefix + "开方医生工号为空");
                    return result;
                }

                if (StringUtils.isEmpty(hospitalRecipe.getCertID())) {
                    result.setMsg(prefix + "患者身份证信息为空");
                    return result;
                }

                if (StringUtils.isEmpty(hospitalRecipe.getSubOrganId())) {
                    result.setMsg(prefix + "下级医院机构号为空");
                    return result;
                }

                if ("1".equals(hospitalRecipe.getGuardianFlag())) {
                    //监护人模式
                    if (StringUtils.isEmpty(hospitalRecipe.getPatientBirthDay())) {
                        result.setMsg(prefix + "患者出生日期为空");
                        return result;
                    } else {
                        try {
                            DateTimeFormat.forPattern(DateConversion.YYYY_MM_DD).parseDateTime(hospitalRecipe.getPatientBirthDay());
                        } catch (Exception e) {
                            result.setMsg(prefix + "患者出生日期格式错误，请使用 yyyy-MM-dd 格式");
                            return result;
                        }
                    }

                    if (StringUtils.isEmpty(hospitalRecipe.getPatientSex())) {
                        result.setMsg(prefix + "患者性别为空");
                        return result;
                    }
                }

                if (CollectionUtils.isEmpty(hospitalRecipe.getOrderList())) {
                    result.setMsg(prefix + "处方详情数据为空");
                    return result;
                }
            }
        } else if (2 == flag) {
            HospitalRecipeBean hospitalRecipe = (HospitalRecipeBean) obj;
            if (CollectionUtils.isEmpty(hospitalRecipe.getRecipeCodeList())) {
                result.setMsg("处方编号集合为空");
                return result;
            }

            if (StringUtils.isEmpty(hospitalRecipe.getOrganID())) {
                result.setMsg("开方机构为空");
                return result;
            }

            if (StringUtils.isEmpty(hospitalRecipe.getSubOrganId())) {
                result.setMsg("下级医院机构号为空");
                return result;
            }

        } else if (3 == flag) {
            HospitalRecipeBean hospitalRecipe = (HospitalRecipeBean) obj;
            if (StringUtils.isEmpty(hospitalRecipe.getRecipeNo())) {
                result.setMsg("处方编号为空");
                return result;
            }

            if (StringUtils.isEmpty(hospitalRecipe.getOriginRecipeNo())) {
                result.setMsg("源处方编号为空");
                return result;
            }

            if (StringUtils.isEmpty(hospitalRecipe.getOrganID())) {
                result.setMsg("开方机构为空");
                return result;
            }

            if (StringUtils.isEmpty(hospitalRecipe.getOriginOrganID())) {
                result.setMsg("源开方机构为空");
                return result;
            }

            if (StringUtils.isEmpty(hospitalRecipe.getRecipeStatus())) {
                result.setMsg("处方状态为空");
                return result;
            }
        }

        result.setMsgCode(HosRecipeResult.SUCCESS);
        return result;
    }
}
