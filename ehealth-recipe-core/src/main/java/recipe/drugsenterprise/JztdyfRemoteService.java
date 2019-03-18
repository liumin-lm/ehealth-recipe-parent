package recipe.drugsenterprise;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.ngari.patient.dto.DoctorDTO;
import com.ngari.patient.dto.EmploymentDTO;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.patient.service.*;
import com.ngari.recipe.entity.*;
import ctd.persistence.DAOFactory;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ObjectUtils;
import recipe.bean.DrugEnterpriseResult;
import recipe.constant.DrugEnterpriseConstant;
import recipe.dao.*;
import recipe.drugsenterprise.bean.JztDrugDTO;
import recipe.drugsenterprise.bean.JztRecipeDTO;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 九州通药企
 * @author yinsheng
 * @date 2019\3\14 0014 11:15
 */
public class JztdyfRemoteService extends AccessDrugEnterpriseService {

    private static final Logger LOGGER = LoggerFactory.getLogger(JztdyfRemoteService.class);

    @Override
    public void tokenUpdateImpl(DrugsEnterprise drugsEnterprise) {
        //此处待定,如果九州通提供token,我们需要在此处实现
        LOGGER.info("JztRemoteService tokenUpdateImpl not implement.");
    }

    @Override
    public DrugEnterpriseResult pushRecipeInfo(List<Integer> recipeIds, DrugsEnterprise enterprise) {
        DrugEnterpriseResult result = DrugEnterpriseResult.getSuccess();
        if (StringUtils.isEmpty(enterprise.getBusinessUrl())) {
           return getDrugEnterpriseResult(result,"药企处理业务URL为空");
        }
        if (CollectionUtils.isEmpty(recipeIds)) {
            return getDrugEnterpriseResult(result,"处方ID参数为空");
        }
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        //准备处方数据
        JztRecipeDTO jztRecipe = new JztRecipeDTO();
        Integer depId = enterprise.getId();
        String orderCode ;
        List<Recipe> recipeList = recipeDAO.findByRecipeIds(recipeIds);
        if (CollectionUtils.isNotEmpty(recipeList)) {
            OrganService organService = BasicAPI.getService(OrganService.class);
            Recipe dbRecipe = recipeList.get(0);
            //加入订单信息
            RecipeOrderDAO orderDAO = DAOFactory.getDAO(RecipeOrderDAO.class);
            RecipeOrder order = orderDAO.getByOrderCode(dbRecipe.getOrderCode());

            String organCode = organService.getOrganizeCodeByOrganId(dbRecipe.getClinicOrgan());
            if (StringUtils.isNotEmpty(organCode)) {
                jztRecipe.setClinicOrgan(organCode);
            } else {
                return getDrugEnterpriseResult(result, "机构不存在");
            }
            setJztRecipeInfo(jztRecipe, dbRecipe);
            if (!setJztRecipePatientInfo(jztRecipe, dbRecipe.getMpiid())) return getDrugEnterpriseResult(result, "患者不存在");
            if (!setJztRecipeDoctorInfo(jztRecipe, dbRecipe)) return getDrugEnterpriseResult(result, "医生或主执业点不存在");
            if (!setJztRecipeOrderInfo(order, jztRecipe, dbRecipe)) return getDrugEnterpriseResult(result, "订单不存在");
            if (!setJztRecipeDetailInfo(jztRecipe, dbRecipe.getRecipeId(), depId)) return getDrugEnterpriseResult(result, "处方详情不存在");

            orderCode = order.getOrderCode();
            //推送给九州通
            if (StringUtils.isNotEmpty(orderCode)) {

            }
        }
        return result;
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
    public DrugEnterpriseResult findSupportDep(List<Integer> recipeIds, DrugsEnterprise enterprise) {
        return DrugEnterpriseResult.getSuccess();
    }

    @Override
    public String getDrugEnterpriseCallSys() {
        return DrugEnterpriseConstant.COMPANY_JZTDYF;
    }

    /**
     * 返回调用信息
     * @param result DrugEnterpriseResult
     * @param msg     提示信息
     * @return DrugEnterpriseResult
     */
    private DrugEnterpriseResult getDrugEnterpriseResult(DrugEnterpriseResult result, String msg) {
        result.setMsg(msg);
        result.setCode(DrugEnterpriseResult.FAIL);
        return result;
    }

    /**
     * 设置九州通处方中患者信息
     * @param jztRecipe   九州通处方详情
     * @param mpiId       患者mpiId
     * @return            是否设置成功
     */
    private boolean setJztRecipePatientInfo(JztRecipeDTO jztRecipe, String mpiId) {
        PatientService patientService = BasicAPI.getService(PatientService.class);
        //加入患者信息
        PatientDTO patient = patientService.get(mpiId);
        if (ObjectUtils.isEmpty(patient)) {
            LOGGER.warn("患者不存在,mpiId:{}.", mpiId);
            return false;
        } else {
            jztRecipe.setPatientName(patient.getPatientName());
            jztRecipe.setPatientTel(patient.getMobile());
            jztRecipe.setCertificateType(patient.getCertificateType().toString());
            jztRecipe.setCertificate(patient.getCertificate());
        }
        return true;
    }

    /**
     * 设置九州通医生信息
     * @param jztRecipe  九州通处方详情
     * @param dbRecipe   处方信息
     * @return           是否设置成功
     */
    private boolean setJztRecipeDoctorInfo(JztRecipeDTO jztRecipe, Recipe dbRecipe) {
        DoctorService doctorService = BasicAPI.getService(DoctorService.class);
        EmploymentService employmentService = BasicAPI.getService(EmploymentService.class);
        //加入医生信息
        DoctorDTO doctor = doctorService.get(dbRecipe.getDoctor());
        if (!ObjectUtils.isEmpty(doctor)) {
            EmploymentDTO employment = employmentService.getPrimaryEmpByDoctorId(dbRecipe.getDoctor());
            if (null != employment) {
                jztRecipe.setDoctorName(doctor.getName());
                jztRecipe.setDoctorNumber(employment.getJobNumber());
                jztRecipe.setDepartId(employment.getDepartment().toString());
            } else {
                LOGGER.warn("医生执业点不存在,recipeId:{}.", dbRecipe.getRecipeId());
                return false;
            }
        } else {
                LOGGER.warn("医生不存在,recipeId:{}.", dbRecipe.getRecipeId());
                return false;
        }
        return true;
    }

    /**
     * 设置九州通处方信息
     * @param jztRecipe 九州通处方详情
     * @param dbRecipe  处方信息
     */
    private void setJztRecipeInfo(JztRecipeDTO jztRecipe, Recipe dbRecipe) {
        //加入处方信息
        jztRecipe.setRecipeCode(dbRecipe.getRecipeCode());
        jztRecipe.setRecipeType(dbRecipe.getRecipeType().toString());
        jztRecipe.setCreateDate(dbRecipe.getSignDate().toString());
    }

    /**
     * 设置九州通药品详情信息
     * @param jztRecipe  九州通处方详情
     * @param recipeId   处方id
     * @param depId      depId
     * @return           是否设置成功
     */
    private boolean setJztRecipeDetailInfo(JztRecipeDTO jztRecipe, Integer recipeId, Integer depId){
        RecipeDetailDAO detailDAO = DAOFactory.getDAO(RecipeDetailDAO.class);
        List<Recipedetail> detailList = detailDAO.findByRecipeId(recipeId);
        if (CollectionUtils.isNotEmpty(detailList)) {
            List<JztDrugDTO> jztDetailList = new ArrayList<>(detailList.size());
            List<Integer> drugIdList = Lists.newArrayList(Collections2.transform(detailList, new Function<Recipedetail, Integer>() {
                @Nullable
                @Override
                public Integer apply(@Nullable Recipedetail input) {
                    return input.getDrugId();
                }
            }));

            DrugListDAO drugDAO = DAOFactory.getDAO(DrugListDAO.class);
            OrganDrugListDAO organDrugDAO = DAOFactory.getDAO(OrganDrugListDAO.class);

            List<OrganDrugList> organDrugList = organDrugDAO.findByOrganIdAndDrugIds(depId, drugIdList);

            List<DrugList> drugList = drugDAO.findByDrugIds(drugIdList);
            if (detailList.size() != organDrugList.size() || organDrugList.size() != drugList.size()) {
                LOGGER.warn("药品数据存在问题,recipeId:{}.", recipeId);
                return false;
            }

            Map<Integer, OrganDrugList> organDrugMap = Maps.uniqueIndex(organDrugList, new Function<OrganDrugList, Integer>() {
                @Override
                public Integer apply(OrganDrugList input) {
                    return input.getDrugId();
                }
            });
            Map<Integer, DrugList> drugMap = Maps.uniqueIndex(drugList, new Function<DrugList, Integer>() {
                @Override
                public Integer apply(DrugList input) {
                    return input.getDrugId();
                }
            });
            JztDrugDTO jztDetail;
            Integer drugId;
            for (Recipedetail detail : detailList) {
                jztDetail = new JztDrugDTO();
                drugId = detail.getDrugId();
                jztDetail.setDrugCode(organDrugMap.get(drugId).getOrganDrugCode());
                jztDetail.setDrugName(detail.getDrugName());
                jztDetail.setSpecification(detail.getDrugSpec());
                jztDetail.setLicenseNumber(drugMap.get(drugId).getApprovalNumber());
                jztDetail.setProducer(drugMap.get(drugId).getProducer());
                jztDetail.setTotal(detail.getUseTotalDose().toString());
                jztDetail.setUseDose(detail.getUseDose().toString());
                jztDetail.setDrugFee(detail.getSalePrice().toPlainString());
                jztDetail.setDrugTotalFee(detail.getDrugCost().toPlainString());
                jztDetail.setUesDays(detail.getUseDays().toString());
                jztDetail.setUsingRate(detail.getUsingRate());
                jztDetail.setUsePathways(detail.getUsePathways());
                jztDetail.setMemo(detail.getMemo());
                jztDetail.setStandardCode(drugMap.get(drugId).getStandardCode());
                jztDetail.setDrugForm(drugMap.get(drugId).getDrugForm());
                jztDetailList.add(jztDetail);
            }
            jztRecipe.setDrugList(jztDetailList);
        } else {
            LOGGER.warn("处方详情不存在,recipeId:{}.", recipeId);
            return false;
        }
        return true;
    }

    /**
     * 设置九州通订单信息
     * @param order      订单信息
     * @param jztRecipe  九州通处方信息
     * @param dbRecipe   平台处方单
     * @return           是否设置成功
     */
    private boolean setJztRecipeOrderInfo(RecipeOrder order, JztRecipeDTO jztRecipe, Recipe dbRecipe) {
        if (ObjectUtils.isEmpty(order)) {
            LOGGER.warn("处方单不存在,recipeId:{}.", dbRecipe.getRecipeId());
            return false;
        } else {
            jztRecipe.setRecipeFee(order.getRecipeFee().toPlainString());
            jztRecipe.setActualFee(order.getActualPrice().toString());
            jztRecipe.setCouponFee(order.getCouponFee().toPlainString());
            jztRecipe.setDecoctionFee(order.getDecoctionFee().toPlainString());
            jztRecipe.setOrderTotalFee(order.getTotalFee().toPlainString());
            jztRecipe.setExpressFee(order.getExpressFee().toPlainString());
        }
        return true;
    }
}
