package recipe.prescription;

import com.ngari.recipe.common.RecipeCommonResTO;
import com.ngari.recipe.hisprescription.model.HisprescriptionTO;
import com.ngari.recipe.hisprescription.service.IHisprescriptionService;
import ctd.util.annotation.RpcBean;

/**
 * @author： 0184/yu_yun
 * @date： 2018/6/28
 * @description： TODO
 * @version： 1.0
 */

@RpcBean("hosRecipeService")
public class HisprescriptionService implements IHisprescriptionService {

    @Override
    public HisprescriptionTO get(Object id) {
        return null;
    }

    @Override
    public RecipeCommonResTO createPrescription(HisprescriptionTO hisprescription) {
        return null;
    }

}
