package recipe.dao;

import com.ngari.recipe.entity.RecipeParameter;
import ctd.persistence.annotation.DAOMethod;
import ctd.persistence.annotation.DAOParam;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.util.annotation.RpcSupportDAO;

/**
 * 处方参数信息
 */
@RpcSupportDAO
public abstract class RecipeParameterDao extends HibernateSupportDelegateDAO<RecipeParameter> {

    public RecipeParameterDao() {
        super();
        this.setEntityName(RecipeParameter.class.getName());
        this.setKeyField("paramName");
    }

    @DAOMethod(sql = "select paramValue from RecipeParameter where paramName=:paramName")
    public abstract String getByName(@DAOParam("paramName")String paramName);
}



