package recipe.drugsenterprise;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.ngari.base.patient.model.PatientBean;
import com.ngari.base.patient.service.IPatientService;
import com.ngari.his.recipe.mode.DrugTakeChangeReqTO;
import com.ngari.infra.invoice.mode.InvoiceRecordDto;
import com.ngari.infra.invoice.service.InvoiceRecordService;
import com.ngari.infra.logistics.mode.OrganLogisticsManageDto;
import com.ngari.infra.logistics.service.IOrganLogisticsManageService;
import com.ngari.patient.dto.DepartmentDTO;
import com.ngari.patient.dto.DoctorDTO;
import com.ngari.patient.dto.OrganDTO;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.patient.service.*;
import com.ngari.recipe.common.RecipeResultBean;
import com.ngari.recipe.drug.model.AuditDrugListBean;
import com.ngari.recipe.drug.model.UpDownDrugBean;
import com.ngari.recipe.drugsenterprise.model.DrugsEnterpriseBean;
import com.ngari.recipe.entity.*;
import com.ngari.recipe.recipe.model.DrugListForThreeBean;
import com.ngari.recipe.recipe.model.RecipeAndOrderDetailBeanNoDs;
import ctd.controller.exception.ControllerException;
import ctd.dictionary.DictionaryController;
import ctd.persistence.DAOFactory;
import ctd.persistence.bean.QueryResult;
import ctd.persistence.exception.DAOException;
import ctd.util.AppContextHolder;
import ctd.util.BeanUtils;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import ctd.util.event.GlobalEventExecFactory;
import eh.utils.ValidateUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.ApplicationUtils;
import recipe.aop.LogRecord;
import recipe.bean.ThirdResultBean;
import recipe.constant.*;
import recipe.dao.*;
import recipe.drugsenterprise.bean.DrugsEnterpriseDTO;
import recipe.drugsenterprise.bean.StandardResultDTO;
import recipe.enumerate.status.*;
import recipe.hisservice.HisMqRequestInit;
import recipe.hisservice.HisRequestInit;
import recipe.hisservice.RecipeToHisMqService;
import recipe.hisservice.RecipeToHisService;
import recipe.hisservice.syncdata.HisSyncSupervisionService;
import recipe.hisservice.syncdata.SyncExecutorService;
import recipe.manager.*;
import recipe.presettle.RecipeOrderTypeEnum;
import recipe.purchase.CommonOrder;
import recipe.service.RecipeHisService;
import recipe.service.RecipeLogService;
import recipe.service.RecipeMsgService;
import recipe.service.RecipeOrderService;
import recipe.serviceprovider.BaseService;
import recipe.third.IFileDownloadService;
import recipe.third.IWXServiceInterface;
import recipe.thread.RecipeBusiThreadPool;
import recipe.util.*;
import recipe.vo.greenroom.InvoiceRecordVO;

import java.math.BigDecimal;
import java.util.*;


/**
 * 第三方药企调用接口,历史原因存在一些平台的接口
 * company: ngarihealth
 *
 * @author: 0184/yu_yun
 * @date:2017/4/20.
 */
@RpcBean(value = "takeDrugService")
public class ThirdEnterpriseCallService extends BaseService<DrugsEnterpriseBean> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ThirdEnterpriseCallService.class);

    public static final Integer REQUEST_OK = 200;

    /**
     * 重复调用
     */
    private static final int REQUEST_ERROR_REAPET = 222;

    /**
     * 请求参数不正确
     */
    private static final int REQUEST_ERROR = 412;

    /**
     * 检查订单
     */
    private static final Integer CHECK_ORDER = 2;

    /**
     * 检查处方
     */
    private static final Integer CHECK_RECIPE = 1;

    private IPatientService iPatientService = ApplicationUtils.getBaseService(IPatientService.class);

    private GroupRecipeManager groupRecipeManager = AppContextHolder.getBean("groupRecipeManager", GroupRecipeManager.class);
    @Autowired
    private OrderManager orderManager;
    @Autowired
    private InvoiceRecordService invoiceRecordService;
    @Autowired
    private IOrganLogisticsManageService iOrganLogisticsManageService;
    @Autowired
    private AuditDrugListDAO auditDrugListDAO;
    @Autowired
    private RecipeParameterDao recipeParameterDao;
    @Autowired
    private StateManager stateManager;
    @Autowired
    private RecipeOrderDAO recipeOrderDAO;
    @Autowired
    private HisRecipeDAO hisRecipeDAO;

    /**
     * 待配送状态
     *
     * @param paramMap
     */
    @RpcService
    public ThirdResultBean readyToSend(Map<String, Object> paramMap) {
        LOGGER.info("readyToSend param : " + JSONUtils.toString(paramMap));
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        RecipeDetailDAO recipeDetailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
        ThirdResultBean backMsg = ThirdResultBean.getFail();
        Recipe recipe = getRecipe(paramMap);
        if (null == recipe) {
            backMsg.setMsg("入参错误");
            return backMsg;
        }
        int code;
        if (recipe.getReviewType() == ReviewTypeConstant.Postposition_Check) {
            //为审方后置
            code = validateRecipe(paramMap, backMsg, RecipeStatusConstant.CHECK_PASS_YS, RecipeStatusConstant.WAIT_SEND, CHECK_RECIPE);
        } else {
            //前置或者不审核
            code = validateRecipe(paramMap, backMsg, RecipeStatusConstant.CHECK_PASS, RecipeStatusConstant.WAIT_SEND, CHECK_RECIPE);
        }

        if (REQUEST_ERROR_REAPET == code) {
            backMsg.setCode(REQUEST_OK);
            return backMsg;
        } else if (REQUEST_ERROR == code) {
            LOGGER.warn("recipeId=[{}], readyToSend:{}", backMsg.getBusId(), JSONUtils.toString(backMsg));
            return backMsg;
        }

        Integer recipeId = recipe.getRecipeId();
        String errorMsg = "";
        String sendDateStr = MapValueUtil.getString(paramMap, "sendDate");
        //此处为发药人
        String sender = MapValueUtil.getString(paramMap, "sender");

        Map<String, Object> attrMap = Maps.newHashMap();
        attrMap.put("startSendDate", DateConversion.parseDate(sendDateStr, DateConversion.DEFAULT_DATE_TIME));
        attrMap.put("sender", sender);
        //以免进行处方失效前提醒
        attrMap.put("remindFlag", 1);
        //更新处方信息
        Boolean rs = recipeDAO.updateRecipeInfoByRecipeId(recipeId, RecipeStatusConstant.WAIT_SEND, attrMap);

        stateManager.updateRecipeState(recipeId, RecipeStateEnum.PROCESS_STATE_DISPENSING,RecipeStateEnum.SUB_ORDER_DELIVERED_MEDICINE);
        RecipeOrder order = recipeOrderDAO.getByOrderCode(recipe.getOrderCode());
        if (Objects.nonNull(order)) {
            stateManager.updateOrderState(order.getOrderId(), OrderStateEnum.PROCESS_STATE_ORDER, OrderStateEnum.SUB_ORDER_DELIVERED_MEDICINE);
        }
        if (rs) {
            //记录日志
            RecipeLogService.saveRecipeLog(recipeId, RecipeStatusConstant.CHECK_PASS_YS, RecipeStatusConstant.WAIT_SEND, "待配送,配送人：" + sender);
        } else {
            code = ErrorCode.SERVICE_ERROR;
            errorMsg = "电子处方更新失败";
        }
        Integer depId = recipe.getEnterpriseId();
        DrugsEnterprise drugsEnterprise = drugsEnterpriseDAO.getById(depId);
        if (!DrugEnterpriseConstant.COMPANY_HR.equals(drugsEnterprise.getCallSys())) {
            Object listObj = paramMap.get("dtl");
            boolean detailRs = false;
            if (rs) {
                if (null != listObj) {
                    if (listObj instanceof List) {
                        List<HashMap<String, Object>> detailList = (List<HashMap<String, Object>>) paramMap.get("dtl");
                        if (CollectionUtils.isNotEmpty(detailList)) {

                            boolean drugSearchFlag = false;
                            //药品和详情关系 key:drugId  value:detailId
                            Map<Integer, Integer> detailIdAndDrugId = new HashMap<>(detailList.size());
                            //判断是传了dtlId或者drugId
                            Integer drugId = MapValueUtil.getInteger(detailList.get(0), "drugId");
                            if (null != drugId) {
                                drugSearchFlag = true;
                                List<Recipedetail> dbDetailList = recipeDetailDAO.findByRecipeId(recipeId);
                                for (Recipedetail recipedetail : dbDetailList) {
                                    detailIdAndDrugId.put(recipedetail.getDrugId(), recipedetail.getRecipeDetailId());
                                }
                            }

                            Map<String, Object> detailAttrMap;
                            Integer dtlId;
                            String goodId;
                            String drugBatch;
                            Date validDate;
                            Double qty;
                            Double price;
                            Double rate;
                            Double ratePrice;
                            Double totalPrice;
                            Double tax;
                            Double totalRatePrice;
                            for (HashMap<String, Object> detailMap : detailList) {
                                detailAttrMap = Maps.newHashMap();
                                if (drugSearchFlag) {
                                    dtlId = detailIdAndDrugId.get(MapValueUtil.getInteger(detailMap, "drugId"));
                                } else {
                                    dtlId = MapValueUtil.getInteger(detailMap, "dtlId");
                                }
                                goodId = MapValueUtil.getString(detailMap, "goodId");
                                drugBatch = MapValueUtil.getString(detailMap, "drugBatch");
                                validDate = DateConversion.parseDate(MapValueUtil.getString(detailMap, "validDate"), DateConversion.DEFAULT_DATE_TIME);
                                qty = MapValueUtil.getDouble(detailMap, "qty");
                                price = MapValueUtil.getDouble(detailMap, "price");
                                rate = MapValueUtil.getDouble(detailMap, "rate");
                                ratePrice = MapValueUtil.getDouble(detailMap, "ratePrice");
                                totalPrice = MapValueUtil.getDouble(detailMap, "value");
                                tax = MapValueUtil.getDouble(detailMap, "tax");
                                totalRatePrice = MapValueUtil.getDouble(detailMap, "sumValue");

                                //药品配送企业平台药品ID
                                detailAttrMap.put("drugCode", goodId);
                                detailAttrMap.put("drugBatch", drugBatch);
                                detailAttrMap.put("validDate", validDate);
                                detailAttrMap.put("sendNumber", qty);
                                if (null != price) {
                                    detailAttrMap.put("price", new BigDecimal(price));
                                }
                                detailAttrMap.put("rate", rate);
                                if (null != ratePrice) {
                                    detailAttrMap.put("ratePrice", new BigDecimal(ratePrice));
                                }
                                if (null != totalPrice) {
                                    detailAttrMap.put("totalPrice", new BigDecimal(totalPrice));
                                }
                                if (null != tax) {
                                    detailAttrMap.put("tax", new BigDecimal(tax));
                                }
                                if (null != totalRatePrice) {
                                    detailAttrMap.put("totalRatePrice", new BigDecimal(totalRatePrice));
                                }

                                if (null != dtlId) {
                                    boolean detailRs1 = recipeDetailDAO.updateRecipeDetailByRecipeDetailId(dtlId, detailAttrMap);
                                    if (detailRs1) {
                                        detailRs = true;
                                    } else {
                                        detailRs = false;
                                        code = ErrorCode.SERVICE_ERROR;
                                        errorMsg = "电子处方详情 ID为：" + dtlId + " 的药品更新失败";
                                        break;
                                    }
                                }
                            }
                        }
                    }
                } else {
                    detailRs = true;
                }
            }

            if (rs && detailRs) {
                code = REQUEST_OK;
                errorMsg = "";
            }
        }

        //监管平台核销上传
        SyncExecutorService syncExecutorService = ApplicationUtils.getRecipeService(SyncExecutorService.class);
        syncExecutorService.uploadRecipeVerificationIndicators(recipeId);

        backMsg.setCode(code);
        backMsg.setMsg(errorMsg);
        backMsg.setRecipe(null);
        LOGGER.info("readyToSend:" + JSONUtils.toString(backMsg));

        return backMsg;
    }

    /**
     * 该处方改成配送中
     *
     * @param paramMap
     * @return
     */
    @RpcService
    public ThirdResultBean toSend(Map<String, Object> paramMap) {
        LOGGER.info("toSend param : " + JSONUtils.toString(paramMap));

        ThirdResultBean backMsg = ThirdResultBean.getFail();
        int code = validateRecipe(paramMap, backMsg, OrderStatusConstant.READY_SEND, OrderStatusConstant.SENDING, CHECK_ORDER);

        if (REQUEST_ERROR_REAPET == code) {
            backMsg.setCode(REQUEST_OK);
            return backMsg;
        } else if (REQUEST_ERROR == code) {
            LOGGER.warn("recipeId=[{}], toSend:{}", backMsg.getBusId(), JSONUtils.toString(backMsg));
            return backMsg;
        }

        sendImpl(backMsg, paramMap);
        LOGGER.info("toSend:" + JSONUtils.toString(backMsg));

        return backMsg;
    }

    /**
     * 处方准备并配送接口 结合readyToSend 和toSend
     * 钥世圈使用
     *
     * @param list
     * @return
     */
    @RpcService
    public List<ThirdResultBean> send(List<Map<String, Object>> list) {
        LOGGER.info("send param : " + JSONUtils.toString(list));

        List<ThirdResultBean> result = new ArrayList<>();
        for (Map<String, Object> paramMap : list) {
            ThirdResultBean thirdResultBean = ThirdResultBean.getFail();
            thirdResultBean.setRecipeCode(MapValueUtil.getString(paramMap, "recipeCode"));
            int code = validateRecipe(paramMap, thirdResultBean, RecipeStatusConstant.CHECK_PASS_YS, RecipeStatusConstant.IN_SEND, CHECK_RECIPE);

            if (REQUEST_ERROR_REAPET == code) {
                thirdResultBean.setCode(REQUEST_OK);
            } else if (REQUEST_ERROR == code) {
                LOGGER.warn("recipeId=[{}], send:{}", thirdResultBean.getBusId(), JSONUtils.toString(thirdResultBean));
            } else if (REQUEST_OK == code) {
                sendImpl(thirdResultBean, paramMap);
            }
            result.add(thirdResultBean);
        }

        return result;
    }

    /**
     * 配送功能实现
     *
     * @param thirdResultBean
     * @param paramMap
     */
    @LogRecord
    private void sendImpl(ThirdResultBean thirdResultBean, Map<String, Object> paramMap) {
        RecipeOrderService orderService = ApplicationUtils.getRecipeService(RecipeOrderService.class);
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        RecipeOrderDAO recipeOrderDAO = DAOFactory.getDAO(RecipeOrderDAO.class);
        RecipeDetailDAO recipeDetailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
        EnterpriseManager enterpriseManager = AppContextHolder.getBean("enterpriseManager", EnterpriseManager.class);
        Recipe recipe = thirdResultBean.getRecipe();
        Integer recipeId = recipe.getRecipeId();
        String sendDateStr = MapValueUtil.getString(paramMap, "sendDate");
        Date sendDate = DateTime.now().toDate();
        if (StringUtils.isNotEmpty(sendDateStr)) {
            sendDate = DateConversion.parseDate(sendDateStr, DateConversion.DEFAULT_DATE_TIME);
        }
        //此处为配送人
        String sender = MapValueUtil.getString(paramMap, "sender");
        RecipeOrder order = recipeOrderDAO.getByOrderCode(recipe.getOrderCode());
        Map<String, Object> attrMap = Maps.newHashMap();
        attrMap.put("sendDate", sendDate);
        attrMap.put("sender", sender);
        //以免进行处方失效前提醒
        attrMap.put("remindFlag", 1);
        //更新处方信息
        Boolean rs = true;
        List<Recipe> recipes = orderManager.getRecipesByOrderCode(recipe.getOrderCode());
        for (Recipe recipe1 : recipes) {
            if (!rs) {
                return;
            }
            rs = recipeDAO.updateRecipeInfoByRecipeId(recipe1.getRecipeId(), RecipeStatusConstant.IN_SEND, attrMap);
            stateManager.updateRecipeState(recipe1.getRecipeId(), RecipeStateEnum.PROCESS_STATE_DISTRIBUTION,RecipeStateEnum.SUB_ORDER_DELIVERED);
        }
        stateManager.updateOrderState(order.getOrderId(),OrderStateEnum.PROCESS_STATE_ORDER,OrderStateEnum.SUB_ORDER_DELIVERED);

        if (rs) {
            updateRecipeDetainInfo(recipe, paramMap);
            Map<String, Object> orderAttr = getOrderInfoMap(recipe, paramMap);
            orderAttr.put("sendTime", sendDate);
            orderAttr.put("status", OrderStatusConstant.SENDING);
            //此处为物流公司字典
            String logisticsCompany = MapValueUtil.getString(paramMap, "logisticsCompany");
            String trackingNumber = MapValueUtil.getString(paramMap, "trackingNumber");
            try {
                logisticsCompany = StringUtils.isEmpty(logisticsCompany) ? null : Integer.valueOf(logisticsCompany).toString();
            } catch (Exception e) {
                //记录日志
                RecipeLogService.saveRecipeLog(recipeId, recipe.getStatus(), RecipeStatusConstant.IN_SEND, "配送中,配送人：" + sender
                        + ",快递公司：" + logisticsCompany + ",快递单号：" + trackingNumber);
                LOGGER.info("sendImpl logisticsCompany ={}", logisticsCompany, e);
                logisticsCompany = null;
            }
            //药店或者站点名称
            String drugStoreName = MapValueUtil.getString(paramMap, "drugStoreName");
            String drugStoreCode = MapValueUtil.getString(paramMap, "drugStoreCode");

            orderAttr.put("logisticsCompany", StringUtils.isEmpty(logisticsCompany) ? null : Integer.valueOf(logisticsCompany));
            orderAttr.put("trackingNumber", trackingNumber);
            orderAttr.put("drugStoreName", drugStoreName);
            orderAttr.put("drugStoreCode", drugStoreCode);

            //BUG#50679 【电子处方】配送中的订单详情页展示了完成时间 配送中不需要调用完成订单的接口
            RecipeResultBean resultBean = orderService.updateOrderInfo(recipe.getOrderCode(), orderAttr, null);
            LOGGER.info("toSend 订单更新 result={}", JSONUtils.toString(resultBean));
            try {
                // 更新处方、处方订单成功：药企对接物流的运单信息同步基础服务
                enterpriseManager.sendLogisticsInfoToBase(order, recipeId, logisticsCompany, trackingNumber);
            } catch (Exception e) {
                LOGGER.error("药企对接物流通知处方运单号，同步运单信息至基础服务异常=", e);
            }
            RecipeMsgService.batchSendMsg(recipeId, RecipeMsgEnum.EXPRESSINFO_REMIND.getStatus());
            String company = logisticsCompany;
            try {
                company = DictionaryController.instance().get("eh.infra.dictionary.LogisticsCode").getText(logisticsCompany);
            } catch (ControllerException e) {
                LOGGER.warn("toSend get logisticsCompany error. logisticsCompany={}", logisticsCompany, e);
            }
            //记录日志
            RecipeLogService.saveRecipeLog(recipeId, recipe.getStatus(), RecipeStatusConstant.IN_SEND, "配送中,配送人：" + sender
                    + ",快递公司：" + company + ",快递单号：" + trackingNumber);
            //信息推送
            RecipeMsgService.batchSendMsg(recipeId, RecipeStatusConstant.IN_SEND);
            //将快递公司快递单号信息用更新配送方式接口更新至his
            if (StringUtils.isNotEmpty(logisticsCompany) && StringUtils.isNotEmpty(trackingNumber)) {
                RecipeBusiThreadPool.submit(() -> {
                    RecipeToHisService service = AppContextHolder.getBean("recipeToHisService", RecipeToHisService.class);
                    List<Recipedetail> details = recipeDetailDAO.findByRecipeId(recipeId);
                    PatientBean patientBean = iPatientService.get(recipe.getMpiid());
                    DrugTakeChangeReqTO request = HisRequestInit.initDrugTakeChangeReqTO(recipe, details, patientBean, null);
                    service.drugTakeChange(request);
                    return null;
                });
                //监管平台上传配送信息(派药)
                RecipeBusiThreadPool.submit(() -> {
                    HisSyncSupervisionService hisSyncService = ApplicationUtils.getRecipeService(HisSyncSupervisionService.class);
                    hisSyncService.uploadSendMedicine(recipeId);

                    return null;
                });
            }
        } else {
            thirdResultBean.setCode(ErrorCode.SERVICE_ERROR);
            thirdResultBean.setMsg("电子处方更新失败");
        }
        thirdResultBean.setRecipe(null);
    }

    /**
     * 配送到家-处方完成方法
     *
     * @param paramMap
     * @return
     */
    @RpcService
    public ThirdResultBean finishRecipe(Map<String, Object> paramMap) {
        LOGGER.info("finishRecipe param : " + JSONUtils.toString(paramMap));
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        RecipeOrderDAO recipeOrderDAO = DAOFactory.getDAO(RecipeOrderDAO.class);
        ThirdResultBean backMsg = ThirdResultBean.getFail();
        int code = validateRecipe(paramMap, backMsg, OrderStatusConstant.SENDING, OrderStatusConstant.FINISH, CHECK_ORDER);
        StateManager stateManager = AppContextHolder.getBean("stateManager", StateManager.class);
        if (REQUEST_ERROR_REAPET == code) {
            backMsg.setCode(REQUEST_OK);
            return backMsg;
        } else if (REQUEST_ERROR == code) {
            LOGGER.warn("recipeId=[{}], finishRecipe:{}", backMsg.getBusId(), JSONUtils.toString(backMsg));
            return backMsg;
        }

        Recipe recipe = backMsg.getRecipe();
        Integer recipeId = recipe.getRecipeId();
        String errorMsg = "";
        String sendDateStr = MapValueUtil.getString(paramMap, "sendDate");
        //此处为配送人
        String sender = MapValueUtil.getString(paramMap, "sender");

        Map<String, Object> attrMap = Maps.newHashMap();
        attrMap.put("giveDate", StringUtils.isEmpty(sendDateStr) ? DateTime.now().toDate() :
                DateConversion.parseDate(sendDateStr, DateConversion.DEFAULT_DATE_TIME));
        attrMap.put("giveFlag", 1);
        //如果是货到付款还要更新付款时间和付款状态
        RecipeOrder order = recipeOrderDAO.getByOrderCode(recipe.getOrderCode());
        if (RecipeBussConstant.GIVEMODE_SEND_TO_HOME.equals(recipe.getGiveMode()) && RecipeBussConstant.PAYMODE_OFFLINE.equals(order.getPayMode())) {
            attrMap.put("payFlag", 1);
            attrMap.put("payDate", new Date());
        }
        String recipeFeeStr = MapValueUtil.getString(paramMap, "recipeFee");
        if (StringUtils.isNotEmpty(recipeFeeStr)) {
            attrMap.put("totalMoney", new BigDecimal(recipeFeeStr));
        }
        //更新处方信息
        Boolean rs = recipeDAO.updateRecipeInfoByRecipeId(recipeId, RecipeStatusConstant.FINISH, attrMap);

        List<Recipe> recipes = orderManager.getRecipesByOrderCode(recipe.getOrderCode());
        recipes.stream().forEach(recipe2 -> {
            recipeDAO.updateRecipeInfoByRecipeId(recipe2.getRecipeId(), RecipeStatusConstant.FINISH, attrMap);
            stateManager.updateRecipeState(recipe2.getRecipeId(), RecipeStateEnum.PROCESS_STATE_DONE, RecipeStateEnum.SUB_DONE_SEND);
        });

        if (rs) {
            //完成订单
            RecipeOrderService orderService = ApplicationUtils.getRecipeService(RecipeOrderService.class);
            RecipeHisService hisService = ApplicationUtils.getRecipeService(RecipeHisService.class);

            updateRecipeDetainInfo(recipe, paramMap);
            Map<String, Object> orderAttr = getOrderInfoMap(recipe, paramMap);
            orderService.finishOrder(recipe.getOrderCode(), orderAttr);
            stateManager.updateOrderState(order.getOrderId(), OrderStateEnum.PROCESS_STATE_DISPENSING, OrderStateEnum.SUB_DONE_SEND);

            //记录日志
            RecipeLogService.saveRecipeLog(recipeId, RecipeStatusConstant.IN_SEND, RecipeStatusConstant.FINISH, "配送到家处方单完成,配送人：" + sender);
            if (RecipeBussConstant.RECIPEMODE_ZJJGPT.equals(recipe.getRecipeMode())) {
                RecipeToHisMqService hisMqService = ApplicationUtils.getRecipeService(RecipeToHisMqService.class);
                hisMqService.recipeStatusToHis(HisMqRequestInit.initRecipeStatusToHisReq(recipe, HisBussConstant.TOHIS_RECIPE_STATUS_FINISH));
            } else {
                //HIS消息发送
                hisService.recipeFinish(recipeId);
            }
            if (RecipeBussConstant.GIVEMODE_SEND_TO_HOME.equals(recipe.getGiveMode())) {
                //配送到家
                RecipeMsgService.batchSendMsg(recipe, RecipeStatusConstant.PATIENT_REACHPAY_FINISH);
            }
            //更新pdf
            CommonOrder.finishGetDrugUpdatePdf(recipeId);
            //监管平台上传配送信息(配送到家-处方完成)
            RecipeBusiThreadPool.submit(() -> {
                LOGGER.info("finishRecipe start");
                long start = System.currentTimeMillis();
                HisSyncSupervisionService hisSyncService = ApplicationUtils.getRecipeService(HisSyncSupervisionService.class);
                hisSyncService.uploadFinishMedicine(recipeId);
                long elapsedTime = System.currentTimeMillis() - start;
                LOGGER.info("RecipeBusiThreadPool finishRecipe 监管平台上传配送信息(配送到家-处方完成) 执行时间:{}.", elapsedTime);
                return null;
            });

        } else {
            code = ErrorCode.SERVICE_ERROR;
            errorMsg = "电子处方更新失败";
        }

        backMsg.setCode(code);
        backMsg.setMsg(errorMsg);
        backMsg.setRecipe(null);
        LOGGER.info("finishRecipe:" + JSONUtils.toString(backMsg));

        return backMsg;
    }

    /**
     * 配送到家-处方完成方法
     *
     * @param paramMap
     * @return
     */
    @RpcService
    public ThirdResultBean RecipeFall(Map<String, Object> paramMap) {
        LOGGER.info("RecipeFall param : " + JSONUtils.toString(paramMap));
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        RecipeOrderDAO recipeOrderDAO = DAOFactory.getDAO(RecipeOrderDAO.class);
        ThirdResultBean backMsg = ThirdResultBean.getFail();
        int code = validateRecipe(paramMap, backMsg, null, OrderStatusConstant.FAIL, CHECK_ORDER);

        if (REQUEST_ERROR_REAPET == code) {
            backMsg.setCode(REQUEST_OK);
            return backMsg;
        } else if (REQUEST_ERROR == code) {
            LOGGER.warn("recipeId=[{}], RecipeFall:{}", backMsg.getBusId(), JSONUtils.toString(backMsg));
            return backMsg;
        }

        Recipe recipe = backMsg.getRecipe();
        Integer recipeId = recipe.getRecipeId();
        String errorMsg = "";
        String sendDateStr = MapValueUtil.getString(paramMap, "sendDate");
        Map<String, Object> attrMap = Maps.newHashMap();
        attrMap.put("giveDate", StringUtils.isEmpty(sendDateStr) ? DateTime.now().toDate() :
                DateConversion.parseDate(sendDateStr, DateConversion.DEFAULT_DATE_TIME));
        attrMap.put("giveFlag", 1);
        String recipeFeeStr = MapValueUtil.getString(paramMap, "recipeFee");
        if (StringUtils.isNotEmpty(recipeFeeStr)) {
            attrMap.put("totalMoney", new BigDecimal(recipeFeeStr));
        }
        //更新处方信息
        Boolean rs = recipeDAO.updateRecipeInfoByRecipeId(recipeId, RecipeStatusConstant.RECIPE_FAIL, attrMap);
        StateManager stateManager = AppContextHolder.getBean("stateManager", StateManager.class);
        stateManager.updateRecipeState(recipeId, RecipeStateEnum.PROCESS_STATE_CANCELLATION, RecipeStateEnum.SUB_CANCELLATION_TIMEOUT_NOT_MEDICINE);
        if (rs) {
            //患者未取药
            RecipeOrderService orderService = ApplicationUtils.getRecipeService(RecipeOrderService.class);
            Map<String, Object> orderAttrMap = new HashMap();

            orderAttrMap.put("status", OrderStatusConstant.FAIL);
            orderAttrMap.put("cancelReason", MapValueUtil.getString(paramMap, "cancelReason"));
            orderAttrMap.put("effective", 0);
            RecipeResultBean result = orderService.updateOrderInfo(recipe.getOrderCode(), orderAttrMap, null);
            RecipeOrder order = recipeOrderDAO.getByOrderCode(recipe.getOrderCode());
            if (Objects.nonNull(order)) {
                stateManager.updateOrderState(order.getOrderId(), OrderStateEnum.PROCESS_STATE_CANCELLATION, OrderStateEnum.SUB_CANCELLATION_TIMEOUT_NOT_MEDICINE);
            }
            if (RecipeResultBean.FAIL == result.getCode()) {
                code = ErrorCode.SERVICE_ERROR;
                errorMsg = "处方订单更新失败";
            } else {
                RecipeLogService.saveRecipeLog(recipeId, recipe.getStatus(), RecipeStatusConstant.RECIPE_FAIL, "取药失败，原因:" + MapValueUtil.getString(paramMap, "cancelReason"));
                //发送取药失败消息
                RecipeMsgService.batchSendMsg(recipeId, RecipeStatusConstant.NO_DRUG);
            }

            groupRecipeManager.updateGroupRecipe(recipeId, recipe.getOrderCode(), RecipeStatusConstant.RECIPE_FAIL);
        } else {
            code = ErrorCode.SERVICE_ERROR;
            errorMsg = "电子处方更新失败";
        }
        backMsg.setCode(code);
        backMsg.setMsg(errorMsg);
        backMsg.setRecipe(null);
        LOGGER.info("RecipeFall:" + JSONUtils.toString(backMsg));

        return backMsg;
    }

    /**
     * 更新处方相关信息
     *
     * @param paramMap
     * @return
     */
    @RpcService
    public ThirdResultBean updateRecipeInfo(Map<String, Object> paramMap) {
        //国药会大量重复调用，故去掉该日志
        LOGGER.info("updateRecipeInfo param : " + JSONUtils.toString(paramMap));
        RecipeDetailDAO recipeDetailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
        ThirdResultBean backMsg = ThirdResultBean.getFail();
        int code = validateRecipe(paramMap, backMsg, null, null, CHECK_RECIPE);

        if (REQUEST_OK != code) {
            LOGGER.warn("updateRecipeInfo error. info={}, recipeId=[{}]", JSONUtils.toString(backMsg), backMsg.getBusId());
            return backMsg;
        }

        Recipe recipe = backMsg.getRecipe();
        Integer recipeId = recipe.getRecipeId();
        String errorMsg = "";
        Object listObj = paramMap.get("dtl");
        boolean detailRs = false;
        if (null != listObj) {
            if (listObj instanceof List) {
                List<HashMap<String, Object>> detailList = (List<HashMap<String, Object>>) paramMap.get("dtl");
                if (CollectionUtils.isNotEmpty(detailList)) {

                    boolean drugSearchFlag = false;
                    //药品和详情关系 key:drugId  value:detailId
                    Map<Integer, Integer> detailIdAndDrugId = new HashMap<>(detailList.size());
                    //判断是传了dtlId或者drugId
                    String drugId = MapValueUtil.getString(detailList.get(0), "drugId");
                    if (StringUtils.isNotEmpty(drugId)) {
                        drugSearchFlag = true;
                        List<Recipedetail> dbDetailList = recipeDetailDAO.findByRecipeId(recipeId);
                        for (Recipedetail recipedetail : dbDetailList) {
                            detailIdAndDrugId.put(recipedetail.getDrugId(), recipedetail.getRecipeDetailId());
                        }
                    }

                    Map<String, Object> detailAttrMap;
                    Integer dtlId;
                    String invoiceNo;
                    Date invoiceDate;
                    for (HashMap<String, Object> detailMap : detailList) {
                        detailAttrMap = Maps.newHashMap();
                        if (drugSearchFlag) {
                            dtlId = detailIdAndDrugId.get(MapValueUtil.getInteger(detailMap, "drugId"));
                        } else {
                            dtlId = MapValueUtil.getInteger(detailMap, "dtlId");
                        }
                        invoiceNo = MapValueUtil.getString(detailMap, "invoiceNo");
                        invoiceDate = DateConversion.parseDate(MapValueUtil.getString(detailMap, "invoiceDate"), DateConversion.DEFAULT_DATE_TIME);

                        //药品配送企业平台药品ID
                        detailAttrMap.put("invoiceNo", invoiceNo);
                        detailAttrMap.put("invoiceDate", invoiceDate);

                        if (null != dtlId) {
                            boolean detailRs1 = recipeDetailDAO.updateRecipeDetailByRecipeDetailId(dtlId, detailAttrMap);
                            if (detailRs1) {
                                detailRs = true;
                            } else {
                                detailRs = false;
                                code = ErrorCode.SERVICE_ERROR;
                                errorMsg = "电子处方详情 ID为：" + dtlId + " 的药品更新失败";
                                break;
                            }
                        }
                    }
                }
            }
        } else {
            detailRs = true;
        }

        RecipeOrderService orderService = ApplicationUtils.getRecipeService(RecipeOrderService.class);
        //此处为物流公司字典
        String logisticsCompany = MapValueUtil.getString(paramMap, "logisticsCompany");
        String trackingNumber = MapValueUtil.getString(paramMap, "trackingNumber");
        Map<String, Object> orderAttr = getOrderInfoMap(recipe, paramMap);
        //此处为物流公司字典
        orderAttr.put("logisticsCompany", StringUtils.isEmpty(logisticsCompany) ? null : Integer.valueOf(logisticsCompany));
        orderAttr.put("trackingNumber", trackingNumber);
        RecipeResultBean resultBean = orderService.updateOrderInfo(recipe.getOrderCode(), orderAttr, null);

        if (detailRs && null != resultBean && resultBean.getCode().equals(RecipeResultBean.SUCCESS)) {
            code = REQUEST_OK;
            errorMsg = "";
        }

        String recipeCodeStr = MapValueUtil.getString(paramMap, "recipeCode");
        if (StringUtils.isNotEmpty(recipeCodeStr)) {
            //钥世圈采用该字段协议
            if (recipe.getStatus().equals(RecipeStatusConstant.CHECK_PASS_YS)
                    || recipe.getStatus().equals(RecipeStatusConstant.WAIT_SEND)) {
                paramMap.put("sendDate", DateTime.now().toString(DateConversion.DEFAULT_DATE_TIME));
                paramMap.put("sender", "system");
                //执行待配送
                readyToSend(paramMap);
                //执行配送中
                toSend(paramMap);
            }
        }

        backMsg.setCode(code);
        backMsg.setMsg(errorMsg);
        backMsg.setRecipe(null);
        LOGGER.info("updateRecipeInfo:" + JSONUtils.toString(backMsg));

        return backMsg;
    }

    /**
     * 药企更新药品配送状态
     *
     * @param paramMap 参数
     * @return
     */
    @RpcService
    public Map<String, Object> setDrugInventory(Map<String, Object> paramMap) {
        LOGGER.info("setDrugInventory param : " + JSONUtils.toString(paramMap));
        DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        SaleDrugListDAO saleDrugListDAO = DAOFactory.getDAO(SaleDrugListDAO.class);
        Map<String, Object> map = Maps.newHashMap();
        int code = REQUEST_OK;
        String msg = "";

        String account = MapValueUtil.getString(paramMap, "account");
        //药品状态 1-有效 0-无效
        Integer status = MapValueUtil.getInteger(paramMap, "status");
        Object goodsIdObj = paramMap.get("goodsId");
        List<Integer> goodsIds = null;
        if (null != goodsIdObj && goodsIdObj instanceof List) {
            goodsIds = (List<Integer>) goodsIdObj;
        }

        if (StringUtils.isEmpty(account) || CollectionUtils.isEmpty(goodsIds)) {
            code = ErrorCode.SERVICE_ERROR;
            msg = "账户为空或者商品号为空";
        }

        if (code == REQUEST_OK) {
            DrugsEnterprise drugsEnterprise = drugsEnterpriseDAO.getByAccount(account);
            if (null == drugsEnterprise) {
                code = ErrorCode.SERVICE_ERROR;
                msg = "此账户不存在";
            } else {
                Integer drugOrganID = drugsEnterprise.getId();
                if (1 == status) {
                    saleDrugListDAO.updateEffectiveByOrganIdAndDrugIds(drugOrganID, goodsIds);
                } else if (0 == status) {
                    saleDrugListDAO.updateInvalidByOrganIdAndDrugIds(drugOrganID, goodsIds);
                }
            }
        }

        map.put("code", code);
        map.put("msg", msg);
        return map;
    }

    /**
     * 药店取药结果记录
     *
     * @param paramMap
     * @return
     */
    @RpcService
    public ThirdResultBean recordDrugStoreResult(Map<String, Object> paramMap) {
        LOGGER.info("recordDrugStoreResult param : " + JSONUtils.toString(paramMap));
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        RecipeOrderDAO recipeOrderDAO = DAOFactory.getDAO(RecipeOrderDAO.class);
        ThirdResultBean backMsg = ThirdResultBean.getFail();
        int code = validateRecipe(paramMap, backMsg, null, null, CHECK_RECIPE);

        if (REQUEST_OK != code) {
            LOGGER.warn("recipeId=[{}], recordDrugStoreResult:{}", backMsg.getBusId(), JSONUtils.toString(backMsg));
            return backMsg;
        }

        RecipeOrderService orderService = ApplicationUtils.getRecipeService(RecipeOrderService.class);
        RecipeHisService hisService = ApplicationUtils.getRecipeService(RecipeHisService.class);
        StateManager stateManager = AppContextHolder.getBean("stateManager", StateManager.class);
        Recipe recipe = backMsg.getRecipe();
        Integer recipeId = recipe.getRecipeId();
        String errorMsg = "";
        Map<String, Object> attrMap = Maps.newHashMap();
        String sendDateStr = MapValueUtil.getString(paramMap, "sendDate");
        String result = MapValueUtil.getString(paramMap, "result");
        String result1 = "1";
        int status;
        if (result1.equals(result)) {
            //取药成功
            //修改处方单信息
            attrMap.put("giveDate", StringUtils.isEmpty(sendDateStr) ? DateTime.now().toDate() :
                    DateConversion.parseDate(sendDateStr, DateConversion.DEFAULT_DATE_TIME));
            attrMap.put("giveFlag", 1);
            attrMap.put("payFlag", 1);
            if (Objects.isNull(recipe.getPayDate())) {
                attrMap.put("payDate", attrMap.get("giveDate"));
            }
            String recipeFeeStr = MapValueUtil.getString(paramMap, "recipeFee");
            if (StringUtils.isNotEmpty(recipeFeeStr)) {
                attrMap.put("totalMoney", new BigDecimal(recipeFeeStr));
            }
            //更新处方信息
            Boolean rs = recipeDAO.updateRecipeInfoByRecipeId(recipeId, RecipeStatusConstant.FINISH, attrMap);
            status = RecipeStatusConstant.FINISH;
            if (rs) {
                updateRecipeDetainInfo(recipe, paramMap);
                Map<String, Object> orderAttr = getOrderInfoMap(recipe, paramMap);
                //完成订单，不需要检查订单有效性，就算失效的订单也直接变成已完成
                orderService.finishOrder(recipe.getOrderCode(), orderAttr);
                //记录日志
                RecipeLogService.saveRecipeLog(recipeId, recipe.getStatus(), RecipeStatusConstant.FINISH, "到店取药订单完成");
                if (RecipeBussConstant.RECIPEMODE_ZJJGPT.equals(recipe.getRecipeMode())) {
                    RecipeToHisMqService hisMqService = ApplicationUtils.getRecipeService(RecipeToHisMqService.class);
                    hisMqService.recipeStatusToHis(HisMqRequestInit.initRecipeStatusToHisReq(recipe, HisBussConstant.TOHIS_RECIPE_STATUS_FINISH));
                } else {
                    //HIS消息发送
                    hisService.recipeFinish(recipeId);
                }
                //发送取药完成消息
                RecipeMsgService.batchSendMsg(recipeId, RecipeStatusConstant.RECIPE_TAKE_MEDICINE_FINISH);

                //监管平台核销上传
                SyncExecutorService syncExecutorService = ApplicationUtils.getRecipeService(SyncExecutorService.class);
                syncExecutorService.uploadRecipeVerificationIndicators(recipeId);
                //更新pdf
                CommonOrder.finishGetDrugUpdatePdf(recipeId);
                // 处方 订单 新状态写入
                stateManager.updateRecipeState(recipeId, RecipeStateEnum.PROCESS_STATE_DONE, RecipeStateEnum.SUB_DONE_SELF_TAKE);
                RecipeOrder order = recipeOrderDAO.getByOrderCode(recipe.getOrderCode());
                if (Objects.nonNull(order)) {
                    stateManager.updateOrderState(order.getOrderId(), OrderStateEnum.PROCESS_STATE_DISPENSING, OrderStateEnum.SUB_DONE_SELF_TAKE);
                }
            } else {
                code = ErrorCode.SERVICE_ERROR;
                errorMsg = "电子处方更新失败";
            }
        } else {
            //患者未取药
            Boolean rs = recipeDAO.updateRecipeInfoByRecipeId(recipeId, RecipeStatusConstant.NO_DRUG, attrMap);
            stateManager.updateRecipeState(recipeId, RecipeStateEnum.PROCESS_STATE_CANCELLATION, RecipeStateEnum.SUB_CANCELLATION_TIMEOUT_NOT_MEDICINE);
            status = RecipeStatusConstant.NO_DRUG;
            if (rs) {
                RecipeOrder order = recipeOrderDAO.getByOrderCode(recipe.getOrderCode());
                orderService.cancelOrderByCode(recipe.getOrderCode(), OrderStatusConstant.CANCEL_AUTO);
                if (Objects.nonNull(order)) {
                    stateManager.updateOrderState(order.getOrderId(), OrderStateEnum.PROCESS_STATE_CANCELLATION, OrderStateEnum.SUB_CANCELLATION_DOCTOR_REPEAL);
                }
                RecipeLogService.saveRecipeLog(recipeId, recipe.getStatus(), RecipeStatusConstant.NO_DRUG, "到店取药失败，原因:" + MapValueUtil.getString(paramMap, "reason"));
                //发送取药失败消息
                RecipeMsgService.batchSendMsg(recipeId, RecipeStatusConstant.NO_DRUG);
            }
        }
        groupRecipeManager.updateGroupRecipe(recipeId, recipe.getOrderCode(), status);
        backMsg.setCode(code);
        backMsg.setMsg(errorMsg);
        backMsg.setRecipe(null);
        LOGGER.info("recordDrugStoreResult:" + JSONUtils.toString(backMsg));
        return backMsg;
    }

    /**
     * 钥世圈处方用户确认回调
     *
     * @param paramMap
     * @return
     */
    @RpcService
    public ThirdResultBean userConfirm(Map<String, Object> paramMap) {
        LOGGER.info("userConfirm param : " + JSONUtils.toString(paramMap));
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        RecipeOrderDAO recipeOrderDAO = DAOFactory.getDAO(RecipeOrderDAO.class);
        ThirdResultBean backMsg = ThirdResultBean.getFail();
        int code = validateRecipe(paramMap, backMsg, null, null, CHECK_RECIPE);

        if (REQUEST_OK != code) {
            LOGGER.warn("recipeId=[{}], userConfirm:{}", backMsg.getBusId(), JSONUtils.toString(backMsg));
            return backMsg;
        }

        Recipe recipe = backMsg.getRecipe();
        Integer recipeId = recipe.getRecipeId();
        backMsg.setMsg("");
        String errorMsg = "";
        String result = MapValueUtil.getString(paramMap, "result");
        String result1 = "1";
        if (result1.equals(result)) {
            RecipeOrderService orderService = ApplicationUtils.getRecipeService(RecipeOrderService.class);

            String sendMethod = MapValueUtil.getString(paramMap, "sendMethod");
            RecipeOrder order = null;
            Integer payMode = null;
            String sendMethod0 = "0";
            String sendMethod1 = "1";
            if (StringUtils.isNotEmpty(sendMethod)) {
                if (sendMethod0.equals(sendMethod)) {
                    payMode = RecipeBussConstant.PAYMODE_COD;
                } else if (sendMethod1.equals(sendMethod)) {
                    payMode = RecipeBussConstant.PAYMODE_TFDS;
                } else {
                    code = REQUEST_ERROR;
                    errorMsg = "不支持的购药方式";
                }

                if (null != payMode) {
                    //需要先去处理订单为有效订单
                    orderService.finishOrderPayWithoutPay(recipe.getOrderCode(), payMode);
                    order = recipeOrderDAO.getByOrderCode(recipe.getOrderCode());
                    if (null == order) {
                        code = REQUEST_ERROR;
                        errorMsg = "该处方没有关联订单";
                    }
                }
            } else {
                code = REQUEST_ERROR;
                errorMsg = "没有购药方式";
            }

            if (REQUEST_OK == code) {
                Map<String, Object> orderAttrMap = Maps.newHashMap();

                String receiver = MapValueUtil.getString(paramMap, "receiver");
                String mobile = MapValueUtil.getString(paramMap, "recMobile");
                String completeAddress = MapValueUtil.getString(paramMap, "completeAddress");

                //简化版地址处理
                if (StringUtils.isNotEmpty(completeAddress) && StringUtils.isNotEmpty(receiver) && StringUtils.isNotEmpty(mobile)) {
                    orderAttrMap.put("receiver", receiver);
                    orderAttrMap.put("recMobile", mobile);
                    recipeDAO.updateRecipeInfoByRecipeId(recipeId, ImmutableMap.of("address4", completeAddress));
                }
                orderAttrMap.put("drugStoreName", MapValueUtil.getString(paramMap, "drugstore"));
                orderAttrMap.put("drugStoreAddr", MapValueUtil.getString(paramMap, "drugstoreAddr"));
                orderService.updateOrderInfo(order.getOrderCode(), orderAttrMap, null);

                RecipeLogService.saveRecipeLog(recipeId, RecipeStatusConstant.READY_CHECK_YS, RecipeStatusConstant.READY_CHECK_YS, "userConfirm 用户确认处方，result=" + result);

                //拼装微信地址
                String needWxUrl = MapValueUtil.getString(paramMap, "wxUrl");
                if (StringUtils.isEmpty(needWxUrl)) {
                    String appid = MapValueUtil.getString(paramMap, "appid");
                    if (StringUtils.isNotEmpty(appid)) {
                        if ("NgariHealth".equals(appid)) {
                            //APP跳转
                            backMsg.setMsg(appid + "://?module=orderList");
                        } else {
                            //微信跳转
                            IWXServiceInterface wxService = AppContextHolder.getBean("wx.wxService", IWXServiceInterface.class);
                            Map<String, String> paramsMap = Maps.newHashMap();
                            paramsMap.put("module", "orderList");
                            String wxUrl = wxService.getSinglePageUrl(appid, paramsMap);

                            if (StringUtils.isNotEmpty(wxUrl)) {
                                wxUrl = wxUrl.replace("&connect_redirect=1", "");
                                backMsg.setMsg(wxUrl);
                            }
                        }
                    }
                }
            }
        }

        backMsg.setCode(code);
        if (REQUEST_OK != code) {
            backMsg.setMsg(errorMsg);
        }
        backMsg.setRecipe(null);
        LOGGER.info("userConfirm:" + JSONUtils.toString(backMsg));

        return backMsg;
    }


    /**
     * 校验处方相关信息
     *
     * @param paramMap
     * @param beforeStatus 有值进行校验，没值不校验
     * @return
     */
    private int validateRecipe(Map<String, Object> paramMap, ThirdResultBean thirdResultBean, Integer beforeStatus, Integer afterStatus, Integer checkStatus) {
        int code = REQUEST_OK;
        if (null == paramMap) {
            code = REQUEST_ERROR;
            return code;
        }
        String errorMsg = "";
        Recipe recipe;
        //处方查询条件可分为
        //1 处方ID
        //2 recipeCode由  机构ID-处方编号  组成 （钥世圈）
        recipe = getRecipe(paramMap);

        if (null == recipe) {
            code = REQUEST_ERROR;
            errorMsg = "该处方不存在";
            return code;
        }
        RecipeOrderDAO recipeOrderDAO = DAOFactory.getDAO(RecipeOrderDAO.class);
        RecipeOrder order = recipeOrderDAO.getOrderByRecipeId(recipe.getRecipeId());
        if (order == null) {
            code = REQUEST_ERROR;
            errorMsg = "处方未获取到有效订单";
        }
        //配送到家-处方完成方法，处方准备并配送接口，该处方改成配送中，待配送状态
        //配送中->已完成，审核通过->配送中，待配送->配送中，审核通过->带配送
        if (REQUEST_OK == code && null != beforeStatus) {
            if (CHECK_ORDER.equals(checkStatus)) {
                if (!order.getStatus().equals(beforeStatus) && RecipeOrderStatusEnum.ORDER_STATUS_DONE_DISPENSING.getType() != order.getStatus()) {
                    if (order.getStatus().equals(afterStatus)) {
                        code = REQUEST_ERROR_REAPET;
                    } else {
                        code = REQUEST_ERROR;
                        if (OrderStatusConstant.READY_SEND == beforeStatus) {
                            errorMsg = "该处方单不是待配送的处方";
                        } else if (OrderStatusConstant.SENDING == beforeStatus) {
                            errorMsg = "该处方单不是配送中的处方";
                        }
                    }
                }
            } else {
                if (!recipe.getStatus().equals(beforeStatus)) {
                    if (recipe.getStatus().equals(afterStatus)) {
                        code = REQUEST_ERROR_REAPET;
                    } else {
                        code = REQUEST_ERROR;
                        if (RecipeStatusConstant.CHECK_PASS_YS == beforeStatus) {
                            errorMsg = "该处方单不是药师审核通过的处方";
                        } else if (RecipeStatusConstant.WAIT_SEND == beforeStatus) {
                            errorMsg = "该处方单不是待配送的处方";
                        } else if (RecipeStatusConstant.IN_SEND == beforeStatus) {
                            errorMsg = "该处方单不是配送中的处方";
                        }
                    }
                }
            }

        }

        thirdResultBean.setBusId((null == recipe) ? null : recipe.getRecipeId());
        if (REQUEST_OK != code) {
            thirdResultBean.setCode(code);
            thirdResultBean.setMsg(errorMsg);
        } else {
            thirdResultBean.setCode(REQUEST_OK);
            thirdResultBean.setMsg("");
            thirdResultBean.setRecipe(recipe);
        }

        return code;
    }

    private Recipe getRecipe(Map<String, Object> paramMap) {
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        Integer recipeId = MapValueUtil.getInteger(paramMap, "recipeId");
        Recipe recipe = null;
        if (null != recipeId) {
            recipe = recipeDAO.getByRecipeId(recipeId);
        } else {
            Integer organId = MapValueUtil.getInteger(paramMap, "organId");
            //该编号有可能组成: 机构ID-处方编号
            String recipeCodeStr = MapValueUtil.getString(paramMap, "recipeCode");
            if (StringUtils.isNotEmpty(recipeCodeStr)) {
                if (recipeCodeStr.contains(YsqRemoteService.YSQ_SPLIT)) {
                    String[] recipeCodeInfo = recipeCodeStr.split(
                            YsqRemoteService.YSQ_SPLIT);
                    Integer length2 = 2;
                    if (null != recipeCodeInfo && length2 == recipeCodeInfo.length) {
                        organId = Integer.parseInt(recipeCodeInfo[0]);
                        recipeCodeStr = recipeCodeInfo[1];
                    }
                }
            }

            if (null != organId && StringUtils.isNotEmpty(recipeCodeStr)) {
                recipe = recipeDAO.getByRecipeCodeAndClinicOrgan(recipeCodeStr, organId);
            }
        }
        return recipe;
    }


    /**
     * 获取订单修改信息
     *
     * @param paramMap
     * @return
     */
    private Map getOrderInfoMap(Recipe recipe, Map<String, Object> paramMap) {
        Map<String, Object> attrMap = Maps.newHashMap();
        // 由于只有钥世圈在用，所以实际支付价格跟总价一致，无需考虑优惠券
        if (!RecipeBussConstant.GIVEMODE_SEND_TO_HOME.equals(recipe.getGiveMode()) &&
                !RecipeBussConstant.GIVEMODE_TFDS.equals(recipe.getGiveMode())) {
            return attrMap;
        }
        String recipeFeeStr = MapValueUtil.getString(paramMap, "recipeFee");
        if (StringUtils.isNotEmpty(recipeFeeStr)) {
            attrMap.put("recipeFee", new BigDecimal(recipeFeeStr));
        }
        String expressFeeStr = MapValueUtil.getString(paramMap, "expressFee");
        if (StringUtils.isNotEmpty(expressFeeStr)) {
            attrMap.put("expressFee", new BigDecimal(expressFeeStr));
        }
        String totalFeeStr = MapValueUtil.getString(paramMap, "totalFee");
        if (StringUtils.isNotEmpty(totalFeeStr)) {
            BigDecimal totalFee = new BigDecimal(totalFeeStr);
            attrMap.put("totalFee", totalFee);
            attrMap.put("actualPrice", totalFee.doubleValue());
        }
        return attrMap;
    }

    /**
     * 更新处方详细信息
     *
     * @param recipe
     * @param paramMap
     */
    private void updateRecipeDetainInfo(Recipe recipe, Map<String, Object> paramMap) {
        RecipeDetailDAO recipeDetailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
        List<Map<String, String>> list = MapValueUtil.getList(paramMap, "details");
        if (CollectionUtils.isNotEmpty(list)) {
            List<Recipedetail> detailList = recipeDetailDAO.findByRecipeId(recipe.getRecipeId());
            Integer goodId;
            BigDecimal salePrice;
            BigDecimal drugCost;
            Map<String, Object> changeAttr = Maps.newHashMap();
            for (Map<String, String> detailInfo : list) {
                changeAttr.clear();
                goodId = MapValueUtil.getInteger(detailInfo, "goodId");
                if (null != goodId) {
                    for (Recipedetail recipedetail : detailList) {
                        if (recipedetail.getDrugId().equals(goodId)) {
                            //更新信息
                            salePrice = MapValueUtil.getBigDecimal(detailInfo, "goodPrice");
                            if (null != salePrice) {
                                changeAttr.put("salePrice", salePrice);
                            }
                            drugCost = MapValueUtil.getBigDecimal(detailInfo, "goodCost");
                            if (null != drugCost) {
                                changeAttr.put("drugCost", drugCost);
                            }
                            if (!changeAttr.isEmpty()) {
                                recipeDetailDAO.updateRecipeDetailByRecipeDetailId(recipedetail.getRecipeDetailId(), changeAttr);
                            }
                            break;
                        }
                    }
                }
            }
        }
    }

    /******************************************药企相关接口 运营平台还在调用该接口****************************************/

    /**
     * 有效药企查询 status为1
     *
     * @param status 药企状态
     * @return
     */
    @RpcService
    public List<DrugsEnterpriseBean> findDrugsEnterpriseByStatus(final Integer status) {
        DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        List<DrugsEnterprise> depList = drugsEnterpriseDAO.findAllDrugsEnterpriseByStatus(status);
        return getList(depList, DrugsEnterpriseBean.class);
    }

    /**
     * 新建药企
     *
     * @param drugsEnterprise
     * @return
     * @author houxr 2016-09-11
     */
    @RpcService
    public DrugsEnterpriseBean addDrugsEnterprise(final DrugsEnterprise drugsEnterprise) {
        DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        if (null == drugsEnterprise) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "DrugsEnterprise is null");
        }
        List<DrugsEnterprise> drugsEnterpriseList = drugsEnterpriseDAO.findAllDrugsEnterpriseByName(drugsEnterprise.getName());
        if (drugsEnterpriseList.size() != 0) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "DrugsEnterprise exist!");
        }
        drugsEnterprise.setSort(100);
        //默认添加的药企当作测试药企处理
        drugsEnterprise.setCallSys("test");
        Date now = DateTime.now().toDate();
        drugsEnterprise.setCreateDate(now);
        drugsEnterprise.setLastModify(now);
        DrugsEnterprise newDrugsEnterprise = drugsEnterpriseDAO.save(drugsEnterprise);
        return getBean(newDrugsEnterprise, DrugsEnterpriseBean.class);
    }


    /**
     * 更新药企
     *
     * @param drugsEnterprise
     * @return
     * @author houxr 2016-09-11
     */
    @RpcService
    public DrugsEnterpriseBean updateDrugsEnterprise(DrugsEnterprise drugsEnterprise) {
        DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        if (null == drugsEnterprise) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "DrugsEnterprise is null");
        }
        LOGGER.info(JSONUtils.toString(drugsEnterprise));
        DrugsEnterprise target = drugsEnterpriseDAO.get(drugsEnterprise.getId());
        if (null == target) {
            throw new DAOException(DAOException.ENTITIY_NOT_FOUND, "DrugsEnterprise not exist!");
        }
        BeanUtils.map(drugsEnterprise, target);
        target = drugsEnterpriseDAO.update(target);
        return ObjectCopyUtils.convert(target, DrugsEnterpriseBean.class);
    }

    /**
     * 根据药企名称分页查询药企
     *
     * @param name  药企名称
     * @param start 分页起始位置
     * @param limit 每页条数
     * @return
     * @author houxr 2016-09-11
     */
    @RpcService
    @Deprecated
    public QueryResult<DrugsEnterpriseBean> queryDrugsEnterpriseByStartAndLimit(final String name, final Integer createType, final int start, final int limit) {
        Integer organId = null;
        DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        QueryResult result = drugsEnterpriseDAO.queryDrugsEnterpriseResultByStartAndLimit(name, createType, organId, start, limit);
        List<DrugsEnterpriseBean> list = getList(result.getItems(), DrugsEnterpriseBean.class);
        result.setItems(list);
        return result;
    }

    @RpcService
    public List<DrugsEnterpriseBean> findByOrganId(Integer organId) {
        DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        List<DrugsEnterprise> list = drugsEnterpriseDAO.findByOrganId(organId);
        return getList(list, DrugsEnterpriseBean.class);
    }

    @RpcService
    public DrugsEnterpriseDTO findByEnterpriseId(Integer id) {
        LOGGER.info("findByEnterpriseId id:{}", id);
        DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        DrugsEnterprise drugsEnterprise = drugsEnterpriseDAO.get(id);
        if (Objects.isNull(drugsEnterprise)) {
            throw new DAOException(ErrorCode.SERVICE_SUCCEED, "DrugsEnterprise is null");
        }
        DrugsEnterpriseDTO drugsEnterpriseDTO = new DrugsEnterpriseDTO();
        drugsEnterprise.setUserId("");
        drugsEnterprise.setPassword("");
        BeanUtils.copy(drugsEnterprise, drugsEnterpriseDTO);

        List<OrganLogisticsManageDto> organLogisticsManageDtos=iOrganLogisticsManageService.getRecipeLogisticsManageByOrganId(id,0);
        LOGGER.info("findByEnterpriseId organLogisticsManageDtos:{}", JSONUtils.toString(organLogisticsManageDtos));
        List<DrugEnterpriseLogistics> drugEnterpriseLogistics =new ArrayList<>();
        if(CollectionUtils.isNotEmpty(organLogisticsManageDtos)){
            organLogisticsManageDtos.forEach(organLogisticsManageDto -> {
                DrugEnterpriseLogistics drugEnterpriseLogistics1=new DrugEnterpriseLogistics();
                drugEnterpriseLogistics1.setCreateTime(organLogisticsManageDto.getCreateTime());
                drugEnterpriseLogistics1.setDrugsEnterpriseId(organLogisticsManageDto.getDrugsEnterpriseId());
                drugEnterpriseLogistics1.setId(organLogisticsManageDto.getOrganId());
                drugEnterpriseLogistics1.setIsDefault(organLogisticsManageDto.getIsDefault());
                drugEnterpriseLogistics1.setIsExpressDelivery(organLogisticsManageDto.getIsExpressDelivery());
                drugEnterpriseLogistics1.setConsignorCityCode(organLogisticsManageDto.getConsignorCityCode());
                drugEnterpriseLogistics1.setLogisticsCompany(organLogisticsManageDto.getLogisticsCode()==null?null:Integer.parseInt(organLogisticsManageDto.getLogisticsCode()));
                drugEnterpriseLogistics1.setLogisticsCompanyName(organLogisticsManageDto.getLogisticsCompanyName());
                drugEnterpriseLogistics1.setUpdateTime(organLogisticsManageDto.getUpdateTime());
                drugEnterpriseLogistics.add(drugEnterpriseLogistics1);
            });
        }
        drugsEnterpriseDTO.setDrugEnterpriseLogisticsList(drugEnterpriseLogistics);
        LOGGER.info("findByEnterpriseId drugsEnterpriseDTO:{}", JSONUtils.toString(drugsEnterpriseDTO));
        return drugsEnterpriseDTO;
    }

    /**
     * 用于钥世圈上传药品信息
     *
     * @param auditDrugListBean 药品信息
     * @return 结果信息
     */
    @RpcService
    public StandardResultDTO receiveDrugInfo(AuditDrugListBean auditDrugListBean) {
        LOGGER.info("钥世圈推送过来的药品信息：{}.", JSONUtils.toString(auditDrugListBean));
        StandardResultDTO result = new StandardResultDTO();
        result.setCode(StandardResultDTO.FAIL);
        SaleDrugListDAO saleDrugListDAO = DAOFactory.getDAO(SaleDrugListDAO.class);
        OrganDrugListDAO organDrugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);
        OrganService organService = BasicAPI.getService(OrganService.class);
        //校验入参
        validate(result, auditDrugListBean);
        AuditDrugList auditDrug = auditDrugListDAO.getByOrganizeCodeAndOrganDrugCode(auditDrugListBean.getOrganizeCode(), auditDrugListBean.getOrganDrugCode());
        if (auditDrug != null) {
            LOGGER.info("更新药品价格:[{}] [{}].", auditDrugListBean.getOrganDrugCode(), auditDrugListBean.getPrice());
            //说明已经上传过该药品,为更新操作
            try {
                auditDrug.setPrice(auditDrugListBean.getPrice());
                auditDrugListDAO.update(auditDrug);
                //更新机构药品目录和配送药品目录的价格
                OrganDrugList organDrugList = organDrugListDAO.get(auditDrug.getOrganDrugListId());
                organDrugList.setSalePrice(BigDecimal.valueOf(auditDrugListBean.getPrice()));
                organDrugListDAO.update(organDrugList);
                SaleDrugList saleDrugList = saleDrugListDAO.get(auditDrug.getSaleDrugListId());
                saleDrugList.setPrice(BigDecimal.valueOf(auditDrugListBean.getPrice()));
                saleDrugListDAO.update(saleDrugList);
                result.setCode(StandardResultDTO.SUCCESS);
                result.setMsg("更新药品信息成功");
            } catch (Exception e) {
                LOGGER.error("更新药品信息失败,{} {}.", auditDrugListBean.getOrganDrugCode(), e.getMessage(), e);
            }
            return result;
        }
        //包装药品数据
        AuditDrugList auditDrugList = packageAuditDrugList(auditDrugListBean);
        //首先保存到auditDrugList
        AuditDrugList resultAudit = auditDrugListDAO.save(auditDrugList);
        //首先查找salaDrugList是否存在该药品
        OrganDTO organ = organService.getOrganByOrganizeCode(auditDrugListBean.getOrganizeCode());
        if (organ == null) {
            LOGGER.warn("机构不存在,机构编号：{}.", auditDrugListBean.getOrganizeCode());
            result.setMsg("该机构在平台不存在");
            return result;
        }
        List<String> drugCodes = new ArrayList<>();
        drugCodes.add(auditDrugListBean.getOrganDrugCode());
        List<SaleDrugList> saleDrugLists = saleDrugListDAO.findByOrganIdAndDrugCodes(organ.getOrganId(), drugCodes);
        if (saleDrugLists != null && saleDrugLists.size() > 0) {
            //说明该药品存在于配送药品目录,校验是否存在于机构药品目录
            List<OrganDrugList> organDrugLists = organDrugListDAO.findByOrganIdAndDrugCodes(organ.getOrganId(), drugCodes);
            if (organDrugLists != null && organDrugLists.size() > 0) {
                //更新临时表标志
                resultAudit.setStatus(1);
                resultAudit.setType(1);
                auditDrugListDAO.update(resultAudit);
            }
        } else {
            //说明配送药品目录中不存在,需要进行平台匹配维护医院审核
            LOGGER.info("钥世圈推送药品在配送目录中不存在,organizeCode:{},organDrugCode:{}", auditDrugListBean.getOrganizeCode(), auditDrugListBean.getOrganDrugCode());
        }
        return null;
    }

    //包装药品对象
    private static AuditDrugList packageAuditDrugList(AuditDrugListBean auditDrugListBean) {
        OrganService organService = BasicAPI.getService(OrganService.class);
        OrganDTO organ = organService.getOrganByOrganizeCode(auditDrugListBean.getOrganizeCode());
        AuditDrugList auditDrugList = new AuditDrugList();
        auditDrugList.setOrganDrugCode(auditDrugListBean.getOrganDrugCode());
        auditDrugList.setOrganizeCode(auditDrugListBean.getOrganizeCode());
        auditDrugList.setOrganId(organ.getOrganId());
        auditDrugList.setDrugName(auditDrugListBean.getDrugName());
        auditDrugList.setSaleName(auditDrugListBean.getSaleName());
        auditDrugList.setCreateDt(auditDrugListBean.getCreateDt());
        auditDrugList.setApprovalNumber(StringUtils.isEmpty(auditDrugListBean.getApprovalNumber()) ? "" : auditDrugListBean.getApprovalNumber());
        auditDrugList.setPack(auditDrugListBean.getPack() == null ? 0 : auditDrugListBean.getPack());
        auditDrugList.setDrugForm(auditDrugListBean.getDrugForm());
        auditDrugList.setDrugType(auditDrugListBean.getDrugType());
        auditDrugList.setProducer(auditDrugListBean.getProducer());
        auditDrugList.setPrice(auditDrugListBean.getPrice());
        auditDrugList.setStatus(0);
        auditDrugList.setUnit(auditDrugListBean.getUnit());
        auditDrugList.setUseDose(auditDrugListBean.getUseDose());
        auditDrugList.setUseDoseUnit(auditDrugListBean.getUseDoseUnit());
        auditDrugList.setUsePathways(auditDrugListBean.getUsePathways());
        auditDrugList.setUsingRate(auditDrugListBean.getUsingRate());
        auditDrugList.setSourceOrgan(auditDrugListBean.getSourceOrgan());
        auditDrugList.setType(0);
        auditDrugList.setDrugSpec(auditDrugListBean.getDrugSpec());
        auditDrugList.setSourceEnterprise(StringUtils.isEmpty(auditDrugListBean.getSourceEnterprise()) ? "钥世圈" : auditDrugListBean.getSourceEnterprise());
        return auditDrugList;
    }

    private static StandardResultDTO validate(StandardResultDTO result, AuditDrugListBean auditDrugListBean) {
        //校验数据参数
        if (auditDrugListBean == null) {
            result.setMsg("入参不能为空");
            return result;
        }
        if (StringUtils.isEmpty(auditDrugListBean.getOrganizeCode())) {
            result.setMsg("医院编码不能为空");
            return result;
        }
        if (StringUtils.isEmpty(auditDrugListBean.getOrganDrugCode())) {
            result.setMsg("医院药品编码不能为空");
            return result;
        }
        if (StringUtils.isEmpty(auditDrugListBean.getDrugName())) {
            result.setMsg("药品名称不能为空");
            return result;
        }
        if (StringUtils.isEmpty(auditDrugListBean.getSaleName())) {
            result.setMsg("商品名称不能为空");
            return result;
        }
        if (StringUtils.isEmpty(auditDrugListBean.getProducer())) {
            result.setMsg("生产厂家不能为空");
            return result;
        }
        if (StringUtils.isEmpty(auditDrugListBean.getUnit())) {
            result.setMsg("药品单位不能为空");
            return result;
        }
        if (auditDrugListBean.getCreateDt() == null) {
            result.setMsg("上传时间不能为空");
            return result;
        }
        if (StringUtils.isEmpty(auditDrugListBean.getDrugSpec())) {
            result.setMsg("药品规格不能为空");
            return result;
        }
        result.setCode(StandardResultDTO.SUCCESS);
        return result;
    }

    /**
     * 上下架药品
     *
     * @param upDownDrugBean
     * @return
     */
    @RpcService
    public StandardResultDTO upDownDrug(UpDownDrugBean upDownDrugBean) {
        StandardResultDTO result = new StandardResultDTO();
        result.setCode(StandardResultDTO.FAIL);
        LOGGER.info("上架或下架药品-upDownDrugBean info:{}.", JSONUtils.toString(upDownDrugBean));
        if (upDownDrugBean == null) {
            result.setMsg("药品信息不能为空");
            return result;
        }
        OrganService organService = BasicAPI.getService(OrganService.class);
        OrganDTO organ = organService.getOrganByOrganizeCode(upDownDrugBean.getOrganizeCode());
        if (organ == null) {
            result.setMsg("机构不存在");
            return result;
        }
        OrganDrugListDAO organDrugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);
        Boolean succ = organDrugListDAO.updateOrganDrugListByOrganIdAndOrganDrugCode(organ.getOrganId(), upDownDrugBean.getOrganDrugCode(), ImmutableMap.of("status", upDownDrugBean.getStatus()));
        if (succ) {
            result.setCode(StandardResultDTO.SUCCESS);
            result.setMsg("success");
            return result;
        }
        return result;
    }

    @RpcService
    public StandardResultDTO recipeDownloadConfirmation(String appKey, List<Integer> recipeIds) {
        StandardResultDTO result = new StandardResultDTO();
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        LOGGER.info("ThirdEnterpriseCallService.recipeDownloadConfirmation appKey:{}, recipeIds:{}", appKey, JSONUtils.toString(recipeIds));
        result.setCode(StandardResultDTO.SUCCESS);
        List<DrugsEnterprise> drugsEnterprises = drugsEnterpriseDAO.findByAppKey(appKey);
        if (drugsEnterprises == null) {
            result.setCode(StandardResultDTO.FAIL);
            result.setMsg("未匹配到药企");
            return result;
        }
        for (DrugsEnterprise drugsEnterprise : drugsEnterprises) {
            for (Integer recipeId : recipeIds) {
                Recipe recipe = recipeDAO.getByRecipeIdAndEnterpriseId(drugsEnterprise.getId(), recipeId);
                if (recipe != null) {
                    recipeDAO.updateRecipeByDepIdAndRecipes(drugsEnterprise.getId(), Arrays.asList(recipeId));
                    RecipeLogService.saveRecipeLog(recipe.getRecipeId(), recipe.getStatus(), recipe.getStatus(), drugsEnterprise.getName() + "获取处方成功");
                }
            }
        }
        return result;
    }

    /**
     * @param paramMap
     * @return
     */
    @RpcService
    public Integer scanStockEnterpriseForHis(Map<String, Object> paramMap) {
        LOGGER.info("scanStockEnterpriseForHis:{}.", JSONUtils.toString(paramMap));
        Integer organId = (Integer) paramMap.get("organId");
        String enterpriseCode = (String) paramMap.get("enterpriseCode");
        DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        OrganDrugListDAO organDrugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);
        DrugsEnterprise drugsEnterprise = drugsEnterpriseDAO.getByAppKey(enterpriseCode);
        if (drugsEnterprise == null) {
            LOGGER.info("scanStockEnterpriseForHis 没有查询到对应的药企");
            return 0;
        }
        List data = (List) paramMap.get("data");
        if (data != null) {
            for (int i = 0; i < data.size(); i++) {
                Map map = (Map) data.get(i);
                String drugCode = (String) map.get("drugCode");
                String total = (String) map.get("total");
                if (StringUtils.isEmpty(drugCode) || StringUtils.isEmpty(total)) {
                    return 0;
                }
                OrganDrugList organDrugList = null;
                try {
                    //TODO 2 drugCode含义
                    organDrugList = organDrugListDAO.getByOrganIdAndProducerCode(organId, drugCode);
                } catch (Exception e) {
                    LOGGER.error("scanStockEnterpriseForHis 查询机构药品错误 drugCode:{}.", drugCode, e);
                    return 0;
                }
                return execScanStockEnterpriseForMaLu(organDrugList, drugsEnterprise, total);
            }
        }
        return 0;
    }

    /**
     * 马陆扣库存操作
     *
     * @param organDrugList
     * @param drugsEnterprise
     * @param total
     * @return
     */
    private Integer execScanStockEnterpriseForMaLu(OrganDrugList organDrugList, DrugsEnterprise drugsEnterprise, String total) {
        SaleDrugListDAO saleDrugListDAO = DAOFactory.getDAO(SaleDrugListDAO.class);
        Integer result = 1;
        if (organDrugList != null) {
            SaleDrugList saleDrugList = saleDrugListDAO.getByDrugIdAndOrganId(organDrugList.getDrugId(), drugsEnterprise.getId());
            if (saleDrugList != null) {
                if (saleDrugList.getInventory() != null) {
                    if (saleDrugList.getInventory().doubleValue() < Double.parseDouble(total)) {
                        result = 0;
                    } else {
                        try {
                            saleDrugListDAO.updateInventoryByOrganIdAndDrugId(drugsEnterprise.getId(), saleDrugList.getDrugId(), new BigDecimal(total));
                        } catch (Exception e) {
                            LOGGER.error("scanStockEnterpriseForHis 扣库存失败,msg:{}.", e.getMessage(), e);
                        }
                    }
                } else {
                    return 0;
                }
            } else {
                return 0;
            }
        } else {
            return 0;
        }
        return result;
    }

    /**
     * 下载处方
     * 返回了收货人手机号，收货人地址，第三方要用到，不需要做脱敏处理
     *
     * @param parames
     * @return
     */
    @RpcService(timeout = 60)
    public StandardResultDTO downLoadRecipes(Map<String, Object> parames) {
        StandardResultDTO standardResult = new StandardResultDTO();
        standardResult.setCode(StandardResultDTO.SUCCESS);
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        RecipeOrderDAO recipeOrderDAO = DAOFactory.getDAO(RecipeOrderDAO.class);
        DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        RecipeExtendDAO recipeExtendDAO = DAOFactory.getDAO(RecipeExtendDAO.class);
        RecipeDetailDAO recipeDetailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
        SaleDrugListDAO saleDrugListDAO = DAOFactory.getDAO(SaleDrugListDAO.class);
        String imgHead = "data:image/jpeg;base64,";
        LOGGER.info("ThirdEnterpriseCallService.downLoadRecipes parames:{}.", JSONUtils.toString(parames));
        if (parames == null) {
            standardResult.setCode(StandardResultDTO.FAIL);
            standardResult.setMsg("参数不能为空");
            return standardResult;
        }
        String appKey = (String) parames.get("appKey");
        String lastUpdateTime = (String) parames.get("lastUpdateTime");
        List<DrugsEnterprise> drugsEnterprises = drugsEnterpriseDAO.findByAppKey(appKey);
        LOGGER.info("ThirdEnterpriseCallService.downLoadRecipes drugsEnterprise:{}.", JSONUtils.toString(drugsEnterprises));
        if (CollectionUtils.isEmpty(drugsEnterprises)) {
            standardResult.setCode(StandardResultDTO.FAIL);
            standardResult.setMsg("无法匹配到药企");
            return standardResult;
        }

        OrganService organService = BasicAPI.getService(OrganService.class);
        DoctorService doctorService = BasicAPI.getService(DoctorService.class);
        DepartmentService departmentService = BasicAPI.getService(DepartmentService.class);
        PatientService patientService = BasicAPI.getService(PatientService.class);

        List<Integer> drugsEnterpriseIds = new ArrayList<>();
        for (DrugsEnterprise drugsEnterprise : drugsEnterprises) {
            drugsEnterpriseIds.add(drugsEnterprise.getId());
        }
        //查找指定药企已支付完成的处方单
        List<RecipeOrder> recipeOrders = new ArrayList<>();
        try {
            recipeOrders = recipeOrderDAO.findRecipeOrderByDepIdAndPayTime(drugsEnterpriseIds, lastUpdateTime);
            LOGGER.info("ThirdEnterpriseCallService.downLoadRecipes recipeOrders:{}.", JSONUtils.toString(recipeOrders));
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.error("ThirdEnterpriseCallService.downLoadRecipes recipeOrders:{} error : {}.", JSONUtils.toString(recipeOrders), e.getMessage(), e);
        }

        List<RecipeAndOrderDetailBeanNoDs> result = new ArrayList<>();
        for (RecipeOrder recipeOrder : recipeOrders) {
            RecipeAndOrderDetailBeanNoDs orderDetailBean = new RecipeAndOrderDetailBeanNoDs();
            String orderCode = recipeOrder.getOrderCode();
            List<Recipe> recipes = recipeDAO.findRecipeListByOrderCode(orderCode);
            LOGGER.info("ThirdEnterpriseCallService.downLoadRecipes recipes:{} .", JSONUtils.toString(recipes));
            Recipe recipe = recipes.get(0);
            RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipe.getRecipeId());
            if (!new Integer(1).equals(recipeOrder.getOrderType()) && BigDecimal.ZERO.compareTo(recipeOrder.getCouponFee()) == 0
                    && new Integer(1).equals(recipeOrder.getPayMode())) {
                //表示不是医保患者并且没有优惠券并且是线上支付的,那他一定要支付钱
                if (StringUtils.isEmpty(recipeOrder.getOutTradeNo())) {
                    continue;
                }
            }
            EmrRecipeManager.getMedicalInfo(recipe, recipeExtend);
            //设置医院信息
            OrganDTO organ = organService.getByOrganId(recipe.getClinicOrgan());
            LOGGER.info("ThirdEnterpriseCallService.downLoadRecipes organ:{} .", JSONUtils.toString(organ));
            if (null != organ) {
                orderDetailBean.setClinicOrgan(convertParame(organ.getOrganId().toString()));
                orderDetailBean.setOrganId(organ.getOrganizeCode());
                orderDetailBean.setOrganName(organ.getName());
            }
            //设置医生信息
            DoctorDTO doctorDTO = doctorService.getByDoctorId(recipe.getDoctor());
            LOGGER.info("ThirdEnterpriseCallService.downLoadRecipes doctorDTO:{} .", JSONUtils.toString(doctorDTO));
            if (null != doctorDTO) {
                orderDetailBean.setDoctorNumber(convertParame(doctorDTO.getIdNumber()));
                orderDetailBean.setDoctorName(convertParame(doctorDTO.getName()));
            }

            //设置科室信息
            DepartmentDTO department = departmentService.get(recipe.getDepart());
            LOGGER.info("ThirdEnterpriseCallService.downLoadRecipes department:{} .", JSONUtils.toString(department));
            if (null != department) {
                orderDetailBean.setDepartId(convertParame(department.getDeptId()));
                orderDetailBean.setDepartName(convertParame(department.getName()));
            }

            //设置患者信息
            PatientDTO patient = patientService.get(recipe.getMpiid());
            LOGGER.info("ThirdEnterpriseCallService.downLoadRecipes patient:{} .", JSONUtils.toString(patient));
            if (null != patient) {
                orderDetailBean.setCertificateType("1");
                orderDetailBean.setCertificate(convertParame(patient.getCertificate()));
                orderDetailBean.setPatientName(convertParame(patient.getPatientName()));
                orderDetailBean.setPatientTel(convertParame(patient.getMobile()));
                orderDetailBean.setPatientAddress(convertParame(patient.getFullHomeArea()));
                orderDetailBean.setBirthday(patient.getBirthday());
                orderDetailBean.setSexCode("1".equals(patient.getPatientSex())?"M":"F");
                orderDetailBean.setGender(patient.getPatientSex());
                try {
                    orderDetailBean.setSexName(DictionaryController.instance().get("eh.base.dictionary.Gender").getText(patient.getPatientSex()));
                } catch (ControllerException e) {
                    e.printStackTrace();
                }
            }
            orderDetailBean.setPatientNumber(convertParame(recipe.getPatientID()));

            //设置处方信息
            orderDetailBean.setRecipeId(convertParame(recipe.getRecipeId()));
            orderDetailBean.setRecipeType(convertParame(recipe.getRecipeType()));
            orderDetailBean.setRecipeCode(recipe.getRecipeCode());
            orderDetailBean.setCreateDate(convertParame(recipe.getCreateDate()));
            orderDetailBean.setOrganDiseaseId(convertParame(recipe.getOrganDiseaseId()));
            orderDetailBean.setOrganDiseaseName(convertParame(recipe.getOrganDiseaseName()));
            orderDetailBean.setRecipeMemo(convertParame(recipe.getRecipeMemo()));
            orderDetailBean.setPharmacyCode(convertParame(recipeOrder.getDrugStoreCode()));
            orderDetailBean.setPharmacyName(convertParame(recipeOrder.getDrugStoreName()));
            if (Objects.nonNull(recipe.getRecipeDrugForm())) {
                orderDetailBean.setRecipeDrugForm(recipe.getRecipeDrugForm());
            }
            if (recipe.getRecipeType() == 3 && recipe.getCopyNum() != null) {
                orderDetailBean.setTcmNum(convertParame(recipe.getCopyNum()));
                orderDetailBean.setDecoctionId(convertParame(recipeExtend.getDecoctionId()));
                orderDetailBean.setDecoctionText(convertParame(recipeExtend.getDecoctionText()));
            } else {
                orderDetailBean.setTcmNum("");
            }
            if (Objects.nonNull(recipe.getDecoctionNum())) {
                orderDetailBean.setDecoctionNum(recipe.getDecoctionNum());
            }
            if (Integer.valueOf(1).equals(recipeOrder.getPayMode())) {
                orderDetailBean.setDistributionFlag("1");
            } else {
                orderDetailBean.setDistributionFlag("0");
            }

            if (RecipeSourceTypeEnum.OFFLINE_RECIPE.getType().equals(recipe.getRecipeSourceType())) {
                HisRecipe hisRecipe = hisRecipeDAO.getHisRecipeByRecipeCodeAndClinicOrgan(recipe.getClinicOrgan(), recipe.getRecipeCode());
                if (Objects.nonNull(hisRecipe) && Objects.nonNull(hisRecipe.getSendType())) {
                    orderDetailBean.setSendType(hisRecipe.getSendType());
                }
            }

            DrugsEnterprise drugsEnterprise = drugsEnterpriseDAO.getById(recipeOrder.getEnterpriseId());
            if (drugsEnterprise.getDownSignImgType() != null && drugsEnterprise.getDownSignImgType() == 1) {
                //获取处方签链接
                String signImgFile = recipeParameterDao.getByName("fileImgUrl");
                if (StringUtils.isNotEmpty(recipe.getChemistSignFile())) {
                    orderDetailBean.setRecipeSignImgUrl(signImgFile + recipe.getChemistSignFile());
                } else {
                    orderDetailBean.setRecipeSignImgUrl(signImgFile + recipe.getSignFile());
                }
            } else {
                //设置处方笺base
                String ossId = recipe.getSignImg();
                if (null != ossId) {
                    try {
                        IFileDownloadService fileDownloadService = ApplicationUtils.getBaseService(IFileDownloadService.class);
                        String imgStr = imgHead + fileDownloadService.downloadImg(ossId);
                        if (org.springframework.util.ObjectUtils.isEmpty(imgStr)) {
                            LOGGER.warn("ThirdEnterpriseCallService.downLoadRecipes:处方ID为{}的ossid为{}处方笺不存在", recipe.getRecipeId(), ossId);
                        }
                        LOGGER.warn("ThirdEnterpriseCallService.downLoadRecipes:{}处方，下载处方笺服务成功", recipe.getRecipeId());
                        orderDetailBean.setRecipeSignImg(imgStr);
                    } catch (Exception e) {
                        e.printStackTrace();
                        LOGGER.error("ThirdEnterpriseCallService.downLoadRecipes:{}处方，下载处方笺服务异常：{}.", recipe.getRecipeId(), e.getMessage(), e);
                    }
                }
            }

            //设置订单信息
            orderDetailBean.setRecipeFee(convertParame(recipeOrder.getRecipeFee()));
            orderDetailBean.setActualFee(convertParame(recipeOrder.getActualPrice()));
            orderDetailBean.setCouponFee(convertParame(recipeOrder.getCouponFee()));
            orderDetailBean.setDecoctionFee(convertParame(recipeOrder.getDecoctionFee()));
            orderDetailBean.setAuditFee(convertParame(recipeOrder.getAuditFee()));
            orderDetailBean.setRegisterFee(convertParame(recipeOrder.getRegisterFee()));
            orderDetailBean.setCommunityCode(convertParame(recipeOrder));
            orderDetailBean.setTrackingNumber(convertParame(recipeOrder.getTrackingNumber()));
            if (ValidateUtil.notNullAndZeroInteger(recipeOrder.getLogisticsCompany())) {
                orderDetailBean.setLogisticsCompany(recipeOrder.getLogisticsCompany());
                String company = DictionaryUtil.getDictionary("eh.infra.dictionary.LogisticsCode", recipeOrder.getLogisticsCompany());
                orderDetailBean.setLogisticsCompanyName(company);
            }
            //代煎费
            orderDetailBean.setDecoctionFee(convertParame(recipeOrder.getDecoctionFee()));
            //设置中医辨证论治费
            orderDetailBean.setTcmFee(convertParame(recipeOrder.getTcmFee()));

            if (recipe.getRecipeType() == 3 && recipeOrder.getDecoctionFee() != null && recipeOrder.getDecoctionFee().compareTo(BigDecimal.ZERO) == 1) {
                orderDetailBean.setDecoctionFlag("1");
            } else {
                orderDetailBean.setDecoctionFlag("0");
            }
            if (recipeOrder.getFundAmount() != null) {
                orderDetailBean.setMedicalFee(convertParame(recipeOrder.getFundAmount()));
            } else {
                orderDetailBean.setMedicalFee("0");
            }
            orderDetailBean.setOrderTotalFee(convertParame(recipeOrder.getTotalFee()));
            if (recipeOrder.getExpressFee() == null) {
                orderDetailBean.setExpressFee("0");
            } else {
                orderDetailBean.setExpressFee(convertParame(recipeOrder.getExpressFee()));
            }
            if (GiveModeEnum.GIVE_MODE_HOME_DELIVERY.getType().equals(recipe.getGiveMode())) {
                orderDetailBean.setDistributionFlag("1");
            } else {
                orderDetailBean.setDistributionFlag("0");
            }
            if (RecipeOrderTypeEnum.PROVINCIAL_MEDICAL.getType().equals(recipeOrder.getOrderType())) {
                orderDetailBean.setMedicalPayFlag("1");
            } else {
                orderDetailBean.setMedicalPayFlag("0");
            }

            if (null != recipeOrder.getInvoiceRecordId()) {
                InvoiceRecordDto invoiceRecordDto = invoiceRecordService.findInvoiceRecordInfo(recipeOrder.getInvoiceRecordId());
                InvoiceRecordVO invoiceRecordVO = new InvoiceRecordVO();
                ObjectCopyUtils.copyProperties(invoiceRecordVO, invoiceRecordDto);
                orderDetailBean.setInvoiceRecord(invoiceRecordVO);
            }
            orderDetailBean.setExpressFee(convertParame(recipeOrder.getExpressFee()));
            String province = LocalStringUtil.getAddressDic(recipeOrder.getAddress1());
            String city = LocalStringUtil.getAddressDic(recipeOrder.getAddress2());
            String district = LocalStringUtil.getAddressDic(recipeOrder.getAddress3());
            String street = LocalStringUtil.getAddressDic(recipeOrder.getStreetAddress());
            orderDetailBean.setProvince(convertParame(province));
            orderDetailBean.setCity(convertParame(city));
            orderDetailBean.setDistrict(convertParame(district));
            orderDetailBean.setStreet(convertParame(street));
            orderDetailBean.setCommunityCode(convertParame(recipeOrder.getAddress5()));
            orderDetailBean.setCommunityName(convertParame(recipeOrder.getAddress5Text()));
            //设置省市区街道编码
            if (StringUtils.isNotEmpty(recipeOrder.getAddress1())) {
                orderDetailBean.setProvinceCode(convertParame(recipeOrder.getAddress1()) + "0000");
                orderDetailBean.setCityCode(convertParame(recipeOrder.getAddress2()) + "00");
            }
            orderDetailBean.setDistrictCode(convertParame(recipeOrder.getAddress3()));
            orderDetailBean.setStreetCode(convertParame(recipeOrder.getStreetAddress()));
            orderDetailBean.setReceiver(convertParame(recipeOrder.getReceiver()));
            orderDetailBean.setRecMobile(convertParame(recipeOrder.getRecMobile()));
            orderDetailBean.setRecAddress(convertParame(recipeOrder.getAddress4()));
            orderDetailBean.setOutTradeNo(convertParame(recipeOrder.getOutTradeNo()));
            orderDetailBean.setTradeNo(convertParame(recipeOrder.getTradeNo()));
            orderDetailBean.setPayMode(convertParame(convertParame(recipeOrder.getPayMode())));
            orderDetailBean.setPayFlag(convertParame(recipeOrder.getPayFlag()));
            orderDetailBean.setGiveMode(convertParame(recipe.getGiveMode()));
            orderDetailBean.setMedicalPayFlag(convertParame(recipeOrder.getOrderType()));
            orderDetailBean.setMemo(convertParame(recipe.getMemo()));
            orderDetailBean.setStatus(convertParame(recipe.getStatus()));
            orderDetailBean.setPharmNo(convertParame(recipeExtend.getPharmNo()));
            orderDetailBean.setCompanyCode(convertParame(recipeOrder.getCompanyCode()));
            orderDetailBean.setCompanyName(convertParame(recipeOrder.getCompanyName()));
            List<DrugListForThreeBean> drugLists = new ArrayList<>();
            //设置药品信息
            List<Recipedetail> recipeDetails = recipeDetailDAO.findByRecipeId(recipe.getRecipeId());
            LOGGER.info("ThirdEnterpriseCallService.downLoadRecipes recipeDetails:{} .", JSONUtils.toString(recipeDetails));
            for (Recipedetail recipeDetail : recipeDetails) {
                DrugListForThreeBean drugList = new DrugListForThreeBean();
                SaleDrugList saleDrugList = saleDrugListDAO.getByDrugIdAndOrganId(recipeDetail.getDrugId(), drugsEnterprise.getId());
                if (null != saleDrugList) {
                    drugList.setDrugCode(saleDrugList.getOrganDrugCode());
                }
                drugList.setDrugName(recipeDetail.getDrugName());
                drugList.setSpecification(convertParame(recipeDetail.getDrugSpec()));
                drugList.setProducer(convertParame(recipeDetail.getProducer()));
                drugList.setTotal(convertParame(recipeDetail.getUseTotalDose()));
                drugList.setUseDose(convertParame(recipeDetail.getUseDose()));
                drugList.setDrugFee(convertParame(recipeDetail.getActualSalePrice()));
                drugList.setUesDays(convertParame(recipeDetail.getUseDays()));
                drugList.setUsingRate(convertParame(recipeDetail.getUsingRate()));
                drugList.setUsePathways(convertParame(recipeDetail.getUsePathways()));
                drugList.setDrugForm(convertParame(recipeDetail.getDrugForm()));
                if (recipe.getRecipeType() == 3 || recipe.getRecipeType() == 4) {
                    orderDetailBean.setTcmUsePathways(convertParame(recipeDetail.getUsePathwaysTextFromHis()));
                    orderDetailBean.setTcmUsingRate(convertParame(recipeDetail.getUsingRateTextFromHis()));
                } else {
                    orderDetailBean.setTcmUsePathways("");
                    orderDetailBean.setTcmUsingRate("");
                }
                drugList.setDrugUnit(convertParame(recipeDetail.getDrugUnit()));
                drugList.setPack(convertParame(recipeDetail.getPack()));
                drugList.setLicenseNumber(convertParame(recipeDetail.getLicenseNumber()));
                drugList.setStandardCode("");
                drugList.setSaleUnit(convertParame(recipeDetail.getSaleUnit()));
                drugList.setSaleUseDose(convertParame(recipeDetail.getSaleUseDose()));
                if (null != recipeDetail.getActualSalePrice()) {
                    drugList.setDrugTotalFee(convertParame(recipeDetail.getActualSalePrice().multiply(new BigDecimal(recipeDetail.getUseTotalDose()))));
                }
                try {
                    String usingRate = recipeDetail.getUsingRateTextFromHis() != null ? recipeDetail.getUsingRateTextFromHis() : DictionaryController.instance().get("eh.cdr.dictionary.UsingRate").getText(recipeDetail.getUsingRate());
                    String usingPathways = recipeDetail.getUsePathwaysTextFromHis() != null ? recipeDetail.getUsePathwaysTextFromHis() : DictionaryController.instance().get("eh.cdr.dictionary.UsePathways").getText(recipeDetail.getUsePathways());
                    drugList.setUsingRateText(usingRate);
                    drugList.setUsePathwaysText(usingPathways);
                } catch (ControllerException e) {
                    LOGGER.warn("ThirdEnterpriseCallService.downLoadRecipes:处方细节ID为{}.", recipeDetail.getRecipeDetailId(), e);
                }
                drugList.setMemo(convertParame(recipeDetail.getMemo()));
                drugList.setUseDoseUnit(convertParame(recipeDetail.getUseDoseUnit()));
                drugLists.add(drugList);
            }
            orderDetailBean.setDrugList(drugLists);
            result.add(orderDetailBean);
        }
        LOGGER.info("ThirdEnterpriseCallService.downLoadRecipes result:{}.", JSONUtils.toString(result));
        standardResult.setData(result);
        return standardResult;
    }

    @RpcService
    public StandardResultDTO synchronizeInventory(Map<String, Object> parames) {
        LOGGER.info("ThirdEnterpriseCallService synchronizeInventory parames:{}", JSONUtils.toString(parames));
        DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        SaleDrugListDAO saleDrugListDAO = DAOFactory.getDAO(SaleDrugListDAO.class);
        StandardResultDTO standardResult = new StandardResultDTO();
        String appKey = (String) parames.get("appKey");
        List<Map<String, Object>> synchronizeDrugBeans = (List) parames.get("drugList");
        DrugsEnterprise drugsEnterprise = drugsEnterpriseDAO.getByAppKey(appKey);
        if (drugsEnterprise == null) {
            standardResult.setCode(StandardResultDTO.FAIL);
            standardResult.setMsg("未匹配到药企");
            return standardResult;
        }
        standardResult.setCode(StandardResultDTO.SUCCESS);
        for (Map<String, Object> synchronizeDrugBean : synchronizeDrugBeans) {
            String drugCode = (String) synchronizeDrugBean.get("drugCode");
            int inventory = (Integer) synchronizeDrugBean.get("inventory");
            try {
                SaleDrugList saleDrugList = saleDrugListDAO.getByOrganIdAndDrugCode(drugsEnterprise.getId(), drugCode);
                if (saleDrugList != null) {
                    saleDrugListDAO.updateDrugInventory(saleDrugList.getDrugId(), drugsEnterprise.getId(), new BigDecimal(inventory));
                } else {
                    LOGGER.info("ThirdEnterpriseCallService synchronizeInventory 未查询到配送药品：{},{}", drugsEnterprise.getName(), drugCode);
                }
            } catch (Exception e) {
                LOGGER.error("ThirdEnterpriseCallService synchronizeInventory error:", e);
            }
        }
        return standardResult;
    }

    private String convertParame(Object o) {
        if (o == null) {
            return "";
        } else {
            return o.toString();
        }
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

    public void getAddressDic(StringBuilder address, String area) {
        if (StringUtils.isNotEmpty(area)) {
            try {
                address.append(DictionaryController.instance().get("eh.base.dictionary.AddrArea").getText(area));
            } catch (ControllerException e) {
                LOGGER.error("getAddressDic 获取地址数据类型失败*****area:" + area, e);
            }
        }
    }

    /**
     * 1.2	同步药企药品库存接口返回给his并异步更新销售药品目录药品价格
     *
     * @param paramMap
     * @return
     */
    @RpcService
    public List<Map<String, Object>> finEnterpriseStockByOrganIdForHisAndUpdateSale(Map<String, Object> paramMap) {
        String organId = (String) paramMap.get("organ");
        Integer start = (Integer) paramMap.get("start");
        Integer limit = (Integer) paramMap.get("limit");
        if (StringUtils.isEmpty(organId)) {
            throw new DAOException(DAOException.VALUE_NEEDED, "organId is needed");
        }
        if (start == null) {
            throw new DAOException(DAOException.VALUE_NEEDED, "start is needed");
        }
        if (limit == null) {
            throw new DAOException(DAOException.VALUE_NEEDED, "limit is needed");
        }
        if (limit > 100) {
            limit = 100;
        }
        //1 同步药企药品库存给HIS
        RemoteDrugEnterpriseService remoteDrugEnterpriseService = ApplicationUtils.getRecipeService(RemoteDrugEnterpriseService.class);
        List<Map<String, Object>> res = remoteDrugEnterpriseService.findEnterpriseStockByPage(organId, start, limit);
        //2 异步更新零售价格
        GlobalEventExecFactory.instance().getExecutor().execute(() -> {
            updateSaleDrugList(res, organId);
        });
        return res;
    }

    /**
     * 更新零售价格
     *
     * @param res
     * @param organ
     */
    private void updateSaleDrugList(List<Map<String, Object>> res, String organ) {
        SaleDrugListDAO saleDrugListDAO = DAOFactory.getDAO(SaleDrugListDAO.class);
        if (res != null) {
            Integer organId = Integer.parseInt(organ);
            for (Map<String, Object> map : res) {
                SaleDrugList saleDrugList = new SaleDrugList();
                saleDrugList = saleDrugListDAO.getByOrganIdAndDrugCode(organId, map.get("PROC_ID").toString());
                if (saleDrugList != null) {
                    saleDrugList.setPrice(new BigDecimal((String) map.get("RETAIL_PRICE")));
                    saleDrugListDAO.update(saleDrugList);
                }
            }
        }

    }
}
