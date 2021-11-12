package recipe.core.api.doctor;

import com.ngari.recipe.entity.ItemList;
import com.ngari.recipe.vo.CheckItemListVo;
import com.ngari.recipe.vo.ItemListVO;
import ctd.persistence.bean.QueryResult;

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
    List<ItemList> listItemList(ItemListVO itemListVO);

    QueryResult<ItemList> pageItemList(ItemListVO itemListVO);

    /**
     * 添加诊疗项目
     *
     * @param itemListVO
     * @return
     */
    boolean saveItemList(ItemList itemListVO);

    /**
     * 更新诊疗项目
     *
     * @param itemList
     */
    boolean updateItemList(ItemList itemList);

    /**
     * 获取单个诊疗项目
     *
     * @param itemList
     * @return
     */
    ItemList getItemListById(ItemList itemList);

    void batchUpdateItemList(List<ItemList> itemLists);

    /**
     * 判断机构下是否已存在项目名称、项目编码
     *
     * @param organId
     * @param itemName
     * @param itemCode
     * @return
     */
    List<ItemList> findItemListByOrganIdAndItemNameOrCode(Integer organId, String itemName, String itemCode);

    CheckItemListVo checkItemList(ItemList itemList);
}
