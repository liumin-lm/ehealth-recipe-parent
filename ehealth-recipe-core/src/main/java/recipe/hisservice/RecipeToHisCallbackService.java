package recipe.hisservice;

import com.google.common.collect.Lists;
import com.ngari.recipe.entity.Recipedetail;
import com.ngari.recipe.recipe.model.HisSendResTO;
import com.ngari.recipe.recipe.model.OrderRepTO;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.bean.RecipeCheckPassResult;
import recipe.service.HisCallBackService;
import recipe.util.LocalStringUtil;

import java.math.BigDecimal;
import java.util.List;

/**
 * 接收处方与HIS交互回调消息
 * 写入HIS处方服务类，由于老版本HIS只会通过BASE调用接口，回调消息也只能通过BASE中转
 */
@RpcBean("recipeToHisCallbackService")
public class RecipeToHisCallbackService {

    /**
     * logger
     */
    private static final Logger logger = LoggerFactory.getLogger(RecipeToHisCallbackService.class);

    /**
     * @param response
     * @return his返回结果消息
     */
    @RpcService
    public void sendSuccess(HisSendResTO response) {
        logger.info("recipeSend recive success. recipeId={}, response={}", response.getRecipeId(), JSONUtils.toString(response));
        List<OrderRepTO> repList = response.getData();
        if (CollectionUtils.isNotEmpty(repList)) {
            RecipeCheckPassResult result = new RecipeCheckPassResult();
            //以下数据取第一个数据是因为这些数据是处方相关的
            String recipeNo = repList.get(0).getRecipeNo();
            String patientId = repList.get(0).getPatientID();
            String amount = repList.get(0).getAmount();

            Recipedetail detail;
            List<Recipedetail> list = Lists.newArrayList();
            for (OrderRepTO rep : repList) {
                detail = new Recipedetail();
                if (StringUtils.isNotEmpty(rep.getPrice())) {
                    detail.setDrugCost(new BigDecimal(rep.getPrice()));
                }
                detail.setRecipeDetailId(Integer.valueOf(rep.getOrderID()));
                detail.setOrderNo(LocalStringUtil.toString(rep.getOrderNo()));
                detail.setDrugGroup(LocalStringUtil.toString(rep.getSetNo()));
                detail.setPharmNo(LocalStringUtil.toString(rep.getPharmNo()));//取药窗口是否都是返回同一窗口
                detail.setMemo(LocalStringUtil.toString(rep.getRemark()));
                list.add(detail);
            }
            if (!StringUtils.isEmpty(amount)) {
                BigDecimal total = new BigDecimal(amount);
                result.setTotalMoney(total);
            }
            result.setRecipeId(Integer.valueOf(response.getRecipeId()));
            result.setRecipeCode(recipeNo);
            result.setPatientID(patientId);
            result.setDetailList(list);
            logger.info("recipeSend recive success. recipeId={}, checkPassSuccess result={}", response.getRecipeId(), JSONUtils.toString(result));
            HisCallBackService.checkPassSuccess(result, true);
        } else {
            logger.error("recipeSend recive success. recipeId={}, data is empty. ");
        }
    }


    /**
     * 该处方单发送his系统失败并给医生发送推送和系统消息通知。
     *
     * @param response
     */
    @RpcService
    public void sendFail(HisSendResTO response) {
        logger.error("recipeSend recive fail. recipeId={}, response={}", response.getRecipeId(), JSONUtils.toString(response));
        // 给申请医生，患者发送推送消息
        HisCallBackService.checkPassFail(Integer.valueOf(response.getRecipeId()), response.getMsgCode(), response.getMsg());
    }
}
