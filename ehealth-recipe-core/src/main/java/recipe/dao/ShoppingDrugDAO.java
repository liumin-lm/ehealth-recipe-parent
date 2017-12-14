package recipe.dao;

import com.ngari.recipe.entity.ShoppingDrug;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.util.annotation.RpcSupportDAO;

@RpcSupportDAO
public class ShoppingDrugDAO extends HibernateSupportDelegateDAO<ShoppingDrug> {
    public ShoppingDrugDAO() {
        super();
        this.setEntityName(ShoppingDrug.class.getName());
        this.setKeyField("id");
    }
}
