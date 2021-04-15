package recipe.service;

import com.google.common.collect.Lists;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.drug.model.DecoctionWayBean;
import com.ngari.recipe.drug.model.DrugMakingMethodBean;
import com.ngari.recipe.drug.service.IDrugExtService;
import com.ngari.recipe.entity.DecoctionWay;
import com.ngari.recipe.entity.DrugMakingMethod;
import ctd.persistence.bean.QueryResult;
import ctd.persistence.exception.DAOException;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.dao.DrugDecoctionWayDao;
import recipe.dao.DrugEntrustDAO;
import recipe.dao.DrugMakingMethodDao;
import recipe.dao.PharmacyTcmDAO;

import java.util.List;

/**
 * @company: ngarihealth
 * @author: gaomw
 * @date:2020/8/5.
 */
@RpcBean("drugExtService")
public class DrugExtService implements IDrugExtService {
    private static final Logger logger = LoggerFactory.getLogger(DrugExtService.class);
    @Autowired
    DrugMakingMethodDao drugMakingMethodDao;
    @Autowired
    DrugDecoctionWayDao drugDecoctionWayDao;
    @Autowired
    private PharmacyTcmDAO pharmacyTcmDAO;
    @Autowired
    private DrugEntrustDAO drugEntrustDAO;

    @RpcService
    @Override
    public DrugMakingMethodBean get(Object id) {
        return null;
    }

    /**
     * 获取机构下所有药品制法
     *
     * @param organId 机构编码
     * @return List<DrugMakingMethod> 药品信息
     */
    @RpcService
    @Override
    public List<DrugMakingMethodBean> findAllDrugMakingMethodByOrganId(Integer organId) {
        List<DrugMakingMethodBean> allUsePathways = drugMakingMethodDao.findAllDrugMakingMethodByOrganId(organId);
        if (allUsePathways.isEmpty()) {
            return Lists.newArrayList();
        }
        return allUsePathways;
    }


    /**
     * 获取机构下指定制法
     *
     * @param organId 机构编码
     * @return List<DecoctionWayBean> 药品信息
     */
    @RpcService
    @Override
    public QueryResult<DrugMakingMethodBean> findDrugMakingMethodByOrganIdAndName(Integer organId, String methodText, Integer start, Integer limit) {
        QueryResult<DrugMakingMethodBean> decoctionWayBean = drugMakingMethodDao.findDrugMakingMethodByOrganIdAndName(organId, methodText, start, limit);
        return decoctionWayBean;
    }

    /**
     * 药品制法法存储
     *
     * @param drugMakingMethodBean 制法信息
     * @return
     */
    @RpcService
    @Override
    public Integer saveDrugMakingMethod(DrugMakingMethodBean drugMakingMethodBean) {
        DrugMakingMethod temp = drugMakingMethodDao.getDrugMakingMethodByOrganIdAndCode(drugMakingMethodBean.getOrganId(),drugMakingMethodBean.getMethodCode());
        if (temp != null) {
            throw new DAOException(DAOException.VALUE_NEEDED, " 此制法编码重复，请修改！");
        }
        temp = drugMakingMethodDao.getDrugMakingMethodByOrganIdAndText(drugMakingMethodBean.getOrganId(),drugMakingMethodBean.getMethodText());
        if (temp != null) {
            throw new DAOException(DAOException.VALUE_NEEDED, " 此制法名称已存在！");
        }
        DrugMakingMethod drugMakingMethod = ObjectCopyUtils.convert(drugMakingMethodBean, DrugMakingMethod.class);
        drugMakingMethod = drugMakingMethodDao.save(drugMakingMethod);
        return drugMakingMethod.getMethodId();
    }

    /**
     * 药品制法更新
     *
     * @param drugMakingMethodBean 制法
     * @return
     */
    @RpcService
    @Override
    public Integer updateDrugMakingMethod(DrugMakingMethodBean drugMakingMethodBean) {
        if(drugMakingMethodBean == null || drugMakingMethodBean.getMethodId() == null){
            throw new DAOException("methodId不能为空");
        }

        DrugMakingMethod drugMakingMethodOld = drugMakingMethodDao.get(drugMakingMethodBean.getMethodId());
        if(drugMakingMethodOld == null){
            throw new DAOException(DAOException.VALUE_NEEDED, " 未查询出有此制法，修改错误！");
        }

        DrugMakingMethod temp = drugMakingMethodDao.getDrugMakingMethodByOrganIdAndText(drugMakingMethodBean.getOrganId(),drugMakingMethodBean.getMethodText());
        if (temp != null && !(temp.getMethodId().equals(drugMakingMethodBean.getMethodId())) ) {
            throw new DAOException(DAOException.VALUE_NEEDED, " 此制法名称已存在！");
        }
        DrugMakingMethod drugMakingMethod = ObjectCopyUtils.convert(drugMakingMethodBean, DrugMakingMethod.class);
        drugMakingMethod = drugMakingMethodDao.update(drugMakingMethod);
        return drugMakingMethod.getMethodId();
    }

    /**
     * 药品制法删除
     *
     * @param methodId 制法ID
     * @return
     */
    @RpcService
    @Override
    public void deleteDrugMakingMethodByMethodId(Integer methodId) {
        if(methodId == null){
            throw new DAOException("methodId不能为空");
        }
        drugMakingMethodDao.deleteDrugMakingMethodByMethodId(methodId);
    }




    /**
     * 获取机构下所有药品煎法
     *
     * @param organId 机构编码
     * @return List<DecoctionWayBean> 药品信息
     */
    @RpcService
    @Override
    public List<DecoctionWayBean> findAllDecoctionWayByOrganId(Integer organId) {
        List<DecoctionWayBean> decoctionWayBean = drugDecoctionWayDao.findAllDecoctionWayByOrganId(organId);
        if (decoctionWayBean.isEmpty()) {
            return Lists.newArrayList();
        }
        return decoctionWayBean;
    }

    /**
     * 获取机构下指定煎法
     *
     * @param organId 机构编码
     * @return List<DecoctionWayBean> 药品信息
     */
    @RpcService
    @Override
    public QueryResult<DecoctionWayBean> findDecoctionWayByOrganIdAndName(Integer organId, String decoctionText, Integer start, Integer limit) {
        QueryResult<DecoctionWayBean> decoctionWayBean = drugDecoctionWayDao.findDecoctionWayByOrganIdAndName(organId, decoctionText, start, limit);

        return decoctionWayBean;
    }

    /**
     * 药品制法存储
     *
     * @param decoctionWayBean 煎法信息
     * @return
     */
    @RpcService
    @Override
    public Integer saveDrugDecoctionWay(DecoctionWayBean decoctionWayBean) {
        DecoctionWay temp = drugDecoctionWayDao.getDecoctionWayByOrganIdAndCode(decoctionWayBean.getOrganId(),decoctionWayBean.getDecoctionCode());
        if (temp != null) {
            throw new DAOException(DAOException.VALUE_NEEDED, " 此煎法编码重复，请修改！");
        }
        temp = drugDecoctionWayDao.getDecoctionWayByOrganIdAndText(decoctionWayBean.getOrganId(),decoctionWayBean.getDecoctionText());
        if (temp != null) {
            throw new DAOException(DAOException.VALUE_NEEDED, " 此煎法名称已存在！");
        }
        DecoctionWay decoctionWay = ObjectCopyUtils.convert(decoctionWayBean, DecoctionWay.class);
        decoctionWay = drugDecoctionWayDao.save(decoctionWay);
        return decoctionWay.getDecoctionId();
    }

    /**
     * 药品制法更新
     *
     * @param decoctionWayBean 煎法
     * @return
     */
    @RpcService
    @Override
    public Integer updateDrugDecoctionWay(DecoctionWayBean decoctionWayBean) {
        if(decoctionWayBean == null || decoctionWayBean.getDecoctionId() == null){
            throw new DAOException("decoctionId不能为空");
        }

        DecoctionWay decoctionWayOld = drugDecoctionWayDao.get(decoctionWayBean.getDecoctionId());
        if(decoctionWayOld == null){
            throw new DAOException(DAOException.VALUE_NEEDED, " 未查询出有此煎法，修改错误！");
        }

        DecoctionWay temp = drugDecoctionWayDao.getDecoctionWayByOrganIdAndText(decoctionWayBean.getOrganId(),decoctionWayBean.getDecoctionText());
        if (temp != null && !(temp.getDecoctionId().equals(decoctionWayBean.getDecoctionId()))) {
            throw new DAOException(DAOException.VALUE_NEEDED, " 此煎法名称已存在！");
        }
        DecoctionWay decoctionWay = ObjectCopyUtils.convert(decoctionWayBean, DecoctionWay.class);
        decoctionWay = drugDecoctionWayDao.update(decoctionWay);
        return decoctionWay.getDecoctionId();
    }

    /**
     * 药品煎法删除
     *
     * @param decoctionId 煎法ID
     * @return
     */
    @RpcService
    @Override
    public void deleteDrugDecoctionWay(Integer decoctionId) {
        if(decoctionId == null){
            throw new DAOException("decoctionId不能为空");
        }
        drugDecoctionWayDao.deleteDecoctionWayByDecoctionId(decoctionId);
    }

    /**
     * 3 制法 4 煎法  5 药房 7医嘱
     * 查询机构字典
     * @param organId
     * @param type
     * @return
     */
    @Override
    public Integer getCountOfOrgan(Integer organId,Integer type) {
        Integer total = 0;
        if (type == 3)
            total = drugMakingMethodDao.getCountOfOrgan(organId).intValue();
        if (type == 4)
            total = drugDecoctionWayDao.getCountOfOrgan(organId).intValue();
        if (type == 5)
            total = pharmacyTcmDAO.getCountOfOrgan(organId).intValue();
        if (type == 7)
            total = drugEntrustDAO.getCountOfOrgan(organId).intValue();
        return total;
    }
}
