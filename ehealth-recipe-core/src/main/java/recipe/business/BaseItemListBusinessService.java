package recipe.business;

import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.entity.ItemList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.core.api.IBaseItemListBusinessService;
import recipe.manager.ItemListManager;
import recipe.vo.doctor.ItemListVO;

import java.util.List;

/**
 * 基础数据-诊疗项目处理
 * @author yinsheng
 * @date 2021\8\21 0021 11:21
 */
@Service
public class BaseItemListBusinessService implements IBaseItemListBusinessService{

    @Autowired
    private ItemListManager itemListManager;

    @Override
    public List<ItemListVO> searchItemListByKeyWord(ItemListVO itemListVO) {
        List<ItemList> itemLists = itemListManager.findItemList(itemListVO.getItemName(), itemListVO.getStart(), itemListVO.getLimit());
        return ObjectCopyUtils.convert(itemLists, ItemListVO.class);
    }

    @Override
    public void deleteItemListById(Integer id) {
        itemListManager.deleteItemListById(id);
    }

    @Override
    public void updateStatusById(Integer id, Integer status) {
        itemListManager.updateItemListStatusById(id, status);
    }
}
