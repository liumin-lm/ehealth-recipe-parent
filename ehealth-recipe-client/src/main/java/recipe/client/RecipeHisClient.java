package recipe.client;

import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.recipe.mode.HisOrderCodeReqTO;
import com.ngari.his.recipe.mode.HisOrderCodeResTO;
import com.ngari.his.recipe.service.IRecipeHisService;
import com.ngari.infra.logistics.mode.ControlLogisticsOrderDto;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.aop.LogRecord;

import java.util.List;
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
}
