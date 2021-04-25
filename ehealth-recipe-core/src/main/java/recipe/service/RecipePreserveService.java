package recipe.service;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.ngari.base.doctor.model.DoctorBean;
import com.ngari.base.doctor.service.IDoctorService;
import com.ngari.base.dto.UsePathwaysDTO;
import com.ngari.base.dto.UsingRateDTO;
import com.ngari.his.base.PatientBaseInfo;
import com.ngari.his.recipe.mode.*;
import com.ngari.his.recipe.service.IRecipeHisService;
import com.ngari.patient.dto.OrganDTO;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.patient.service.HealthCardService;
import com.ngari.patient.service.OrganService;
import com.ngari.patient.service.PatientService;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.platform.recipe.mode.NoticeNgariRecipeInfoReq;
import com.ngari.recipe.basic.ds.PatientVO;
import com.ngari.recipe.common.RecipeResultBean;
import com.ngari.recipe.entity.DrugList;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.Recipedetail;
import com.ngari.recipe.recipe.model.*;
import com.ngari.recipe.recipelog.model.RecipeLogBean;
import com.ngari.revisit.RevisitAPI;
import com.ngari.revisit.RevisitBean;
import com.ngari.revisit.common.model.RevisitExDTO;
import com.ngari.revisit.common.service.IRevisitExService;
import com.ngari.revisit.common.service.IRevisitService;
import ctd.persistence.DAOFactory;
import static ctd.persistence.DAOFactory.getDAO;
import ctd.persistence.exception.DAOException;
import ctd.spring.AppDomainContext;
import ctd.util.AppContextHolder;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import ctd.util.event.GlobalEventExecFactory;
import eh.recipeaudit.model.Intelligent.AutoAuditResultBean;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.ApplicationUtils;
import recipe.audit.auditmode.AuditModeContext;
import recipe.audit.auditmode.IAuditMode;
import recipe.audit.service.PrescriptionService;
import recipe.bean.DrugEnterpriseResult;
import recipe.bussutil.drugdisplay.DrugDisplayNameProducer;
import recipe.bussutil.drugdisplay.DrugNameDisplayUtil;
import recipe.constant.CacheConstant;
import recipe.dao.DrugListDAO;
import recipe.dao.OrganDrugListDAO;
import recipe.dao.RecipeDAO;
import recipe.dao.RecipeDetailDAO;
import recipe.drugsenterprise.RemoteDrugEnterpriseService;
import recipe.hisservice.RecipeToHisService;
import recipe.mq.RecipeStatusFromHisObserver;
import static recipe.service.RecipeServiceSub.convertSensitivePatientForRAP;
import recipe.thread.PushRecipeToRegulationCallable;
import recipe.thread.RecipeBusiThreadPool;
import recipe.util.DateConversion;
import recipe.util.MapValueUtil;
import recipe.util.RedisClient;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;


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

    /**
     * 多线程查询多机构线下处方
     * @param consultId
     * @param organIds
     * @param mpiId
     * @param daysAgo
     * @Author liumin
     * @return
     */
    public Map<String, Object> getAllHosRecipeList(Integer consultId, List<Integer> organIds, String mpiId, Integer daysAgo) {
        LOGGER.info("getAllHosRecipeList consultId={}, organIds={},mpiId={}", consultId, JSONUtils.toString(organIds), mpiId);
        OrganService bean = AppContextHolder.getBean("basic.organService", OrganService.class);
        List<FutureTask<Map<String, Object> >> futureTasks = new ArrayList<FutureTask<Map<String, Object> >>();
        for(int i=0;i<organIds.size();i++){
            Integer organIdChild=organIds.get(i);
            futureTasks.add(new FutureTask<>(new Callable<Map<String, Object> >() {
                @Override
                public Map<String, Object>  call() {
                    // 线程执行程序
                    return getHosRecipeList(consultId, organIdChild, mpiId, daysAgo);
                }
            }));
        }
        // 加入 线程池
        for (FutureTask<Map<String, Object>> futureTask : futureTasks) {
            GlobalEventExecFactory.instance().getExecutor().submit(futureTask);
        }

        List<HisRecipeBean> hisRecipes=new ArrayList<>();
        PatientVO patientVO=new PatientVO();
        Map<String, Object> upderLineRecipesByHis = new ConcurrentHashMap<>();
        // 获取线程返回结果
        for (int i = 0; i < futureTasks.size(); i++) {
            Map<String, Object> map = new ConcurrentHashMap<>();
            try {
                map = futureTasks.get(i).get(4000, TimeUnit.MILLISECONDS);
                LOGGER.info("getAllHosRecipeList 从his获取已缴费处方信息:{}", JSONUtils.toString(map));
                if(i==0){
                    patientVO=(PatientVO) map.get("patient");
                }
                List<HisRecipeBean> hisRecipeBeans=(List<HisRecipeBean>)map.get("hisRecipe");
                if(CollectionUtils.isNotEmpty(hisRecipeBeans)){
                    hisRecipes.addAll(hisRecipeBeans);
                }
            } catch (Exception e) {
                e.printStackTrace();
                LOGGER.error("getAllHosRecipeList futureTasks exception:{}", e.getMessage(), e);
            }
        }
        upderLineRecipesByHis.put("hisRecipe",hisRecipes);
        upderLineRecipesByHis.put("patient",patientVO);
        LOGGER.info("getAllHosRecipeList response:{}",JSONUtils.toString(upderLineRecipesByHis));
        return upderLineRecipesByHis;
    }

    @RpcService
    public Map<String, Object> getHosRecipeList(Integer consultId, Integer organId, String mpiId, Integer daysAgo) {
        LOGGER.info("getHosRecipeList consultId={}, organId={},mpiId={}", consultId, organId, mpiId);
        PatientService patientService = ApplicationUtils.getBasicService(PatientService.class);
        HealthCardService healthCardService = ApplicationUtils.getBasicService(HealthCardService.class);
        Map<String, Object> result = Maps.newHashMap();
        PatientDTO patientDTO = patientService.get(mpiId);
        if (patientDTO == null) {
            throw new DAOException(609, "找不到该患者");
        }
        OrganService organService = ApplicationUtils.getBasicService(OrganService.class);
        OrganDTO organDTO = organService.getByOrganId(organId);
        if (organDTO == null) {
            throw new DAOException(609, "找不到该机构");
        }
        String cardId = null;
        String cardType = null;
        IRevisitService iRevisitService = RevisitAPI.getService(IRevisitService.class);
        if (consultId == null) {
            List<RevisitBean> revisitBeans = iRevisitService.findConsultByMpiId(Arrays.asList(mpiId));
            if (CollectionUtils.isNotEmpty(revisitBeans)) {
                consultId = revisitBeans.get(0).getConsultId();
            }
        }
        if (consultId != null) {
            RevisitBean revisitBean = iRevisitService.getById(consultId);
            if (null != revisitBean) {
                IRevisitExService exService = RevisitAPI.getService(IRevisitExService.class);
                RevisitExDTO consultExDTO = exService.getByConsultId(consultId);
                if (null != consultExDTO && StringUtils.isNotEmpty(consultExDTO.getCardId())) {
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
        String cityCardNumber = healthCardService.getMedicareCardId(mpiId, organId);
        if (StringUtils.isNotEmpty(cityCardNumber)) {
            patientBaseInfo.setCityCardNumber(cityCardNumber);
        }
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
        LOGGER.info("getHosRecipeList res={}", JSONUtils.toString(response));
        if(null == response){
            return result;
        }
        List<RecipeInfoTO> data = response.getData();
        //转换平台字段
        if (CollectionUtils.isEmpty(data)){
            return result;
        }
        List<HisRecipeBean> recipes = Lists.newArrayList();
        //默认西药
        Integer recipeType = 1;
        for (RecipeInfoTO recipeInfoTO: data){
            HisRecipeBean recipeBean = ObjectCopyUtils.convert(recipeInfoTO, HisRecipeBean.class);
            recipeBean.setSignDate(recipeInfoTO.getSignTime());
            recipeBean.setOrganDiseaseName(recipeInfoTO.getDiseaseName());
            recipeBean.setDepartText(recipeInfoTO.getDepartName());
            recipeBean.setCopyNum(recipeInfoTO.getCopyNum()==null?0:recipeInfoTO.getCopyNum());
            recipeBean.setRecipeMemo(StringUtils.isEmpty(recipeInfoTO.getRecipeMemo())?"":recipeInfoTO.getRecipeMemo());
            RecipeExtendBean recipeExtend = new RecipeExtendBean();
            if (recipeInfoTO.getRecipeExtendBean() != null) {
                recipeExtend.setDecoctionText(recipeInfoTO.getRecipeExtendBean().getDecoctionText());
                recipeExtend.setJuice(recipeInfoTO.getRecipeExtendBean().getJuice());
                recipeExtend.setJuiceUnit(recipeInfoTO.getRecipeExtendBean().getJuiceUnit());
                recipeExtend.setMakeMethodText(recipeInfoTO.getRecipeExtendBean().getMakeMethodText());
                recipeExtend.setMinor(recipeInfoTO.getRecipeExtendBean().getMinor());
                recipeExtend.setMinorUnit(recipeInfoTO.getRecipeExtendBean().getMinorUnit());
            } else {
                recipeExtend.setDecoctionText("");
                recipeExtend.setJuice("");
                recipeExtend.setJuiceUnit("");
                recipeExtend.setMakeMethodText("");
                recipeExtend.setMinor("");
                recipeExtend.setMinorUnit("");
            }
            recipeBean.setRecipeExtend(recipeExtend);
            List<RecipeDetailTO> detailData = recipeInfoTO.getDetailData();
            List<HisRecipeDetailBean> hisRecipeDetailBeans = Lists.newArrayList();
            try {
                recipeType = Integer.valueOf(recipeBean.getRecipeType());
            } catch (NumberFormatException e) {
                LOGGER.error("getHosRecipeList recipeType trans error", e);
            }
            //药品名拼接配置
            Map<String, Integer> configDrugNameMap = MapValueUtil.strArraytoMap(DrugNameDisplayUtil.getDrugNameConfigByDrugType(organId, recipeType));
            for (RecipeDetailTO recipeDetailTO: detailData){
                HisRecipeDetailBean detailBean = ObjectCopyUtils.convert(recipeDetailTO, HisRecipeDetailBean.class);
                detailBean.setDrugUnit(recipeDetailTO.getUnit());
                detailBean.setUsingRateText(recipeDetailTO.getUsingRate());
                detailBean.setUsePathwaysText(recipeDetailTO.getUsePathWays());
                detailBean.setUseDays(recipeDetailTO.getDays());
                detailBean.setUseTotalDose(recipeDetailTO.getAmount());
                detailBean.setDrugSpec(recipeDetailTO.getDrugSpec());
                detailBean.setPharmacyCode(recipeDetailTO.getPharmacyCode());
                detailBean.setUsingRate(recipeDetailTO.getUsingRateCode());
                detailBean.setUsePathways(recipeDetailTO.getUsePathwaysCode());
                detailBean.setUseDose(recipeDetailTO.getUseDose());
                detailBean.setUseDoseUnit(recipeDetailTO.getUseDoseUnit());
                detailBean.setDrugForm(recipeDetailTO.getDrugForm());
                detailBean.setDrugDisplaySplicedName(DrugDisplayNameProducer.getDrugName(detailBean, configDrugNameMap, DrugNameDisplayUtil.getDrugNameConfigKey(recipeType)));
                hisRecipeDetailBeans.add(detailBean);
            }
            recipeBean.setDetailData(hisRecipeDetailBeans);
            recipeBean.setClinicOrgan(organId);
            recipeBean.setOrganName(organDTO.getShortName());
            recipes.add(recipeBean);
        }
        result.put("hisRecipe",recipes);
        result.put("patient",convertSensitivePatientForRAP(patientDTO));
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
                LOGGER.error("redis error" , e);
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
    public AutoAuditResultBean testGetPAAnalysis(int recipeId) throws Exception {
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
     * 模糊获取key
     * @param pattern 例如 RCP_NGARI_USEPATHWAYS_*
     * @return
     */
    @RpcService
    public Set<String> scanLikeKey(String pattern) {
        return redisClient.scan(pattern);
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

    /**
     *
     * 获取维护到redis里的老的机构用药频次对照数据
     */
    @RpcService
    public List<UsingRateDTO> findUsingRateRelationFromRedis(){

        Set<String> usingRateParams = null;
        try {
            usingRateParams = redisClient.scan("RCP_NGARI_USINGRATE_*");
        } catch (Exception e) {
            LOGGER.error("findUsingRateRelationFromRedis redis scan error",e);
        }
        List<UsingRateDTO> usingRateDTOS = Lists.newArrayList();
        LOGGER.info("findUsingRateRelationFromRedis init usingRateParams[{}] size[{}]",JSONUtils.toString(usingRateParams),usingRateParams.size());
        try {
            if (CollectionUtils.isNotEmpty(usingRateParams)){
                for (String usingRateParam : usingRateParams) {
                    String organId = usingRateParam.substring(20);
                    Map<String, Object> map = redisScanForHash(usingRateParam, "*");
                    if (map != null){
                        UsingRateDTO usingRateDTO;
                        for (Map.Entry<String, Object> entry : map.entrySet()) {
                            usingRateDTO = new UsingRateDTO();
                            usingRateDTO.setRelatedPlatformKey(entry.getKey());
                            usingRateDTO.setUsingRateKey((String) entry.getValue());
                            usingRateDTO.setOrganId(Integer.valueOf(organId));
                            usingRateDTOS.add(usingRateDTO);
                        }
                    }else {
                        LOGGER.error("findUsingRateRelationFromRedis null organId[{}]",organId);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("findUsingRateRelationFromRedis error",e);
        }
        return usingRateDTOS;
    }

    /**
     *
     * 获取维护到redis里的老的机构用药途径对照数据
     */
    @RpcService
    public List<UsePathwaysDTO> findUsePathwaysRelationFromRedis(){
        Set<String> usingPathwaysParams = null;
        try {
            usingPathwaysParams = redisClient.scan("RCP_NGARI_USEPATHWAYS_*");
        } catch (Exception e) {
            LOGGER.error("findUsePathwaysRelationFromRedis redis scan error",e);
        }
        List<UsePathwaysDTO> usePathwaysDTOS = Lists.newArrayList();
        LOGGER.info("findUsePathwaysRelationFromRedis init usingPathwaysParams[{}] size[{}]",JSONUtils.toString(usingPathwaysParams),usingPathwaysParams.size());
        try {
            if (CollectionUtils.isNotEmpty(usingPathwaysParams)){
                for (String usingPathwaysParam : usingPathwaysParams) {
                    String organId = usingPathwaysParam.substring(22);
                    Map<String, Object> map = redisScanForHash(usingPathwaysParam, "*");
                    if (map != null){
                        UsePathwaysDTO usePathwaysDTO;
                        for (Map.Entry<String, Object> entry : map.entrySet()) {
                            usePathwaysDTO = new UsePathwaysDTO();
                            usePathwaysDTO.setRelatedPlatformKey(entry.getKey());
                            usePathwaysDTO.setPathwaysKey((String) entry.getValue());
                            usePathwaysDTO.setOrganId(Integer.valueOf(organId));
                            usePathwaysDTOS.add(usePathwaysDTO);
                        }
                    }else {
                        LOGGER.error("findUsePathwaysRelationFromRedis null organId[{}]",organId);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("findUsePathwaysRelationFromRedis error",e);
        }
        return usePathwaysDTOS;
    }
}
