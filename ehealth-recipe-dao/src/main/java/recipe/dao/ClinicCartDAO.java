package recipe.dao;

import com.ngari.recipe.entity.ClinicCart;
import ctd.persistence.annotation.DAOMethod;
import ctd.persistence.annotation.DAOParam;
import ctd.persistence.exception.DAOException;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.persistence.support.hibernate.template.AbstractHibernateStatelessResultAction;
import ctd.persistence.support.hibernate.template.HibernateSessionTemplate;
import ctd.persistence.support.hibernate.template.HibernateStatelessResultAction;
import ctd.util.annotation.RpcSupportDAO;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Query;
import org.hibernate.StatelessSession;
import recipe.vo.second.ClinicCartVO;

import java.util.List;
import java.util.Objects;

/**
 * @Description
 * @Author yzl
 * @Date 2022-07-14
 */
@RpcSupportDAO
public abstract class ClinicCartDAO extends HibernateSupportDelegateDAO<ClinicCart> {

    public ClinicCartDAO() {
        super();
        this.setEntityName(ClinicCart.class.getName());
        this.setKeyField("id");
    }

    @DAOMethod(sql = "FROM ClinicCart WHERE organId = :organId AND userId = :userId AND deleteFlag = 0 AND workType = :workType", limit = 0)
    public abstract List<ClinicCart> findClinicCartsByOrganIdAndUserId(@DAOParam("organId") Integer organId,
                                                                       @DAOParam("userId") String userId,
                                                                       @DAOParam("workType") Integer workType);

    @DAOMethod(sql = "UPDATE ClinicCart SET deleteFlag = :deleteFlag WHERE id IN (:ids)")
    public abstract void deleteClinicCartByIds(@DAOParam("ids") List<Integer> ids,
                                               @DAOParam("deleteFlag") Integer deleteFlag);

    @DAOMethod(sql = "UPDATE ClinicCart SET deleteFlag = 1 WHERE organId = :organId AND userId = :userId AND workType = :workType AND deleteFlag = 0")
    public abstract void deleteClinicCartByUserId(@DAOParam("organId") Integer organId,
                                                  @DAOParam("userId") String userId,
                                                  @DAOParam("workType") Integer workType);

    public List<ClinicCart> findClinicCartsByParam(ClinicCartVO clinicCartVO) {
        HibernateStatelessResultAction<List<ClinicCart>> action = new AbstractHibernateStatelessResultAction<List<ClinicCart>>() {
            @Override
            public void execute(StatelessSession ss) throws DAOException {
                StringBuilder hql = new StringBuilder("FROM ClinicCart where deleteFlag = 0 ");
                if (Objects.nonNull(clinicCartVO.getOrganId())) {
                    hql.append(" AND organId = :organId");
                }
                if (StringUtils.isNotEmpty(clinicCartVO.getUserId())) {
                    hql.append(" AND userId = :userId");
                }
                if (StringUtils.isNotEmpty(clinicCartVO.getItemId())) {
                    hql.append(" AND itemId = :itemId");
                }
                if (Objects.nonNull(clinicCartVO.getItemType())) {
                    hql.append(" AND itemType = :itemType");
                }
                if (Objects.nonNull(clinicCartVO.getWorkType())) {
                    hql.append(" AND workType = :workType");
                }
                if (Objects.nonNull(clinicCartVO.getPharmacyCode())) {
                    hql.append(" AND pharmacyCode = :pharmacyCode");
                }
                hql.append(" order by id desc");
                Query query = ss.createQuery(hql.toString());

                if (Objects.nonNull(clinicCartVO.getOrganId())) {
                    query.setParameter("organId", clinicCartVO.getOrganId());
                }
                if (StringUtils.isNotEmpty(clinicCartVO.getUserId())) {
                    query.setParameter("userId", clinicCartVO.getUserId());
                }
                if (StringUtils.isNotEmpty(clinicCartVO.getItemId())) {
                    query.setParameter("itemId", clinicCartVO.getItemId());
                }
                if (Objects.nonNull(clinicCartVO.getItemType())) {
                    query.setParameter("itemType", clinicCartVO.getItemType());
                }
                if (Objects.nonNull(clinicCartVO.getWorkType())) {
                    query.setParameter("workType", clinicCartVO.getWorkType());
                }
                if (Objects.nonNull(clinicCartVO.getPharmacyCode())) {
                    query.setParameter("pharmacyCode", clinicCartVO.getPharmacyCode());
                }
                setResult(query.list());
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }
}

