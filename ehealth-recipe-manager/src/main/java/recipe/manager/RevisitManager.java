package recipe.manager;

import com.ngari.common.dto.RevisitTracesMsg;
import com.ngari.recipe.entity.Recipe;
import ctd.net.broadcast.MQHelper;
import ctd.util.JSONUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.client.RevisitClient;
import recipe.common.OnsConfig;
import recipe.constant.RecipeSystemConstant;

/**
 * 复诊处理通用类
 *
 * @author fuzi
 */
@Service
public class RevisitManager extends BaseManager {
    @Autowired
    private RevisitClient revisitClient;

    /**
     * 通知复诊——添加处方追溯数据
     *
     * @param recipe
     */
    public void saveRevisitTracesList(Recipe recipe) {
        try {
            if (recipe == null) {
                logger.info("saveRevisitTracesList recipe is null ");
                return;
            }
            if (recipe.getClinicId() == null || 2 != recipe.getBussSource()) {
                logger.info("saveRevisitTracesList return param:{}", JSONUtils.toString(recipe));
                return;
            }
            RevisitTracesMsg revisitTracesMsg = new RevisitTracesMsg();
            revisitTracesMsg.setOrganId(recipe.getClinicOrgan());
            revisitTracesMsg.setConsultId(recipe.getClinicId());
            revisitTracesMsg.setBusId(recipe.getRecipeId().toString());
            revisitTracesMsg.setBusType(1);
            revisitTracesMsg.setBusNumOrder(10);
            revisitTracesMsg.setBusOccurredTime(recipe.getCreateDate());
            try {
                logger.info("saveRevisitTracesList sendMsgToMq send to MQ start, busId:{}，revisitTracesMsg:{}", recipe.getRecipeId(), JSONUtils.toString(revisitTracesMsg));
                MQHelper.getMqPublisher().publish(OnsConfig.revisitTraceTopic, revisitTracesMsg, null);
                logger.info("saveRevisitTracesList sendMsgToMq send to MQ end, busId:{}", recipe.getRecipeId());
            } catch (Exception e) {
                logger.error("saveRevisitTracesList sendMsgToMq can't send to MQ,  busId:{}", recipe.getRecipeId(), e);
            }
        } catch (Exception e) {
            logger.error("RevisitClient saveRevisitTracesList error recipeId:{}", recipe.getRecipeId(), e);
            e.printStackTrace();
        }
    }

    /**
     * 获取医生下同一个患者 最新 复诊的id
     *
     * @param mpiId        患者id
     * @param doctorId     医生id
     * @param isRegisterNo 是否存在挂号序号
     * @return 复诊id
     */
    public Integer getRevisitId(String mpiId, Integer doctorId, Boolean isRegisterNo) {
        if (isRegisterNo) {
            //获取存在挂号序号的复诊id
            return revisitClient.getRevisitIdByRegisterNo(mpiId, doctorId, RecipeSystemConstant.CONSULT_TYPE_RECIPE, true);
        }
        //获取最新的复诊id
        return revisitClient.getRevisitIdByRegisterNo(mpiId, doctorId, null, null);
    }
}
