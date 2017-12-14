package recipe.dao;

import com.ngari.recipe.entity.ShoppingOrder;
import ctd.persistence.annotation.DAOMethod;
import ctd.persistence.annotation.DAOParam;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.util.annotation.RpcSupportDAO;

@RpcSupportDAO
public abstract class ShoppingOrderDAO extends HibernateSupportDelegateDAO<ShoppingOrder> {
    public ShoppingOrderDAO() {
        super();
        this.setEntityName(ShoppingOrder.class.getName());
        this.setKeyField("orderId");
    }

    @DAOMethod(sql = "from ShoppingOrder where mpiId=:mpiId and orderCode=:orderCode")
    public abstract ShoppingOrder getByMpiIdAndOrderCode(@DAOParam("mpiId") String mpiId, @DAOParam("orderCode") String orderCode);
    

}
