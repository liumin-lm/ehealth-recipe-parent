package recipe.client;

import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeExtend;
import ctd.persistence.exception.DAOException;
import org.springframework.stereotype.Service;
import recipe.constant.PayConstant;
import recipe.constant.RecipeBussConstant;
import recipe.constant.RecipeStatusConstant;
import recipe.enumerate.status.RecipeAuditStateEnum;
import recipe.enumerate.status.RecipeStateEnum;
import recipe.enumerate.status.SignEnum;
import recipe.enumerate.status.WriteHisEnum;
import recipe.enumerate.type.MedicalTypeEnum;
import recipe.enumerate.type.RecipeBusinessTypeEnum;
import recipe.util.RecipeUtil;
import recipe.util.ValidateUtil;

import java.util.Date;
import java.util.Objects;

/**
 * 默认值设置工具类
 *
 * @author fuzi
 */
@Service
public class DefaultValueClient extends BaseClient {
    /**
     * 类加载排序
     *
     * @return
     */
    @Override
    public Integer getSort() {
        return 1;
    }

    /**
     * 设置处方默认数据
     *
     * @param recipe 处方头对象
     */
    @Override
    public void setRecipe(Recipe recipe) {
        recipe.setStatus(RecipeStatusConstant.UNSIGN);
        recipe.setProcessState(RecipeStateEnum.NONE.getType());
        recipe.setSubState(RecipeStateEnum.NONE.getType());
        recipe.setAuditState(RecipeAuditStateEnum.DEFAULT.getType());
        recipe.setWriteHisState(WriteHisEnum.NONE.getType());
        recipe.setDoctorSignState(SignEnum.NONE.getType());
        recipe.setCheckerSignState(SignEnum.NONE.getType());
        recipe.setPayFlag(PayConstant.PAY_FLAG_NOT_PAY);
        recipe.setSignDate(new Date());
        recipe.setCreateDate(new Date());
        recipe.setLastModify(new Date());
        recipe.setMedicalFlag(0);
        //设置处方支付类型 0 普通支付 1 不选择购药方式直接去支付
        recipe.setRecipePayType(0);
        //监管同步标记
        recipe.setSyncFlag(0);
        //默认流转模式为平台模式
        recipe.setRecipeMode(null == recipe.getRecipeMode() ? RecipeBussConstant.RECIPEMODE_NGARIHEALTH : recipe.getRecipeMode());
        //默认无法医保支付
        recipe.setMedicalPayFlag(ValidateUtil.integerIsEmpty(recipe.getMedicalPayFlag()) ? 0 : recipe.getMedicalPayFlag());
        //默认可以医院，药企发药
        recipe.setDistributionFlag(ValidateUtil.integerIsEmpty(recipe.getDistributionFlag()) ? 0 : recipe.getDistributionFlag());
        //设置处方来源类型
        recipe.setRecipeSourceType(ValidateUtil.integerIsEmpty(recipe.getRecipeSourceType()) ? 1 : recipe.getRecipeSourceType());
        //默认非外带处方
        recipe.setTakeMedicine(ValidateUtil.integerIsEmpty(recipe.getTakeMedicine()) ? 0 : recipe.getTakeMedicine());
        //默认有效天数
        recipe.setValueDays(ValidateUtil.integerIsEmpty(recipe.getValueDays()) ? 3 : recipe.getValueDays());
        //默认来源为纳里APP处方
        recipe.setFromflag(ValidateUtil.integerIsEmpty(recipe.getFromflag()) ? 1 : recipe.getFromflag());
        recipe.setChooseFlag(ValidateUtil.integerIsEmpty(recipe.getChooseFlag()) ? 0 : recipe.getChooseFlag());
        recipe.setPatientStatus(1);
        recipe.setGiveFlag(0);
        recipe.setRemindFlag(0);
        recipe.setPushFlag(0);
        //设置处方审核状态默认值
        recipe.setCheckStatus(0);
        //设置抢单的默认状态
        recipe.setGrabOrderStatus(0);
        //设置为非快捷购药处方
        recipe.setFastRecipeFlag(0);
        //默认剂数为1
        if (!RecipeUtil.isTcmType(recipe.getRecipeType())) {
            recipe.setCopyNum(0);
        } else {
            recipe.setCopyNum(ValidateUtil.integerIsEmpty(recipe.getCopyNum()) ? 1 : recipe.getCopyNum());
        }
    }

    /**
     * 设置处方默认数据
     * @param recipe
     * @param extend
     */
    @Override
    public void setRecipeExt(Recipe recipe, RecipeExtend extend) {
        if (Objects.isNull(recipe.getRecipeId())) {
            throw new DAOException("处方id不能为空");
        }
        extend.setRecipeId(recipe.getRecipeId());
        extend.setCancellation("");
        extend.setCanUrgentAuditRecipe(null == extend.getCanUrgentAuditRecipe() ? 0 : extend.getCanUrgentAuditRecipe());
        extend.setAppointEnterpriseType(null == extend.getAppointEnterpriseType() ? 0 : extend.getAppointEnterpriseType());
        extend.setMedicalType(null != extend.getPatientType() ? extend.getPatientType() : extend.getMedicalType());
        //老的字段兼容处理
        String medicalTypeText = MedicalTypeEnum.getOldMedicalTypeText(extend.getPatientType());
        extend.setMedicalTypeText(null != medicalTypeText ? medicalTypeText : extend.getMedicalTypeText());
        extend.setRecipeBusinessType(RecipeBusinessTypeEnum.getRecipeBusinessType(recipe.getBussSource()));
    }

}
