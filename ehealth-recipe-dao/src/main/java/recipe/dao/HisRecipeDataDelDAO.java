package recipe.dao;


import com.ngari.recipe.entity.HisRecipeDataDel;

import ctd.persistence.annotation.DAOMethod;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.util.annotation.RpcSupportDAO;

/**
 * @author zgy
 * @date 2021\11\10
 */
@RpcSupportDAO
public class HisRecipeDataDelDAO extends HibernateSupportDelegateDAO<HisRecipeDataDel> {

    public HisRecipeDataDelDAO() {
        super();
        this.setEntityName(HisRecipeDataDel.class.getName());
        this.setKeyField("id");
    }


}
