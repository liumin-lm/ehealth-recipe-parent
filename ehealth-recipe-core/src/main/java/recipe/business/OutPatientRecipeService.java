package recipe.business;

import com.alibaba.fastjson.JSON;
import com.ngari.follow.utils.ObjectCopyUtil;
import com.ngari.his.recipe.mode.OutPatientRecipeReq;
import com.ngari.recipe.dto.DiseaseInfoDTO;
import com.ngari.recipe.dto.OutPatientRecipeDTO;
import com.ngari.recipe.recipe.model.OutPatientRecipeVO;
import com.ngari.recipe.vo.OutPatientRecipeReqVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.core.api.patient.IOutPatientRecipeService;
import recipe.manager.OutPatientRecipeManager;

import java.util.List;

/**
 * 门诊处方相关服务
 * @author yinsheng
 * @date 2021\7\16 0016 17:30
 */
@Service
public class OutPatientRecipeService extends BaseService implements IOutPatientRecipeService{

    @Autowired
    private OutPatientRecipeManager outPatientRecipeManager;

    @Override
    public List<DiseaseInfoDTO> getOutRecipeDisease(Integer organId, String patientName, String registerID, String patientId) {
        return outPatientRecipeManager.getOutRecipeDisease(organId, patientName, registerID, patientId);
    }

    @Override
    public List<OutPatientRecipeVO> queryOutPatientRecipe(OutPatientRecipeReqVO outPatientRecipeReqVO) {
        logger.info("OutPatientRecipeService queryOutPatientRecipe outPatientRecipeReq:{}.", JSON.toJSONString(outPatientRecipeReqVO));
        OutPatientRecipeReq outPatientRecipeReq = ObjectCopyUtil.convert(outPatientRecipeReqVO, OutPatientRecipeReq.class);
        List<OutPatientRecipeDTO> outPatientRecipeDTOS = outPatientRecipeManager.queryOutPatientRecipe(outPatientRecipeReq);
        //TODO 业务逻辑处理
        return ObjectCopyUtil.convert(outPatientRecipeDTOS, OutPatientRecipeVO.class);
    }
}
