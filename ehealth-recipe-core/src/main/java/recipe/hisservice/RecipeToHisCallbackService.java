package recipe.hisservice;

import com.google.common.collect.Lists;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.Recipedetail;
import com.ngari.recipe.recipe.model.HisSendResTO;
import com.ngari.recipe.recipe.model.OrderRepTO;
import ctd.persistence.DAOFactory;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.bean.RecipeCheckPassResult;
import recipe.dao.RecipeDAO;
import recipe.service.DrugsEnterpriseService;
import recipe.service.HisCallBackService;
import recipe.service.RecipeLogService;
import recipe.util.LocalStringUtil;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author yuyun
 * 接收处方与HIS交互回调消息
 * 写入HIS处方服务类，由于老版本HIS只会通过BASE调用接口，回调消息也只能通过BASE中转
 */
@RpcBean("recipeToHisCallbackService")
public class RecipeToHisCallbackService {

    /**
     * LOGGER
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(RecipeToHisCallbackService.class);

    @Autowired
    private DrugsEnterpriseService drugsEnterpriseService;

    /**
     * @param response
     * @return his返回结果消息
     */
    @RpcService
    public void sendSuccess(HisSendResTO response) {
        LOGGER.info("recipeSend recive success. recipeId={}, response={}", response.getRecipeId(), JSONUtils.toString(response));
        List<OrderRepTO> repList = response.getData();
        if (CollectionUtils.isNotEmpty(repList)) {
            RecipeCheckPassResult result = new RecipeCheckPassResult();
            //以下数据取第一个数据是因为这些数据是处方相关的
            String recipeNo = repList.get(0).getRecipeNo();
            String patientId = repList.get(0).getPatientID();
            String amount = repList.get(0).getAmount();
            boolean isWuChang = false;
            //是否武昌模式
            if (StringUtils.isNotEmpty(repList.get(0).getIsDrugStock())){
                isWuChang = true;
            }

            Recipedetail detail;
            List<Recipedetail> list = Lists.newArrayList();
            boolean isDrugStock = true;

            for (OrderRepTO rep : repList) {
                detail = new Recipedetail();
                //是否有库存
                if (StringUtils.isNotEmpty(rep.getIsDrugStock())
                        &&"0".equals(rep.getIsDrugStock())){
                    isDrugStock = false;
                }
                if (StringUtils.isNotEmpty(rep.getPrice())) {
                    detail.setDrugCost(new BigDecimal(rep.getPrice()));
                }
                detail.setRecipeDetailId(Integer.valueOf(rep.getOrderID()));
                detail.setOrderNo(LocalStringUtil.toString(rep.getOrderNo()));
                detail.setDrugGroup(LocalStringUtil.toString(rep.getSetNo()));
                //取药窗口是否都是返回同一窗口
                detail.setPharmNo(LocalStringUtil.toString(rep.getPharmNo()));
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
            LOGGER.info("recipeSend recive success. recipeId={}, checkPassSuccess result={}", response.getRecipeId(), JSONUtils.toString(result));
            HisCallBackService.checkPassSuccess(result, true);
            //没库存操作----推送九州通
            RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
            Recipe recipe = recipeDAO.getByRecipeId(Integer.valueOf(response.getRecipeId()));
            String memo = "";
            if (!isDrugStock){
                //没库存操作----推送九州通
                drugsEnterpriseService.pushHosInteriorSupport(recipe.getRecipeId(),recipe.getClinicOrgan());
                //发送患者没库存消息


                memo = "药品没库存,推送九州通";
                //日志记录
                RecipeLogService.saveRecipeLog(recipe.getRecipeId(), recipe.getStatus(), recipe.getStatus(), memo);
            }else if (isWuChang){
                //有库存操作----发送患者消息


                memo = "药品有库存,发生患者取药消息";
                //日志记录
                RecipeLogService.saveRecipeLog(recipe.getRecipeId(), recipe.getStatus(), recipe.getStatus(), memo);
            }
        } else {
            LOGGER.error("recipeSend recive success. recipeId={}, data is empty. ");
        }
    }


    /**
     * 该处方单发送his系统失败并给医生发送推送和系统消息通知。
     *
     * @param response
     */
    @RpcService
    public void sendFail(HisSendResTO response) {
        LOGGER.error("recipeSend recive fail. recipeId={}, response={}", response.getRecipeId(), JSONUtils.toString(response));
        // 给申请医生，患者发送推送消息
        HisCallBackService.checkPassFail(Integer.valueOf(response.getRecipeId()), response.getMsgCode(), response.getMsg());
    }
}
