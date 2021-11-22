package recipe.api.open;

import ctd.util.annotation.RpcService;
import recipe.vo.doctor.ItemListBean;

/**
 * @Author liuzj
 * @Date 2021/11/10 11:39
 * @Description
 */
public interface ITherapyItemOpenAtopService {

    /**
     * 判断机构下是否存在itemName或者itemCode
     * @param organId
     * @param itemName
     * @param itemCode
     * @return true表示存在，false表示不存在
     */
    @RpcService
    Boolean checkExistByOrganIdAndItemNameOrCode(Integer organId, String itemName, String itemCode);

    /**
     * 判断机构下是否存在itemName或者itemCode
     * @param organId
     * @param itemName
     * @param itemCode
     * @return true表示存在，false表示不存在
     */
    @RpcService
    Boolean checkExistByOrganIdAndItemNameOrCode2(Integer organId, String itemName, String itemCode);

    /**
     * 新增或更新诊疗项目
     * @param itemListBean
     */
    @RpcService
    void saveOrUpdateBean(ItemListBean itemListBean);
}
