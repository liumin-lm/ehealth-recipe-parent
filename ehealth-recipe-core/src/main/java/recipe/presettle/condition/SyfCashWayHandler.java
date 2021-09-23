package recipe.presettle.condition;

import com.ngari.revisit.common.model.RevisitExDTO;
import ctd.util.JSONUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import recipe.client.IConfigurationClient;
import recipe.client.RevisitClient;
import recipe.enumerate.type.BussSourceType;
import recipe.enumerate.type.MedicalTypeEnum;
import recipe.enumerate.type.RecipePayTypeEnum;
import recipe.presettle.RecipeOrderTypeEnum;
import recipe.presettle.model.OrderTypeCreateConditionRequest;

import java.util.Objects;

/**
 * 邵逸夫医院新增结算方式
 */
@Component
public class SyfCashWayHandler implements IOrderTypeConditionHandler{
    private static final Logger LOGGER = LoggerFactory.getLogger(SyfCashWayHandler.class);
    @Autowired
    private IConfigurationClient configurationClient;
    @Autowired
    private RevisitClient revisitClient;

    @Override
    public Integer getOrderType(OrderTypeCreateConditionRequest request) {
        LOGGER.info("SyfCashWayHandler getOrderType request:{}.", JSONUtils.toString(request));
        Integer organId = request.getRecipe().getClinicOrgan();
        Integer bussSource = request.getRecipe().getBussSource();
        if (null == organId || BussSourceType.BUSSSOURCE_CONSULT.getType().equals(bussSource)) {
            return null;
        }
        //通过运营平台控制开关决定是否走此种模式
        Boolean syfPayMode = configurationClient.getValueBooleanCatch(organId, "syfPayMode",false);
        if (!syfPayMode) {
            //说明不需要走邵逸支付
            return null;
        }
        //查询复诊获取患者类型（自费or医保）
        RevisitExDTO revisitExDTO = revisitClient.getByClinicId(request.getRecipe().getClinicId());
        if(Objects.isNull(revisitExDTO)){
            return null;
        }
        if (MedicalTypeEnum.SELF_PAY.getType().equals(revisitExDTO.getMedicalFlag())) {
            //自费结算
            return RecipeOrderTypeEnum.HOSPITAL_SELF.getType();
        } else if (MedicalTypeEnum.MEDICAL_PAY.getType().equals(revisitExDTO.getMedicalFlag())) {
            //医保结算
            return RecipeOrderTypeEnum.PROVINCIAL_MEDICAL.getType();
        }
        return null;
    }

    @Override
    public int getPriorityLevel() {
        return -1;
    }
}
