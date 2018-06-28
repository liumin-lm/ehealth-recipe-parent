package recipe.dao;

import com.ngari.recipe.entity.Hisprescription;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.util.annotation.RpcSupportDAO;

/**
 * @author： 0184/yu_yun
 * @date： 2018/6/28
 * @description：
 * @version： 1.0
 */
@RpcSupportDAO
public class HisprescriptionDAO extends HibernateSupportDelegateDAO<Hisprescription> {

//    private static final Log LOGGER = LogFactory.getLog(HisprescriptionDAO.class);

    public HisprescriptionDAO() {
        super();
        this.setEntityName(Hisprescription.class.getName());
        this.setKeyField("recipeId");
    }


}
