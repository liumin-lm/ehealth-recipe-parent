package recipe.atop.doctor;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.commonrecipe.model.CommonDTO;
import com.ngari.recipe.commonrecipe.model.CommonRecipeDTO;
import com.ngari.recipe.dto.HisRecipeDTO;
import com.ngari.recipe.dto.HisRecipeInfoDTO;
import com.ngari.recipe.entity.CommonRecipe;
import com.ngari.recipe.entity.OrganDrugList;
import com.ngari.recipe.recipe.model.HisRecipeBean;
import com.ngari.recipe.recipe.model.HisRecipeDetailBean;
import com.ngari.recipe.recipe.model.RecipeBean;
import com.ngari.recipe.recipe.model.RecipeExtendBean;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.atop.BaseAtop;
import recipe.core.api.IDrugBusinessService;
import recipe.core.api.doctor.ICommonRecipeBusinessService;
import recipe.enumerate.type.RecipeDrugFormTypeEnum;
import recipe.enumerate.type.RecipeTypeEnum;
import recipe.util.ValidateUtil;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 常用方服务入口类
 *
 * @author fuzi
 */
@RpcBean("commonRecipeAtop")
public class CommonRecipeDoctorAtop extends BaseAtop {
    @Autowired
    private ICommonRecipeBusinessService commonRecipeService;
    @Autowired
    private IDrugBusinessService drugBusinessService;

    /**
     * 获取常用方列表
     *
     * @param recipeType 处方类型
     * @param doctorId   医生id
     * @param organId    机构id
     * @param start      开始
     * @param limit      分页条数
     * @return ResultBean
     */
    @RpcService
    @Deprecated
    public List<CommonRecipeDTO> commonRecipeList(Integer organId, Integer doctorId, List<Integer> recipeType, int start, int limit) {
        validateAtop(doctorId, organId);
        List<CommonDTO> resultNew = commonRecipeService.commonRecipeList(organId, doctorId, recipeType, start, limit);
        logger.info("CommonRecipeAtop commonRecipeList resultNew = {}", JSON.toJSONString(resultNew));
        List<CommonRecipeDTO> result = new LinkedList<>();
        resultNew.forEach(a -> {
            CommonRecipeDTO commonRecipeDTO = a.getCommonRecipeDTO();
            commonRecipeDTO.setCommonRecipeExt(a.getCommonRecipeExt());
            commonRecipeDTO.setCommonDrugList(a.getCommonRecipeDrugList());
            result.add(commonRecipeDTO);
        });
        return result;
    }

    /**
     * 获取常用方列表
     *
     * @param recipeType 处方类型
     * @param doctorId   医生id
     * @param organId    机构id
     * @param start      开始
     * @param limit      分页条数
     * @return 常用方列表
     */
    @RpcService
    @Deprecated
    public List<CommonDTO> commonRecipeListV1(Integer organId, Integer doctorId, List<Integer> recipeType, int start, int limit) {
        validateAtop(doctorId, organId);
        return commonRecipeService.commonRecipeList(organId, doctorId, recipeType, start, limit);
    }

    /**
     * 获取常用方列表
     *
     * @param recipeType 处方类型
     * @param doctorId   医生id
     * @param organId    机构id
     * @param start      开始
     * @param limit      分页条数
     * @return 常用方列表
     */
    @RpcService
    public List<CommonRecipeDTO> commonRecipeListV2(Integer organId, Integer doctorId, List<Integer> recipeType, int start, int limit) {
        validateAtop(doctorId, organId);
        List<CommonRecipe> list = commonRecipeService.commonRecipeListV2(organId, doctorId, recipeType, start, limit);
        if (CollectionUtils.isEmpty(list)) {
            return Collections.emptyList();
        }
        return ObjectCopyUtils.convert(list, CommonRecipeDTO.class);
    }

    /**
     * 获取常用方详情
     *
     * @param commonRecipeId 常用方id
     * @return
     */
    @RpcService
    public CommonDTO commonRecipeInfo(Integer commonRecipeId) {
        validateAtop(commonRecipeId);
        return commonRecipeService.commonRecipeInfo(commonRecipeId);
    }


    /**
     * 新增或更新常用方  选好药品后将药品加入到常用处方
     *
     * @param common 常用方
     */
    @RpcService
    public void saveCommonRecipe(CommonDTO common) {
        validateAtop("常用方必填参数为空", common, common.getCommonRecipeDTO(), common.getCommonRecipeDrugList());
        CommonRecipeDTO commonRecipe = common.getCommonRecipeDTO();
        validateAtop("常用方必填参数为空", commonRecipe.getDoctorId(), commonRecipe.getRecipeType(), commonRecipe.getCommonRecipeType(), commonRecipe.getCommonRecipeName());
        commonRecipeService.saveCommonRecipe(common);
    }
//
//    /**
//     * 刷新常用方校验状态
//     *
//     * @param drugList 常用方药品
//     */
//    @RpcService
//    public void refreshCommonValidateStatus(List<CommonRecipeDrugDTO> drugList) {
//        validateAtop(drugList);
//        drugList.forEach(a -> validateAtop(a.getCommonRecipeId(), a.getId()));
//        commonRecipeService.refreshCommonValidateStatus(drugList);
//    }

    /**
     * 删除常用方
     *
     * @param commonRecipeId 常用方Id
     */
    @RpcService
    public void deleteCommonRecipe(Integer commonRecipeId) {
        validateAtop(commonRecipeId);
        commonRecipeService.deleteCommonRecipe(commonRecipeId);

    }

    /**
     * 查询线下常用方
     * todo 新方法：offlineCommonV1
     * 产品流程上放弃使用
     *
     * @param organId  机构id
     * @param doctorId 医生id
     * @return 线下常用方数据集合
     */
    @RpcService
    @Deprecated
    public List<CommonDTO> offlineCommon(Integer organId, Integer doctorId) {
        validateAtop(doctorId, organId);
        return commonRecipeService.offlineCommon(organId, doctorId);

    }

    /**
     * 添加线下常用方到线上
     * 产品流程上放弃使用
     *
     * @param commonList 线下常用方数据集合
     * @return boolean
     */
    @RpcService
    @Deprecated
    public List<String> batchAddOfflineCommon(Integer organId, List<CommonDTO> commonList) {
        validateAtop(organId, commonList);
        return commonRecipeService.addOfflineCommon(organId, commonList);
    }

    /**
     * 获取线下常用方列表(协定方)
     *
     * @param recipeBean 查询入参对象
     * @return
     */
    @RpcService
    public List<CommonRecipeDTO> offlineCommonList(RecipeBean recipeBean) {
        validateAtop(recipeBean, recipeBean.getDoctor(), recipeBean.getClinicOrgan());
        return commonRecipeService.offlineCommonList(recipeBean);
    }

    /**
     * 获取线下常用方详情(协定方)
     *
     * @param commonRecipe 常用方头
     * @return
     */
    @RpcService
    public HisRecipeBean offlineCommonV1(CommonRecipeDTO commonRecipe) {
        validateAtop(commonRecipe, commonRecipe.getOrganId(), commonRecipe.getCommonRecipeCode());
        HisRecipeDTO hisRecipeDTO = commonRecipeService.offlineCommonV1(commonRecipe.getOrganId(), commonRecipe.getCommonRecipeCode());

        HisRecipeInfoDTO hisRecipeInfo = hisRecipeDTO.getHisRecipeInfo();
        HisRecipeBean recipeBean = ObjectCopyUtils.convert(hisRecipeInfo, HisRecipeBean.class);
        if (StringUtils.isNotEmpty(hisRecipeInfo.getSignTime())) {
            recipeBean.setSignDate(hisRecipeInfo.getSignTime());
            recipeBean.setCreateDate(Timestamp.valueOf(hisRecipeInfo.getSignTime()));
        }
        recipeBean.setOrganDiseaseName(hisRecipeInfo.getDiseaseName());
        recipeBean.setDepartText(hisRecipeInfo.getDepartName());
        recipeBean.setClinicOrgan(commonRecipe.getOrganId());
        recipeBean.setRecipeExtend(ObjectCopyUtils.convert(hisRecipeDTO.getHisRecipeExtDTO(), RecipeExtendBean.class));
        if (StringUtils.isEmpty(recipeBean.getRecipeType()) && !ValidateUtil.integerIsEmpty(commonRecipe.getRecipeType())) {
            recipeBean.setRecipeType(commonRecipe.getRecipeType().toString());
        }
        List<HisRecipeDetailBean> hisRecipeDetailBeans = Lists.newArrayList();
        hisRecipeDTO.getHisRecipeDetail().forEach(a -> {
            HisRecipeDetailBean detailBean = ObjectCopyUtils.convert(a, HisRecipeDetailBean.class);
            detailBean.setDrugUnit(a.getDrugUnit());
            detailBean.setUsingRateText(a.getUsingRate());
            detailBean.setUsePathwaysText(a.getUsePathWays());
            detailBean.setUseDays(null == a.getUseDays() ? null : a.getUseDays().toString());
            detailBean.setUseTotalDose(null != a.getUseTotalDose() ? a.getUseTotalDose().doubleValue() : 0.0);
            if (StringUtils.isNotEmpty(commonRecipe.getPharmacyCode()) && StringUtils.isEmpty(a.getPharmacyCode())) {
                detailBean.setPharmacyCode(commonRecipe.getPharmacyCode());
            }
            if (StringUtils.isNotEmpty(commonRecipe.getOrganUsePathways()) && StringUtils.isEmpty(a.getUsePathwaysCode())) {
                a.setUsePathwaysCode(commonRecipe.getOrganUsePathways());
            }
            detailBean.setUsingRate(a.getUsingRateCode());
            detailBean.setUsePathways(a.getUsePathwaysCode());
            hisRecipeDetailBeans.add(detailBean);
        });
        if (RecipeTypeEnum.RECIPETYPE_TCM.getType().toString().equals(recipeBean.getRecipeType())) {
            List<String> drugCodeList = hisRecipeDetailBeans.stream().filter(hisRecipeDetailBean -> StringUtils.isNotEmpty(hisRecipeDetailBean.getDrugCode())).map(HisRecipeDetailBean::getDrugCode).collect(Collectors.toList());
            List<OrganDrugList> organDrugLists = drugBusinessService.organDrugList(commonRecipe.getOrganId(), drugCodeList);
            List<String> organDrugFormLists = organDrugLists.stream().filter(organDrugList -> StringUtils.isNotEmpty(organDrugList.getDrugForm())).map(OrganDrugList::getDrugForm).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(organDrugFormLists)) {
                recipeBean.setRecipeDrugForm(RecipeDrugFormTypeEnum.TCM_DECOCTION_PIECES.getType());
            } else {
                recipeBean.setRecipeDrugForm(RecipeDrugFormTypeEnum.getDrugFormType(organDrugFormLists.get(0)));
            }
        }
        recipeBean.setDetailData(hisRecipeDetailBeans);
        return recipeBean;
    }

}
