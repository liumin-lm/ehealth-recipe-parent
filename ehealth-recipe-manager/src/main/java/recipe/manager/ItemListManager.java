package recipe.manager;

import com.alibaba.fastjson.JSON;
import com.ngari.recipe.entity.ItemList;
import ctd.util.JSONUtils;
import org.springframework.stereotype.Service;
import recipe.dao.ItemListDAO;

import javax.annotation.Resource;
import java.util.List;

/**
 * 项目列表
 *
 * @author yinsheng
 * @date 2021\8\21 0021 09:41
 */
@Service
public class ItemListManager extends BaseManager {

    @Resource
    private ItemListDAO itemListDAO;

    public List<ItemList> findItemList(Integer organId, Integer status, String itemName, int start, int limit) {
        List<ItemList> itemLists = itemListDAO.findItemList(organId, status, itemName, start, limit);
        logger.info("ItemListManager findItemList itemLists:{}.", JSON.toJSONString(itemLists));
        return itemLists;
    }

    public void updateItemListStatusById(Integer id, Integer status) {
        itemListDAO.updateStatus(id, status);
    }

    public void deleteItemListById(Integer id) {
        itemListDAO.delete(id);
    }

    public ItemList saveItemList(ItemList itemList) {
        ItemList itemList1 = itemListDAO.save(itemList);
        logger.info("saveItemList result:{}", JSONUtils.toString(itemList));
        return itemList1;
    }

    public void updateItemList(ItemList itemList) {
        itemListDAO.updateNonNullFieldByPrimaryKey(itemList);
    }

    public ItemList getItemListById(ItemList itemList) {
        return itemListDAO.get(itemList.getId());
    }
}
