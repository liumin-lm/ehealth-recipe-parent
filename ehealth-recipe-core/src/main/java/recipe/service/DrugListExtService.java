package recipe.service;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.ngari.base.property.service.IConfigurationCenterUtilsService;
import com.ngari.base.searchcontent.model.SearchContentBean;
import com.ngari.base.searchcontent.service.ISearchContentService;
import com.ngari.base.searchservice.model.DrugSearchTO;
import com.ngari.his.recipe.mode.DrugInfoRequestTO;
import com.ngari.his.recipe.mode.DrugInfoResponseTO;
import com.ngari.his.recipe.mode.DrugInfoTO;
import com.ngari.his.recipe.service.IRecipeHisService;
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
import es.api.DrugSearchService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.util.Args;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.ApplicationUtils;
import recipe.dao.*;
import recipe.serviceprovider.BaseService;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    private void setInventoriesIfRealTime(Integer organId, List<? extends IDrugInventory> drugListBeans,
                                          @Nullable Integer pharmacyId) {
        if (CollectionUtils.isEmpty(drugListBeans) || !isViewInventoryRealtime(organId)) {
            return;
        }
        // 如果实时查询库存
        // 1. 调用his前置接口查询医院库存并赋值
        List<OrganDrugList> organDrugList = drugListBeans.stream().map(item -> {
            OrganDrugList organDrug = new OrganDrugList();
            organDrug.setDrugId(item.getDrugId());
            organDrug.setOrganDrugCode(item.getOrganDrugCode());
            return organDrug;
        }).collect(Collectors.toList());

        DrugInfoResponseTO hisResp = this.getHisDrugStock(organId, drugListBeans, pharmacyId);
        if (hisResp == null || CollectionUtils.isEmpty(hisResp.getData())
                || hisResp.getMsgCode() != null && !hisResp.getMsgCode().equals(0)) {

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

        // 2. 调用药企接口查询药企库存并赋值（暂不实现）
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
            pharmacyInventory.setAmount(drugInfoTO.getStockAmount());
            pharmacyInventories.add(pharmacyInventory);
        }
        return pharmacyInventories;
    }

    private List<DrugInfoTO> findDrugInfoTOList(IDrugInventory drugListBean, List<DrugInfoTO> drugInfoTOList) {
        return drugInfoTOList.stream().filter(item ->
                item.getDrcode().equalsIgnoreCase(drugListBean.getOrganDrugCode()))
                .collect(Collectors.toList());
    }

    private boolean isViewInventoryRealtime(Integer organId) {
        IConfigurationCenterUtilsService configService = ApplicationUtils.getBaseService(IConfigurationCenterUtilsService.class);
        Integer cfgValue = (Integer) configService.getConfiguration(organId, "viewDrugInventoryRealTime");
        return  cfgValue.equals(1);
    }

    /***
     *
     * @param organId 机构id
     * @param organDrugList 只需OrganDrugList对象的OrganDrugCode，drugId
     * @param pharmacyId 药房id，可为null
     * @return DrugInfoResponseTO.code = 0
     */
    public DrugInfoResponseTO getHisDrugStock(Integer organId,
                                              List<? extends IHisDrugInventoryCondition> organDrugList,
                                              @Nullable Integer pharmacyId) {
        OrganDrugListDAO drugDao = DAOFactory.getDAO(OrganDrugListDAO.class);
        IRecipeHisService hisService = AppDomainContext.getBean("his.iRecipeHisService", IRecipeHisService.class);

        // 从同事那儿得知有的医院需要ProduceCode
        List<Integer> drugIds = organDrugList.stream().map(IHisDrugInventoryCondition::getDrugId)
                .collect(Collectors.toList());
        List<OrganDrugList> organDrugListWithProducer = drugDao.findByOrganIdAndDrugIds(organId, drugIds);
        Map<String, List<OrganDrugList>> drugIdProduceMap = organDrugListWithProducer.stream()
                .collect(Collectors.groupingBy(OrganDrugList::getOrganDrugCode));

        // 构建请求体
        DrugInfoRequestTO request = new DrugInfoRequestTO();
        // 1-查询并校验库存是否充足；2-查询库存；传空默认1
        request.setType("2");
        request.setOrganId(organId);
        List<DrugInfoTO> data = new ArrayList<>(organDrugList.size());
        organDrugList.forEach(organDrugItem -> {
            DrugInfoTO drugInfo = new DrugInfoTO(organDrugItem.getOrganDrugCode());
            List<OrganDrugList> organDrugs = drugIdProduceMap.get(organDrugItem.getOrganDrugCode());
            if (CollectionUtils.isNotEmpty(organDrugs)) {
                Map<Integer, String> producerCodeMap = organDrugs.stream().collect(Collectors.toMap(OrganDrugList::getDrugId, OrganDrugList::getProducerCode));
                String producerCode = producerCodeMap.get(organDrugItem.getDrugId());
                if (StringUtils.isNotEmpty(producerCode)) {
                    drugInfo.setManfcode(producerCode);
                }
            }

            // 药房
            if (pharmacyId != null){
                PharmacyTcmDAO pharmacyTcmDAO = DAOFactory.getDAO(PharmacyTcmDAO.class);
                PharmacyTcm pharmacyTcm = pharmacyTcmDAO.get(pharmacyId);
                if (pharmacyTcm != null){
                    drugInfo.setPharmacyCode(pharmacyTcm.getPharmacyCode());
                    drugInfo.setPharmacy(pharmacyTcm.getPharmacyName());
                }
            }
            data.add(drugInfo);
        });

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
                searchTO.setLimit(limit - drugInfo.size());
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
}
