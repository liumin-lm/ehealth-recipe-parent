package recipe.service;

import com.google.common.collect.Maps;
import com.ngari.base.organ.model.OrganBean;
import com.ngari.base.organ.service.IOrganService;
import com.ngari.recipe.entity.CommonRecipe;
import com.ngari.recipe.entity.CommonRecipeDrug;
import com.ngari.recipe.entity.OrganDrugList;
import ctd.persistence.DAOFactory;
import ctd.persistence.exception.DAOException;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;
import recipe.constant.ErrorCode;
import recipe.dao.CommonRecipeDAO;
import recipe.dao.CommonRecipeDrugDAO;
import recipe.dao.OrganDrugListDAO;
import recipe.util.ApplicationUtils;

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

    private static final Log LOGGER = LogFactory.getLog(CommonRecipeService.class);

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

        if (null != commonRecipe && null != drugList && !drugList.isEmpty()) {
            Integer commonRecipeId = commonRecipe.getCommonRecipeId();

            LOGGER.info("CommonRecipeService.addCommonRecipe  commonRecipeId = " + commonRecipeId);
            Date now = DateTime.now().toDate();
            // commonRecipeId不为空表示更新常用方操作，空表示新增常用方操作
            if (null != commonRecipeId) {
                commonRecipeDAO.remove(commonRecipeId);
            }
            validateParam(commonRecipe);
            commonRecipe.setCreateDt(now);
            commonRecipeDAO.save(commonRecipe);
            getTotalDrugPay(drugList);
            // 如果在插入药品时数据库报错，需要将数据回滚
            try {
                commonRecipeDrugDAO.addCommonRecipeDrugList(drugList, commonRecipe.getCommonRecipeId(), now);
            } catch (DAOException e) {
                commonRecipeDAO.remove(commonRecipe.getCommonRecipeId());
                throw new DAOException(ErrorCode.SERVICE_ERROR, "insert fail errormsg = " + e.getMessage());
            }
        } else {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "commonRecipe or drugList is null");
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
    @RpcService
    public List<CommonRecipe> getCommonRecipeList(Integer organId, Integer doctorId, String recipeType, int start, int limit) {
        IOrganService iorganService = ApplicationUtils.getBaseService(IOrganService.class);
        CommonRecipeDAO commonRecipeDAO = DAOFactory.getDAO(CommonRecipeDAO.class);
        LOGGER.info("CommonRecipeService.getCommonRecipeList  recipeType = " + recipeType + "doctorId" + doctorId +
                "organId = " + organId);

        OrganBean organ = iorganService.get(organId);
        // 检验机构是否还存在，机构注销抛异常
        if (null != organ) {
            Integer status = organ.getStatus();
            // OrganStatus.dic  注销
            Integer status9 = 9;
            if (status9.equals(status)) {
                throw new DAOException(ErrorCode.SERVICE_ERROR, "organ has been shut down");
            }
        }
        if (null != doctorId) {
            // 获取所有常用方列表
            String allCommonRecipeFlag = "0";
            if (allCommonRecipeFlag.equals(recipeType)) {
                return commonRecipeDAO.findByDoctorId(doctorId, start, limit);
            }
            // 根据常用方类型获取常用方列表
            else if (StringUtils.isNotEmpty(recipeType)) {
                return commonRecipeDAO.findByRecipeType(recipeType, doctorId, start, limit);
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
            List<CommonRecipe> list = commonRecipeDAO.findByRecipeType(recipeType, doctorId, 0, 10);
            LOGGER.info("Do CommonRecipe.checkCommonRecipeExistByRecipeType the doctorId = " + doctorId + ",recipeType" + recipeType);
            if (null != list && list.isEmpty()) {
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
                    commonRecipeDrug.setSalePrice(organDrug.getSalePrice().multiply(
                            new BigDecimal(commonRecipeDrug.getUseTotalDose())).divide(BigDecimal.ONE, 3, RoundingMode.UP));
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
    public static void validateParam(CommonRecipe commonRecipe) {
        Integer doctorId = commonRecipe.getDoctorId();
        String recipeType = commonRecipe.getRecipeType();
        String commonRecipeName = commonRecipe.getCommonRecipeName();

        if (null == doctorId) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "doctorId can not be null or empty");
        } else if (StringUtils.isEmpty(recipeType)) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "请填写处方类型");
        } else if (StringUtils.isEmpty(commonRecipeName)) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "请填写处方名称");
        }
        // 常用方名称校验
        CommonRecipeDAO commonRecipeDAO = DAOFactory.getDAO(CommonRecipeDAO.class);
        List<CommonRecipe> list = commonRecipeDAO.findByDoctorId(doctorId, 0, 10);

        for (CommonRecipe cr : list) {
            if (commonRecipeName.equals(cr.getCommonRecipeName())) {
                throw new DAOException(ErrorCode.SERVICE_ERROR, "常用方名称已存在");
            }
        }
    }

    /**
     * 计算单个药品在处方中的总价：单价x数量
     *
     * @param list
     * @return
     */
    public void getTotalDrugPay(List<CommonRecipeDrug> list) {
        for (CommonRecipeDrug commonRecipeDrug : list) {
            if (null != commonRecipeDrug.getSalePrice()) {
                // 计算出总价格
                BigDecimal drugCost = commonRecipeDrug.getSalePrice().multiply(new BigDecimal(commonRecipeDrug.getUseTotalDose()))
                        .divide(BigDecimal.ONE, 3, RoundingMode.UP);
                commonRecipeDrug.setDrugCost(drugCost);
            }
        }
    }

}
