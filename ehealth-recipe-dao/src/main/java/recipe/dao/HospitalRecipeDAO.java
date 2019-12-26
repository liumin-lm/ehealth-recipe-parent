package recipe.dao;


import com.ngari.recipe.entity.HospitalRecipe;
import ctd.persistence.annotation.DAOMethod;
import ctd.persistence.annotation.DAOParam;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.util.annotation.RpcSupportDAO;

import java.util.List;

/**
 * @author yinsheng
 * @date 2019\12\19 0015 21:06
 */
@RpcSupportDAO
public abstract class HospitalRecipeDAO extends HibernateSupportDelegateDAO<HospitalRecipe> {


    public HospitalRecipeDAO() {
        super();
        this.setEntityName(HospitalRecipe.class.getName());
        this.setKeyField("hospitalRecipeID");
    }

    @DAOMethod(sql = " from HospitalRecipe where certificate=:certificate order by hospitalRecipeID desc")
    public abstract List<HospitalRecipe> findByCertificate(@DAOParam("certificate") String certificate);
}
