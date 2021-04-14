package recipe.service.sync;

import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.entity.DrugEntrust;
import com.ngari.recipe.entity.SyncDrugExc;
import com.ngari.recipe.recipe.model.DrugEntrustDTO;
import ctd.persistence.annotation.DAOParam;
import ctd.persistence.bean.QueryResult;
import ctd.persistence.exception.DAOException;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.constant.ErrorCode;
import recipe.dao.SyncDrugExcDAO;

import java.util.List;

/**
 * @author renfuhao
 * @date 2021/4/8
 * 药品同步错误数据临时表服务
 */
@RpcBean("syncDrugExcService")
public class SyncDrugExcService {
    private static final Logger logger = LoggerFactory.getLogger(SyncDrugExcService.class);

    @Autowired
    private SyncDrugExcDAO syncDrugExcDAO;


    /**
     * 根据Id查询
     * @param id
     * @return
     */
    @RpcService
    public SyncDrugExc getSyncDrugExc(Integer id ) {
        if (null == id) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "Id不能为空");
        }
        SyncDrugExc syncDrugExc = syncDrugExcDAO.get(id);
        return  syncDrugExc;
    }

    /**
     * 根据机构Id查询
     * @param organId
     * @return
     */
    @RpcService
    public List<SyncDrugExc> querSyncDrugExcByOrganId(Integer organId ) {
        if (null == organId) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "机构Id不能为空");
        }
        List<SyncDrugExc> syncDrugExcs = syncDrugExcDAO.findByOrganId(organId);
        return  syncDrugExcs;
    }

    /**
     * 按照条件查询
     * @param organId
     * @return
     */
    @RpcService
    public QueryResult<SyncDrugExc> querySyncDrugExcByOrganIdAndInput(Integer organId , String input, String type, Integer start, Integer limit) {
        return syncDrugExcDAO.querySyncDrugExcByOrganIdAndInput(organId, input, type, start, limit);
    }



    /**
     * 按照机构ID 删除原先的错误数据
     * @param organId
     * @return
     */
    @RpcService
    public  void deleteByOrganId( Integer organId ,Integer syncType){
        syncDrugExcDAO.deleteByOrganId(organId,syncType);
    }
}
