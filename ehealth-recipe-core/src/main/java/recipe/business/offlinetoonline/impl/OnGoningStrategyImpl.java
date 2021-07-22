package recipe.business.offlinetoonline.impl;

import com.google.common.collect.Lists;
import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.recipe.mode.QueryHisRecipResTO;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.recipe.entity.HisRecipe;
import com.ngari.recipe.offlinetoonline.model.FindHisRecipeDetailReqVO;
import com.ngari.recipe.offlinetoonline.model.FindHisRecipeDetailResVO;
import com.ngari.recipe.offlinetoonline.model.FindHisRecipeListVO;
import com.ngari.recipe.offlinetoonline.model.SettleForOfflineToOnlineVO;
import com.ngari.recipe.recipe.model.GiveModeButtonBean;
import com.ngari.recipe.recipe.model.MergeRecipeVO;
import ctd.persistence.exception.DAOException;
import ctd.util.JSONUtils;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.business.offlinetoonline.IOfflineToOnlineStrategy;
import recipe.dao.bean.HisRecipeListBean;
import recipe.enumerate.status.OfflineToOnlineEnum;
import recipe.manager.GroupRecipeManager;
import recipe.manager.HisRecipeManager;
import recipe.vo.patient.RecipeGiveModeButtonRes;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author liumin
 * @Date 2021/6/8上午11:42
 * @Description 线下转线上进行中处方实现类
 */
@Service
public class OnGoningStrategyImpl extends BaseOfflineToOnlineService  implements IOfflineToOnlineStrategy {

    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    @Autowired
    HisRecipeManager hisRecipeManager;

    @Autowired
    GroupRecipeManager groupRecipeManager;

    @Override
    public List<MergeRecipeVO> findHisRecipeList(HisResponseTO<List<QueryHisRecipResTO>> hisRecipeInfos, PatientDTO patientDTO, FindHisRecipeListVO request) {
        LOGGER.info("OnGoningStrategyServiceImpl findHisRecipeList hisRecipeInfos:{}",JSONUtils.toString(hisRecipeInfos));
        // 2、返回进行中的线下处方
        GiveModeButtonBean giveModeButtonBean=getGiveModeButtonBean(request.getOrganId());
        List<MergeRecipeVO> res=findOngoingHisRecipeList(hisRecipeInfos.getData(), patientDTO, giveModeButtonBean, request.getStart(), request.getLimit());
        LOGGER.info("OnGoningStrategyServiceImpl res:{}",JSONUtils.toString(hisRecipeInfos));
        return res;
    }

    @Override
    public FindHisRecipeDetailResVO findHisRecipeDetail(FindHisRecipeDetailReqVO request) {
        LOGGER.info("OnGoningStrategyServiceImpl findHisRecipeDetail request:{}",JSONUtils.toString(request));
        // 跟待处理获取详情一致 先判断数据是否变更 然后返回详情
        // 1获取his数据
        PatientDTO patientDTO = hisRecipeManager.getPatientBeanByMpiId(request.getMpiId());
        if (null == patientDTO) {
            throw new DAOException(609, "患者信息不存在");
        }
        HisResponseTO<List<QueryHisRecipResTO>> hisRecipeInfos= hisRecipeManager.queryData(request.getOrganId(),patientDTO,180,1,request.getRecipeCode());

        try {
            // 2更新数据校验
            hisRecipeManager.hisRecipeInfoCheck(hisRecipeInfos.getData(), patientDTO);
        } catch (Exception e) {
            LOGGER.error("queryHisRecipeInfo hisRecipeInfoCheck error ", e);
        }
        List<HisRecipe> hisRecipes=new ArrayList<>();
        try {
            // 3保存数据到cdr_his_recipe相关表（cdr_his_recipe、cdr_his_recipeExt、cdr_his_recipedetail）
            hisRecipes=hisRecipeManager.saveHisRecipeInfo(hisRecipeInfos, patientDTO, 1);
        } catch (Exception e) {
            LOGGER.error("queryHisRecipeInfo saveHisRecipeInfo error ", e);
        }
        Integer hisRecipeId=hisRecipeManager.attachRecipeId(request.getOrganId(),request.getRecipeCode(),hisRecipes);

        // 4.保存数据到cdr_recipe相关表（cdr_recipe、cdr_recipeext、cdr_recipeDetail）
        Integer recipeId=saveRecipeInfo(hisRecipeId);

        // 5.通过cdrHisRecipeId返回数据详情
        FindHisRecipeDetailResVO res=getHisRecipeDetailByHisRecipeIdAndRecipeId(hisRecipeId,recipeId);
        LOGGER.info("OnGoningStrategyServiceImpl findHisRecipeDetail res:{}",JSONUtils.toString(res));
        return res;

    }

    @Override
    public List<RecipeGiveModeButtonRes> settleForOfflineToOnline(SettleForOfflineToOnlineVO request) {
       return null;
    }

    @Override
    public String getHandlerMode() {
        return OfflineToOnlineEnum.OFFLINE_TO_ONLINE_ONGOING.getName();
    }

    /**
     * @param data 当前获取HIS的处方单集合
     * @return 前端需要展示的进行中的处方单集合, 先获取进行中的处方返回给前端展示, 然后对处方数据进行校验, 处方发生
     * 变更需要删除处方,当患者点击处方列表时如果订单已删除,会弹框提示"该处方单信息已变更，请退出重新获取处方信息"
     */
    public List<MergeRecipeVO> findOngoingHisRecipeList(List<QueryHisRecipResTO> data, PatientDTO patientDTO, GiveModeButtonBean giveModeButtonBean, Integer start, Integer limit) {
        LOGGER.info("offlineToOnlineService findOngoingHisRecipe request:{}", ctd.util.JSONUtils.toString(data));
        List<MergeRecipeVO> result = Lists.newArrayList();
        //查询所有进行中的线下处方
        List<HisRecipeListBean> hisRecipeListBeans = findOngoingHisRecipeListByMPIId(data.get(0).getClinicOrgan(),patientDTO.getMpiId(), start, limit);
        if (CollectionUtils.isEmpty(hisRecipeListBeans)) {
            return result;
        }
        //返回前端所需数据
        result = listShow(hisRecipeListBeans,hisRecipeListBeans.get(0).getClinicOrgan(),patientDTO.getMpiId(),giveModeButtonBean,start,limit);
        try {
            //更新数据校验
            hisRecipeManager.hisRecipeInfoCheck(data, patientDTO);
        } catch (Exception e) {
            LOGGER.error("queryHisRecipeInfo hisRecipeInfoCheck error ", e);
        }
        LOGGER.info("offlineToOnlineService findOngoingHisRecipe result:{}", JSONUtils.toString(result));
        return result;
    }
}
