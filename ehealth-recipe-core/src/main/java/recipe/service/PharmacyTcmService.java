package recipe.service;

import com.aliyun.openservices.shade.org.apache.commons.lang3.StringUtils;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.ngari.base.organconfig.service.IOrganConfigService;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.entity.OrganDrugList;
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
import org.apache.commons.collections.CollectionUtils;
import org.omg.CORBA.INTERNAL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.ObjectUtils;
import recipe.ApplicationUtils;
import recipe.constant.ErrorCode;
import recipe.dao.OrganDrugListDAO;
import recipe.dao.PharmacyTcmDAO;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author renfuhao
 * @date 2020/8/3
 * 药房服务
 */
@RpcBean("pharmacyTcmService")
public class PharmacyTcmService  implements IPharmacyTcmService {
    private static final Logger logger = LoggerFactory.getLogger(PharmacyTcmService.class);

    @Autowired
    private PharmacyTcmDAO pharmacyTcmDAO;
    @Autowired
    private OrganDrugListDAO organDrugListDAO;



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
        logger.info("编辑药房服务[updatePharmacyTcmForOrgan]:" + JSONUtils.toString(pharmacyTcm));
        PharmacyTcm update = pharmacyTcmDAO.update(convert);
        return update;

    }

    /**
     * 根据药房ID 删除药房数据
     * @param pharmacyTcmId
     * @return
     */
    @RpcService
    public void deletePharmacyTcmForId(Integer pharmacyTcmId,Integer organId) {
        if (null == pharmacyTcmId) {
            throw new DAOException(DAOException.VALUE_NEEDED, "pharmacyTcmId is null");
        }
        PharmacyTcm pharmacyTcm = pharmacyTcmDAO.get(pharmacyTcmId);
        if (pharmacyTcm == null){
            throw new DAOException(DAOException.VALUE_NEEDED, "此药房不存在！");
        }
        String pharmacyId="%"+pharmacyTcmId+"%";
        List<OrganDrugList> byOrganIdAndPharmacyId = organDrugListDAO.findByOrganIdAndPharmacyId(organId, pharmacyId);
        if (!ObjectUtils.isEmpty(byOrganIdAndPharmacyId)){
            for (OrganDrugList organDrugList : byOrganIdAndPharmacyId) {
                String pharmacy = organDrugList.getPharmacy();
                if (pharmacy != null){
                    String s = removeOne(pharmacy, pharmacyTcmId);
                    organDrugList.setPharmacy(s);
                }
                organDrugListDAO.update(organDrugList);
            }
        }
        pharmacyTcmDAO.remove(pharmacyTcmId);
    }



    /**
     *  机构药品数据 药房脏数据处理
     * @return
     */
    @RpcService
    public Map<Integer,String> deleteOrganDrugListPharmacy() {
        Map<Integer,String> map= Maps.newHashMap();
        List<Integer> organIdBypharmacy = organDrugListDAO.findOrganIdByPharmacy();
        if (organIdBypharmacy != null){
            for (Integer p : organIdBypharmacy) {
                String ss = deletePharmacy(p);
                map.put(p,ss);
            }
        }
        return map;
    }
    /**
     *  机构药品数据 药房脏数据处理  单个机构处理
     * @return
     */
    @RpcService
    public String deletePharmacy(Integer p) {

        String ss="";
        List<Integer> byOrganId = pharmacyTcmDAO.findPharmacyByOrganId(p);
        List<String> byOrganId2 =Lists.newArrayList();
        for (Integer i : byOrganId) {
            byOrganId2.add(i.toString());
        }
        List<OrganDrugList> byOrganIdAndPharmacy = organDrugListDAO.findByOrganIdAndPharmacy(p);
        if ( byOrganId2!=null && byOrganId2.size()>=0){
            if (byOrganIdAndPharmacy != null  && byOrganIdAndPharmacy.size()>0){
                for (OrganDrugList organDrugList : byOrganIdAndPharmacy) {
                    String pharmacy = organDrugList.getPharmacy();
                    String[] userIdArray = pharmacy.split(",");
                    // 返回结果
                    String result = "-1";
                    // 数组转集合
                    List<String> userIdList = new ArrayList<String>(Arrays.asList(userIdArray));
                    List<String> userIdList2= Lists.newArrayList();
                    if (userIdList != null && userIdList.size() > 0){
                        for (String s : userIdList) {
                            userIdList2.add(s);
                        }
                        for (String s : userIdList2) {
                            if (byOrganId2.indexOf(s) == -1){
                                // 移除指定药房 ID
                                userIdList.remove(s);
                                // 把剩下的药房 ID 再拼接起来
                                result = StringUtils.join(userIdList, ",");
                            }
                        }
                        if ( !"-1".equals(result)){
                            organDrugList.setPharmacy(result);
                            organDrugListDAO.update(organDrugList);
                        }
                    }
                }
            }
            ss="移除药房";
        }else {
            if (byOrganIdAndPharmacy != null  && byOrganIdAndPharmacy.size()>0){
                for (OrganDrugList organDrugList : byOrganIdAndPharmacy) {
                    organDrugList.setPharmacy("");
                    organDrugListDAO.update(organDrugList);
                }
            }
            ss="清空药房";
        }

        return ss;
    }

    /**
     * 移除指定药品 药房字符串中  此药房ID
     * @param pharmacyIds
     * @param pharmacyId
     * @return
     */
    public static String removeOne(String pharmacyIds, Integer pharmacyId) {
        // 返回结果
        String result = "";
        // 判断是否存在。如果存在，移除指定药房 ID；如果不存在，则直接返回空
            // 拆分成数组
            String[] userIdArray = pharmacyIds.split(",");
            // 数组转集合
            List<String> userIdList = new ArrayList<String>(Arrays.asList(userIdArray));
            if(userIdList.indexOf(pharmacyId)==-1){
            // 移除指定药房 ID
            userIdList.remove(pharmacyId.toString());
            // 把剩下的药房 ID 再拼接起来
            result = StringUtils.join(userIdList, ",");
            }
        // 返回
        return result;
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
    public QueryResult<PharmacyTcmDTO> querPharmacyTcmByOrganIdAndName(Integer organId , String input,  Integer start,  Integer limit) {
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
        logger.info("查询药房服务[querPharmacyTcmByOrganId]:" + JSONUtils.toString(symptomQueryResult));
        return  ObjectCopyUtils.convert(symptomQueryResult, PharmacyTcmDTO.class);
    }

    /**
     * 根据机构Id查询药房
     * @param organId
     * @return
     */
    @RpcService
    public PharmacyTcm querPharmacyTcmByOrganIdAndName2(Integer organId ,String name) {
        if (null == organId) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "机构Id不能为空");
        }
        PharmacyTcmDAO pharmacyTcmDAO = DAOFactory.getDAO(PharmacyTcmDAO.class);
        PharmacyTcm symptomQueryResult = pharmacyTcmDAO.getByOrganIdAndName(organId,name);
        return  symptomQueryResult;
    }
}
