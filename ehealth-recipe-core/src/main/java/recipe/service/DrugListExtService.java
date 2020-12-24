package recipe.service;

import com.alibaba.fastjson.JSONObject;
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
import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.util.Args;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import recipe.ApplicationUtils;
import recipe.bean.HisSearchDrugDTO;
import recipe.dao.*;
import recipe.drugsenterprise.RemoteDrugEnterpriseService;
import recipe.serviceprovider.BaseService;

import javax.annotation.Nullable;
import javax.validation.constraints.Null;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static recipe.bussutil.RecipeUtil.getHospitalPrice;

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
        //  test
        hisDrug = new QueryDrugResTO();
        List<DrugDetailTO> hisList = new ArrayList<>();
        DrugDetailTO drugDetailTO = new DrugDetailTO();
        drugDetailTO.setOrganDrugCode("042000601");
        drugDetailTO.setReimburse("商30%");
        drugDetailTO.setIsClaim(1);
        hisList.add(drugDetailTO);
        DrugDetailResTO data= new DrugDetailResTO();
        data.setDetails(hisList);
        hisDrug.setData(data);
        //  test
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
            getHospitalPrice(drugDTO.getOrganId(), drugList);
            DrugListDAO drugListDAO = DAOFactory.getDAO(DrugListDAO.class);
            DrugsEnterpriseService drugsEnterpriseService = ApplicationUtils.getRecipeService(DrugsEnterpriseService.class);
            for (DrugListBean drugListBean : drugList) {
                DrugList drugList1 = drugListDAO.getById(drugListBean.getDrugId());
                if (drugList != null) {
                    drugListBean.setPrice1(drugList1.getPrice1());
                    drugListBean.setPrice2(drugList1.getPrice2());
                }
                boolean drugInventoryFlag = drugsEnterpriseService.isExistDrugsEnterprise(drugListBean.getDrugId(), drugListBean.getDrugId());
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

        //  test
        QueryDrugResTO his = new QueryDrugResTO();
        DrugDetailResTO detailResTO = new DrugDetailResTO();
        List<DrugDetailTO> details = new ArrayList<>();
        DrugDetailTO drugDetailTO = new DrugDetailTO();
        drugDetailTO.setOrganDrugCode("214500903");
        drugDetailTO.setIsClaim(1);
        drugDetailTO.setReimburse("商30%");
        details.add(drugDetailTO);
        detailResTO.setDetails(details);
        his.setData(detailResTO);
        searchDrugDTO.setHisDrug(his);
        //  test

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
            for (DrugDetailTO drug : hisDrugList){
                OrganDrugList organDrug = organDrugListDAO.getByOrganIdAndOrganDrugCode(searchDrug.getOrganId(),drug.getOrganDrugCode());
                if (null != organDrug && null != detailMap.get(drug.getOrganDrugCode())){
                    SearchDrugDetailDTO drugListBean = detailMap.get(drug.getOrganDrugCode());
                    drugListBean.setHisciIsClaim(drug.getIsClaim());
                    drugListBean.setHisciReimburseRate(drug.getReimburse());
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
        Boolean search = false;
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
                }else {
                    search = false;
                }
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

        // 如果配置成实时查看库存，则查询医院库存、药企库存等
        setInventoriesIfRealTime(commonDrugListDTO.getOrganId(), drugListBeans,
                commonDrugListDTO.getPharmacyId());
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

                // 说明查询错误, 或者
                List<DrugInventoryInfo> drugInventoryInfos = new ArrayList<>();
                drugInventoryInfos.add(new DrugInventoryInfo("his", null, "1"));
                for (IDrugInventory drugListBean : drugListBeans) {
                    drugListBean.setInventories(drugInventoryInfos);
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
            return null;
        }
        List<DrugPharmacyInventoryInfo> pharmacyInventories = new ArrayList<>(drugInfoTOList.size());
        for (DrugInfoTO drugInfoTO : drugInfoTOList) {
            DrugPharmacyInventoryInfo pharmacyInventory = new DrugPharmacyInventoryInfo();
            pharmacyInventory.setPharmacyCode(drugInfoTO.getPharmacyCode());
            pharmacyInventory.setPharmacyName(drugInfoTO.getPharmacy());
            Double amount = drugInfoTO.getStockAmount() == null ? 0 : drugInfoTO.getStockAmount();
            // 改成字符串返回
            pharmacyInventory.setAmount(BigDecimal.valueOf(amount).toPlainString());
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

        // 如果配置成实时查看库存，则查询医院库存、药企库存等
        setInventoriesIfRealTime(req.getOrganId(), resultList, req.getPharmacyId());
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
            //OrganDrugListDAO organDrugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);
            DrugList drugListNow;
            boolean drugInventoryFlag;
            List<UseDoseAndUnitRelationBean> useDoseAndUnitRelationList;
            for (String s : drugInfo) {
               /* try {
                    drugList = JSONUtils.parse(s, SearchDrugDetailDTO.class);
                    //考虑到在es做过滤有可能会导致老版本搜索出多个重复药品
                    //(如果X药品有AB两个药房，要同步两次到es，如果不根据药房id搜索就会出现两个重复药品),so过滤药房暂时先放这
                    if (organId != null && pharmacyId != null){
                        canAddDrug = false;
                        OrganDrugList organDrugList = organDrugListDAO.getByOrganIdAndOrganDrugCodeAndDrugId(organId, drugList.getOrganDrugCode(), drugList.getDrugId());
                        if (organDrugList !=null && StringUtils.isNotEmpty(organDrugList.getPharmacy())){
                            //过滤掉不在此药房内的药
                            List<String> pharmacyIds = Splitter.on(",").splitToList(organDrugList.getPharmacy());
                            if (pharmacyIds.contains(String.valueOf(pharmacyId))){
                                canAddDrug = true;
                            }
                        }
                    }
                } catch (Exception e) {
                    LOGGER.error("searchDrugListWithES parse error.  String=" + s,e);
                }*/
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
        Assert.notNull(req.getDrugIds(),"drugIds is required");
        DrugListDAO drugListDAO = DAOFactory.getDAO(DrugListDAO.class);
        List<DrugList> drugLists = drugListDAO.findByDrugIds(req.getDrugIds());
        List<DrugListBean> drugListBeans = getList(drugLists, DrugListBean.class);
        //查询医院库存
        setHosInventories(req.getOrganId(),req.getDrugIds(),drugListBeans,req.getPharmacyId());
        //查询药企库存
        setDrugsEnterpriseInventories(req.getOrganId(),drugListBeans);
        return drugListBeans;
    }

    private void setHosInventories(Integer organId, List<Integer> drugIds, List<DrugListBean> drugListBeans, Integer pharmacyId) {
        try {
            if (CollectionUtils.isEmpty(drugListBeans)) {
                return;
            }
            OrganDrugListDAO drugDao = DAOFactory.getDAO(OrganDrugListDAO.class);
            List<OrganDrugList> organDrugLists = drugDao.findByOrganIdAndDrugIds(organId, drugIds);
            if (CollectionUtils.isEmpty(organDrugLists)){
                return;
            }
            Map<Integer, String> drugIdAndOrganDrugCode = organDrugLists.stream().collect(Collectors.toMap(OrganDrugList::getDrugId,
                    OrganDrugList::getOrganDrugCode));
            // 调用his前置接口查询医院库存并赋值
            DrugInfoResponseTO hisResp = this.getHisDrugStock(organId, organDrugLists, pharmacyId);
            if (hisResp == null || CollectionUtils.isEmpty(hisResp.getData())) {
                // 说明查询错误
                List<DrugInventoryInfo> drugInventoryInfos = new ArrayList<>(1);
                drugInventoryInfos.add(new DrugInventoryInfo("his", null, "1"));
                drugListBeans.forEach(drugListBean -> drugListBean.setInventories(drugInventoryInfos));
            } else {
                DrugInventoryInfo drugInventory;
                List<DrugInventoryInfo> drugInventoryInfos;
                //循环查询的药品
                for (IDrugInventory drugListBean : drugListBeans) {
                    List<DrugInfoTO> drugInfoTOListMatched = hisResp.getData().stream().filter(item ->
                            drugIdAndOrganDrugCode.get(drugListBean.getDrugId()).equalsIgnoreCase(item.getDrcode()))
                            .collect(Collectors.toList());
                    drugInventoryInfos = Lists.newArrayList();
                    drugInventory = DrugInventoryInfo.builder()
                            .type("his")
                            .pharmacyInventories(convertFrom(drugInfoTOListMatched))
                            .remoteQueryStatus("0")
                            .build();
                    drugInventoryInfos.add(drugInventory);
                    drugListBean.setInventories(drugInventoryInfos);
                }
            }
        } catch (Exception e) {
            LOGGER.error("查询医院药品实时查询库存错误setHosInventories ", e);
        }
    }

    private void setDrugsEnterpriseInventories(Integer organId, List<? extends IDrugInventory> drugListBeans) {
        try {
            OrganAndDrugsepRelationDAO relationDAO = DAOFactory.getDAO(OrganAndDrugsepRelationDAO.class);
            SaleDrugListDAO saleDrugListDAO = DAOFactory.getDAO(SaleDrugListDAO.class);
            DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
            RemoteDrugEnterpriseService enterpriseService = ApplicationUtils.getRecipeService(RemoteDrugEnterpriseService.class);
            //1.先判断机构下面存不存在药企，如果存在获取机构下关联的药企
            List<DrugsEnterprise> enterprises = relationDAO.findDrugsEnterpriseByOrganIdAndStatus(organId, 1);
            List<Integer> deps = enterprises.stream().map(DrugsEnterprise::getId).collect(Collectors.toList());
            //2.找到每一个药能支持的药企关系
            List<Integer> drugIds = drugListBeans.stream().map(IHisDrugInventoryCondition::getDrugId).collect(Collectors.toList());
            // 2.1药品1:药企A,药企B
            Map<Integer, List<String>> drugDepIdRel = saleDrugListDAO.findDrugDepRelation(drugIds, deps);
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
                if (drugInventoryInfos == null){
                    drugInventoryInfos = Lists.newArrayList();
                }
                // 3.1 获取药品关联的药企列表
                if (CollectionUtils.isNotEmpty(drugDepRel.get(drugListBean.getDrugId()))){
                    //4.通过查询库存接口查询药企药品库存
                    String inventory;
                    pharmacyInventories = Lists.newArrayList();
                    DrugPharmacyInventoryInfo pharmacyInventory;
                    List<DrugsEnterprise> drugsEnterprises = drugDepRel.get(drugListBean.getDrugId());
                    for (DrugsEnterprise drugsEnterprise : drugsEnterprises) {
                        inventory = enterpriseService.getDrugInventory(drugsEnterprise.getId(), drugListBean.getDrugId(), organId);
                        pharmacyInventory = new DrugPharmacyInventoryInfo();
                        pharmacyInventory.setPharmacyCode(String.valueOf(drugsEnterprise.getId()));
                        pharmacyInventory.setPharmacyName(drugsEnterprise.getName());
                        //库存数量or有无库存
                        pharmacyInventory.setAmount(inventory);
                        pharmacyInventories.add(pharmacyInventory);
                    }
                    drugInventory = DrugInventoryInfo.builder()
                            .type("drugEnterprise")
                            .pharmacyInventories(pharmacyInventories)
                            .remoteQueryStatus("0")
                            .build();
                    drugInventoryInfos.add(drugInventory);
                    drugListBean.setInventories(drugInventoryInfos);
                }
            }
        } catch (Exception e) {
            LOGGER.error("查询药企药品实时查询库存错误setDrugsEnterpriseInventories ", e);
        }

    }
}
