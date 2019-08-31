package recipe.purchase;

import com.ngari.base.hisconfig.service.IHisConfigService;
import com.ngari.consult.common.model.ConsultExDTO;
import com.ngari.consult.common.service.IConsultExService;
import com.ngari.patient.dto.OrganDTO;
import com.ngari.patient.service.OrganService;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.common.RecipeResultBean;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeOrder;
import com.ngari.recipe.entity.Recipedetail;
import com.ngari.recipe.recipe.model.RecipeBean;
import com.ngari.recipe.recipeorder.model.OrderCreateResult;
import com.ngari.recipe.recipeorder.model.RecipeOrderBean;
import ctd.persistence.DAOFactory;
import ctd.persistence.exception.DAOException;
import ctd.util.AppContextHolder;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.ApplicationUtils;
import recipe.bean.PltPurchaseResponse;
import recipe.constant.*;
import recipe.dao.RecipeDAO;
import recipe.dao.RecipeDetailDAO;
import recipe.dao.RecipeOrderDAO;
import recipe.service.RecipeListService;
import recipe.service.RecipeService;
import recipe.util.MapValueUtil;
import recipe.util.RedisClient;

import java.util.List;
import java.util.Map;

/**
 * @author： 0184/yu_yun
 * @date： 2019/6/18
 * @description： 购药入口类
 * @version： 1.0
 */
@RpcBean(value = "purchaseService")
public class PurchaseService {

    /**
     * logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(PurchaseService.class);

    @Autowired
    private RedisClient redisClient;


    /**
     * 获取可用购药方式
     * @param recipeId 处方单ID
     * @param mpiId    患者mpiId
     * @return         响应
     */
    @RpcService
    public PltPurchaseResponse showPurchaseMode(Integer recipeId, String mpiId) {
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        RecipeListService recipeListService = ApplicationUtils.getRecipeService(RecipeListService.class);
        PltPurchaseResponse result = new PltPurchaseResponse();
        if (StringUtils.isNotEmpty(mpiId)) {
            Map<String, Object> map = recipeListService.getLastestPendingRecipe(mpiId);
            List<Map> recipes = (List<Map>) map.get("recipes");
            if (CollectionUtils.isNotEmpty(recipes)) {
                RecipeBean recipeBean = (RecipeBean) recipes.get(0).get("recipe");
                recipeId = recipeBean.getRecipeId();
            }
        }
        Recipe dbRecipe = recipeDAO.get(recipeId);
        if (null == dbRecipe) {
            return result;
        }
        //TODO 配送到家和药店取药默认可用
        result.setSendToHome(true);
        result.setTfds(true);
        //到院取药判断
        try {
            IHisConfigService iHisConfigService = ApplicationUtils.getBaseService(IHisConfigService.class);
            OrganService organService = ApplicationUtils.getBasicService(OrganService.class);
            boolean hisStatus = iHisConfigService.isHisEnable(dbRecipe.getClinicOrgan());
            //机构设置，是否可以到院取药
            boolean flag = organService.getTakeMedicineFlagById(dbRecipe.getClinicOrgan());
            if (Integer.valueOf(0).equals(dbRecipe.getDistributionFlag())
                    && hisStatus && flag) {
                result.setToHos(true);
            }
        } catch (Exception e) {
            LOG.warn("showPurchaseMode 到院取药判断 exception. recipeId={}", recipeId, e);
        }
        return result;
    }

    /**
     * 根据对应的购药方式展示对应药企
     *
     * @param recipeId  处方id
     * @param payModes  购药方式
     */
    @RpcService
    public RecipeResultBean filterSupportDepList(Integer recipeId, List<Integer> payModes, Map<String, String> extInfo) {
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);

        RecipeResultBean resultBean = RecipeResultBean.getSuccess();
        Recipe dbRecipe = recipeDAO.get(recipeId);
        if (null == dbRecipe) {
            resultBean.setCode(RecipeResultBean.FAIL);
            resultBean.setMsg("处方不存在");
            return resultBean;
        }

        if(CollectionUtils.isEmpty(payModes)){
            resultBean.setCode(RecipeResultBean.FAIL);
            resultBean.setMsg("参数错误");
            return resultBean;
        }

        //处方单状态不是待处理 or 处方单已被处理
        boolean dealFlag = checkRecipeIsUser(dbRecipe, resultBean);
        if(dealFlag){
            return resultBean;
        }

        for (Integer i : payModes) {
            IPurchaseService purchaseService = getService(i);
            //如果涉及到多种购药方式合并成一个列表，此处需要进行合并
            resultBean = purchaseService.findSupportDepList(dbRecipe, extInfo);

        }

        return resultBean;
    }

    /**
     * @param recipeId
     * @param extInfo  参照RecipeOrderService createOrder定义
     *                 {"operMpiId":"当前操作者编码","addressId":"当前选中地址","payway":"支付方式（payway）","payMode":"处方支付方式",
     *                 "decoctionFlag":"1(1：代煎，0：不代煎)", "gfFeeFlag":"1(1：表示需要制作费，0：不需要)", “depId”:"指定药企ID",
     *                 "expressFee":"快递费","gysCode":"药店编码","sendMethod":"送货方式","payMethod":"支付方式","appId":"公众号ID",
     *                 "calculateFee":"1(1:需要，0:不需要)"}
     *                 <p>
     *                 ps: decoctionFlag是中药处方时设置为1，gfFeeFlag是膏方时设置为1
     *                 gysCode, sendMethod, payMethod 字段为钥世圈字段，会在findSupportDepList接口中给出
     *                 payMode 如果钥世圈有供应商是多种方式支持，就传0
     * @return  结果
     */
    @RpcService
    public OrderCreateResult order(Integer recipeId, Map<String, String> extInfo) {
        OrderCreateResult result = new OrderCreateResult(RecipeResultBean.SUCCESS);
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);

        Recipe dbRecipe = recipeDAO.get(recipeId);
        if (null == dbRecipe) {
            result.setCode(RecipeResultBean.FAIL);
            result.setMsg("处方不存在");
            return result;
        }

        Integer payMode = MapValueUtil.getInteger(extInfo, "payMode");
        if (null == payMode) {
            result.setCode(RecipeResultBean.FAIL);
            result.setMsg("缺少购药方式");
            return result;
        }

        //处方单状态不是待处理 or 处方单已被处理
        boolean dealFlag = checkRecipeIsDeal(dbRecipe, result, extInfo);
        if(dealFlag){
            return result;
        }

        //判断是否存在订单
        RecipeOrderDAO orderDAO = DAOFactory.getDAO(RecipeOrderDAO.class);
        if (StringUtils.isNotEmpty(dbRecipe.getOrderCode())) {
            RecipeOrder order = orderDAO.getByOrderCode(dbRecipe.getOrderCode());
            if (1 == order.getEffective()) {
                result.setOrderCode(order.getOrderCode());
                result.setBusId(order.getOrderId());
                result.setObject(ObjectCopyUtils.convert(order, RecipeOrderBean.class));
                result.setCode(RecipeResultBean.FAIL);
                result.setMsg("您有正在进行中的订单");
                unLock(recipeId);
                return result;
            }
        }

        OrganService organService = ApplicationUtils.getBasicService(OrganService.class);
        RecipeService recipeService = ApplicationUtils.getRecipeService(RecipeService.class);
        IHisConfigService iHisConfigService = ApplicationUtils.getBaseService(IHisConfigService.class);
        try {
            //判断院内是否已取药，防止重复购买
            boolean flag = organService.getTakeMedicineFlagById(dbRecipe.getClinicOrgan());
            boolean hisStatus = iHisConfigService.isHisEnable(dbRecipe.getClinicOrgan());
            //是否支持医院取药 true：支持
            //该医院不对接HIS的话，则不需要进行该校验
           if (flag && hisStatus) {
                String backInfo = recipeService.searchRecipeStatusFromHis(recipeId, 1);
                if (StringUtils.isNotEmpty(backInfo)) {
                    result.setCode(RecipeResultBean.FAIL);
                    result.setMsg(backInfo);
                    return result;
                }
            }
        } catch (Exception e) {
            LOG.warn("order searchRecipeStatusFromHis exception. recipeId={}", recipeId, e);
        }

        //判断是否存在分布式锁
        boolean unlock = lock(recipeId);
        if (!unlock) {
            //存在锁则需要返回
            result.setCode(RecipeResultBean.FAIL);
            result.setMsg("您有正在进行中的订单");
            return result;
        } else {
            //设置默认超时时间 30s
            redisClient.setex(CacheConstant.KEY_RCP_BUSS_PURCHASE_LOCK + recipeId, 30L);
        }

        try {
            IPurchaseService purchaseService = getService(payMode);
            result = purchaseService.order(dbRecipe, extInfo);
        } catch (Exception e) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        } finally {
            //订单创建完解锁
            unLock(recipeId);
        }

        return result;
    }

    public IPurchaseService getService(Integer payMode) {
        PurchaseEnum[] list = PurchaseEnum.values();
        String serviceName = null;
        for (PurchaseEnum e : list) {
            if (e.getPayMode().equals(payMode)) {
                serviceName = e.getServiceName();
                break;
            }
        }

        IPurchaseService purchaseService = null;
        if (StringUtils.isNotEmpty(serviceName)) {
            purchaseService = AppContextHolder.getBean(serviceName, IPurchaseService.class);
        }

        return purchaseService;
    }

    /**
     * 检查处方是否已被处理
     * @param dbRecipe  处方详情
     * @param result    结果
     * @return true 已被处理
     */
    private boolean checkRecipeIsDeal(Recipe dbRecipe, RecipeResultBean result, Map<String, String> extInfo){
        Integer payMode = MapValueUtil.getInteger(extInfo, "payMode");
        if (RecipeStatusConstant.CHECK_PASS != dbRecipe.getStatus()
                || 1 == dbRecipe.getChooseFlag()) {
            result.setCode(RecipeResultBean.FAIL);
            result.setMsg("处方单已被处理");
            //判断是否已到院取药，查看 HisCallBackService *RecipesFromHis 方法处理
            if (Integer.valueOf(1).equals(dbRecipe.getPayFlag())) {
                if (RecipeBussConstant.PAYMODE_TO_HOS.equals(dbRecipe.getPayMode()) && RecipeBussConstant.PAYMODE_TFDS == payMode) {
                    result.setCode(2);
                    result.setMsg("您已到院自取药品，无法提交药店取药");
                } else if (RecipeBussConstant.PAYMODE_TO_HOS.equals(dbRecipe.getPayMode()) && RecipeBussConstant.PAYMODE_ONLINE == payMode) {
                    result.setCode(3);
                    result.setMsg("您已到院自取药品，无法进行配送");
                } else if (RecipeBussConstant.PAYMODE_ONLINE.equals(dbRecipe.getPayMode())) {
                    result.setCode(4);
                    result.setMsg(dbRecipe.getOrderCode());
                }
            }
            return true;
        }
        return false;
    }

    /**
     * 检查处方是否已被处理
     * @param dbRecipe   处方详情
     * @param result     结果
     * @return true 已被处理
     */
    private boolean checkRecipeIsUser(Recipe dbRecipe, RecipeResultBean result){
        if (RecipeStatusConstant.CHECK_PASS != dbRecipe.getStatus()
                || 1 == dbRecipe.getChooseFlag()) {
            result.setCode(RecipeResultBean.FAIL);
            result.setMsg("处方单已被处理");
            //判断是否已到院取药，查看 HisCallBackService *RecipesFromHis 方法处理
            if (Integer.valueOf(1).equals(dbRecipe.getPayFlag())) {
                if (RecipeBussConstant.PAYMODE_TO_HOS.equals(dbRecipe.getPayMode())) {
                    result.setMsg("您已到院自取药品，无法选择其他购药方式");
                }
            }
            return true;
        }
        if (RecipeStatusConstant.CHECK_PASS == dbRecipe.getStatus()) {
            Integer consultId = dbRecipe.getClinicId();
            Integer medicalFlag = 0;
            IConsultExService consultExService = ApplicationUtils.getConsultService(IConsultExService.class);
            if (consultId != null) {
                ConsultExDTO consultExDTO = consultExService.getByConsultId(consultId);
                if (consultExDTO != null) {
                    medicalFlag = consultExDTO.getMedicalFlag();
                }
            }
            RecipeDetailDAO detailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
            OrganService organService = ApplicationUtils.getBasicService(OrganService.class);
            if (RecipeBussConstant.RECIPEMODE_ZJJGPT.equals(dbRecipe.getRecipeMode()) && (RecipeExtendConstant.MEDICAL_FALG_YES == medicalFlag || dbRecipe.getChooseFlag() == 1)) {
                OrganDTO organDTO = organService.getByOrganId(dbRecipe.getClinicOrgan());
                List<Recipedetail> detailList = detailDAO.findByRecipeId(dbRecipe.getRecipeId());
                result.setCode(RecipeResultBean.FAIL);
                StringBuilder sb = new StringBuilder("您是医保病人，请到医院支付取药");
                if(CollectionUtils.isNotEmpty(detailList)){
                    String pharmNo = detailList.get(0).getPharmNo();
                    if(StringUtils.isNotEmpty(pharmNo)){
                        sb.append("医院取药窗口取药：["+ organDTO.getName() + "" + pharmNo + "取药窗口]");
                    }else {
                        sb.append("医院取药窗口取药：["+ organDTO.getName() + "取药窗口]");
                    }
                }
                result.setMsg(sb.toString());
                return true;
            }
        }
        return false;
    }

    private boolean lock(Integer recipeId) {
        return redisClient.setNX(CacheConstant.KEY_RCP_BUSS_PURCHASE_LOCK + recipeId, "true");
    }

    private boolean unLock(Integer recipeId) {
        return redisClient.setex(CacheConstant.KEY_RCP_BUSS_PURCHASE_LOCK + recipeId, 1L);
    }

}
