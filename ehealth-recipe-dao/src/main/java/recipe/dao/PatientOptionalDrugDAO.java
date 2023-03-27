package recipe.dao;

import com.ngari.recipe.entity.PatientOptionalDrug;
import ctd.persistence.annotation.DAOMethod;
import ctd.persistence.annotation.DAOParam;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.persistence.support.hibernate.template.AbstractHibernateStatelessResultAction;
import ctd.persistence.support.hibernate.template.HibernateSessionTemplate;
import ctd.persistence.support.hibernate.template.HibernateStatelessResultAction;
import ctd.util.annotation.RpcSupportDAO;
import org.apache.commons.collections.CollectionUtils;
import org.hibernate.Query;
import org.hibernate.StatelessSession;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


/**
 * @description： 患者自选药品 dao
 * @author： whf
 * @date： 2021-11-22 20:00
 */
@RpcSupportDAO
public abstract class PatientOptionalDrugDAO extends HibernateSupportDelegateDAO<PatientOptionalDrug> {

    /**
     * 根据复诊id获取患者自选药品
     *
     * @param clinicId
     * @return
     */
    @DAOMethod(sql = "from PatientOptionalDrug where clinicId= :clinicId ", limit = 0)
    public abstract List<PatientOptionalDrug> findPatientOptionalDrugByClinicId(@DAOParam("clinicId") Integer clinicId);


    public List<PatientOptionalDrug> findPatientOptionalDrugByClinicIdV1(Integer clinicId) {
        HibernateStatelessResultAction<List<PatientOptionalDrug>> action = new AbstractHibernateStatelessResultAction<List<PatientOptionalDrug>>() {
            @Override
            public void execute(StatelessSession ss) throws Exception {
                StringBuilder hql = new StringBuilder();
                hql.append("select id,drug_id organ_drug_code ,organ_id ,SUM(patient_drug_num) as patient_drug_num ,clinic_id ,create_time ,modified_time from patient_optional_drug where clinic_id = :clinicId");
                hql.append("group by organ_drug_code");
                Query q = ss.createSQLQuery(hql.toString());
                q.setParameter("clinicId", clinicId);
                List<Object[]> result = q.list();
                List<PatientOptionalDrug> backList = new ArrayList<>();
                if (CollectionUtils.isNotEmpty(result)) {
                    PatientOptionalDrug vo;
                    for (Object[] objs : result) {
                        vo = new PatientOptionalDrug();
                        vo.setId(objs[0] == null ? null : (Integer) objs[0]);
                        vo.setDrugId(objs[1] == null ? null : (Integer) objs[1]);
                        vo.setOrganDrugCode(objs[2] == null ? null : objs[2] + "");
                        vo.setOrganId(objs[3] == null ? null : (Integer) objs[3]);
                        vo.setPatientDrugNum(objs[4] == null ? null : (Integer) objs[4]);
                        vo.setClinicId(objs[5] == null ? null : (Integer) objs[5]);
                        vo.setCreateTime(objs[6] == null ? null : (Date) objs[6]);
                        vo.setModifiedTime(objs[7] == null ? null : (Date) objs[7]);
                        backList.add(vo);
                    }
                }
                setResult(backList);
            }
        };
        HibernateSessionTemplate.instance().execute(action);
        return action.getResult();
    }

}
