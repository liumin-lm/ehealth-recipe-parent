package recipe.core.api;

import recipe.vo.doctor.ItemListVO;

import java.util.List;

/**
 * 基础数据-诊疗项目处理
 * @author yinsheng
 * @date 2021\8\21 0021 11:18
 */
public interface IBaseItemListBusinessService {

    /**
     * 搜索诊疗项目
     * @param itemListVO
     * @return
     */
    List<ItemListVO> searchItemListByKeyWord(ItemListVO itemListVO);

    void deleteItemListById(Integer id);

    void updateStatusById(Integer id, Integer status);
}
