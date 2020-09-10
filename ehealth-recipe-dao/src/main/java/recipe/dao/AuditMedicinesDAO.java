package recipe.dao;

import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.audit.model.AuditMedicineIssueDTO;
import com.ngari.recipe.audit.model.AuditMedicinesDTO;
import com.ngari.recipe.entity.AuditMedicineIssue;
import com.ngari.recipe.entity.AuditMedicines;
import ctd.persistence.DAOFactory;
import ctd.persistence.annotation.DAOMethod;
import ctd.persistence.annotation.DAOParam;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.persistence.support.hibernate.template.AbstractHibernateStatelessResultAction;
import ctd.persistence.support.hibernate.template.HibernateSessionTemplate;
import ctd.persistence.support.hibernate.template.HibernateStatelessResultAction;
import ctd.util.annotation.RpcSupportDAO;
import org.apache.commons.collections.CollectionUtils;
import org.hibernate.StatelessSession;
import org.joda.time.DateTime;

import java.util.Date;
import java.util.List;

/**
 * created by shiyuping on 2018/11/23
 */
@RpcSupportDAO
public abstract class AuditMedicinesDAO extends HibernateSupportDelegateDAO<AuditMedicines> {

    public AuditMedicinesDAO() {
        super();
        this.setEntityName(AuditMedicines.class.getName());
        this.setKeyField("id");
    }

    @DAOMethod(sql = "from AuditMedicines where recipeId =:recipeId and logicalDeleted = 0 and status = 1")
    public abstract List<AuditMedicines> findMedicinesByRecipeId(@DAOParam("recipeId") Integer recipeId);

    @DAOMethod(sql = "select count(*) from AuditMedicines where recipeId =:recipeId and logicalDeleted = 0 and status = 1")
    public abstract Long getCountByRecipeId(@DAOParam("recipeId") Integer recipeId);

    @DAOMethod(sql = "update AuditMedicines set logicalDeleted = 1, status = 0 where recipeId =:recipeId and status = 1")
    public abstract void updateForDelByRecipeId(@DAOParam("recipeId") Integer recipeId);

//    public boolean save(final Integer recipeId, final List<AuditMedicinesDTO> medicines) {
//        HibernateStatelessResultAction<Boolean> action = new AbstractHibernateStatelessResultAction<Boolean>() {
//            @Override
//            public void execute(StatelessSession ss) throws Exception {
//                //判断是否存在数据
//                long count = getCountByRecipeId(recipeId);
//                //删除数据
////                AuditMedicineIssueDAO auditMedicineIssueDAO = DAOFactory.getDAO(AuditMedicineIssueDAO.class);
//                if(count > 0){
//                    updateForDelByRecipeId(recipeId);
//                    auditMedicineIssueDAO.updateForDelByRecipeId(recipeId);
//                }
//                //保存记录
//                Integer medicineId;
//                AuditMedicines dbMedicine;
//                AuditMedicineIssue dbIssue;
//                List<AuditMedicineIssueDTO> issueList;
//                Date now = DateTime.now().toDate();
//                for(AuditMedicinesDTO medicinesDTO : medicines){
//                    medicinesDTO.setRecipeId(recipeId);
//                    dbMedicine = ObjectCopyUtils.convert(medicinesDTO, AuditMedicines.class);
//                    dbMedicine.setCreateTime(now);
//                    dbMedicine.setLogicalDeleted(0);
//                    dbMedicine.setStatus(1);
//                    dbMedicine = save(dbMedicine);
//                    medicineId = dbMedicine.getId();
//                    if(null != medicineId){
//                        //保存问题详情
//                        issueList = medicinesDTO.getAuditMedicineIssues();
//                        if(CollectionUtils.isNotEmpty(issueList)){
//                            for(AuditMedicineIssueDTO issueDTO : issueList){
//                                issueDTO.setMedicineId(medicineId);
//                                dbIssue = ObjectCopyUtils.convert(issueDTO, AuditMedicineIssue.class);
//                                dbIssue.setRecipeId(recipeId);
//                                dbIssue.setCreateTime(now);
//                                dbIssue.setLogicalDeleted(0);
//                                auditMedicineIssueDAO.save(dbIssue);
//                            }
//                        }
//                    }
//                }
//
//                setResult(true);
//            }
//        };
//
//        HibernateSessionTemplate.instance().executeTrans(action);
//        return action.getResult();
//    }
}
