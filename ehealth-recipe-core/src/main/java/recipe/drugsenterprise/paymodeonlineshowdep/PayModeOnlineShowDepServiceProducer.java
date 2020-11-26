package recipe.drugsenterprise.paymodeonlineshowdep;

import com.ngari.recipe.entity.DrugsEnterprise;
import com.ngari.recipe.entity.Recipe;
import recipe.ApplicationUtils;
import recipe.drugsenterprise.AccessDrugEnterpriseService;
import recipe.drugsenterprise.RemoteDrugEnterpriseService;

/**
 * created by shiyuping on 2020/11/10
 * @author shiyuping
 */
public class PayModeOnlineShowDepServiceProducer {

    public static PayModeOnlineShowDepInterface getShowDepService(DrugsEnterprise subDep, Recipe recipe) {

        if (new Integer(1).equals(subDep.getShowStoreFlag())) {
            return new ShowStoreFlagDepService();
        }
        if (new Integer(2).equals(recipe.getRecipeSource())) {
            return new OfflineToOnlineShowDepService();
        }
        RemoteDrugEnterpriseService remoteDrugEnterpriseService = ApplicationUtils.getRecipeService(RemoteDrugEnterpriseService.class);
        AccessDrugEnterpriseService remoteService = remoteDrugEnterpriseService.getServiceByDep(subDep);
        boolean specialMake = remoteService.specialMakeDepList(subDep, recipe);
        if (specialMake) {
            return new HisAdministrationShowDepService();
        }
        return new CommonShowDepService();
    }
}
