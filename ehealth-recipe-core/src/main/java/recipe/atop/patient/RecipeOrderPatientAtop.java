package recipe.atop.patient;

import com.alibaba.fastjson.JSON;
import com.ngari.recipe.recipe.model.SkipThirdBean;
import com.ngari.recipe.recipe.model.SkipThirdReqVO;
import ctd.persistence.exception.DAOException;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.atop.BaseAtop;
import recipe.constant.ErrorCode;
import recipe.service.RecipeOrderService;

/**
 * @description： 处方订单 患者端入口
 * @author yinsheng
 * @date 2021\6\20 0020 16:43
 */
@RpcBean("recipeOrderPatientAtop")
public class RecipeOrderPatientAtop  extends BaseAtop {

    @Autowired
    private RecipeOrderService recipeOrderService;

    /**
     * 跳转到第三方处方购药页面
     * @param skipThirdReqVO  处方ID + 患者选择的购药方式
     * @return  跳转链接地址
     */
    @RpcService
    public SkipThirdBean skipThirdPage(SkipThirdReqVO skipThirdReqVO){
        logger.info("RecipeOrderPatientAtop skipThirdPage skipThirdReqVO:{}.", JSON.toJSONString(skipThirdReqVO));
        validateAtop(skipThirdReqVO.getRecipeIds());
        try {
            //上传处方到第三方,上传失败提示HIS返回的失败信息
            recipeOrderService.uploadRecipeInfoToThird(skipThirdReqVO);
            //获取跳转链接
            SkipThirdBean skipThirdBean = recipeOrderService.getSkipUrl(skipThirdReqVO);
            logger.info("RecipeOrderPatientAtop skipThirdPage skipThirdBean:{}.", JSON.toJSONString(skipThirdBean));
            return skipThirdBean;
        } catch (DAOException e1) {
            logger.error("RecipeOrderPatientAtop skipThirdPage error", e1);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e1.getMessage());
        } catch (Exception e) {
            logger.error("RecipeOrderPatientAtop skipThirdPage error e", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        }
    }
}
