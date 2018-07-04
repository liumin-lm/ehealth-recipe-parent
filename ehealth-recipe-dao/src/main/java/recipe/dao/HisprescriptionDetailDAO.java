package recipe.dao;

import com.ngari.recipe.entity.HisprescriptionDetail;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.util.annotation.RpcSupportDAO;

/**
 * @author： 0184/yu_yun
 * @date： 2018/6/28
 * @description：
 * @version： 1.0
 */
@RpcSupportDAO
public class HisprescriptionDetailDAO extends HibernateSupportDelegateDAO<HisprescriptionDetail> {

//    private static final Log LOGGER = LogFactory.getLog(HisprescriptionDAO.class);

    public HisprescriptionDetailDAO() {
        super();
        this.setEntityName(HisprescriptionDetail.class.getName());
        this.setKeyField("recipedetailId");
    }


}
