package recipe.drugsenterprise;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.ngari.recipe.entity.DrugsEnterprise;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeOrder;
import com.ngari.recipe.entity.Recipedetail;
import ctd.controller.exception.ControllerException;
import ctd.dictionary.DictionaryController;
import ctd.persistence.DAOFactory;
import ctd.persistence.bean.QueryResult;
import ctd.persistence.exception.DAOException;
import ctd.util.BeanUtils;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.bean.RecipeResultBean;
import recipe.bean.ThirdResultBean;
import recipe.constant.ErrorCode;
import recipe.constant.OrderStatusConstant;
import recipe.constant.RecipeBussConstant;
import recipe.constant.RecipeStatusConstant;
import recipe.dao.*;
import recipe.service.*;
import recipe.third.IWXServiceInterface;
import recipe.util.ApplicationUtils;
import recipe.util.DateConversion;
import recipe.util.MapValueUtil;

import java.math.BigDecimal;
import java.util.*;

/**
 * 第三方药企调用接口,历史原因存在一些平台的接口
 * company: ngarihealth
 * @author: 0184/yu_yun
 * @date:2017/4/20.
 */
@RpcBean("takeDrugService")
public class ThirdEnterpriseCallService {

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
     * 待配送状态
     *
     * @param paramMap
     */
    @RpcService
    public ThirdResultBean readyToSend(Map<String, Object> paramMap) {
        LOGGER.info("readyToSend param : " + JSONUtils.toString(paramMap));

        ThirdResultBean backMsg = ThirdResultBean.getFail();
        int code = validateRecipe(paramMap, backMsg, RecipeStatusConstant.CHECK_PASS_YS, RecipeStatusConstant.WAIT_SEND);

        if (REQUEST_ERROR_REAPET == code) {
            backMsg.setCode(REQUEST_OK);
            return backMsg;
        } else if (REQUEST_ERROR == code) {
            LOGGER.error("recipeId=[{}], readyToSend:{}", backMsg.getBusId(), JSONUtils.toString(backMsg));
            return backMsg;
        }

        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);

        Recipe recipe = backMsg.getRecipe();
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

        if (rs) {
            //记录日志
            RecipeLogService.saveRecipeLog(recipeId, RecipeStatusConstant.CHECK_PASS_YS, RecipeStatusConstant.WAIT_SEND, "待配送,配送人：" + sender);
        } else {
            code = ErrorCode.SERVICE_ERROR;
            errorMsg = "电子处方更新失败";
        }

        Object listObj = paramMap.get("dtl");
        boolean detailRs = false;
        if (rs) {
            if (null != listObj) {
                if (listObj instanceof List) {
                    List<HashMap<String, Object>> detailList = (List<HashMap<String, Object>>) paramMap.get("dtl");
                    if (CollectionUtils.isNotEmpty(detailList)) {
                        RecipeDetailDAO recipeDetailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);

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
        int code = validateRecipe(paramMap, backMsg, RecipeStatusConstant.WAIT_SEND, RecipeStatusConstant.IN_SEND);

        if (REQUEST_ERROR_REAPET == code) {
            backMsg.setCode(REQUEST_OK);
            return backMsg;
        } else if (REQUEST_ERROR == code) {
            LOGGER.error("recipeId=[{}], toSend:{}", backMsg.getBusId(), JSONUtils.toString(backMsg));
            return backMsg;
        }

        sendImpl(backMsg, paramMap);
        LOGGER.info("toSend:" + JSONUtils.toString(backMsg));

        return backMsg;
    }

    /**
     * 处方准备并配送接口 结合readyToSend 和toSend
     * 钥世圈使用
     * @param list
     * @return
     */
    @RpcService
    public List<ThirdResultBean> send(List<Map<String, Object>> list) {
        LOGGER.info("send param : " + JSONUtils.toString(list));

        List<ThirdResultBean> result = new ArrayList<>();
        for(Map<String, Object> paramMap : list){
            ThirdResultBean thirdResultBean = ThirdResultBean.getFail();
            thirdResultBean.setRecipeCode(MapValueUtil.getString(paramMap, "recipeCode"));
            int code = validateRecipe(paramMap, thirdResultBean, RecipeStatusConstant.CHECK_PASS_YS, RecipeStatusConstant.IN_SEND);

            if (REQUEST_ERROR_REAPET == code) {
                thirdResultBean.setCode(REQUEST_OK);
            } else if (REQUEST_ERROR == code) {
                LOGGER.error("recipeId=[{}], send:{}", thirdResultBean.getBusId(), JSONUtils.toString(thirdResultBean));
            } else if (REQUEST_OK == code) {
                sendImpl(thirdResultBean, paramMap);
            }
            result.add(thirdResultBean);
        }

        return result;
    }

    /**
     * 配送功能实现
     * @param thirdResultBean
     * @param paramMap
     */
    private void sendImpl(ThirdResultBean thirdResultBean, Map<String, Object> paramMap){
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        RecipeOrderService orderService = ApplicationUtils.getRecipeService(RecipeOrderService.class);

        Recipe recipe = thirdResultBean.getRecipe();
        Integer recipeId = recipe.getRecipeId();
        String sendDateStr = MapValueUtil.getString(paramMap, "sendDate");
        Date sendDate = DateTime.now().toDate();
        if (StringUtils.isNotEmpty(sendDateStr)) {
            sendDate = DateConversion.parseDate(sendDateStr, DateConversion.DEFAULT_DATE_TIME);
        }
        //此处为配送人
        String sender = MapValueUtil.getString(paramMap, "sender");

        Map<String, Object> attrMap = Maps.newHashMap();
        attrMap.put("sendDate", sendDate);
        attrMap.put("sender", sender);
        //以免进行处方失效前提醒
        attrMap.put("remindFlag", 1);
        String recipeFeeStr = MapValueUtil.getString(paramMap, "recipeFee");
        if (StringUtils.isNotEmpty(recipeFeeStr)) {
            attrMap.put("totalMoney", new BigDecimal(recipeFeeStr));
        }
        //更新处方信息
        Boolean rs = recipeDAO.updateRecipeInfoByRecipeId(recipeId, RecipeStatusConstant.IN_SEND, attrMap);

        if (rs) {
            updateRecipeDetainInfo(recipe, paramMap);
            Map<String, Object> orderAttr = getOrderInfoMap(recipe, paramMap);
            orderAttr.put("sendTime", sendDate);
            orderAttr.put("status", OrderStatusConstant.SENDING);
            //此处为物流公司字典
            String logisticsCompany = MapValueUtil.getString(paramMap, "logisticsCompany");
            String trackingNumber = MapValueUtil.getString(paramMap, "trackingNumber");
            orderAttr.put("logisticsCompany", StringUtils.isEmpty(logisticsCompany)?null:Integer.valueOf(logisticsCompany));
            orderAttr.put("trackingNumber", trackingNumber);
            orderService.finishOrder(recipe.getOrderCode(), recipe.getPayMode(), orderAttr);
            RecipeResultBean resultBean = orderService.updateOrderInfo(recipe.getOrderCode(), orderAttr, null);
            LOGGER.info("toSend 订单更新 result={}", JSONUtils.toString(resultBean));

            String company = logisticsCompany;
            try {
                company = DictionaryController.instance().get("eh.cdr.dictionary.LogisticsCompany").getText(logisticsCompany);
            } catch (ControllerException e) {
                LOGGER.error("toSend get logisticsCompany error. logisticsCompany=" + logisticsCompany);
            }
            //记录日志
            RecipeLogService.saveRecipeLog(recipeId, recipe.getStatus(), RecipeStatusConstant.IN_SEND, "配送中,配送人：" + sender
                    + ",快递公司：" + company + ",快递单号：" + trackingNumber);
            //信息推送
            RecipeMsgService.batchSendMsg(recipeId, RecipeStatusConstant.IN_SEND);
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

        ThirdResultBean backMsg = ThirdResultBean.getFail();
        int code = validateRecipe(paramMap, backMsg, RecipeStatusConstant.IN_SEND, RecipeStatusConstant.FINISH);

        if (REQUEST_ERROR_REAPET == code) {
            backMsg.setCode(REQUEST_OK);
            return backMsg;
        } else if (REQUEST_ERROR == code) {
            LOGGER.error("recipeId=[{}], finishRecipe:{}", backMsg.getBusId(), JSONUtils.toString(backMsg));
            return backMsg;
        }

        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);

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
        attrMap.put("giveUser", sender);
        //如果是货到付款还要更新付款时间和付款状态
        if (RecipeBussConstant.GIVEMODE_SEND_TO_HOME.equals(recipe.getGiveMode()) && RecipeBussConstant.PAYMODE_COD.equals(recipe.getPayMode())) {
            attrMap.put("payFlag", 1);
            attrMap.put("payDate", new Date());
        }
        String recipeFeeStr = MapValueUtil.getString(paramMap, "recipeFee");
        if (StringUtils.isNotEmpty(recipeFeeStr)) {
            attrMap.put("totalMoney", new BigDecimal(recipeFeeStr));
        }
        //更新处方信息
        Boolean rs = recipeDAO.updateRecipeInfoByRecipeId(recipeId, RecipeStatusConstant.FINISH, attrMap);

        if (rs) {
            //完成订单
            RecipeOrderService orderService = ApplicationUtils.getRecipeService(RecipeOrderService.class);
            RecipeHisService hisService = ApplicationUtils.getRecipeService(RecipeHisService.class);

            updateRecipeDetainInfo(recipe, paramMap);
            Map<String, Object> orderAttr = getOrderInfoMap(recipe, paramMap);
            orderService.finishOrder(recipe.getOrderCode(), recipe.getPayMode(), orderAttr);
            //保存至电子病历
            RecipeService recipeService = ApplicationUtils.getRecipeService(RecipeService.class);
            recipeService.saveRecipeDocIndex(recipe);
            //记录日志
            RecipeLogService.saveRecipeLog(recipeId, RecipeStatusConstant.IN_SEND, RecipeStatusConstant.FINISH, "配送到家处方单完成,配送人：" + sender);
            //HIS消息发送
            hisService.recipeFinish(recipeId);
            if (RecipeBussConstant.GIVEMODE_SEND_TO_HOME.equals(recipe.getGiveMode())) {
                //配送到家
                RecipeMsgService.batchSendMsg(recipe, RecipeStatusConstant.PATIENT_REACHPAY_FINISH);
            }
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
     * 更新处方相关信息
     *
     * @param paramMap
     * @return
     */
    @RpcService
    public ThirdResultBean updateRecipeInfo(Map<String, Object> paramMap) {
        LOGGER.info("updateRecipeInfo param : " + JSONUtils.toString(paramMap));

        ThirdResultBean backMsg = ThirdResultBean.getFail();
        int code = validateRecipe(paramMap, backMsg, null, null);

        if (REQUEST_OK != code) {
            LOGGER.error("recipeId=[{}], updateRecipeInfo:{}", backMsg.getBusId(), JSONUtils.toString(backMsg));
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
                if (!detailList.isEmpty()) {
                    RecipeDetailDAO recipeDetailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);

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
        orderAttr.put("logisticsCompany", Integer.valueOf(logisticsCompany));
        orderAttr.put("trackingNumber", trackingNumber);
        RecipeResultBean resultBean = orderService.updateOrderInfo(recipe.getOrderCode(), orderAttr, null);

        if (detailRs && null != resultBean && resultBean.getCode().equals(RecipeResultBean.SUCCESS)) {
            code = REQUEST_OK;
            errorMsg = "";
        }

        if (null != recipeId) {
            RecipeLogService.saveRecipeLog(recipeId, RecipeStatusConstant.UNKNOW, RecipeStatusConstant.UNKNOW, "updateRecipeInfo info=" + JSONUtils.toString(paramMap));
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
                SaleDrugListDAO saleDrugListDAO = DAOFactory.getDAO(SaleDrugListDAO.class);
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

        ThirdResultBean backMsg = ThirdResultBean.getFail();
        int code = validateRecipe(paramMap, backMsg, null, null);

        if (REQUEST_OK != code) {
            LOGGER.error("recipeId=[{}], recordDrugStoreResult:{}", backMsg.getBusId(), JSONUtils.toString(backMsg));
            return backMsg;
        }

        RecipeOrderService orderService = ApplicationUtils.getRecipeService(RecipeOrderService.class);
        RecipeService recipeService = ApplicationUtils.getRecipeService(RecipeService.class);
        RecipeHisService hisService = ApplicationUtils.getRecipeService(RecipeHisService.class);
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);

        Recipe recipe = backMsg.getRecipe();
        Integer recipeId = recipe.getRecipeId();
        String errorMsg = "";
        Map<String, Object> attrMap = Maps.newHashMap();
        String sendDateStr = MapValueUtil.getString(paramMap, "sendDate");
        String result = MapValueUtil.getString(paramMap, "result");
        String result1 = "1";
        if (result1.equals(result)) {
            //取药成功
            //修改处方单信息
            attrMap.put("giveDate", StringUtils.isEmpty(sendDateStr) ? DateTime.now().toDate() :
                    DateConversion.parseDate(sendDateStr, DateConversion.DEFAULT_DATE_TIME));
            attrMap.put("giveFlag", 1);
            attrMap.put("payFlag", 1);
            attrMap.put("payDate", attrMap.get("giveDate"));
            String recipeFeeStr = MapValueUtil.getString(paramMap, "recipeFee");
            if (StringUtils.isNotEmpty(recipeFeeStr)) {
                attrMap.put("totalMoney", new BigDecimal(recipeFeeStr));
            }
            //更新处方信息
            Boolean rs = recipeDAO.updateRecipeInfoByRecipeId(recipeId, RecipeStatusConstant.FINISH, attrMap);
            if (rs) {
                updateRecipeDetainInfo(recipe, paramMap);
                Map<String, Object> orderAttr = getOrderInfoMap(recipe, paramMap);
                //完成订单，不需要检查订单有效性，就算失效的订单也直接变成已完成
                orderService.finishOrder(recipe.getOrderCode(), recipe.getPayMode(), orderAttr);
                //保存至电子病历
                recipeService.saveRecipeDocIndex(recipe);
                //记录日志
                RecipeLogService.saveRecipeLog(recipeId, recipe.getStatus(), RecipeStatusConstant.FINISH, "到店取药订单完成");
                //HIS消息发送
                hisService.recipeFinish(recipeId);
                //发送取药完成消息
                RecipeMsgService.batchSendMsg(recipeId, RecipeStatusConstant.PATIENT_GETGRUG_FINISH);
            } else {
                code = ErrorCode.SERVICE_ERROR;
                errorMsg = "电子处方更新失败";
            }
        } else {
            //患者未取药
            Boolean rs = recipeDAO.updateRecipeInfoByRecipeId(recipeId, RecipeStatusConstant.NO_DRUG, attrMap);
            if (rs) {
                orderService.cancelOrderByCode(recipe.getOrderCode(), OrderStatusConstant.CANCEL_AUTO);
                RecipeLogService.saveRecipeLog(recipeId, recipe.getStatus(), RecipeStatusConstant.NO_DRUG, "到店取药失败，原因:" + MapValueUtil.getString(paramMap, "reason"));
                //发送取药失败消息
                RecipeMsgService.batchSendMsg(recipeId, RecipeStatusConstant.NO_DRUG);
            }
        }

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

        ThirdResultBean backMsg = ThirdResultBean.getFail();
        int code = validateRecipe(paramMap, backMsg, null, null);

        if (REQUEST_OK != code) {
            LOGGER.error("recipeId=[{}], userConfirm:{}", backMsg.getBusId(), JSONUtils.toString(backMsg));
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
            RecipeOrderDAO orderDAO = DAOFactory.getDAO(RecipeOrderDAO.class);
            RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);

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
                    order = orderDAO.getByOrderCode(recipe.getOrderCode());
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

                //复杂版地址处理
                /*if(StringUtils.isNotEmpty(address) && StringUtils.isNotEmpty(receiver) && StringUtils.isNotEmpty(mobile)){
                    AddrAreaService addrAreaService = ApplicationUtils.getRecipeService(AddrAreaService.class);
                    AddressDAO addressDAO = DAOFactory.getDAO(AddressDAO.class);

                    //用于标记药企传入地址是否能完全匹配
                    boolean addressIsOk = false;
                    orderAttrMap.put("receiver",receiver);
                    orderAttrMap.put("recMobile",mobile);
                    //处理地址信息
                    orderAttrMap.put("address4",address);
                    String province = MapValueUtil.getString(paramMap,"province");
                    String city = MapValueUtil.getString(paramMap,"city");
                    String area = MapValueUtil.getString(paramMap,"area");
                    List<AddrArea> areas = addrAreaService.getByName(area,null);
                    if(CollectionUtils.isNotEmpty(areas)){
                        if(areas.size() == 1){
                            String areaCode = areas.get(0).getId();
                            if(StringUtils.isNotEmpty(areaCode)) {
                                orderAttrMap.put("address3", areaCode);
                                orderAttrMap.put("address2", areaCode.substring(0,4));
                                orderAttrMap.put("address1", areaCode.substring(0,2));
                                addressIsOk = true;
                            }
                        }else{
                            //获取到多个地址对象则需要从省份开始查询
                            List<AddrArea> pareas = addrAreaService.getByName(province,null);
                            if(CollectionUtils.isNotEmpty(pareas) && pareas.size() == 1){
                                orderAttrMap.put("address1", pareas.get(0).getId());
                                //省份一般就一个值
                                //获取城市
                                List<AddrArea> careas = addrAreaService.getByName(city,pareas.get(0).getId());
                                if(CollectionUtils.isNotEmpty(careas)){
                                    orderAttrMap.put("address2", careas.get(0).getId());
                                    //某个省里面一般只有一个城市
                                    for(AddrArea a : areas){
                                        if(a.getId().startsWith(careas.get(0).getId())){
                                            orderAttrMap.put("address3", a.getId());
                                            addressIsOk = true;
                                            break;
                                        }
                                    }
                                }
                            }
                        }

                        if(addressIsOk) {
                            //添加地址到用户地址进行保存
                            Address newAddress = new Address();
                            newAddress.setMpiId(recipe.getMpiid());
                            newAddress.setAddress1(MapValueUtil.getString(orderAttrMap, "address1"));
                            newAddress.setAddress2(MapValueUtil.getString(orderAttrMap, "address2"));
                            newAddress.setAddress3(MapValueUtil.getString(orderAttrMap, "address3"));
                            newAddress.setAddress4(MapValueUtil.getString(orderAttrMap, "address4"));
                            newAddress.setReceiver(receiver);
                            newAddress.setRecMobile(mobile);
                            try {
                                Integer addressId = addressDAO.addAddress(newAddress);
                                if(null != addressId){
                                    orderAttrMap.put("addressID", addressId);
                                }
                            } catch (Exception e) {
                                logger.error("userConfirm addAddress error[{}].", e.getMessage());
                            }
                        }
                    }

                    if(!addressIsOk){
                        //不对订单表的地址进行更新
                        orderAttrMap.remove("address1");
                        orderAttrMap.remove("address2");
                        orderAttrMap.remove("address3");
                        orderAttrMap.remove("address4");

                        //将没能匹配的地址存入处方address4字段
                        recipeDAO.updateRecipeInfoByRecipeId(recipeId, ImmutableMap.of("address4",province+city+area+address));
                    }
                }*/

                orderAttrMap.put("drugStoreName", MapValueUtil.getString(paramMap, "drugstore"));
                orderAttrMap.put("drugStoreAddr", MapValueUtil.getString(paramMap, "drugstoreAddr"));
                orderService.updateOrderInfo(order.getOrderCode(), orderAttrMap, null);

                RecipeLogService.saveRecipeLog(recipeId, RecipeStatusConstant.READY_CHECK_YS, RecipeStatusConstant.READY_CHECK_YS, "userConfirm 用户确认处方，result=" + result);

                //拼装微信地址
                String needWxUrl = MapValueUtil.getString(paramMap, "wxUrl");
                if (StringUtils.isEmpty(needWxUrl)) {
                    String appid = MapValueUtil.getString(paramMap, "appid");
                    if (StringUtils.isNotEmpty(appid)) {
                        IWXServiceInterface wxService = ApplicationUtils.getService(IWXServiceInterface.class, "wxService");
                        Map<String, String> paramsMap = Maps.newHashMap();
                        paramsMap.put("module", "orderList");
//                        paramsMap.put("cid", order.getOrderId().toString());
                        String wxUrl = wxService.getSinglePageUrl(appid, paramsMap);

                        if (StringUtils.isNotEmpty(wxUrl)) {
                            wxUrl = wxUrl.replace("&connect_redirect=1", "");
                            backMsg.setMsg(wxUrl);
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
     * @param backMsg
     * @param beforeStatus 有值进行校验，没值不校验
     * @return
     */
    private int validateRecipe(Map<String, Object> paramMap, ThirdResultBean thirdResultBean, Integer beforeStatus, Integer afterStatus) {
        int code = REQUEST_OK;
        if (null == paramMap) {
            code = REQUEST_ERROR;
            return code;
        }

        String errorMsg = "";
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        Recipe recipe = null;
        //处方查询条件可分为
        //1 处方ID
        //2 recipeCode由  机构ID-处方编号  组成 （钥世圈）
        Integer recipeId = MapValueUtil.getInteger(paramMap, "recipeId");
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

        if (null == recipe) {
            code = REQUEST_ERROR;
            errorMsg = "该处方不存在";
        }

        if (REQUEST_OK == code && null != beforeStatus) {
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

    /**
     * 获取订单修改信息
     *
     * @param paramMap
     * @return
     */
    private Map getOrderInfoMap(Recipe recipe, Map<String, Object> paramMap) {
        Map<String, Object> attrMap = Maps.newHashMap();
        // 由于只有钥世圈在用，所以实际支付价格跟总价一致，无需考虑优惠券
        if (!RecipeBussConstant.PAYMODE_COD.equals(recipe.getPayMode()) && !RecipeBussConstant.PAYMODE_TFDS.equals(recipe.getPayMode())) {
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
        RecipeDetailDAO detailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
        List<Map<String, String>> list = MapValueUtil.getList(paramMap, "details");
        if (CollectionUtils.isNotEmpty(list)) {
            List<Recipedetail> detailList = detailDAO.findByRecipeId(recipe.getRecipeId());
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
                                detailDAO.updateRecipeDetailByRecipeDetailId(recipedetail.getRecipeDetailId(), changeAttr);
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
    public List<DrugsEnterprise> findDrugsEnterpriseByStatus(final Integer status) {
        DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        return drugsEnterpriseDAO.findAllDrugsEnterpriseByStatus(status);
    }

    /**
     * 新建药企
     *
     * @param drugsEnterprise
     * @return
     * @author houxr 2016-09-11
     */
    @RpcService
    public DrugsEnterprise addDrugsEnterprise(final DrugsEnterprise drugsEnterprise) {
        if (null == drugsEnterprise) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "DrugsEnterprise is null");
        }
        DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        List<DrugsEnterprise> drugsEnterpriseList = drugsEnterpriseDAO.findAllDrugsEnterpriseByName(drugsEnterprise.getName());
        if (drugsEnterpriseList.size() != 0) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "DrugsEnterprise exist!");
        }
        drugsEnterprise.setSort(100);
        DrugsEnterprise newDrugsEnterprise = drugsEnterpriseDAO.save(drugsEnterprise);
        return newDrugsEnterprise;
    }


    /**
     * 更新药企
     *
     * @param drugsEnterprise
     * @return
     * @author houxr 2016-09-11
     */
    @RpcService
    public DrugsEnterprise updateDrugsEnterprise(final DrugsEnterprise drugsEnterprise) {
        if (null == drugsEnterprise) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "DrugsEnterprise is null");
        }
        LOGGER.info(JSONUtils.toString(drugsEnterprise));
        DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        DrugsEnterprise target = drugsEnterpriseDAO.get(drugsEnterprise.getId());
        if (null == target) {
            throw new DAOException(DAOException.ENTITIY_NOT_FOUND, "DrugsEnterprise not exist!");
        }
        BeanUtils.map(drugsEnterprise, target);
        target = drugsEnterpriseDAO.update(target);
        return target;
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
    public QueryResult<DrugsEnterprise> queryDrugsEnterpriseByStartAndLimit(final String name, final int start, final int limit) {
        DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        return drugsEnterpriseDAO.queryDrugsEnterpriseResultByStartAndLimit(name, start, limit);
    }

    @RpcService
    public List<DrugsEnterprise> findByOrganId(Integer organId) {
        DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        return drugsEnterpriseDAO.findByOrganId(organId);
    }
}
