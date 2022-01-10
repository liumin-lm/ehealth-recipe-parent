package recipe.factory.offlinetoonline.impl;

import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.recipe.mode.QueryHisRecipResTO;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.recipe.dto.GiveModeButtonDTO;
import com.ngari.recipe.dto.GroupRecipeConfDTO;
import com.ngari.recipe.entity.HisRecipe;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.offlinetoonline.model.FindHisRecipeDetailReqVO;
import com.ngari.recipe.offlinetoonline.model.FindHisRecipeDetailResVO;
import com.ngari.recipe.offlinetoonline.model.FindHisRecipeListVO;
import com.ngari.recipe.offlinetoonline.model.SettleForOfflineToOnlineVO;
import com.ngari.recipe.recipe.model.HisRecipeVONoDS;
import com.ngari.recipe.recipe.model.MergeRecipeVO;
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
import recipe.manager.GroupRecipeManager;
import recipe.manager.HisRecipeManager;
import recipe.manager.RecipeManager;
import recipe.vo.patient.RecipeGiveModeButtonRes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Author liumin
 * @Date 2021/1/26 上午11:42
 * @Description 线下转线上待缴费处方实现类
 */
@Service("noPayStrategyImpl")
class NoPayStrategyImpl extends BaseOfflineToOnlineService implements IOfflineToOnlineStrategy {

    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    @Autowired
    HisRecipeManager hisRecipeManager;

    @Autowired
    RecipeManager recipeManager;

    @Autowired
    GroupRecipeManager groupRecipeManager;


    @Override
    public List<MergeRecipeVO> findHisRecipeList(HisResponseTO<List<QueryHisRecipResTO>> hisRecipeInfos, PatientDTO patientDTO, FindHisRecipeListVO request) {
        LOGGER.info("NoPayStrategyImpl findHisRecipeList hisRecipeInfos:{},patientDTO:{},request:{}", JSONUtils.toString(hisRecipeInfos), JSONUtils.toString(patientDTO), JSONUtils.toString(request));
        // 2、将his数据转换成recipe对象
        List<HisRecipeVONoDS> noPayFeeHisRecipeVO = covertToHisRecipeVoObject(hisRecipeInfos, patientDTO);
        // 3、包装成前端所需线下处方列表对象
        GiveModeButtonDTO giveModeButtonBean = getGiveModeButtonBean(request.getOrganId());
        List<MergeRecipeVO> res = findOnReadyHisRecipeList(noPayFeeHisRecipeVO, giveModeButtonBean);
        LOGGER.info("NoPayStrategyImpl findHisRecipeList res:{}", JSONUtils.toString(res));
        return res;
    }

    @Override
    public FindHisRecipeDetailResVO findHisRecipeDetail(FindHisRecipeDetailReqVO request) {
        LOGGER.info("findHisRecipeDetail request:{}", JSONUtils.toString(request));
        // 1获取his数据
        PatientDTO patientDTO = hisRecipeManager.getPatientBeanByMpiId(request.getMpiId());
        if (null == patientDTO) {
            throw new DAOException(609, "患者信息不存在");
        }
        HisResponseTO<List<QueryHisRecipResTO>> hisRecipeInfos = hisRecipeManager.queryData(request.getOrganId(), patientDTO, request.getTimeQuantum(), OfflineToOnlineEnum.OFFLINE_TO_ONLINE_NO_PAY.getType(), request.getRecipeCode());
        if (null == hisRecipeInfos || CollectionUtils.isEmpty(hisRecipeInfos.getData())) {
            return null;
        }
        try {
            // 2更新数据校验
            hisRecipeInfoCheck(hisRecipeInfos.getData(), patientDTO);
        } catch (Exception e) {
            LOGGER.error("queryHisRecipeInfo hisRecipeInfoCheck error ", e);
        }
        List<HisRecipe> hisRecipes = new ArrayList<>();
        try {
            // 3保存数据到cdr_his_recipe相关表（cdr_his_recipe、cdr_his_recipeExt、cdr_his_recipedetail）
            hisRecipes = saveHisRecipeInfo(hisRecipeInfos, patientDTO, OfflineToOnlineEnum.OFFLINE_TO_ONLINE_NO_PAY.getType());
        } catch (Exception e) {
            LOGGER.error("queryHisRecipeInfo saveHisRecipeInfo error ", e);
        }

        // 4.保存数据到cdr_recipe相关表（cdr_recipe、cdr_recipeext、cdr_recipeDetail）
        Integer hisRecipeId = hisRecipeManager.attachRecipeId(request.getOrganId(), request.getRecipeCode(), hisRecipes);
        Integer recipeId = saveRecipeInfo(hisRecipeId);

        // 5.通过cdrHisRecipeId返回数据详情
        FindHisRecipeDetailResVO res = getHisRecipeDetailByHisRecipeIdAndRecipeId(hisRecipeId, recipeId);
        LOGGER.info("findHisRecipeDetail res:{}", JSONUtils.toString(res));
        return res;
    }

    @Override
    public List<RecipeGiveModeButtonRes> settleForOfflineToOnline(SettleForOfflineToOnlineVO request) {
        LOGGER.info("NoPayServiceImpl settleForOfflineToOnline request = {}", JSONUtils.toString(request));
        // 1、线下转线上
        List<Integer> recipeIds = batchSyncRecipeFromHis(request);
        // 2、获取购药按钮
        List<RecipeGiveModeButtonRes> res = getRecipeGiveModeButtonRes(recipeIds);
        LOGGER.info("NoPayServiceImpl settleForOfflineToOnline res:{}", JSONUtils.toString(res));
        return res;
    }


    @Override
    public String getHandlerMode() {
        return OfflineToOnlineEnum.OFFLINE_TO_ONLINE_NO_PAY.getName();
    }

    @Override
    public void offlineToOnlineForRecipe(FindHisRecipeDetailReqVO request) {
        LOGGER.info("offlineToOnlineForRecipe request:{}", JSONUtils.toString(request));
        // 1获取his数据
        PatientDTO patientDTO = hisRecipeManager.getPatientBeanByMpiId(request.getMpiId());
        if (null == patientDTO) {
            throw new DAOException(609, "患者信息不存在");
        }
        HisResponseTO<List<QueryHisRecipResTO>> hisRecipeInfos = hisRecipeManager.queryData(request.getOrganId(), patientDTO, request.getTimeQuantum(), OfflineToOnlineEnum.OFFLINE_TO_ONLINE_NO_PAY.getType(), request.getRecipeCode());
        if (null == hisRecipeInfos || CollectionUtils.isEmpty(hisRecipeInfos.getData())) {
            return;
        }
        try {
            // 2更新数据校验
            hisRecipeInfoCheck(hisRecipeInfos.getData(), patientDTO);
        } catch (Exception e) {
            LOGGER.error("queryHisRecipeInfo hisRecipeInfoCheck error ", e);
        }
        List<HisRecipe> hisRecipes = new ArrayList<>();
        try {
            // 3保存数据到cdr_his_recipe相关表（cdr_his_recipe、cdr_his_recipeExt、cdr_his_recipedetail）
            hisRecipes = saveHisRecipeInfo(hisRecipeInfos, patientDTO, OfflineToOnlineEnum.OFFLINE_TO_ONLINE_NO_PAY.getType());
        } catch (Exception e) {
            LOGGER.error("queryHisRecipeInfo saveHisRecipeInfo error ", e);
        }

        // 4.保存数据到cdr_recipe相关表（cdr_recipe、cdr_recipeext、cdr_recipeDetail）
        hisRecipes.forEach(hisRecipe -> {
            saveRecipeInfo(hisRecipe.getHisRecipeID());
        });
        return;

    }

    /**
     * 线下待处理处方转换成前端列表所需对象
     *
     * @param responseTo his返回线下处方
     * @param patientDTO 患者信息
     * @return
     */
    public List<HisRecipeVONoDS> covertToHisRecipeVoObject(HisResponseTO<List<QueryHisRecipResTO>> responseTo, PatientDTO patientDTO) {
        LOGGER.info("NoPayServiceImpl covertHisRecipeObject param responseTO:{},patientDTO:{}" + JSONUtils.toString(responseTo), JSONUtils.toString(patientDTO));
        List<HisRecipeVONoDS> hisRecipeVos = new ArrayList<>();
        if (responseTo == null) {
            return hisRecipeVos;
        }
        List<QueryHisRecipResTO> queryHisRecipResToList = responseTo.getData();
        if (CollectionUtils.isEmpty(queryHisRecipResToList)) {
            return hisRecipeVos;
        }
        LOGGER.info("NoPayServiceImpl covertHisRecipeObject queryHisRecipResTOList:" + JSONUtils.toString(queryHisRecipResToList));
        for (QueryHisRecipResTO queryHisRecipResTo : queryHisRecipResToList) {
            HisRecipe hisRecipeDb = hisRecipeManager.getHisRecipeBMpiIdyRecipeCodeAndClinicOrgan(
                    patientDTO.getMpiId(), queryHisRecipResTo.getClinicOrgan(), queryHisRecipResTo.getRecipeCode());
            //移除已在平台处理的处方单
            if (null != hisRecipeDb && new Integer("2").equals(hisRecipeDb.getStatus())) {
                continue;
            }
            //移除正在进行中的处方单
            Recipe recipe = recipeManager.getByRecipeCodeAndClinicOrgan(queryHisRecipResTo.getRecipeCode(), queryHisRecipResTo.getClinicOrgan());
            if (null != recipe && StringUtils.isNotEmpty(recipe.getOrderCode()) && recipe.getMpiid().equals(patientDTO.getMpiId())) {
                continue;
            }

            HisRecipeVONoDS hisRecipeVO = new HisRecipeVONoDS();
            //详情需要
            hisRecipeVO.setMpiId(patientDTO.getMpiId());
            hisRecipeVO.setClinicOrgan(queryHisRecipResTo.getClinicOrgan());
            //列表显示需要
            hisRecipeVO.setPatientName(patientDTO.getPatientName());
            hisRecipeVO.setCreateDate(queryHisRecipResTo.getCreateDate());
            hisRecipeVO.setRecipeCode(queryHisRecipResTo.getRecipeCode());
            if (!StringUtils.isEmpty(queryHisRecipResTo.getDiseaseName())) {
                hisRecipeVO.setDiseaseName(queryHisRecipResTo.getDiseaseName());
            } else {
                hisRecipeVO.setDiseaseName("无");
            }
            hisRecipeVO.setDisease(queryHisRecipResTo.getDisease());
            hisRecipeVO.setDoctorName(queryHisRecipResTo.getDoctorName());
            hisRecipeVO.setDepartName(queryHisRecipResTo.getDepartName());
            hisRecipeVO.setOrganDiseaseName(queryHisRecipResTo.getDiseaseName());
            setOtherInfo(hisRecipeVO, patientDTO.getMpiId(), queryHisRecipResTo.getRecipeCode(), queryHisRecipResTo.getClinicOrgan());
            //其它需要
            hisRecipeVO.setStatus(queryHisRecipResTo.getStatus());
            hisRecipeVO.setRecipeMode("ngarihealth");
            hisRecipeVO.setRegisteredId(queryHisRecipResTo.getRegisteredId());
            hisRecipeVos.add(hisRecipeVO);
        }
        LOGGER.info("NoPayServiceImpl covertHisRecipeObject response hisRecipeVOs:{}", JSONUtils.toString(hisRecipeVos));
        return hisRecipeVos;
    }

    /**
     * 获取待处理的线下的处方单
     *
     * @param request his的处方单集合
     * @return 前端需要的处方单集合
     */
    public List<MergeRecipeVO> findOnReadyHisRecipeList(List<HisRecipeVONoDS> request, GiveModeButtonDTO giveModeButtonBean) {
        LOGGER.info("NoPayServiceImpl findOnReadyHisRecipe request:{}", JSONUtils.toString(request));
        //查询线下待缴费处方
        List<MergeRecipeVO> result = new ArrayList<>();
        GroupRecipeConfDTO groupRecipeConfDTO = groupRecipeManager.getMergeRecipeSetting();
        Boolean mergeRecipeFlag = groupRecipeConfDTO.getMergeRecipeFlag();
        String mergeRecipeWayAfter = groupRecipeConfDTO.getMergeRecipeWayAfter();
        if (mergeRecipeFlag) {
            //开启合并支付开关
            if (BY_REGISTERID.equals(mergeRecipeWayAfter)) {
                //表示根据挂号序号分组
                Map<String, List<HisRecipeVONoDS>> registerIdRelation = request.stream().collect(Collectors.groupingBy(HisRecipeVONoDS::getRegisteredId));
                for (Map.Entry<String, List<HisRecipeVONoDS>> entry : registerIdRelation.entrySet()) {
                    List<HisRecipeVONoDS> recipes = entry.getValue();
                    if (StringUtils.isEmpty(entry.getKey())) {
                        //表示挂号序号为空,不能进行处方合并
                        covertMergeRecipeVO(null, false, null, null, giveModeButtonBean.getButtonSkipType(), recipes, result);
                    } else {
                        //可以进行合并支付
                        covertMergeRecipeVO(recipes.get(0).getRegisteredId(), true, mergeRecipeWayAfter, recipes.get(0).getHisRecipeID(), giveModeButtonBean.getButtonSkipType(), recipes, result);
                    }
                }
            } else {
                //表示根据相同挂号序号下的同一病种分组
                Map<String, Map<String, List<HisRecipeVONoDS>>> map = request.stream().collect(Collectors.groupingBy(HisRecipeVONoDS::getRegisteredId, Collectors.groupingBy(HisRecipeVONoDS::getChronicDiseaseName)));
                for (Map.Entry<String, Map<String, List<HisRecipeVONoDS>>> entry : map.entrySet()) {
                    //挂号序号为空表示不能进行处方合并
                    if (StringUtils.isEmpty(entry.getKey())) {
                        Map<String, List<HisRecipeVONoDS>> recipeMap = entry.getValue();
                        for (Map.Entry<String, List<HisRecipeVONoDS>> recipeEntry : recipeMap.entrySet()) {
                            List<HisRecipeVONoDS> recipes = recipeEntry.getValue();
                            covertMergeRecipeVO(null, false, null, null, giveModeButtonBean.getButtonSkipType(), recipes, result);
                        }
                    } else {
                        //表示挂号序号不为空,需要根据当前病种
                        Map<String, List<HisRecipeVONoDS>> recipeMap = entry.getValue();
                        for (Map.Entry<String, List<HisRecipeVONoDS>> recipeEntry : recipeMap.entrySet()) {
                            //如果病种为空不能进行合并
                            List<HisRecipeVONoDS> recipes = recipeEntry.getValue();
                            if (StringUtils.isEmpty(recipeEntry.getKey())) {
                                covertMergeRecipeVO(null, false, null, null, giveModeButtonBean.getButtonSkipType(), recipes, result);
                            } else {
                                //可以进行合并支付
                                covertMergeRecipeVO(recipes.get(0).getChronicDiseaseName(), true, mergeRecipeWayAfter, recipes.get(0).getHisRecipeID(), giveModeButtonBean.getButtonSkipType(), recipes, result);
                            }
                        }
                    }
                }
            }
        } else {
            //不开启合并支付开关
            covertMergeRecipeVO(null, false, null, null, giveModeButtonBean.getButtonSkipType(), request, result);
        }
        LOGGER.info("NoPayServiceImpl findOnReadyHisRecipe result:{}", JSONUtils.toString(result));
        return result;
    }

}
