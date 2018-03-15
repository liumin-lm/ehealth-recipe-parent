package recipe.service;

import com.google.common.collect.Maps;
import com.ngari.recipe.entity.CommonRecipe;
import com.ngari.recipe.entity.CommonRecipeDrug;
import com.ngari.recipe.entity.OrganDrugList;
import ctd.persistence.DAOFactory;
import ctd.persistence.exception.DAOException;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.bussutil.RecipeUtil;
import recipe.constant.ErrorCode;
import recipe.dao.CommonRecipeDAO;
import recipe.dao.CommonRecipeDrugDAO;
import recipe.dao.OrganDrugListDAO;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * 常用方服务
 * Created by jiangtingfeng on 2017/5/23.
 * @author jiangtingfeng
 */
@RpcBean("commonRecipeService")
public class CommonRecipeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommonRecipeService.class);

    /**
     * 新增或更新常用方
     *
     * @param commonRecipe
     * @param drugList
     */
    @RpcService
    public void addCommonRecipe(CommonRecipe commonRecipe, List<CommonRecipeDrug> drugList) {
        CommonRecipeDAO commonRecipeDAO = DAOFactory.getDAO(CommonRecipeDAO.class);
        CommonRecipeDrugDAO commonRecipeDrugDAO = DAOFactory.getDAO(CommonRecipeDrugDAO.class);
        LOGGER.error("addCommonRecipe param. commonRecipe={}, drugList={}", JSONUtils.toString(commonRecipe),
                JSONUtils.toString(drugList));
        if (null != commonRecipe && CollectionUtils.isNotEmpty(drugList)) {
            Integer commonRecipeId = commonRecipe.getCommonRecipeId();
            LOGGER.info("addCommonRecipe commonRecipeId={} ", commonRecipeId);
            validateParam(commonRecipe, drugList);
            try {
                commonRecipe.setCommonRecipeId(null);
                commonRecipeDAO.save(commonRecipe);
                for (CommonRecipeDrug commonRecipeDrug : drugList) {
                    commonRecipeDrug.setCommonRecipeId(commonRecipe.getCommonRecipeId());
                    commonRecipeDrugDAO.save(commonRecipeDrug);
                }
                if(null != commonRecipeId) {
                    commonRecipeDAO.remove(commonRecipeId);
                }
            } catch (DAOException e) {
                LOGGER.error("addCommonRecipe add to db error. commonRecipe={}, drugList={}", JSONUtils.toString(commonRecipe),
                        JSONUtils.toString(drugList), e);
                throw new DAOException(ErrorCode.SERVICE_ERROR, "常用方添加出错");
            }
        } else {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "常用方数据不完整，请重试");
        }
    }


    /**
     * 删除常用方
     *
     * @param commonRecipeId
     */
    @RpcService
    public void deleteCommonRecipe(Integer commonRecipeId) {
        LOGGER.info("CommonRecipeService.deleteCommonRecipe  commonRecipeId = " + commonRecipeId);
        CommonRecipeDAO commonRecipeDAO = DAOFactory.getDAO(CommonRecipeDAO.class);
        // 删除常用方
        commonRecipeDAO.remove(commonRecipeId);
    }

    /**
     * 获取常用方列表
     *
     * @param doctorId
     * @param recipeType 0：获取全部常用方  其他：按类型获取
     * @param start
     * @param limit
     * @return
     */
    @Deprecated
    @RpcService
    public List<CommonRecipe> getCommonRecipeList(Integer organId, Integer doctorId, String recipeType, int start, int limit) {
        CommonRecipeDAO commonRecipeDAO = DAOFactory.getDAO(CommonRecipeDAO.class);
        LOGGER.info("getCommonRecipeList  recipeType={}, doctorId={}, organId={} ", recipeType, doctorId, organId);

        if (null != doctorId) {
            if(StringUtils.isNotEmpty(recipeType) && !"0".equals(recipeType)){
                return commonRecipeDAO.findByRecipeType(Arrays.asList(Integer.valueOf(recipeType)), doctorId, start, limit);
            }else{
                return commonRecipeDAO.findByDoctorId(doctorId, start, limit);
            }
        }
        return null;
    }

    /**
     * 获取常用方扩展
     * @param organId
     * @param doctorId
     * @param recipeType
     * @param start
     * @param limit
     * @return
     */
    @RpcService
    public List<CommonRecipe> findCommonRecipeListExt(Integer organId, Integer doctorId, List<Integer> recipeType,
                                                     int start, int limit) {
        CommonRecipeDAO commonRecipeDAO = DAOFactory.getDAO(CommonRecipeDAO.class);
        LOGGER.info("getCommonRecipeListExt  organId={}, doctorId={}, recipeType={}", organId, doctorId,
                JSONUtils.toString(recipeType));

        if (null != doctorId) {
            if(CollectionUtils.isNotEmpty(recipeType)){
                return commonRecipeDAO.findByRecipeType(recipeType, doctorId, start, limit);
            }else{
                return commonRecipeDAO.findByDoctorId(doctorId, start, limit);
            }
        }
        return null;
    }

    /**
     * 根据常用方类型检查是否存在常用方
     *
     * @param doctorId
     * @param recipeType
     * @return
     */
    @RpcService
    public Boolean checkCommonRecipeExist(Integer doctorId, String recipeType) {
        CommonRecipeDAO commonRecipeDAO = DAOFactory.getDAO(CommonRecipeDAO.class);
        if (null != doctorId && StringUtils.isNotEmpty(recipeType)) {
            List<CommonRecipe> list = commonRecipeDAO.findByRecipeType(Arrays.asList(Integer.valueOf(recipeType)), doctorId, 0, 1);
            LOGGER.info("checkCommonRecipeExist the doctorId={}, recipeType={} ", doctorId, recipeType);
            if (CollectionUtils.isEmpty(list)) {
                return false;
            } else {
                return true;
            }
        }

        return false;
    }

    /**
     * 查询常用方和常用方下的药品列表信息
     *
     * @param commonRecipeId
     * @return
     */
    @RpcService
    public Map getCommonRecipeDetails(Integer commonRecipeId) {
        LOGGER.info("CommonRecipeService.getCommonRecipeDrugList  commonRecipeId = " + commonRecipeId);

        CommonRecipeDrugDAO commonRecipeDrugDAO = DAOFactory.getDAO(CommonRecipeDrugDAO.class);
        CommonRecipeDAO commonRecipeDAO = DAOFactory.getDAO(CommonRecipeDAO.class);
        OrganDrugListDAO organDrugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);

        if (null == commonRecipeId) {
            throw new DAOException(DAOException.VALUE_NEEDED, "commonRecipeId is null");
        }

        List<CommonRecipeDrug> drugList = commonRecipeDrugDAO.findByCommonRecipeId(commonRecipeId);
        CommonRecipe commonRecipe = commonRecipeDAO.get(commonRecipeId);

        List drugIds = new ArrayList();
        for (CommonRecipeDrug commonRecipeDrug : drugList) {
            if (null != commonRecipeDrug && null != commonRecipeDrug.getDrugId()) {
                drugIds.add(commonRecipeDrug.getDrugId());
            }
            LOGGER.info("CommonRecipeService.getCommonRecipeDrugList  drugIds = " + drugIds);

        }

        // 查询机构药品表，同步药品状态
        List<OrganDrugList> organDrugList = organDrugListDAO.findByOrganIdAndDrugIdWithoutStatus(commonRecipe.getOrganId(), drugIds);
        for (CommonRecipeDrug commonRecipeDrug : drugList) {
            Integer durgId = commonRecipeDrug.getDrugId();
            for (OrganDrugList organDrug : organDrugList) {
                if (durgId.equals(organDrug.getDrugId())) {
                    commonRecipeDrug.setDrugStatus(organDrug.getStatus());
                    commonRecipeDrug.setSalePrice(organDrug.getSalePrice());
                    commonRecipeDrug.setPrice1(organDrug.getSalePrice().doubleValue());
                    if(null != commonRecipeDrug.getUseTotalDose()) {
                        commonRecipeDrug.setDrugCost(organDrug.getSalePrice().multiply(
                                new BigDecimal(commonRecipeDrug.getUseTotalDose())).divide(BigDecimal.ONE, 3, RoundingMode.UP));
                    }
                    break;
                }
            }
        }
        Map map = Maps.newHashMap();
        map.put("drugList", drugList);
        map.put("commonRecipe", commonRecipe);

        return map;
    }

    /**
     * 参数校验
     *
     * @param commonRecipe
     */
    public static void validateParam(CommonRecipe commonRecipe, List<CommonRecipeDrug> drugList) {
        Integer doctorId = commonRecipe.getDoctorId();
        Integer recipeType = commonRecipe.getRecipeType();
        String commonRecipeName = commonRecipe.getCommonRecipeName();

        if (null == doctorId || null == recipeType || StringUtils.isEmpty(commonRecipeName)) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "常用方必填参数为空");
        }

        // 常用方名称校验
        CommonRecipeDAO commonRecipeDAO = DAOFactory.getDAO(CommonRecipeDAO.class);
        CommonRecipe dbCommonRecipe = commonRecipeDAO.getByDoctorIdAndName(commonRecipe.getDoctorId(), commonRecipeName);
        if(null != dbCommonRecipe && !dbCommonRecipe.getCommonRecipeId().equals(commonRecipe.getCommonRecipeId())){
            throw new DAOException(ErrorCode.SERVICE_ERROR, "已存在相同常用方名称");
        }

        Date now = DateTime.now().toDate();
        commonRecipe.setCreateDt(now);
        commonRecipe.setLastModify(now);

        for(CommonRecipeDrug drug : drugList){
            drug.setSalePrice(null);
            drug.setDrugCost(null);
            drug.setCreateDt(now);
            drug.setLastModify(now);
            if(RecipeUtil.isTcmType(recipeType)) {
                drug.setUsePathways(null);
                drug.setUsingRate(null);
                drug.setUseTotalDose(drug.getUseDose());
            }
        }
    }

}
