package recipe.api.open;

import com.ngari.common.mode.HisResponseTO;
import com.ngari.recipe.hisprescription.model.RegulationRecipeIndicatorsDTO;
import com.ngari.recipe.offlinetoonline.model.FindHisRecipeDetailReqVO;
import com.ngari.recipe.recipe.model.RecipeBean;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import com.ngari.recipe.recipe.model.SymptomDTO;
import com.ngari.recipe.vo.FormWorkRecipeVO;
import ctd.util.annotation.RpcService;
import recipe.vo.doctor.RecipeInfoVO;
import recipe.vo.patient.PatientOptionalDrugVo;
import recipe.vo.second.RevisitRecipeTraceVo;

import java.util.Date;
import java.util.List;

/**
 * 处方提供的服务接口
 *
 * @author zhaoh
 * @date 2021/7/19
 */
public interface IRecipeAtopService {
    /**
     * 查询是否存在药师未审核状态的处方
     *
     * @param bussSource 处方来源
     * @param clinicID   复诊ID
     * @return True存在 False不存在
     * @date 2021/7/19
     */
    @RpcService
    Boolean existUncheckRecipe(Integer bussSource, Integer clinicID);

    /**
     * 复诊处方追溯
     *
     * @param bussSource 处方来源
     * @param clinicID   复诊ID
     * @return
     */
    @RpcService
    List<RevisitRecipeTraceVo> revisitRecipeTrace(Integer bussSource, Integer clinicID);

    /**
     * 复诊处方追溯列表数据处理
     *
     * @param startTime
     * @param endTime
     * @param recipeIds
     * @param organId
     */
    @RpcService
    void handDealRevisitTraceRecipe(String startTime, String endTime, List<Integer> recipeIds, Integer organId);

    /**
     * 获取处方信息
     *
     * @param recipeId 处方id
     * @return
     */
    @RpcService
    RecipeBean getByRecipeId(Integer recipeId);

    /**
     * 根据状态和失效时间获取处方列表
     *
     * @param status      状态
     * @param invalidTime 时间
     * @return 处方列表
     */
    @RpcService
    List<RecipeBean> findRecipesByStatusAndInvalidTime(List<Integer> status, Date invalidTime);

    /**
     * 患者自选药品信息保存
     *
     * @param patientOptionalDrugVo
     */
    @RpcService
    void savePatientDrug(PatientOptionalDrugVo patientOptionalDrugVo);

    /**
     * 监管平台数据反查接口
     *
     * @param recipeId
     * @return
     */
    @RpcService(mvcDisabled = true)
    RegulationRecipeIndicatorsDTO regulationRecipe(Integer recipeId);

    /**
     * 消息推送线下转线上
     *
     * @param
     * @return
     */
    @RpcService(mvcDisabled = true)
    void offlineToOnlineForRecipe(FindHisRecipeDetailReqVO request);

    /**
     * 修改审方状态
     *
     * @param recipeId
     * @param state
     * @return
     */
    @RpcService(mvcDisabled = true)
    Boolean updateAuditState(Integer recipeId, Integer state);

    /**
     * 修改处方状态
     * @param recipeId
     * @param processState
     * @param subState
     * @return
     */
    Boolean updateRecipeState(Integer recipeId, Integer processState, Integer subState);

    /**
     * 根据his处方号和挂号序号机构查询处方
     *
     * @param recipeCode his处方号
     * @param registerId 挂号序号
     * @param organId    机构ID
     * @return 处方
     */
    @RpcService(mvcDisabled = true)
    RecipeBean getByRecipeCodeAndRegisterIdAndOrganId(String recipeCode, String registerId, int organId);


    /**
     * 获取模板
     *
     * @param mouldId
     * @return
     */
    @RpcService(mvcDisabled = true)
    FormWorkRecipeVO getFormWorkRecipeById(Integer mouldId, Integer organId);

    /**
     * 获取 中医诊断
     *
     * @param id
     * @return
     */
    @RpcService(mvcDisabled = true)
    SymptomDTO symptomId(Integer id);

    /**
     * 撤销线下处方
     *
     * @param organId
     * @param recipeCode
     * @return
     */
    @RpcService
    HisResponseTO abolishOffLineRecipe(Integer organId, List<String> recipeCode);


    /**
     * 从his更新处方信息
     *
     * @param organId
     * @param recipeCodes
     * @return
     */
    @RpcService
    HisResponseTO recipeListQuery(Integer organId, List<String> recipeCodes);

    /**
     * 根据 二方id 查询处方列表
     *
     * @param clinicId   二方业务id
     * @param bussSource 开处方来源 1问诊 2复诊(在线续方) 3网络门诊
     * @return
     */
    @RpcService(mvcDisabled = true)
    List<RecipeBean> recipeListByClinicId(Integer clinicId, Integer bussSource);

    /**
     * 通过处方ID获取处方明细
     *
     * @param recipeId
     * @return
     */
    @RpcService
    List<RecipeDetailBean> findRecipeDetailByRecipeId(Integer recipeId);


    @RpcService(mvcDisabled = true)
    List<RecipeInfoVO> findRelatedRecipeRecordByRegisterNo(Integer recipeId, Integer doctorId,
                                                           List<Integer> recipeTypeList, List<Integer> organIds);
}
