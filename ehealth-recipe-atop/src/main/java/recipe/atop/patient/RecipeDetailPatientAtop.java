package recipe.atop.patient;

import ctd.persistence.exception.DAOException;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import recipe.atop.BaseAtop;
import recipe.constant.ErrorCode;
import recipe.core.api.IRecipeDetailService;

import javax.annotation.Resource;
import java.util.Objects;

/**
 * @description： 处方明细 患者端入口
 * @author： whf
 * @date： 2021-06-04 17:00
 */
@RpcBean("recipeDetailPatientAtop")
public class RecipeDetailPatientAtop extends BaseAtop {

    @Resource
    private IRecipeDetailService recipeDetailService;

    /**
     * todo 需要确认是否修改正确 （王鸿芳）
     * 患者端处方进行中列表查询药品信息
     *
     * @param orderCode 订单code
     * @return
     */
    @RpcService
    public String getDrugName(String orderCode) {
        logger.info("RecipeDetailPatientAtop getDrugName orderCode {}", orderCode);
        if (Objects.isNull(orderCode)) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "入参为空");
        }
        try {
            String drugNames = recipeDetailService.getDrugName(orderCode);
            logger.info("RecipeDetailPatientAtop getDrugName result = {}", orderCode);
            return drugNames;
        } catch (DAOException e1) {
            logger.error("RecipeDetailPatientAtop getDrugName error", e1);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e1.getMessage());
        } catch (Exception e) {
            logger.error("RecipeDetailPatientAtop getDrugName error e", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        }
    }
}
