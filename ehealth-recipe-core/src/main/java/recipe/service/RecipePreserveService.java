package recipe.service;

import com.google.common.collect.Lists;
import com.ngari.base.doctor.model.DoctorBean;
import com.ngari.base.doctor.service.IDoctorService;
import com.ngari.consult.ConsultAPI;
import com.ngari.consult.ConsultBean;
import com.ngari.consult.common.model.ConsultExDTO;
import com.ngari.consult.common.service.IConsultExService;
import com.ngari.consult.common.service.IConsultService;
import com.ngari.his.base.PatientBaseInfo;
import com.ngari.his.recipe.mode.QueryRecipeRequestTO;
import com.ngari.his.recipe.mode.QueryRecipeResponseTO;
import com.ngari.his.recipe.mode.RecipeInfoTO;
import com.ngari.his.recipe.service.IRecipeHisService;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.patient.service.PatientService;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.common.RecipeResultBean;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.Recipedetail;
import com.ngari.recipe.recipe.model.RecipeBean;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import com.ngari.recipe.recipelog.model.RecipeLogBean;
import ctd.persistence.DAOFactory;
import ctd.persistence.exception.DAOException;
import ctd.spring.AppDomainContext;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.ApplicationUtils;
import recipe.audit.bean.AutoAuditResult;
import recipe.audit.service.PrescriptionService;
import recipe.bean.DrugEnterpriseResult;
import recipe.constant.CacheConstant;
import recipe.dao.RecipeDAO;
import recipe.dao.RecipeDetailDAO;
import recipe.drugsenterprise.RemoteDrugEnterpriseService;
import recipe.util.DateConversion;
import recipe.util.RedisClient;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;


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

    @RpcService
    public DrugEnterpriseResult pushSingleRecipeInfo(Integer recipeId) {
        RemoteDrugEnterpriseService service = ApplicationUtils.getRecipeService(RemoteDrugEnterpriseService.class);
        return service.pushSingleRecipeInfo(recipeId);
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
    public List<RecipeInfoTO> getHosRecipeList(Integer consultId, Integer organId,String mpiId){
        LOGGER.info("getHosRecipeList consultId={}, organId={},mpiId={}", consultId, organId,mpiId);
        String cardId = null;
        if (consultId != null){
            IConsultService service = ConsultAPI.getService(IConsultService.class);
            ConsultBean consultBean = service.getById(consultId);
            if(null == consultBean){
                return Lists.newArrayList();
            }

            IConsultExService exService = ConsultAPI.getService(IConsultExService.class);
            ConsultExDTO consultExDTO = exService.getByConsultId(consultId);
            if(null == consultExDTO || StringUtils.isEmpty(consultExDTO.getCardId())){
                return Lists.newArrayList();
            }
            cardId = consultExDTO.getCardId();
        }
        PatientService patientService = ApplicationUtils.getBasicService(PatientService.class);
        PatientDTO patientDTO = patientService.get(mpiId);
        if (patientDTO == null){
            throw new DAOException(609, "找不到该患者");
        }

        Date endDate = DateTime.now().toDate();
        Date startDate = DateConversion.getDateTimeDaysAgo(180);

        IRecipeHisService hisService = AppDomainContext.getBean("his.iRecipeHisService", IRecipeHisService.class);
        QueryRecipeRequestTO request = new QueryRecipeRequestTO();
        PatientBaseInfo patientBaseInfo = new PatientBaseInfo();
        patientBaseInfo.setPatientName(patientDTO.getPatientName());
        patientBaseInfo.setPatientID(cardId);
        patientBaseInfo.setCertificate(patientDTO.getCertificate());
        patientBaseInfo.setCertificateType(patientDTO.getCertificateType());
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
            return Lists.newArrayList();
        }
        LOGGER.info("getHosRecipeList msgCode={}, msg={} ", response.getMsgCode(), response.getMsg());
        return response.getData();
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
}
