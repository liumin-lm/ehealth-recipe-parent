package recipe.drugsenterprise;

import com.ngari.base.employment.model.EmploymentBean;
import com.ngari.base.employment.service.IEmploymentService;
import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.base.PatientBaseInfo;
import com.ngari.his.recipe.mode.UpdateTakeDrugWayReqTO;
import com.ngari.patient.dto.DoctorDTO;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.patient.service.BasicAPI;
import com.ngari.patient.service.DoctorService;
import com.ngari.patient.service.OrganService;
import com.ngari.patient.service.PatientService;
import com.ngari.recipe.drugsenterprise.model.DrugsDataBean;
import com.ngari.recipe.entity.*;
import com.ngari.recipe.hisprescription.model.HospitalRecipeDTO;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import ctd.persistence.DAOFactory;
import ctd.persistence.exception.DAOException;
import ctd.util.AppContextHolder;
import ctd.util.JSONUtils;
import eh.base.constant.ErrorCode;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.ApplicationUtils;
import recipe.bean.DrugEnterpriseResult;
import recipe.constant.DrugEnterpriseConstant;
import recipe.dao.RecipeDAO;
import recipe.dao.RecipeExtendDAO;
import recipe.dao.RecipeOrderDAO;
import recipe.dao.SaleDrugListDAO;
import recipe.hisservice.RecipeToHisService;
import recipe.service.RecipeLogService;
import recipe.service.RecipeService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author yinsheng
 * @date 2019\6\6 0006 14:16
 */
public class HdVirtualdyfRemoteService extends AccessDrugEnterpriseService {

    private static final Logger LOGGER = LoggerFactory.getLogger(HdVirtualdyfRemoteService.class);

    @Override
    public void tokenUpdateImpl(DrugsEnterprise drugsEnterprise) {
        LOGGER.info("HdVirtualdyfRemoteService tokenUpdateImpl not implement.");
    }

    @Override
    public String getDrugInventory(Integer drugId, DrugsEnterprise drugsEnterprise, Integer organId) {
        return "暂不支持库存查询";
    }

    @Override
    public List<String> getDrugInventoryForApp(DrugsDataBean drugsDataBean, DrugsEnterprise drugsEnterprise, Integer flag) {
        List<String> result = new ArrayList<>();
        SaleDrugListDAO saleDrugListDAO = DAOFactory.getDAO(SaleDrugListDAO.class);
        for (RecipeDetailBean recipeDetailBean : drugsDataBean.getRecipeDetailBeans()) {
            SaleDrugList saleDrugList = saleDrugListDAO.getByDrugIdAndOrganIdAndStatus(recipeDetailBean.getDrugId(), drugsEnterprise.getId());
            if (saleDrugList != null) {
                result.add(recipeDetailBean.getDrugName());
            }
        }
        return result;
    }

    @Override
    public DrugEnterpriseResult pushRecipeInfo(List<Integer> recipeIds, DrugsEnterprise enterprise) {
        //0医院取药 1物流配送 2药店取药 3未知
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);

        Recipe recipe = recipeDAO.getByRecipeId(recipeIds.get(0));
        RecipeOrderDAO dao = DAOFactory.getDAO(RecipeOrderDAO.class);
        RecipeOrder order = dao.getByOrderCode(recipe.getOrderCode());
        //如果走杭州市医保预结算了就不走这里去做his结算了
        if (order != null && order.getPreSettletotalAmount()!=null) {
            return DrugEnterpriseResult.getSuccess();
        }
        RecipeToHisService service = AppContextHolder.getBean("recipeToHisService", RecipeToHisService.class);
        OrganService organService = BasicAPI.getService(OrganService.class);
        DoctorService doctorService = BasicAPI.getService(DoctorService.class);
        PatientService patientService = BasicAPI.getService(PatientService.class);
        PatientDTO patient = patientService.get(recipe.getMpiid());
        if (patient == null){
            throw new DAOException(ErrorCode.SERVICE_ERROR, "平台查询不到患者信息");
        }
        //患者信息
        PatientBaseInfo patientBaseInfo = new PatientBaseInfo();
        patientBaseInfo.setCertificateType(patient.getCertificateType());
        patientBaseInfo.setCertificate(patient.getCertificate());
        patientBaseInfo.setPatientName(patient.getPatientName());
        patientBaseInfo.setPatientID(recipe.getPatientID());

        UpdateTakeDrugWayReqTO updateTakeDrugWayReqTO = new UpdateTakeDrugWayReqTO();
        updateTakeDrugWayReqTO.setPatientBaseInfo(patientBaseInfo);
        updateTakeDrugWayReqTO.setClinicOrgan(recipe.getClinicOrgan());
        //医院处方号
        updateTakeDrugWayReqTO.setRecipeID(recipe.getRecipeCode());
        updateTakeDrugWayReqTO.setNgarRecipeId(String.valueOf(recipe.getRecipeId()));
        updateTakeDrugWayReqTO.setOrganID(organService.getOrganizeCodeByOrganId(recipe.getClinicOrgan()));
        //审方药师工号和姓名
        if (null != recipe.getChecker()){
            IEmploymentService iEmploymentService = ApplicationUtils.getBaseService(IEmploymentService.class);
            EmploymentBean primaryEmp = iEmploymentService.getPrimaryEmpByDoctorId(recipe.getChecker());
            if (primaryEmp != null){
                updateTakeDrugWayReqTO.setCheckerId(primaryEmp.getJobNumber());
            }
            DoctorDTO doctorDTO = doctorService.getByDoctorId(recipe.getChecker());
            if (doctorDTO!=null){
                updateTakeDrugWayReqTO.setCheckerName(doctorDTO.getName());
            }
        }
        //处方总金额
        updateTakeDrugWayReqTO.setPayment(recipe.getActualPrice());
        //支付状态
        updateTakeDrugWayReqTO.setPayFlag(recipe.getPayFlag());
        //支付方式
        updateTakeDrugWayReqTO.setPayMode("1");
        if (recipe.getPayFlag() ==1){
            if (StringUtils.isNotEmpty(recipe.getOrderCode())){
                if (order!=null){
                    //收货人
                    updateTakeDrugWayReqTO.setConsignee(order.getReceiver());
                    //联系电话
                    updateTakeDrugWayReqTO.setContactTel(order.getRecMobile());
                    //收货地址
                    CommonRemoteService commonRemoteService = AppContextHolder.getBean("commonRemoteService", CommonRemoteService.class);
                    updateTakeDrugWayReqTO.setAddress(commonRemoteService.getCompleteAddress(order));
                }
            }
        }
        if (recipe.getClinicId() != null) {
            updateTakeDrugWayReqTO.setClinicID(recipe.getClinicId().toString());
        }
        //流转到这里来的属于物流配送，市三无药店取药
        updateTakeDrugWayReqTO.setDeliveryType("1");
        LOGGER.info("华东虚拟药企-取药方式更新通知his. req={}",JSONUtils.toString(updateTakeDrugWayReqTO));
        HisResponseTO hisResult = service.updateTakeDrugWay(updateTakeDrugWayReqTO);
        LOGGER.info("华东虚拟药企-取药方式更新通知his. res={}",JSONUtils.toString(hisResult));
        if (hisResult!=null && !("200".equals(hisResult.getMsgCode()))){
            //推送不成功就退款
            //退款
            RecipeService recipeService = ApplicationUtils.getRecipeService(RecipeService.class);
            recipeService.wxPayRefundForRecipe(1, recipe.getRecipeId(), null);
            RecipeLogService.saveRecipeLog(recipe.getRecipeId(), recipe.getStatus(), recipe.getStatus(), "更新取药方式失败，his返回原因：" + hisResult.getMsg());

        }
        return DrugEnterpriseResult.getSuccess();
    }

    @Override
    public DrugEnterpriseResult pushRecipe(HospitalRecipeDTO hospitalRecipeDTO, DrugsEnterprise enterprise) {
        return DrugEnterpriseResult.getSuccess();
    }

    @Override
    public DrugEnterpriseResult scanStock(Integer recipeId, DrugsEnterprise drugsEnterprise) {
        return DrugEnterpriseResult.getSuccess();
    }

    @Override
    public DrugEnterpriseResult syncEnterpriseDrug(DrugsEnterprise drugsEnterprise, List<Integer> drugIdList) {
        return DrugEnterpriseResult.getSuccess();
    }

    @Override
    public DrugEnterpriseResult pushCheckResult(Integer recipeId, Integer checkFlag, DrugsEnterprise enterprise) {
        return DrugEnterpriseResult.getSuccess();
    }

    @Override
    public DrugEnterpriseResult findSupportDep(List<Integer> recipeIds, Map ext, DrugsEnterprise enterprise) {
        return DrugEnterpriseResult.getSuccess();
    }

    @Override
    public String getDrugEnterpriseCallSys() {
        return DrugEnterpriseConstant.COMPANY_HDVIRTUALDYF;
    }

}
