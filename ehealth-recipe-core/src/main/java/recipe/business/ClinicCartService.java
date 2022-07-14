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

/**
 * @Description
 * @Author yzl
 * @Date 2022-07-14
 */
@Service
public class ClinicCartService implements IClinicCartBusinessService {

    @Autowired
    ClinicCartDAO clinicCartDAO;

    @Override
    public List<ClinicCartVO> findClinicCartsByOrganIdAndUserId(Integer organId, String userId) {
        List<ClinicCart> clinicCartList = clinicCartDAO.findClinicCartsByOrganIdAndUserId(organId, userId);
        if (CollectionUtils.isNotEmpty(clinicCartList)) {
            return BeanCopyUtils.copyList(clinicCartList, ClinicCartVO::new);
        } else {
            return new ArrayList<>();
        }
    }

    @Override
    public Integer addClinicCart(ClinicCartVO clinicCartVO) {
        ClinicCart clinicCart = BeanUtils.map(clinicCartVO, ClinicCart.class);
        ClinicCart result = clinicCartDAO.save(clinicCart);
        return result.getId();
    }
}
