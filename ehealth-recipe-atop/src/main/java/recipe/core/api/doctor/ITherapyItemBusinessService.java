package recipe.core.api.doctor;

import com.ngari.recipe.entity.ItemList;
import com.ngari.recipe.vo.ItemListVO;

import java.util.List;

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
    void saveItemList(ItemList itemListVO);

    /**
     * 更新诊疗项目
     *
     * @param itemList
     */
    void updateItemList(ItemList itemList);
}
