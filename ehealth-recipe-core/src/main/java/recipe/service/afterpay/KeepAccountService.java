package recipe.service.afterpay;

import com.alibaba.fastjson.JSONObject;
import com.ngari.patient.dto.DepartmentDTO;
import com.ngari.patient.dto.OrganDTO;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.patient.service.BasicAPI;
import com.ngari.patient.service.OrganService;
import com.ngari.patient.service.PatientService;
import com.ngari.pay.api.dto.param.WnAccountDetail;
import com.ngari.pay.api.dto.param.WnAccountSplitParam;
import com.ngari.recipe.entity.DrugsEnterprise;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeExtend;
import com.ngari.recipe.entity.RecipeOrder;
import ctd.persistence.DAOFactory;
import ctd.util.AppContextHolder;
import ctd.util.JSONUtils;
import ctd.util.event.GlobalEventExecFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import recipe.ApplicationUtils;
import recipe.constant.RecipeFeeEnum;
import recipe.constant.RecipeSystemConstant;
import recipe.dao.DrugsEnterpriseDAO;
import recipe.dao.RecipeExtendDAO;
import recipe.easypay.IEasyPayService;
import recipe.manager.DepartManager;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 记账业务
 *
 * @author yinsheng
 * @date 2021\4\12 0012 18:36
 */
@Component("keepAccountService")
public class KeepAccountService implements IAfterPayBussService {

    private static final Logger LOGGER = LoggerFactory.getLogger(KeepAccountService.class);

    @Autowired
    private DepartManager departManager;

    /**
     * 上传记账信息
     *
     * @param order   订单信息
     * @param recipes 处方信息
     */
    public void uploadKeepAccount(RecipeOrder order, List<Recipe> recipes) {
        LOGGER.info("KeepAccountService uploadKeepAccount recipes:{}", JSONUtils.toString(recipes));
        //(异步的过程，不影响主流程)
        GlobalEventExecFactory.instance().getExecutor().submit(() -> {
            try {
                handleRecipeSplit(order, recipes);
            } catch (Exception e) {
                LOGGER.error("KeepAccountService uploadKeepAccount 支付回调处方记账业务异常，error=", e);
            }
        });
    }

    /**
     * 处方记账处理
     *
     * @param order   订单信息
     * @param recipes 处方信息
     */
    private void handleRecipeSplit(RecipeOrder order, List<Recipe> recipes) {
        WnAccountSplitParam wnSplitParam = new WnAccountSplitParam();
        // 记账基本信息
        getSplitBaseInfo(order, recipes, wnSplitParam);
        // 记账业务详情
        List<JSONObject> feeList = getSplitFeeInfo(order);
        wnSplitParam.setBusDetail(feeList);
        // 记账账户信息
        getSplitAccountInfo(order, wnSplitParam, recipes);
        LOGGER.info("支付回调支付平台记账入参={}", JSONObject.toJSONString(wnSplitParam));
        IEasyPayService easyPayService = AppContextHolder.getBean("easypay.payService", IEasyPayService.class);
        String splitResult = easyPayService.wnAccountSplitUpload(wnSplitParam);
        LOGGER.info("支付回调支付平台记账结果={}", splitResult);
    }

    /**
     * 处方分账基础信息
     *
     * @param order        订单信息
     * @param recipes      处方信息
     * @param wnSplitParam 卫宁返回参数
     */
    private void getSplitBaseInfo(RecipeOrder order, List<Recipe> recipes, WnAccountSplitParam wnSplitParam) {
        // 商户订单号
        wnSplitParam.setOutTradeNo(order.getOutTradeNo());
        Recipe recipe = recipes.get(0);
        // 交易金额 总的需要分账的金额
        BigDecimal payAmount = new BigDecimal(order.getActualPrice().toString());
        wnSplitParam.setAmount(payAmount);
        // 业务类型 5-处方
        wnSplitParam.setBusType("5");
        // 患者姓名
        PatientService patientService = BasicAPI.getService(PatientService.class);
        PatientDTO patientDTO = patientService.getPatientByMpiId(recipe.getMpiid());
        if (patientDTO != null) {
            wnSplitParam.setPatientName(patientDTO.getPatientName());
        }
        // 就诊卡号
        RecipeExtendDAO recipeExtendDAO = DAOFactory.getDAO(RecipeExtendDAO.class);
        RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipe.getRecipeId());
        if (recipeExtend != null) {
            wnSplitParam.setCardNo(recipeExtend.getCardNo());
        }
        // 问诊科室
        DepartmentDTO departmentDTO = departManager.getDepartmentByDepart(recipe.getDepart());
        if (departmentDTO != null) {
            wnSplitParam.setDepartId(departmentDTO.getCode());
        }
    }

    /**
     * 获取处方记账账户信息
     * 账户类型 平台-1、医院-2、药店/药企-3、 医生-4、 药师-5
     *
     * @param order        订单信息
     * @param wnSplitParam 卫宁返回参数
     * @param recipes      处方信息
     */
    private void getSplitAccountInfo(RecipeOrder order, WnAccountSplitParam wnSplitParam, List<Recipe> recipes) {
        Recipe recipe = recipes.get(0);
        // 医院编码
        wnSplitParam.setYydm(recipe.getClinicOrgan() + "");
        // 分账方编码
        String splitNumber = "";
        // 分账方类型
        Integer splitType = null;
        // 分账方名称
        String splitName = "";
        // getPayeeCode:0平台，1机构，2药企根据getPayeeCode获取对应角色编码、类型、名称
        // 账户类型 平台-1、医院/机构-2、药店/药企-3、 医生-4、 药师-5
        switch (order.getPayeeCode()) {
            case 0:
                splitNumber = RecipeSystemConstant.SPLIT_NO_PLATFORM;
                splitType = 1;
                splitName = RecipeSystemConstant.SPLIT_NAME_PLATFORM;
                break;
            case 1:
                OrganService organService = ApplicationUtils.getBasicService(OrganService.class);
                OrganDTO organDTO = organService.getByOrganId(recipe.getClinicOrgan());
                splitNumber = organDTO.getOrganId() + "";
                splitType = 2;
                splitName = organDTO.getName();
                break;
            case 2:
                splitNumber = order.getEnterpriseId() + "";
                splitType = 3;
                DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
                DrugsEnterprise enterprise = drugsEnterpriseDAO.getById(order.getEnterpriseId());
                if (null != enterprise) {
                    splitName = enterprise.getName();
                }
                break;
            default:
                break;
        }
        wnSplitParam.setFromName(splitName);
        wnSplitParam.setFromType(splitType + "");
        wnSplitParam.setFromNo(splitNumber);
        // 分账明细 : 处方无法确定各项金额对应的分账比例金额,所以收款方=参与方=分账方 分账金额=总支付金额
        List<WnAccountDetail> splitList = new ArrayList<>();
        WnAccountDetail splitDTO = new WnAccountDetail();
        splitDTO.setAmount(new BigDecimal(order.getActualPrice().toString()));
        splitDTO.setAccountName(splitName);
        splitDTO.setAccountNo(splitNumber);
        splitDTO.setAccountType(splitType + "");
        splitList.add(splitDTO);

        wnSplitParam.setSplitDetail(splitList);
    }

    /**
     * 获取处方记账业务详情
     * 业务详情 : type 1-药费；2-挂号费；3-审方费；4-配送 amount对应金额
     *
     * @param order 订单信息
     */
    private List<JSONObject> getSplitFeeInfo(RecipeOrder order) {
        BigDecimal payAmount = new BigDecimal(order.getActualPrice().toString());
        List<JSONObject> feeList = new ArrayList<>();
        // 审方费
        BigDecimal auditFee = order.getAuditFee();
        if (null != auditFee && auditFee.compareTo(BigDecimal.ZERO) != 0) {
            JSONObject auditDTO = new JSONObject();
            auditDTO.put("type", RecipeFeeEnum.AUDIT_FEE.getFeeType());
            auditDTO.put("amount", auditFee);
            feeList.add(auditDTO);
        }
        // 药费
        BigDecimal drugAmount = payAmount.subtract(auditFee == null ? new BigDecimal(0) : auditFee);
        JSONObject drugDTO = new JSONObject();
        drugDTO.put("type", RecipeFeeEnum.DRUG_FEE.getFeeType());
        drugDTO.put("amount", drugAmount);
        feeList.add(drugDTO);
        // 挂号费
        if (null != order.getRegisterFee() && order.getRegisterFee().compareTo(BigDecimal.ZERO) != 0) {
            JSONObject registerDTO = new JSONObject();
            registerDTO.put("type", RecipeFeeEnum.REGISTER_FEE.getFeeType());
            registerDTO.put("amount", order.getRegisterFee());
            feeList.add(registerDTO);
        }
        // 配送费
        if (null != order.getExpressFee() && order.getExpressFee().compareTo(BigDecimal.ZERO) != 0) {
            JSONObject expressDTO = new JSONObject();
            expressDTO.put("type", RecipeFeeEnum.EXPRESS_FEE.getFeeType());
            expressDTO.put("amount", order.getRegisterFee());
            feeList.add(expressDTO);
        }
        return feeList;
    }
}
