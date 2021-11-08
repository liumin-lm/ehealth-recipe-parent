package recipe.business;

import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.entity.ItemList;
import com.ngari.recipe.vo.ItemListVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.client.OrganClient;
import recipe.client.PatientClient;
import recipe.core.api.doctor.ITherapyItemBusinessService;
import recipe.manager.*;

import java.util.List;

/**
 * 诊疗项目
 *
 * @author liumin
 */
@Service
public class RecipeItemBusinessService extends BaseService implements ITherapyItemBusinessService {
    @Autowired
    private RecipeManager recipeManager;
    @Autowired
    private RecipeTherapyManager recipeTherapyManager;
    @Autowired
    private RecipeDetailManager recipeDetailManager;
    @Autowired
    private OrganDrugListManager organDrugListManager;
    @Autowired
    private PatientClient patientClient;
    @Autowired
    private OrganClient organClient;
    @Autowired
    private ItemListManager itemListManager;

    @Override
    public List<ItemListVO> listItemList(ItemListVO itemListVO) {
        List<ItemList> itemLists = itemListManager.findItemList(itemListVO.getOrganId(), itemListVO.getStatus(), itemListVO.getItemName(), itemListVO.getStart(), itemListVO.getLimit());
        return ObjectCopyUtils.convert(itemLists, ItemListVO.class);
    }

    @Override
    public void saveItemList(ItemList itemList) {
        itemListManager.saveItemList(itemList);
    }

}
