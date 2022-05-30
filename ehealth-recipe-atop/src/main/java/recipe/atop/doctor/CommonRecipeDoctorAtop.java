package recipe.atop.doctor;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.commonrecipe.model.CommonDTO;
import com.ngari.recipe.commonrecipe.model.CommonRecipeDTO;
import com.ngari.recipe.dto.HisRecipeDTO;
import com.ngari.recipe.dto.HisRecipeInfoDTO;
import com.ngari.recipe.recipe.model.HisRecipeBean;
import com.ngari.recipe.recipe.model.HisRecipeDetailBean;
import com.ngari.recipe.recipe.model.RecipeBean;
import com.ngari.recipe.recipe.model.RecipeExtendBean;
import ctd.persistence.exception.DAOException;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.atop.BaseAtop;
import recipe.constant.ErrorCode;
import recipe.core.api.doctor.ICommonRecipeBusinessService;

import java.sql.Timestamp;
import java.util.LinkedList;
import java.util.List;

/**
 * 常用方服务入口类
 *
 * @author fuzi
 */
@RpcBean("commonRecipeAtop")
public class CommonRecipeDoctorAtop extends BaseAtop {
    @Autowired
    private ICommonRecipeBusinessService commonRecipeService;

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
        logger.info("CommonRecipeAtop commonRecipeList organId = {},doctorId = {},recipeType = {},start = {},limit = {}"
                , organId, doctorId, recipeType, start, limit);
        validateAtop(doctorId, organId);
        try {
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
        } catch (DAOException e1) {
            logger.warn("CommonRecipeAtop commonRecipeList error", e1);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e1.getMessage());
        } catch (Exception e) {
            logger.error("CommonRecipeAtop commonRecipeList error", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        }
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
    public List<CommonDTO> commonRecipeListV1(Integer organId, Integer doctorId, List<Integer> recipeType, int start, int limit) {
        logger.info("CommonRecipeAtop commonRecipeListV1 organId = {},doctorId = {},recipeType = {},start = {},limit = {}"
                , organId, doctorId, recipeType, start, limit);
        validateAtop(doctorId, organId);
        return commonRecipeService.commonRecipeList(organId, doctorId, recipeType, start, limit);
    }

    /**
     * 新增或更新常用方  选好药品后将药品加入到常用处方
     *
     * @param common 常用方
     */
    @RpcService
    public void saveCommonRecipe(CommonDTO common) {
        logger.info("CommonRecipeAtop addCommonRecipe common = {}", JSON.toJSONString(common));
        validateAtop("常用方必填参数为空", common, common.getCommonRecipeDTO(), common.getCommonRecipeDrugList());
        CommonRecipeDTO commonRecipe = common.getCommonRecipeDTO();
        validateAtop("常用方必填参数为空", commonRecipe.getDoctorId(), commonRecipe.getRecipeType(), commonRecipe.getCommonRecipeType(), commonRecipe.getCommonRecipeName());

        try {
            commonRecipeService.saveCommonRecipe(common);
        } catch (DAOException e1) {
            logger.warn("CommonRecipeAtop addCommonRecipe error", e1);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e1.getMessage());
        } catch (Exception e) {
            logger.error("CommonRecipeAtop addCommonRecipe error", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        }
    }

    /**
     * 删除常用方
     *
     * @param commonRecipeId 常用方Id
     */
    @RpcService
    public void deleteCommonRecipe(Integer commonRecipeId) {
        logger.info("CommonRecipeAtop deleteCommonRecipe commonRecipeId = {}", commonRecipeId);
        validateAtop(commonRecipeId);
        try {
            commonRecipeService.deleteCommonRecipe(commonRecipeId);
        } catch (DAOException e1) {
            logger.warn("CommonRecipeAtop deleteCommonRecipe error", e1);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e1.getMessage());
        } catch (Exception e) {
            logger.error("CommonRecipeAtop deleteCommonRecipe error", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        }
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
        List<CommonDTO> result = commonRecipeService.offlineCommon(organId, doctorId);
        logger.info("CommonRecipeAtop offlineCommon result = {}", JSON.toJSONString(result));
        return result;

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
        logger.info("CommonRecipeAtop addOfflineCommon commonList = {}", JSON.toJSONString(commonList));
        validateAtop(organId, commonList);
        try {
            List<String> result = commonRecipeService.addOfflineCommon(organId, commonList);
            logger.info("CommonRecipeAtop addOfflineCommon result = {}", result);
            return result;
        } catch (DAOException e1) {
            logger.warn("CommonRecipeAtop addOfflineCommon error", e1);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e1.getMessage());
        } catch (Exception e) {
            logger.error("CommonRecipeAtop addOfflineCommon error", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        }
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
        recipeBean.setSignDate(hisRecipeInfo.getSignTime());
        recipeBean.setCreateDate(Timestamp.valueOf(hisRecipeInfo.getSignTime()));
        recipeBean.setOrganDiseaseName(hisRecipeInfo.getDiseaseName());
        recipeBean.setDepartText(hisRecipeInfo.getDepartName());
        recipeBean.setClinicOrgan(commonRecipe.getOrganId());
        recipeBean.setRecipeExtend(ObjectCopyUtils.convert(hisRecipeDTO.getHisRecipeExtDTO(), RecipeExtendBean.class));

        List<HisRecipeDetailBean> hisRecipeDetailBeans = Lists.newArrayList();
        hisRecipeDTO.getHisRecipeDetail().forEach(a -> {
            HisRecipeDetailBean detailBean = ObjectCopyUtils.convert(a, HisRecipeDetailBean.class);
            detailBean.setDrugUnit(a.getUnit());
            detailBean.setUsingRateText(a.getUsingRate());
            detailBean.setUsePathwaysText(a.getUsePathWays());
            detailBean.setUseDays(a.getDays());
            detailBean.setUseTotalDose(a.getAmount());
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
        recipeBean.setDetailData(hisRecipeDetailBeans);
        return recipeBean;
    }

}
