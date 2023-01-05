package recipe.business;

import com.ngari.recipe.entity.ClinicCart;
import com.ngari.recipe.entity.FastRecipe;
import ctd.util.BeanUtils;
import eh.utils.BeanCopyUtils;
import eh.utils.ValidateUtil;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.core.api.IClinicCartBusinessService;
import recipe.dao.ClinicCartDAO;
import recipe.dao.FastRecipeDAO;
import recipe.vo.second.ClinicCartVO;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 购物车核心业务类
 *
 * @Description
 * @Author yzl
 * @Date 2022-07-14
 */
@Service
public class ClinicCartService implements IClinicCartBusinessService {

    @Autowired
    ClinicCartDAO clinicCartDAO;

    @Autowired
    FastRecipeDAO fastRecipeDAO;

    /**
     * 方便门诊、便捷购药 购物车列表查询
     *
     * @param organId
     * @param userId
     * @return
     */
    @Override
    public List<ClinicCartVO> findClinicCartsByOrganIdAndUserId(Integer organId, String userId, Integer workType) {
        List<ClinicCart> clinicCartList = clinicCartDAO.findClinicCartsByOrganIdAndUserId(organId, userId, workType);
        if (CollectionUtils.isNotEmpty(clinicCartList)) {
            for (ClinicCart clinicCart : clinicCartList) {
                if (!Integer.valueOf("2").equals(workType)) {
                    continue;
                }
                FastRecipe fastRecipe = fastRecipeDAO.get(clinicCart.getItemId());
                if (Objects.nonNull(fastRecipe)) {
                    clinicCart.setStockNum(fastRecipe.getStockNum());
                }
            }
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
        List<ClinicCart> clinicCartList = clinicCartDAO.findClinicCartsByParam(clinicCartVO);
        if (CollectionUtils.isEmpty(clinicCartList)) {
            clinicCart.setDeleteFlag(0);
            ClinicCart result = clinicCartDAO.save(clinicCart);
            return result.getId();
        } else {
            return 0;
        }
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
        if (ValidateUtil.nullOrZeroInteger(clinicCartVO.getAmount())) {
            return false;
        }
        if (Objects.nonNull(clinicCart)) {
            clinicCart.setAmount(clinicCartVO.getAmount());
            clinicCartDAO.update(clinicCart);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public Boolean deleteClinicCartByUserId(ClinicCartVO clinicCartVO) {
        clinicCartDAO.deleteClinicCartByUserId(clinicCartVO.getOrganId(), clinicCartVO.getUserId(), clinicCartVO.getWorkType());
        return true;
    }

}
