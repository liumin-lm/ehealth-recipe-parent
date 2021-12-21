package recipe.manager;

import com.ngari.recipe.entity.RecipeDictionary;
import org.springframework.stereotype.Service;
import recipe.dao.RecipeDictionaryDAO;

import javax.annotation.Resource;
import java.util.List;

/**
 * 字典
 *
 * @author yinsheng
 * @date 2021\12\21 15:22
 */
@Service
public class RecipeDictionaryManager extends BaseManager{

    @Resource
    private RecipeDictionaryDAO recipeDictionaryDAO;

    /**
     * 保存处方字典相关信息
     * @param recipeDictionary
     * @return
     */
    public RecipeDictionary saveRecipeDictionary(RecipeDictionary recipeDictionary){
        if (null != recipeDictionary.getId()) {
            recipeDictionary = recipeDictionaryDAO.update(recipeDictionary);
        } else {
            recipeDictionary = recipeDictionaryDAO.save(recipeDictionary);
        }
        return recipeDictionary;
    }

    /**
     * 搜索字典
     * @param organId
     * @param searchName
     * @return
     */
    public List<RecipeDictionary> findRecipeDictionaryByName( Integer organId, String searchName){
        return recipeDictionaryDAO.findRecipeDictionaryByName(organId, searchName);
    }
}
