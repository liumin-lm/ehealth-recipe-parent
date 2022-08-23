package recipe.core.api;

import recipe.vo.second.ClinicCartVO;

import java.util.List;

public interface IClinicCartBusinessService {

    /**
     * 方便门诊\便捷购药 购物车列表查询
     *
     * @param organId
     * @param userId
     * @return
     */
    List<ClinicCartVO> findClinicCartsByOrganIdAndUserId(Integer organId, String userId, Integer workType);

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

    /**
     * 购物车根据用户Id,机构id和业务场景删除数据
     *
     * @param clinicCartVO
     * @return
     */
    Boolean deleteClinicCartByUserId(ClinicCartVO clinicCartVO);
}
