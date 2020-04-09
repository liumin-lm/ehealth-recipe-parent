package recipe.service;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.ngari.base.doctor.model.DoctorBean;
import com.ngari.base.doctor.service.IDoctorService;
import com.ngari.consult.ConsultAPI;
import com.ngari.consult.ConsultBean;
import com.ngari.consult.common.model.ConsultExDTO;
import com.ngari.consult.common.service.IConsultExService;
import com.ngari.consult.common.service.IConsultService;
import com.ngari.his.base.PatientBaseInfo;
import com.ngari.his.recipe.mode.*;
import com.ngari.his.recipe.service.IRecipeHisService;
import com.ngari.patient.dto.OrganDTO;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.patient.service.OrganService;
import com.ngari.patient.service.PatientService;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.platform.recipe.mode.NoticeNgariRecipeInfoReq;
import com.ngari.recipe.common.RecipeResultBean;
import com.ngari.recipe.entity.DrugList;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.Recipedetail;
import com.ngari.recipe.recipe.model.HisRecipeBean;
import com.ngari.recipe.recipe.model.HisRecipeDetailBean;
import com.ngari.recipe.recipe.model.RecipeBean;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import com.ngari.recipe.recipelog.model.RecipeLogBean;
import ctd.persistence.DAOFactory;
import ctd.persistence.exception.DAOException;
import ctd.spring.AppDomainContext;
import ctd.util.AppContextHolder;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.ApplicationUtils;
import recipe.audit.auditmode.AuditModeContext;
import recipe.audit.auditmode.IAuditMode;
import recipe.audit.bean.AutoAuditResult;
import recipe.audit.service.PrescriptionService;
import recipe.bean.DrugEnterpriseResult;
import recipe.constant.CacheConstant;
import recipe.dao.DrugListDAO;
import recipe.dao.OrganDrugListDAO;
import recipe.dao.RecipeDAO;
import recipe.dao.RecipeDetailDAO;
import recipe.drugsenterprise.RemoteDrugEnterpriseService;
import recipe.hisservice.RecipeToHisService;
import recipe.mq.RecipeStatusFromHisObserver;
import recipe.thread.PushRecipeToRegulationCallable;
import recipe.thread.RecipeBusiThreadPool;
import recipe.util.DateConversion;
import recipe.util.RedisClient;

import java.math.BigDecimal;
import java.util.*;

import static ctd.persistence.DAOFactory.getDAO;
import static recipe.service.RecipeServiceSub.convertPatientForRAP;


/**
 * company: ngarihealth
 *
 * @author: 0184/yu_yun
 * @date:2017/10/31.
 */
@RpcBean(value = "recipePreserveService", mvc_authentication = false)
public class RecipePreserveService {
    private static final Logger LOGGER = LoggerFactory.getLogger(RecipePreserveService.class);

    @Autowired
    private RedisClient redisClient;
    @Autowired
    private AuditModeContext auditModeContext;

    @RpcService
    public RecipeBean getByRecipeId(int recipeId) {
        Recipe recipe = DAOFactory.getDAO(RecipeDAO.class).get(recipeId);
        return ObjectCopyUtils.convert(recipe, RecipeBean.class);
    }

    @RpcService
    public void manualRefundForRecipe(int recipeId, String operName, String reason) {
        RecipeService recipeService = ApplicationUtils.getRecipeService(RecipeService.class);
        recipeService.manualRefundForRecipe(recipeId, operName, reason);
    }

    /**
     * 手动推送处方单到药企
     * @param recipeId
     * @return
     */
    @RpcService
    public DrugEnterpriseResult pushSingleRecipeInfo(Integer recipeId) {
        RemoteDrugEnterpriseService service = ApplicationUtils.getRecipeService(RemoteDrugEnterpriseService.class);
        return service.pushSingleRecipeInfo(recipeId);
    }

    /**
     * 手动推送处方单到his
     * @param recipeId
     * @return
     */
    @RpcService
    public void pushSingleRecipeInfoToHis(Integer recipeId) {
        RecipeHisService hisService = ApplicationUtils.getRecipeService(RecipeHisService.class);
        RecipeDAO recipeDAO = getDAO(RecipeDAO.class);
        Recipe dbRecipe = recipeDAO.getByRecipeId(recipeId);
        hisService.sendRecipe(recipeId, dbRecipe.getClinicOrgan());
    }

    @RpcService
    public RecipeResultBean getOrderDetail(String orderCoe) {
        RecipeOrderService service = ApplicationUtils.getRecipeService(RecipeOrderService.class);
        return service.getOrderDetail(orderCoe);
    }

    @RpcService
    public RecipeResultBean finishOrderPay(String orderCode, int payFlag, Integer payMode) {
        RecipeOrderService service = ApplicationUtils.getRecipeService(RecipeOrderService.class);
        return service.finishOrderPay(orderCode, payFlag, payMode);
    }

    @RpcService
    public List<RecipeLogBean> findByRecipeId(Integer recipeId) {
        RecipeLogService service = ApplicationUtils.getRecipeService(RecipeLogService.class);
        return service.findByRecipeId(recipeId);
    }

    @RpcService
    public DoctorBean getDoctorTest(Integer doctorId) {
        IDoctorService doctorService = ApplicationUtils.getBaseService(IDoctorService.class);
        return doctorService.getBeanByDoctorId(doctorId);
    }

    @RpcService
    public Map<String,Object> getHosRecipeList(Integer consultId, Integer organId,String mpiId,Integer daysAgo){
        LOGGER.info("getHosRecipeList consultId={}, organId={},mpiId={}", consultId, organId,mpiId);
        PatientService patientService = ApplicationUtils.getBasicService(PatientService.class);
        Map<String,Object> result = Maps.newHashMap();
        PatientDTO patientDTO = patientService.get(mpiId);
        if (patientDTO == null){
            throw new DAOException(609, "找不到该患者");
        }
        OrganService organService = ApplicationUtils.getBasicService(OrganService.class);
        OrganDTO organDTO = organService.getByOrganId(organId);
        if (organDTO == null){
            throw new DAOException(609, "找不到该机构");
        }
        String cardId = null;
        String cardType = null;
        IConsultService service = ConsultAPI.getService(IConsultService.class);
        if (consultId == null){
            List<ConsultBean> consultBeans = service.findConsultByMpiId(Arrays.asList(mpiId));
            if (CollectionUtils.isNotEmpty(consultBeans)){
                consultId = consultBeans.get(0).getConsultId();
            }
        }
        if (consultId != null){
            ConsultBean consultBean = service.getById(consultId);
            if(null != consultBean){
                IConsultExService exService = ConsultAPI.getService(IConsultExService.class);
                ConsultExDTO consultExDTO = exService.getByConsultId(consultId);
                if(null != consultExDTO && StringUtils.isNotEmpty(consultExDTO.getCardId())){
                    cardId = consultExDTO.getCardId();
                    cardType = consultExDTO.getCardType();
                }
            }
        }

        Date endDate = DateTime.now().toDate();
        Date startDate = DateConversion.getDateTimeDaysAgo(daysAgo);

        IRecipeHisService hisService = AppDomainContext.getBean("his.iRecipeHisService", IRecipeHisService.class);
        QueryRecipeRequestTO request = new QueryRecipeRequestTO();
        PatientBaseInfo patientBaseInfo = new PatientBaseInfo();
        patientBaseInfo.setPatientName(patientDTO.getPatientName());
        //福建省立需要传输的就诊卡号
        patientBaseInfo.setPatientID(cardId);
        patientBaseInfo.setCertificate(patientDTO.getCertificate());
        patientBaseInfo.setCertificateType(patientDTO.getCertificateType());
        patientBaseInfo.setCardID(cardId);
        patientBaseInfo.setCardType(cardType);
        request.setPatientInfo(patientBaseInfo);
        request.setStartDate(startDate);
        request.setEndDate(endDate);
        request.setOrgan(organId);
        LOGGER.info("getHosRecipeList request={}", JSONUtils.toString(request));
        QueryRecipeResponseTO response = null;
        try {
            response = hisService.queryRecipeListInfo(request);
        } catch (Exception e) {
            LOGGER.warn("getHosRecipeList his error. ", e);
        }
        if(null == response){
            return result;
        }
        LOGGER.info("getHosRecipeList res={}", JSONUtils.toString(response));
        List<RecipeInfoTO> data = response.getData();
        //转换平台字段
        if (CollectionUtils.isEmpty(data)){
            return result;
        }
        List<HisRecipeBean> recipes = Lists.newArrayList();
        for (RecipeInfoTO recipeInfoTO: data){
            HisRecipeBean recipeBean = ObjectCopyUtils.convert(recipeInfoTO, HisRecipeBean.class);
            recipeBean.setSignDate(recipeInfoTO.getSignTime());
            recipeBean.setOrganDiseaseName(recipeInfoTO.getDiseaseName());
            recipeBean.setDepartText(recipeInfoTO.getDepartName());
            List<RecipeDetailTO> detailData = recipeInfoTO.getDetailData();
            List<HisRecipeDetailBean> hisRecipeDetailBeans = Lists.newArrayList();
            for (RecipeDetailTO recipeDetailTO: detailData){
                HisRecipeDetailBean detailBean = ObjectCopyUtils.convert(recipeDetailTO, HisRecipeDetailBean.class);
                detailBean.setDrugUnit(recipeDetailTO.getUnit());
                detailBean.setUsingRateText(recipeDetailTO.getUsingRate());
                detailBean.setUsePathwaysText(recipeDetailTO.getUsePathWays());
                detailBean.setUseDays(recipeDetailTO.getDays());
                detailBean.setUseTotalDose(recipeDetailTO.getAmount());
                hisRecipeDetailBeans.add(detailBean);
            }
            recipeBean.setDetailData(hisRecipeDetailBeans);
            recipeBean.setClinicOrgan(organId);
            recipeBean.setOrganName(organDTO.getShortName());
            recipes.add(recipeBean);
        }
        result.put("hisRecipe",recipes);
        result.put("patient",convertPatientForRAP(patientDTO));
        return result;
    }

    @RpcService
    public void deleteOldRedisDataForRecipe() {
        RecipeDAO dao = DAOFactory.getDAO(RecipeDAO.class);
        RedisClient redisClient = RedisClient.instance();
        List<String> mpiIds = dao.findAllMpiIdsFromHis();
        Set<String> keys;
        int num = 0;
        for (String mpiId : mpiIds) {
            try {
                keys = redisClient.scan("*_" + mpiId + "_1");
            } catch (Exception e) {
                LOGGER.error("redis error" + e.toString());
                return;
            }
            if (keys != null && keys.size() > 0) {
                for (String key : keys) {
                    Long del = redisClient.del(key);
                    if (del == 1) {
                        num++;
                    }
                }
            }
        }
        LOGGER.info("deleteOldRedisDataForRecipe Success num=" + num);
    }

    /**
     * 内部测试方法
     *
     * @param recipeId
     * @return
     * @throws Exception
     */
    @RpcService
    public AutoAuditResult testGetPAAnalysis(int recipeId) throws Exception {
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        RecipeDetailDAO detailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);

        Recipe dbrecipe = recipeDAO.getByRecipeId(recipeId);
        List<Recipedetail> dbdetails = detailDAO.findByRecipeId(recipeId);
        RecipeBean recipe = ObjectCopyUtils.convert(dbrecipe, RecipeBean.class);
        List<RecipeDetailBean> details = ObjectCopyUtils.convert(dbdetails, RecipeDetailBean.class);
        PrescriptionService service = ApplicationUtils.getRecipeService(PrescriptionService.class);
        return service.analysis(recipe, details);
    }

    /**
     * hash操作
     *
     * @param key
     * @param pattern
     * @return
     */
    @RpcService
    public Map<String, Object> redisScanForHash(String key, String pattern) {
        return redisClient.hScan(key, 10000, pattern);
    }

    @RpcService
    public Object redisGetForHash(String key, String filed) {
        return redisClient.hget(key, filed);
    }

    @RpcService
    public boolean redisAddForHash(String key, String filed, String value) {
        return redisClient.hset(key, filed, value);
    }
    @RpcService
    public Long redisDelForHash(String key, String filed) {
        return redisClient.hdel(key, filed);
    }

    /**
     * Set操作
     *
     * @param key
     * @param organId
     */
    @RpcService
    public void redisAddForSet(String key, String organId) {
        redisClient.sAdd(key, organId);
    }

    @RpcService
    public Set redisGetForSet(String key) {
        return redisClient.sMembers(key);
    }

    @RpcService
    public Long redisRemoveForSet(String key, String organId) {
        return redisClient.sRemove(key, organId);
    }

    @RpcService
    public void redisSet(String key, String value){
        redisClient.set(key, value);
    }

    /**
     * 以下为key的操作
     *
     * @param key
     * @param val
     * @param timeout
     */
    @RpcService
    public void redisForAdd(String key, String val, Long timeout) {
        if (null == timeout || Long.valueOf(-1L).equals(timeout)) {
            redisClient.setForever(key, val);
        } else {
            redisClient.setEX(key, timeout, val);
        }
    }

    @RpcService
    public boolean redisForAddNx(String key, String val) {
        return redisClient.setNX(key, val);
    }

    @RpcService
    public long redisForDel(String key) {
        return redisClient.del(key);
    }

    @RpcService
    public Object redisGet(String key) {
        return redisClient.get(key);
    }

    /************************************以下为一些数据初始化操作******************************/

    /**
     * 机构用药频次初始化， 缓存内数据结构应该为 key为xxx_organId， map的key为his内编码，value为平台内编码
     *
     * @param organId
     * @param map
     */
    @RpcService
    public void initUsingRate(int organId, Map<String, String> map) {
        Set<Map.Entry<String, String>> set = map.entrySet();
        for (Map.Entry<String, String> entry : set) {
            redisAddForHash(CacheConstant.KEY_ORGAN_USINGRATE + organId, entry.getKey(), entry.getValue());
        }
    }

    @RpcService
    public void initNgariUsingRate(int organId, Map<String, String> map) {
        Set<Map.Entry<String, String>> set = map.entrySet();
        for (Map.Entry<String, String> entry : set) {
            redisAddForHash(CacheConstant.KEY_NGARI_USINGRATE + organId, entry.getKey(), entry.getValue());
        }
    }

    /**
     * 机构用药方式初始化，缓存内数据结构应该为 key为xxx_organId， map的key为his内编码，value为平台内编码
     *
     * @param organId
     * @param map
     */
    @RpcService
    public void initUsePathways(int organId, Map<String, String> map) {
        Set<Map.Entry<String, String>> set = map.entrySet();
        for (Map.Entry<String, String> entry : set) {
            redisAddForHash(CacheConstant.KEY_ORGAN_USEPATHWAYS + organId, entry.getKey(), entry.getValue());
        }
    }

    @RpcService
    public void initNgariUsePathways(int organId, Map<String, String> map) {
        Set<Map.Entry<String, String>> set = map.entrySet();
        for (Map.Entry<String, String> entry : set) {
            redisAddForHash(CacheConstant.KEY_NGARI_USEPATHWAYS + organId, entry.getKey(), entry.getValue());
        }
    }

    /**
     * 机构用药方式初始化，缓存内数据结构应该为 key为xxx_organId， map的key为平台内编码，value为his内编码
     *  平台转his
     * @param organId
     * @param map
     */
    @RpcService
    public void initUsingRateForNagri(int organId, Map<String, String> map) {
        Set<Map.Entry<String, String>> set = map.entrySet();
        for (Map.Entry<String, String> entry : set) {
            redisAddForHash(CacheConstant.KEY_NGARI_USINGRATE + organId, entry.getKey(), entry.getValue());
        }
    }

    /**
     * 机构用药方式初始化，缓存内数据结构应该为 key为xxx_organId，map的key为平台内编码，value为his内编码
     *
     * @param organId
     * @param map
     */
    @RpcService
    public void initUsePathwaysForNgari(int organId, Map<String, String> map) {
        Set<Map.Entry<String, String>> set = map.entrySet();
        for (Map.Entry<String, String> entry : set) {
            redisAddForHash(CacheConstant.KEY_NGARI_USEPATHWAYS + organId, entry.getKey(), entry.getValue());
        }
    }

    /**
     * 机构用药频次初始化，缓存内数据结构应该为 key为xxx_organId， map的key为平台内编码，value为频次医保编码
     *  平台转his
     * @param organId
     * @param map
     */
    @RpcService
    public void initMedicalUsingRateForNagri(String organId, Map<String, String> map) {
        Set<Map.Entry<String, String>> set = map.entrySet();
        for (Map.Entry<String, String> entry : set) {
            redisAddForHash(CacheConstant.KEY_MEDICAL_NGARI_USINGRATE + organId, entry.getKey(), entry.getValue());
        }
    }

    /**
     * 更新his药品药房名称
     * @param organId
     * @param pharmacy
     * @return
     */
    @RpcService
    public Boolean updatePharmacyName(int organId, String pharmacy) {
        OrganDrugListDAO dao = DAOFactory.getDAO(OrganDrugListDAO.class);
        Boolean result = dao.updatePharmacy(organId, pharmacy);
        return result;

    }

    /**
     * 手动推送处方审核数据到监管平台
     * @param recipeId
     */
    @RpcService
    public void uploadRegulationAuditData(Integer recipeId){
        //手动推送处方到监管平台
        RecipeBusiThreadPool.submit(new PushRecipeToRegulationCallable(recipeId,2));
    }

    /**
     * 手动接收从HIS发来的处方状态变更消息
     */
    @RpcService
    public void testRecipeStatusFromHisObserver(NoticeNgariRecipeInfoReq req){
        RecipeStatusFromHisObserver observer = new RecipeStatusFromHisObserver();
        observer.onMessage(req);
    }

    /**
     * 测试获取审方模式
     */
    @RpcService
    public void test(Integer audit){
        IAuditMode auditModes = auditModeContext.getAuditModes(audit);
        System.out.println(auditModes);
    }

    /**
     * 手动同步基础药品数据给his(武昌)
     * @param sourceOrgan
     */
    @RpcService(timeout = 600)
    public int syncDrugListToHis(Integer sourceOrgan){
        DrugListDAO dao = DAOFactory.getDAO(DrugListDAO.class);
        RecipeToHisService service = AppContextHolder.getBean("recipeToHisService", RecipeToHisService.class);
        OrganService organService = ApplicationUtils.getBasicService(OrganService.class);
        String organCode = organService.getOrganizeCodeByOrganId(sourceOrgan);
        //批量同步
        int start = 0;
        int limit = 100;
        int total = 0;
        while (true){
            List<DrugList> drugs = dao.findDrugListBySourceOrgan(1001780,start,limit);
            if (drugs == null || drugs.size() == 0){
                return total;
            }
            total += drugs.size();
            List<DrugListTO> drugListTO = ObjectCopyUtils.convert(drugs, DrugListTO.class);
            //double失真处理
            for (DrugListTO drugTO : drugListTO){
                if (drugTO.getUseDose() != null){
                    drugTO.setUseDose(BigDecimal.valueOf(drugTO.getUseDose()).doubleValue());
                }
                if (drugTO.getPrice1() != null){
                    drugTO.setPrice1(BigDecimal.valueOf(drugTO.getPrice1()).doubleValue());

                }else {
                    drugTO.setPrice1(0.0);
                }
                drugTO.setPrice2(drugTO.getPrice1());

            }
            SyncDrugListToHisReqTO request = new SyncDrugListToHisReqTO();
            request.setClinicOrgan(sourceOrgan);
            //组织机构编码
            request.setOrganCode(organCode);
            request.setDrugList(drugListTO);
            service.syncDrugListToHis(request);
            start += limit;
        }

    }
}
