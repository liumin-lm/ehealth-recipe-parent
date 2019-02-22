package recipe.dao;

import com.ngari.recipe.entity.DrugToolUser;
import ctd.persistence.annotation.DAOMethod;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.util.annotation.RpcSupportDAO;

/**
 * created by shiyuping on 2019/2/18
 */
@RpcSupportDAO
public abstract class DrugToolUserDAO extends HibernateSupportDelegateDAO<DrugToolUser> {
    public DrugToolUserDAO() {
        super();
        this.setEntityName(DrugToolUser.class.getName());
        this.setKeyField("id");
    }

    /**
     * 根据id获取
     * @param id
     * @return
     */
    @DAOMethod
    public abstract DrugToolUser getById(int id);

    @DAOMethod
    public abstract DrugToolUser getByMobile(String mobile);
}
