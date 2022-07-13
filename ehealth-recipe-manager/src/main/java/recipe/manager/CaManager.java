package recipe.manager;

import ca.vo.CaSignResultVo;
import com.ngari.base.BaseAPI;
import com.ngari.base.property.service.IConfigurationCenterUtilsService;
import com.ngari.his.ca.model.CaSealRequestTO;
import com.ngari.patient.dto.DoctorDTO;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.Recipedetail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.aop.LogRecord;
import recipe.client.CaClient;
import recipe.enumerate.status.RecipeStatusEnum;
import recipe.util.RedisClient;

import java.util.List;

/**
 * 处方
 *
 * @author liumin
 * @date 2022\6\17 0030 14:21
 */
@Service
public class CaManager extends BaseManager {
    private static final Integer CA_OLD_TYPE = new Integer(0);
    @Autowired
    private RedisClient redisClient;
    @Autowired
    private CaClient caClient;
    /**
     * 运营平台机构CA配置
     */
    private String THIRD_CA_SIGN = "thirdCASign";

    /**
     * 陕西CA
     */
    private String CA_TYPE_SHANXI = "shanxiCA";

    /**
     * 陕西CA密码前缀
     */
    private String SHANXI_CA_PASSWORD = "SHANXI_CA_PASSWORD";


    /**
     * 设置CA密码
     *
     * @param clinicOrgan
     * @param doctor
     * @param caPassword
     */
    @LogRecord
    public void setCaPassWord(Integer clinicOrgan, Integer doctor, String caPassword) {
        //将密码放到redis中
        IConfigurationCenterUtilsService configurationService = BaseAPI.getService(IConfigurationCenterUtilsService.class);
        String thirdCASign = (String) configurationService.getConfiguration(clinicOrgan, THIRD_CA_SIGN);
        if (CA_TYPE_SHANXI.equals(thirdCASign)) {
            redisClient.set(SHANXI_CA_PASSWORD + clinicOrgan + doctor, caPassword);
        } else {
            redisClient.set("caPassword", caPassword);
        }
    }

    /**
     * 获取CA密码
     *
     * @param clinicOrgan
     * @param doctor
     * @return
     */
    @LogRecord
    public String getCaPassWord(Integer clinicOrgan, Integer doctor) {
        String caPassword = "";
        IConfigurationCenterUtilsService configurationService = BaseAPI.getService(IConfigurationCenterUtilsService.class);
        String thirdCASign = (String) configurationService.getConfiguration(clinicOrgan, THIRD_CA_SIGN);
        if (CA_TYPE_SHANXI.equals(thirdCASign)) {
            caPassword = redisClient.get(SHANXI_CA_PASSWORD + clinicOrgan + doctor);
        } else {
            caPassword = redisClient.get("caPassword");
        }
        caPassword = null == caPassword ? "" : caPassword;
        return caPassword;
    }

    /**
     * 获取ca新老流程状态
     *
     * @param recipe
     * @return
     */
    public Integer caProcessType(Recipe recipe) {
        if (null == recipe) {
            logger.warn("当前处方不存在!");
            return null;
        }
        //处方签名中 点击撤销按钮 如果处方单状态处于已取消 则不走下面逻辑
        if (RecipeStatusEnum.RECIPE_STATUS_REVOKE.getType().equals(recipe.getStatus())) {
            logger.info("retryDoctorSignCheck 处方单已经撤销，recipeid：{}", recipe.getRecipeId());
            return null;
        }
        Integer caType = configurationClient.getValueCatchReturnInteger(recipe.getClinicOrgan(), "CAProcessType", CA_OLD_TYPE);
        logger.info("RecipeService retryDoctorSignCheck CANewOldWay = {}", caType);
        return caType;
    }

    /**
     * 老流程保存sign，新流程已经移动至CA保存
     *
     * @param recipe
     * @param resultVo
     * @param isDoctor
     */
    public void signRecipeInfoSave(Recipe recipe, CaSignResultVo resultVo, boolean isDoctor) {
        caClient.signRecipeInfoSave(recipe.getRecipeId(), isDoctor, resultVo, recipe.getClinicOrgan());
    }

    /**
     * 老流程保存sign，
     *
     * @param recipe
     * @param details
     * @param resultVo
     * @param isDoctor
     */
    public void oldCaCallBack(Recipe recipe, List<Recipedetail> details, CaSignResultVo resultVo, boolean isDoctor) {
        caClient.signRecipeInfoSave(recipe.getRecipeId(), isDoctor, resultVo, recipe.getClinicOrgan());
        caClient.signUpdate(recipe, details);
    }

    /**
     * 调用ca老二方包签名接口-ca回调接口为retryCaDoctorCallBackToRecipe
     *
     * @param requestSeal ca入参数
     * @param recipe
     */
    public void oldCommonCASign(CaSealRequestTO requestSeal, Recipe recipe) {
        //签名时的密码从redis中获取
        String caPassword = this.getCaPassWord(recipe.getClinicOrgan(), recipe.getDoctor());
        DoctorDTO doctor = doctorClient.getDoctor(recipe.getDoctor());
        caClient.oldCommonCASign(requestSeal, recipe, doctor.getIdNumber(), caPassword);
        logger.info("generateRecipePdfAndSign 签名成功. 标准对接CA模式, recipeId={}", recipe.getRecipeId());
    }

}
