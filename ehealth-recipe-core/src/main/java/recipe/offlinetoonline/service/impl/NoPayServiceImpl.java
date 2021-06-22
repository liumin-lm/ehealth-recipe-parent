package recipe.offlinetoonline.service.impl;

import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.recipe.mode.QueryHisRecipResTO;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.patient.service.PatientService;
import com.ngari.recipe.entity.HisRecipe;
import com.ngari.recipe.recipe.model.GiveModeButtonBean;
import com.ngari.recipe.recipe.model.MergeRecipeVO;
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
import recipe.service.OfflineToOnlineService;
import recipe.service.OfflineToOnlineService2;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @Author liumin
 * @Date 2021/1/26 上午11:42
 * @Description 线下转线上待缴费处方实现类
 */
@Service
public class NoPayServiceImpl implements IOfflineToOnlineService {

    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    @Autowired
    OfflineToOnlineService offlineToOnlineService;

    @Autowired
    RecipeHisService recipeHisService;

    @Autowired
    @Qualifier("basic.patientService")
    PatientService patientService;

    @Autowired
    OfflineToOnlineService2 offlineToOnlineService2;

    @Override
    public List<MergeRecipeVO> findHisRecipeList(HisResponseTO<List<QueryHisRecipResTO>> hisRecipeInfos, PatientDTO patientDTO, FindHisRecipeListVO request) {
        // 2、转换成前端所需recipe对象
        List<HisRecipeVO> noPayFeeHisRecipeVO = offlineToOnlineService.covertToHisRecipeVoObject(hisRecipeInfos, patientDTO);
        // 3、包装成前端所需线下处方列表对象
        GiveModeButtonBean giveModeButtonBean=offlineToOnlineService.getGiveModeButtonBean(request.getOrganId());
        return offlineToOnlineService.findOnReadyHisRecipe(noPayFeeHisRecipeVO, giveModeButtonBean);
    }

    @Override
    public Map<String, Object> findHisRecipeDetail(FindHisRecipeDetailVO request) {
        // 1获取his数据
        PatientDTO patientDTO = patientService.getPatientBeanByMpiId(request.getMpiId());
        if (null == patientDTO) {
            throw new DAOException(609, "患者信息不存在");
        }
        HisResponseTO<List<QueryHisRecipResTO>> hisRecipeInfos=recipeHisService.queryData(request.getOrganId(),patientDTO,180,1,null);

        try {
            // 2更新数据校验
            offlineToOnlineService.hisRecipeInfoCheck(hisRecipeInfos.getData(), patientDTO);
        } catch (Exception e) {
            LOGGER.error("queryHisRecipeInfo hisRecipeInfoCheck error ", e);
        }
        List<HisRecipe> recipes=new ArrayList<>();
        try {
            // 3保存数据到cdr_his_recipe相关表（cdr_his_recipe、cdr_his_recipeExt、cdr_his_recipedetail）
            recipes=offlineToOnlineService.saveHisRecipeInfo(hisRecipeInfos, patientDTO, 1);
        } catch (Exception e) {
            LOGGER.error("queryHisRecipeInfo saveHisRecipeInfo error ", e);
        }

        // 4.保存数据到cdr_recipe相关表（cdr_recipe、cdr_recipeext、cdr_recipeDetail）
        Integer recipeId=offlineToOnlineService.saveRecipeInfo(recipes.get(0).getHisRecipeID());
        // 5.通过cdrHisRecipeId返回数据详情
        return offlineToOnlineService.getHisRecipeDetailByHisRecipeIdAndRecipeId(request.getHisRecipeId(),recipeId);
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
