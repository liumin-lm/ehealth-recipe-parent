package recipe.mq;

import com.alibaba.fastjson.JSON;
import com.ngari.recipe.entity.Recipe;
import com.ngari.revisit.common.model.RevisitStatusNotifyDTO;
import com.ngari.revisit.enums.StatusEnum;
import ctd.net.broadcast.Observer;
import ctd.util.AppContextHolder;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.dao.RecipeDAO;
import recipe.enumerate.status.RecipeStateEnum;
import recipe.enumerate.status.RecipeStatusEnum;
import recipe.manager.StateManager;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static ctd.persistence.DAOFactory.getDAO;

/**
 * @author fuzi
 */
public class MqRevisitRecipeServer implements Observer<RevisitStatusNotifyDTO> {
    private static final Logger logger = LoggerFactory.getLogger(MqRevisitRecipeServer.class);

    private static final List<Integer> revisitStatus = Arrays.asList(StatusEnum.CANCEL_OF_NO_PAY.getKey(),
            StatusEnum.CANCEL_OF_PATIENT.getKey(),StatusEnum.CANCEL_OF_SYSTEM.getKey(),StatusEnum.CANCEL_OF_HIS.getKey(),
            StatusEnum.REFUSED.getKey(),StatusEnum.FINISHED.getKey(),StatusEnum.RETURNED.getKey(),StatusEnum.CANCEL_OF_DOCTOR.getKey()
            ,StatusEnum.CANCEL_OF_PATIENT_REFUND.getKey());

    @Override
    public void onMessage(RevisitStatusNotifyDTO revisitStatusNotifyDTO) {
        logger.info("MqRevisitRecipeServer onMessage revisitStatusNotifyDTO ={} ", JSON.toJSONString(revisitStatusNotifyDTO));
        if (!revisitStatus.contains(revisitStatusNotifyDTO.getRevisitStatus())) {
            return;
        }
        RecipeDAO recipeDAO = getDAO(RecipeDAO.class);
        List<Recipe> recipeList = recipeDAO.findRecipeClinicIdAndStatusV1(revisitStatusNotifyDTO.getRevisitId(), 2, Collections.singletonList(RecipeStatusEnum.RECIPE_STATUS_UNSIGNED.getType()));
        logger.info("MqRevisitRecipeServer onMessage recipeList ={} ", JSON.toJSONString(recipeList));
        if (CollectionUtils.isEmpty(recipeList)) {
            return;
        }
        StateManager stateManager = AppContextHolder.getBean("stateManager", StateManager.class);
        recipeList.forEach(a -> {
            stateManager.updateStatus(a.getRecipeId(), RecipeStatusEnum.RECIPE_STATUS_DELETE);
            stateManager.updateRecipeState(a.getRecipeId(), RecipeStateEnum.PROCESS_STATE_DELETED, RecipeStateEnum.SUB_DELETED_REVISIT_END);
        });
    }
}
