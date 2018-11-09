package recipe.service;

import com.ngari.base.doctor.model.DoctorBean;
import com.ngari.base.doctor.service.IDoctorService;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.common.RecipeResultBean;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.recipe.model.RecipeBean;
import com.ngari.recipe.recipelog.model.RecipeLogBean;
import ctd.persistence.DAOFactory;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.ApplicationUtils;
import recipe.bean.DrugEnterpriseResult;
import recipe.constant.CacheConstant;
import recipe.dao.RecipeDAO;
import recipe.drugsenterprise.RemoteDrugEnterpriseService;
import recipe.util.RedisClient;

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
}
