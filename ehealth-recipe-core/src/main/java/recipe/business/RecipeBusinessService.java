package recipe.business;

import com.ngari.recipe.vo.OutPatientRecipeVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.core.api.IRecipeBusinessService;
import recipe.manager.OutPatientRecipeManager;

/**
 * 处方业务核心逻辑处理类
 *
 * @author yinsheng
 * @date 2021\7\16 0016 17:30
 */
@Service
public class RecipeBusinessService extends BaseService implements IRecipeBusinessService {

    @Autowired
    private OutPatientRecipeManager outPatientRecipeManager;

    @Override
    public String getOutRecipeDisease(Integer organId, String patientName, String registerID, String patientId) {
        return outPatientRecipeManager.getOutRecipeDisease(organId, patientName, registerID, patientId);
    }

    @Override
    public void queryOutPatientRecipe(OutPatientRecipeVO outPatientRecipeVO) {

    }
}
