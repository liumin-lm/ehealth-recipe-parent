package recipe.core.api;

import com.ngari.recipe.vo.ConfigStatusCheckVO;

import java.util.List;

/**
 * 配置状态获取
 *
 * @author fuzi
 */
public interface IConfigStatusBusinessService {

    /**
     * 根据位置查询状态数据
     *
     * @param location 位置
     * @return
     */
    List<ConfigStatusCheckVO> getConfigStatus(Integer location);

    /**
     * 根据位置与源状态查询数据
     *
     * @param location 位置
     * @param source   源状态
     * @return
     */
    List<ConfigStatusCheckVO> findByLocationAndSource(Integer location, Integer source);

    /**
     * 获取处方是否展示生产厂商
     * @param openRecipeHideDrugManufacturer
     * @return
     */
    Boolean getOpenRecipeHideDrugManufacturer(Integer organId,String openRecipeHideDrugManufacturer);
}
