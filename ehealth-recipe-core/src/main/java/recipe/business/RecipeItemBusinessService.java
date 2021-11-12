package recipe.business;

import com.ngari.recipe.entity.ItemList;
import com.ngari.recipe.vo.ItemListVO;
import ctd.persistence.bean.QueryResult;
import ctd.util.JSONUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.core.api.doctor.ITherapyItemBusinessService;
import recipe.manager.ItemListManager;

import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

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
    public boolean saveItemList(ItemList itemList) {
        if (CollectionUtils.isNotEmpty(itemListManager.findItemListByOrganIdAndItemNameOrCode(itemList.getOrganID(), itemList.getItemName(), itemList.getItemCode()))) {
            return false;
        }
        itemListManager.saveItemList(itemList);
        return true;
    }

    @Override
    public boolean updateItemList(ItemList itemList) {
        AtomicBoolean res = new AtomicBoolean(true);
        //只有修改项目编码和名称才需要校验
        if (itemList.getOrganID() != null && (StringUtils.isNotEmpty(itemList.getItemName()) || StringUtils.isNotEmpty(itemList.getItemName()))) {
            List<ItemList> itemListDbs = itemListManager.findItemListByOrganIdAndItemNameOrCode(itemList.getOrganID(), itemList.getItemName(), itemList.getItemCode());
            itemListDbs.forEach(itemListDb -> {
                if (itemListDb != null && !itemListDb.getId().equals(itemListDb.getId())) {
                    logger.info("updateItemList itemListDb:{}", JSONUtils.toString(itemListDb));
                    res.set(false);
                }
            });
        }
        if (!res.get()) {
            return false;
        }
        if (null == itemList.getGmtModified()) {
            itemList.setGmtModified(new Date());
        }
        itemListManager.updateItemList(itemList);
        return true;
    }

    @Override
    public ItemList getItemListById(ItemList itemList) {
        return itemListManager.getItemListById(itemList);
    }

    @Override
    public void batchUpdateItemList(List<ItemList> itemLists) {
        itemLists.forEach(itemList -> {
            if (itemList != null && itemList.getId() != null) {
                itemListManager.updateItemList(itemList);
            }
        });
    }

    @Override
    public List<ItemList> findItemListByOrganIdAndItemNameOrCode(Integer organId, String itemName, String itemCode) {
        return itemListManager.findItemListByOrganIdAndItemNameOrCode(organId, itemName, itemCode);
    }

    @Override
    public boolean checkItemList(ItemList itemList) {
        //true表示存在，false表示不存在除当前项目外其他项目（为了跟另一个先写的校验方法保持一致）
        AtomicBoolean res = new AtomicBoolean(false);
        List<ItemList> itemListDbs = itemListManager.findItemListByOrganIdAndItemNameOrCode(itemList.getOrganID(), itemList.getItemName(), itemList.getItemCode());
        itemListDbs.forEach(itemListDb -> {
            //存在一样的id false表示不存在
            if (itemListDb != null && !itemListDb.getId().equals(itemList.getId())) {
                logger.info("findItemListByOrganIdAndItemNameOrCode itemListDb:{}", JSONUtils.toString(itemListDb));
                res.set(true);
                return;
            }
        });
        return res.get();
    }


}
