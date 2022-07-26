package recipe.core.api.doctor;

/**
 * @author yinsheng
 * @date 2021\7\16 0016 17:16
 */
public interface ICaBusinessService {

    void signRecipeCAInterruptForStandard(Integer recipeId);

    void checkRecipeCAInterruptForStandard(Integer recipeId);
}
