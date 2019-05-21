package recipe.dao;

import com.ngari.recipe.entity.AuditDrugList;
import ctd.persistence.annotation.DAOMethod;
import ctd.persistence.annotation.DAOParam;
import ctd.persistence.exception.DAOException;
import ctd.persistence.support.hibernate.HibernateSupportDelegateDAO;
import ctd.util.annotation.RpcSupportDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;

/**
 * @author yinsheng
 * @date 2019\5\15 0015 21:06
 */
@RpcSupportDAO
public abstract class AuditDrugListDAO extends HibernateSupportDelegateDAO<AuditDrugList> {

    /**
     * LOGGER
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(AuditDrugListDAO.class);

    public AuditDrugListDAO() {
        super();
        this.setEntityName(AuditDrugList.class.getName());
        this.setKeyField("AuditDrugId");
    }

    @Override
    public AuditDrugList update(AuditDrugList auditDrugList) {
        auditDrugList.setLastModify(new Date());
        return super.update(auditDrugList);
    }

    @Override
    public AuditDrugList save(AuditDrugList auditDrugList) throws DAOException {
        auditDrugList.setLastModify(new Date());
        return super.save(auditDrugList);
    }

    /**
     * [平台使用]查询所有审核药品,药品未进行匹配
     * @param start  起始页
     * @param limit  限制页
     * @return       药品列表
     */
    @DAOMethod(sql = "from AuditDrugList where Type = 0 and Status = 0 order by Status asc,CreateDt desc")
    public abstract List<AuditDrugList> findAllDrugList(@DAOParam(pageStart = true) int start,
                                                        @DAOParam(pageLimit = true) int limit);

    /**
     * [医院使用]根据机构查询审核药品,药品已经匹配成功
     * @param organId  机构ID
     * @param start    起始页
     * @param limit    限制页
     * @return         药品列表
     */
    @DAOMethod(sql = "from AuditDrugList where OrganId=:organId and Type = 1 and Status = 0 order by Status asc, CreateDt desc")
    public abstract List<AuditDrugList> findAllDrugListByOrganId(@DAOParam("organId") Integer organId,
                                                                 @DAOParam(pageStart = true) int start,
                                                                 @DAOParam(pageLimit = true) int limit);

    /**
     * 更新审核信息
     * @param auditDrugId      主键
     * @param status           状态
     * @param rejectReason     拒绝原因
     */
    @DAOMethod(sql = "update AuditDrugList set Status=:status ,RejectReason=:rejectReason where AuditDrugId=:auditDrugId")
    public abstract void updateAuditDrugListStatus(@DAOParam("auditDrugId") Integer auditDrugId,
                                                   @DAOParam("status") Integer status,
                                                   @DAOParam("rejectReason") String rejectReason);
}
