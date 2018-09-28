package recipe.drugsenterprise;

import com.google.common.collect.Maps;
import com.ngari.base.BaseAPI;
import com.ngari.base.organ.model.OrganBean;
import com.ngari.base.organ.service.IOrganService;
import com.ngari.recipe.entity.Recipe;
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
import recipe.constant.OrderStatusConstant;
import recipe.constant.RecipeBussConstant;
import recipe.constant.RecipeMsgEnum;
import recipe.constant.RecipeStatusConstant;
import recipe.dao.RecipeDAO;
import recipe.dao.RecipeLogDAO;
import recipe.dao.RecipeOrderDAO;
import recipe.drugsenterprise.bean.StandardFinishDTO;
import recipe.drugsenterprise.bean.StandardResult;
import recipe.service.RecipeHisService;
import recipe.service.RecipeLogService;
import recipe.service.RecipeMsgService;
import recipe.service.RecipeOrderService;
import recipe.util.DateConversion;
import recipe.util.MapValueUtil;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author： 0184/yu_yun
 * @date： 2018/9/28
 * @description： 药企标准服务
 * @version： 1.0
 */
@RpcBean("distributionService")
public class StandardEnterpriseCallService {

    /** logger */
    private static final Logger LOGGER = LoggerFactory.getLogger(StandardEnterpriseCallService.class);

    @Autowired
    private RecipeDAO recipeDAO;

    @Autowired
    private RecipeOrderDAO orderDAO;

    @Autowired
    private RecipeLogDAO recipeLogDAO;

    @RpcService
    public StandardResult send(List<Map<String, Object>> list){
        LOGGER.info("send param : " + JSONUtils.toString(list));
        StandardResult result = new StandardResult();
        if(CollectionUtils.isEmpty(list)){
            result.setMsg("参数错误");
            return result;
        }



        return null;
    }

    @RpcService
    public StandardResult finish(List<StandardFinishDTO> list){
        LOGGER.info("finish param : " + JSONUtils.toString(list));
        StandardResult result = new StandardResult();
        //默认为失败
        result.setCode(StandardResult.FAIL);
        if(CollectionUtils.isEmpty(list)){
            result.setMsg("参数错误");
            return result;
        }

        for(StandardFinishDTO finishDTO : list){
            boolean isSuccess = finishDTO.getCode().equals(StandardFinishDTO.SUCCESS) ? true : false;
            //转换组织结构编码
            Integer clinicOrgan = null;
            try {
                OrganBean organ = getOrganByOrganId(finishDTO.getOrganId());
                if (null != organ) {
                    clinicOrgan = organ.getOrganId();
                }
            } catch (Exception e) {
                LOGGER.warn("finish 查询机构异常，organId={}", finishDTO.getOrganId(), e);
            } finally {
                if (null == clinicOrgan) {
                    LOGGER.warn("finish 平台未匹配到该组织机构编码，organId={}", finishDTO.getOrganId());
                    result.setMsg("平台未匹配到该组织机构编码");
                    return result;
                }
            }

            Recipe dbRecipe = recipeDAO.getByRecipeCodeAndClinicOrganWithAll(finishDTO.getRecipeCode(), clinicOrgan);
            if(null == dbRecipe){
                result.setMsg("["+finishDTO.getRecipeCode()+"]处方单不存在");
                return result;
            }

            //重复处理
            if(RecipeStatusConstant.FINISH == dbRecipe.getStatus()){
                continue;
            }

            Integer recipeId = dbRecipe.getRecipeId();
            if(isSuccess){
                Map<String, Object> attrMap = Maps.newHashMap();
                attrMap.put("giveDate", StringUtils.isEmpty(finishDTO.getDate()) ? DateTime.now().toDate() :
                        DateConversion.parseDate(finishDTO.getDate(), DateConversion.DEFAULT_DATE_TIME));
                attrMap.put("giveFlag", 1);
                attrMap.put("giveUser", finishDTO.getSender());
                attrMap.put("payFlag", 1);
                attrMap.put("payDate", DateTime.now().toDate());
                //更新处方信息
                Boolean rs = recipeDAO.updateRecipeInfoByRecipeId(recipeId, RecipeStatusConstant.FINISH, attrMap);
                if (rs) {
                    RecipeOrderService orderService = ApplicationUtils.getRecipeService(RecipeOrderService.class);
                    RecipeHisService hisService = ApplicationUtils.getRecipeService(RecipeHisService.class);

                    //完成订单，不需要检查订单有效性，就算失效的订单也直接变成已完成
                    orderService.finishOrder(dbRecipe.getOrderCode(), dbRecipe.getPayMode(), null);
                    //记录日志
                    RecipeLogService.saveRecipeLog(recipeId, dbRecipe.getStatus(),
                            RecipeStatusConstant.FINISH, "处方单配送完成,配送人：" + finishDTO.getSender());
                    //HIS消息发送
                    hisService.recipeFinish(recipeId);
                }

                //HOS处方发送完成短信
                if(RecipeBussConstant.FROMFLAG_HIS_USE == dbRecipe.getFromflag()){
                    RecipeMsgService.sendRecipeMsg(RecipeMsgEnum.RECIPE_FINISH_4HIS, dbRecipe);
                }

            } else{
                //患者未取药
                Boolean rs = recipeDAO.updateRecipeInfoByRecipeId(recipeId, RecipeStatusConstant.NO_DRUG, null);
                if (rs) {
                    RecipeOrderService orderService = ApplicationUtils.getRecipeService(RecipeOrderService.class);
                    orderService.cancelOrderByCode(dbRecipe.getOrderCode(), OrderStatusConstant.CANCEL_AUTO);
                }

                //记录日志
                RecipeLogService.saveRecipeLog(recipeId, dbRecipe.getStatus(), RecipeStatusConstant.NO_DRUG,
                        "处方单配送失败:" + finishDTO.getMsg());
            }

        }

        result.setCode(StandardResult.SUCCESS);
        return result;
    }

    @RpcService
    public StandardResult updatePrescription(List<StandardFinishDTO> list){
        LOGGER.info("finish param : " + JSONUtils.toString(list));
        StandardResult result = new StandardResult();
        //默认为失败
        result.setCode(StandardResult.FAIL);
        if(CollectionUtils.isEmpty(list)){
            result.setMsg("参数错误");
            return result;
        }

        for(StandardFinishDTO finishDTO : list){
            // 未取药
            if(StandardFinishDTO.FAIL.equals(finishDTO.getCode())){

            }
        }



        return null;
    }

    public OrganBean getOrganByOrganId(String organId) throws Exception {
        IOrganService organService = BaseAPI.getService(IOrganService.class);
        OrganBean organ = null;
        List<OrganBean> organList = organService.findByOrganizeCode(organId);
        if (CollectionUtils.isNotEmpty(organList)) {
            organ = organList.get(0);
        }

        return organ;
    }
}
