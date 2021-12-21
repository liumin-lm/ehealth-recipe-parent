package recipe.business;

import com.ngari.recipe.entity.RecipeDictionary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.core.api.IRecipeDictionaryBusinessService;
import recipe.manager.RecipeDictionaryManager;
import recipe.util.ObjectCopyUtils;
import recipe.vo.second.RecipeDictionaryVO;

import java.util.List;

/**
 * 处方字典处理类
 *
 * @author yinsheng
 * @date 2021\12\20 17:30
 */
@Service
public class RecipeDictionaryBusinessService extends BaseService implements IRecipeDictionaryBusinessService {

    @Autowired
    private RecipeDictionaryManager recipeDictionaryManager;

    @Override
    public Integer saveRecipeDictionary(RecipeDictionaryVO recipeDictionaryVO) {
        RecipeDictionary recipeDictionary = ObjectCopyUtils.convert(recipeDictionaryVO, RecipeDictionary.class);
        recipeDictionary = recipeDictionaryManager.saveRecipeDictionary(recipeDictionary);
        return recipeDictionary.getId();
    }

    @Override
    public List<RecipeDictionaryVO> findRecipeDictionaryByName(Integer organId, String searchName) {
        List<RecipeDictionary> recipeDictionaryList = recipeDictionaryManager.findRecipeDictionaryByName(organId, searchName);
        return ObjectCopyUtils.convert(recipeDictionaryList, RecipeDictionaryVO.class);
    }
}
