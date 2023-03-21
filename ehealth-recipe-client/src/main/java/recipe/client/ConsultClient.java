package recipe.client;

import com.alibaba.fastjson.JSON;
import com.ngari.common.mode.HisResponseTO;
import com.ngari.consult.ConsultBean;
import com.ngari.consult.common.model.ConsultExDTO;
import com.ngari.consult.common.model.ConsultRegistrationNumberResultVO;
import com.ngari.consult.common.service.IConsultExService;
import com.ngari.consult.common.service.IConsultRedisService;
import com.ngari.consult.common.service.IConsultService;
import com.ngari.consult.process.service.IRecipeOnLineConsultService;
import com.ngari.his.consult.model.BusinessLogTO;
import com.ngari.his.consult.service.IConsultHisService;
import com.ngari.his.recipe.mode.OutPatientRecordResTO;
import com.ngari.his.visit.mode.*;
import com.ngari.patient.dto.ConsultSetDTO;
import com.ngari.patient.service.ConsultSetService;
import com.ngari.recipe.dto.DoctorPermissionDTO;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeExtend;
import ctd.util.JSONUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.constant.RecipeBussConstant;
import recipe.enumerate.type.BussSourceTypeEnum;
import recipe.util.RecipeBusiThreadPool;
import recipe.util.ValidateUtil;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * 咨询相关服务
 *
 * @Author liumin
 * @Date 2022/1/10 下午2:26
 * @Description
 */
@Service
public class ConsultClient extends BaseClient {
    @Autowired
    private IConsultExService consultExService;
    @Autowired
    private IConsultHisService iConsultHisService;
    @Autowired
    private ConsultSetService consultSetService;
    @Autowired
    private IConsultRedisService iConsultRedisService;
    @Resource
    private IConsultService consultService;
    @Autowired
    private IConfigurationClient iConfigurationClient;
    @Autowired
    private IRecipeOnLineConsultService recipeOnLineConsultService;


    public void sendRecipeMsg(Integer consultId, Integer bussSource) {
        logger.info("ConsultClient sendRecipeMsg consultId={},bussSource={}", consultId, bussSource);
        if (!RecipeBussConstant.BUSS_SOURCE_WZ.equals(bussSource)) {
            return;
        }
        try {
            recipeOnLineConsultService.sendRecipeMsg(consultId, 2);
        } catch (Exception e) {
            logger.error("ConsultClient sendRecipeMsg error", e);
        }
    }

    /**
     * 类加载排序
     *
     * @return
     */
    @Override
    public Integer getSort() {
        return 14;
    }

    public ConsultRegistrationNumberResultVO getConsult(Integer consultId) {
        logger.info("ConsultClient getConsult consultId={}", consultId);
        ConsultRegistrationNumberResultVO consult = iConsultRedisService.getConsultRegistrationNumber(consultId);
        logger.info("ConsultClient getConsult consult={}", JSON.toJSONString(consult));
        return consult;
    }

    /**
     * 根据医生id获取开靶向药的权限
     *
     * @param doctorId
     * @return
     */
    public Boolean getTargetedDrugTypeRecipeRight(Integer doctorId) {
        logger.info("ConsultClient getTargetedDrugTypeRecipeRight doctorId={}", JSON.toJSONString(doctorId));
        ConsultSetDTO consultSetDTO = consultSetService.getBeanByDoctorId(doctorId);
        logger.info("ConsultClient getTargetedDrugTypeRecipeRight consultSetDTO={}", JSON.toJSONString(consultSetDTO));
        boolean targetedDrugTypeRecipeRight = null == consultSetDTO.getTargetedDrugTypeRecipeRight() ? false : consultSetDTO.getTargetedDrugTypeRecipeRight();
        return targetedDrugTypeRecipeRight;
    }

    /**
     * 根据医生id获取开抗肿瘤药物的权限
     * 返回1为抗肿瘤药物使用权限普通级
     * 返回2为抗肿瘤药物使用权限限制级
     * @param doctorId
     * @return
     */
    public List<Integer> getAntiTumorDrugLevel(Integer doctorId) {
        logger.info("ConsultClient getAntiTumorDrugLevel doctorId={}", JSON.toJSONString(doctorId));
        ConsultSetDTO consultSetDTO = consultSetService.getBeanByDoctorId(doctorId);
        logger.info("ConsultClient getAntiTumorDrugLevel consultSetDTO={}", JSON.toJSONString(consultSetDTO));
        List<Integer> flag = new ArrayList<>();
        if(consultSetDTO.getAntiTumorDrugCommonLevel()){
            flag.add(1);
        }
        if(consultSetDTO.getAntiTumorDrugRestrictLevel()){
            flag.add(2);
        }
        return flag;
    }

    /**
     * 根据医生id获取开抗肿瘤药物的权限
     * 返回1为抗肿瘤药物使用权限普通级
     * 返回2为抗肿瘤药物使用权限限制级
     * @param doctorId
     * @return
     */
    public List<Integer> getAntibioticsDrugLevelLevel(Integer doctorId) {
        logger.info("ConsultClient getAntibioticsDrugLevelLevel doctorId={}", JSON.toJSONString(doctorId));
        ConsultSetDTO consultSetDTO = consultSetService.getBeanByDoctorId(doctorId);
        logger.info("ConsultClient getAntibioticsDrugLevelLevel consultSetDTO={}", JSON.toJSONString(consultSetDTO));
        List<Integer> flag = new ArrayList<>();
        if(consultSetDTO.getAntibioticsLevel1()){
            flag.add(1);
        }
        if(consultSetDTO.getAntibioticsLevel2()){
            flag.add(2);
        }
        if(consultSetDTO.getAntibioticsLevel3()){
            flag.add(3);
        }
        return flag;
    }

    /**
     * 向门诊获取代缴费用
     *
     * @param needPaymentRecipeReqTo
     * @return
     */
    public NeedPaymentRecipeResTo getRecipePaymentFee(NeedPaymentRecipeReqTo needPaymentRecipeReqTo) {
        logger.info("ConsultClient getRecipePaymentFee needPaymentRecipeReqTo={}", JSON.toJSONString(needPaymentRecipeReqTo));
        NeedPaymentRecipeResTo response = null;
        try {
            HisResponseTO<NeedPaymentRecipeResTo> hisResponseTO = recipeHisService.getRecipePaymentFee(needPaymentRecipeReqTo);
            response = this.getResponse(hisResponseTO);
        } catch (Exception e) {
            logger.error("ConsultClient getRecipePaymentFee error ", e);
        }
        logger.info("ConsultClient getRecipePaymentFee res={}", JSON.toJSONString(response));

        return response;
    }

    /**
     * 获取门诊收费项目
     * @param recipeChargeItemCodeReqTo
     * @return
     */
    public RecipeChargeItemCodeResTo getRecipeChargeItems(RecipeChargeItemCodeReqTo recipeChargeItemCodeReqTo){
        logger.info("ConsultClient getRecipeChargeItems recipeChargeItemCodeReqTo:{}", JSON.toJSONString(recipeChargeItemCodeReqTo));
        RecipeChargeItemCodeResTo response = null;
        try {
            HisResponseTO<RecipeChargeItemCodeResTo> hisResponseTO = recipeHisService.getRecipeChargeItems(recipeChargeItemCodeReqTo);
            response = this.getResponse(hisResponseTO);
        } catch (Exception e) {
            logger.error("ConsultClient getRecipeChargeItems error ", e);
        }
        logger.info("ConsultClient getRecipeChargeItems res:{}", JSON.toJSONString(response));

        return response;
    }

    /**
     * 根据单号获取网络门诊信息
     *
     * @param clinicId 业务单号
     * @return 网络门诊信息
     */
    public ConsultExDTO getConsultExByClinicId(Integer clinicId) {
        if (ValidateUtil.integerIsEmpty(clinicId)) {
            return null;
        }
        logger.info("ConsultClient getByClinicId param clinicId:{}", clinicId);
        ConsultExDTO consultExDTO = consultExService.getByConsultId(clinicId);
        logger.info("ConsultClient getByClinicId res consultExDTO:{} ", JSONUtils.toString(consultExDTO));
        return consultExDTO;
    }

    /**
     * 获取有效门诊记录
     *
     * @param writeDrugRecipeReqTO 获取有效门诊记录请求入参
     * @return 门诊记录
     */
    public HisResponseTO<OutPatientRecordResTO> findOutPatientRecordFromHis(WriteDrugRecipeReqTO writeDrugRecipeReqTO) {
        logger.info("ConsultClient findOutPatientRecordFromHis writeDrugRecipeReqTO={}", JSON.toJSONString(writeDrugRecipeReqTO));
        HisResponseTO<OutPatientRecordResTO> hisResponseTO = new HisResponseTO<>();
        try {
            hisResponseTO = recipeHisService.findOutPatientRecordFromHis(writeDrugRecipeReqTO);
        } catch (Exception e) {
            logger.error("ConsultClient findOutPatientRecordFromHis error ", e);
        }
        return hisResponseTO;
    }

    /**
     * 处方开成功回写咨询更改处方id
     *
     * @param recipeId
     * @param clinicId
     */
    public void updateRecipeIdByConsultId(Integer recipeId, Integer clinicId) {
        consultExService.updateRecipeIdByConsultId(recipeId, clinicId);
    }

    /**
     * 获取医生权限
     *
     * @param doctorId  医生id
     * @param isDrugNum 是否有中药数量
     * @return
     */
    public DoctorPermissionDTO doctorPermissionSetting(Integer doctorId, boolean isDrugNum) {
        DoctorPermissionDTO doctorPermission = new DoctorPermissionDTO();
        ConsultSetDTO permission = consultSetService.getBeanByDoctorId(doctorId);
        logger.info("ConsultClient doctorPermissionSetting permission ={}", JSON.toJSONString(permission));
        //西药开方权
        boolean xiYaoRecipeRight = null != permission.getXiYaoRecipeRight() && permission.getXiYaoRecipeRight();
        doctorPermission.setXiYaoRecipeRight(xiYaoRecipeRight);
        //中成药开方权
        boolean zhongChengRecipeRight = null != permission.getZhongChengRecipeRight() && permission.getZhongChengRecipeRight();
        doctorPermission.setZhongChengRecipeRight(zhongChengRecipeRight);
        //中药开方权
        boolean zhongRecipeRight = null != permission.getZhongRecipeRight() && permission.getZhongRecipeRight() && isDrugNum;
        doctorPermission.setZhongRecipeRight(zhongRecipeRight);
        //膏方开方权
        boolean gaoFangRecipeRight = null != permission.getGaoFangRecipeRight() && permission.getGaoFangRecipeRight();
        doctorPermission.setGaoFangRecipeRight(gaoFangRecipeRight);
        //靶向药开方权
        boolean targetedDrugTypeRecipeRight = null != permission.getTargetedDrugTypeRecipeRight() && permission.getTargetedDrugTypeRecipeRight();
        doctorPermission.setTargetedDrugTypeRecipeRight(targetedDrugTypeRecipeRight);
        //开方权
        boolean prescription = xiYaoRecipeRight || zhongChengRecipeRight || zhongRecipeRight || gaoFangRecipeRight || targetedDrugTypeRecipeRight;
        doctorPermission.setPrescription(prescription);
        //能否开医保处方
        boolean medicalFlag = null != permission.getMedicarePrescription() && permission.getMedicarePrescription();
        doctorPermission.setMedicalFlag(medicalFlag);
        logger.info("ConsultClient doctorPermissionSetting doctorPermission ={}", JSON.toJSONString(doctorPermission));
        return doctorPermission;
    }

    /**
     * 互联互通平台 日志写入
     *
     * @param recipes
     */
    public void uploadBusinessLog(List<Recipe> recipes) {
        RecipeBusiThreadPool.execute(() ->
                recipes.forEach(recipe -> {
                    Boolean isRetrySettle = iConfigurationClient.getValueBooleanCatch(recipe.getClinicOrgan(), "recipeUploadBusinessLog", false);
                    if (!isRetrySettle) {
                        return;
                    }
                    BusinessLogTO business = new BusinessLogTO();
                    business.setOrganId(recipe.getClinicOrgan());
                    business.setYwgnmc("医疗业务协同协同公卫随访管");
                    business.setYwgndm("YLYWXT");
                    business.setYwlxmc("处方流转服务");
                    business.setYwlxdm("YLYWXT_CFLZ");
                    business.setRzmx(JSON.toJSONString(recipe));
                    logger.info("ConsultClient uploadBusinessLog recipe={}", JSON.toJSONString(business));
                    iConsultHisService.uploadBusinessLog(business);
                }));
    }


    /**
     * 设置处方默认数据
     *
     * @param recipe 处方头对象
     */
    @Override
    public void setRecipe(Recipe recipe) {
        if (ValidateUtil.integerIsEmpty(recipe.getClinicId())) {
            return;
        }
        if (!BussSourceTypeEnum.BUSSSOURCE_CONSULT.getType().equals(recipe.getBussSource())) {
            return;
        }
        ConsultBean consultBean = consultService.getById(recipe.getClinicId());
        if (null != consultBean && Integer.valueOf(1).equals(consultBean.getConsultSource())) {
            recipe.setRecipeSource(1);
        }
        ConsultRegistrationNumberResultVO consult = this.getConsult(recipe.getClinicId());
        if (null != consult) {
            recipe.setPatientID(consult.getPatientId());
        }
    }

    /**
     * 设置处方默认数据
     *
     * @param recipe 处方头对象
     */
    @Override
    public void setRecipeExt(Recipe recipe, RecipeExtend extend) {
        if (!BussSourceTypeEnum.BUSSSOURCE_CONSULT.getType().equals(recipe.getBussSource())) {
            return;
        }
        ConsultExDTO consultExDTO = this.getConsultExByClinicId(recipe.getClinicId());
        if (null != consultExDTO) {
            extend.setCardNo(consultExDTO.getCardId());
            extend.setCardType(consultExDTO.getCardType());
            extend.setRegisterID(consultExDTO.getRegisterNo());
            extend.setWeight(consultExDTO.getWeight());
        }
        ConsultRegistrationNumberResultVO consult = this.getConsult(recipe.getClinicId());
        if (null != consult) {
            extend.setRegisterID(StringUtils.isNotEmpty(consult.getRegistrationNumber()) ? consult.getRegistrationNumber() : extend.getRegisterID());
            extend.setSeries(consult.getSeries());
        }
    }
}
