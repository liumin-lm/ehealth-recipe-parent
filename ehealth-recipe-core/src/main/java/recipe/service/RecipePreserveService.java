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
import org.springframework.beans.factory.annotation.Autowired;
import recipe.ApplicationUtils;
import recipe.bean.DrugEnterpriseResult;
import recipe.dao.RecipeDAO;
import recipe.drugsenterprise.RemoteDrugEnterpriseService;
import recipe.util.RedisClient;

import java.util.List;
import java.util.Set;

/**
 * company: ngarihealth
 *
 * @author: 0184/yu_yun
 * @date:2017/10/31.
 */
@RpcBean(value = "recipePreserveService", mvc_authentication = false)
public class RecipePreserveService {

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


    /**
     * Set操作
     * @param key
     * @param organId
     */
    @RpcService
    public void redisAddForSet(String key, String organId){
         redisClient.sAdd(key, organId);
    }

    @RpcService
    public Set redisGetForSet(String key){
        return redisClient.sMembers(key);
    }

    @RpcService
    public Long redisRemoveForSet(String key, String organId){
        return redisClient.sRemove(key, organId);
    }


    /**
     * 以下为key的操作
     * @param key
     * @param val
     * @param timeout
     */
    @RpcService
    public void redisForAdd(String key, String val, Long timeout){
        if(null == timeout || Long.valueOf(-1L).equals(timeout)){
            redisClient.setForever(key, val);
        }else {
            redisClient.setEX(key, timeout, val);
        }
    }

    @RpcService
    public boolean redisForAddNx(String key, String val){
        return redisClient.setNX(key, val);
    }

    @RpcService
    public long redisForDel(String key){
        return redisClient.del(key);
    }

    @RpcService
    public Object redisGet(String key){
        return redisClient.get(key);
    }
}
