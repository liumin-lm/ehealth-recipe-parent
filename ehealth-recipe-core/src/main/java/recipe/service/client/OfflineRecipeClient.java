package recipe.service.client;

import com.ngari.recipe.commonrecipe.model.CommonDTO;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 获取
 *
 * @author fuzi
 */
@Service
public class OfflineRecipeClient extends BaseClient {
    public List<CommonDTO> offlineCommonRecipe(Integer doctorId) {
        return null;
    }
}
