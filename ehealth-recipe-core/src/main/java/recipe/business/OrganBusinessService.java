package recipe.business;

import com.alibaba.fastjson.JSON;
import com.ngari.base.scratchable.model.ScratchableBean;
import com.ngari.recipe.dto.EnterpriseStock;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeOrder;
import com.ngari.recipe.entity.Recipedetail;
import com.ngari.recipe.recipe.model.GiveModeButtonBean;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.client.IConfigurationClient;
import recipe.client.OrganClient;
import recipe.core.api.IOrganBusinessService;
import recipe.manager.ButtonManager;
import recipe.manager.OrderManager;
import recipe.manager.OrganDrugListManager;

import java.util.ArrayList;
import java.util.List;

@Service
public class OrganBusinessService extends BaseService implements IOrganBusinessService {
    @Autowired
    private ButtonManager buttonManager;
    @Autowired
    private OrganClient organClient;
    @Autowired
    private IConfigurationClient configurationClient;
    @Autowired
    private OrganDrugListManager organDrugListManager;
    @Autowired
    private OrderManager orderManager;

    @Override
    public List<Integer> getOrganForWeb() {
        List<Integer> organIds = organClient.findOrganIdsByCurrentClient();
        if (CollectionUtils.isNotEmpty(organIds)) {
            return organIds;
        }
        return organClient.findAllOrganIds();
    }

    @Override
    public List<GiveModeButtonBean> getOrganGiveModeConfig(Integer organId){
        List<GiveModeButtonBean> result = new ArrayList<>();
        List<ScratchableBean> scratchableBeans = configurationClient.getOrganGiveMode(organId);
        scratchableBeans.stream().filter(scratchableBean -> !"listItem".equals(scratchableBean.getBoxLink())).forEach(scratchableBean -> {
            GiveModeButtonBean giveModeButtonBean = new GiveModeButtonBean();
            giveModeButtonBean.setShowButtonKey(scratchableBean.getBoxLink());
            giveModeButtonBean.setShowButtonName(scratchableBean.getBoxTxt());
            result.add(giveModeButtonBean);
        });
        logger.info("OrganBusinessService getOrganGiveModeConfig result:{}.", JSON.toJSONString(result));
        return result;
    }


    @Override
    public EnterpriseStock organStock(Integer organId, List<Recipedetail> detailList) {
        Recipe recipe = new Recipe();
        recipe.setClinicOrgan(organId);
        return organDrugListManager.organStock(recipe, detailList);
    }


    @Override
    public boolean giveModeValidate(Integer orderId) {
        RecipeOrder recipeOrder = orderManager.getRecipeOrder(null, orderId);
        if (null == recipeOrder) {
            return false;
        }
        return giveModeValidate(recipeOrder.getOrganId(), recipeOrder.getGiveModeKey());
    }

    @Override
    public boolean giveModeValidate(Integer organId, String giveModeKey) {
        List<String> recipeTypes = configurationClient.getValueListCatch(organId, "patientRecipeUploadHis", null);
        if (CollectionUtils.isEmpty(recipeTypes)) {
            return false;
        }
        return recipeTypes.contains(giveModeKey);
    }

}
