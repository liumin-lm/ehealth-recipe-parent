package recipe.factory.offlinetoonline.impl;

import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.recipe.mode.QueryHisRecipResTO;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.recipe.dto.GiveModeButtonDTO;
import com.ngari.recipe.entity.HisRecipe;
import com.ngari.recipe.offlinetoonline.model.*;
import com.ngari.recipe.recipe.model.MergeRecipeVO;
import com.ngari.recipe.recipe.model.RecipeBean;
import ctd.persistence.exception.DAOException;
import ctd.util.JSONUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.enumerate.status.OfflineToOnlineEnum;
import recipe.factory.offlinetoonline.IOfflineToOnlineStrategy;
import recipe.manager.HisRecipeManager;
import recipe.vo.patient.RecipeGiveModeButtonRes;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

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
    public void offlineToOnlineForRecipe(FindHisRecipeDetailReqVO request) {

    }



    @Override
    public List<OfflineToOnlineResVO> batchOfflineToOnline(BatchOfflineToOnlineReqVO request) {
        return null;
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
        FindHisRecipeDetailResVO res = getHisRecipeDetailByHisRecipeIdAndRecipeId(request.getHisRecipeId(), recipeId,request);
        LOGGER.info("AlreadyPayStrategyServiceImpl findHisRecipeDetail res:{}", JSONUtils.toString(res));
        return res;
    }

    @Override
    public List<RecipeGiveModeButtonRes> settleForOfflineToOnline(SettleForOfflineToOnlineVO request) {
        return null;
    }

    @Override
    public OfflineToOnlineResVO offlineToOnline(OfflineToOnlineReqVO request) {
        LOGGER.info("offlineToOnlineForRecipe request:{}", JSONUtils.toString(request));
        OfflineToOnlineResVO res=new OfflineToOnlineResVO();
        // 1、获取his数据
        PatientDTO patientDTO = hisRecipeManager.getPatientBeanByMpiId(request.getMpiid());
        if (null == patientDTO) {
            throw new DAOException(609, "患者信息不存在");
        }
        patientDTO.setCardId(StringUtils.isNotEmpty(request.getCardId()) ? request.getCardId() : patientDTO.getCardId());
        HisResponseTO<List<QueryHisRecipResTO>> hisRecipeInfos = hisRecipeManager.queryHisRecipeData(request.getOrganId(), patientDTO, null, OfflineToOnlineEnum.getOfflineToOnlineEnum(request.getProcessState()).getType(), null,null,null);

        //2 更新数据校验
        hisRecipeInfoCheck(hisRecipeInfos.getData(), patientDTO);

        List<HisRecipe> hisRecipes = new ArrayList<>();
        try {
            //3 保存数据到cdr_his_recipe相关表（cdr_his_recipe、cdr_his_recipeExt、cdr_his_recipedetail）
            hisRecipes = saveHisRecipeInfo(hisRecipeInfos, patientDTO, OfflineToOnlineEnum.OFFLINE_TO_ONLINE_ALREADY_PAY.getType());
        } catch (Exception e) {
            LOGGER.error("queryHisRecipeInfo saveHisRecipeInfo error ", e);
        }

        //4 保存数据到cdr_recipe相关表（cdr_recipe、cdr_recipeext、cdr_recipeDetail）
        AtomicReference<Integer> recipeId = new AtomicReference<>();
        hisRecipes.forEach(hisRecipe -> {
            recipeId.set(saveRecipeInfo(hisRecipe.getHisRecipeID()));
        });
        //5 返回出参
        RecipeBean recipeBean=new RecipeBean();
        if(null!=recipeId.get()){
            recipeBean.setRecipeId(recipeId.get());
            res.setRecipe(recipeBean);
        }
        return res;
    }

}
