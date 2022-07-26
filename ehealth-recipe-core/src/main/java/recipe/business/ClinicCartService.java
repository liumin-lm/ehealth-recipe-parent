package recipe.business;

import com.ngari.recipe.entity.ClinicCart;
import ctd.util.BeanUtils;
import eh.utils.BeanCopyUtils;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.core.api.IClinicCartBusinessService;
import recipe.dao.ClinicCartDAO;
import recipe.vo.second.ClinicCartVO;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @Description
 * @Author yzl
 * @Date 2022-07-14
 */
@Service
public class ClinicCartService implements IClinicCartBusinessService {

    @Autowired
    ClinicCartDAO clinicCartDAO;

    /**
     * 方便门诊购物车列表查询
     *
     * @param organId
     * @param userId
     * @return
     */
    @Override
    public List<ClinicCartVO> findClinicCartsByOrganIdAndUserId(Integer organId, String userId) {
        List<ClinicCart> clinicCartList = clinicCartDAO.findClinicCartsByOrganIdAndUserId(organId, userId);
        if (CollectionUtils.isNotEmpty(clinicCartList)) {
            return BeanCopyUtils.copyList(clinicCartList, ClinicCartVO::new);
        } else {
            return new ArrayList<>();
        }
    }

    /**
     * 方便门诊购物车列表新增
     *
     * @param clinicCartVO
     * @return
     */
    @Override
    public Integer addClinicCart(ClinicCartVO clinicCartVO) {
        ClinicCart clinicCart = BeanUtils.map(clinicCartVO, ClinicCart.class);
        clinicCart.setDeleteFlag(0);
        ClinicCart result = clinicCartDAO.save(clinicCart);
        return result.getId();
    }

    /**
     * 方便门诊购物车列表删除
     *
     * @param ids
     * @return
     */
    @Override
    public Boolean deleteClinicCartByIds(List<Integer> ids) {
        clinicCartDAO.deleteClinicCartByIds(ids, 1);
        return true;
    }

    /**
     * 方便门诊购物车列表更新
     *
     * @param clinicCartVO
     * @return
     */
    @Override
    public Boolean updateClinicCartById(ClinicCartVO clinicCartVO) {
        ClinicCart clinicCart = clinicCartDAO.get(clinicCartVO.getId());
        if (Objects.nonNull(clinicCart)) {
            clinicCart.setAmount(clinicCartVO.getAmount());
            clinicCartDAO.update(clinicCart);
            return true;
        } else {
            return false;
        }
    }

}
