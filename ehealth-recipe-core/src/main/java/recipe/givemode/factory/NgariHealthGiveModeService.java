package recipe.givemode.factory;

import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.recipe.model.GiveModeShowButtonVO;
import com.ngari.recipe.recipe.model.PatientTabStatusRecipeDTO;
import org.springframework.stereotype.Component;


/**
 * @author yinsheng
 * @date 2020\12\3 0003 19:58
 */
@Component("ngariHealthGiveModeService")
public class NgariHealthGiveModeService extends AbstractGiveModeService implements IGiveModeBase{

    @Override
    public void setSpecialItem(PatientTabStatusRecipeDTO record, GiveModeShowButtonVO giveModeShowButtonVO, Recipe recipe) {

    }
}
