package recipe.business;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.core.api.patient.IOutPatientRecipeService;
import recipe.manager.OutPatientRecipeManager;

/**
 * 门诊处方
 * @author yinsheng
 * @date 2021\7\16 0016 17:30
 */
@Service
public class OutPatientRecipeService extends BaseService implements IOutPatientRecipeService{

    @Autowired
    private OutPatientRecipeManager outPatientRecipeManager;

    @Override
    public String getOutRecipeDisease(Integer organId, String patientName, String registerID, String patientId) {
        return outPatientRecipeManager.getOutRecipeDisease(organId, patientName, registerID, patientId);
    }
}
