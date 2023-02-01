package recipe.client;

import com.ngari.common.mode.HisResponseTO;
import com.ngari.consult.ConsultAPI;
import com.ngari.consult.common.model.ConsultExDTO;
import com.ngari.consult.common.service.IConsultExService;
import com.ngari.his.recipe.mode.*;
import com.ngari.his.recipe.service.IRecipeHisService;
import com.ngari.infra.logistics.mode.ControlLogisticsOrderDto;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.recipe.dto.RecipeDTO;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeExtend;
import com.ngari.recipe.entity.RecipeOrder;
import com.ngari.revisit.RevisitAPI;
import com.ngari.revisit.common.model.RevisitExDTO;
import com.ngari.revisit.common.service.IRevisitExService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.aop.LogRecord;
import recipe.common.OnsConfig;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @description： 处方his接口调用
 * @author： whf
 * @date： 2022-12-15 11:51
 */
@Service
public class RecipeHisClient extends BaseClient {

    @Autowired
    private IRecipeHisService recipeHisService;

    /**
     * 获取处方医保id
     * @param organId
     * @param recipeCode
     * @return
     */
    @LogRecord
    public List<HisOrderCodeResTO> queryHisOrderCodeByRecipeCode(String patientID,Integer organId,List<String> recipeCode) {
        if (CollectionUtils.isEmpty(recipeCode) || Objects.isNull(organId)) {
            return null;
        }
        List<HisOrderCodeResTO> hisOrderCodeResTOS = recipeCode.stream().map(code -> {
            HisOrderCodeResTO hisOrderCodeResTO = new HisOrderCodeResTO();
            hisOrderCodeResTO.setRecipeCode(code);
            return hisOrderCodeResTO;
        }).collect(Collectors.toList());
        HisOrderCodeReqTO hisOrderCodeReqTOS = new HisOrderCodeReqTO();
        hisOrderCodeReqTOS.setOrganId(organId);
        hisOrderCodeReqTOS.setPatientID(patientID);
        hisOrderCodeReqTOS.setHisOrderCodeResTOS(hisOrderCodeResTOS);
        List<HisOrderCodeResTO> response = null;
        try {
            HisResponseTO<List<HisOrderCodeResTO>> listHisResponseTO = recipeHisService.queryHisOrderCodeByRecipeCode(hisOrderCodeReqTOS);
            response = this.getResponse(listHisResponseTO);
        } catch (Exception e) {
            logger.error("RecipeHisClient queryHisOrderCodeByRecipeCode error ", e);
        }
        return response;
    }


    @LogRecord
    public List<HisSettleResTo> queryHisSettle(HisSettleReqTo hisSettleReqTo) {
        if (Objects.isNull(hisSettleReqTo)) {
            return null;
        }
        List<HisSettleResTo> response = null;
        try {
            HisResponseTO<List<HisSettleResTo>> listHisResponseTO = recipeHisService.queryHisSettle(hisSettleReqTo);
            response = this.getResponse(listHisResponseTO);
        } catch (Exception e) {
            logger.error("RecipeHisClient queryHisSettle error ", e);
        }
        return response;
    }
}
