package recipe.audit.auditmode;

import com.ngari.home.asyn.model.BussCreateEvent;
import com.ngari.home.asyn.service.IAsynDoBussService;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.common.RecipeResultBean;
import com.ngari.recipe.entity.DrugsEnterprise;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeOrder;
import com.ngari.recipe.recipe.model.RecipeBean;
import ctd.persistence.DAOFactory;
import eh.base.constant.BussTypeConstant;
import eh.cdr.constant.RecipeStatusConstant;
import eh.wxpay.constant.PayConstant;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.ApplicationUtils;
import recipe.bean.CheckYsInfoBean;
import recipe.bussutil.RecipeUtil;
import recipe.constant.*;
import recipe.dao.DrugsEnterpriseDAO;
import recipe.dao.RecipeDAO;
import recipe.dao.RecipeOrderDAO;
import recipe.drugsenterprise.RemoteDrugEnterpriseService;
import recipe.service.*;
import recipe.util.MapValueUtil;
import recipe.util.RedisClient;

import java.util.Map;
import java.util.Set;

import static ctd.persistence.DAOFactory.getDAO;

/**
 * created by shiyuping on 2019/8/15
 * 审方后置
 */
@AuditMode(ReviewTypeConstant.Postposition_Check)
public class AuditPostMode extends AbstractAuidtMode {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuditPostMode.class);

    private RecipeService recipeService = ApplicationUtils.getRecipeService(RecipeService.class);

    private RecipeOrderDAO orderDAO = DAOFactory.getDAO(RecipeOrderDAO.class);

    @Override
    public void afterCheckPassYs(Recipe recipe) {
        recipeService.afterCheckPassYs(recipe);
    }

    @Override
    public void afterCheckNotPassYs(Recipe recipe) { recipeService.afterCheckNotPassYs(recipe);
    }

    @Override
    public void afterPayChange(Boolean saveFlag, Recipe dbRecipe, RecipeResultBean result, Map<String, Object> attrMap) {
        //默认审核通过
        Integer status = RecipeStatusConstant.CHECK_PASS;
        RecipeDAO recipeDAO = getDAO(RecipeDAO.class);
        Integer giveMode = null == MapValueUtil.getInteger(attrMap,"giveMode") ? dbRecipe.getGiveMode() : MapValueUtil.getInteger(attrMap,"giveMode");
        Integer payMode = null == MapValueUtil.getInteger(attrMap, "payMode") ? dbRecipe.getPayMode() : MapValueUtil.getInteger(attrMap,"payMode");
        Integer payFlag = MapValueUtil.getInteger(attrMap, "payFlag");
        //根据传入的方式来处理, 因为供应商列表，钥世圈提供的有可能是多种方式都支持，当时这2个值是保存为null的
        if (saveFlag) {
            attrMap.put("chooseFlag", 1);
            String memo = "";
            if (RecipeBussConstant.GIVEMODE_SEND_TO_HOME.equals(giveMode)) {
                if (RecipeBussConstant.PAYMODE_ONLINE.equals(payMode)) {
                    //线上支付
                    if (PayConstant.PAY_FLAG_PAY_SUCCESS == payFlag) {
                        //配送到家-线上支付
                        status = RecipeStatusConstant.READY_CHECK_YS;
                        // 如果处方类型是中药或膏方不需要走药师审核流程,默认状态审核通过
                        if (RecipeUtil.isTcmType(dbRecipe.getRecipeType())) {
                            status = RecipeStatusConstant.CHECK_PASS_YS;
                        }
                        memo = "配送到家-线上支付成功";
                    } else {
                        memo = "配送到家-线上支付失败";
                    }
                } else if (RecipeBussConstant.PAYMODE_MEDICAL_INSURANCE.equals(payMode)) {
                    if (dbRecipe.canMedicalPay()) {
                        //可医保支付的单子在用户看到之前已进行审核
                        status = RecipeStatusConstant.CHECK_PASS_YS;
                        memo = "医保支付成功，发送药企处方";
                    }
                } else if (RecipeBussConstant.PAYMODE_COD.equals(payMode)) {
//                    //收到userConfirm通知
//                    status = RecipeStatusConstant.READY_CHECK_YS;
//                    memo = "配送到家-货到付款成功";
                    //date 21090925
                    //货到付款添加支付成功后修改状态
                    if (PayConstant.PAY_FLAG_PAY_SUCCESS == payFlag) {
                        status = RecipeStatusConstant.READY_CHECK_YS;
                    }
                    memo = "配送到家-货到付款成功";
                }
            } else if (RecipeBussConstant.GIVEMODE_TO_HOS.equals(giveMode)) {
                //医院取药-线上支付，这块其实已经用不到了
//                status = RecipeStatusConstant.HAVE_PAY;
//                memo = "医院取药-线上支付成功";
                //date 20190925
                //添加支付成功后修改状态
                if(PayConstant.PAY_FLAG_PAY_SUCCESS == payFlag){
                    status = RecipeStatusConstant.READY_CHECK_YS;
                }
                memo = "医院取药-线上支付成功";
            } else if (RecipeBussConstant.GIVEMODE_TFDS.equals(giveMode)) {
                //收到userConfirm通知
//                status = RecipeStatusConstant.READY_CHECK_YS;
//                memo = "药店取药-到店取药成功";
                //date 20190925
                //添加支付成功后修改状态
                if(PayConstant.PAY_FLAG_PAY_SUCCESS == payFlag){
                    status = RecipeStatusConstant.READY_CHECK_YS;
                }
                memo = "药店取药-到店取药成功";
                //此处增加药店取药消息推送
                RemoteDrugEnterpriseService remoteDrugService = ApplicationUtils.getRecipeService(RemoteDrugEnterpriseService.class);
                DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
                if (dbRecipe.getEnterpriseId() == null) {
                    LOGGER.info("审方后置-药店取药-药企为空");
                } else {
                    DrugsEnterprise drugsEnterprise = drugsEnterpriseDAO.getById(dbRecipe.getEnterpriseId());
                    boolean scanFlag = remoteDrugService.scanStock(dbRecipe.getRecipeId(), drugsEnterprise);
                    if (scanFlag) {
                        //表示需要进行库存校验并且有库存
                        RecipeMsgService.sendRecipeMsg(RecipeMsgEnum.RECIPE_DRUG_HAVE_STOCK, dbRecipe);
                    } else if (drugsEnterprise.getCheckInventoryFlag() == 2) {
                        //表示无库存但是药店可备货
                        RecipeMsgService.sendRecipeMsg(RecipeMsgEnum.RECIPE_DRUG_NO_STOCK_READY, dbRecipe);
                    }
                }

            }else if (RecipeBussConstant.GIVEMODE_DOWNLOAD_RECIPE.equals(giveMode)){
                if(PayConstant.PAY_FLAG_PAY_SUCCESS == payFlag){
                    status = RecipeStatusConstant.READY_CHECK_YS;
                }
            }
            //记录日志
            RecipeLogService.saveRecipeLog(dbRecipe.getRecipeId(), RecipeStatusConstant.CHECK_PASS, status, memo);
        } else {
            attrMap.put("chooseFlag", 0);
            if (RecipeBussConstant.FROMFLAG_HIS_USE.equals(dbRecipe.getFromflag())) {
                status = dbRecipe.getStatus();
            }
        }

        super.updateRecipeInfoByRecipeId(dbRecipe.getRecipeId(),status,attrMap,result);

        if (RecipeResultBean.SUCCESS.equals(result.getCode())) {
            if (RecipeStatusConstant.READY_CHECK_YS == status) {
                RedisClient redisClient = RedisClient.instance();
                //目前只有水果湖社区医院在用
                Set<String> organIdList = redisClient.sMembers(CacheConstant.KEY_SKIP_YSCHECK_LIST);
                if (CollectionUtils.isNotEmpty(organIdList) && organIdList.contains(dbRecipe.getClinicOrgan().toString())) {
                    RecipeCheckService checkService = ApplicationUtils.getRecipeService(RecipeCheckService.class);
                    //跳过人工审核
                    CheckYsInfoBean checkResult = new CheckYsInfoBean();
                    checkResult.setRecipeId(dbRecipe.getRecipeId());
                    checkResult.setCheckDoctorId(dbRecipe.getDoctor());
                    checkResult.setCheckOrganId(dbRecipe.getClinicOrgan());
                    try {
                        checkService.autoPassForCheckYs(checkResult);
                    } catch (Exception e) {
                        LOGGER.error("updateRecipePayResultImplForOrder 药师自动审核失败. recipeId={}", dbRecipe.getRecipeId());
                        RecipeLogService.saveRecipeLog(dbRecipe.getRecipeId(), dbRecipe.getStatus(), status,
                                "updateRecipePayResultImplForOrder 药师自动审核失败:" + e.getMessage());
                    }
                } else {
                    //如果处方 在待药师审核状态 给对应机构的药师进行消息推送
                    RecipeMsgService.batchSendMsg(dbRecipe.getRecipeId(), status);
                    if (RecipeBussConstant.FROMFLAG_HIS_USE.equals(dbRecipe.getFromflag())) {
                        //进行身边医生消息推送
                        RecipeMsgService.sendRecipeMsg(RecipeMsgEnum.RECIPE_YS_READYCHECK_4HIS, dbRecipe);
                    }

                    if(RecipeBussConstant.RECIPEMODE_NGARIHEALTH.equals(dbRecipe.getRecipeMode())) {
                        //增加药师首页待处理任务---创建任务
                        Recipe recipe = recipeDAO.getByRecipeId(dbRecipe.getRecipeId());
                        RecipeBean recipeBean = ObjectCopyUtils.convert(recipe, RecipeBean.class);
                        ApplicationUtils.getBaseService(IAsynDoBussService.class).fireEvent(new BussCreateEvent(recipeBean, BussTypeConstant.RECIPE));
                    }
                }
            }
            if (RecipeStatusConstant.CHECK_PASS_YS == status) {
                //说明是可进行医保支付的单子或者是中药或膏方处方
                RemoteDrugEnterpriseService remoteDrugEnterpriseService = ApplicationUtils.getRecipeService(RemoteDrugEnterpriseService.class);
                remoteDrugEnterpriseService.pushSingleRecipeInfo(dbRecipe.getRecipeId());
            }
        }
    }

}
