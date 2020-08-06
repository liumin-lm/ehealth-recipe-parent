package recipe.service;

import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.entity.PharmacyTcm;
import com.ngari.recipe.recipe.model.PharmacyTcmDTO;
import com.ngari.recipe.recipe.service.IPharmacyTcmService;
import ctd.persistence.DAOFactory;
import ctd.persistence.bean.QueryResult;
import ctd.persistence.exception.DAOException;
import ctd.util.BeanUtils;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.constant.ErrorCode;
import recipe.dao.PharmacyTcmDAO;

import java.util.List;

/**
 * @author renfuhao
 * @date 2020/8/3
 * 药房服务
 */
@RpcBean("pharmacyTcmService")
public class PharmacyTcmService  implements IPharmacyTcmService {
    private static final Logger logger = LoggerFactory.getLogger(SymptomService.class);

    @Autowired
    private PharmacyTcmDAO pharmacyTcmDAO;


    /**
     * 根据药房ID 查找药房数据
     * @param pharmacyTcmId
     * @return
     */
    @RpcService
    public PharmacyTcmDTO getPharmacyTcmForId(Integer pharmacyTcmId) {
        if (null == pharmacyTcmId) {
            throw new DAOException(DAOException.VALUE_NEEDED, "pharmacyTcmId is null");
        }
        PharmacyTcm pharmacyTcm = pharmacyTcmDAO.get(pharmacyTcmId);
        if (pharmacyTcm == null){
            throw new DAOException(DAOException.VALUE_NEEDED, "此药房不存在！");
        }
        return ObjectCopyUtils.convert(pharmacyTcm, PharmacyTcmDTO.class);
    }


    /**
     * 新增药房
     * @param pharmacyTcm
     * @return
     */
    @RpcService
    public boolean addPharmacyTcmForOrgan(PharmacyTcm pharmacyTcm) {
        PharmacyTcmDAO pharmacyTcmDAO = DAOFactory.getDAO(PharmacyTcmDAO.class);
        //验证症候必要信息
        validate(pharmacyTcm);
        if (pharmacyTcmDAO.getByOrganIdAndPharmacyCode(pharmacyTcm.getOrganId(),pharmacyTcm.getPharmacyCode()) != null){
            throw new DAOException(DAOException.VALUE_NEEDED, "机构此药房编码已存在，请重新输入！");
        }
        if (pharmacyTcmDAO.getByOrganIdAndPharmacyName(pharmacyTcm.getOrganId(),pharmacyTcm.getPharmacyName()) != null){
            throw new DAOException(DAOException.VALUE_NEEDED, "机构此药房名称已存在，请重新输入！");
        }
        //PharmacyTcm convert = ObjectCopyUtils.convert(pharmacyTcm, PharmacyTcm.class);
        logger.info("新增药房服务[addPharmacyTcmForOrgan]:" + JSONUtils.toString(pharmacyTcm));
        pharmacyTcmDAO.save(pharmacyTcm);
        return true;

    }
    /**
     * 验证
     * @param pharmacyTcm
     */
    private void validate(PharmacyTcm pharmacyTcm) {
        if (null == pharmacyTcm) {
            throw new DAOException(DAOException.VALUE_NEEDED, "symptom is null");
        }
        if (pharmacyTcm.getPharmacyCode() == null){
            throw new DAOException(DAOException.VALUE_NEEDED, "药房编码不能为空！");
        }
        if (pharmacyTcm.getPharmacyName() == null){
            throw new DAOException(DAOException.VALUE_NEEDED, "药房名称不能为空！");
        }
        if (pharmacyTcm.getOrganId() == null){
            throw new DAOException(DAOException.VALUE_NEEDED, "机构ID不能为空！");
        }

    }


    /**
     * 编辑药房
     * @param pharmacyTcm
     * @return
     */
    @RpcService
    public PharmacyTcm updatePharmacyTcmForOrgan(PharmacyTcm pharmacyTcm) {
        PharmacyTcmDAO pharmacyTcmDAO = DAOFactory.getDAO(PharmacyTcmDAO.class);
        //验证症候必要信息
        PharmacyTcm pharmacyTcm1 = pharmacyTcmDAO.get(pharmacyTcm.getPharmacyId());
        if (pharmacyTcm1 == null){
            throw new DAOException(DAOException.VALUE_NEEDED, "此药房不存在！");
        }
        validate(pharmacyTcm);
        if (!pharmacyTcm.getPharmacyCode().equals(pharmacyTcm1.getPharmacyCode()) && pharmacyTcmDAO.getByOrganIdAndPharmacyCode( pharmacyTcm.getOrganId(),pharmacyTcm.getPharmacyCode()) != null){
            throw new DAOException(DAOException.VALUE_NEEDED, "机构此药房编码已存在，请重新输入！");
        }
        if (!pharmacyTcm.getPharmacyName().equals(pharmacyTcm1.getPharmacyName()) && pharmacyTcmDAO.getByOrganIdAndPharmacyName(pharmacyTcm.getOrganId(),pharmacyTcm.getPharmacyName()) != null){
            throw new DAOException(DAOException.VALUE_NEEDED, "机构此药房名称已存在，请重新输入！");
        }
        PharmacyTcm convert = ObjectCopyUtils.convert(pharmacyTcm, PharmacyTcm.class);
        logger.info("新增药房服务[addPharmacyTcmForOrgan]:" + JSONUtils.toString(pharmacyTcm));
        PharmacyTcm update = pharmacyTcmDAO.update(convert);
        return update;

    }

    /**
     * 根据药房ID 删除药房数据
     * @param pharmacyTcmId
     * @return
     */
    @RpcService
    public void deletePharmacyTcmForId(Integer pharmacyTcmId) {
        if (null == pharmacyTcmId) {
            throw new DAOException(DAOException.VALUE_NEEDED, "pharmacyTcmId is null");
        }
        PharmacyTcm pharmacyTcm = pharmacyTcmDAO.get(pharmacyTcmId);
        if (pharmacyTcm == null){
            throw new DAOException(DAOException.VALUE_NEEDED, "此药房不存在！");
        }
        pharmacyTcmDAO.remove(pharmacyTcmId);
    }

    /**
     * 根据机构Id和查询条件查询药房
     * @param organId
     * @param input
     * @param start
     * @param limit
     * @return
     */
    @RpcService
    public QueryResult<PharmacyTcmDTO> querPharmacyTcmByOrganIdAndName(Integer organId , String input, final int start, final int limit) {
        if (null == organId) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "机构Id不能为空");
        }
        PharmacyTcmDAO pharmacyTcmDAO = DAOFactory.getDAO(PharmacyTcmDAO.class);
        QueryResult<PharmacyTcmDTO> symptomQueryResult = pharmacyTcmDAO.queryTempByTimeAndName(organId, input, start, limit);
        logger.info("查询药房服务[queryymptomByOrganIdAndName]:" + JSONUtils.toString(symptomQueryResult.getItems()));
        return  symptomQueryResult;
    }


    /**
     * 根据机构Id查询药房
     * @param organId
     * @return
     */
    @RpcService
    public List<PharmacyTcmDTO> querPharmacyTcmByOrganId(Integer organId ) {
        if (null == organId) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "机构Id不能为空");
        }
        PharmacyTcmDAO pharmacyTcmDAO = DAOFactory.getDAO(PharmacyTcmDAO.class);
        List<PharmacyTcm> symptomQueryResult = pharmacyTcmDAO.findByOrganId(organId);
        logger.info("查询药房服务[queryymptomByOrganIdAndName]:" + JSONUtils.toString(symptomQueryResult));
        return  ObjectCopyUtils.convert(symptomQueryResult, PharmacyTcmDTO.class);
    }
}
