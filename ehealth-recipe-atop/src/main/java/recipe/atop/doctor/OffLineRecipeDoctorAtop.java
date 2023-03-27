package recipe.atop.doctor;

import com.google.common.collect.Lists;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.dto.HisRecipeDTO;
import com.ngari.recipe.dto.HisRecipeInfoDTO;
import com.ngari.recipe.entity.OrganDrugList;
import com.ngari.recipe.recipe.model.*;
import com.ngari.recipe.vo.OffLineRecipeDetailVO;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.atop.BaseAtop;
import recipe.core.api.IDrugBusinessService;
import recipe.core.api.patient.IOfflineRecipeBusinessService;
import recipe.enumerate.type.RecipeDrugFormTypeEnum;
import recipe.enumerate.type.RecipeTypeEnum;

import java.sql.Timestamp;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 线下处方服务入口类
 *
 * @date 2021/8/06
 */
@RpcBean("offLineRecipeAtop")
public class OffLineRecipeDoctorAtop extends BaseAtop {
    @Autowired
    private IOfflineRecipeBusinessService offlineRecipeBusinessService;
    @Autowired
    private IDrugBusinessService drugBusinessService;

    /**
     * 获取线下处方详情
     * new method getHisRecipeDetail
     * @param mpiId       患者ID
     * @param clinicOrgan 机构ID
     * @param recipeCode  处方号码
     * @date 2021/8/06
     */
    @RpcService
    @Deprecated
    public OffLineRecipeDetailVO getOffLineRecipeDetails(String mpiId, Integer clinicOrgan, String recipeCode) {
        validateAtop(mpiId, clinicOrgan, recipeCode);
        return offlineRecipeBusinessService.getHisRecipeDetail(mpiId, clinicOrgan, recipeCode,null);
    }

    @RpcService
    public OffLineRecipeDetailVO getHisRecipeDetail(String mpiId, Integer clinicOrgan, String recipeCode,String createDate) {
        validateAtop(mpiId, clinicOrgan, recipeCode);
        return offlineRecipeBusinessService.getHisRecipeDetail(mpiId, clinicOrgan, recipeCode ,createDate);
    }


    /**
     * 根据处方code 获取线下处方详情
     *
     * @param recipe        患者ID
     * @param createDate    处方时间
     * @param recipeDetails 机构ID
     * @date
     */
    @RpcService
    public HisRecipeBean getOffLineRecipeDetailsV1(RecipeBean recipe, String createDate, List<RecipeDetailBean> recipeDetails) {
        validateAtop(recipe, recipe.getClinicOrgan(), recipe.getRecipeCode());
        HisRecipeDTO hisRecipeDTO = offlineRecipeBusinessService.getOffLineRecipeDetailsV1(recipe.getClinicOrgan(), recipe.getRecipeCode(), createDate);
        HisRecipeInfoDTO hisRecipeInfo = hisRecipeDTO.getHisRecipeInfo();
        HisRecipeBean recipeBean = ObjectCopyUtils.convert(hisRecipeInfo, HisRecipeBean.class);
        if (StringUtils.isNotEmpty(hisRecipeInfo.getSignTime())) {
            recipeBean.setSignDate(hisRecipeInfo.getSignTime());
            recipeBean.setCreateDate(Timestamp.valueOf(hisRecipeInfo.getSignTime()));
        }
        recipeBean.setOrganDiseaseName(hisRecipeInfo.getDiseaseName());
        recipeBean.setDepartText(hisRecipeInfo.getDepartName());
        recipeBean.setClinicOrgan(recipe.getClinicOrgan());
        recipeBean.setRecipeExtend(ObjectCopyUtils.convert(hisRecipeDTO.getHisRecipeExtDTO(), RecipeExtendBean.class));
        List<HisRecipeDetailBean> hisRecipeDetailBeans = Lists.newArrayList();
        hisRecipeDTO.getHisRecipeDetail().forEach(a -> {
            HisRecipeDetailBean detailBean = ObjectCopyUtils.convert(a, HisRecipeDetailBean.class);
            detailBean.setDrugUnit(a.getDrugUnit());
            detailBean.setUsingRateText(a.getUsingRate());
            detailBean.setUsePathwaysText(a.getUsePathWays());
            detailBean.setUseDays(null == a.getUseDays() ? null : a.getUseDays().toString());
            detailBean.setUseTotalDose(null != a.getUseTotalDose() ? a.getUseTotalDose().doubleValue() : 0.0);
            detailBean.setUsingRate(a.getUsingRateCode());
            detailBean.setUsePathways(a.getUsePathwaysCode());
            hisRecipeDetailBeans.add(detailBean);
        });
        List<String> organDrugCodeList = recipeDetails.stream().map(RecipeDetailBean::getOrganDrugCode).distinct().collect(Collectors.toList());
        List<HisRecipeDetailBean> recipeDetailBeans = hisRecipeDetailBeans.stream().filter(a -> organDrugCodeList.contains(a.getDrugCode())).collect(Collectors.toList());
        recipeBean.setDetailData(recipeDetailBeans);
        if (RecipeTypeEnum.RECIPETYPE_TCM.getType().toString().equals(recipeBean.getRecipeType()) && CollectionUtils.isNotEmpty(recipeDetailBeans)) {
            List<String> drugCodeList = recipeDetailBeans.stream().map(HisRecipeDetailBean::getDrugCode).filter(StringUtils::isNotEmpty).collect(Collectors.toList());
            List<OrganDrugList> organDrugLists = drugBusinessService.organDrugList(recipe.getClinicOrgan(), drugCodeList);
            List<String> organDrugFormLists = organDrugLists.stream().map(OrganDrugList::getDrugForm).filter(StringUtils::isNotEmpty).collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(organDrugFormLists)) {
                recipeBean.setRecipeDrugForm(RecipeDrugFormTypeEnum.getDrugFormType(organDrugFormLists.get(0)));
            }
        }
        return recipeBean;
    }
}