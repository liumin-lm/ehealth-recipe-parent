package recipe.presettle;

import com.alibaba.fastjson.JSONArray;
import com.google.common.collect.Maps;
import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.recipe.mode.RecipeCashPreSettleInfo;
import com.ngari.his.recipe.mode.RecipeCashPreSettleReqTO;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.patient.service.BasicAPI;
import com.ngari.patient.service.PatientService;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeExtend;
import com.ngari.recipe.entity.RecipeOrder;
import ctd.persistence.DAOFactory;
import ctd.util.AppContextHolder;
import ctd.util.JSONUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import recipe.ApplicationUtils;
import recipe.constant.RecipeBussConstant;
import recipe.dao.RecipeDAO;
import recipe.dao.RecipeExtendDAO;
import recipe.dao.RecipeOrderDAO;
import recipe.hisservice.RecipeToHisService;
import recipe.purchase.PurchaseEnum;
import recipe.service.RecipeLogService;
import recipe.service.RecipeOrderService;
import recipe.util.MapValueUtil;

import java.util.Map;

/**
 * created by shiyuping on 2020/11/27
 * 自费预结算
 * @author shiyuping
 */
@Service
public class CashPreSettleService implements IRecipePreSettleService {
    private static final Logger LOGGER = LoggerFactory.getLogger(CashPreSettleService.class);
    @Override
    public Map<String, Object> recipePreSettle(Integer recipeId, Map<String, Object> extInfo) {
        LOGGER.info("CashPreSettleService.recipePreSettle req recipeId={} extInfo={}",recipeId, JSONArray.toJSONString(extInfo));
        Map<String, Object> result = Maps.newHashMap();
        result.put("code", "-1");
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        RecipeExtendDAO recipeExtendDAO = DAOFactory.getDAO(RecipeExtendDAO.class);
        RecipeOrderDAO recipeOrderDAO = DAOFactory.getDAO(RecipeOrderDAO.class);
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        if (recipe == null) {
            result.put("msg", "查不到该处方");
            return result;
        }
        try {
            Integer payMode = MapValueUtil.getInteger(extInfo, "payMode");
            RecipeCashPreSettleReqTO request = new RecipeCashPreSettleReqTO();
            //购药方式
            if (RecipeBussConstant.PAYMODE_COD.equals(payMode)) {
                //配送到家
                request.setDeliveryType("1");
            } else if (RecipeBussConstant.PAYMODE_TO_HOS.equals(payMode)) {
                //到院取药
                request.setDeliveryType("0");
            }
            request.setClinicOrgan(recipe.getClinicOrgan());
            request.setRecipeId(String.valueOf(recipeId));
            request.setHisRecipeNo(recipe.getRecipeCode());
            //患者信息
            PatientService patientService = BasicAPI.getService(PatientService.class);
            PatientDTO patientBean = patientService.get(recipe.getMpiid());
            request.setPatientName(patientBean.getPatientName());
            request.setIdcard(patientBean.getIdcard());
            RecipeToHisService service = AppContextHolder.getBean("recipeToHisService", RecipeToHisService.class);
            LOGGER.info("CashPreSettleService recipeId={} req={}", recipeId, JSONUtils.toString(request));
            HisResponseTO<RecipeCashPreSettleInfo> hisResult = service.recipeCashPreSettleHis(request);
            LOGGER.info("CashPreSettleService recipeId={} res={}", recipeId, JSONUtils.toString(hisResult));
            if (hisResult != null && "200".equals(hisResult.getMsgCode())) {
                if (hisResult.getData() != null) {
                    //自费金额
                    String cashAmount = hisResult.getData().getZfje();
                    //总金额
                    String totalAmount = hisResult.getData().getZje();
                    //his收据号
                    String hisSettlementNo = hisResult.getData().getSjh();
                    if (StringUtils.isNotEmpty(cashAmount) && StringUtils.isNotEmpty(totalAmount)) {
                            Map<String, String> map = Maps.newHashMap();
                            map.put("preSettleTotalAmount", totalAmount);
                            map.put("cashAmount", cashAmount);
                            map.put("hisSettlementNo", hisSettlementNo);
                            //订单信息更新
                            RecipeOrder recipeOrder = recipeOrderDAO.getByOrderCode(recipe.getOrderCode());
                            if (recipeOrder != null) {
                                RecipeOrderService recipeOrderService = ApplicationUtils.getRecipeService(RecipeOrderService.class);
                                if (!recipeOrderService.dealWithOrderInfo(map, recipeOrder, recipe)) {
                                    result.put("msg", "预结算更新订单信息失败");
                                    return result;
                                }
                            }
                        }
                    result.put("totalAmount", totalAmount);
                    result.put("cashAmount", cashAmount);
                }
                result.put("code", "200");
                //日志记录
                RecipeLogService.saveRecipeLog(recipeId, recipe.getStatus(), recipe.getStatus(), "处方自费预结算成功");
            } else if (hisResult != null && "0".equals(hisResult.getMsgCode())) {
                result.put("code", "200");
                //日志记录
                RecipeLogService.saveRecipeLog(recipeId, recipe.getStatus(), recipe.getStatus(), "处方自费预结算成功，无返回值");
            } else {
                String msg;
                if (hisResult != null) {
                    msg = "his返回:" + hisResult.getMsg();
                } else {
                    msg = "平台前置机未实现自费预结算接口";
                }
                result.put("msg", msg);
                //日志记录
                RecipeLogService.saveRecipeLog(recipeId, recipe.getStatus(), recipe.getStatus(), "处方自费预结算失败,原因:" + msg);
            }
        } catch (Exception e) {
            LOGGER.error("CashPreSettleService recipeId={} error", recipeId, e);
        }
        return result;
    }
}
