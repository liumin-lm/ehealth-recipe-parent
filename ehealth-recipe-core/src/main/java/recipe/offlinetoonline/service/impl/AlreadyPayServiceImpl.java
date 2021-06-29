package recipe.offlinetoonline.service.impl;

import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.recipe.mode.QueryHisRecipResTO;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.recipe.recipe.model.GiveModeButtonBean;
import com.ngari.recipe.recipe.model.MergeRecipeVO;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.bean.RecipeGiveModeButtonRes;
import recipe.offlinetoonline.constant.OfflineToOnlineEnum;
import recipe.offlinetoonline.service.IOfflineToOnlineService;
import recipe.offlinetoonline.vo.FindHisRecipeDetailVO;
import recipe.offlinetoonline.vo.FindHisRecipeListVO;
import recipe.offlinetoonline.vo.SettleForOfflineToOnlineVO;
import recipe.service.OfflineToOnlineService;

import java.util.List;
import java.util.Map;

/**
 * @Author liumin
 * @Date 2021/6/26 上午11:42
 * @Description 线下转线上已缴费处方实现类
 */
@Service
public class AlreadyPayServiceImpl implements IOfflineToOnlineService {
    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private OfflineToOnlineService offlineToOnlineService;

    @Override
    public String getHandlerMode() {
        return OfflineToOnlineEnum.OFFLINE_TO_ONLINE_ALREADY_PAY.getName();
    }

    @Override
    public List<MergeRecipeVO>  findHisRecipeList(HisResponseTO<List<QueryHisRecipResTO>> hisRecipeInfos, PatientDTO patientDTO, FindHisRecipeListVO request) {
        try {
            if (null == hisRecipeInfos || CollectionUtils.isEmpty(hisRecipeInfos.getData())) {
                return null;
            }
            // 2.更新数据校验
            offlineToOnlineService.hisRecipeInfoCheck(hisRecipeInfos.getData(), patientDTO);
        } catch (Exception e) {
            LOGGER.error("findHisRecipeList hisRecipeInfoCheck error ", e);
        }
        try {
            // 3.保存数据到cdr_his_recipe相关表（cdr_his_recipe、cdr_his_recipeExt、cdr_his_recipedetail）
            offlineToOnlineService.saveHisRecipeInfo(hisRecipeInfos, patientDTO, 2);
        } catch (Exception e) {
            LOGGER.error("findHisRecipeList saveHisRecipeInfo error ", e);
        }
        // 4.查询并转换成前端所需对象
        GiveModeButtonBean giveModeButtonBean=offlineToOnlineService.getGiveModeButtonBean(request.getOrganId());
        return offlineToOnlineService.findFinishHisRecipeList(request.getOrganId(),request.getMpiId(), giveModeButtonBean, request.getStart(), request.getLimit());
    }

    @Override
    public Map<String, Object> findHisRecipeDetail(FindHisRecipeDetailVO request) {
        // 1.保存数据到cdr_recipe相关表（cdr_recipe、cdr_recipeext、cdr_recipeDetail）
        Integer recipeId=offlineToOnlineService.saveRecipeInfo(request.getHisRecipeId());
        // 2.通过cdrHisRecipeId返回数据详情
        return offlineToOnlineService.getHisRecipeDetailByHisRecipeIdAndRecipeId(request.getHisRecipeId(),recipeId);
    }

    @Override
    public List<RecipeGiveModeButtonRes> settleForOfflineToOnline(SettleForOfflineToOnlineVO request) {
        return null;
    }

}
