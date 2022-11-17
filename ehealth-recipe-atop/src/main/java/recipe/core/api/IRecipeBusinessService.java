package recipe.core.api;

import com.ngari.patient.dto.HealthCardDTO;
import com.ngari.platform.recipe.mode.OutpatientPaymentRecipeDTO;
import com.ngari.platform.recipe.mode.QueryRecipeInfoHisDTO;
import com.ngari.recipe.dto.DiseaseInfoDTO;
import com.ngari.recipe.dto.OutPatientRecipeDTO;
import com.ngari.recipe.dto.OutPatientRecordResDTO;
import com.ngari.recipe.dto.RecipeRefundDTO;
import com.ngari.recipe.entity.DoctorCommonPharmacy;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.Symptom;
import com.ngari.recipe.hisprescription.model.RegulationRecipeIndicatorsDTO;
import com.ngari.recipe.recipe.model.*;
import com.ngari.recipe.vo.*;
import recipe.enumerate.status.RecipeAuditStateEnum;
import recipe.enumerate.status.RecipeStateEnum;
import recipe.enumerate.status.SignEnum;
import recipe.vo.PageGenericsVO;
import recipe.vo.doctor.PatientOptionalDrugVO;
import recipe.vo.doctor.RecipeInfoVO;
import recipe.vo.greenroom.DrugUsageLabelResp;
import recipe.vo.greenroom.FindRecipeListForPatientVO;
import recipe.vo.greenroom.RecipeRefundInfoReqVO;
import recipe.vo.patient.PatientOptionalDrugVo;
import recipe.vo.second.*;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author yinsheng
 * @date 2021\7\16 0016 17:16
 */
public interface IRecipeBusinessService {

    /**
     * 获取线下门诊处方诊断信息
     *
     * @param patientInfoVO 患者信息
     * @return 诊断列表
     */
    List<DiseaseInfoDTO> getOutRecipeDisease(PatientInfoVO patientInfoVO);

    /**
     * 查询门诊处方信息
     *
     * @param outPatientRecipeReqVO 患者信息
     */
    List<OutPatientRecipeDTO> queryOutPatientRecipe(OutPatientRecipeReqVO outPatientRecipeReqVO);

    /**
     * 获取门诊处方详情信息
     *
     * @param outRecipeDetailReqVO 门诊处方信息
     * @return 图片或者PDF链接等
     */
    OutRecipeDetailVO queryOutRecipeDetail(OutRecipeDetailReqVO outRecipeDetailReqVO);

    /**
     * 前端获取用药指导
     *
     * @param medicationGuidanceReqVO 用药指导入参
     * @return 用药指导出参
     */
    MedicationGuideResVO getMedicationGuide(MedicationGuidanceReqVO medicationGuidanceReqVO);

    /**
     * 根据处方来源，复诊id查询未审核处方个数
     *
     * @param bussSource 处方来源
     * @param clinicId   复诊Id
     * @return True存在 False不存在
     * @date 2021/7/20
     */
    Boolean existUncheckRecipe(Integer bussSource, Integer clinicId);

    /**
     * 获取处方信息
     *
     * @param recipeId 处方id
     * @return
     */
    Recipe getByRecipeId(Integer recipeId);

    /**
     * 校验开处方单数限制
     *
     * @param clinicId 复诊id
     * @param organId  机构id
     * @param recipeId 排除的处方id
     * @return true 可开方
     */
    Boolean validateOpenRecipeNumber(Integer clinicId, Integer organId, Integer recipeId);

    /**
     * 根据状态和失效时间获取处方列表
     *
     * @param status      状态
     * @param invalidTime 时间
     * @return 处方列表
     */
    List<Recipe> findRecipesByStatusAndInvalidTime(List<Integer> status, Date invalidTime);

    /**
     * 医生端获取处方指定药品
     *
     * @param clinicId 复诊id
     * @return
     */
    List<PatientOptionalDrugVO> findPatientOptionalDrugDTO(Integer clinicId);

    /**
     * 保存患者自选药品
     *
     * @param patientOptionalDrugVo
     */
    void savePatientDrug(PatientOptionalDrugVo patientOptionalDrugVo);

    /**
     * 监管平台数据反查接口
     *
     * @param recipeId
     * @return
     */
    RegulationRecipeIndicatorsDTO regulationRecipe(Integer recipeId);


    /**
     * 获取电子病历数据
     * actionType，为查看：则先根据clinicId查电子病历，若没有则在根据docIndexId查电子病历信息，返回前端结果
     * actionType，为copy：则recipeId不为空则根据recipeId查docIndexId再调用copy接口，否则用clinicId调用查看接口返回前端结果
     *
     * @param caseHistoryVO 电子病历查询对象
     */
    MedicalDetailVO getDocIndexInfo(CaseHistoryVO caseHistoryVO);

    /**
     * 更新电子病例 开方科室医生信息
     *
     * @param caseHistoryVO
     * @return
     */
    Boolean updateDocIndexInfo(CaseHistoryVO caseHistoryVO);

    /**
     * 医生二次确认药师审核结果-不通过
     *
     * @param recipeId
     * @return
     */
    Boolean confirmAgain(Integer recipeId);

    /**
     * 修改审方状态
     *
     * @param recipeId
     * @param recipeAuditStateEnum
     * @return
     */
    Boolean updateAuditState(Integer recipeId, RecipeAuditStateEnum recipeAuditStateEnum);

    /**
     * 修改处方状态
     * @param recipeId
     * @param processState
     * @param subState
     * @return
     */
    Boolean updateRecipeState(Integer recipeId, RecipeStateEnum processState, RecipeStateEnum subState);

    /**
     * 更新药师的签名状态
     * @param recipeId
     * @param checkerSignState
     * @return
     */
    Boolean updateCheckerSignState(Integer recipeId, SignEnum checkerSignState);

    RecipeBean getByRecipeCodeAndRegisterIdAndOrganId(String recipeCode, String registerId, int organId);

    /**
     * 获取有效门诊记录
     *
     * @param mpiId    患者唯一标识
     * @param organId  机构ID
     * @param doctorId 医生ID
     * @return 门诊记录
     */
    OutPatientRecordResDTO findOutPatientRecordFromHis(String mpiId, Integer organId, Integer doctorId);

    /**
     * 获取 中医诊断
     *
     * @param id
     * @return
     */
    Symptom symptomId(Integer id);

    /**
     * his 更新处方信息
     *
     * @param organId
     * @param recipeCodes
     */
    void recipeListQuery(Integer organId, List<String> recipeCodes);

    /**
     * 暂存处方
     *
     * @param recipeBean     处方信息
     * @param detailBeanList 药品信息
     * @return 处方组号
     */
    String splitDrugRecipe(RecipeBean recipeBean, List<RecipeDetailBean> detailBeanList);

    /**
     * 查询同组处方
     *
     * @param groupCode 处方组号
     * @param type      0： 默认全部 1：查询暂存，2查询可撤销
     * @return 处方id集合
     */
    List<Integer> recipeByGroupCode(String groupCode, Integer type);

    /**
     * 根据 二方id 查询处方列表
     *
     * @param clinicId   二方业务id
     * @param bussSource 开处方来源 1问诊 2复诊(在线续方) 3网络门诊
     * @return
     */
    List<RecipeBean> recipeListByClinicId(Integer clinicId, Integer bussSource);

    /**
     * 根据 二方id 查询处方列表全部数据
     *
     * @param clinicId   二方业务id
     * @param bussSource 开处方来源 1问诊 2复诊(在线续方) 3网络门诊
     * @return
     */
    List<Recipe> recipeAllByClinicId(Integer clinicId, Integer bussSource);

    /**
     * 通过处方ID获取处方明细
     *
     * @param recipeId
     * @return
     */
    List<RecipeDetailBean> findRecipeDetailByRecipeId(Integer recipeId);

    /**
     * 根据处方单获取药品用法标签列表
     *
     * @param recipeId
     * @return
     */
    DrugUsageLabelResp queryRecipeDrugUsageLabel(Integer recipeId);

    /**
     * 获取某处方单关联处方（同一个患者同一次就诊）
     *
     * @param recipeId
     * @param doctorId
     * @return
     */
    List<RecipeInfoVO> findRelatedRecipeRecordByRegisterNo(Integer recipeId, Integer doctorId,
                                                           List<Integer> recipeTypeList, List<Integer> organIds);

    /**
     * 根据订单的维度查询药品用量标签
     * @param orderId
     * @return
     */
    List<DrugUsageLabelResp> queryRecipeDrugUsageLabelByOrder(Integer orderId);

    AdvanceWarningResVO getAdvanceWarning(AdvanceWarningReqVO advanceWarningReqDTO);

    List<HealthCardDTO> findByCardOrganAndMpiId(String mpiId);

    /**
     * 药师签名 只是获取药师手签更新PDF
     * @param recipeId
     */
    void pharmacyToRecipePDF(Integer recipeId);

    /**
     * 药师签名并进行CA操作
     * @param recipeId
     * @param checker
     */
    void pharmacyToRecipePDFAndCa(Integer recipeId, Integer checker);

    List<Map<String, Object>> findRecipeDetailsByOrderCode(String orderCode);

    List<Recipe> findRecipeByMpiidAndrecipeStatus(String mpiid, List<Integer> recipeStatus, Integer terminalType,Integer organId);

    /**
     * 获取煎法关联用药途径和用药频次
     *
     * @param organId
     * @param decoctionId
     * @return
     */
    RateAndPathwaysVO queryRateAndPathwaysByDecoctionId(Integer organId, Integer decoctionId);

    /**
     * his支付回调
     * @param recipePayHISCallbackReq
     */
    void recipePayHISCallback(RecipePayHISCallbackReq recipePayHISCallbackReq);

    @Deprecated
    DoctorCommonPharmacy findDoctorCommonPharmacyByOrganIdAndDoctorId(Integer organId, Integer doctorId);

    @Deprecated
    void saveDoctorCommonPharmacy(DoctorCommonPharmacy doctorCommonPharmacy);

    /**
     * 门诊缴费支付回调
     * @param recipeOutpatientPaymentDTO
     */
    void recipeOutpatientPaymentCallback(RecipeOutpatientPaymentDTO recipeOutpatientPaymentDTO);

    /**
     * 获取煎法关联服用要求
     *
     * @param organId
     * @param decoctionId
     * @return
     */
    List<RequirementsForTakingVO> findRequirementsForTakingByDecoctionId(Integer organId, Integer decoctionId);

    /**
     * 自助机查询接口
     *
     * @param automatonVO
     * @return
     */
    Integer automatonCount(AutomatonVO automatonVO, Recipe recipe);

    /**
     * 自助机查询接口
     *
     * @param automatonVO
     * @return
     */
    List<AutomatonResultVO> automatonList(AutomatonVO automatonVO, Recipe recipe);


    /**
     * 根据处方id 获取处方详细信息
     * @param recipeIds
     * @return
     */
    List<QueryRecipeInfoHisDTO> findRecipeByIds(List<Integer> recipeIds);

    List<RecipeRefundDTO> getRecipeRefundInfo(RecipeRefundInfoReqVO recipeRefundInfoReqVO);

    /**
     * 自助机查询处方信息
     * @param selfServiceMachineReqVO
     * @return
     */
    PageGenericsVO<List<SelfServiceMachineResVo>> findRecipeToZiZhuJi(SelfServiceMachineReqVO selfServiceMachineReqVO);

    /**
     * 门诊缴费查询 待缴费且未上传his 处方信息
     * @param organId
     * @param mpiId
     * @return
     */
    List<OutpatientPaymentRecipeDTO> findOutpatientPaymentRecipes(Integer organId, String mpiId);

    /**
     * 复诊会话消息推送
     *
     * @param recipeId
     * @param clinicId
     * @param contentType
     * @param sessionId
     * @param doctorId
     * @param mpiId
     */
    void sendMsgToMq(String recipeId, String clinicId, String contentType, String sessionId, Integer doctorId, String mpiId);

    /**
     * 患者端查询我的处方列表
     *
     * @param param
     * @return
     */
    List<PatientTabStatusMergeRecipeDTO> findRecipeListForPatientByTabStatus(FindRecipeListForPatientVO param);

    /**
     * 就医引导--根据复诊id查询复诊下的处方
     * @param clinicId
     * @return
     */
    List<RecipeToGuideResVO> findRecipeByClinicId(Integer clinicId);
}
