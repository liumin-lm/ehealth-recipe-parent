package recipe.dao;

import com.ngari.recipe.entity.AuditMedicineIssue;
import ctd.persistence.annotation.DAOMethod;
import ctd.persistence.annotation.DAOParam;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.util.annotation.RpcSupportDAO;

import java.util.List;

/**
 * created by shiyuping on 2018/11/23
 */
@RpcSupportDAO
public abstract class AuditMedicineIssueDAO extends HibernateSupportDelegateDAO<AuditMedicineIssue> {

    public AuditMedicineIssueDAO() {
        super();
        this.setEntityName(AuditMedicineIssue.class.getName());
        this.setKeyField("issueId");
    }

    @DAOMethod(sql = "from AuditMedicineIssue where recipeId =:recipeId and logicalDeleted = 0")
    public abstract List<AuditMedicineIssue> findIssueByRecipeId(@DAOParam("recipeId") Integer recipeId);

    @DAOMethod(sql = "update AuditMedicineIssue set logicalDeleted = 1 where recipeId = :recipeId")
    public abstract void updateForDelByRecipeId(@DAOParam("recipeId") Integer recipeId);
}
