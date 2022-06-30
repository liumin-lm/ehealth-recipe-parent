package recipe.manager;

import com.ngari.base.BaseAPI;
import com.ngari.base.property.service.IConfigurationCenterUtilsService;
import com.ngari.recipe.entity.Recipe;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.aop.LogRecord;
import recipe.enumerate.status.RecipeStatusEnum;
import recipe.util.RedisClient;

/**
 * 处方
 *
 * @author liumin
 * @date 2022\6\17 0030 14:21
 */
@Service
public class CaManager extends BaseManager {
    @Autowired
    private RedisClient redisClient;
    private static final Integer CA_OLD_TYPE = new Integer(0);
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

}
