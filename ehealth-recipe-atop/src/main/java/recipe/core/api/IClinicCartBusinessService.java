package recipe.core.api;

import recipe.vo.second.ClinicCartVO;

import java.util.List;

public interface IClinicCartBusinessService {

    /**
     * 方便门诊购物车列表查询
     *
     * @param organId
     * @param userId
     * @return
     */
    List<ClinicCartVO> findClinicCartsByOrganIdAndUserId(Integer organId, String userId);

    /**
     * 方便门诊购物车新增
     *
     * @param clinicCartVO
     * @return
     */
    Integer addClinicCart(ClinicCartVO clinicCartVO);

    /**
     * 方便门诊购物车删除
     *
     * @param id
     * @return
     */
    Boolean deleteClinicCartByIds(List<Integer> id);

    /**
     * 方便门诊购物车 修改某一条记录（目前只需要修改数量）
     *
     * @param clinicCartVO
     * @return
     */
    Boolean updateClinicCartById(ClinicCartVO clinicCartVO);
}
