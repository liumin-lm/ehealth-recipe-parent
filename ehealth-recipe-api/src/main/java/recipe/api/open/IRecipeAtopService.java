package recipe.api.open;

import com.ngari.common.dto.CheckRequestCommonOrderPageDTO;
import com.ngari.common.dto.SyncOrderVO;
import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.recipe.mode.RecipeInfoTO;
import com.ngari.platform.recipe.mode.OutpatientPaymentRecipeDTO;
import com.ngari.platform.recipe.mode.QueryRecipeInfoHisDTO;
import com.ngari.recipe.hisprescription.model.RegulationRecipeIndicatorsDTO;
import com.ngari.recipe.offlinetoonline.model.FindHisRecipeDetailReqVO;
import com.ngari.recipe.recipe.model.RecipeBean;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import com.ngari.recipe.recipe.model.SymptomDTO;
import com.ngari.recipe.vo.FastRecipeVO;
import ctd.util.annotation.RpcService;
import recipe.vo.PageGenericsVO;
import recipe.vo.doctor.RecipeInfoVO;
import recipe.vo.patient.PatientOptionalDrugVo;
import recipe.vo.second.*;

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
    @RpcService(mvcDisabled = true)
    Boolean updateRecipeState(Integer recipeId, Integer processState, Integer subState);

    /**
     * 更新药师的签名状态
     * @param recipeId
     * @param checkerSignState
     * @return
     */
    @RpcService(mvcDisabled = true)
    Boolean updateCheckerSignState(Integer recipeId, Integer checkerSignState);

    /**
     * 根据his处方号和挂号序号机构查询处方
     *
     * @param recipeCode his处方号
     * @param registerId 挂号序号
     * @param organId    机构ID
     * @return 处方
     */
    @RpcService
    RecipeBean getByRecipeCodeAndRegisterIdAndOrganId(String recipeCode, String registerId, int organId);

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
     * 根据 二方id 查询处方列表全部数据
     *
     * @param clinicId
     * @param bussSource
     * @return
     */

    @RpcService(mvcDisabled = true)
    List<RecipeBean> recipeAllByClinicId(Integer clinicId, Integer bussSource);

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

    /**
     * 药师签名 只是获取药师手签更新PDF
     * @param recipeId
     */
    @RpcService(mvcDisabled = true)
    void pharmacyToRecipePDF(Integer recipeId);

    /**
     * 药师签名并进行CA操作
     * @param recipeId
     * @param checker
     */
    @RpcService(mvcDisabled = true)
    void pharmacyToRecipePDFAndCa(Integer recipeId, Integer checker);


    /**
     * 方便门诊购物车删除
     *
     * @param ids
     * @return
     */
    @RpcService(mvcDisabled = true)
    Boolean deleteClinicCartByIds(List<Integer> ids);

    /**
     * 自助机——根据mpiid,recipeStatus获取处方
     */
    @RpcService
    List<RecipeBean> findRecipeByMpiidAndrecipeStatus(String mpiid, List<Integer> recipeStatus,Integer terminalType,Integer organId);

    /**
     * 自助机——工作台大盘数据 根据时间等查询处方申请量/完成量
     */
    @RpcService
    AutomatonCountVO findRecipeCountForAutomaton(AutomatonVO automatonVO);

    /**
     * 自助机——工作台大盘数据 处方订单趋势 根据时间等查询每一天的申请量/完成量
     */
    @RpcService
    List<AutomatonCountVO> findRecipeEveryDayForAutomaton(AutomatonVO automatonVO);

    /**
     * 自助机——工作台大盘数据 根据时间等查询电子处方机构top5
     */
    @RpcService
    List<AutomatonCountVO> findRecipeTop5ForAutomaton(AutomatonVO automatonVO);



    /**
     * his支付回调
     * @param recipePayHISCallbackReq
     * @return
     */
    @RpcService
    HisResponseTO recipePayHISCallback(RecipePayHISCallbackReq recipePayHISCallbackReq);


    /**
     * 便捷购药获取药方模板
     *
     * @param id
     * @return
     */
    @RpcService
    FastRecipeVO getFastRecipeById(Integer id);

    /**
     * 根据处方id获取处方详情
     *
     * @param recipeIds
     * @return
     */
    @RpcService
    List<QueryRecipeInfoHisDTO> findRecipeByIds(List<Integer> recipeIds);

    /**
     * 门诊缴费查询 待缴费且未上传his 处方信息
     * @param organId
     * @param mpiId
     * @return
     */
    @RpcService
    List<OutpatientPaymentRecipeDTO> findOutpatientPaymentRecipes(Integer organId, String mpiId);

    /**
     * 自助机查询接口
     *
     * @param automatonVO
     * @return
     */
    @RpcService
    PageGenericsVO<AutomatonResultVO> automatonList(AutomatonVO automatonVO);

    /**
     * 自助机处方接口
     *
     * @param selfServiceMachineReqVO
     * @return
     */
    @RpcService(mvcDisabled = true)
    PageGenericsVO<List<SelfServiceMachineResVo>> findRecipeToZiZhuJi(SelfServiceMachineReqVO selfServiceMachineReqVO);

    /**
     * 根据患者id获取下线处方列表
     *
     * @param patientId
     * @param startTime
     * @param endTime
     * @return
     */
    @RpcService(mvcDisabled = true)
    List<RecipeInfoTO> patientOfflineRecipe(Integer organId, String patientId, String patientName, Date startTime, Date endTime);
    /**
     *  端 药品处方 历史数据同步使用
     * @param request
     * @return
     */
    @RpcService(mvcDisabled = true)
    CheckRequestCommonOrderPageDTO findRecipePageForCommonOrder(SyncOrderVO request);

    /**
     * 日志分析接口
     *
     * @param serviceLog
     */
    @RpcService(mvcDisabled = true)
    void serviceTimeLog(ServiceLogVO serviceLog);

    /**
     * 就医引导--根据复诊id查询复诊下的处方
     *
     * @param clinicId 复诊id
     * @return
     */
    @RpcService(mvcDisabled = true)
    List<RecipeToGuideResVO> findRecipeByClinicId(Integer clinicId);

    /**
     * 订单中心--根据处方id查询处方信息
     *
     * @param recipeId 处方id
     * @return
     */
    @RpcService(mvcDisabled = true)
    RecipeVo getRecipeByBusId(Integer recipeId);

    /**
     * 查询超时未审核的处方单（10分钟未审核定义为超时）
     *
     * @param startTime
     * @param endTime
     * @param organIds
     * @return
     */
    @RpcService(mvcDisabled = true)
    List<RecipeBean> findAuditOverTimeRecipeList(Date startTime, Date endTime, List<Integer> organIds);

    /**
     * 便捷购药处方结束通知复诊
     *
     * @param recipeId
     * @param failFlag
     */
    @RpcService(mvcDisabled = true)
    void auditRecipeNoticeRevisit(Integer recipeId, Boolean failFlag);

}
