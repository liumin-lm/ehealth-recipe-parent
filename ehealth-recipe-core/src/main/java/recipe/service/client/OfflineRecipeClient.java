package recipe.service.client;

import com.alibaba.fastjson.JSON;
import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.recipe.mode.OfflineCommonRecipeRequestTO;
import com.ngari.patient.dto.DoctorDTO;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.commonrecipe.model.CommonDTO;
import com.ngari.recipe.commonrecipe.model.CommonRecipeDTO;
import com.ngari.recipe.commonrecipe.model.CommonRecipeDrugDTO;
import com.ngari.recipe.commonrecipe.model.CommonRecipeExtDTO;
import ctd.persistence.exception.DAOException;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.stereotype.Service;
import recipe.constant.ErrorCode;

import java.util.LinkedList;
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
            List<com.ngari.his.recipe.mode.CommonDTO> resultDTO = getResponse(hisResponse);
            List<CommonDTO> result = new LinkedList<>();
            if (CollectionUtils.isEmpty(resultDTO)) {
                return result;
            }
            resultDTO.forEach(a -> {
                CommonDTO commonDTO = new CommonDTO();
                commonDTO.setCommonRecipeDTO(ObjectCopyUtils.convert(a.getCommonRecipeDTO(), CommonRecipeDTO.class));
                commonDTO.setCommonRecipeExt(ObjectCopyUtils.convert(a.getCommonRecipeExt(), CommonRecipeExtDTO.class));
                commonDTO.setCommonRecipeDrugList(ObjectCopyUtils.convert(a.getCommonRecipeDrugList(), CommonRecipeDrugDTO.class));
                result.add(commonDTO);
            });
            return result;
        } catch (Exception e) {
            logger.error("OfflineRecipeClient offlineCommonRecipe hisResponse", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        }
    }
}
