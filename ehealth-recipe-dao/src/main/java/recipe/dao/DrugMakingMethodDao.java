package recipe.dao;

import com.google.common.collect.Maps;
import com.ngari.recipe.drug.model.DrugMakingMethodBean;
import com.ngari.recipe.entity.DrugMakingMethod;
import ctd.dictionary.DictionaryItem;
import ctd.persistence.annotation.DAOMethod;
import ctd.persistence.annotation.DAOParam;
import ctd.persistence.bean.QueryResult;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.persistence.support.hibernate.template.AbstractHibernateStatelessResultAction;
import ctd.persistence.support.hibernate.template.HibernateSessionTemplate;
import ctd.persistence.support.hibernate.template.HibernateStatelessResultAction;
import ctd.util.annotation.RpcSupportDAO;
import org.hibernate.Query;
import org.hibernate.StatelessSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

@RpcSupportDAO
public abstract class DrugMakingMethodDao extends HibernateSupportDelegateDAO<DrugMakingMethod> {
    public static final Logger log = LoggerFactory.getLogger(DrugMakingMethod.class);
    public DrugMakingMethodDao() {
        super();
        this.setEntityName(DrugMakingMethod.class.getName());
        this.setKeyField("methodId");
    }

    @DAOMethod(sql = "from DrugMakingMethod where organId =:organId", limit = 0)
    public abstract List<DrugMakingMethodBean> findAllDrugMakingMethodByOrganId(@DAOParam("organId")Integer organId);
}
