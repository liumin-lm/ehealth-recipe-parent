package recipe.dao;

import com.alibaba.druid.util.StringUtils;
import com.ngari.recipe.entity.AuditDrugList;
import com.ngari.recipe.entity.HospitalRecipe;
import ctd.persistence.annotation.DAOMethod;
import ctd.persistence.annotation.DAOParam;
import ctd.persistence.bean.QueryResult;
import ctd.persistence.exception.DAOException;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.persistence.support.hibernate.template.AbstractHibernateStatelessResultAction;
import ctd.persistence.support.hibernate.template.HibernateSessionTemplate;
import ctd.persistence.support.hibernate.template.HibernateStatelessResultAction;
import ctd.util.annotation.RpcSupportDAO;
import org.hibernate.Query;
import org.hibernate.StatelessSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;

/**
 * @author yinsheng
 * @date 2019\12\19 0015 21:06
 */
@RpcSupportDAO
public abstract class HospitalRecipeDAO extends HibernateSupportDelegateDAO<HospitalRecipe> {

    /**
     * LOGGER
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(HospitalRecipeDAO.class);

    public HospitalRecipeDAO() {
        super();
        this.setEntityName(AuditDrugList.class.getName());
        this.setKeyField("hospitalRecipeID");
    }


}
