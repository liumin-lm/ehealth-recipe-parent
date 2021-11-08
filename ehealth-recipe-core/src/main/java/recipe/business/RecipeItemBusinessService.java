package recipe.business;

import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.entity.ItemList;
import com.ngari.recipe.vo.ItemListVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.core.api.doctor.ITherapyItemBusinessService;
import recipe.manager.ItemListManager;

import java.util.List;

/**
 * 诊疗项目服务实现类
 *
 * @author liumin
 */
@Service
public class RecipeItemBusinessService extends BaseService implements ITherapyItemBusinessService {

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

    @Override
    public void updateItemList(ItemList itemList) {
        itemListManager.updateItemList(itemList);
    }

    @Override
    public ItemList getItemListById(ItemList itemList) {
        return itemListManager.getItemListById(itemList);
    }

}
