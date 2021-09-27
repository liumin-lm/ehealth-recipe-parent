package recipe.service;

import com.alibaba.fastjson.JSONArray;
import com.google.common.collect.Lists;
import com.ngari.patient.dto.OrganDTO;
import com.ngari.patient.service.OrganService;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.common.RecipeResultBean;
import com.ngari.recipe.drugsenterprise.model.DrugEnterpriseLogisticsBean;
import com.ngari.recipe.drugsenterprise.model.DrugsEnterpriseBean;
import com.ngari.recipe.entity.*;
import com.ngari.recipe.recipe.constant.RecipeDistributionFlagEnum;
import com.ngari.recipe.recipe.constant.RecipeSendTypeEnum;
import com.ngari.recipe.recipe.constant.RecipeSupportGiveModeEnum;
import com.ngari.recipe.recipe.model.GiveModeButtonBean;
import com.ngari.recipe.recipe.model.GiveModeShowButtonVO;
import ctd.account.UserRoleToken;
import ctd.dictionary.DictionaryController;
import ctd.persistence.DAOFactory;
import ctd.persistence.bean.QueryResult;
import ctd.persistence.exception.DAOException;
import ctd.util.AppContextHolder;
import ctd.util.BeanUtils;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.ApplicationUtils;
import recipe.constant.DrugEnterpriseConstant;
import recipe.constant.ErrorCode;
import recipe.dao.*;
import recipe.drugsenterprise.RemoteDrugEnterpriseService;
import recipe.givemode.business.GiveModeFactory;
import recipe.givemode.business.IGiveModeBase;
import recipe.manager.DrugStockManager;
import recipe.service.drugs.IDrugEnterpriseLogisticsService;
import recipe.serviceprovider.BaseService;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 药企相关接口
 * company: ngarihealth
 *
 * @author: 0184/yu_yun
 * @date:2016/6/2.
 */
@RpcBean("drugsEnterpriseService")
public class DrugsEnterpriseService extends BaseService<DrugsEnterpriseBean> {

    /**
     * LOGGER
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(DrugsEnterpriseService.class);

    @Autowired
    private IDrugEnterpriseLogisticsService drugEnterpriseLogisticsService;
    @Autowired
    private DrugEnterpriseLogisticsDAO drugEnterpriseLogisticsDAO;
    @Resource
    private OrganDrugListDAO organDrugListDAO;
    @Resource
    private RecipeService recipeService;
    @Autowired
    private DrugStockManager drugStockManager;

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

    @RpcService
    public List<DrugsEnterpriseBean> getDrugsEnterpriseByName(String name) {
        DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        List<DrugsEnterprise> drugsEnterpriseList = drugsEnterpriseDAO.findAllDrugsEnterpriseByName(name);
        return getList(drugsEnterpriseList, DrugsEnterpriseBean.class);
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
        LOGGER.info("addDrugsEnterprise params={}", JSONArray.toJSONString(drugsEnterpriseBean));
        DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        List<DrugsEnterprise> drugsEnterpriseList = drugsEnterpriseDAO.findAllDrugsEnterpriseByName(drugsEnterpriseBean.getName());
        if (drugsEnterpriseList.size() != 0) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "DrugsEnterprise exist!");
        }

        if (null == drugsEnterpriseBean.getCreateType()) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "createType is null!");
        }
        // 药企物流信息校验：平台对接物流，物流公司、寄件人信息不能为空
        checkEnterpriseLogisticsInfo(drugsEnterpriseBean);
        drugsEnterpriseBean.setSort(100);
        drugsEnterpriseBean.setCreateDate(new Date());
        drugsEnterpriseBean.setLastModify(new Date());

        //拆分药企信息
        DrugsEnterprise drugsEnterprise = getBean(drugsEnterpriseBean, DrugsEnterprise.class);

        //默认值设定
        if (drugsEnterprise.getHosInteriorSupport() == null) {
            drugsEnterprise.setHosInteriorSupport(0);
        }
        if (drugsEnterprise.getCheckInventoryFlag() == null) {
            drugsEnterprise.setCheckInventoryFlag(1);
        }
        if (drugsEnterprise.getShowStoreFlag() == null) {
            drugsEnterprise.setShowStoreFlag(1);
        }
        if (drugsEnterprise.getDownSignImgType() == null) {
            drugsEnterprise.setDownSignImgType(1);
        }
        if (drugsEnterprise.getSettlementMode() == null) {
            drugsEnterprise.setSettlementMode(1);
        }
        if (drugsEnterprise.getOperationType() == null) {
            drugsEnterprise.setOperationType(1);
        }
        if (StringUtils.isEmpty(drugsEnterprise.getCallSys())) {
            drugsEnterprise.setCallSys("commonSelf");
        }
        // 药企物流对接方式默认药企对接
        if (null == drugsEnterprise.getLogisticsType()) {
            drugsEnterprise.setLogisticsType(DrugEnterpriseConstant.LOGISTICS_ENT);
        }
        drugsEnterprise.setOrderType(1);
        drugsEnterprise.setStorePayFlag(0);
        drugsEnterprise.setSendType(drugsEnterpriseBean.getSendType());

        //存储药企信息
        DrugsEnterprise newDrugsEnterprise = drugsEnterpriseDAO.save(drugsEnterprise);
        // 写入药企关联物流公司信息
        drugEnterpriseLogisticsService.saveDrugEnterpriseLogistics(drugsEnterpriseBean.getDrugEnterpriseLogisticsBeans(), newDrugsEnterprise.getId());
        //更新管理单元
        String manageUnit = "yq" + newDrugsEnterprise.getId();
        drugsEnterpriseDAO.updateManageUnitById(newDrugsEnterprise.getId(), manageUnit);
        if (0 == drugsEnterpriseBean.getCreateType()) {
            //自建药企要存储药店信息

            //拆分药企信息
            Map<String, String> map = drugsEnterpriseBean.getPharmacyInfo();
            if (null == map || map.size() == 0) {
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
        LOGGER.info("updateDrugsEnterprise params={}", JSONUtils.toString(drugsEnterpriseBean));
        // 药企物流信息校验：平台对接物流，物流公司、寄件人信息不能为空
        checkEnterpriseLogisticsInfo(drugsEnterpriseBean);
        DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        DrugsEnterprise target = drugsEnterpriseDAO.get(drugsEnterpriseBean.getId());
        if (null == target) {
            throw new DAOException(DAOException.ENTITIY_NOT_FOUND, "DrugsEnterprise not exist!");
        }

        //拆分药企信息
        DrugsEnterprise drugsEnterprise = getBean(drugsEnterpriseBean, DrugsEnterprise.class);

        BeanUtils.map(drugsEnterprise, target);
        target.setLastModify(new Date());
        if (drugsEnterprise.getOrganId() == null || drugsEnterprise.getOrganId().equals("")) {
            target.setOrganId(null);
        }
        if (drugsEnterpriseBean.getExpressFeePayWay() == null) {
            target.setExpressFeePayWay(null);
        }
        if (drugsEnterpriseBean.getTransFeeDetail() == null) {
            target.setTransFeeDetail(null);
        }
        target = drugsEnterpriseDAO.update(target);
        // 写入药企关联物流公司信息
        drugEnterpriseLogisticsService.saveDrugEnterpriseLogistics(drugsEnterpriseBean.getDrugEnterpriseLogisticsBeans(), target.getId());

        if (null != drugsEnterpriseBean.getCreateType() && 0 == drugsEnterpriseBean.getCreateType()) {
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
     * 药企平台物流对接信息校验
     *
     * @param drugsEnterpriseBean
     */
    private void checkEnterpriseLogisticsInfo(DrugsEnterpriseBean drugsEnterpriseBean) {
        if (DrugEnterpriseConstant.LOGISTICS_PLATFORM.equals(drugsEnterpriseBean.getLogisticsType())) {
            if (CollectionUtils.isEmpty(drugsEnterpriseBean.getDrugEnterpriseLogisticsBeans())
                    || StringUtils.isBlank(drugsEnterpriseBean.getConsignorAddress())
                    || StringUtils.isBlank(drugsEnterpriseBean.getConsignorCity()) || StringUtils.isBlank(drugsEnterpriseBean.getConsignorDistrict())
                    || StringUtils.isBlank(drugsEnterpriseBean.getConsignorMobile()) || StringUtils.isBlank(drugsEnterpriseBean.getConsignorName())
                    || StringUtils.isBlank(drugsEnterpriseBean.getConsignorProvince())) {
                throw new DAOException(ErrorCode.SERVICE_ERROR, "平台对接物流，物流公司、寄件人信息不能为空!");
            }
        }
    }

    /**
     * @param id
     * @return com.ngari.recipe.drugsenterprise.model.DrugsEnterpriseBean
     * @description 根据药企ID获取药企
     * @author gmw
     * @date 2019/11/12
     */
    @RpcService
    public DrugsEnterpriseBean findDrugsEnterpriseById(final Integer id) {
        DrugsEnterpriseBean result;
        DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        DrugsEnterprise drugsEnterprise = drugsEnterpriseDAO.get(id);
        result = getBean(drugsEnterprise, DrugsEnterpriseBean.class);

        //自建药企关联药店信息
        if (null == result.getCreateType() || result.getCreateType().equals(0)) {
            PharmacyDAO pharmacyDAO = DAOFactory.getDAO(PharmacyDAO.class);
            List<Pharmacy> listS = pharmacyDAO.findByDepId(id);
            for (Pharmacy pharmacy : listS) {
                HashMap<String, String> map = new HashMap<String, String>();
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

    @RpcService
    public DrugsEnterpriseBean getDrugsEnterpriseById(Integer drugsEnterpriseId) {
        if (drugsEnterpriseId == null) {
            throw new DAOException(DAOException.VALUE_NEEDED, "药企Id为null!");
        }
        DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        DrugsEnterprise drugsEnterprise = drugsEnterpriseDAO.get(drugsEnterpriseId);
        if (drugsEnterprise == null) {
            return null;
        }
        PharmacyDAO pharmacyDAO = DAOFactory.getDAO(PharmacyDAO.class);
        List<Pharmacy> listS = pharmacyDAO.find1();
        DrugsEnterpriseBean drugsEnterpriseBean = ObjectCopyUtils.convert(drugsEnterprise, DrugsEnterpriseBean.class);
        if (0 == drugsEnterpriseBean.getCreateType()) {
            for (Pharmacy pharmacy : listS) {
                if (pharmacy.getDrugsenterpriseId().equals(drugsEnterpriseBean.getId())) {
                    HashMap<String, String> map = new HashMap<String, String>();
                    map.put("pharmacyAddress", pharmacy.getPharmacyAddress());
                    map.put("pharmacyPhone", pharmacy.getPharmacyPhone());
                    //获取药店经度
                    map.put("pharmacyLongitude", pharmacy.getPharmacyLongitude());
                    //获取药店纬度
                    map.put("pharmacyLatitude", pharmacy.getPharmacyLatitude());
                    OrganService bean = AppContextHolder.getBean("basic.organService", OrganService.class);
                    if (drugsEnterpriseBean.getOrganId() != null) {
                        OrganDTO byOrganId = bean.getByOrganId(drugsEnterpriseBean.getOrganId());
                        map.put("organName", byOrganId.getName());
                    } else {
                        map.put("organName", null);
                    }
                    drugsEnterpriseBean.setPharmacyInfo(map);
                    break;
                }
            }
        }
        List<DrugEnterpriseLogistics> byDrugsEnterpriseId = drugEnterpriseLogisticsDAO.getByDrugsEnterpriseId(drugsEnterpriseId);
        List<DrugEnterpriseLogisticsBean> drugEnterpriseLogisticsBeans = byDrugsEnterpriseId.stream().map(drugEnterpriseLogistics -> {
            DrugEnterpriseLogisticsBean drugEnterpriseLogisticsBean = new DrugEnterpriseLogisticsBean();
            BeanUtils.copy(drugEnterpriseLogistics, drugEnterpriseLogisticsBean);
            return drugEnterpriseLogisticsBean;
        }).collect(Collectors.toList());
        drugsEnterpriseBean.setDrugEnterpriseLogisticsBeans(drugEnterpriseLogisticsBeans);
        return drugsEnterpriseBean;
    }

    @RpcService
    public DrugsEnterpriseBean getDrugsEnterpriseByIdForOp(Integer drugsEnterpriseId){
        /*ISecurityService securityService = AppContextHolder.getBean("opbase.securityService",ISecurityService.class);*/
        DrugsEnterpriseBean bean = getDrugsEnterpriseById(drugsEnterpriseId);
        /*UserRoleToken urt = UserRoleToken.getCurrent();
        String mu = urt.getManageUnit();
        if (bean != null){
            if (!"eh".equals(mu) && null == bean.getOrganId()){
                throw new DAOException(DAOException.ACCESS_DENIED,"权限验证失败");
            }
            securityService.isAuthoritiedOrganNew(bean.getOrganId());
        }*/
        return bean;
    }

    @RpcService
    public QueryResult<DrugsEnterpriseBean> getDrugsEnterprise(Integer status) {
        UserRoleToken urt = UserRoleToken.getCurrent();
        String manageUnit = urt.getManageUnit();
        QueryResult<DrugsEnterpriseBean> result = new QueryResult<>();
        DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        if (manageUnit.equals("eh")) {
            result = drugsEnterpriseDAO.queryDrugsEnterpriseResultByManageUnit(manageUnit, null, status);
        } else if (manageUnit.startsWith("yq")) {
            result = drugsEnterpriseDAO.queryDrugsEnterpriseResultByManageUnit(manageUnit, null, status);
        } else {
            OrganService bean = AppContextHolder.getBean("basic.organService", OrganService.class);
            List<Integer> organIdsByManageUnit = bean.findOrganIdsByManageUnit("%" + manageUnit + "%");
            if (organIdsByManageUnit == null) {
                return result;
            }
            result = drugsEnterpriseDAO.queryDrugsEnterpriseResultByManageUnit(null, organIdsByManageUnit, status);
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
    public QueryResult<DrugsEnterpriseBean> queryDrugsEnterpriseByStartAndLimit(final String name, final Integer createType, final Integer organId, int start, final int limit) {
        DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        UserRoleToken urt = UserRoleToken.getCurrent();
        String manageUnit = urt.getManageUnit();
        QueryResult result = new QueryResult<>();
        if (!manageUnit.equals("eh")) {
            OrganService bean = AppContextHolder.getBean("basic.organService", OrganService.class);
            List<Integer> organIdsByManageUnit = bean.findOrganIdsByManageUnit("%" + manageUnit + "%");
            if (organIdsByManageUnit == null) {
                return result;
            }
            result = drugsEnterpriseDAO.queryDrugsEnterpriseResultByOrganId(name, createType, organId, organIdsByManageUnit, start, limit);
        } else {
            result = drugsEnterpriseDAO.queryDrugsEnterpriseResultByOrganId(name, createType, organId, null, start, limit);
        }
        List<DrugsEnterpriseBean> list = getList(result.getItems(), DrugsEnterpriseBean.class);
        if (null == createType || createType.equals(0)) {
            PharmacyDAO pharmacyDAO = DAOFactory.getDAO(PharmacyDAO.class);
            List<Pharmacy> listS = pharmacyDAO.find1();
            for (DrugsEnterpriseBean drugsEnterpriseBean : list) {
                //自建药企关联药店信息
                if (0 == drugsEnterpriseBean.getCreateType()) {
                    for (Pharmacy pharmacy : listS) {
                        if (pharmacy.getDrugsenterpriseId().equals(drugsEnterpriseBean.getId())) {
                            HashMap<String, String> map = new HashMap<String, String>();
                            map.put("pharmacyAddress", pharmacy.getPharmacyAddress());
                            map.put("pharmacyPhone", pharmacy.getPharmacyPhone());
                            //获取药店经度
                            map.put("pharmacyLongitude", pharmacy.getPharmacyLongitude());
                            //获取药店纬度
                            map.put("pharmacyLatitude", pharmacy.getPharmacyLatitude());
                            OrganService bean = AppContextHolder.getBean("basic.organService", OrganService.class);
                            if (drugsEnterpriseBean.getOrganId() != null) {
                                OrganDTO byOrganId = bean.getByOrganId(drugsEnterpriseBean.getOrganId());
                                map.put("organName", byOrganId.getName());
                            } else {
                                map.put("organName", null);
                            }
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
     *
     * @param organId
     * @return true:需要校验  false:不需要校验
     */
    @RpcService
    public boolean checkEnterprise(Integer organId) {
        return drugStockManager.checkEnterprise(organId);
    }

    /**
     * 推送医院补充库存药企
     * 流转处方推送
     *
     * @param recipeId
     * @param organId
     */
    @RpcService
    public void pushHosInteriorSupport(Integer recipeId, Integer organId) {
        //武昌需求处理，推送无库存的处方至医院补充库存药企||流转处方推送
        DrugsEnterpriseDAO enterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        List<DrugsEnterprise> enterpriseList = enterpriseDAO.findByOrganIdAndHosInteriorSupport(organId);
        if (CollectionUtils.isNotEmpty(enterpriseList)) {
            RemoteDrugEnterpriseService service = ApplicationUtils.getRecipeService(RemoteDrugEnterpriseService.class);
            for (DrugsEnterprise dep : enterpriseList) {
                service.pushSingleRecipeInfoWithDepId(recipeId, dep.getId());
            }
        }
    }

    /**
     * 流转处方推送药企
     *
     * @param recipeId
     * @param organId
     */
    @RpcService
    public void pushHosTransferSupport(Integer recipeId, Integer organId) {
        //推送流转处方至医院指定取药药企
        DrugsEnterpriseDAO enterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        RecipeOrderDAO recipeOrderDAO = DAOFactory.getDAO(RecipeOrderDAO.class);
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        RecipeOrder order = recipeOrderDAO.getByOrderCode(recipe.getOrderCode());
        // paymode 转老一套

        Integer payMode = PayModeGiveModeUtil.getPayMode(order.getPayMode(), recipe.getGiveMode());

        List<Integer> payModeSupport = RecipeServiceSub.getDepSupportMode(payMode);
        List<DrugsEnterprise> enterpriseList = enterpriseDAO.findByOrganIdAndPayModeSupport(organId, payModeSupport);
        if (CollectionUtils.isNotEmpty(enterpriseList)) {
            RemoteDrugEnterpriseService service = ApplicationUtils.getRecipeService(RemoteDrugEnterpriseService.class);
            for (DrugsEnterprise dep : enterpriseList) {
                service.pushSingleRecipeInfoWithDepId(recipeId, dep.getId());
            }
        }
    }

    /**
     * 根据机构获取是否配置配送药企
     *
     * @param organId 机构
     * @return true 是 false 否
     */
    @RpcService
    public boolean isExistDrugsEnterprise(Integer organId, Integer drugId) {
        LOGGER.info("isExistDrugsEnterpriseByOrgan organId:{}, drugId:{}.", organId, drugId);
        try {
            SaleDrugListDAO saleDrugListDAO = DAOFactory.getDAO(SaleDrugListDAO.class);
            OrganAndDrugsepRelationDAO organAndDrugsepRelationDAO = DAOFactory.getDAO(OrganAndDrugsepRelationDAO.class);
            List<DrugsEnterprise> drugsEnterprises = organAndDrugsepRelationDAO.findDrugsEnterpriseByOrganIdAndStatus(organId, 1);
            if (CollectionUtils.isEmpty(drugsEnterprises)) {
                return false;
            }
            List<DrugsEnterprise> drugsEnterpriseList = new ArrayList<>();
            for (DrugsEnterprise drugsEnterprise : drugsEnterprises) {
                if (drugsEnterprise.getPayModeSupport() == 1 || drugsEnterprise.getPayModeSupport() == 7 || drugsEnterprise.getPayModeSupport() == 9) {
                    drugsEnterpriseList.add(drugsEnterprise);
                }
            }
            //如果药企不存在在任何一家可配送药企则不显示按钮
            //date 20200921 修改【his管理的药企】默认有配送药品
            if (new Integer(0).equals(RecipeServiceSub.getOrganEnterprisesDockType(organId))) {
                for (DrugsEnterprise drugsEnterprise : drugsEnterpriseList) {
                    SaleDrugList saleDrugList = saleDrugListDAO.getByDrugIdAndOrganIdAndStatus(drugId, drugsEnterprise.getId());
                    if (saleDrugList != null) {
                        return true;
                    }
                }
            } else {
                return true;
            }
            return false;
        } catch (Exception e) {
            LOGGER.error("isExistDrugsEnterpriseByOrgan error:{}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 展示药企药品库存----使用新接口queryDrugInventoriesByRealTime 这里只做兼容使用
     *
     * @param drugId  药品编码
     * @param organId 机构编码
     * @return 库存情况
     */
    @RpcService
    @Deprecated
    public Map<String, Object> showDrugsEnterpriseInventory(Integer drugId, Integer organId) {
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
        List<DrugsEnterprise> enterprises = new ArrayList<>();
        if (new Integer(0).equals(RecipeServiceSub.getOrganEnterprisesDockType(organId))) {
            SaleDrugListDAO saleDrugListDAO = DAOFactory.getDAO(SaleDrugListDAO.class);
            for (DrugsEnterprise enterprise : drugsEnterprises) {
                SaleDrugList saleDrugList = saleDrugListDAO.getByDrugIdAndOrganIdAndStatus(drugId, enterprise.getId());
                if (saleDrugList != null) {
                    enterprises.add(enterprise);
                }
            }
        } else {
            //date 20200921 修改【his管理的药企】不需要校验配送药品，直接添加
            enterprises.addAll(drugsEnterprises);
        }
        List<List<String>> inventoryList = new ArrayList<>();
        for (DrugsEnterprise drugsEnterprise : enterprises) {
            List<String> inventoryData = new ArrayList<>();
            String inventory = enterpriseService.getDrugInventory(drugsEnterprise.getId(), drugId, organId);
            if ("有库存".equals(inventory) || "无库存".equals(inventory) || "暂不支持库存查询".equals(inventory)) {
                inventoryData.add(drugsEnterprise.getName());
                inventoryData.add(inventory);
            } else {
                try {
                    inventoryData.add(drugsEnterprise.getName());
                    Integer number;
                    if (inventory.contains(".")) {
                        String num = inventory.substring(0, inventory.indexOf("."));
                        number = Integer.parseInt(num);
                    } else {
                        number = Integer.parseInt(inventory);
                    }
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

    @RpcService
    public List<DrugsEnterprise> findDrugsEnterpriseForOpUser() {
        List<DrugsEnterprise> list = Lists.newArrayList();
        UserRoleToken ur = UserRoleToken.getCurrent();
        DrugsEnterpriseDAO enterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        DrugsEnterprise enterprise = enterpriseDAO.getByManageUnit(ur.getManageUnit());
        list.add(enterprise);
        return list;
    }

    @RpcService
    public String getLogisticsCompanyName(String logisticsCompany) {
        try {
            return DictionaryController.instance().get("eh.cdr.dictionary.KuaiDiNiaoCode")
                    .getText(logisticsCompany);
        } catch (Exception e) {
            LOGGER.info("getTrackingNumber error msg:{}.", e.getMessage());
        }
        return "";
    }

    /**
     * 根据机构支持的购药方式以及药品库存查询处方支持的购药方式
     *
     * @param recipeId
     * @param organId
     * @return
     */
    @RpcService
    public List<Integer> getDrugsEnterpriseContinue(Integer recipeId, int organId) {
        LOGGER.info("getDrugsEnterpriseContinue recipeId = {} organId= {}}", recipeId, organId);
        List<Integer> recipeSupportGiveModeList = new ArrayList<>();
        // 获取机构支持的配置
        IGiveModeBase giveModeBase = GiveModeFactory.getGiveModeBaseByRecipe(new Recipe());
        GiveModeShowButtonVO giveModeShowButtonVO = giveModeBase.getGiveModeSettingFromYypt(organId);
        List<GiveModeButtonBean> giveModeButtons = giveModeShowButtonVO.getGiveModeButtons();
        if (CollectionUtils.isEmpty(giveModeButtons)) {
            return recipeSupportGiveModeList;
        }
        List<String> configurations = giveModeButtons.stream().map(e -> e.getShowButtonKey()).collect(Collectors.toList());
        // 0是什么都没有，1是指配置了到院取药，2是配置到药企相关，3是医院药企都配置了
        int checkFlag = 0;
        for (String configuration : configurations) {
            switch (configuration) {
                case "supportToHos":
                    if (checkFlag == 0 || checkFlag == 1) {
                        checkFlag = 1;
                    } else {
                        checkFlag = 3;
                    }
                    break;
                case "showSendToHos":
                case "showSendToEnterprises":
                case "supportTFDS":
                    if (checkFlag == 0 || checkFlag == 2) {
                        checkFlag = 2;
                    } else {
                        checkFlag = 3;
                    }
                    break;
                default:
                    break;
            }
        }
        switch (checkFlag) {
            case 1:
                getGiveModeWhenContinueByHos(recipeSupportGiveModeList, recipeId);
                break;
            case 2:
                recipeSupportGiveModeList = getGiveModeWhenContinueOne(recipeSupportGiveModeList, recipeId, organId);
                break;
            case 3:
                recipeSupportGiveModeList = getGiveModeWhenContinueOne(recipeSupportGiveModeList, recipeId, organId);
                getGiveModeWhenContinueByHos(recipeSupportGiveModeList, recipeId);
                break;
            default:
                break;
        }

        setOtherGiveMode(configurations,recipeId,organId,recipeSupportGiveModeList);
        LOGGER.info("getDrugsEnterpriseContinue  recipeId= {} recipeSupportGiveModeList= {}", recipeId, JSONUtils.toString(recipeSupportGiveModeList));
        return recipeSupportGiveModeList;
    }

    /**
     * 传入库存信息,获取处方的购药方式
     * @param scanResult
     * @param supportDepList
     * @param checkFlag
     * @param recipeId
     * @param organId
     * @return
     */
    public List<Integer> getRecipeGiveMode(com.ngari.platform.recipe.mode.RecipeResultBean scanResult, List<DrugsEnterprise> supportDepList, int checkFlag, Integer recipeId, int organId, List<String> configurations) {
        LOGGER.info("getRecipeGiveMode scanResult = {} supportDepList= {} checkFlag={} recipeId={} organId={} configurations = {}", JSONArray.toJSONString(scanResult), JSONArray.toJSONString(supportDepList),checkFlag,recipeId,organId,JSONArray.toJSONString(configurations));
        List<Integer> recipeSupportGiveModeList = new ArrayList<>();
        switch (checkFlag) {
            case 1:
                if (RecipeResultBean.SUCCESS.equals(scanResult.getCode())) {
                    recipeSupportGiveModeList.add(RecipeSupportGiveModeEnum.SUPPORT_TO_HOS.getType());
                }
                break;
            case 2:
                recipeSupportGiveModeList = getGiveModeBuEnterprise(supportDepList,recipeSupportGiveModeList, recipeId, organId);
                break;
            case 3:
                recipeSupportGiveModeList = getGiveModeBuEnterprise(supportDepList,recipeSupportGiveModeList, recipeId, organId);
                if (RecipeResultBean.SUCCESS.equals(scanResult.getCode())) {
                    recipeSupportGiveModeList.add(RecipeSupportGiveModeEnum.SUPPORT_TO_HOS.getType());
                }
                break;
            default:
                break;
        }
        setOtherGiveMode(configurations,recipeId,organId,recipeSupportGiveModeList);
        LOGGER.info("getRecipeGiveMode  recipeId= {} recipeSupportGiveModeList= {}", recipeId, JSONUtils.toString(recipeSupportGiveModeList));
        return recipeSupportGiveModeList;
    }

    /**
     *  例外支付下载处方
     * @param configurations
     * @param recipeId
     * @param organId
     * @param recipeSupportGiveModeList
     * @return
     */
    private List<Integer> setOtherGiveMode(List<String> configurations,Integer recipeId, int organId,List<Integer> recipeSupportGiveModeList){
        // 查询药品是否不支持下载处方
        if (configurations.contains(RecipeSupportGiveModeEnum.DOWNLOAD_RECIPE.getText())) {
            Integer integer = organDrugListDAO.countIsSupperDownloadRecipe(organId, recipeId);
            if (integer == 0) {
                recipeSupportGiveModeList.add(RecipeSupportGiveModeEnum.DOWNLOAD_RECIPE.getType());
            }
        }
        // 例外支付 只要机构配置了就支持
        if (configurations.contains(RecipeSupportGiveModeEnum.SUPPORT_MEDICAL_PAYMENT.getText())) {
            recipeSupportGiveModeList.add(RecipeSupportGiveModeEnum.SUPPORT_MEDICAL_PAYMENT.getType());
        }
        return recipeSupportGiveModeList;
    }

    /**
     * 校验医院库存
     *
     * @param recipeSupportGiveModeList
     * @param recipeId
     * @return
     */
    private List<Integer> getGiveModeWhenContinueByHos(List<Integer> recipeSupportGiveModeList, Integer recipeId) {
        // 校验医院库存
        RecipeHisService hisService = ApplicationUtils.getRecipeService(RecipeHisService.class);
        RecipeResultBean scanResult = hisService.scanDrugStockByRecipeId(recipeId);
        LOGGER.info("getGiveModeWhenContinueByHos recipeId = {} ,scanResult = {} ", recipeId, JSONUtils.toString(scanResult));
        if (RecipeResultBean.SUCCESS.equals(scanResult.getCode())) {
            recipeSupportGiveModeList.add(RecipeSupportGiveModeEnum.SUPPORT_TO_HOS.getType());
        }
        return recipeSupportGiveModeList;
    }

    /**
     * 在药企有库存情况下 根据药企支持的购药方式与药企配送主体获取处方支持的购药方式
     *
     * @param recipeSupportGiveModeList
     * @param recipeId
     * @param organId
     * @return
     */
    private List<Integer> getGiveModeWhenContinueOne(List<Integer> recipeSupportGiveModeList, Integer recipeId, int organId) {
        List<Integer> list = new ArrayList<>();
        list.add(recipeId);
        // 获取所有有库存的药企
        List<DrugsEnterprise> supportDepList = recipeService.findSupportDepList(list, organId, null, false, null);
        LOGGER.info("getGiveModeWhenContinueOne recipeId = {} ,supportDepList = {} ", recipeId, JSONUtils.toString(supportDepList));
       return getGiveModeBuEnterprise(supportDepList,recipeSupportGiveModeList,recipeId,organId);
    }

    /**
     *  传入药企信息
     * @param supportDepList
     * @param recipeSupportGiveModeList
     * @param recipeId
     * @param organId
     * @return
     */
    private List<Integer> getGiveModeBuEnterprise(List<DrugsEnterprise> supportDepList,List<Integer> recipeSupportGiveModeList, Integer recipeId, int organId) {
        Set<Integer> sendTypes = new HashSet<>();
        // 获取所有药企支持的购药方式
        if (CollectionUtils.isNotEmpty(supportDepList)) {
            Set<Integer> collect = supportDepList.stream().map(drugsEnterprise -> {
                Integer payModeSupport = drugsEnterprise.getPayModeSupport();
                Integer sendType = drugsEnterprise.getSendType();
                sendTypes.add(sendType);
                if (RecipeDistributionFlagEnum.drugsEnterpriseAll.contains(payModeSupport)) {
                    return RecipeDistributionFlagEnum.DRUGS_HAVE.getType();
                } else if (RecipeDistributionFlagEnum.drugsEnterpriseTo.contains(payModeSupport)) {
                    return RecipeDistributionFlagEnum.DRUGS_HAVE_TO.getType();
                } else if (RecipeDistributionFlagEnum.drugsEnterpriseSend.contains(payModeSupport)) {
                    return RecipeDistributionFlagEnum.DRUGS_HAVE_SEND.getType();
                }
                return null;
            }).filter(Objects::nonNull).collect(Collectors.toSet());

            // 是否支持到店自取
            boolean drugHaveTo = collect.contains(RecipeDistributionFlagEnum.DRUGS_HAVE_TO.getType());
            // 是否支持配送
            boolean drugHaveSend = collect.contains(RecipeDistributionFlagEnum.DRUGS_HAVE_SEND.getType());
            // 根据药企的配送方式获取支持模式
            if (collect.contains(RecipeDistributionFlagEnum.DRUGS_HAVE.getType()) || (drugHaveTo && drugHaveSend)) {
                recipeSupportGiveModeList.add(RecipeSupportGiveModeEnum.SUPPORT_TFDS.getType());
                sendTypes(sendTypes, recipeSupportGiveModeList);
            } else if (drugHaveTo) {
                recipeSupportGiveModeList.add(RecipeSupportGiveModeEnum.SUPPORT_TFDS.getType());
            } else if (drugHaveSend) {
                // 根据配送主体区分医院配送还是药企配送
                sendTypes(sendTypes, recipeSupportGiveModeList);
            }
            LOGGER.info("getGiveModeWhenContinueOne  recipeId= {} recipeSupportGiveModeList= {}", recipeId, JSONUtils.toString(recipeSupportGiveModeList));
            return recipeSupportGiveModeList;
        } else {
            LOGGER.info("getGiveModeWhenContinueOne 药企没有库存 recipeId = {} recipeSupportGiveModeList= {}", recipeId, JSONUtils.toString(recipeSupportGiveModeList));
            return recipeSupportGiveModeList;
        }
    }
    /**
     * 根据配送主体获取购药方式
     *
     * @param sendTypes
     * @param recipeSupportGiveModeList
     */
    private void sendTypes(Set<Integer> sendTypes, List<Integer> recipeSupportGiveModeList) {
        if (CollectionUtils.isEmpty(sendTypes)) {
            recipeSupportGiveModeList.add(RecipeSupportGiveModeEnum.SHOW_SEND_TO_HOS.getType());
            recipeSupportGiveModeList.add(RecipeSupportGiveModeEnum.SHOW_SEND_TO_ENTERPRISES.getType());
            return;
        }
        boolean alReadyPay = sendTypes.contains(RecipeSendTypeEnum.ALRAEDY_PAY.getSendType());
        boolean noPay = sendTypes.contains(RecipeSendTypeEnum.NO_PAY.getSendType());
        if (alReadyPay && noPay) {
            recipeSupportGiveModeList.add(RecipeSupportGiveModeEnum.SHOW_SEND_TO_HOS.getType());
            recipeSupportGiveModeList.add(RecipeSupportGiveModeEnum.SHOW_SEND_TO_ENTERPRISES.getType());
            return;
        }
        if (alReadyPay) {
            recipeSupportGiveModeList.add(RecipeSupportGiveModeEnum.SHOW_SEND_TO_HOS.getType());
            return;
        }
        if (noPay) {
            recipeSupportGiveModeList.add(RecipeSupportGiveModeEnum.SHOW_SEND_TO_ENTERPRISES.getType());
        }
    }

}
