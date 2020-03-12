package recipe.dao;

import com.ngari.recipe.entity.HisRecipe;
import ctd.persistence.annotation.DAOMethod;
import ctd.persistence.annotation.DAOParam;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.util.annotation.RpcSupportDAO;

import java.util.List;

/**
 * @author yinsheng
 * @date 2020\3\10 0010 20:23
 */
@RpcSupportDAO
public abstract class HisRecipeDAO extends HibernateSupportDelegateDAO<HisRecipe> {

    public HisRecipeDAO() {
        super();
        this.setEntityName(HisRecipe.class.getName());
        this.setKeyField("hisRecipeID");
    }

    @DAOMethod(sql = " From HisRecipe where clinicOrgan=:clinicOrgan and mpiId=:mpiId and status=:status ")
    public abstract List<HisRecipe> findHisRecipes(@DAOParam("clinicOrgan") int clinicOrgan, @DAOParam("mpiId") String mpiId, @DAOParam("status") int status);

}
