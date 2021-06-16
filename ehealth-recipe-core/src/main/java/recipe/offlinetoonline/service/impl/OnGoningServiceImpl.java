package recipe.offlinetoonline.service.impl;

import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.recipe.mode.QueryHisRecipResTO;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.patient.service.PatientService;
import com.ngari.recipe.entity.HisRecipe;
import com.ngari.recipe.recipe.model.GiveModeButtonBean;
import com.ngari.recipe.recipe.model.HisPatientTabStatusMergeRecipeVO;
import com.ngari.recipe.recipe.model.HisRecipeVO;
import ctd.persistence.exception.DAOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import recipe.bean.RecipeGiveModeButtonRes;
import recipe.bussutil.openapi.util.JSONUtils;
import recipe.offlinetoonline.constant.OfflineToOnlineEnum;
import recipe.offlinetoonline.service.IOfflineToOnlineService;
import recipe.offlinetoonline.service.third.RecipeHisService;
import recipe.offlinetoonline.vo.FindHisRecipeDetailVO;
import recipe.offlinetoonline.vo.FindHisRecipeListVO;
import recipe.offlinetoonline.vo.SettleForOfflineToOnlineVO;
import recipe.service.HisRecipeService;
import recipe.service.OfflineToOnlineService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @Author liumin
 * @Date 2021/6/8上午11:42
 * @Description 线下转线上进行中处方实现类
 */
@Service
public class OnGoningServiceImpl implements IOfflineToOnlineService {

    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    @Autowired
    HisRecipeService offlineToOnlineService;

    @Autowired
    RecipeHisService recipeHisService;

    @Autowired
    @Qualifier("basic.patientService")
    PatientService patientService;

    @Autowired
    OfflineToOnlineService offlineToOnlineService2;

    @Override
    public List<HisPatientTabStatusMergeRecipeVO> findHisRecipeList(HisResponseTO<List<QueryHisRecipResTO>> hisRecipeInfos, PatientDTO patientDTO, FindHisRecipeListVO request) {
        // 2、返回进行中的线下处方
        GiveModeButtonBean giveModeButtonBean=offlineToOnlineService.getGiveModeButtonBean(request.getOrganId());
        return offlineToOnlineService.findOngoingHisRecipe(hisRecipeInfos.getData(), patientDTO, giveModeButtonBean, request.getStart(), request.getLimit());

    }

    @Override
    public Map<String, Object> findHisRecipeDetail(FindHisRecipeDetailVO request) {
        // 跟待处理获取详情一致 先判断数据是否变更 然后返回详情
        // 1.返回数据详情

        return new;

    }

    @Override
    public List<RecipeGiveModeButtonRes> settleForOfflineToOnline(SettleForOfflineToOnlineVO request) {
        LOGGER.info("NoPayServiceImpl settleForOfflineToOnline request = {}",  JSONUtils.toString(request));
        // 1、线下转线上
        List<Integer> recipeIds = offlineToOnlineService2.batchSyncRecipeFromHis(request);
        // 2、获取购药按钮
        List<RecipeGiveModeButtonRes> recipeGiveModeButtonResList = offlineToOnlineService2.getRecipeGiveModeButtonRes(recipeIds);
        LOGGER.info("NoPayServiceImpl settleForOfflineToOnline response:{}", JSONUtils.toString(recipeGiveModeButtonResList));
        return recipeGiveModeButtonResList;
    }

    @Override
    public Integer getPayMode() {
        return OfflineToOnlineEnum.OFFLINE_TO_ONLINE_NO_PAY.getType();
    }


}
