package recipe.thread;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.ngari.recipe.entity.DrugsEnterprise;
import ctd.persistence.DAOFactory;
import ctd.util.JSONUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.ApplicationUtils;
import recipe.constant.RecipeStatusConstant;
import recipe.dao.DrugsEnterpriseDAO;
import recipe.dao.RecipeDAO;
import recipe.dao.SaleDrugListDAO;
import recipe.drugsenterprise.RemoteDrugEnterpriseService;
import recipe.service.RecipeLogService;
import recipe.service.RecipeOrderService;
import recipe.util.HttpHelper;
import recipe.util.MapValueUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * 往药企推送处方数据 Callable
 * company: ngarihealth
 * @author: 0184/yu_yun
 * @date:2016/6/15.
 */
public class PushRecipToEpCallable implements Callable<String> {

    private Logger logger = LoggerFactory.getLogger(PushRecipToEpCallable.class);

    private Integer enterpriseId;

    private List<Map<String, Object>> recipesList;

    private Set<Integer> recipeIds;

    private Map<Integer, Map<String, Object>> drugsMap;

    public PushRecipToEpCallable(Integer enterpriseId, List<Map<String, Object>> recipesList,
                                 Set<Integer> recipeIds, Map<Integer, Map<String, Object>> drugsMap) {
        this.enterpriseId = enterpriseId;
        this.recipesList = recipesList;
        this.recipeIds = recipeIds;
        this.drugsMap = drugsMap;
    }

    @Override
    public String call() throws Exception {
        if (null == enterpriseId || null == recipesList || recipesList.isEmpty()) {
            return null;
        }

        DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);

        Map<String, Object> sendMap;
        DrugsEnterprise de = drugsEnterpriseDAO.getById(enterpriseId);
        if (null != de) {
            String logPrefix = "PushRecipToEpCallable 推送药企处方，药企ID:" + this.enterpriseId + "，名称:" + de.getName() + "***";
            String busUrl = de.getBusinessUrl();
            logger.info(logPrefix + "业务URL:" + busUrl + "，推送处方单数量:" + recipesList.size());
            //推送成功的处方集合
            List<Integer> succList = new ArrayList<>();
            //记录推送失败的原因
            Map<Integer, String> errInfo = Maps.newHashMap();

            String method = "setPrscription";
            for (Map<String, Object> recipeInfo : recipesList) {
                Integer recipeId = MapValueUtil.getInteger(recipeInfo, "recipeid");
                if (null != recipeId) {
                    sendMap = Maps.newHashMap();
                    sendMap.put("access_token", de.getToken());
                    sendMap.put("action", method);
                    sendMap.put("data", recipeInfo);
                    String backMsg;

                    String sendInfoStr = JSONUtils.toString(sendMap);
                    logger.info("发送[{}][{}]内容：{}", de.getName(), method, sendInfoStr);

                    try {
                        backMsg = HttpHelper.doPost(busUrl, sendInfoStr);
                        logger.info("调用[{}][{}]结果返回={}", de.getName(), method, backMsg);
                    } catch (IOException e) {
                        logger.error("调用[{}][{}] IOException: " + e.getMessage() + "，详细数据：" + sendInfoStr, de.getName(), method,e);
                        backMsg = null;
                    }

                    //{"code":-1,"message":"falied","keyword":{"goodsid":[6],"recipeid":10,"errorMsg":"\u8ba2\u5355ID10\u5df2\u5b58\u5728\uff01"}}
                    if (StringUtils.isNotEmpty(backMsg)) {
                        Map backMap = JSONUtils.parse(backMsg, Map.class);
                        // code 1成功
                        Integer code = MapValueUtil.getInteger(backMap, "code");
                        if (1 == code) {
                            succList.add(recipeId);
                        } else {
                            RemoteDrugEnterpriseService remoteDrugService = ApplicationUtils.getRecipeService(RemoteDrugEnterpriseService.class);
                            remoteDrugService.updateAccessTokenById(code, enterpriseId);
                            Map keyword = null;
                            try {
                                keyword = (Map) backMap.get("keyword");
                            } catch (Exception e) {
                                keyword = null;
                                logger.error(logPrefix + "药企返回信息keyword解析失败",e);
                            }
                            if (null != keyword) {
                                List<Integer> drugsIdList = null;
                                if (keyword.get("goodsid") instanceof List) {
                                    drugsIdList = (List<Integer>) keyword.get("goodsid");
                                    //将药企对该药品可配送的记录置为无效
                                    SaleDrugListDAO saleDrugListDAO = DAOFactory.getDAO(SaleDrugListDAO.class);
                                    saleDrugListDAO.updateInvalidByOrganIdAndDrugIds(this.enterpriseId, drugsIdList);
                                    sendDrugsInfo(drugsIdList, de);
                                }

                                String errorMsg = MapValueUtil.getString(keyword, "errorMsg");
                                if (StringUtils.isNotEmpty(errorMsg)) {
                                    errInfo.put(recipeId, "药企ID:" + this.enterpriseId + ",药品:" + JSONUtils.toString(drugsIdList) + ",错误:" + errorMsg);
                                } else {
                                    errInfo.put(recipeId, "药企ID:" + this.enterpriseId + ",药品:" + JSONUtils.toString(drugsIdList) + ",错误: 存在不可配送药品");
                                }
                            }
                        }
                    } else {
                        errInfo.put(recipeId, "药企推送异常，无返回消息,URL:" + busUrl);
                    }
                }
            }

            RecipeOrderService orderService = ApplicationUtils.getRecipeService(RecipeOrderService.class);
            if (CollectionUtils.isNotEmpty(succList)) {
                recipeDAO.updatePushFlagByRecipeId(succList);
                for (Integer recipeId : succList) {
                    //修改订单标志位
                    orderService.updateOrderInfo(orderService.getOrderCodeByRecipeId(recipeId), ImmutableMap.of("pushFlag", 1), null);
                    RecipeLogService.saveRecipeLog(recipeId, RecipeStatusConstant.CHECK_PASS_YS, RecipeStatusConstant.CHECK_PASS_YS, "药企推送成功:" + de.getName());
                }
            }

            if (null != recipeIds && !recipeIds.isEmpty()) {
                for (Integer recipeId : recipeIds) {
                    if (!succList.contains(recipeId)) {
                        orderService.updateOrderInfo(orderService.getOrderCodeByRecipeId(recipeId), ImmutableMap.of("pushFlag", -1), null);
                        RecipeLogService.saveRecipeLog(recipeId, RecipeStatusConstant.CHECK_PASS_YS, RecipeStatusConstant.CHECK_PASS_YS, "药企推送失败:" + errInfo.get(recipeId));
                    }
                }
            }
        } else {
            logger.error("PushRecipToEpCallable 推送药企处方，药企ID: " + enterpriseId + "，药企不存在");
        }

        return null;
    }

    /**
     * 推送药企失败后往药企推送药品数据
     *
     * @param drugsIdList
     * @param de
     * @return
     */
    private boolean sendDrugsInfo(List<Integer> drugsIdList, DrugsEnterprise de) {
        if (null == drugsIdList || null == de) {
            return false;
        }

        if (!drugsIdList.isEmpty()) {
            String method = "setGoods";
            Map<String, Object> sendMap = Maps.newHashMap();
            sendMap.put("access_token", de.getToken());
            sendMap.put("action", method);

            List<Map<String, Object>> drugs = new ArrayList<>(0);
            sendMap.put("data", drugs);

            for (Integer drugId : drugsIdList) {
                if (drugsMap.containsKey(drugId)) {
                    drugs.add(drugsMap.get(drugId));
                }
            }

            String logPrefix = "sendDrugsInfo 推送药企药品数据setGoods，药企ID:" + this.enterpriseId + "，名称:" + de.getName() + "***";
            String sendInfoStr = JSONUtils.toString(sendMap);
            logger.info("发送[{}][{}]内容：{}", de.getName(), method, sendInfoStr);
            String backMsg;
            try {
                backMsg = HttpHelper.doPost(de.getBusinessUrl(), sendInfoStr);
                logger.info("调用[{}][{}]结果返回={}", de.getName(), method, backMsg);
            } catch (IOException e) {
                logger.error("调用[{}][{}] IOException: " + e.getMessage() + "，详细数据：" + sendInfoStr, de.getName(), method,e);
                backMsg = "";
            }

            if (StringUtils.isNotEmpty(backMsg)) {
                // code 1成功
                Map backMap = JSONUtils.parse(backMsg, Map.class);
                Integer code = MapValueUtil.getInteger(backMap, "code");
                if (1 == code) {
                    logger.info(logPrefix + "药品推送成功:" + JSONUtils.toString(drugs));
                    return true;
                } else {
                    RemoteDrugEnterpriseService remoteDrugService = ApplicationUtils.getRecipeService(RemoteDrugEnterpriseService.class);
                    remoteDrugService.updateAccessTokenById(code, de.getId());
                    logger.error(logPrefix + "失败信息：" + MapValueUtil.getString(backMap, "message") + "***推送信息:" + sendInfoStr);
                }
            }
        }

        return false;
    }
}
