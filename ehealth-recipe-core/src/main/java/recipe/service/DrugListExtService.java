package recipe.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.ngari.base.BaseAPI;
import com.ngari.base.property.service.IConfigurationCenterUtilsService;
import com.ngari.base.searchcontent.model.SearchContentBean;
import com.ngari.base.searchcontent.service.ISearchContentService;
import com.ngari.base.searchservice.model.DrugSearchTO;
import com.ngari.his.recipe.mode.*;
import com.ngari.his.recipe.mode.PatientDiagnosisDTO;
import com.ngari.his.recipe.service.IRecipeHisService;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.patient.service.PatientService;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.drug.model.*;
import com.ngari.recipe.entity.*;
import com.ngari.recipe.recipe.model.GiveModeButtonBean;
import com.ngari.recipe.recipe.model.GiveModeShowButtonVO;
import ctd.controller.exception.ControllerException;
import ctd.dictionary.DictionaryController;
import ctd.dictionary.DictionaryItem;
import ctd.persistence.DAOFactory;
import ctd.spring.AppDomainContext;
import ctd.util.AppContextHolder;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import ctd.util.event.GlobalEventExecFactory;
import es.api.DrugSearchService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.util.Args;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import recipe.ApplicationUtils;
import recipe.bean.HisSearchDrugDTO;
import static recipe.bussutil.RecipeUtil.getHospitalPrice;
import recipe.bussutil.drugdisplay.DrugDisplayNameProducer;
import recipe.bussutil.drugdisplay.DrugNameDisplayUtil;
import recipe.constant.RecipeBussConstant;
import recipe.dao.*;
import recipe.drugsenterprise.RemoteDrugEnterpriseService;
import recipe.givemode.business.GiveModeFactory;
import recipe.givemode.business.IGiveModeBase;
import recipe.serviceprovider.BaseService;
import recipe.util.MapValueUtil;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author： 0184/yu_yun
 * @date： 2018/7/25
 * @description： 原DrugListDAO层里的一些rpc方法
 * @version： 1.0
 */
@RpcBean("drugList")
public class DrugListExtService extends BaseService<DrugListBean> {

    /**
     * logger
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(DrugListExtService.class);

    private static final String SUPPORT_TO_HOS = "supportToHos";
    private static final String SUPPORT_SEND_TO_ENTERPRISES = "showSendToEnterprises";
    private static final String SUPPORT_SEND_TO_HOS = "showSendToHos";
    private static final String SUPPORT_TFDS = "supportTFDS";

    private static Pattern p = Pattern.compile("(?<=<em>).+?(?=</em>)");

    @Autowired
    private PatientService patientService;

    @RpcService
    public DrugListBean getById(int drugId) {
        DrugListDAO drugListDAO = DAOFactory.getDAO(DrugListDAO.class);
        DrugList drugList = drugListDAO.getById(drugId);
        return getBean(drugList, DrugListBean.class);
    }

    /**
     * 药品分类下的全部药品列表服务
     * （全部药品 drugClass 入参为空字符串）
     *
     * @param organId   医疗机构代码
     * @param drugClass 药品分类
     * @param start     分页起始位置
     * @return List<DrugList>
     * zhongzx 加 drugType
     * @author luf
     */
    @RpcService
    public List<DrugListBean> findAllInDrugClassByOrgan(int organId, int drugType, String drugClass, int start) {
        DrugListDAO drugListDAO = DAOFactory.getDAO(DrugListDAO.class);
        List<DrugList> dList = drugListDAO.findDrugListsByOrganOrDrugClass(organId, drugType, drugClass, start, 10);
        List<DrugListBean> drugListBeans = getList(dList, DrugListBean.class);
        // 添加医院药品数据
        if (!drugListBeans.isEmpty()) {
            getHospitalPrice(organId, drugListBeans);
        }
        //设置岳阳市人民医院药品库存
        setStoreIntroduce(organId, drugListBeans);
        return drugListBeans;
    }

    private void setStoreIntroduce(int organId, List<DrugListBean> drugListBeans) {
        if (organId == 1003083) {
            for (DrugListBean drugListBean : drugListBeans) {
                SaleDrugListDAO saleDrugListDAO = DAOFactory.getDAO(SaleDrugListDAO.class);
                DrugsEnterpriseDAO enterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
                List<DrugsEnterprise> drugsEnterprises = enterpriseDAO.findAllDrugsEnterpriseByName("岳阳-钥世圈");
                SaleDrugList saleDrugList = saleDrugListDAO.getByDrugIdAndOrganId(drugListBean.getDrugId(), drugsEnterprises.get(0).getId());
                if (saleDrugList != null) {
                    drugListBean.setInventory(saleDrugList.getInventory());
                }
            }
        }
    }

    /**
     * 查询his常用药品列表
     * @param drugDTO
     * @return
     */
    @RpcService
    public List<DrugListBean> findHisCommonDrugList(HisCommonDrugReqDTO drugDTO) {
        LOGGER.info("查询his常用药品列表入参={}", JSONObject.toJSONString(drugDTO));
        Args.notNull(drugDTO, "drugDTO");
        Args.notNull(drugDTO.getOrganId(),"organId");
        Args.notNull(drugDTO.getOrganName(),"organName");
        Args.notBlank(drugDTO.getLineCode(),"lineCode");
        Args.notNull(drugDTO.getDrugType(),"drugType");
        Args.notNull(drugDTO.getMpiId(),"mpiId");
        if (drugDTO.getDrugLimit() == null ){
            drugDTO.setDrugLimit(20);
        }
        Future<QueryDrugResTO> hisTask = GlobalEventExecFactory.instance().getExecutor().submit(() -> {
            QueryDrugReqTO reqTO = getHisCommonDrugReqTO(drugDTO);
            IRecipeHisService hisService = AppDomainContext.getBean("his.iRecipeHisService", IRecipeHisService.class);
            LOGGER.info("查询his常用药品列表--调用his开始，入参={}",JSONObject.toJSONString(reqTO));
            QueryDrugResTO result = hisService.queryHisCommonDrugList(reqTO);
            LOGGER.info("查询his常用药品列表--查询结果={}",JSONObject.toJSONString(result));
            return result;
        });
        QueryDrugResTO hisDrug = null;
        try {
            hisDrug = hisTask.get(5000, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            LOGGER.error("查询his常用药品列表--调用异常，入参={}",JSONObject.toJSONString(drugDTO),e);
        }

        List<DrugListBean> drugList = new ArrayList<>();
        if (null != hisDrug && null != hisDrug.getData() && CollectionUtils.isNotEmpty(hisDrug.getData().getDetails())){
            OrganDrugListDAO organDrugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);
            List<DrugDetailTO> hisDrugList = hisDrug.getData().getDetails();
            for (DrugDetailTO drug : hisDrugList){
                OrganDrugList organDrug = organDrugListDAO.getByOrganIdAndOrganDrugCode(drugDTO.getOrganId(),drug.getOrganDrugCode());
                if (null != organDrug){
                    DrugListBean drugListBean = getBean(organDrug,DrugListBean.class);
                    drugListBean.setHisciIsClaim(drug.getIsClaim());
                    drugListBean.setHisciReimburseRate(drug.getReimburse());
                    drugList.add(drugListBean);
                }
            }
        }
        // 添加医院数据
        if (CollectionUtils.isNotEmpty(drugList)) {
            drugList.forEach(a -> a.setDrugType(drugDTO.getDrugType()));
            getHospitalPrice(drugDTO.getOrganId(), drugList);
            DrugListDAO drugListDAO = DAOFactory.getDAO(DrugListDAO.class);
            DrugsEnterpriseService drugsEnterpriseService = ApplicationUtils.getRecipeService(DrugsEnterpriseService.class);
            for (DrugListBean drugListBean : drugList) {
                DrugList drugList1 = drugListDAO.getById(drugListBean.getDrugId());
                if (drugList != null) {
                    drugListBean.setPrice1(drugList1.getPrice1());
                    drugListBean.setPrice2(drugList1.getPrice2());
                }
                boolean drugInventoryFlag = drugsEnterpriseService.isExistDrugsEnterprise(drugDTO.getOrganId(), drugListBean.getDrugId());
                drugListBean.setDrugInventoryFlag(drugInventoryFlag);
            }
        }
        LOGGER.info("查询his常用药品列表--返回前端={}",JSONObject.toJSONString(drugList));
        return drugList;
    }

    /**
     * 组装his常用药品列表查询入参
     *
     * @param drugDTO
     * @return
     */
    private QueryDrugReqTO getHisCommonDrugReqTO(HisCommonDrugReqDTO drugDTO) {
        QueryDrugReqTO reqTO = new QueryDrugReqTO();
        reqTO.setOrganId(drugDTO.getOrganId());
        reqTO.setOrganName(drugDTO.getOrganName());
        reqTO.setDrugLimit(drugDTO.getDrugLimit());
        reqTO.setDeptCode(drugDTO.getDeptCode());
        reqTO.setDeptName(drugDTO.getDeptName());
        List<PatientDiagnosisDTO> diagnosisList = ObjectCopyUtils.convert(drugDTO.getDiagnosisList(), PatientDiagnosisDTO.class);
        reqTO.setDiagnosisList(diagnosisList);
        reqTO.setDoctorId(drugDTO.getDoctorId());
        reqTO.setDoctorName(drugDTO.getDoctorName());
        reqTO.setDrugType(drugDTO.getDrugType());
        reqTO.setIsInsurance(1);
        reqTO.setLineCode(drugDTO.getLineCode());
        reqTO.setMpiId(drugDTO.getMpiId());
        PatientDTO patient = patientService.getPatientByMpiId(drugDTO.getMpiId());
        if (null != patient){
            reqTO.setPatientName(patient.getPatientName());
            reqTO.setIdType(patient.getCertificateType());
            reqTO.setIdNumber(patient.getCertificate());
        }
        return reqTO;
    }

    /**
     * 搜索his药品信息
     *
     * @param searchDrug
     * @return
     */
    @RpcService
    public HisDrugInfoDTO searchHisDrugDetailForDortor(HisDrugInfoReqDTO searchDrug){
        LOGGER.info("搜索his药品列表入参={}", JSONObject.toJSONString(searchDrug));
        Args.notNull(searchDrug, "drugDTO");
        Args.notNull(searchDrug.getOrganId(),"organId");
        Args.notNull(searchDrug.getOrganName(),"organName");
        Args.notBlank(searchDrug.getLineCode(),"lineCode");
        Args.notNull(searchDrug.getDrugType(),"drugType");
        Args.notNull(searchDrug.getMpiId(),"mpiId");
        Args.notNull(searchDrug.getPageNum(),"pageNum");
        Integer pageSize = searchDrug.getPageSize() == null ? 20 : searchDrug.getPageSize();

        Future<HisSearchDrugDTO> hisTask = GlobalEventExecFactory.instance().getExecutor().submit(() -> {
            return queryHisDrugInfo(searchDrug, pageSize);
        });
        HisSearchDrugDTO searchDrugDTO = null;
        try {
            searchDrugDTO = hisTask.get(5000, TimeUnit.MILLISECONDS);
            LOGGER.info("查询his药品商保信息--查询结果={}",JSONObject.toJSONString(searchDrugDTO));
        } catch (Exception e) {
            LOGGER.error("查询his药品商保信息--调用异常，入参={}",JSONObject.toJSONString(searchDrug),e);
        }

        HisDrugInfoDTO result = handleDrugInfoResponse(searchDrug, pageSize, searchDrugDTO);
        LOGGER.info("查询his药品商保信息--返回前端={}",JSONObject.toJSONString(result));
        return result;
    }

    /**
     * 商保药品信息处理
     *
     * @param searchDrug
     * @param pageSize
     * @param searchDrugDTO
     * @return
     */
    private HisDrugInfoDTO handleDrugInfoResponse(HisDrugInfoReqDTO searchDrug, Integer pageSize, HisSearchDrugDTO searchDrugDTO) {
        List<SearchDrugDetailDTO> drugList = new ArrayList<>();
        QueryDrugResTO hisDrug = null== searchDrugDTO ? null : searchDrugDTO.getHisDrug();
        List<SearchDrugDetailDTO> searchList = searchDrugDTO == null ? null : searchDrugDTO.getSearchList();
        if (null != hisDrug && null != hisDrug.getData() && CollectionUtils.isNotEmpty(hisDrug.getData().getDetails())){
            Map<String, SearchDrugDetailDTO> detailMap = searchList.stream().collect(Collectors.toMap(SearchDrugDetailDTO::getOrganDrugCode, (drugdetail -> drugdetail)));
            OrganDrugListDAO organDrugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);
            List<DrugDetailTO> hisDrugList = hisDrug.getData().getDetails();
            //药品名拼接配置
            Map<String, Integer> configDrugNameMap = MapValueUtil.strArraytoMap(DrugNameDisplayUtil.getDrugNameConfigByDrugType(searchDrug.getOrganId(), searchDrug.getDrugType()));
            //药品商品名拼接配置
            Map<String, Integer> configSaleNameMap = MapValueUtil.strArraytoMap(DrugNameDisplayUtil.getSaleNameConfigByDrugType(searchDrug.getOrganId(), searchDrug.getDrugType()));
            for (DrugDetailTO drug : hisDrugList){
                OrganDrugList organDrug = organDrugListDAO.getByOrganIdAndOrganDrugCode(searchDrug.getOrganId(),drug.getOrganDrugCode());
                if (null != organDrug && null != detailMap.get(drug.getOrganDrugCode())){
                    SearchDrugDetailDTO drugListBean = detailMap.get(drug.getOrganDrugCode());
                    drugListBean.setHisciIsClaim(drug.getIsClaim());
                    drugListBean.setHisciReimburseRate(drug.getReimburse());
                    //前端展示的药品拼接名处理
                    drugListBean.setDrugDisplaySplicedName(DrugDisplayNameProducer.getDrugName(organDrug, configDrugNameMap, DrugNameDisplayUtil.getDrugNameConfigKey(searchDrug.getDrugType())));
                    //前端展示的药品商品名拼接名处理
                    drugListBean.setDrugDisplaySplicedSaleName(DrugDisplayNameProducer.getDrugName(organDrug, configSaleNameMap, DrugNameDisplayUtil.getSaleNameConfigKey(searchDrug.getDrugType())));
                    drugList.add(drugListBean);
                }
            }
        }
        //是否有下一页
        Boolean hasNextPage = (CollectionUtils.isEmpty(searchList) || searchList.size() < pageSize) ? false : true;
        HisDrugInfoDTO result = new HisDrugInfoDTO();
        result.setDrugDetailList(drugList);
        Integer nextPage = null == searchDrugDTO ? searchDrug.getPageNum() + 1 : searchDrugDTO.getNextPage();
        result.setNextPage(nextPage);
        result.setHasNextPage(hasNextPage);
        return result;
    }

    /**
     * 查询his商保药品信息
     *
     * @param searchDrug
     * @param pageSize
     * @return
     */
    private HisSearchDrugDTO queryHisDrugInfo(HisDrugInfoReqDTO searchDrug, Integer pageSize) {
        QueryDrugResTO hisDrug;
        Integer nextPage = searchDrug.getPageNum();
        Boolean search;
        List<SearchDrugDetailDTO> searchList;
        Integer pageNum = searchDrug.getPageNum();
        HisSearchDrugDTO searchResult = new HisSearchDrugDTO();

        do {
            nextPage = nextPage + 1;
            searchResult.setNextPage(nextPage);
            Integer startNum = (pageNum - 1) * pageSize;
            searchList = searchDrugListWithES(searchDrug.getOrganId(),
                    searchDrug.getDrugType(), searchDrug.getKeyWord(), null, startNum, pageSize);
            searchResult.setSearchList(searchList);
            if (CollectionUtils.isNotEmpty(searchList)){
                QueryDrugReqTO reqTO = getQueryDrugReqTO(searchDrug, searchList);
                LOGGER.info("查询his药品商保信息--开始查询，入参={}", JSONObject.toJSONString(reqTO));
                IRecipeHisService hisService = AppDomainContext.getBean("his.iRecipeHisService", IRecipeHisService.class);
                hisDrug = hisService.queryHisDrugInfo(reqTO);
                searchResult.setHisDrug(hisDrug);
                LOGGER.info("查询his药品商保信息--过滤结果={}",JSONObject.toJSONString(hisDrug));
                // 过滤后为0(请求成功但药品列表为空)，且es有下一页-->搜索es下一页
                if (searchList.size() == pageSize && null != hisDrug && "200".equals(hisDrug.getMsgCode()) && CollectionUtils.isEmpty(hisDrug.getData().getDetails())){
                    search = true;
                    pageNum = pageNum + 1;
                    LOGGER.info("查询his药品商保信息--当前页没有商保药品信息，查询下一页，es当前页查询结果数量={}",searchList.size());
                }else {
                    search = false;
                }
            }else {
                search = false;
            }
        }while (search);

        return searchResult;
    }

    /**
     * 获取商保药品信息入参
     *
     * @param searchDrug
     * @param searchList
     * @return
     */
    private QueryDrugReqTO getQueryDrugReqTO(HisDrugInfoReqDTO searchDrug, List<SearchDrugDetailDTO> searchList) {
        List<String> organDrugCodeList = searchList.stream().map(SearchDrugDetailDTO::getOrganDrugCode).collect(Collectors.toList());
        QueryDrugReqTO reqTO = new QueryDrugReqTO();
        reqTO.setOrganDrugCodeList(organDrugCodeList);
        reqTO.setOrganId(searchDrug.getOrganId());
        reqTO.setOrganName(searchDrug.getOrganName());
        reqTO.setDeptCode(searchDrug.getDeptCode());
        reqTO.setDeptName(searchDrug.getDeptName());
        List<PatientDiagnosisDTO> diagnosisList = ObjectCopyUtils.convert(searchDrug.getDiagnosisList(), PatientDiagnosisDTO.class);
        reqTO.setDiagnosisList(diagnosisList);
        reqTO.setDoctorId(searchDrug.getDoctorId());
        reqTO.setDoctorName(searchDrug.getDoctorName());
        reqTO.setDrugType(searchDrug.getDrugType());
        reqTO.setIsInsurance(1);
        reqTO.setLineCode(searchDrug.getLineCode());
        reqTO.setMpiId(searchDrug.getMpiId());
        PatientDTO patient = patientService.getPatientByMpiId(searchDrug.getMpiId() + "");
        if (null != patient){
            reqTO.setPatientName(patient.getPatientName());
            reqTO.setIdType(patient.getCertificateType());
            reqTO.setIdNumber(patient.getCertificate());
        }
        return reqTO;
    }



    /**
     * 常用药品列表服务new
     *
     * @return List<DrugList>
     * 新增 根据药房pharmacyId过滤
     */
    @RpcService
    public List<DrugListBean> findCommonDrugListsNew(CommonDrugListDTO commonDrugListDTO) {
        Args.notNull(commonDrugListDTO.getDoctor(), "doctor");
        Args.notNull(commonDrugListDTO.getDrugType(), "drugType");
        Args.notNull(commonDrugListDTO.getOrganId(), "organId");
        String pharmacyId = commonDrugListDTO.getPharmacyId() == null ? null : String.valueOf(commonDrugListDTO.getPharmacyId());
        DrugListDAO drugListDAO = DAOFactory.getDAO(DrugListDAO.class);
        DrugsEnterpriseService drugsEnterpriseService = ApplicationUtils.getRecipeService(DrugsEnterpriseService.class);
        List<OrganDrugList> dList = drugListDAO.findCommonDrugListsWithPage(commonDrugListDTO.getDoctor(), commonDrugListDTO.getOrganId(), commonDrugListDTO.getDrugType(), pharmacyId, 0, 20);
        //支持开西药（含中成药）的临时解决方案  如果是西药或者中成药就检索两次
        Boolean isMergeRecipeType = null;
        try {
            IConfigurationCenterUtilsService configurationService = ApplicationUtils.getBaseService(IConfigurationCenterUtilsService.class);
            isMergeRecipeType = (Boolean) configurationService.getConfiguration(commonDrugListDTO.getOrganId(), "isMergeRecipeType");
        } catch (Exception e) {
            LOGGER.error("获取运营平台处方支付配置异常:isMergeRecipeType。", e);
        }
        if (isMergeRecipeType != null && isMergeRecipeType == true) {
            if (1 == commonDrugListDTO.getDrugType()) {
                commonDrugListDTO.setDrugType(2);
            } else if (2 == commonDrugListDTO.getDrugType()) {
                commonDrugListDTO.setDrugType(1);
            } else {
                isMergeRecipeType = false;
            }
            if (isMergeRecipeType) {
                List<OrganDrugList> dList2 = drugListDAO.findCommonDrugListsWithPage(commonDrugListDTO.getDoctor(), commonDrugListDTO.getOrganId(), commonDrugListDTO.getDrugType(), pharmacyId, 0, 20 - dList.size());

                if (dList != null && dList2 != null && dList2.size() != 0) {
                    dList.addAll(dList2);
                }
            }
        }


        List<DrugListBean> drugListBeans = getList(dList, DrugListBean.class);
        // 添加医院数据
        if (CollectionUtils.isNotEmpty(drugListBeans)) {
            drugListBeans.forEach(a -> a.setDrugType(commonDrugListDTO.getDrugType()));
            getHospitalPrice(commonDrugListDTO.getOrganId(), drugListBeans);
        }
        if (CollectionUtils.isNotEmpty(drugListBeans)) {
            for (DrugListBean drugListBean : drugListBeans) {
                DrugList drugList = drugListDAO.getById(drugListBean.getDrugId());
                if (drugList != null) {
                    drugListBean.setPrice1(drugList.getPrice1());
                    drugListBean.setPrice2(drugList.getPrice2());
                }
                boolean drugInventoryFlag = drugsEnterpriseService.isExistDrugsEnterprise(commonDrugListDTO.getOrganId(), drugListBean.getDrugId());
                drugListBean.setDrugInventoryFlag(drugInventoryFlag);
            }
        }
        //设置岳阳市人民医院药品库存
        setStoreIntroduce(commonDrugListDTO.getOrganId(), drugListBeans);
        return drugListBeans;
    }

    /**
     * 该方法已经挪到queryDrugInventoriesByRealTime方法了--这里只做兼容使用
     * @param organId
     * @param drugListBeans
     * @param pharmacyId
     */
    @Deprecated
    private void setInventoriesIfRealTime(Integer organId, List<? extends IDrugInventory> drugListBeans,
                                          @Nullable Integer pharmacyId) {
        try {
            if (CollectionUtils.isEmpty(drugListBeans) || !isViewInventoryRealtime(organId)) {
                return;
            }

            // 如果实时查询库存
            // 1. 调用his前置接口查询医院库存并赋值
            OrganDrugListDAO drugDao = DAOFactory.getDAO(OrganDrugListDAO.class);
            List<Integer> drugIds = drugListBeans.stream().map(IHisDrugInventoryCondition::getDrugId)
                    .collect(Collectors.toList());
            List<OrganDrugList> organDrugLists = drugDao.findByOrganIdAndDrugIds(organId, drugIds);
            DrugInfoResponseTO hisResp = this.getHisDrugStock(organId, organDrugLists, pharmacyId);
            if (hisResp == null || CollectionUtils.isEmpty(hisResp.getData())) {
//                || hisResp.getMsgCode() != null && !hisResp.getMsgCode().equals(200)
                List<DrugInventoryInfo> drugInventoryInfos = new ArrayList<>();
                if (hisResp == null){
                    // 说明查询错误, 或者
                    drugInventoryInfos.add(new DrugInventoryInfo("his", null, "1"));
                    for (IDrugInventory drugListBean : drugListBeans) {
                        drugListBean.setInventories(drugInventoryInfos);
                    }
                }else {
                    String amount;
                    if (Integer.valueOf(0).equals(hisResp.getMsgCode()) || Integer.valueOf(200).equals(hisResp.getMsgCode())){
                        amount = "有库存";
                    }else {
                        amount = "无库存";
                    }
                    drugInventoryInfos.add(new DrugInventoryInfo("his", Lists.newArrayList(new DrugPharmacyInventoryInfo(amount)), "0"));
                    for (IDrugInventory drugListBean : drugListBeans) {
                        drugListBean.setInventories(drugInventoryInfos);
                    }
                }
            } else {
                for (IDrugInventory drugListBean : drugListBeans) {
                    List<DrugInfoTO> drugInfoTOListMatched = findDrugInfoTOList(drugListBean, hisResp.getData());
                    List<DrugInventoryInfo> drugInventoryInfos = new ArrayList<>();
                    DrugInventoryInfo drugInventory = new DrugInventoryInfo("his", null, "0");
                    drugInventory.setPharmacyInventories(convertFrom(drugInfoTOListMatched));
                    drugInventoryInfos.add(drugInventory);
                    drugListBean.setInventories(drugInventoryInfos);
                }
            }

            // 2. 调用药企接口查询药企库存并赋值
        } catch (Exception e) {
            LOGGER.error("药品实时查询库存错误setInventoriesIfRealTime ", e);
        }
    }

    private List<DrugPharmacyInventoryInfo> convertFrom(List<DrugInfoTO> drugInfoTOList) {
        if (CollectionUtils.isEmpty(drugInfoTOList)) {
            return new ArrayList<>();
        }
        List<DrugPharmacyInventoryInfo> pharmacyInventories = new ArrayList<>(drugInfoTOList.size());
        String amount;
        for (DrugInfoTO drugInfoTO : drugInfoTOList) {
            DrugPharmacyInventoryInfo pharmacyInventory = new DrugPharmacyInventoryInfo();
            pharmacyInventory.setPharmacyCode(drugInfoTO.getPharmacyCode());
            pharmacyInventory.setPharmacyName(drugInfoTO.getPharmacy());
            if (drugInfoTO.getStockAmount() == null){
                amount = "有库存";
            }else {
                amount = BigDecimal.valueOf(drugInfoTO.getStockAmount()).toPlainString();
            }
            // 改成字符串返回
            pharmacyInventory.setAmount(amount);
            //设置到院取药的类型
            pharmacyInventory.setType(3);
            pharmacyInventories.add(pharmacyInventory);
        }
        return pharmacyInventories;
    }

    /***
     *  <p>注意：目前通过前置机开发人员确认：只需上送机构（医院）药品编码OrganDrugCode，
     *  * 针对一个药品编码OrganDrugCode返回>1条不同规格的药品时，参照前置机校验库存的做法：
     *  * 匹配时也只需按机构（医院）药品编码去匹配，不考虑同种药品的不同规格
     *  * （前置机做药品库存是否充足时也是不考虑规格的）</>
     * @param drugListBean
     * @param drugInfoTOList
     * @return
     */
    private List<DrugInfoTO> findDrugInfoTOList(IDrugInventory drugListBean, List<DrugInfoTO> drugInfoTOList) {
        return drugInfoTOList.stream().filter(item ->
                drugListBean.getOrganDrugCode().equalsIgnoreCase(item.getDrcode()))
                .collect(Collectors.toList());
    }

    private boolean isViewInventoryRealtime(Integer organId) {
        IConfigurationCenterUtilsService configService = BaseAPI.getService(IConfigurationCenterUtilsService.class);
        try {
            Integer cfgValue = (Integer) configService.getConfiguration(organId, "viewDrugInventoryRealTime");
            return cfgValue.equals(1);
        } catch (Exception e) {
            LOGGER.error("获取参数viewDrugInventoryRealTime 错误 ", e);
            return false;
        }
    }

    /***
     *  <p>注意：目前通过前置机开发人员确认：只需上送机构（医院）药品编码OrganDrugCode，
     *  * 针对一个药品编码OrganDrugCode返回>1条不同规格的药品时，参照前置机校验库存的做法：
     *  * 匹配时也只需按机构（医院）药品编码去匹配，不考虑同种药品的不同规格
     *  * （前置机做药品库存是否充足时也是不考虑规格的）</>
     * @param organId 机构id
     * @param organDrugList 只需OrganDrugList对象的OrganDrugCode，drugId
     * @param pharmacyId 药房id，可为null
     * @return DrugInfoResponseTO.code = 0
     */
    public DrugInfoResponseTO getHisDrugStock(Integer organId,
                                              List<OrganDrugList> organDrugList,
                                              @Nullable Integer pharmacyId) {

        IRecipeHisService hisService = AppDomainContext.getBean("his.iRecipeHisService", IRecipeHisService.class);

        // 从同事那儿得知有的医院需要ProduceCode
        Map<String, List<OrganDrugList>> drugIdProduceMap = organDrugList.stream()
                .collect(Collectors.groupingBy(OrganDrugList::getOrganDrugCode));

        // 构建请求体
        DrugInfoRequestTO request = new DrugInfoRequestTO();
        // 1-查询并校验库存是否充足；2-查询库存；传空默认1
        request.setType("2");
        request.setOrganId(organId);
        List<DrugInfoTO> data = new ArrayList<>(organDrugList.size());
        for (OrganDrugList organDrugItem : organDrugList) {
            DrugInfoTO drugInfo = new DrugInfoTO(organDrugItem.getOrganDrugCode());
            List<OrganDrugList> organDrugs = drugIdProduceMap.get(organDrugItem.getOrganDrugCode());
            if (CollectionUtils.isNotEmpty(organDrugs)) {
                Map<Integer, String> producerCodeMap = organDrugs.stream().collect(
                        Collectors.toMap(OrganDrugList::getDrugId, OrganDrugList::getProducerCode));
                String producerCode = producerCodeMap.get(organDrugItem.getDrugId());
                if (StringUtils.isNotEmpty(producerCode)) {
                    drugInfo.setManfcode(producerCode);
                }
            }

            // 药房
            if (pharmacyId != null) {
                PharmacyTcmDAO pharmacyTcmDAO = DAOFactory.getDAO(PharmacyTcmDAO.class);
                PharmacyTcm pharmacyTcm = pharmacyTcmDAO.get(pharmacyId);
                if (pharmacyTcm != null) {
                    drugInfo.setPharmacyCode(pharmacyTcm.getPharmacyCode());
                    drugInfo.setPharmacy(pharmacyTcm.getPharmacyName());
                }
            }
            data.add(drugInfo);
        }

        request.setData(data);
        LOGGER.info("getDrugStock request={}", JSONUtils.toString(request));
        DrugInfoResponseTO response;
        try {
            response = hisService.scanDrugStock(request);
            LOGGER.info("getDrugStock response={}", JSONUtils.toString(response));
            return response;
        } catch (Exception e) {
            LOGGER.error("getDrugStock error ", e);
            response = new DrugInfoResponseTO();
            response.setMsgCode(-1);
        }
        return response;
    }

    /**
     * 常用药品列表服务
     *
     * @param doctor 开方医生
     * @return List<DrugList>
     * zhongzx 加 organId,drugType
     * @author luf
     */
    @RpcService
    public List<DrugListBean> findCommonDrugLists(int doctor, int organId, int drugType) {
        CommonDrugListDTO dto = new CommonDrugListDTO(doctor, organId, drugType);
        return findCommonDrugListsNew(dto);
    }

    /**
     * 获取一个药品类别下面的第一子集和第二子集，重新组装
     *
     * @param parentKey 父级
     * @return
     * @author zhangx
     * @date 2015-12-7 下午7:42:26
     */
    @RpcService
    public List<HashMap<String, Object>> findDrugClass(String parentKey) {
        List<HashMap<String, Object>> returnList = new ArrayList<HashMap<String, Object>>();

        List<DictionaryItem> list = getDrugClass(parentKey, 3);
        for (DictionaryItem dictionaryItem : list) {
            HashMap<String, Object> map = Maps.newHashMap();
            map.put("key", dictionaryItem.getKey());
            map.put("text", dictionaryItem.getText());
            map.put("leaf", dictionaryItem.isLeaf());
            map.put("index", dictionaryItem.getIndex());
            map.put("mcode", dictionaryItem.getMCode());
            map.put("child", getDrugClass(dictionaryItem.getKey(), 3));
            returnList.add(map);
        }
        return returnList;
    }

    /**
     * 药品目录搜索服务（每页限制10条）
     *
     * @param drugName 药品名称
     * @param start    分页起始位置
     * @return List<DrugList>
     * zhongzx 加 organId,drugType
     * @author luf
     */
    @RpcService
    public List<SearchDrugDetailDTO> findDrugListsByNameOrCodePageStaitc(Integer organId, Integer drugType, String drugName, int start) {

        if (null == organId) {
            //患者查询药品
            return searchDrugListWithESForPatient(organId, drugType, drugName, start, 10);
        } else {
            //医生查询药品信息
            return searchDrugListWithES(organId, drugType, drugName, null, start, 10);
        }

    }

    /**
     * 药品目录搜索服务医生端NEW（每页限制10条）
     * 新增-可根据药品药房进行搜索
     */
    @RpcService
    public List<SearchDrugDetailDTO> findDrugListsByNameOrCodePageStaticNew(SearchDrugDetailReqDTO req) {
        LOGGER.info("findDrugListsByNameOrCodePageStaticNew req={}", JSONUtils.toString(req));
        //医生查询药品信息
        List<SearchDrugDetailDTO> resultList = searchDrugListWithES(req.getOrganId(),
                req.getDrugType(), req.getDrugName(), req.getPharmacyId(), req.getStart(), 10);
        return resultList;
    }


    /**
     * 患者端药品目录搜索并保存搜索记录
     *
     * @param organId
     * @param drugType
     * @param drugName
     * @param start
     * @param MPIID
     * @return
     */
    @RpcService
    public List<SearchDrugDetailDTO> findDrugListsByNameOrCodeAndSaveRecord(Integer organId, Integer drugType, String drugName, int start, String MPIID) {
        if (StringUtils.isNotEmpty(drugName) && StringUtils.isNotEmpty(MPIID)) {
            ISearchContentService iSearchContentService = ApplicationUtils.getBaseService(ISearchContentService.class);
            SearchContentBean searchContentBean = new SearchContentBean();
            searchContentBean.setMpiId(MPIID);
            searchContentBean.setContent(drugName);
            searchContentBean.setBussType(18);
            iSearchContentService.addSearchContent(searchContentBean, 0);
        }
        return searchDrugListWithES(organId, drugType, drugName, null, start, 10);
    }


    /**
     * zhongzx
     * 搜索药品 使用es新方式搜索
     *
     * @return
     */
    public List<SearchDrugDetailDTO> searchDrugListWithES(Integer organId, Integer drugType, String drugName, Integer pharmacyId, Integer start, Integer limit) {
        DrugSearchService searchService = AppContextHolder.getBean("es.drugSearchService", DrugSearchService.class);
        SaleDrugListDAO saleDrugListDAO = DAOFactory.getDAO(SaleDrugListDAO.class);
        DrugsEnterpriseDAO enterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        DrugsEnterpriseService drugsEnterpriseService = ApplicationUtils.getRecipeService(DrugsEnterpriseService.class);
        DrugSearchTO searchTO = new DrugSearchTO();
        searchTO.setDrugName(StringUtils.isEmpty(drugName) ? "" : drugName.toLowerCase());
        searchTO.setOrgan(null == organId ? null : String.valueOf(organId));
        searchTO.setDrugType(null == drugType ? "" : String.valueOf(drugType));
        searchTO.setStart(start);
        searchTO.setLimit(limit);
        LOGGER.info("searchDrugListWithES DrugSearchTO={} ", JSONUtils.toString(searchTO));
        List<String> drugInfo = searchService.searchHighlightedPagesForDoctor(searchTO.getDrugName(), searchTO.getOrgan(), searchTO.getDrugType(), pharmacyId, searchTO.getStart(), searchTO.getLimit());
        LOGGER.info("searchDrugListWithES drugInfo={} ", JSON.toJSONString(drugInfo));
        //支持开西药（含中成药）的临时解决方案  如果是西药或者中成药就检索两次，分页可能有问题时间紧急后面再说
        Boolean isMergeRecipeType = null;
        try {
            IConfigurationCenterUtilsService configurationService = ApplicationUtils.getBaseService(IConfigurationCenterUtilsService.class);
            isMergeRecipeType = (Boolean) configurationService.getConfiguration(organId, "isMergeRecipeType");
        } catch (Exception e) {
            LOGGER.error("获取运营平台处方支付配置异常:isMergeRecipeType。", e);
        }
        if (isMergeRecipeType != null && isMergeRecipeType == true) {
            if (drugType != null && 1 == drugType) {
                searchTO.setDrugType("2");
            } else if (drugType != null && 2 == drugType) {
                searchTO.setDrugType("1");
            } else {
                //bug# 中药或者膏方会重复搜索
                isMergeRecipeType = false;
            }
            if (isMergeRecipeType) {
                searchTO.setLimit((limit - drugInfo.size()) == 0 ? 1 : (limit - drugInfo.size()));
                List<String> drugInfo2 = searchService.searchHighlightedPagesForDoctor(searchTO.getDrugName(), searchTO.getOrgan(), searchTO.getDrugType(), pharmacyId, searchTO.getStart(), searchTO.getLimit());
                LOGGER.info("searchDrugListWithES drugInfo2={} ", JSON.toJSONString(drugInfo2));
                if (drugInfo != null && drugInfo2 != null && drugInfo2.size() != 0) {
                    drugInfo.addAll(drugInfo2);
                }
            }
        }

        List<SearchDrugDetailDTO> dList = new ArrayList<>(drugInfo.size());
        // 将String转化成DrugList对象返回给前端
        if (CollectionUtils.isNotEmpty(drugInfo)) {
            SearchDrugDetailDTO drugList = null;
            DrugListDAO drugListDAO = DAOFactory.getDAO(DrugListDAO.class);
            //获取药品展示拼接配置
            //药品名拼接配置---这里处理防止每次循环还得处理一遍
            Map<String, Integer> configDrugNameMap = MapValueUtil.strArraytoMap(DrugNameDisplayUtil.getDrugNameConfigByDrugType(organId, drugType));
            //药品商品名拼接配置
            Map<String, Integer> configSaleNameMap = MapValueUtil.strArraytoMap(DrugNameDisplayUtil.getSaleNameConfigByDrugType(organId, drugType));
            DrugList drugListNow;
            boolean drugInventoryFlag;
            List<UseDoseAndUnitRelationBean> useDoseAndUnitRelationList;
            for (String s : drugInfo) {
                drugList = JSONUtils.parse(s, SearchDrugDetailDTO.class);
                drugList.setHospitalPrice(drugList.getSalePrice());
                //该高亮字段给微信端使用:highlightedField
                //该高亮字段给ios前端使用:highlightedFieldForIos
                if (null != drugList && StringUtils.isNotEmpty(drugList.getHighlightedField())) {
                    drugList.setHighlightedFieldForIos(getListByHighlightedField(drugList.getHighlightedField()));
                }
                if (null != drugList && StringUtils.isNotEmpty(drugList.getHighlightedField2())) {
                    drugList.setHighlightedFieldForIos2(getListByHighlightedField(drugList.getHighlightedField2()));
                }
                if (null != drugList && StringUtils.isEmpty(drugList.getUsingRate())) {
                    drugList.setUsingRate("");
                }
                if (null != drugList && StringUtils.isEmpty(drugList.getUsePathways())) {
                    drugList.setUsePathways("");
                }
                //针对岳阳市人民医院增加库存
                if (organId != null && organId == 1003083) {
                    List<DrugsEnterprise> drugsEnterprises = enterpriseDAO.findAllDrugsEnterpriseByName("岳阳-钥世圈");
                    SaleDrugList saleDrugList = saleDrugListDAO.getByDrugIdAndOrganId(drugList.getDrugId(), drugsEnterprises.get(0).getId());
                    if (saleDrugList != null) {
                        drugList.setInventory(saleDrugList.getInventory());
                    }
                }
                drugListNow = drugListDAO.getById(drugList.getDrugId());
                //添加es价格空填值逻辑
                if (null != drugListNow) {
                    drugList.setPrice1(null == drugList.getPrice1() ? drugListNow.getPrice1() : drugList.getPrice1());
                    drugList.setPrice2(null == drugList.getPrice2() ? drugListNow.getPrice2() : drugList.getPrice2());
                }
                //药品库存标志-是否查药企库存
                if (organId != null) {
                    drugInventoryFlag = drugsEnterpriseService.isExistDrugsEnterprise(organId, drugList.getDrugId());
                    drugList.setDrugInventoryFlag(drugInventoryFlag);
                }
                //设置医生端每次剂量和剂量单位联动关系
                useDoseAndUnitRelationList = Lists.newArrayList();
                //用药单位不为空时才返回给前端
                if (StringUtils.isNotEmpty(drugList.getUseDoseUnit())) {
                    useDoseAndUnitRelationList.add(new UseDoseAndUnitRelationBean(drugList.getRecommendedUseDose(), drugList.getUseDoseUnit(), drugList.getUseDose()));
                }
                if (StringUtils.isNotEmpty(drugList.getUseDoseSmallestUnit())) {
                    useDoseAndUnitRelationList.add(new UseDoseAndUnitRelationBean(drugList.getDefaultSmallestUnitUseDose(), drugList.getUseDoseSmallestUnit(), drugList.getSmallestUnitUseDose()));
                }
                drugList.setUseDoseAndUnitRelation(useDoseAndUnitRelationList);
                //前端展示的药品拼接名处理
                drugList.setDrugDisplaySplicedName(DrugDisplayNameProducer.getDrugName(drugList, configDrugNameMap, DrugNameDisplayUtil.getDrugNameConfigKey(drugType)));
                //前端展示的药品商品名拼接名处理
                drugList.setDrugDisplaySplicedSaleName(DrugDisplayNameProducer.getDrugName(drugList, configSaleNameMap, DrugNameDisplayUtil.getSaleNameConfigKey(drugType)));
                dList.add(drugList);
            }
            LOGGER.info("searchDrugListWithES result DList.size = " + dList.size());
        } else {
            LOGGER.info("searchDrugListWithES result isEmpty! drugName = " + drugName);
            //organDrugListDAO.findByDrugNameLikeNew(organId,drugName,start,limit);
        }
        return dList;
    }

    public List<SearchDrugDetailDTO> searchDrugListWithESForPatient(Integer organId, Integer drugType, String drugName, Integer start, Integer limit) {
        DrugSearchService searchService = AppContextHolder.getBean("es.drugSearchService", DrugSearchService.class);
        OrganDrugListDAO organDrugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);
        DrugSearchTO searchTO = new DrugSearchTO();
        searchTO.setDrugName(StringUtils.isEmpty(drugName) ? "" : drugName.toLowerCase());
        searchTO.setOrgan(null == organId ? null : String.valueOf(organId));
        searchTO.setDrugType(null == drugType ? "" : String.valueOf(drugType));
        searchTO.setStart(start);
        searchTO.setLimit(limit);
        LOGGER.info("searchDrugListWithESForPatient DrugSearchTO={} ", JSONUtils.toString(searchTO));
        List<String> drugInfo = searchService.searchHighlightedPagesForPatient(searchTO.getDrugName(), searchTO.getOrgan(), searchTO.getDrugType(), searchTO.getStart(), searchTO.getLimit());
        List<SearchDrugDetailDTO> dList = new ArrayList<>(drugInfo.size());
        // 将String转化成DrugList对象返回给前端
        if (CollectionUtils.isNotEmpty(drugInfo)) {
            SearchDrugDetailDTO drugList = null;
            for (String s : drugInfo) {
                try {
                    drugList = JSONUtils.parse(s, SearchDrugDetailDTO.class);
                    List<OrganDrugList> organDrugLists = organDrugListDAO.findByDrugIdAndOrganId(drugList.getDrugId(), organId);
                    drugList.setHospitalPrice(organDrugLists.get(0).getSalePrice());
                } catch (Exception e) {
                    LOGGER.error("searchDrugListWithESForPatient parse error. drugInfo={}", s, e);
                }
                dList.add(drugList);
            }

            LOGGER.info("searchDrugListWithESForPatient result size={} ", dList.size());
        } else {
            LOGGER.info("searchDrugListWithESForPatient result isEmpty! drugName={} ", drugName);
        }
        return dList;
    }

    /**
     * 用正则截取指定标记间的字符串
     *
     * @param highlightedField
     * @return
     */
    public List<String> getListByHighlightedField(String highlightedField) {
        List list = new ArrayList();
        Matcher m = p.matcher(highlightedField);
        while (m.find()) {
            list.add(m.group().trim());
        }
//        LOGGER.info("highlightedField is " + list.toString());
        return list;
    }

    /**
     * 获取药品类别
     *
     * @param parentKey 父节点值
     * @param sliceType --0所有子节点 1所有叶子节点 2所有文件夹节点 3所有子级节点 4所有子级叶子节点 5所有子级文件夹节点
     * @return List<DictionaryItem>
     * @author luf
     */
    @RpcService
    public List<DictionaryItem> getDrugClass(String parentKey, int sliceType) {
        List<DictionaryItem> list = new ArrayList<DictionaryItem>();
        try {
            list = DictionaryController.instance().get("eh.base.dictionary.DrugClass").getSlice(parentKey, sliceType, "");
        } catch (ControllerException e) {
            LOGGER.error("getDrugClass() error : ", e);
        }
        return list;
    }

    /**
     * 根据药品Id获取药品记录
     * (不包括机构没有配置的药品)
     *
     * @param drugId 药品id
     * @return
     * @author yaozh
     */
    @RpcService
    public DrugListBean findByDrugIdAndOrganId(int drugId) {
        DrugListDAO drugListDAO = DAOFactory.getDAO(DrugListDAO.class);
        DrugList drugList = drugListDAO.findByDrugIdAndOrganId(drugId);
        return getBean(drugList, DrugListBean.class);
    }


    /**
     * 获取存在有效药品目录的一级、二级、三级类目(西药)；一级、二级（中成药）
     * zhongzx
     *
     * @param organId
     * @param drugType
     * @return
     */
    @RpcService
    public List<HashMap<String, Object>> findAllClassByDrugType(int organId, int drugType) {
        List<HashMap<String, Object>> returnList = new ArrayList<HashMap<String, Object>>();

        //先获得一级有效类目
        List<DictionaryItem> firstList = findChildByDrugClass(organId, drugType, "");

        for (DictionaryItem first : firstList) {
            List<HashMap<String, Object>> childList = Lists.newArrayList();
            HashMap<String, Object> map = Maps.newHashMap();
            map.put("key", first.getKey());
            map.put("text", first.getText());
            map.put("leaf", first.isLeaf());
            map.put("index", first.getIndex());
            map.put("mcode", first.getMCode());
            map.put("child", childList);
            List<DictionaryItem> list = findChildByDrugClass(organId, drugType, first.getKey());
            if (null != list && list.size() != 0) {
                for (DictionaryItem dictionaryItem : list) {
                    HashMap<String, Object> map1 = Maps.newHashMap();
                    map1.put("key", dictionaryItem.getKey());
                    map1.put("text", dictionaryItem.getText());
                    map1.put("leaf", dictionaryItem.isLeaf());
                    map1.put("index", dictionaryItem.getIndex());
                    map1.put("mcode", dictionaryItem.getMCode());
                    //如果是中成药 就不用判断是否有第三级类目 它只有二级类目
                    if (drugType == 1) {
                        //判断是否有第三级类目 如果有则显示 如果没有 以第二类目的名称命名生成一个第三子类
                        List<DictionaryItem> grandchild = findChildByDrugClass(organId, drugType, dictionaryItem.getKey());
                        if (null != grandchild && 0 != grandchild.size()) {
                            map1.put("grandchild", grandchild);
                        } else {
                            List one = new ArrayList();
                            one.add(dictionaryItem);
                            map1.put("grandchild", one);
                        }
                    }
                    childList.add(map1);
                }
            } else {
                HashMap<String, Object> map1 = Maps.newHashMap();
                map1.put("key", first.getKey());
                map1.put("text", first.getText());
                map1.put("leaf", first.isLeaf());
                map1.put("index", first.getIndex());
                map1.put("mcode", first.getMCode());
                childList.add(map1);
            }
            returnList.add(map);
        }
        return returnList;
    }

    /**
     * 查找存在有效药品的 类目(第一级类目传空)
     * zhongzx
     *
     * @param parentKey
     * @return
     */
    @RpcService
    public List<DictionaryItem> findChildByDrugClass(Integer organId, Integer drugType, String parentKey) {
        return findDrugClassByDrugType(organId, drugType, parentKey);
    }

    /**
     * 获得 对应机构 对应药品类型 存在有效药品目录的某级药品类目。
     * zhongzx
     *
     * @param organId
     * @param drugType
     * @return
     */
    public List<DictionaryItem> findDrugClassByDrugType(Integer organId, Integer drugType, String parentKey) {
        //从数据库进行筛选
        DrugListDAO drugListDAO = DAOFactory.getDAO(DrugListDAO.class);
        List<DrugList> drugList = drugListDAO.findDrugClassByDrugList(organId, drugType, parentKey, null, null);
        List<DictionaryItem> allItemList = getDrugClass(parentKey, 3);
        List<DictionaryItem> itemList = new ArrayList<>();

        for (DictionaryItem item : allItemList) {
            for (DrugList d : drugList) {
                //根据药品类目 是不是以 某级类目的key值开头的 来判断
                if (d.getDrugClass().startsWith(item.getKey())) {
                    itemList.add(item);
                    break;
                }
            }
        }
        //现在 按照字典的录入顺序显示
        return itemList;
    }

    /**
     * 医生端 搜索药品页和常用药品页----实时显示药品库存
     * @param req
     * @return
     */
    @RpcService
    public List<DrugListBean> queryDrugInventoriesByRealTime(QueryDrugInventoriesDTO req){
        LOGGER.info("queryDrugInventoriesByRealTime req:{}", JSONUtils.toString(req));
        Assert.notNull(req,"req is required");
        Assert.notNull(req.getOrganId(),"organId is required");
        Assert.notEmpty(req.getDrugIds(), "drugIds is required");
        List<DrugListBean> drugListBeans = new ArrayList<>(req.getDrugIds().size());
        req.getDrugIds().forEach(a-> drugListBeans.add(new DrugListBean(a)));
        //查询医院库存
        setHosInventories(req.getOrganId(),req.getDrugIds(),drugListBeans,req.getPharmacyId());
        //查询药企库存----若超过5s还未返回库存, 则不展示对应药企库存字段;
        setDrugsEnterpriseInventoriesByFiveSeconds(req.getOrganId(),drugListBeans);
        //按照机构配置的购药方式进行数据过滤
        return filterInventoriesData(req.getOrganId(), drugListBeans);
    }

    /**
     * 数据过滤
     * @param organId       机构ID
     * @param drugListBeans  药品数据
     * @return
     */
    private List<DrugListBean> filterInventoriesData(Integer organId, List<DrugListBean> drugListBeans){
        LOGGER.info("filterInventoriesData drugListBeans:{}", JSONUtils.toString(drugListBeans));
        Iterator iterator = drugListBeans.iterator();
        boolean inventoryFlag = false;
        while (iterator.hasNext()) {
            DrugListBean drugListBean = (DrugListBean)iterator.next();
            if (drugListBean != null) {
                List<DrugInventoryInfo> drugInventoryInfos = drugListBean.getInventories();
                if (CollectionUtils.isNotEmpty(drugInventoryInfos)) {
                    inventoryFlag = true;
                }
                Iterator drugIterator = drugInventoryInfos.iterator();
                while (drugIterator.hasNext()) {
                    DrugInventoryInfo drugInventoryInfo = (DrugInventoryInfo)drugIterator.next();
                    LOGGER.info("filterInventoriesData drugInventoryInfo:{}", JSONUtils.toString(drugInventoryInfo));
                    List<DrugPharmacyInventoryInfo> drugPharmacyInventoryInfos = drugInventoryInfo.getPharmacyInventories();
                    LOGGER.info("filterInventoriesData drugPharmacyInventoryInfos:{}", JSONUtils.toString(drugPharmacyInventoryInfos));
                    if (CollectionUtils.isEmpty(drugPharmacyInventoryInfos)) {
                        continue;
                    }
                    Iterator drugPharmacyIterator = drugPharmacyInventoryInfos.iterator();
                    while (drugPharmacyIterator.hasNext()) {
                        DrugPharmacyInventoryInfo drugPharmacyInventoryInfo = (DrugPharmacyInventoryInfo)drugPharmacyIterator.next();
                        LOGGER.info("filterInventoriesData drugPharmacyInventoryInfo:{}", JSONUtils.toString(drugPharmacyInventoryInfo));
                        if (getOrganGiveMode(organId, SUPPORT_TO_HOS) && 3 == drugPharmacyInventoryInfo.getType()) {
                            //说明运营平台没有配置到院取药
                            drugPharmacyIterator.remove();
                        }
                        if (getOrganGiveMode(organId, SUPPORT_SEND_TO_ENTERPRISES) && 2 == drugPharmacyInventoryInfo.getType()) {
                            //说明运营平台没有配置药企配送
                            drugPharmacyIterator.remove();
                        }
                        if (getOrganGiveMode(organId, SUPPORT_SEND_TO_HOS) && 1 == drugPharmacyInventoryInfo.getType()) {
                            //说明运营平台没有配置医院配送
                            drugPharmacyIterator.remove();
                        }
                        if (getOrganGiveMode(organId, SUPPORT_TFDS) && 4 == drugPharmacyInventoryInfo.getType()) {
                            //说明运营平台没有配置药店取药
                            drugPharmacyIterator.remove();
                        }
                        if (5 == drugPharmacyInventoryInfo.getType() && getOrganGiveMode(organId, SUPPORT_TFDS) && getOrganGiveMode(organId, SUPPORT_SEND_TO_ENTERPRISES)) {
                            //说明运营平台没有配置药店取药和配送到家
                            drugPharmacyIterator.remove();
                        }
                        if (6 == drugPharmacyInventoryInfo.getType() && getOrganGiveMode(organId, SUPPORT_TFDS) && getOrganGiveMode(organId, SUPPORT_SEND_TO_ENTERPRISES) && getOrganGiveMode(organId, SUPPORT_SEND_TO_HOS)) {
                            //说明运营平台没有配置药店取药和配送到家和医院配送
                            drugPharmacyIterator.remove();
                        }
                    }
                }
            }
            if (inventoryFlag && drugListBean != null) {
                drugListBean.setInventoriesFlag(inventoryFlag);
            }
        }
        return drugListBeans;
    }

    private boolean getOrganGiveMode(Integer organId, String giveModeText){
        //获取机构支持的购药方式
        IGiveModeBase giveModeBase = GiveModeFactory.getGiveModeBaseByRecipe(new Recipe());
        GiveModeShowButtonVO giveModeShowButtonVO = giveModeBase.getGiveModeSettingFromYypt(organId);
        Map configurations = giveModeShowButtonVO.getGiveModeButtons().stream().collect(Collectors.toMap(GiveModeButtonBean::getShowButtonKey, GiveModeButtonBean::getShowButtonName));
        return !configurations.containsKey(giveModeText);
    }

    private void setHosInventories(Integer organId, List<Integer> drugIds, List<DrugListBean> drugListBeans,@Nullable Integer pharmacyId) {
        try {
            if (CollectionUtils.isEmpty(drugListBeans)) {
                return;
            }
            OrganDrugListDAO drugDao = DAOFactory.getDAO(OrganDrugListDAO.class);
            List<OrganDrugList> organDrugLists = drugDao.findByOrganIdAndDrugIds(organId, drugIds);
            if (CollectionUtils.isEmpty(organDrugLists)){
                return;
            }
            setHosInventoriesRealTime(organId,organDrugLists,drugListBeans,pharmacyId);
        } catch (Exception e) {
            LOGGER.error("查询医院药品实时查询库存错误setHosInventories ", e);
        }
    }

    private void setHosInventoriesRealTime(Integer organId, List<OrganDrugList> organDrugLists, List<DrugListBean> drugListBeans,
                                           Integer pharmacyId) {
        //获取drugId和OrganDrugCode的关联关系
        Map<Integer, String> drugIdAndOrganDrugCode = organDrugLists.stream().collect(Collectors.toMap(OrganDrugList::getDrugId,
                OrganDrugList::getOrganDrugCode));
        // 调用his前置接口查询医院库存并赋值
        DrugInfoResponseTO hisResp = this.getHisDrugStock(organId, organDrugLists, pharmacyId);
        List<DrugInventoryInfo> drugInventoryInfos;
        if (hisResp == null) {
            // 说明查询错误 设置status为1为查询失败-前端不显示
            for (DrugListBean drugListBean : drugListBeans) {
                drugInventoryInfos = new ArrayList<>(1);
                drugInventoryInfos.add(new DrugInventoryInfo("his", null, "1"));
                drugListBean.setInventories(drugInventoryInfos);
            }
        } else {
            //显示医院库存
            String amount;
            DrugInventoryInfo drugInventory;
            //前置机成功码可能返回0或者200
            if (Integer.valueOf(0).equals(hisResp.getMsgCode()) || Integer.valueOf(200).equals(hisResp.getMsgCode())){
                //当没返回库存数量的时候
                if (CollectionUtils.isEmpty(hisResp.getData())){
                    amount = "有库存";
                }else {
                    //当返回库存数量的时候
                    //循环查询的药品
                    for (IDrugInventory drugListBean : drugListBeans) {
                        //只保存organDrugCode一样的药品
                        List<DrugInfoTO> drugInfoTOListMatched = hisResp.getData().stream().filter(item ->
                                drugIdAndOrganDrugCode.get(drugListBean.getDrugId()).equalsIgnoreCase(item.getDrcode()))
                                .collect(Collectors.toList());
                        drugInventoryInfos = Lists.newArrayList();
                        //一个药品下his有可能返回多个药房
                        drugInventory = DrugInventoryInfo.builder()
                                .type("his")
                                .pharmacyInventories(convertFrom(drugInfoTOListMatched))
                                .remoteQueryStatus("0")
                                .build();
                        drugInventoryInfos.add(drugInventory);
                        drugListBean.setInventories(drugInventoryInfos);
                    }
                    return;
                }
            }else {
                amount = "无库存";
                //特殊情况返回-1时会返回部分药品没库存
                OrganDrugListDAO organDrugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);
                if (StringUtils.isNotEmpty(hisResp.getMsg())) {
                    //如果返回多个没库存的药品会有,分隔
                    List<String> noStock = Splitter.on(",").splitToList(hisResp.getMsg());
                    List<String> noStockFiltDrugs = noStock.stream().filter((a -> organDrugListDAO.getCountByOrganDrugCode(a) > 0)).collect(Collectors.toList());
                    for (IDrugInventory drugListBean : drugListBeans) {
                        //如果机构药品中查不到就不返回医院库存
                        if (StringUtils.isEmpty(drugIdAndOrganDrugCode.get(drugListBean.getDrugId()))) {
                            continue;
                        }
                        //只要没库存的药品列表没包含就说明有库存
                        if (!noStockFiltDrugs.contains(drugIdAndOrganDrugCode.get(drugListBean.getDrugId()))) {
                            amount = "有库存";
                        }
                        drugInventoryInfos = new ArrayList<>(1);
                        drugInventory = DrugInventoryInfo.builder().type("his").pharmacyInventories(Lists.newArrayList(new DrugPharmacyInventoryInfo(amount, 3))).remoteQueryStatus("0").build();
                        drugInventoryInfos.add(drugInventory);
                        drugListBean.setInventories(drugInventoryInfos);
                    }
                    return;
                }

            }
            //只处理有无库存情况 不显示数量
            //循环查询的药品
            for (IDrugInventory drugListBean : drugListBeans) {
                //如果机构药品中查不到就不返回医院库存
                if(StringUtils.isEmpty(drugIdAndOrganDrugCode.get(drugListBean.getDrugId()))){
                    continue;
                }
                drugInventoryInfos = new ArrayList<>(1);
                drugInventory = DrugInventoryInfo.builder()
                        .type("his")
                        .pharmacyInventories(Lists.newArrayList(new DrugPharmacyInventoryInfo(amount, 3)))
                        .remoteQueryStatus("0")
                        .build();
                drugInventoryInfos.add(drugInventory);
                drugListBean.setInventories(drugInventoryInfos);
            }
        }
    }

    private void setDrugsEnterpriseInventoriesByFiveSeconds(Integer organId, List<DrugListBean> drugListBeans) {
        Future<? extends List<? extends IDrugInventory>> fiveSecondsTask =
                GlobalEventExecFactory.instance().getExecutor().submit(() -> setDrugsEnterpriseInventories(organId, drugListBeans));
        try {
            fiveSecondsTask.get(5000, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            LOGGER.error("查询药企药品库存错误setDrugsEnterpriseInventoriesByFiveSeconds ", e);
            //单个药库存对象
            DrugInventoryInfo drugInventory;
            //库存对象集合
            List<DrugInventoryInfo> drugInventoryInfos = null;
            for (IDrugInventory drugListBean : drugListBeans) {
                //覆盖原有的，设置成查询失败
                drugInventoryInfos = Lists.newArrayList();
                drugInventory = DrugInventoryInfo.builder()
                        .type("drugEnterprise")
                        .pharmacyInventories(null)
                        .remoteQueryStatus("1")
                        .build();
                drugInventoryInfos.add(drugInventory);
                drugListBean.setInventories(drugInventoryInfos);
            }
        }
    }

    private List<? extends IDrugInventory> setDrugsEnterpriseInventories(Integer organId, List<? extends IDrugInventory> drugListBeans) {
        try {
            OrganAndDrugsepRelationDAO relationDAO = DAOFactory.getDAO(OrganAndDrugsepRelationDAO.class);
            SaleDrugListDAO saleDrugListDAO = DAOFactory.getDAO(SaleDrugListDAO.class);
            DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
            RemoteDrugEnterpriseService enterpriseService = ApplicationUtils.getRecipeService(RemoteDrugEnterpriseService.class);
            //1.先判断机构下面存不存在药企，如果存在获取机构下关联的药企
            List<DrugsEnterprise> enterprises = relationDAO.findDrugsEnterpriseByOrganIdAndStatus(organId, 1);
            List<Integer> deps = enterprises.stream().map(DrugsEnterprise::getId).collect(Collectors.toList());
            if (CollectionUtils.isEmpty(deps)) {
                return Lists.newArrayList();
            }
            //2.找到每一个药能支持的药企关系
            List<Integer> drugIds = drugListBeans.stream().map(IHisDrugInventoryCondition::getDrugId).collect(Collectors.toList());
            // 2.1药品1:药企A,药企B
            Map<Integer, List<String>> drugDepIdRel = saleDrugListDAO.findDrugDepRelation(drugIds, deps);
            LOGGER.info("setDrugsEnterpriseInventories drugDepIdRel:{}", JSONUtils.toString(drugDepIdRel));
            // 2.2将药企id转换成药企对象
            Map<Integer, List<DrugsEnterprise>> drugDepRel = Maps.newHashMap();
            drugDepIdRel.forEach((a,b)-> drugDepRel.put(a,drugsEnterpriseDAO.findByIdIn(b.stream().map(Integer::valueOf).collect(Collectors.toList()))));
            //3.再判断该药品是否存在药企的配送目录里
            //单个药库存对象
            DrugInventoryInfo drugInventory;
            //库存对象集合
            List<DrugInventoryInfo> drugInventoryInfos = null;
            List<DrugPharmacyInventoryInfo> pharmacyInventories;
            for (IDrugInventory drugListBean : drugListBeans) {
                if (drugListBean instanceof DrugListBean){
                    drugInventoryInfos = ((DrugListBean) drugListBean).getInventories();
                }
                if (drugInventoryInfos == null) {
                    drugInventoryInfos = Lists.newArrayList();
                }
                // 3.1 获取药品关联的药企列表
                List<DrugsEnterprise> drugsEnterprises = drugDepRel.get(drugListBean.getDrugId());
                if (CollectionUtils.isNotEmpty(drugsEnterprises)){
                    //4.通过查询库存接口查询药企药品库存
                    String inventory;
                    DrugPharmacyInventoryInfo pharmacyInventory;
                    pharmacyInventories = new ArrayList<>(drugsEnterprises.size());
                    for (DrugsEnterprise drugsEnterprise : drugsEnterprises) {
                        //如果配置了不需要校验库存默认显示有
                        if (new Integer(0).equals(drugsEnterprise.getCheckInventoryFlag())){
                            inventory = "有";
                        } else if (new Integer(3).equals(drugsEnterprise.getCheckInventoryFlag()) && CollectionUtils.isNotEmpty(drugInventoryInfos)) {
                            inventory = drugInventoryInfos.get(0).getPharmacyInventories().get(0).getAmount();
                        }else {
                            inventory = enterpriseService.getDrugInventory(drugsEnterprise.getId(), drugListBean.getDrugId(), organId);
                        }
                        //过滤掉暂不支持库存查询的药企
                        if (inventory.length()>5){
                            continue;
                        }
                        pharmacyInventory = new DrugPharmacyInventoryInfo();
                        pharmacyInventory.setPharmacyCode(String.valueOf(drugsEnterprise.getId()));
                        pharmacyInventory.setPharmacyName(drugsEnterprise.getName());
                        //库存数量or有无库存
                        pharmacyInventory.setAmount(inventory);
                        //设置药企类型
                        setPharmacyInventoryType(drugsEnterprise, pharmacyInventory);
                        pharmacyInventories.add(pharmacyInventory);
                    }
                    drugInventory = DrugInventoryInfo.builder()
                            .type("drugEnterprise")
                            .pharmacyInventories(pharmacyInventories)
                            .remoteQueryStatus("0")
                            .build();
                    drugInventoryInfos.add(drugInventory);
                }
                drugListBean.setInventories(drugInventoryInfos);
            }
        } catch (Exception e) {
            LOGGER.error("查询药企药品实时查询库存错误setDrugsEnterpriseInventories ", e);
        }
        return drugListBeans;
    }

    private void setPharmacyInventoryType(DrugsEnterprise drugsEnterprise, DrugPharmacyInventoryInfo pharmacyInventory){
        if (new Integer(1).equals(drugsEnterprise.getSendType())) {
            pharmacyInventory.setType(1);
        } else {
            //需要根据药企支持的配送类型设置药企配送或者药店取药或者两个都支持
            if (RecipeBussConstant.DEP_SUPPORT_ONLINE.equals(drugsEnterprise.getPayModeSupport()) || RecipeBussConstant.DEP_SUPPORT_COD.equals(drugsEnterprise.getPayModeSupport())) {
                pharmacyInventory.setType(2);
            } else if (RecipeBussConstant.DEP_SUPPORT_TFDS.equals(drugsEnterprise.getPayModeSupport())) {
                pharmacyInventory.setType(4);
            } else if (RecipeBussConstant.DEP_SUPPORT_ONLINE_TFDS.equals(drugsEnterprise.getPayModeSupport()) || RecipeBussConstant.DEP_SUPPORT_COD_TFDS.equals(drugsEnterprise.getPayModeSupport())) {
                pharmacyInventory.setType(5);
            } else if (RecipeBussConstant.DEP_SUPPORT_ALL.equals(drugsEnterprise.getPayModeSupport())) {
                pharmacyInventory.setType(6);
            }
        }
    }
}
