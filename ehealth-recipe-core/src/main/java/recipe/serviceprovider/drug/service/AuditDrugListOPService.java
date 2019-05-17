package recipe.serviceprovider.drug.service;

import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.drug.model.AuditDrugListDTO;
import com.ngari.recipe.drug.service.IAuditDrugListService;
import com.ngari.recipe.entity.AuditDrugList;
import ctd.persistence.DAOFactory;
import ctd.util.annotation.RpcBean;
import recipe.dao.AuditDrugListDAO;

import java.util.List;

/**
 * @author yinsheng
 * @date 2019\5\16 0016 09:41
 */
@RpcBean("auditDrugListOPService")
public class AuditDrugListOPService implements IAuditDrugListService{

    /**
     * 更新审核信息
     * @param auditDrugListId  主键
     * @param status           状态
     * @param rejectReason     拒绝原因
     */
    @Override
    public void updateAuditDrugListStatus(Integer auditDrugListId, Integer status, String rejectReason) {
        AuditDrugListDAO auditDrugListDAO = DAOFactory.getDAO(AuditDrugListDAO.class);
        auditDrugListDAO.updateAuditDrugListStatus(auditDrugListId, status, rejectReason);
    }

    /**
     * 查询所有审核药品
     * @param start  起始页
     * @param limit  限制页
     * @return  药品列表
     */
    @Override
    public List<AuditDrugListDTO> findAllDrugList(Integer start, Integer limit) {
        AuditDrugListDAO auditDrugListDAO = DAOFactory.getDAO(AuditDrugListDAO.class);
        List<AuditDrugList> auditDrugLists = auditDrugListDAO.findAllDrugList(start, limit);
        return ObjectCopyUtils.convert(auditDrugLists, AuditDrugListDTO.class);
    }

    /**
     * 根据机构查询审核药品
     * @param organId  机构ID
     * @param start    起始页
     * @param limit    限制页
     * @return   药品列表
     */
    @Override
    public List<AuditDrugListDTO> findAllDrugListByOrganId(Integer organId, Integer start, Integer limit) {
        AuditDrugListDAO auditDrugListDAO = DAOFactory.getDAO(AuditDrugListDAO.class);
        List<AuditDrugList> auditDrugLists = auditDrugListDAO.findAllDrugListByOrganId(organId, start, limit);
        return ObjectCopyUtils.convert(auditDrugLists, AuditDrugListDTO.class);
    }
}
