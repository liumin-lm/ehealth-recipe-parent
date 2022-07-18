package recipe.business;

import com.ngari.recipe.entity.ItemList;
import com.ngari.recipe.vo.CheckItemListVo;
import com.ngari.recipe.vo.ItemListVO;
import ctd.persistence.bean.QueryResult;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.core.api.doctor.ITherapyItemBusinessService;
import recipe.manager.RecipeTherapyManager;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 诊疗项目服务实现类
 *
 * @author liumin
 */
@Service
public class TherapyItemBusinessService extends BaseService implements ITherapyItemBusinessService {

    public final String ITEM_NAME = "itemName";
    public final String ITEM_CODE = "itemCode";

    @Autowired
    private RecipeTherapyManager recipeTherapyManager;

    @Override
    public List<ItemList> listItemList(ItemListVO itemListVO) {
        return recipeTherapyManager.findItemList(itemListVO.getOrganId(), itemListVO.getStatus(), itemListVO.getItemName(), itemListVO.getStart(), itemListVO.getLimit(), itemListVO.getId(), itemListVO.getItemCode());
    }

    @Override
    public QueryResult<ItemList> pageItemList(ItemListVO itemListVO) {
        QueryResult<ItemList> itemLists = recipeTherapyManager.pageItemList(itemListVO.getOrganId(), itemListVO.getStatus(), itemListVO.getItemName(), itemListVO.getStart(), itemListVO.getLimit(), itemListVO.getId(), itemListVO.getItemCode());
        return itemLists;
    }

    @Override
    public boolean saveItemList(ItemList itemList) {
        if (StringUtils.isNotEmpty(itemList.getItemName())) {
            List<ItemList> resByItemName = recipeTherapyManager.findItemListByOrganIdAndItemNameOrCode(itemList.getOrganID(), itemList.getItemName(), null);
            if (CollectionUtils.isNotEmpty(resByItemName)) {
                return false;
            }
        }
        if (StringUtils.isNotEmpty(itemList.getItemCode())) {
            List<ItemList> resByItemName = recipeTherapyManager.findItemListByOrganIdAndItemNameOrCode(itemList.getOrganID(), null, itemList.getItemCode());
            if (CollectionUtils.isNotEmpty(resByItemName)) {
                return false;
            }
        }
        recipeTherapyManager.saveItemList(itemList);
        return true;
    }

    @Override
    public boolean updateItemList(ItemList itemList) {
        AtomicBoolean res = new AtomicBoolean(true);
        if (StringUtils.isNotEmpty(itemList.getItemName())) {
            List<ItemList> itemListsByItemName = recipeTherapyManager.findItemListByOrganIdAndItemNameOrCode(itemList.getOrganID(), itemList.getItemName(), null);
            itemListsByItemName.forEach(itemListByItemName -> {
                //存在一条id与当前传入itemList.getId不相同的就为false
                if (itemListByItemName != null && !itemList.getId().equals(itemListByItemName.getId())) {
                    res.set(false);
                }
            });
        }
        if (StringUtils.isNotEmpty(itemList.getItemCode())) {
            List<ItemList> itemListsByItemCode = recipeTherapyManager.findItemListByOrganIdAndItemNameOrCode(itemList.getOrganID(), null, itemList.getItemCode());
            itemListsByItemCode.forEach(itemListByItemCode -> {
                //存在一条id与当前传入itemList.getId不相同的就为false
                if (itemListByItemCode != null && !itemList.getId().equals(itemListByItemCode.getId())) {
                    res.set(false);
                }
            });
        }
        if (!res.get()) {
            return false;
        }
        itemList.setGmtModified(new Date());
        recipeTherapyManager.updateItemList(itemList);
        return true;
    }

    @Override
    public ItemList getItemListById(ItemList itemList) {
        return recipeTherapyManager.getItemListById(itemList);
    }

    @Override
    public void batchUpdateItemList(List<ItemList> itemLists) {
        itemLists.forEach(itemList -> {
            if (itemList != null && itemList.getId() != null) {
                recipeTherapyManager.updateItemList(itemList);
            }
        });
    }

    @Override
    public List<ItemList> findItemListByOrganIdAndItemNameOrCode(Integer organId, String itemName, String itemCode) {
        return recipeTherapyManager.findItemListByOrganIdAndItemNameOrCode(organId, itemName, itemCode);
    }

    @Override
    public List<ItemList> findItemListByOrganIdAndItemNameOrCode2(Integer organId, String itemName, String itemCode) {
        return recipeTherapyManager.findItemListByOrganIdAndItemNameOrCode2(organId, itemName, itemCode);
    }

    @Override
    public CheckItemListVo checkItemList(ItemList itemList) {
        CheckItemListVo checkItemListVo = new CheckItemListVo();
        List<String> cause = Arrays.asList("", "");
        AtomicBoolean result = new AtomicBoolean(true);
        //true表示可向下执行流程 false抛出错误
        AtomicBoolean res = new AtomicBoolean(true);
        if (itemList.getId() != null) {
            //修改
            if (StringUtils.isNotEmpty(itemList.getItemName())) {
                List<ItemList> itemListsByItemName = recipeTherapyManager.findItemListByOrganIdAndItemNameOrCode(itemList.getOrganID(), itemList.getItemName(), null);
                itemListsByItemName.forEach(itemListByItemName -> {
                    //存在一条id与当前传入itemList.getId不相同的就为false
                    if (itemListByItemName != null && !itemList.getId().equals(itemListByItemName.getId())) {
                        result.set(false);
                        cause.set(0, ITEM_NAME);
                    }
                });
            }
            if (StringUtils.isNotEmpty(itemList.getItemCode())) {
                List<ItemList> itemListsByItemCode = recipeTherapyManager.findItemListByOrganIdAndItemNameOrCode(itemList.getOrganID(), null, itemList.getItemCode());
                itemListsByItemCode.forEach(itemListByItemCode -> {
                    //存在一条id与当前传入itemList.getId不相同的就为false
                    if (itemListByItemCode != null && !itemList.getId().equals(itemListByItemCode.getId())) {
                        result.set(false);
                        cause.set(1, ITEM_CODE);
                    }
                });
            }
        } else {
            //新增
            if (StringUtils.isNotEmpty(itemList.getItemName())) {
                List<ItemList> resByItemName = recipeTherapyManager.findItemListByOrganIdAndItemNameOrCode(itemList.getOrganID(), itemList.getItemName(), null);
                if (CollectionUtils.isNotEmpty(resByItemName)) {
                    result.set(false);
                    cause.set(0, ITEM_NAME);
                }
            }
            if (StringUtils.isNotEmpty(itemList.getItemCode())) {
                List<ItemList> resByItemName = recipeTherapyManager.findItemListByOrganIdAndItemNameOrCode(itemList.getOrganID(), null, itemList.getItemCode());
                if (CollectionUtils.isNotEmpty(resByItemName)) {
                    result.set(false);
                    cause.set(1, ITEM_CODE);
                }
            }
        }
        checkItemListVo.setCause(cause);
        checkItemListVo.setResult(result.get());
        return checkItemListVo;
    }


}
