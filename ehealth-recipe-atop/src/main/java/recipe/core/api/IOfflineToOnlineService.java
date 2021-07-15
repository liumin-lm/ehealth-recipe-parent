package recipe.core.api;

import com.ngari.recipe.vo.SettleForOfflineToOnlineVO;
import recipe.vo.patient.RecipeGiveModeButtonRes;

import java.util.List;

/**
 * @author fuzi
 */
public interface IOfflineToOnlineService {

    List<RecipeGiveModeButtonRes> settleForOfflineToOnline(SettleForOfflineToOnlineVO request);
}
