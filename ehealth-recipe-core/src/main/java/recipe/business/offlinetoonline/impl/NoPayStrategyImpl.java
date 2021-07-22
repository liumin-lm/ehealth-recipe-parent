package recipe.business.offlinetoonline.impl;

import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.recipe.mode.QueryHisRecipResTO;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.recipe.dto.GroupRecipeConf;
import com.ngari.recipe.entity.HisRecipe;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.offlinetoonline.model.FindHisRecipeDetailReqVO;
import com.ngari.recipe.offlinetoonline.model.FindHisRecipeDetailResVO;
import com.ngari.recipe.offlinetoonline.model.FindHisRecipeListVO;
import com.ngari.recipe.offlinetoonline.model.SettleForOfflineToOnlineVO;
import com.ngari.recipe.recipe.model.GiveModeButtonBean;
import com.ngari.recipe.recipe.model.HisRecipeVO;
import com.ngari.recipe.recipe.model.MergeRecipeVO;
import ctd.persistence.exception.DAOException;
import ctd.util.JSONUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.business.offlinetoonline.IOfflineToOnlineStrategy;
import recipe.enumerate.status.OfflineToOnlineEnum;
import recipe.manager.GroupRecipeManager;
import recipe.manager.HisRecipeManager;
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
    GroupRecipeManager groupRecipeManager;



    @Override
    public List<MergeRecipeVO> findHisRecipeList(HisResponseTO<List<QueryHisRecipResTO>> hisRecipeInfos, PatientDTO patientDTO, FindHisRecipeListVO request) {
        LOGGER.info("findHisRecipeList hisRecipeInfos:{},patientDTO:{},request:{}",JSONUtils.toString(hisRecipeInfos),JSONUtils.toString(patientDTO),JSONUtils.toString(request));
        // 2、将his数据转换成recipe对象
        List<HisRecipeVO> noPayFeeHisRecipeVO = covertToHisRecipeVoObject(hisRecipeInfos, patientDTO);
        // 3、包装成前端所需线下处方列表对象
        GiveModeButtonBean giveModeButtonBean=getGiveModeButtonBean(request.getOrganId());
        List<MergeRecipeVO> res=findOnReadyHisRecipeList(noPayFeeHisRecipeVO, giveModeButtonBean);
        LOGGER.info("findHisRecipeList res:{}",JSONUtils.toString(res));
        return res;
    }

    @Override
    public FindHisRecipeDetailResVO findHisRecipeDetail(FindHisRecipeDetailReqVO request) {
        LOGGER.info("findHisRecipeDetail request:{}",JSONUtils.toString(request));
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
        LOGGER.info("findHisRecipeDetail res:{}",JSONUtils.toString(res));
        return res;
    }

    @Override
    public List<RecipeGiveModeButtonRes> settleForOfflineToOnline(SettleForOfflineToOnlineVO request) {
        LOGGER.info("NoPayServiceImpl settleForOfflineToOnline request = {}",  JSONUtils.toString(request));
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

    /**
     * 线下待处理处方转换成前端列表所需对象
     *
     * @param responseTO    his返回线下处方
     * @param patientDTO    患者信息
     * @return
     */
    public List<HisRecipeVO> covertToHisRecipeVoObject(HisResponseTO<List<QueryHisRecipResTO>> responseTO, PatientDTO patientDTO) {
        LOGGER.info("covertHisRecipeObject param responseTO:{},patientDTO:{}" + JSONUtils.toString(responseTO),JSONUtils.toString(patientDTO));
        List<HisRecipeVO> hisRecipeVOs = new ArrayList<>();
        if (responseTO == null) {
            return hisRecipeVOs;
        }
        List<QueryHisRecipResTO> queryHisRecipResTOList = responseTO.getData();
        if (CollectionUtils.isEmpty(queryHisRecipResTOList)) {
            return hisRecipeVOs;
        }
        LOGGER.info("covertHisRecipeObject queryHisRecipResTOList:" + JSONUtils.toString(queryHisRecipResTOList));
        for (QueryHisRecipResTO queryHisRecipResTO : queryHisRecipResTOList) {
            HisRecipe hisRecipeDb = hisRecipeManager.getHisRecipeBMpiIdyRecipeCodeAndClinicOrgan(
                    patientDTO.getMpiId(), queryHisRecipResTO.getClinicOrgan(), queryHisRecipResTO.getRecipeCode());
            //移除已在平台处理的处方单
            if (null != hisRecipeDb && new Integer("2").equals(hisRecipeDb.getStatus())) {
                continue;
            }
            //移除正在进行中的处方单
            Recipe recipe = recipeManager.getByRecipeCodeAndClinicOrgan(queryHisRecipResTO.getRecipeCode(), queryHisRecipResTO.getClinicOrgan());
            if (null != recipe && StringUtils.isNotEmpty(recipe.getOrderCode())) {
                continue;
            }

            HisRecipeVO hisRecipeVO =new HisRecipeVO();
            //详情需要
            hisRecipeVO.setMpiId(patientDTO.getMpiId());
            hisRecipeVO.setClinicOrgan(queryHisRecipResTO.getClinicOrgan());
            //列表显示需要
            hisRecipeVO.setPatientName(patientDTO.getPatientName());
            hisRecipeVO.setCreateDate(queryHisRecipResTO.getCreateDate());
            hisRecipeVO.setRecipeCode(queryHisRecipResTO.getRecipeCode());
            if (!StringUtils.isEmpty(queryHisRecipResTO.getDiseaseName())) {
                hisRecipeVO.setDiseaseName(queryHisRecipResTO.getDiseaseName());
            } else {
                hisRecipeVO.setDiseaseName("无");
            }
            hisRecipeVO.setDisease(queryHisRecipResTO.getDisease());
            hisRecipeVO.setDoctorName(queryHisRecipResTO.getDoctorName());
            hisRecipeVO.setDepartName(queryHisRecipResTO.getDepartName());
            setOtherInfo(hisRecipeVO, patientDTO.getMpiId(), queryHisRecipResTO.getRecipeCode(), queryHisRecipResTO.getClinicOrgan());
            //其它需要
            hisRecipeVO.setStatus(queryHisRecipResTO.getStatus());
            hisRecipeVO.setRecipeMode("ngarihealth");
            hisRecipeVOs.add(hisRecipeVO);
        }
        LOGGER.info("covertHisRecipeObject response hisRecipeVOs:{}" , JSONUtils.toString(hisRecipeVOs));
        return hisRecipeVOs;
    }

    /**
     * 获取待处理的线下的处方单
     *
     * @param request his的处方单集合
     * @return 前端需要的处方单集合
     */
    public List<MergeRecipeVO> findOnReadyHisRecipeList(List<HisRecipeVO> request, GiveModeButtonBean giveModeButtonBean) {
        LOGGER.info("offlineToOnlineService findOnReadyHisRecipe request:{}", JSONUtils.toString(request));
        //查询线下待缴费处方
        List<MergeRecipeVO> result = new ArrayList<>();
        GroupRecipeConf groupRecipeConf = groupRecipeManager.getMergeRecipeSetting();
        Boolean mergeRecipeFlag = groupRecipeConf.getMergeRecipeFlag();
        String mergeRecipeWayAfter = groupRecipeConf.getMergeRecipeWayAfter();
        if (mergeRecipeFlag) {
            //开启合并支付开关
            if ("e.registerId".equals(mergeRecipeWayAfter)) {
                //表示根据挂号序号分组
                Map<String, List<HisRecipeVO>> registerIdRelation = request.stream().collect(Collectors.groupingBy(HisRecipeVO::getRegisteredId));
                for (Map.Entry<String, List<HisRecipeVO>> entry : registerIdRelation.entrySet()) {
                    List<HisRecipeVO> recipes = entry.getValue();
                    if (StringUtils.isEmpty(entry.getKey())) {
                        //表示挂号序号为空,不能进行处方合并
                        covertMergeRecipeVO(null,false,null,null,giveModeButtonBean.getButtonSkipType(),recipes,result);
                    } else {
                        //可以进行合并支付
                        covertMergeRecipeVO(recipes.get(0).getRegisteredId(),true,mergeRecipeWayAfter,recipes.get(0).getHisRecipeID(),giveModeButtonBean.getButtonSkipType(),recipes,result);
                    }
                }
            } else {
                //表示根据相同挂号序号下的同一病种分组
                Map<String, Map<String, List<HisRecipeVO>>> map = request.stream().collect(Collectors.groupingBy(HisRecipeVO::getRegisteredId, Collectors.groupingBy(HisRecipeVO::getChronicDiseaseName)));
                for (Map.Entry<String, Map<String, List<HisRecipeVO>>> entry : map.entrySet()) {
                    //挂号序号为空表示不能进行处方合并
                    if (StringUtils.isEmpty(entry.getKey())) {
                        Map<String, List<HisRecipeVO>> recipeMap = entry.getValue();
                        for (Map.Entry<String, List<HisRecipeVO>> recipeEntry : recipeMap.entrySet()) {
                            List<HisRecipeVO> recipes = recipeEntry.getValue();
                            covertMergeRecipeVO(null,false,null,null,giveModeButtonBean.getButtonSkipType(),recipes,result);
                        }
                    } else {
                        //表示挂号序号不为空,需要根据当前病种
                        Map<String, List<HisRecipeVO>> recipeMap = entry.getValue();
                        for (Map.Entry<String, List<HisRecipeVO>> recipeEntry : recipeMap.entrySet()) {
                            //如果病种为空不能进行合并
                            List<HisRecipeVO> recipes = recipeEntry.getValue();
                            if (StringUtils.isEmpty(recipeEntry.getKey())) {
                                covertMergeRecipeVO(null,false,null,null,giveModeButtonBean.getButtonSkipType(),recipes,result);
                            } else {
                                //可以进行合并支付
                                covertMergeRecipeVO(recipes.get(0).getChronicDiseaseName(),true,mergeRecipeWayAfter,recipes.get(0).getHisRecipeID(),giveModeButtonBean.getButtonSkipType(),recipes,result);
                            }
                        }
                    }
                }
            }
        } else {
            //不开启合并支付开关
            covertMergeRecipeVO(null,false,null,null,giveModeButtonBean.getButtonSkipType(),request,result);
        }
        LOGGER.info("offlineToOnlineService findOnReadyHisRecipe result:{}", JSONUtils.toString(result));
        return result;
    }

}
