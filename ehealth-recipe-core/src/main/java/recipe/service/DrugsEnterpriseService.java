package recipe.service;

import com.ngari.patient.service.BasicAPI;
import com.ngari.patient.service.OrganConfigService;
import com.ngari.recipe.drugsenterprise.model.DrugsEnterpriseBean;
import com.ngari.recipe.entity.*;
import ctd.persistence.DAOFactory;
import ctd.persistence.bean.QueryResult;
import ctd.persistence.exception.DAOException;
import ctd.util.BeanUtils;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.ApplicationUtils;
import recipe.constant.ErrorCode;
import recipe.dao.*;
import recipe.drugsenterprise.RemoteDrugEnterpriseService;
import recipe.serviceprovider.BaseService;

import java.util.*;

/**
 * 药企相关接口
 * company: ngarihealth
 * @author: 0184/yu_yun
 * @date:2016/6/2.
 */
@RpcBean("drugsEnterpriseService")
public class DrugsEnterpriseService extends BaseService<DrugsEnterpriseBean>{

    /**
     * LOGGER
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(DrugsEnterpriseService.class);

    /**
     * 有效药企查询 status为1
     *
     * @param status 药企状态
     * @return
     */
    @RpcService
    public List<DrugsEnterpriseBean> findDrugsEnterpriseByStatus(final Integer status) {
        DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        List<DrugsEnterprise> list = drugsEnterpriseDAO.findAllDrugsEnterpriseByStatus(status);
        return getList(list, DrugsEnterpriseBean.class);
    }

    /**
     * 新建药企
     *
     * @param drugsEnterpriseBean
     * @return
     * @author houxr 2016-09-11
     */
    @RpcService
    public DrugsEnterpriseBean addDrugsEnterprise(final DrugsEnterpriseBean drugsEnterpriseBean) {
        if (null == drugsEnterpriseBean) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "DrugsEnterprise is null");
        }
        DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        List<DrugsEnterprise> drugsEnterpriseList = drugsEnterpriseDAO.findAllDrugsEnterpriseByName(drugsEnterpriseBean.getName());
        if (drugsEnterpriseList.size() != 0) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "DrugsEnterprise exist!");
        }

        if( null == drugsEnterpriseBean.getCreateType()){
            throw new DAOException(ErrorCode.SERVICE_ERROR, "createType is null!");
        }
        drugsEnterpriseBean.setSort(100);
        drugsEnterpriseBean.setCreateDate(new Date());
        drugsEnterpriseBean.setLastModify(new Date());

        //拆分药企信息
        DrugsEnterprise drugsEnterprise = getBean(drugsEnterpriseBean, DrugsEnterprise.class);

        //默认值设定
        drugsEnterprise.setCallSys("commonSelf");
        drugsEnterprise.setHosInteriorSupport(0);
        drugsEnterprise.setOrderType(1);
        drugsEnterprise.setCheckInventoryFlag(1);
        drugsEnterprise.setSettlementMode(0);
        drugsEnterprise.setStorePayFlag(0);
        drugsEnterprise.setSendType(drugsEnterpriseBean.getSendType());

        //存储药企信息
        DrugsEnterprise newDrugsEnterprise = drugsEnterpriseDAO.save(drugsEnterprise);

        if( 0 == drugsEnterpriseBean.getCreateType()){
            //自建药企要存储药店信息

            //拆分药企信息
            Map<String, String> map = drugsEnterpriseBean.getPharmacyInfo();
            if(null == map || map.size() == 0){
                throw new DAOException(ErrorCode.SERVICE_ERROR, "pharmacy is null!");
            }

            //封装药店信息
            Pharmacy pharmacy = new Pharmacy();
            pharmacy.setDrugsenterpriseId(newDrugsEnterprise.getId());
            pharmacy.setPharmacyName(drugsEnterpriseBean.getName());
            pharmacy.setPharmacyAddress(map.get("pharmacyAddress"));
            pharmacy.setPharmacyPhone(map.get("pharmacyPhone"));
            //获取药店经度
            pharmacy.setPharmacyLongitude(map.get("pharmacyLongitude"));
            //获取药店纬度
            pharmacy.setPharmacyLatitude(map.get("pharmacyLatitude"));

            pharmacy.setPharmacyCode(newDrugsEnterprise.getId() + "");
            pharmacy.setStatus(1);
            pharmacy.setCreateTime(drugsEnterprise.getCreateDate());
            pharmacy.setLastModify(drugsEnterprise.getLastModify());

            //存储药店信息
            PharmacyDAO pharmacyDAO = DAOFactory.getDAO(PharmacyDAO.class);
            pharmacyDAO.save(pharmacy);

        }
        return drugsEnterpriseBean;
    }

    /**
     * 更新药企
     *
     * @param drugsEnterpriseBean
     * @return
     * @author houxr 2016-09-11
     */
    @RpcService
    public DrugsEnterpriseBean updateDrugsEnterprise(final DrugsEnterpriseBean drugsEnterpriseBean) {
        if (null == drugsEnterpriseBean) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "DrugsEnterprise is null");
        }
        LOGGER.info(JSONUtils.toString(drugsEnterpriseBean));
        DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        DrugsEnterprise target = drugsEnterpriseDAO.get(drugsEnterpriseBean.getId());
        if (null == target) {
            throw new DAOException(DAOException.ENTITIY_NOT_FOUND, "DrugsEnterprise not exist!");
        }

        //拆分药企信息
        DrugsEnterprise drugsEnterprise = getBean(drugsEnterpriseBean, DrugsEnterprise.class);

        BeanUtils.map(drugsEnterprise, target);
        target.setLastModify(new Date());
        target = drugsEnterpriseDAO.update(target);


        if(null != drugsEnterpriseBean.getCreateType() && 0 == drugsEnterpriseBean.getCreateType()){
            //自建药企要存储药店信息

            //拆分药企信息
            Map<String, String> map = drugsEnterpriseBean.getPharmacyInfo();

            PharmacyDAO pharmacyDAO = DAOFactory.getDAO(PharmacyDAO.class);


            //封装药店信息
            Pharmacy pharmacy = new Pharmacy();
            pharmacy.setDrugsenterpriseId(drugsEnterpriseBean.getId());
            pharmacy.setPharmacyAddress(map.get("pharmacyAddress"));
            pharmacy.setPharmacyPhone(map.get("pharmacyPhone"));
            //获取药店经度
            pharmacy.setPharmacyLongitude(map.get("pharmacyLongitude"));
            //获取药店纬度度
            pharmacy.setPharmacyLatitude(map.get("pharmacyLatitude"));
            pharmacy.setLastModify(target.getLastModify());

            pharmacy.setPharmacyCode(drugsEnterpriseBean.getId() + "");
            List<Pharmacy> list = pharmacyDAO.findByDepId(drugsEnterpriseBean.getId());
            if (null == list || list.size() == 0) {
                //插入药店信息
                pharmacy.setPharmacyName(drugsEnterpriseBean.getName());
                pharmacy.setCreateTime(target.getLastModify());
                pharmacy.setStatus(1);
                pharmacyDAO.save(pharmacy);
            } else {
                BeanUtils.map(pharmacy, list.get(0));
                //更新药店信息
                pharmacyDAO.update(list.get(0));
            }


        }
        return getBean(target, DrugsEnterpriseBean.class);
    }

    /**
     * @description 根据药企ID获取药企
     * @author gmw
     * @date 2019/11/12
     * @param id
     * @return com.ngari.recipe.drugsenterprise.model.DrugsEnterpriseBean
     */
    @RpcService
    public DrugsEnterpriseBean findDrugsEnterpriseById(final Integer id) {
        DrugsEnterpriseBean result;
        DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        DrugsEnterprise drugsEnterprise = drugsEnterpriseDAO.get(id);
        result = getBean(drugsEnterprise, DrugsEnterpriseBean.class);

        //自建药企关联药店信息
        if(null == result.getCreateType() || result.getCreateType().equals(0)){
            PharmacyDAO pharmacyDAO = DAOFactory.getDAO(PharmacyDAO.class);
            List<Pharmacy> listS = pharmacyDAO.findByDepId(id);
            for(Pharmacy pharmacy : listS){
                HashMap<String, String> map = new HashMap<String, String> ();
                map.put("pharmacyAddress", pharmacy.getPharmacyAddress());
                map.put("pharmacyPhone", pharmacy.getPharmacyPhone());
                //获取药店经度
                map.put("pharmacyLongitude", pharmacy.getPharmacyLongitude());
                //获取药店纬度
                map.put("pharmacyLatitude", pharmacy.getPharmacyLatitude());
                result.setPharmacyInfo(map);
                break;
            }
        }

        return result;
    }


    /**
     * 根据药企名称分页查询药企
     *
     * @param name  药企名称
     * @param start 分页起始位置
     * @param limit 每页条数
     * @return
     * @author houxr 2016-09-11
     */
    @RpcService
    public QueryResult<DrugsEnterpriseBean> queryDrugsEnterpriseByStartAndLimit(final String name, final Integer createType, final int start, final int limit) {
        DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        QueryResult result = drugsEnterpriseDAO.queryDrugsEnterpriseResultByStartAndLimit(name, createType, start, limit);
        List<DrugsEnterpriseBean> list = getList(result.getItems(), DrugsEnterpriseBean.class);

        if(null == createType || createType.equals(0)){
            PharmacyDAO pharmacyDAO = DAOFactory.getDAO(PharmacyDAO.class);
            List<Pharmacy> listS = pharmacyDAO.find1();
            for(DrugsEnterpriseBean drugsEnterpriseBean : list){
                //自建药企关联药店信息
                if(0 == drugsEnterpriseBean.getCreateType()){
                    for(Pharmacy pharmacy : listS){
                        if(pharmacy.getDrugsenterpriseId().equals(drugsEnterpriseBean.getId())){
                            HashMap<String, String> map = new HashMap<String, String> ();
                            map.put("pharmacyAddress", pharmacy.getPharmacyAddress());
                            map.put("pharmacyPhone", pharmacy.getPharmacyPhone());
                            //获取药店经度
                            map.put("pharmacyLongitude", pharmacy.getPharmacyLongitude());
                            //获取药店纬度
                            map.put("pharmacyLatitude", pharmacy.getPharmacyLatitude());
                            drugsEnterpriseBean.setPharmacyInfo(map);
                            break;
                        }
                    }
                }
            }
        }

        result.setItems(list);
        return result;
    }

    @RpcService
    public List<DrugsEnterpriseBean> findByOrganId(Integer organId) {
        DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        return getList(drugsEnterpriseDAO.findByOrganId(organId), DrugsEnterpriseBean.class);
    }

    /**
     * 检查开处方是否需要进行药企库存校验
     * @param organId
     * @return true:需要校验  false:不需要校验
     */
    @RpcService
    public boolean checkEnterprise(Integer organId){
        OrganConfigService organConfigService = BasicAPI.getService(OrganConfigService.class);
        Integer checkEnterprise = organConfigService.getCheckEnterpriseByOrganId(organId);
        //获取机构配置的药企是否存在 如果有则需要校验 没有则不需要
        OrganAndDrugsepRelationDAO dao = DAOFactory.getDAO(OrganAndDrugsepRelationDAO.class);
        List<DrugsEnterprise> enterprise = dao.findDrugsEnterpriseByOrganIdAndStatus(organId, 1);
        if(Integer.valueOf(0).equals(checkEnterprise) || CollectionUtils.isEmpty(enterprise)){
            return false;
        }

        return true;
    }

    /**
     * 推送医院补充库存药企
     * 流转处方推送
     * @param recipeId
     * @param organId
     */
    @RpcService
    public void pushHosInteriorSupport(Integer recipeId, Integer organId){
        //武昌需求处理，推送无库存的处方至医院补充库存药企||流转处方推送
        DrugsEnterpriseDAO enterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        List<DrugsEnterprise> enterpriseList = enterpriseDAO.findByOrganIdAndHosInteriorSupport(organId);
        if(CollectionUtils.isNotEmpty(enterpriseList)){
            RemoteDrugEnterpriseService service = ApplicationUtils.getRecipeService(RemoteDrugEnterpriseService.class);
            for (DrugsEnterprise dep : enterpriseList) {
                service.pushSingleRecipeInfoWithDepId(recipeId, dep.getId());
            }
        }
    }

    /**
     * 流转处方推送药企
     * @param recipeId
     * @param organId
     */
    @RpcService
    public void pushHosTransferSupport(Integer recipeId, Integer organId){
        //推送流转处方至医院指定取药药企
        DrugsEnterpriseDAO enterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        List<Integer> payModeSupport = RecipeServiceSub.getDepSupportMode(recipe.getPayMode());
        List<DrugsEnterprise> enterpriseList = enterpriseDAO.findByOrganIdAndPayModeSupport(organId,payModeSupport);
        if(CollectionUtils.isNotEmpty(enterpriseList)){
            RemoteDrugEnterpriseService service = ApplicationUtils.getRecipeService(RemoteDrugEnterpriseService.class);
            for (DrugsEnterprise dep : enterpriseList) {
                service.pushSingleRecipeInfoWithDepId(recipeId, dep.getId());
            }
        }
    }

    /**
     * 根据机构获取是否配置配送药企
     * @param organId  机构
     * @return         true 是 false 否
     */
    @RpcService
    public boolean isExistDrugsEnterpriseByOrgan(Integer organId){
        OrganAndDrugsepRelationDAO organAndDrugsepRelationDAO = DAOFactory.getDAO(OrganAndDrugsepRelationDAO.class);
        List<DrugsEnterprise> drugsEnterprises = organAndDrugsepRelationDAO.findDrugsEnterpriseByOrganIdAndStatus(organId, 1);
        if (CollectionUtils.isEmpty(drugsEnterprises)) {
            return false;
        }
        for (DrugsEnterprise drugsEnterprise : drugsEnterprises) {
            if (drugsEnterprise.getPayModeSupport() == 1 || drugsEnterprise.getPayModeSupport() == 7 || drugsEnterprise.getPayModeSupport() == 9) {
                return true;
            }
        }
        return false;
    }

    /**
     * 展示药企药品库存
     * @param drugId   药品编码
     * @param organId  机构编码
     * @return         库存情况
     */
    @RpcService
    public Map<String, Object> showDrugsEnterpriseInventory(Integer drugId, Integer organId){
        LOGGER.info("showDrugsEnterpriseInventory drugId:{},organId:{}.", drugId, organId);
        Map<String, Object> result = new HashMap<>();
        //查询当前药品数据
        OrganDrugListDAO organDrugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);
        List<OrganDrugList> organDrugLists = organDrugListDAO.findByDrugIdAndOrganId(drugId, organId);
        if (CollectionUtils.isEmpty(organDrugLists)) {
            throw new DAOException("没有查询到药品数据");
        }
        //查询当前机构配置的药企
        OrganAndDrugsepRelationDAO drugsepRelationDAO = DAOFactory.getDAO(OrganAndDrugsepRelationDAO.class);
        List<DrugsEnterprise> drugsEnterprises = drugsepRelationDAO.findDrugsEnterpriseByOrganIdAndStatus(organId, 1);
        RemoteDrugEnterpriseService enterpriseService = ApplicationUtils.getRecipeService(RemoteDrugEnterpriseService.class);
        List<List<String>> inventoryList = new ArrayList<>();
        for (DrugsEnterprise drugsEnterprise : drugsEnterprises) {
            List<String> inventoryData = new ArrayList<>();
            String inventory = enterpriseService.getDrugInventory(drugsEnterprise.getId(), drugId);
            if ("有库存".equals(inventory) || "无库存".equals(inventory) || "暂不支持库存查询".equals(inventory)) {
                inventoryData.add(drugsEnterprise.getName());
                if ("暂不支持库存查询".equals(inventory)) {
                    inventoryData.add("无库存");
                } else {
                    inventoryData.add(inventory);
                }
            } else {
                try{
                    inventoryData.add(drugsEnterprise.getName());
                    Double number = Double.parseDouble(inventory);
                    if (number > 0) {
                        inventoryData.add("有库存");
                        inventoryData.add(number + "");
                    } else {
                        inventoryData.add("无库存");
                        inventoryData.add("0");
                    }
                } catch (Exception e) {
                    inventoryData.add("无库存");
                    inventoryData.add("0");
                    LOGGER.info("showDrugsEnterpriseInventory drugId:{},organId:{},err:{}.", drugId, organId, e.getMessage(), e);
                }

            }
            inventoryList.add(inventoryData);
        }
        result.put("enterpriseInventory", inventoryList);
        return result;
    }
}
