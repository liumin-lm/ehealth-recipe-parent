package recipe.manager;

import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.recipe.mode.ExtInfoTO;
import com.ngari.his.recipe.mode.MedicalInfo;
import com.ngari.his.recipe.mode.QueryHisRecipResTO;
import com.ngari.his.recipe.mode.RecipeDetailTO;
import com.ngari.patient.dto.OrganDTO;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.patient.service.BasicAPI;
import com.ngari.patient.service.OrganService;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.entity.*;
import ctd.persistence.DAOFactory;
import ctd.persistence.exception.DAOException;
import ctd.util.JSONUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import recipe.client.OfflineRecipeClient;
import recipe.client.PatientClient;
import recipe.dao.*;
import recipe.enumerate.status.OfflineToOnlineEnum;
import recipe.enumerate.status.RecipeStatusEnum;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @Author liumin
 * @Date 2021/7/20 下午5:30
 * @Description
 */
@Service
public class HisRecipeManager extends BaseManager {

    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    @Autowired
    PatientClient patientClient;

    @Autowired
    OfflineRecipeClient offlineRecipeClient;

    @Autowired
    HisRecipeDAO hisRecipeDao;


    @Autowired
    private HisRecipeExtDAO hisRecipeExtDAO;
    @Autowired
    private HisRecipeDetailDAO hisRecipeDetailDAO;
    @Autowired
    private RecipeDAO recipeDAO;
    @Autowired
    private RecipeOrderDAO recipeOrderDAO;
    @Autowired
    private RecipeExtendDAO recipeExtendDAO;
    @Autowired
    private RecipeDetailDAO recipeDetailDAO;
    @Autowired
    private RecipeLogDAO recipeLogDao;


    /**
     * 获取患者信息
     *
     * @param mpiId
     * @return
     */
    public PatientDTO getPatientBeanByMpiId(String mpiId) {
        return patientClient.getPatientBeanByMpiId(mpiId);
    }

    /**
     * 查询线下处方数据
     *
     * @param organId
     * @param patientDTO
     * @param timeQuantum
     * @param flag
     * @param recipeCode
     * @return
     */
    public HisResponseTO<List<QueryHisRecipResTO>> queryData(Integer organId, PatientDTO patientDTO, Integer timeQuantum, Integer flag, String recipeCode) {
        LOGGER.info("HisRecipeManager queryData param organId:{},patientDTO:{},timeQuantum:{},flag:{},recipeCode:{}", organId, JSONUtils.toString(patientDTO), timeQuantum, flag, recipeCode);
        HisResponseTO<List<QueryHisRecipResTO>> res = offlineRecipeClient.queryData(organId, patientDTO, timeQuantum, flag, recipeCode);
        logger.info("HisRecipeManager res:{}.", JSONUtils.toString(res));
        return res;
    }


    /**
     * 获取线下处方
     *
     * @param mpiId
     * @param clinicOrgan
     * @param recipeCode
     * @return
     */
    public HisRecipe getHisRecipeBMpiIdyRecipeCodeAndClinicOrgan(String mpiId, Integer clinicOrgan, String recipeCode) {
        return hisRecipeDao.getHisRecipeBMpiIdyRecipeCodeAndClinicOrgan(mpiId, clinicOrgan, recipeCode);
    }

    /**
     * 获取未处理的线下处方
     *
     * @param clinicOrgan
     * @param recipeCodeList
     * @return
     */
    public List<HisRecipe> findNoDealHisRecipe(Integer clinicOrgan, List<String> recipeCodeList) {
        LOGGER.info("HisRecipeManager findNoDealHisRecipe param clinicOrgan:{},recipeCodeList:{}", clinicOrgan, JSONUtils.toString(recipeCodeList));
        List<HisRecipe> hisRecipes = hisRecipeDao.findNoDealHisRecipe(clinicOrgan, recipeCodeList);
        LOGGER.info("HisRecipeManager findNoDealHisRecipe res hisRecipes:{}", JSONUtils.toString(hisRecipes));
        return hisRecipes;
    }

    /**
     * 校验his线下处方是否发生变更 如果变更则处理数据
     *
     * @param hisRecipeTO his处方数据
     * @param patientDTO  患者信息
     */
    public void hisRecipeInfoCheck(List<QueryHisRecipResTO> hisRecipeTO, PatientDTO patientDTO) {
        LOGGER.info("hisRecipeInfoCheck param hisRecipeTO = {} , patientDTO={}", JSONUtils.toString(hisRecipeTO), JSONUtils.toString(patientDTO));
        if (CollectionUtils.isEmpty(hisRecipeTO)) {
            return;
        }
        Integer clinicOrgan = hisRecipeTO.get(0).getClinicOrgan();
        if (null == clinicOrgan) {
            LOGGER.info("hisRecipeInfoCheck his data error clinicOrgan is null");
            return;
        }
        List<String> recipeCodeList = hisRecipeTO.stream().map(QueryHisRecipResTO::getRecipeCode).distinct().collect(Collectors.toList());
        if (CollectionUtils.isEmpty(recipeCodeList)) {
            LOGGER.info("hisRecipeInfoCheck his data error recipeCodeList is null");
            return;
        }
        //获取平台处方
        List<Recipe> recipeList = recipeDAO.findByRecipeCodeAndClinicOrgan(recipeCodeList, clinicOrgan);
        LOGGER.info("hisRecipeInfoCheck recipeList = {}", JSONUtils.toString(recipeList));

        //获取未处理的线下处方
        List<HisRecipe> hisRecipeList = hisRecipeDao.findNoDealHisRecipe(clinicOrgan, recipeCodeList);
        LOGGER.info("hisRecipeInfoCheck hisRecipeList = {}", JSONUtils.toString(hisRecipeList));
        if (CollectionUtils.isEmpty(hisRecipeList)) {
            return;
        }
        //获取一个key为未处理recipeCode,值为未处理HisRecipe的map对象
        Map<String, HisRecipe> hisRecipeMap = hisRecipeList.stream().collect(Collectors.toMap(HisRecipe::getRecipeCode, a -> a, (k1, k2) -> k1));
        //获取未处理的线下处方Ids，用来获取线下处方详情
        List<Integer> hisRecipeIds = hisRecipeList.stream().map(HisRecipe::getHisRecipeID).distinct().collect(Collectors.toList());
        //获取未处理的线下处方详情
        List<HisRecipeDetail> hisRecipeDetailList = hisRecipeDetailDAO.findByHisRecipeIds(hisRecipeIds);
        LOGGER.info("hisRecipeInfoCheck hisRecipeDetailList = {}", JSONUtils.toString(hisRecipeDetailList));
        //诊断变更，更新诊断
        updateDisease(hisRecipeTO, recipeList, hisRecipeMap);
        //药品发生变更，删除关联信息
        deleteRecipeData(hisRecipeTO, hisRecipeMap, hisRecipeDetailList, patientDTO.getMpiId());
    }

    /**
     * 药品详情发生变化、数据不是由本人生成的未支付处方 数据处理
     *
     * @param hisRecipeTO         his处方数据
     * @param hisRecipeMap        key为未处理recipeCode,值为未处理HisRecipe的map对象
     * @param hisRecipeDetailList 未处理的线下处方详情
     * @param mpiId               查看详情处方的操作用户的mpiid
     */
    private void deleteRecipeData(List<QueryHisRecipResTO> hisRecipeTO, Map<String, HisRecipe> hisRecipeMap, List<HisRecipeDetail> hisRecipeDetailList, String mpiId) {
        if (CollectionUtils.isEmpty(hisRecipeDetailList)) {
            return;
        }
        Set<String> deleteSetRecipeCode = obtainDeleteRecipeCodes(hisRecipeTO, hisRecipeMap, hisRecipeDetailList, mpiId);
        deleteSetRecipeCode(hisRecipeTO.get(0).getClinicOrgan(), deleteSetRecipeCode);
    }

    /**
     * 更新诊断字段
     *
     * @param hisRecipeTO  his处方数据
     * @param hisRecipeMap key为未处理recipeCode,值为未处理HisRecipe的map对象
     * @param recipeList   平台处方
     */
    private Map<String, HisRecipe> updateDisease(List<QueryHisRecipResTO> hisRecipeTO, List<Recipe> recipeList, Map<String, HisRecipe> hisRecipeMap) {
        LOGGER.info("updateHisRecipe param hisRecipeTO:{},recipeList:{},hisRecipeMap:{}", JSONUtils.toString(hisRecipeTO), JSONUtils.toString(recipeList), JSONUtils.toString(hisRecipeMap));
        Map<String, Recipe> recipeMap = recipeList.stream().collect(Collectors.toMap(Recipe::getRecipeCode, a -> a, (k1, k2) -> k1));
        hisRecipeTO.forEach(a -> {
            HisRecipe hisRecipe = hisRecipeMap.get(a.getRecipeCode());
            if (null == hisRecipe) {
                return;
            }
            String disease = null != a.getDisease() ? a.getDisease() : "";
            String diseaseName = null != a.getDiseaseName() ? a.getDiseaseName() : "";
            if (!disease.equals(hisRecipe.getDisease()) || !diseaseName.equals(hisRecipe.getDiseaseName())) {
                hisRecipe.setDisease(disease);
                hisRecipe.setDiseaseName(diseaseName);
                hisRecipeDao.update(hisRecipe);

                LOGGER.info("updateHisRecipe hisRecipe = {}", JSONUtils.toString(hisRecipe));
                Recipe recipe = recipeMap.get(a.getRecipeCode());
                recipe.setOrganDiseaseId(disease);
                recipe.setOrganDiseaseName(diseaseName);
                recipeDAO.update(recipe);


                RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipe.getRecipeId());
                EmrRecipeManager.getMedicalInfo(recipe, recipeExtend);
                recipeExtendDAO.saveOrUpdateRecipeExtend(recipeExtend);
            }
        });
        LOGGER.info("updateHisRecipe response hisRecipeMap:{}", JSONUtils.toString(hisRecipeMap));
        return hisRecipeMap;
    }

    /**
     * 删除未支付处方，同时判断是否存在已缴费处方 若存在 返回true
     *
     * @param organId     机构id
     * @param recipeCodes 需删除处方号
     * @return
     */
    public void deleteRecipeByRecipeCodes(String organId, List<String> recipeCodes) {
        //默认不存在
        boolean isExistPayRecipe = false;
        List<Recipe> recipes = recipeDAO.findRecipeByRecipeCodeAndClinicOrgan(Integer.parseInt(organId), recipeCodes);
        if (CollectionUtils.isNotEmpty(recipes) && recipes.size() > 0) {
            //存在已支付处方出现在（待处理列表） 提示用户刷新列表
            isExistPayRecipe = true;
        }
        if (isExistPayRecipe) {
            throw new DAOException(609, "处方单已经缴费，请刷新重试");
        }
        //2 删除数据
        deleteSetRecipeCode(Integer.parseInt(organId), new HashSet<>(recipeCodes));
    }

    /**
     * 删除线下处方相关数据
     *
     * @param clinicOrgan         机构id
     * @param deleteSetRecipeCode 要删除的recipeCodes
     */
    public void deleteSetRecipeCode(Integer clinicOrgan, Set<String> deleteSetRecipeCode) {
        LOGGER.info("deleteSetRecipeCode clinicOrgan = {},deleteSetRecipeCode = {}", clinicOrgan, JSONUtils.toString(deleteSetRecipeCode));
        if (CollectionUtils.isEmpty(deleteSetRecipeCode)) {
            return;
        }
        List<String> recipeCodeList = new ArrayList<>(deleteSetRecipeCode);
        List<HisRecipe> hisRecipeList = hisRecipeDao.findHisRecipeByRecipeCodeAndClinicOrgan(clinicOrgan, recipeCodeList);
        List<Integer> hisRecipeIds = hisRecipeList.stream().map(HisRecipe::getHisRecipeID).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(hisRecipeIds)) {
            LOGGER.info("deleteSetRecipeCode 查找无处方");
            return;
        }
        hisRecipeExtDAO.deleteByHisRecipeIds(hisRecipeIds);
        hisRecipeDetailDAO.deleteByHisRecipeIds(hisRecipeIds);
        hisRecipeDao.deleteByHisRecipeIds(hisRecipeIds);
        List<Recipe> recipeList = recipeDAO.findByRecipeCodeAndClinicOrgan(recipeCodeList, clinicOrgan);
        if (CollectionUtils.isEmpty(recipeList)) {
            return;
        }
        List<Integer> recipeIds = recipeList.stream().map(Recipe::getRecipeId).collect(Collectors.toList());
        List<String> orderCodeList = recipeList.stream().filter(a -> StringUtils.isNotEmpty(a.getOrderCode())).map(Recipe::getOrderCode).collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(orderCodeList)) {
            recipeOrderDAO.deleteByRecipeIds(orderCodeList);
        }
        recipeExtendDAO.deleteByRecipeIds(recipeIds);
        recipeDetailDAO.deleteByRecipeIds(recipeIds);
        //recipe表不删 添加的时候修改（除id外所有字段）
        recipeDAO.updateRecipeStatusByRecipeIds(recipeIds);
        //日志记录
        Map<Integer, Recipe> recipeMap = recipeList.stream().collect(Collectors.toMap(Recipe::getRecipeId, Function.identity(), (key1, key2) -> key2));
        recipeIds.forEach(a -> {
            RecipeLog recipeLog = new RecipeLog();
            recipeLog.setRecipeId(a);
            recipeLog.setBeforeStatus(recipeMap.get(a).getStatus());
            recipeLog.setAfterStatus(RecipeStatusEnum.RECIPE_STATUS_DELETE.getType());
            recipeLog.setMemo("线下转线上：修改处方状态为已删除");
            recipeLogDao.saveRecipeLog(recipeLog);

        });
        LOGGER.info("deleteSetRecipeCode is delete end ");
    }

    /**
     * 获取需要删除的recipeCodes
     * 判断药品详情发生变化、数据不是由本人生成的未支付处方、中药tcmFee变更
     *
     * @param hisRecipeTO         his处方数据
     * @param hisRecipeMap        key为未处理recipeCode,值为未处理HisRecipe的map对象
     * @param hisRecipeDetailList 未处理的线下处方详情
     * @param mpiId               查看详情处方的操作用户的mpiid
     * @return
     */
    public Set<String> obtainDeleteRecipeCodes(List<QueryHisRecipResTO> hisRecipeTO, Map<String, HisRecipe> hisRecipeMap, List<HisRecipeDetail> hisRecipeDetailList, String mpiId) {
        Set<String> deleteSetRecipeCode = new HashSet<>();
        Map<Integer, List<HisRecipeDetail>> hisRecipeIdDetailMap = hisRecipeDetailList.stream().collect(Collectors.groupingBy(HisRecipeDetail::getHisRecipeId));
        hisRecipeTO.forEach(a -> {
            String recipeCode = a.getRecipeCode();
            HisRecipe hisRecipe = hisRecipeMap.get(recipeCode);
            if (null == hisRecipe) {
                return;
            } else {
                if (!hisRecipe.getMpiId().equals(mpiId)) {
                    deleteSetRecipeCode.add(recipeCode);
                    LOGGER.info("deleteSetRecipeCode cause mpiid recipeCode:{}", recipeCode);
                    return;
                }
            }
            //场景：没付钱跑到线下去支付了
            //如果已缴费处方在数据库里已存在，且数据里的状态是未缴费，则处理数据
            if (a.getStatus() == 2) {
                if (1 == hisRecipe.getStatus()) {
                    deleteSetRecipeCode.add(recipeCode);
                    LOGGER.info("deleteSetRecipeCode cause Status recipeCode:{}", recipeCode);
                }
            }

            //已处理处方(现在因为其他用户绑定了该就诊人也要查询到数据，所以mpiid不一致，数据需要删除)
            if (2 == hisRecipe.getStatus()) {
                return;
            }
            List<HisRecipeDetail> hisDetailList = hisRecipeIdDetailMap.get(hisRecipe.getHisRecipeID());
            if (CollectionUtils.isEmpty(a.getDrugList()) || CollectionUtils.isEmpty(hisDetailList)) {
                deleteSetRecipeCode.add(recipeCode);
                LOGGER.info("deleteSetRecipeCode cause drugList empty recipeCode:{}", recipeCode);
                return;
            }
            if (a.getDrugList().size() != hisDetailList.size()) {
                deleteSetRecipeCode.add(recipeCode);
                LOGGER.info("deleteSetRecipeCode cause drugList size no equal recipeCode:{}", recipeCode);
                return;
            }
            Map<String, HisRecipeDetail> recipeDetailMap = hisDetailList.stream().collect(Collectors.toMap(HisRecipeDetail::getDrugCode, b -> b, (k1, k2) -> k1));
            for (RecipeDetailTO recipeDetailTO : a.getDrugList()) {
                HisRecipeDetail hisRecipeDetail = recipeDetailMap.get(recipeDetailTO.getDrugCode());
                LOGGER.info("recipeDetailTO:{},hisRecipeDetail:{}.", JSONUtils.toString(recipeDetailTO), JSONUtils.toString(hisRecipeDetail));
                if (null == hisRecipeDetail) {
                    deleteSetRecipeCode.add(recipeCode);
                    LOGGER.info("deleteSetRecipeCode cause hisRecipeDetail is null recipeCode:{}", recipeCode);
                    continue;
                }
                if (0 != covertBigdecimal(hisRecipeDetail.getUseTotalDose()).compareTo(covertBigdecimal(recipeDetailTO.getUseTotalDose()))) {
                    deleteSetRecipeCode.add(recipeCode);
                    LOGGER.info("deleteSetRecipeCode cause useTotalDose recipeCode:{}", recipeCode);
                    continue;
                }
                if (!covertObject(hisRecipeDetail.getUseDose()).equals(covertObject(recipeDetailTO.getUseDose()))) {
                    deleteSetRecipeCode.add(recipeCode);
                    LOGGER.info("deleteSetRecipeCode cause useDose recipeCode:{}", recipeCode);
                    continue;
                }
                if ((!covertObject(hisRecipeDetail.getUseDoseStr()).equals(covertObject(recipeDetailTO.getUseDoseStr())))) {
                    deleteSetRecipeCode.add(recipeCode);
                    LOGGER.info("deleteSetRecipeCode cause useDoseStr recipeCode:{}", recipeCode);
                    continue;
                }
                if ((!covertObject(hisRecipeDetail.getUseDaysB()).equals(covertObject(recipeDetailTO.getUseDaysB())))) {
                    deleteSetRecipeCode.add(recipeCode);
                    LOGGER.info("deleteSetRecipeCode cause useDaysB recipeCode:{}", recipeCode);
                    continue;
                }

                if ((!covertObject(hisRecipeDetail.getUseDays()).equals(covertObject(recipeDetailTO.getUseDays())))) {
                    deleteSetRecipeCode.add(recipeCode);
                    LOGGER.info("deleteSetRecipeCode cause useDays recipeCode:{}", recipeCode);
                    continue;
                }

                if (!covertObject(hisRecipeDetail.getUsingRate()).equals(covertObject(recipeDetailTO.getUsingRate()))) {
                    deleteSetRecipeCode.add(recipeCode);
                    LOGGER.info("deleteSetRecipeCode cause usingRate recipeCode:{}", recipeCode);
                    continue;
                }

                if (!covertObject(hisRecipeDetail.getUsingRateText()).equals(covertObject(recipeDetailTO.getUsingRateText()))) {
                    deleteSetRecipeCode.add(recipeCode);
                    LOGGER.info("deleteSetRecipeCode cause usingRateText recipeCode:{}", recipeCode);
                    continue;
                }
                if (!covertObject(hisRecipeDetail.getUsePathways()).equals(covertObject(recipeDetailTO.getUsePathWays()))) {
                    deleteSetRecipeCode.add(recipeCode);
                    LOGGER.info("deleteSetRecipeCode cause usePathWays recipeCode:{}", recipeCode);
                    continue;
                }
                if (!covertObject(hisRecipeDetail.getUsePathwaysText()).equals(covertObject(recipeDetailTO.getUsePathwaysText()))) {
                    LOGGER.info("deleteSetRecipeCode cause usePathwaysText recipeCode:{}", recipeCode);
                    deleteSetRecipeCode.add(recipeCode);
                }
            }
            //中药判断tcmFee发生变化,删除数据
            BigDecimal tcmFee = a.getTcmFee();
            if (covertBigdecimal(tcmFee).compareTo(covertBigdecimal(hisRecipe.getTcmFee())) != 0) {
                LOGGER.info("deleteSetRecipeCode cause tcmFee recipeCode:{}", recipeCode);
                deleteSetRecipeCode.add(hisRecipe.getRecipeCode());
            }
        });
        return deleteSetRecipeCode;
    }

    /**
     * 数据转换
     *
     * @param obj
     * @return
     */
    private Object covertObject(Object obj) {
        if (obj instanceof BigDecimal) {
            if (obj == null) {
                return BigDecimal.ZERO;
            }
        } else if (obj instanceof String) {
            if (obj == null) {
                return "";
            }
        } else if (obj instanceof Integer) {
            if (obj == null) {
                return 0;
            }
        }
        return obj;
    }


    private BigDecimal covertBigdecimal(BigDecimal obj) {
        if (obj == null) {
            return BigDecimal.ZERO;
        }
        return obj;
    }

    /**
     * 保存线下处方数据到cdr_his_recipe、HisRecipeDetail、HisRecipeExt
     *
     * @param responseTO
     * @param patientDTO
     * @param flag
     * @return
     */
    public List<HisRecipe> saveHisRecipeInfo(HisResponseTO<List<QueryHisRecipResTO>> responseTO, PatientDTO patientDTO, Integer flag) {
        List<HisRecipe> hisRecipes = new ArrayList<>();
        if (responseTO == null) {
            return hisRecipes;
        }
        List<QueryHisRecipResTO> queryHisRecipResTOList = responseTO.getData();

        if (CollectionUtils.isEmpty(queryHisRecipResTOList)) {
            return hisRecipes;
        }
        LOGGER.info("saveHisRecipeInfo queryHisRecipResTOList:" + JSONUtils.toString(queryHisRecipResTOList));
        for (QueryHisRecipResTO queryHisRecipResTO : queryHisRecipResTOList) {
            HisRecipe hisRecipe1 = hisRecipeDao.getHisRecipeByRecipeCodeAndClinicOrgan(queryHisRecipResTO.getClinicOrgan(), queryHisRecipResTO.getRecipeCode());

            //数据库不存在处方信息，则新增
            if (null == hisRecipe1) {
                HisRecipe hisRecipe = new HisRecipe();
                hisRecipe.setCertificate(patientDTO.getCertificate());
                hisRecipe.setCertificateType(patientDTO.getCertificateType());
                hisRecipe.setMpiId(patientDTO.getMpiId());
                hisRecipe.setPatientName(patientDTO.getPatientName());
                hisRecipe.setPatientAddress(patientDTO.getAddress());
                hisRecipe.setPatientNumber(queryHisRecipResTO.getPatientNumber());
                hisRecipe.setPatientTel(patientDTO.getMobile());
                hisRecipe.setRegisteredId(StringUtils.isNotEmpty(queryHisRecipResTO.getRegisteredId()) ? queryHisRecipResTO.getRegisteredId() : "");
                hisRecipe.setChronicDiseaseCode(StringUtils.isNotEmpty(queryHisRecipResTO.getChronicDiseaseCode()) ? queryHisRecipResTO.getChronicDiseaseCode() : "");
                hisRecipe.setChronicDiseaseName(StringUtils.isNotEmpty(queryHisRecipResTO.getChronicDiseaseName()) ? queryHisRecipResTO.getChronicDiseaseName() : "");
                hisRecipe.setRecipeCode(queryHisRecipResTO.getRecipeCode());
                hisRecipe.setDepartCode(queryHisRecipResTO.getDepartCode());
                hisRecipe.setDepartName(queryHisRecipResTO.getDepartName());
                hisRecipe.setDoctorName(queryHisRecipResTO.getDoctorName());
                hisRecipe.setCreateDate(queryHisRecipResTO.getCreateDate());
                if (queryHisRecipResTO.getTcmNum() != null) {
                    hisRecipe.setTcmNum(queryHisRecipResTO.getTcmNum().toString());
                }
                hisRecipe.setStatus(queryHisRecipResTO.getStatus());
                if (new Integer(2).equals(queryHisRecipResTO.getMedicalType())) {
                    hisRecipe.setMedicalType(queryHisRecipResTO.getMedicalType());//医保类型
                } else {
                    //默认自费
                    hisRecipe.setMedicalType(1);
                }
                hisRecipe.setRecipeFee(queryHisRecipResTO.getRecipeFee());
                hisRecipe.setRecipeType(queryHisRecipResTO.getRecipeType());
                hisRecipe.setClinicOrgan(queryHisRecipResTO.getClinicOrgan());
                hisRecipe.setCreateTime(new Date());
                hisRecipe.setExtensionFlag(1);
                if (queryHisRecipResTO.getExtensionFlag() == null) {
                    //设置外延处方的标志
                    hisRecipe.setRecipePayType(0);
                } else {
                    //设置外延处方的标志
                    hisRecipe.setRecipePayType(queryHisRecipResTO.getExtensionFlag());
                }

                if (!StringUtils.isEmpty(queryHisRecipResTO.getDiseaseName())) {
                    hisRecipe.setDiseaseName(queryHisRecipResTO.getDiseaseName());
                } else {
                    hisRecipe.setDiseaseName("无");
                }
                hisRecipe.setDisease(queryHisRecipResTO.getDisease());
                if (!StringUtils.isEmpty(queryHisRecipResTO.getDoctorCode())) {
                    hisRecipe.setDoctorCode(queryHisRecipResTO.getDoctorCode());
                }
                OrganService organService = BasicAPI.getService(OrganService.class);
                OrganDTO organDTO = organService.getByOrganId(queryHisRecipResTO.getClinicOrgan());
                if (null != organDTO) {
                    hisRecipe.setOrganName(organDTO.getName());
                }
                setMedicalInfo(queryHisRecipResTO, hisRecipe);
                hisRecipe.setGiveMode(queryHisRecipResTO.getGiveMode());
                hisRecipe.setDeliveryCode(queryHisRecipResTO.getDeliveryCode());
                hisRecipe.setDeliveryName(queryHisRecipResTO.getDeliveryName());
                hisRecipe.setSendAddr(queryHisRecipResTO.getSendAddr());
                hisRecipe.setRecipeSource(queryHisRecipResTO.getRecipeSource());
                hisRecipe.setReceiverName(queryHisRecipResTO.getReceiverName());
                hisRecipe.setReceiverTel(queryHisRecipResTO.getReceiverTel());

                //中药
                hisRecipe.setRecipeCostNumber(queryHisRecipResTO.getRecipeCostNumber());
                hisRecipe.setTcmFee(queryHisRecipResTO.getTcmFee());
                hisRecipe.setDecoctionFee(queryHisRecipResTO.getDecoctionFee());
                hisRecipe.setDecoctionCode(queryHisRecipResTO.getDecoctionCode());
                hisRecipe.setDecoctionText(queryHisRecipResTO.getDecoctionText());
                hisRecipe.setTcmNum(queryHisRecipResTO.getTcmNum() == null ? null : String.valueOf(queryHisRecipResTO.getTcmNum()));
                //中药医嘱跟着处方 西药医嘱跟着药品（见药品详情）
                hisRecipe.setRecipeMemo(queryHisRecipResTO.getRecipeMemo());
//                hisRecipe.setMakeMethodCode(queryHisRecipResTO.getMakeMethodCode());
//                hisRecipe.setMakeMethodText(queryHisRecipResTO.getMakeMethodText());
//                hisRecipe.setJuice(queryHisRecipResTO.getJuice());
//                hisRecipe.setJuiceUnit(queryHisRecipResTO.getJuiceUnit());
//                hisRecipe.setMinor(queryHisRecipResTO.getMinor());
//                hisRecipe.setMinorUnit(queryHisRecipResTO.getMinorUnit());
//                hisRecipe.setSymptomCode(queryHisRecipResTO.getSymptomCode());
//                hisRecipe.setSymptomName(queryHisRecipResTO.getSysmptomName());
//                hisRecipe.setSpecialDecoctionCode(queryHisRecipResTO.getSpecialDecoctiionCode());
//                hisRecipe.setCardNo(queryHisRecipResTO.getCardNo());
//                hisRecipe.setCardTypeCode(queryHisRecipResTO.getCardTypeCode());
//                hisRecipe.setCardTypeName(queryHisRecipResTO.getCardTypeName());

                //审核药师
                hisRecipe.setCheckerCode(queryHisRecipResTO.getCheckerCode());
                hisRecipe.setCheckerName(queryHisRecipResTO.getCheckerName());
                try {
                    hisRecipe = hisRecipeDao.save(hisRecipe);
                    LOGGER.info("saveHisRecipeInfo hisRecipe:{} 当前时间：{}", hisRecipe, System.currentTimeMillis());
                    hisRecipes.add(hisRecipe);
                } catch (Exception e) {
                    LOGGER.error("hisRecipeDAO.save error ", e);
                    return hisRecipes;
                }

                if (null != queryHisRecipResTO.getExt()) {
                    for (ExtInfoTO extInfoTO : queryHisRecipResTO.getExt()) {
                        HisRecipeExt ext = ObjectCopyUtils.convert(extInfoTO, HisRecipeExt.class);
                        ext.setHisRecipeId(hisRecipe.getHisRecipeID());
                        hisRecipeExtDAO.save(ext);
                    }
                }

                if (null != queryHisRecipResTO.getDrugList()) {
                    for (RecipeDetailTO recipeDetailTO : queryHisRecipResTO.getDrugList()) {
                        HisRecipeDetail detail = ObjectCopyUtils.convert(recipeDetailTO, HisRecipeDetail.class);
                        detail.setHisRecipeId(hisRecipe.getHisRecipeID());
                        detail.setRecipeDeatilCode(recipeDetailTO.getRecipeDeatilCode());
                        detail.setDrugName(recipeDetailTO.getDrugName());
                        detail.setPrice(recipeDetailTO.getPrice());
                        detail.setTotalPrice(recipeDetailTO.getTotalPrice());
                        detail.setUsingRate(recipeDetailTO.getUsingRate());
                        detail.setUsePathways(recipeDetailTO.getUsePathWays());
                        detail.setDrugSpec(recipeDetailTO.getDrugSpec());
                        detail.setDrugUnit(recipeDetailTO.getDrugUnit());
                        detail.setUseDays(recipeDetailTO.getUseDays());
                        detail.setUseDaysB(recipeDetailTO.getUseDays().toString());
                        detail.setDrugCode(recipeDetailTO.getDrugCode());
                        detail.setUsingRateText(recipeDetailTO.getUsingRateText());
                        detail.setUsePathwaysText(recipeDetailTO.getUsePathwaysText());
                        //  线下特殊用法
                        detail.setUseDoseStr(recipeDetailTO.getUseDoseStr());
                        detail.setUseDose(recipeDetailTO.getUseDose());
                        detail.setUseDoseUnit(recipeDetailTO.getUseDoseUnit());
                        detail.setSaleName(recipeDetailTO.getSaleName());
                        detail.setPack(recipeDetailTO.getPack());
                        OrganDrugListDAO organDrugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);
                        if (StringUtils.isNotEmpty(detail.getRecipeDeatilCode())) {
                            List<OrganDrugList> organDrugLists = organDrugListDAO.findByOrganIdAndDrugCodes(hisRecipe.getClinicOrgan(), Arrays.asList(detail.getDrugCode()));
                            if (CollectionUtils.isEmpty(organDrugLists)) {
                                LOGGER.info("saveHisRecipeInfo organDrugLists his传过来的药品编码没有在对应机构维护,organId:" + hisRecipe.getClinicOrgan() + ",organDrugCode:" + detail.getDrugCode());
                            }
                        }
                        detail.setStatus(1);
                        //西药医嘱
                        detail.setMemo(recipeDetailTO.getMemo());
                        //药房信息
                        detail.setPharmacyCode(recipeDetailTO.getPharmacyCode());
                        detail.setPharmacyName(recipeDetailTO.getPharmacyName());
//                        detail.setPharmacyCategray(recipeDetailTO.getPharmacyCategray());
//                        detail.setTcmContraindicationCause(recipeDetailTO.getTcmContraindicationCause());
//                        detail.setTcmContraindicationType(recipeDetailTO.getTcmContraindicationType());
                        hisRecipeDetailDAO.save(detail);
                    }
                }
            } else {
                hisRecipes.add(hisRecipe1);
            }
        }
        return hisRecipes;
    }

    /**
     * 获取处方id
     *
     * @param organId    机构
     * @param recipeCode 处方号
     * @param hisRecipes 线下处方
     * @return
     */
    public Integer attachRecipeId(Integer organId, String recipeCode, List<HisRecipe> hisRecipes) {
        HisRecipe hisRecipe = new HisRecipe();
        if (CollectionUtils.isEmpty(hisRecipes)) {
            //点击卡片 历史处方his不会返回 故从表查  同时也兼容已处理状态的处方，前端漏传hisRecipeId的情况
            if (!StringUtils.isEmpty(recipeCode)) {
                hisRecipe = hisRecipeDao.getHisRecipeByRecipeCodeAndClinicOrgan(organId, recipeCode);
            }
            if (hisRecipe != null) {
                return hisRecipe.getHisRecipeID();
            } else {
                return null;
            }
        }
        return hisRecipes.get(0).getHisRecipeID();
    }


    /**
     * 设置医保信息
     *
     * @param queryHisRecipResTO his处方数据
     * @param hisRecipe          返回对象
     */
    private void setMedicalInfo(QueryHisRecipResTO queryHisRecipResTO, HisRecipe hisRecipe) {
        LOGGER.info("setMedicalInfo param queryHisRecipResTO:{},hisRecipe:{}", JSONUtils.toString(queryHisRecipResTO), JSONUtils.toString(hisRecipe));
        if (null != queryHisRecipResTO.getMedicalInfo()) {
            MedicalInfo medicalInfo = queryHisRecipResTO.getMedicalInfo();
            if (!ObjectUtils.isEmpty(medicalInfo.getMedicalAmount())) {
                hisRecipe.setMedicalAmount(medicalInfo.getMedicalAmount());
            }
            if (!ObjectUtils.isEmpty(medicalInfo.getCashAmount())) {
                hisRecipe.setCashAmount(medicalInfo.getCashAmount());
            }
            if (!ObjectUtils.isEmpty(medicalInfo.getTotalAmount())) {
                hisRecipe.setTotalAmount(medicalInfo.getTotalAmount());
            }
        }
        LOGGER.info("setMedicalInfo response hisRecipe:{} ", JSONUtils.toString(hisRecipe));
    }


    /**
     * 根据处方号获取状态
     *
     * @param mpiId      患者mpiid
     * @param organCode  查询机构
     * @param recipeCode 查询处方号
     * @return
     */
    public String attachHisRecipeStatus(String mpiId, Integer organCode, String recipeCode) {
        LOGGER.info("attachHisRecipeStatus param mpiId:{},organCode:{},recipeCode:{}", mpiId, organCode, recipeCode);
        String status = "";
        HisRecipe hisRecipe = hisRecipeDao.getHisRecipeBMpiIdyRecipeCodeAndClinicOrgan(mpiId, organCode, recipeCode);
        if (hisRecipe != null) {
            //已处理
            if (OfflineToOnlineEnum.OFFLINE_TO_ONLINE_ALREADY_PAY.getType().equals(hisRecipe.getStatus())) {
                status = OfflineToOnlineEnum.OFFLINE_TO_ONLINE_ALREADY_PAY.getName();
            }
            //待处理或进行中
            if (OfflineToOnlineEnum.OFFLINE_TO_ONLINE_NO_PAY.getType().equals(hisRecipe.getStatus())) {
                Recipe recipe = recipeDAO.getByHisRecipeCodeAndClinicOrganAndMpiid(mpiId, recipeCode, organCode);
                if (recipe != null && !StringUtils.isEmpty(recipe.getOrderCode())) {
                    status = OfflineToOnlineEnum.OFFLINE_TO_ONLINE_ONGOING.getName();
                } else {
                    status = OfflineToOnlineEnum.OFFLINE_TO_ONLINE_NO_PAY.getName();
                }
            }
        }
        if (StringUtils.isEmpty(status)) {
            LOGGER.info("attachHisRecipeStatus 根据处方单号获取不到状态");
            throw new DAOException(recipe.constant.ErrorCode.SERVICE_ERROR, "参数异常，请刷新页面后重试");
        }
        LOGGER.info("attachHisRecipeStatus res status:{}", status);
        return status;
    }
}
