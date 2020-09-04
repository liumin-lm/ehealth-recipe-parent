package recipe.dao;


import com.ngari.recipe.entity.Pharmacy;
import ctd.persistence.annotation.DAOMethod;
import ctd.persistence.annotation.DAOParam;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.persistence.support.hibernate.template.AbstractHibernateStatelessResultAction;
import ctd.persistence.support.hibernate.template.HibernateSessionTemplate;
import ctd.persistence.support.hibernate.template.HibernateStatelessResultAction;
import ctd.util.annotation.RpcSupportDAO;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Query;
import org.hibernate.StatelessSession;
import recipe.dao.bean.PatientRecipeBean;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
    public abstract List<Pharmacy> findByDrugsenterpriseIdAndStatus(int drugsenterpriseId, int status);

    public List<Pharmacy> findByDrugsenterpriseIdAndStatusAndRangeAndLongitudeAndLatitude(final Integer id, final Object range, final double longitude, final double latitude){
        HibernateStatelessResultAction<List<Pharmacy>> action = new AbstractHibernateStatelessResultAction<List<Pharmacy>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder();
                hql.append("select * from cdr_pharmacy cp where cp.status = 1 and cp.drugsenterpriseId = :enterpriseId and " +
                        "round(6378.138*2*asin(sqrt(pow(sin( (cp.pharmacyLatitude*pi()/180 - :latitude*pi()/180)/2),2)+cos(cp.pharmacyLatitude*pi()/180)*cos(:latitude*pi()/180)* pow(sin( (cp.pharmacyLongitude*pi()/180 - :longitude*pi()/180)/2),2)))) <= :range");
                Query q = ss.createSQLQuery(hql.toString());
                q.setParameter("latitude", latitude);
                q.setParameter("enterpriseId", id);
                q.setParameter("longitude", longitude);
                q.setParameter("range", range);
                List<Object[]> result = q.list();
                List<Pharmacy> backList = new ArrayList<>(result.size());
                Pharmacy pharmacy;
                if (CollectionUtils.isNotEmpty(result)) {
                    for (Object[] oj : result) {
                        pharmacy = new Pharmacy();
                        backList.add(pharmacy);
                        pharmacy.setPharmacyId(Integer.parseInt(oj[0].toString()));
                        pharmacy.setDrugsenterpriseId(Integer.parseInt(oj[1].toString()));
                        pharmacy.setPharmacyCode(oj[2].toString());
                        pharmacy.setPharmacyName(oj[3].toString());
                        pharmacy.setPharmacyAddress(oj[4].toString());
                        pharmacy.setPharmacyLongitude(oj[5].toString());
                        pharmacy.setPharmacyLatitude(oj[6].toString());
                        pharmacy.setStatus("true".equals(oj[7].toString()) ? 1 : 0);
                        SimpleDateFormat format = new SimpleDateFormat("yyyy-M-dd HH:mm:ss");
                        try {
                            pharmacy.setCreateTime(format.parse(oj[8].toString()));
                            pharmacy.setLastModify(format.parse(oj[9].toString()));
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                    }

                }
                setResult(backList);
            }
        };

        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

    @DAOMethod(sql = "from Pharmacy where 1=1 ")
    public abstract List<Pharmacy> findAll(@DAOParam(pageStart = true) int start,
                                           @DAOParam(pageLimit = true) int limit);

    @DAOMethod(sql = "from Pharmacy where status=1 ",limit = 0)
    public abstract List<Pharmacy> find1();

    @DAOMethod(sql = "from Pharmacy where drugsenterpriseId=:drugsenterpriseId ")
    public abstract List<Pharmacy> findByDepId(@DAOParam("drugsenterpriseId") Integer drugsenterpriseId);

}
