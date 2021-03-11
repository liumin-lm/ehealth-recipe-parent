package recipe.dao;

import com.ngari.recipe.entity.DrugList;
import com.ngari.recipe.entity.DrugSources;
import ctd.persistence.annotation.DAOMethod;
import ctd.persistence.annotation.DAOParam;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.util.annotation.RpcSupportDAO;

import java.util.List;

@RpcSupportDAO
public abstract class DrugSourcesDAO extends HibernateSupportDelegateDAO<DrugSources> {

    public DrugSourcesDAO() {
        super();
        this.setEntityName(DrugSources.class.getName());
        this.setKeyField("drugSourcesId");
    }



    @DAOMethod(sql = "from DrugSources where 1=1 ",limit=0)
    public abstract List<DrugSources> findAll();


    @DAOMethod(sql = "from DrugSources where drugSourcesName like:drugSourcesName ",limit=0)
    public abstract List<DrugSources> findByInput(@DAOParam("drugSourcesName") String drugSourcesName);

}
