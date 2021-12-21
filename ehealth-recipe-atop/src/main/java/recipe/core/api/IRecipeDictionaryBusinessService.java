package recipe.core.api;

import recipe.vo.second.RecipeDictionaryVO;

import java.util.List;

/**
 * 字典对照数据操作
 *
 * @author yins
 */
public interface IRecipeDictionaryBusinessService {

    Integer saveRecipeDictionary(RecipeDictionaryVO recipeDictionaryVO);

    List<RecipeDictionaryVO> findRecipeDictionaryByName(Integer organId, String searchName);
}
