package recipe.core.api.doctor;

import com.ngari.recipe.entity.ItemList;
import com.ngari.recipe.vo.ItemListVO;

import java.util.List;

/**
 * 诊疗项目服务
 *
 * @author fuzi
 */
public interface ITherapyItemBusinessService {

    /**
     * 搜索诊疗项目
     *
     * @param itemListVO 诊疗项目
     * @return 诊疗项目列表
     */
    List<ItemListVO> listItemList(ItemListVO itemListVO);


    /**
     * 添加诊疗项目
     *
     * @param itemListVO
     * @return
     */
    ItemList saveItemList(ItemList itemListVO);

    /**
     * 更新诊疗项目
     *
     * @param itemList
     */
    void updateItemList(ItemList itemList);

    /**
     * 获取单个诊疗项目
     *
     * @param itemList
     * @return
     */
    ItemList getItemListById(ItemList itemList);
}
