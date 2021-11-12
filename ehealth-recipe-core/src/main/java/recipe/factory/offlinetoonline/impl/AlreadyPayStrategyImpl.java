package recipe.factory.offlinetoonline.impl;

import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.recipe.mode.QueryHisRecipResTO;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.recipe.dto.GiveModeButtonDTO;
import com.ngari.recipe.offlinetoonline.model.FindHisRecipeDetailReqVO;
import com.ngari.recipe.offlinetoonline.model.FindHisRecipeDetailResVO;
import com.ngari.recipe.offlinetoonline.model.FindHisRecipeListVO;
import com.ngari.recipe.offlinetoonline.model.SettleForOfflineToOnlineVO;
import com.ngari.recipe.recipe.model.MergeRecipeVO;
import ctd.util.JSONUtils;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.enumerate.status.OfflineToOnlineEnum;
import recipe.factory.offlinetoonline.IOfflineToOnlineStrategy;
import recipe.manager.HisRecipeManager;
import recipe.vo.patient.RecipeGiveModeButtonRes;

import java.util.List;

/**
 * @Author liumin
 * @Date 2021/6/26 上午11:42
 * @Description 线下转线上已缴费处方实现类
 */
@Service
public class AlreadyPayStrategyImpl extends BaseOfflineToOnlineService implements IOfflineToOnlineStrategy {
    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    @Autowired
    HisRecipeManager hisRecipeManager;

    @Override
    public String getHandlerMode() {
        return OfflineToOnlineEnum.OFFLINE_TO_ONLINE_ALREADY_PAY.getName();
    }

    @Override
    public List<MergeRecipeVO> findHisRecipeList(HisResponseTO<List<QueryHisRecipResTO>> hisRecipeInfos, PatientDTO patientDTO, FindHisRecipeListVO request) {
        LOGGER.info("AlreadyPayStrategyServiceImpl findHisRecipeList hisRecipeInfos:{},patientDTO:{},request:{}", JSONUtils.toString(hisRecipeInfos), JSONUtils.toString(patientDTO), JSONUtils.toString(request));
        if (null != hisRecipeInfos && !CollectionUtils.isEmpty(hisRecipeInfos.getData())) {
            try {
                // 2.更新数据校验
                hisRecipeInfoCheck(hisRecipeInfos.getData(), patientDTO);
            } catch (Exception e) {
                LOGGER.error("findHisRecipeList hisRecipeInfoCheck error ", e);
            }
            try {
                // 3.保存数据到cdr_his_recipe相关表（cdr_his_recipe、cdr_his_recipeExt、cdr_his_recipedetail）
                saveHisRecipeInfo(hisRecipeInfos, patientDTO, OfflineToOnlineEnum.OFFLINE_TO_ONLINE_ALREADY_PAY.getType());
            } catch (Exception e) {
                LOGGER.error("findHisRecipeList saveHisRecipeInfo error ", e);
            }
        }
        // 4.查询并转换成前端所需对象
        GiveModeButtonDTO giveModeButtonBean = getGiveModeButtonBean(request.getOrganId());
        List<MergeRecipeVO> res = findFinishHisRecipeList(request.getOrganId(), request.getMpiId(), giveModeButtonBean, request.getStart(), request.getLimit());
        LOGGER.info("AlreadyPayStrategyServiceImpl findHisRecipeList res:{}", JSONUtils.toString(res), JSONUtils.toString(patientDTO), JSONUtils.toString(request));
        return res;
    }

    @Override
    public FindHisRecipeDetailResVO findHisRecipeDetail(FindHisRecipeDetailReqVO request) {
        LOGGER.info("AlreadyPayStrategyServiceImpl findHisRecipeDetail request:{}", JSONUtils.toString(request));
        // 1.保存数据到cdr_recipe相关表（cdr_recipe、cdr_recipeext、cdr_recipeDetail）
        Integer recipeId = saveRecipeInfo(request.getHisRecipeId());
        // 2.通过cdrHisRecipeId返回数据详情
        FindHisRecipeDetailResVO res = getHisRecipeDetailByHisRecipeIdAndRecipeId(request.getHisRecipeId(), recipeId);
        LOGGER.info("AlreadyPayStrategyServiceImpl findHisRecipeDetail res:{}", JSONUtils.toString(res));
        return res;
    }

    @Override
    public List<RecipeGiveModeButtonRes> settleForOfflineToOnline(SettleForOfflineToOnlineVO request) {
        return null;
    }

}
