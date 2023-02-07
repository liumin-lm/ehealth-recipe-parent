package recipe.core.api;

import com.ngari.recipe.dto.RecipeDetailDTO;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.Recipedetail;
import com.ngari.recipe.recipe.model.HisSendResTO;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import com.ngari.recipe.vo.RecipeSkipVO;
import recipe.vo.ResultBean;
import recipe.vo.doctor.ConfigOptionsVO;
import recipe.vo.doctor.RecipeInfoVO;
import recipe.vo.doctor.ValidateDetailVO;

import java.util.List;

/**
 * 处方明细接口
 *
 * @author fuzi
 */
public interface IRecipeDetailBusinessService {
    /**
     * 校验线上线下 药品数据 用于续方需求
     *
     * @param validateDetailVO 机构id
     * @return
     */
    ValidateDetailVO continueRecipeValidateDrug(ValidateDetailVO validateDetailVO);

    /**
     * 校验处方药品配置时间
     *
     * @param validateDetailVO 药品数据VO
     * @return 处方药品明细
     */
    List<RecipeDetailBean> useDayValidate(ValidateDetailVO validateDetailVO);

    /**
     * 检验处方药品超量
     * @param validateDetailVO 药品数据VO
     * @return 处方药品明细
     */
    List<RecipeDetailBean> drugSuperScalarValidate(ValidateDetailVO validateDetailVO);

    /**
     * 患者端处方进行中列表查询药品信息
     *
     * @param orderCode 订单code
     * @return
     */
    String getDrugName(String orderCode, Integer orderId);

    /**
     * 校验复诊下重复处方/药品
     *
     * @param validateDetailVO 当前处方药品
     * @return
     */
    ResultBean<String> validateRepeatRecipe(ValidateDetailVO validateDetailVO);

    /**
     * 校验复诊下重复药品最大数量
     *
     * @param validateDetailVO
     * @return
     */
    ResultBean<String> validateRepeatRecipeDetail(ValidateDetailVO validateDetailVO);

    /**
     * 跳转到第三方
     *
     * @param organId    机构ID
     * @param recipeCode his处方号
     * @param recipeType 类型 1 线上处方 2 门诊处方
     * @return
     */
    RecipeSkipVO getRecipeSkipUrl(Integer organId, String recipeCode, Integer recipeType);

    /**
     * 校验his 药品规则，靶向药，大病医保，抗肿瘤药物等
     *
     * @param recipe          处方信息
     * @param recipeDetailDTO 药品信息
     * @return
     */
    List<RecipeDetailDTO> validateHisDrugRule(Recipe recipe, List<RecipeDetailDTO> recipeDetailDTO, String registerId, String dbType);

    /**
     * 复杂逻辑配置项处理
     * 由于判断配置项逻辑 对于前端复杂，由后端统一处理返回结果，此接口仅仅处理复杂逻辑判断
     * <p>
     * 处方天数大于多少天需要医生二次确认
     * recipeNumberDoctorConfirmCaution
     * <p>
     * 处方天数大于多少天不允许开具
     * recipeNumberDoctorConfirmBlocking
     * <p>
     * 处方金额大于多少元需要医生二次确认
     * recipeMoneyDoctorConfirmCaution
     * <p>
     * 处方金额大于多少元不允许开具
     * recipeMoneyDoctorConfirmBlocking
     *
     * @param validateDetailVO
     * @return
     */
    List<ConfigOptionsVO> validateConfigOptions(ValidateDetailVO validateDetailVO);

    /**
     * 获取二方id下关联的处方和明细
     *
     * @param clinicId   二方id
     * @param bussSource 开处方来源 1问诊 2复诊(在线续方) 3网络门诊
     * @return
     */
    List<RecipeInfoVO> recipeAllByClinicId(Integer clinicId, Integer bussSource);

    /**
     * his回写处方数据更新表字段
     *
     * @param response
     * @return
     */
    List<Recipedetail> sendSuccessDetail(HisSendResTO response, Recipe recipe);
}
