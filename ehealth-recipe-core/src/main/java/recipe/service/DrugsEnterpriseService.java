package recipe.service;

import com.alibaba.fastjson.JSONArray;
import com.google.common.collect.Lists;
import com.ngari.infra.logistics.mode.OrganLogisticsManageDto;
import com.ngari.infra.logistics.service.IOrganLogisticsManageService;
import com.ngari.opbase.util.OpSecurityUtil;
import com.ngari.patient.dto.OrganDTO;
import com.ngari.patient.service.OrganService;
import com.ngari.patient.service.PatientService;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.drugsenterprise.model.DrugEnterpriseLogisticsBean;
import com.ngari.recipe.drugsenterprise.model.DrugsEnterpriseBean;
import com.ngari.recipe.drugsenterprise.model.DrugsEnterpriseBeanNoDS;
import com.ngari.recipe.drugsenterprise.model.DrugsEnterpriseRes;
import com.ngari.recipe.entity.*;
import com.ngari.recipe.recipeorder.model.RecipeOrderBean;
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
import eh.utils.EscapeUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.ApplicationUtils;
import recipe.aop.LogRecord;
import recipe.constant.DrugEnterpriseConstant;
import recipe.constant.ErrorCode;
import recipe.core.api.IEnterpriseBusinessService;
import recipe.dao.*;
import recipe.drugsenterprise.RemoteDrugEnterpriseService;
import recipe.enumerate.type.RecipeSupportGiveModeEnum;
import recipe.manager.ButtonManager;
import recipe.manager.EnterpriseManager;
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
    private EnterpriseManager enterpriseManager;
    @Autowired
    private DrugsEnterpriseConfigService drugsEnterpriseConfigService;
    @Autowired
    private IOrganLogisticsManageService iOrganLogisticsManageService;

    @Autowired
    private IEnterpriseBusinessService enterpriseBusinessService;

    /**
     * 有效药企查询 status为1
     *
     * @param status 药企状态
     * @return
     */
    @RpcService
    public List<DrugsEnterpriseRes> findDrugsEnterpriseByStatus(final Integer status) {
        DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        List<DrugsEnterprise> list = drugsEnterpriseDAO.findAllDrugsEnterpriseByStatus(status);
        List<DrugsEnterpriseRes> drugsEnterpriseBeans = ObjectCopyUtils.convert(list, DrugsEnterpriseRes.class);
        return drugsEnterpriseBeans;
    }

    @RpcService
    public List<DrugsEnterprise> getDrugsEnterpriseByName(String name) {
        DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        List<DrugsEnterprise> drugsEnterpriseList = drugsEnterpriseDAO.findAllDrugsEnterpriseByName(name);
        return drugsEnterpriseList;
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

        //处理xxs漏洞
        String  enterpriseName=drugsEnterpriseBean.getName()==null?"":drugsEnterpriseBean.getName();
        drugsEnterpriseBean.setName(EscapeUtil.escapeHtml(enterpriseName));

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
        if (Objects.isNull(drugsEnterprise.getIsShowToday())) {
            drugsEnterprise.setIsShowToday(0);
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
        //新增药企配置
        DrugsEnterpriseConfig config = new DrugsEnterpriseConfig();
        config.setDrugsenterpriseId(newDrugsEnterprise.getId());
        if (newDrugsEnterprise.getCreateType() == 0) {
            config.setEnable_drug_sync(1);
            drugsEnterpriseConfigService.addOrUpdateDrugsEnterpriseConfig2(config);
        } else {
            DrugsEnterpriseConfigDAO dao = DAOFactory.getDAO(DrugsEnterpriseConfigDAO.class);
            config.setEnable_drug_sync(0);
            config.setSyncDataSource(1);
            dao.save(config);
            drugsEnterpriseConfigService.addSaleDrugListSyncFieldForEnterprise(config.getDrugsenterpriseId());
        }
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
        //处理xxs漏洞
        String  enterpriseName=drugsEnterpriseBean.getName()==null?"":drugsEnterpriseBean.getName();
        drugsEnterpriseBean.setName(EscapeUtil.escapeHtml(enterpriseName));

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
        if (null == drugsEnterprise.getFreeDeliveryMoney()) {
            target.setFreeDeliveryMoney(null);
        }
        target = drugsEnterpriseDAO.update(target);

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
    @LogRecord
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
        List<OrganLogisticsManageDto> organLogisticsManageDtos=iOrganLogisticsManageService.getRecipeLogisticsManageByOrganId(drugsEnterpriseId,0);
        LOGGER.info("getDrugsEnterpriseById organLogisticsManageDtos:{}",JSONUtils.toString(organLogisticsManageDtos));
        List<DrugEnterpriseLogisticsBean> drugEnterpriseLogisticsBeans  =new ArrayList<>();
        if(CollectionUtils.isNotEmpty(organLogisticsManageDtos)){
            organLogisticsManageDtos.forEach(organLogisticsManageDto -> {
                DrugEnterpriseLogisticsBean drugEnterpriseLogistics1=new DrugEnterpriseLogisticsBean();
                drugEnterpriseLogistics1.setCreateTime(organLogisticsManageDto.getCreateTime());
                drugEnterpriseLogistics1.setDrugsEnterpriseId(organLogisticsManageDto.getDrugsEnterpriseId());
                drugEnterpriseLogistics1.setId(organLogisticsManageDto.getOrganId());
                drugEnterpriseLogistics1.setIsDefault(organLogisticsManageDto.getIsDefault());
                drugEnterpriseLogistics1.setLogisticsCompany(organLogisticsManageDto.getLogisticsCode()==null?null:Integer.parseInt(organLogisticsManageDto.getLogisticsCode()));
                drugEnterpriseLogistics1.setLogisticsCompanyName(organLogisticsManageDto.getLogisticsCompanyName());
                drugEnterpriseLogistics1.setUpdateTime(organLogisticsManageDto.getUpdateTime());
                drugEnterpriseLogisticsBeans.add(drugEnterpriseLogistics1);
            });
        }
        drugsEnterpriseBean.setDrugEnterpriseLogisticsBeans(drugEnterpriseLogisticsBeans);
        return drugsEnterpriseBean;
    }

    /**
     * 运营平台-查询药企名
     * @param drugsEnterpriseId
     * @return
     */
    @RpcService
    public DrugsEnterpriseBeanNoDS getEnterpriseSimpleInfoByIdForOp(Integer drugsEnterpriseId){
        DrugsEnterpriseBeanNoDS enterprise=new DrugsEnterpriseBeanNoDS();
        DrugsEnterpriseBean bean = getDrugsEnterpriseById(drugsEnterpriseId);
        if(bean!=null){
            enterprise.setName(bean.getName());
        }

        return enterprise;
    }

    /**
     * 运营平台-查询药企详情
     * @param drugsEnterpriseId
     * @return
     */
    @RpcService
    public DrugsEnterpriseBeanNoDS getDrugsEnterpriseByIdForOp(Integer drugsEnterpriseId) {
        DrugsEnterpriseBean bean = getDrugsEnterpriseById(drugsEnterpriseId);
        UserRoleToken urt = UserRoleToken.getCurrent();
        String mu = urt.getManageUnit();
        if (bean != null) {
            if (!"eh".equals(mu) && null != bean.getOrganId()) {
                //越权
                List<OrganAndDrugsepRelation> relastionList=enterpriseBusinessService.findOrganAndDrugsepRelationBean(drugsEnterpriseId);
                List<Integer> organIdList= relastionList.stream().map(OrganAndDrugsepRelation::getOrganId)
                        .collect(Collectors.toList());
                if(organIdList==null ){
                    organIdList=Lists.newArrayList();
                }
                organIdList.add(bean.getOrganId());
                OpSecurityUtil.isAuthorisedOrgans(organIdList);
            }
        }
        return ObjectCopyUtils.convert(bean, DrugsEnterpriseBeanNoDS.class);
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

        if (result == null) {
            return result;
        }
        LOGGER.info(JSONUtils.toString(result));
        List<DrugsEnterpriseBean> drugsEnterpriseBeans = result.getItems();
        //不知道为啥不能直接循环drugsEnterpriseBeans 报转换异常,所以给转换了一下
        List<DrugsEnterpriseBean> drugsEnterpriseBeans1 = ObjectCopyUtils.convert(drugsEnterpriseBeans, DrugsEnterpriseBean.class);
        if (CollectionUtils.isNotEmpty(drugsEnterpriseBeans1)) {
            for (DrugsEnterpriseBean drugsEnterpriseBean : drugsEnterpriseBeans1) {
                drugsEnterpriseBean.setTel(null);
                drugsEnterpriseBean.setPassword(null);
                drugsEnterpriseBean.setAccount(null);
            }
        }
        result.setItems(drugsEnterpriseBeans1);
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
    @Deprecated
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

    @RpcService
    public List<DrugsEnterpriseBean> findDrugsEnterpriseByOrganId(Integer organId) {
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
        return enterpriseManager.checkEnterprise(organId);
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
        Integer type = RecipeSupportGiveModeEnum.getGiveModeType(order.getGiveModeKey());
        List<DrugsEnterprise> enterpriseList = enterpriseDAO.findByOrganIdAndPayModeSupport(organId, "%" + type.toString() + "%");
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

    private void checkUserHasPermission(RecipeOrderBean recipeOrder){
        PatientService patientService = ApplicationUtils.getBasicService(PatientService.class);
        UserRoleToken urt = UserRoleToken.getCurrent();
        String methodName = Thread.currentThread().getStackTrace()[2].getMethodName();
        if (recipeOrder != null){
            if ((urt.isPatient() && patientService.isPatientBelongUser(recipeOrder.getMpiId()))) {
                return;
            }else{
                LOGGER.error("当前用户没有权限调用orderId[{}],methodName[{}]", recipeOrder.getOrderId() ,methodName);
                throw new DAOException("当前登录用户没有权限");
            }
        }
    }
}
