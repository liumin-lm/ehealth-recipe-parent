package recipe.client;

import com.alibaba.fastjson.JSON;
import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.recipe.mode.CommonDTO;
import com.ngari.his.recipe.mode.OfflineCommonRecipeRequestTO;
import com.ngari.patient.dto.DoctorDTO;
import ctd.persistence.exception.DAOException;
import org.springframework.stereotype.Service;
import recipe.constant.ErrorCode;

import java.util.List;

/**
 * his处方 交互处理类
 *
 * @author fuzi
 */
@Service
public class OfflineRecipeClient extends BaseClient {
    /**
     * @param organId   机构id
     * @param doctorDTO 医生信息
     * @return 线下常用方对象
     */
    public List<CommonDTO> offlineCommonRecipe(Integer organId, DoctorDTO doctorDTO) {
        logger.info("OfflineRecipeClient offlineCommonRecipe organId:{}，doctorDTO:{}", organId, JSON.toJSONString(doctorDTO));
        OfflineCommonRecipeRequestTO request = new OfflineCommonRecipeRequestTO();
        request.setOrganId(organId);
        request.setDoctorId(doctorDTO.getDoctorId());
        request.setJobNumber(doctorDTO.getJobNumber());
        request.setName(doctorDTO.getName());
        try {
            HisResponseTO<List<com.ngari.his.recipe.mode.CommonDTO>> hisResponse = recipeHisService.offlineCommonRecipe(request);
            List<CommonDTO> resultDTO = getResponse(hisResponse);
            return resultDTO;
        } catch (Exception e) {
            logger.error("OfflineRecipeClient offlineCommonRecipe hisResponse", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        }
    }
}
