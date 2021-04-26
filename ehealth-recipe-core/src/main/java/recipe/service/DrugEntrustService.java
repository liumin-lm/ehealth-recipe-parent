package recipe.service;

import com.ngari.his.recipe.service.IRecipeHisService;
import com.ngari.opbase.base.service.IBusActionLogService;
import com.ngari.patient.service.BusActionLogService;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.entity.DrugEntrust;
import com.ngari.recipe.entity.OrganDrugList;
import com.ngari.recipe.recipe.model.DrugEntrustDTO;
import com.ngari.recipe.recipe.service.IDrugEntrustService;
import ctd.persistence.DAOFactory;
import ctd.persistence.bean.QueryResult;
import ctd.persistence.exception.DAOException;
import ctd.spring.AppDomainContext;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import ctd.util.exp.standard.IF;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ObjectUtils;
import recipe.constant.ErrorCode;
import recipe.dao.DrugEntrustDAO;
import recipe.dao.OrganDrugListDAO;
import recipe.dao.PharmacyTcmDAO;

import java.util.*;

/**
 * @author renfuhao
 * @date 2021/4/8
 * 药品嘱托服务
 */
@RpcBean("drugEntrustService")
public class DrugEntrustService implements IDrugEntrustService {
    private static final Logger logger = LoggerFactory.getLogger(DrugEntrustService.class);

    @Autowired
    private DrugEntrustDAO drugEntrustDAO;
    @Autowired
    private OrganDrugListDAO organDrugListDAO;
    /**
     * 根据药品嘱托ID 查找药品嘱托数据
     * @param entrustForId
     * @return
     */
    @RpcService
    public DrugEntrustDTO getDrugEntrustForId(Integer entrustForId) {
        if (null == entrustForId) {
            throw new DAOException(DAOException.VALUE_NEEDED, "pharmacyTcmId is null");
        }
        DrugEntrust drugEntrust = drugEntrustDAO.get(entrustForId);
        if (drugEntrust == null){
            throw new DAOException(DAOException.VALUE_NEEDED, "此药品嘱托不存在！");
        }
        return ObjectCopyUtils.convert(drugEntrust, DrugEntrustDTO.class);
    }


    /**
     * 新增药品嘱托
     * @param drugEntrust
     * @return
     */
    @RpcService
    public boolean addDrugEntrustForOrgan(DrugEntrust drugEntrust) {
        PharmacyTcmDAO pharmacyTcmDAO = DAOFactory.getDAO(PharmacyTcmDAO.class);
        //验证症候必要信息
        validate(drugEntrust);
        if (drugEntrustDAO.getByOrganIdAndDrugEntrustCode(drugEntrust.getOrganId(),drugEntrust.getDrugEntrustCode()) != null){
            throw new DAOException(DAOException.VALUE_NEEDED, "机构此药品嘱托编码已存在，请重新输入！");
        }
        if (drugEntrustDAO.getByOrganIdAndDrugEntrustName(drugEntrust.getOrganId(),drugEntrust.getDrugEntrustName()) != null){
            throw new DAOException(DAOException.VALUE_NEEDED, "机构此药品嘱托名称已存在，请重新输入！");
        }
        //PharmacyTcm convert = ObjectCopyUtils.convert(pharmacyTcm, PharmacyTcm.class);
        logger.info("新增药品嘱托服务[addDrugEntrustForOrgan]:" + JSONUtils.toString(drugEntrust));
        if (drugEntrust.getSort() == null){
            drugEntrust.setSort(1000);
        }
        drugEntrust.setCreateDt(new Date());
        DrugEntrust save = drugEntrustDAO.save(drugEntrust);
        IBusActionLogService bean = AppDomainContext.getBean("opbase.busActionLogService", IBusActionLogService.class);
        bean.recordBusinessLogRpcNew("机构数据字典", "", "DrugEntrust", "新增药品嘱托，名称为【" + save.getDrugEntrustName() + "】", BusActionLogService.defaultSubjectName);
        return true;

    }
    /**
     * 验证
     * @param drugEntrust
     */
    private void validate(DrugEntrust drugEntrust) {
        if (null == drugEntrust) {
            throw new DAOException(DAOException.VALUE_NEEDED, "symptom is null");
        }
        if (drugEntrust.getDrugEntrustCode() == null){
            throw new DAOException(DAOException.VALUE_NEEDED, "药品嘱托编码不能为空！");
        }
        if (drugEntrust.getDrugEntrustName() == null){
            throw new DAOException(DAOException.VALUE_NEEDED, "药品嘱托名称不能为空！");
        }
        if (drugEntrust.getOrganId() == null){
            throw new DAOException(DAOException.VALUE_NEEDED, "机构ID不能为空！");
        }

    }


    /**
     * 编辑药品嘱托
     * @param drugEntrust
     * @return
     */
    @RpcService
    public DrugEntrust updateDrugEntrustForOrgan(DrugEntrust drugEntrust) {
        //验证症候必要信息
        DrugEntrust drugEntrust1 = drugEntrustDAO.get(drugEntrust.getDrugEntrustId());
        if (drugEntrust1 == null){
            throw new DAOException(DAOException.VALUE_NEEDED, "此药品嘱托不存在！");
        }
        validate(drugEntrust);
        if (!drugEntrust.getDrugEntrustCode().equals(drugEntrust1.getDrugEntrustCode()) && drugEntrustDAO.getByOrganIdAndDrugEntrustCode(drugEntrust.getOrganId(),drugEntrust.getDrugEntrustCode()) != null){
            throw new DAOException(DAOException.VALUE_NEEDED, "机构此药品嘱托编码已存在，请重新输入！");
        }
        if (!drugEntrust.getDrugEntrustName().equals(drugEntrust1.getDrugEntrustName()) &&drugEntrustDAO.getByOrganIdAndDrugEntrustName(drugEntrust.getOrganId(),drugEntrust.getDrugEntrustName()) != null){
            throw new DAOException(DAOException.VALUE_NEEDED, "机构此药品嘱托名称已存在，请重新输入！");
        }
        DrugEntrust convert = ObjectCopyUtils.convert(drugEntrust, DrugEntrust.class);
        logger.info("编辑药品嘱托服务[updateDrugEntrustForOrgan]:" + JSONUtils.toString(convert));
        DrugEntrust update = drugEntrustDAO.update(convert);
        IBusActionLogService bean = AppDomainContext.getBean("opbase.busActionLogService", IBusActionLogService.class);
        bean.recordBusinessLogRpcNew("机构数据字典", "", "DrugEntrust", "修改药品嘱托，名称【" + drugEntrust.getDrugEntrustName() + "】修改为【" + update.getDrugEntrustName() + "】", BusActionLogService.defaultSubjectName);
        return update;

    }

    /**
     * 根据药房ID 删除药品嘱托数据
     * @param drugEntrustId
     * @return
     */
    @RpcService
    public void deleteDrugEntrustForId(Integer drugEntrustId,Integer organId) {
        if (null == drugEntrustId) {
            throw new DAOException(DAOException.VALUE_NEEDED, "drugEntrustId is null");
        }
        DrugEntrust drugEntrust = drugEntrustDAO.get(drugEntrustId);
        if (drugEntrust.getOrganId()==0){
            throw new DAOException(DAOException.VALUE_NEEDED, "平台默认药品嘱托不支持删除!");
        }
        if (drugEntrust == null){
            throw new DAOException(DAOException.VALUE_NEEDED, "此药品嘱托不存在！");
        }
        List<OrganDrugList> byOrganIdAndPharmacyId = organDrugListDAO.findByOrganIdAndDrugEntrust(organId, drugEntrust.getDrugEntrustId().toString());
        if (!ObjectUtils.isEmpty(byOrganIdAndPharmacyId)){
            for (OrganDrugList organDrugList : byOrganIdAndPharmacyId) {
                organDrugList.setDrugEntrust(null);
                organDrugListDAO.update(organDrugList);
            }
        }
        drugEntrustDAO.remove(drugEntrustId);
        IBusActionLogService bean = AppDomainContext.getBean("opbase.busActionLogService", IBusActionLogService.class);
        bean.recordBusinessLogRpcNew("机构数据字典", "", "DrugEntrust", "删除药品嘱托，名称为【" + drugEntrust.getDrugEntrustName() + "】", BusActionLogService.defaultSubjectName);

    }





    /**
     * 根据机构Id和查询条件查询药品嘱托
     * @param input
     * @param start
     * @param limit
     * @return
     */
    @RpcService
    public QueryResult<DrugEntrustDTO> querDrugEntrustByOrganIdAndName(Integer organId , String input, Integer start, Integer limit) {
        if (null == organId) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "机构Id不能为空");
        }
        QueryResult<DrugEntrustDTO> queryResult = drugEntrustDAO.queryTempByTimeAndName(organId, input, start, limit);
        logger.info("查询药品嘱托服务[querDrugEntrustByOrganIdAndName]:" + JSONUtils.toString(queryResult.getItems()));
        return  queryResult;
    }


    /**
     * 根据机构Id查询药品嘱托
     * @param organId
     * @return
     */
    @RpcService
    public List<DrugEntrustDTO> querDrugEntrustByOrganId(Integer organId ) {
        if (null == organId) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "机构Id不能为空");
        }
        List<DrugEntrust> drugEntrusts = drugEntrustDAO.findByOrganId(organId);
        logger.info("查询药品嘱托服务[querDrugEntrustByOrganId]:" + JSONUtils.toString(drugEntrusts));
        if (drugEntrusts == null || drugEntrusts.size() <= 0){
            return  ObjectCopyUtils.convert(drugEntrustDAO.findByOrganId(0), DrugEntrustDTO.class);
        }
        return  ObjectCopyUtils.convert(drugEntrusts, DrugEntrustDTO.class);
    }


    /**
     * 根据机构Id查询药品嘱托
     * @param organId
     * @return
     */
    @RpcService
    public List<DrugEntrustDTO> querAllDrugEntrustByOrganId(Integer organId ) {
        if (null == organId) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "机构Id不能为空");
        }
        List<DrugEntrust> drugEntrusts = drugEntrustDAO.findByOrganId(organId);
        logger.info("查询药品嘱托服务[querAllDrugEntrustByOrganId]:" + JSONUtils.toString(drugEntrusts));
       /* List<DrugEntrust> byOrganId = drugEntrustDAO.findByOrganId(0);
        if (drugEntrusts == null || drugEntrusts.size() <= 0){
            return  ObjectCopyUtils.convert(byOrganId, DrugEntrustDTO.class);
        }
        if (byOrganId != null){
            for (DrugEntrust drugEntrust : byOrganId) {
                drugEntrusts.add(drugEntrust);
            }
        }*/
        return  ObjectCopyUtils.convert(drugEntrusts, DrugEntrustDTO.class);
    }


    /**
     * 根据机构Id查询药品嘱托
     * @param organId
     * @return
     */
    @RpcService
    public DrugEntrust querDrugEntrustByOrganIdAndName2(Integer organId ,String name) {
        if (null == organId) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "机构Id不能为空");
        }
        DrugEntrust drugEntrust = drugEntrustDAO.getByOrganIdAndName(organId,name);
        return  drugEntrust;
    }

}
