package recipe.business;

import com.ngari.recipe.entity.ItemList;
import com.ngari.recipe.vo.ItemListVO;
import ctd.persistence.bean.QueryResult;
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
    public List<ItemList> listItemList(ItemListVO itemListVO) {
        return itemListManager.findItemList(itemListVO.getOrganId(), itemListVO.getStatus(), itemListVO.getItemName(), itemListVO.getStart(), itemListVO.getLimit(), itemListVO.getId(), itemListVO.getItemCode());
    }

    @Override
    public QueryResult<ItemList> pageItemList(ItemListVO itemListVO) {
        QueryResult<ItemList> itemLists = itemListManager.pageItemList(itemListVO.getOrganId(), itemListVO.getStatus(), itemListVO.getItemName(), itemListVO.getStart(), itemListVO.getLimit(), itemListVO.getId(), itemListVO.getItemCode());
        return itemLists;
    }

    @Override
    public ItemList saveItemList(ItemList itemList) {
        return itemListManager.saveItemList(itemList);
    }

    @Override
    public void updateItemList(ItemList itemList) {
        itemListManager.updateItemList(itemList);
    }

    @Override
    public ItemList getItemListById(ItemList itemList) {
        return itemListManager.getItemListById(itemList);
    }

    @Override
    public List<ItemList> findItemListByOrganIdAndItemNameOrCode(Integer organId, String itemName, String itemCode) {
        return itemListManager.findItemListByOrganIdAndItemNameOrCode(organId, itemName, itemCode);
    }


}
