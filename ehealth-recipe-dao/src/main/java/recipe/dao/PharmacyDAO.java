package recipe.dao;


import com.ngari.recipe.entity.Pharmacy;
import ctd.persistence.annotation.DAOMethod;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.persistence.support.hibernate.template.AbstractHibernateStatelessResultAction;
import ctd.persistence.support.hibernate.template.HibernateSessionTemplate;
import ctd.persistence.support.hibernate.template.HibernateStatelessResultAction;
import ctd.util.annotation.RpcSupportDAO;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Query;
import org.hibernate.StatelessSession;

import java.util.List;


@RpcSupportDAO
public abstract class PharmacyDAO extends HibernateSupportDelegateDAO<Pharmacy> {

    private static final Log LOGGER = LogFactory.getLog(PharmacyDAO.class);

    public PharmacyDAO() {
        super();
        this.setEntityName(Pharmacy.class.getName());
        this.setKeyField("pharmacyId");
    }
     /**
      * @method  findByDrugsenterpriseId
      * @description 返回药企下的药店
      * @date: 2019/7/10
      * @author: JRK
      * @param drugsenterpriseId 药企id
      * @return
      */
    @DAOMethod
    public abstract List<Pharmacy> findByDrugsenterpriseId(int drugsenterpriseId);

    public List<Pharmacy> findByDrugsenterpriseIdAndRangeAndLongitudeAndLatitude(final Integer id, final Object range, final double longitude, final double latitude){
        HibernateStatelessResultAction<List<Pharmacy>> action = new AbstractHibernateStatelessResultAction<List<Pharmacy>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder();
                hql.append("select * from cdr_pharmacy cp where cp.drugsenterpriseId = :enterpriseId and " +
                        "round(6378.138*2*asin(sqrt(pow(sin( (cp.latitude*pi()/180 - :latitude*pi()/180)/2),2)+cos(cp.latitude*pi()/180)*cos(:latitude*pi()/180)* pow(sin( (cp.longitude*pi()/180 - :longitude*pi()/180)/2),2)))) <= :range");
                Query q = ss.createSQLQuery(hql.toString());
                q.setParameter("latitude", latitude);
                q.setParameter("enterpriseId", id);
                q.setParameter("longitude", longitude);
                q.setParameter("range", range);
                setResult(q.list());
            }
        };

        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

}
