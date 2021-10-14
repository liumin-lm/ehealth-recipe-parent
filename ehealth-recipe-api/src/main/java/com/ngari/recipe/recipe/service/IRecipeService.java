package com.ngari.recipe.recipe.service;

import com.ngari.common.dto.DepartChargeReportResult;
import com.ngari.common.dto.HosBusFundsReportResult;
import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.ca.model.CaSealRequestTO;
import com.ngari.his.recipe.mode.weijianwei.DrugDetailResult;
import com.ngari.his.recipe.mode.weijianwei.DrugInfoReq;
import com.ngari.his.regulation.entity.RegulationRecipeIndicatorsReq;
import com.ngari.platform.ca.mode.CaSignResultTo;
import com.ngari.platform.recipe.mode.ReadjustDrugDTO;
import com.ngari.platform.recipe.mode.RecipeStatusReqTO;
import com.ngari.recipe.IBaseService;
import com.ngari.recipe.ca.CaSignResultUpgradeBean;
import com.ngari.recipe.common.RecipeBussReqTO;
import com.ngari.recipe.common.RecipeListReqTO;
import com.ngari.recipe.common.RecipeListResTO;
import com.ngari.recipe.drugsenterprise.model.DrugsEnterpriseBean;
import com.ngari.recipe.drugsenterprise.model.StandardResultBean;
import com.ngari.recipe.drugsenterprise.model.ThirdResultBean;
import com.ngari.recipe.hisprescription.model.SyncEinvoiceNumberDTO;
import com.ngari.recipe.recipe.model.*;
import com.ngari.recipe.recipeorder.model.RecipeOrderBean;
import com.ngari.recipe.recipereportform.model.*;
import ctd.persistence.bean.QueryResult;
import ctd.util.annotation.RpcService;

import java.math.BigDecimal;
import java.util.*;

/**
 * @company: ngarihealth
 * @author: 0184/yu_yun
 * @date:2017/7/31.
 */
public interface IRecipeService extends IBaseService<RecipeBean> {

    /**
     * HIS回调BASE接口，处方写入成功消息
     *
     * @param request HIS返回数据
     */
    @RpcService
    void sendSuccess(RecipeBussReqTO request);

    /**
     * HIS回调BASE接口，处方写入失败消息
     * <p>
     * 判断医生是否具有开处方权限
     *
     * @param request HIS返回数据
     */
    @RpcService
    void sendFail(RecipeBussReqTO request);

    /**
     * @param doctorId 医生ID
     * @return boolean true:有开处方权限
     */
    @RpcService
    boolean haveRecipeAuthority(int doctorId);

    /**
     * 医保支付完成后处理
     *
     * @param recipeId 处方ID
     * @param success  是否支付成功 true:支付成功
     */
    @RpcService
    void afterMedicalInsurancePay(int recipeId, boolean success);

    /**
     * 获取医生ID按照开方数量从多到少排序
     *
     * @param request 查询参数
     * @return RecipeListResTO<List < Integer>> 医生ID集合
     */
    @RpcService
    RecipeListResTO<Integer> findDoctorIdSortByCount(RecipeListReqTO request);

    /**
     * 更新处方状态
     *
     * @param recipeId    处方ID
     * @param afterStatus 变更后状态 (参考RecipeStatusConstant)
     * @return boolean true:完成更新
     */
    @RpcService
    boolean changeRecipeStatus(int recipeId, int afterStatus);

    /**
     * 更新处方信息
     *
     * @param recipeId    处方ID
     * @param afterStatus 变更后状态 (参考RecipeStatusConstant)
     * @param changeAttr  需要级联修改的其他字段
     * @return
     */
    @RpcService
    boolean updateRecipeInfoByRecipeIdAndAfterStatus(int recipeId, int afterStatus, Map<String, Object> changeAttr);

    /**
     * 根据第三方传的状态更新处方相关信息,用于订单在第三方生成模式
     * @param recipeStatusReqTO
     * @return
     */
    @RpcService
    boolean updateRecipeInfoForThirdOrder(RecipeStatusReqTO recipeStatusReqTO);

    /**
     * 获取处方信息
     *
     * @param recipeId 处方ID
     * @return RecipeBean 处方信息
     */
    @RpcService
    RecipeBean getByRecipeId(int recipeId);

    /**
     * 获取待审核的处方数量
     *
     * @param doctorId
     * @return
     */
    @RpcService
    long getUncheckedRecipeNum(int doctorId);

    /**
     * 根据HIS处方编号和机构编号获取处方信息
     *
     * @param recipeCode HIS处方编号(医院内部处方号)
     * @param organId    机构编码
     * @return 处方信息
     */
    @RpcService
    RecipeBean getByRecipeCodeAndClinicOrgan(String recipeCode, int organId);

    /**
     * 当患者mpiId更新后，同步更新处方表该患者的信息
     *
     * @param newMpiId 新值
     * @param oldMpiId 旧值
     */
    @RpcService
    void changePatientMpiId(String newMpiId, String oldMpiId);

    /**
     * 患者端 获取最新处方数据用于展示跑马灯效果
     *
     * @param request 查询条件
     *                organIds 查询机构范围
     * @return RecipeBussResTO<List < RecipeRollingInfoBean> 最新处方数据
     */
    @RpcService
    RecipeListResTO<RecipeRollingInfoBean> findLastesRecipeList(RecipeListReqTO request);

    /**
     * 运营平台使用
     * 查询处方，详情，审核信息
     *
     * @param recipeId 处方ID
     * @return Map<String, Object> 信息
     */
    @RpcService
    Map<String, Object> findRecipeAndDetailsAndCheckById(int recipeId);

    /**
     * 运营平台使用
     * 查询处方详细信息
     * @param organId
     * @param status
     * @param doctor
     * @param mpiid
     * @param bDate
     * @param eDate
     * @param dateType
     * @param depart
     * @param start
     * @param limit
     * @param organIds
     * @param giveMode
     * @param refundNodeStatus efundNodeStatus 退款节点、退款状态
     *                         0-待审核；1-审核通过，退费成功；2-审核通过，退费失败；3-审核不通过
     *                         参见
     * @return
     */
    @RpcService
    QueryResult<Map> findRecipesByInfo(Integer organId, Integer status, Integer doctor, String mpiid,
                                       Date bDate, Date eDate, Integer dateType,
                                       Integer depart, int start, int limit, List<Integer> organIds,
                                       Integer giveMode, Integer sendType, Integer fromflag,
                                       Integer recipeId, Integer enterpriseId, Integer checkStatus,
                                       Integer payFlag, Integer orderType, Integer refundNodeStatus,Integer recipeType);

    /**
     * 运营平台使用
     * 处方查询
     *
     * @param recipesQueryVO
     * @return
     */
    @RpcService
    QueryResult<Map> findRecipesByInfo2(RecipesQueryVO recipesQueryVO);


    /**
     * 运营平台使用
     *
     * @param organId
     * @param status
     * @param doctor
     * @param mpiid
     * @param bDate
     * @param eDate
     * @param dateType
     * @param depart
     * @param start
     * @param limit
     * @param organIds
     * @param giveMode
     * @return
     */
    @RpcService
    Map<String, Integer> getStatisticsByStatus(Integer organId,
                                               Integer status, Integer doctor, String mpiid,
                                               Date bDate, Date eDate, Integer dateType,
                                               Integer depart, int start, int limit, List<Integer> organIds, Integer giveMode, Integer fromflag, Integer recipeId);

    /**
     * 运营平台使用 根据电话号查询处方单
     *
     * @param mpis
     * @return
     */
    @RpcService
    List<Map> queryRecipesByMobile(List<String> mpis);

    /**
     * 运营平台使用 根据处方单状态查询医生列表
     *
     * @param status
     * @return
     */
    @RpcService
    List<Integer> findDoctorIdsByRecipeStatus(Integer status);

    /**
     * 运营平台使用 传入患者mpiid列表和机构id列表 查询符合条件的患者mpiid
     *
     * @param mpiIds
     * @param organIds
     * @return
     */
    @RpcService
    List<String> findPatientMpiIdForOp(List<String> mpiIds, List<Integer> organIds);

    /**
     * 获取常用诊断
     *
     * @param doctorId
     * @param organId
     * @return
     */
    @RpcService
    List<String> findCommonDiseasByDoctorAndOrganId(int doctorId, int organId);

    /**
     * 就诊人改造：获取医生的历史患者MpiId：从处方表中查询
     *
     * @param doctorId
     * @param start
     * @param limit
     * @return
     */
    @RpcService
    List<String> findHistoryMpiIdsByDoctorId(int doctorId, Integer start, Integer limit);

    /**
     * 同步患者注销状态到处方表
     *
     * @param mpiId
     */
    @RpcService
    void synPatientStatusToRecipe(String mpiId);

    /**
     * 从缴费记录中保存电子处方数据
     *
     * @param recipeBean
     * @param recipeDetailBeans
     */
    @RpcService
    void saveRecipeDataFromPayment(RecipeBean recipeBean, List<RecipeDetailBean> recipeDetailBeans);

    /**
     * 根据日期范围，机构归类的业务量(天，月)
     *
     * @param startDate
     * @param endDate
     * @return
     */
    @RpcService
    HashMap<Integer, Long> getCountByDateAreaGroupByOrgan(final String startDate, final String endDate);

    /**
     * 根据日期范围，机构归类的业务量(小时)
     *
     * @param startDate
     * @param endDate
     * @return
     */
    @RpcService
    HashMap<Object, Integer> getCountByHourAreaGroupByOrgan(final Date startDate, final Date endDate);

    /**
     * 处方导出Excel
     *
     * @param organId
     * @param status
     * @param doctor
     * @param patientName
     * @param bDate
     * @param eDate
     * @param dateType
     * @param depart
     * @param organIds
     * @param giveMode
     * @param fromflag
     * @return
     */
    @RpcService(timeout = 600000)
    @Deprecated
    List<Object[]> findRecipesByInfoForExcel(final Integer organId, final Integer status, final Integer doctor, final String patientName, final Date bDate, final Date eDate, final Integer dateType,
                                             final Integer depart, List<Integer> organIds, Integer giveMode, Integer fromflag, Integer recipeId, Integer enterpriseId, Integer checkStatus, Integer payFlag, Integer orderType, Integer sendType);

    /**
     * 处方导出Excel
     *
     * @param recipesQueryVO
     * @return
     */
    @RpcService(timeout = 600000)
    List<Object[]> findRecipesByInfoForExcel2(RecipesQueryVO recipesQueryVO);

    /**
     * 处方订单导出Excel
     *
     * @param organId
     * @param status
     * @param doctor
     * @param patientName
     * @param bDate
     * @param eDate
     * @param dateType
     * @param depart
     * @param giveMode
     * @param fromflag
     * @return
     */
    @RpcService(timeout = 600000)
    @Deprecated
    List<Object[]> findRecipeOrdersByInfoForExcel(Integer organId, List<Integer> organIds, Integer status, Integer doctor, String patientName, Date bDate, Date eDate, Integer dateType,
                                             Integer depart, Integer giveMode, Integer fromflag, Integer recipeId);

    /**
     * 处方订单导出Excel
     *
     * @param recipesQueryVO
     * @return
     */
    @RpcService(timeout = 600000)
    List<Object[]> findRecipeOrdersByInfoForExcel2(RecipesQueryVO recipesQueryVO);

    @RpcService
    HashMap<Integer, Long> getCountGroupByOrgan();

    @RpcService
    HashMap<Integer, Long> getRecipeRequestCountGroupByDoctor();

    @RpcService
    List<RecipeBean> findAllReadyAuditRecipe();

    @RpcService
    List<RecipeDetailBean> findRecipeDetailsByRecipeId(Integer recipeId);

    /**
     * 查询药品id 根据处方id
     *
     * @param recipeId
     * @return
     */
    @RpcService
    List<Integer> findDrugIdByRecipeId(Integer recipeId);

    @RpcService
    RecipeDetailBean getRecipeDetailByDetailId(Integer detailId);


    @RpcService
    RecipeExtendBean findRecipeExtendByRecipeId(Integer recipeId);

    @RpcService
    List<RecipeExtendBean> findRecipeExtendByRecipeIds(List<Integer> recipeIds);

    /**
     * 保存处方扩展信息
     *
     * @param recipeExtendBean
     * @return
     */
    @RpcService
    boolean saveOrUpdateRecipeExtend(RecipeExtendBean recipeExtendBean);

    @RpcService
    List<Integer> findReadyAuditRecipeIdsByOrganIds(List<Integer> organIds);

    @RpcService
    List<String> findSignFileIdByPatientId(String patientId);

    /**
     * 运营平台使用 获取可以开方的医生处理老数据
     *
     * @return
     */
    @RpcService
    List<Integer> findDoctorIdByHistoryRecipe();

    @RpcService
    RecipeBean getRecipeByOrderCode(String orderCode);

    @RpcService
    Map<String, Object> noticePlatRecipeFlowInfo(NoticePlatRecipeFlowInfoDTO req);

    /**
     * 前置机通知平台医保上传信息---杭州市互联网--暂时没用到
     *
     * @param req
     */
    @RpcService
    void noticePlatRecipeMedicalInsuranceInfo(NoticePlatRecipeMedicalInfoDTO req);

    /**
     * 获取处方类型的参数接口对像
     * 区别 中药、西药、膏方
     *
     * @param paramMapType
     * @param recipe
     * @param details
     * @param fileName
     * @return
     */
    @RpcService
    Map<String, Object> createRecipeParamMapForPDF(Integer paramMapType, RecipeBean recipe, List<RecipeDetailBean> details, String fileName);

    @RpcService
    Boolean updateRecipeInfoByRecipeId(int recipeId, final Map<String, Object> changeAttr);

    @RpcService
    Map<String, Object> getHtml5LinkInfo(PatientInfoDTO patient, RecipeBean recipeBean, List<RecipeDetailBean> recipeDetails, Integer reqType);

    @RpcService
    Map<String, String> getEnterpriseCodeByRecipeId(Integer recipeId);

    /**
     * 判断能否发起复诊申请-3天内有无待处理处方
     * 先查3天内未处理的线上处方-平台
     * 再查3天内线上未缴费的处方-到院取药推送的处方-his
     *
     * @param mpiId
     * @param depId
     * @param organId
     * @return
     */
    @RpcService
    Boolean canRequestConsultForRecipe(String mpiId, Integer depId, Integer organId);

    /**
     * 医保结算成功通知平台（结算数据回填到平台数据库）
     *
     * @param request
     * @return
     */
    @RpcService
    void recipeMedicInsurSettle(MedicInsurSettleSuccNoticNgariReqDTO request);

    @RpcService
    String getRecipeOrderCompleteAddress(RecipeOrderBean orderBean);

    @RpcService
    String getRecipeOrderCompleteAddressByRecipeId(Integer recipeId);

    /**
     * 第三方审核结果通知平台接口
     *
     * @param
     * @return
     */
    @RpcService
    Map<String, Object> noticePlatRecipeAuditResult(NoticeNgariAuditResDTO req);

    @RpcService
    long getCountByOrganAndDeptIds(Integer organId, List<Integer> deptIds, Integer plusDays);

    @RpcService
    List<Object[]> countRecipeIncomeGroupByDeptId(Date startDate, Date endDate, Integer organId);

    @RpcService
    List<RecipeBean> findByClinicId(Integer consultId);

    @RpcService
    BigDecimal getRecipeCostCountByOrganIdAndDepartIds(Integer organId, Date startDate, Date endDate, List<Integer> deptIds);

    @RpcService
    HisResponseTO syncEinvoiceNumberToPay(SyncEinvoiceNumberDTO syncEinvoiceNumberDTO);

    @RpcService
    Map<String, String> findMsgByparameters(Date startTime, Date endTime, Integer organId);

    @RpcService
    public List<RecipeBean> findByMpiId(String mpiId);

    @RpcService
    DrugsEnterpriseBean getDrugsEnterpriseBeanById(Integer depId);

    /**
     * CA异步回调接口
     *
     * @return
     */
    @RpcService
    Boolean saveSignRecipePDF(CaSignResultTo caSignResultTo);

    /**
     * 取签章pdf数据。签名原文
     *
     * @param recipeId
     * @param isDoctor
     * @return
     */
    @RpcService
    CaSealRequestTO signCreateRecipePDF(Integer recipeId, boolean isDoctor);

    /**
     * 根据机构id去标准化CA签名及签章接口
     *
     * @param organId
     */
    @RpcService
    CaSignResultBean commonCASignAndSealOrganId(CaSealRequestTO requestSealTO, RecipeBean recipe, Integer organId, String userAccount, String caPassword);

    /**
     * 为 处方 pdf 盖章
     *
     * @param recipeId
     * @param organId
     */
    @RpcService
    void generateSignetRecipePdf(Integer recipeId, Integer organId);


    /**
     * 推送处方到监管平台(审核后数据)
     * @param recipeId
     * @param status
     */
    @RpcService
    void pushRecipeToRegulation(Integer recipeId,Integer status);

    @RpcService
    ThirdResultBean readyToSend(Map<String, Object> paramMap);

    @RpcService
    ThirdResultBean toSend(Map<String, Object> paramMap);

    @RpcService
    ThirdResultBean finishRecipe(Map<String, Object> paramMap);

    @RpcService
    StandardResultBean downLoadRecipes(Map<String, Object> parames);

    @RpcService
    StandardResultBean recipeDownloadConfirmation(String appKey, List<Integer> recipeIds);

    @RpcService
    StandardResultBean synchronizeInventory(Map<String, Object> parames);

    @RpcService
    ThirdResultBean recordDrugStoreResult(Map<String, Object> paramMap);

    @RpcService
    List<StandardResultBean> readjustDrugPrice(List<ReadjustDrugDTO> readjustDrugDTOS);

    @RpcService
    Integer scanStockEnterpriseForHis(Map<String, Object> paramMap);

    /**
     * 根据审核状态查询处方列表
     *
     * @param organ 机构集合
     * @param recipeIds 处方集合
     * @param recipeTypes 处方类型集合
     * @param flag  审核标记
     * @param start 当前页
     * @param limit 页数
     * @return
     */
    @RpcService
    List<RecipeBean> findRecipeByFlag(List<Integer> organ,List<Integer> recipeIds,List<Integer> recipeTypes, int flag, int start, int limit);


    /**
     * 根据审核状态查询处方列表
     *
     * @param organ 机构集合
     * @param recipeIds 处方集合
     * @param recipeTypes 处方类型集合
     * @param flag  审核标记
     * @param start 当前页
     * @param limit 页数
     * @return
     */
    @RpcService
    Long findRecipeCountByFlag(List<Integer> organ,List<Integer> recipeIds,List<Integer> recipeTypes, int flag, int start, int limit);

    /**
     * 审核不通过后处理
     */
    @RpcService
    void doAfterCheckNotPassYs(RecipeBean recipeBean);

    /**
     * 处方药师审核通过后处理
     */
    @RpcService
    void afterCheckPassYs(Integer auditMode,RecipeBean recipeBean);

    /**
     * 处方药师审核不通过后处理
     * @param auditMode
     * @param recipeBean
     */
    @RpcService
    void afterCheckNotPassYs(Integer auditMode,RecipeBean recipeBean);

    /**
     * 获取审核处方状态
     * @param reviewType
     * @return
     */
    @RpcService
    int getAuditStatusByReviewType(int reviewType);

    /**
     * 单个处方信息推送
     * @param recipe
     * @param afterStatus
     */
    @RpcService
    void batchSendMsg(RecipeBean recipe, int afterStatus);

    /**
     * 审方处方列表全文检索查询
     * @param organs  机构id
     * @param searchFlag 检索类型
     * @param searchString  检索内容
     * @param start
     * @param limit
     * @return
     */
    @RpcService
    List<RecipeBean> searchRecipe( Set<Integer> organs, Integer searchFlag,  String searchString,
                                   Integer start,  Integer limit);

    /**
     *  审方处方列表
     * @param recipeIds
     * @param organIds
     * @return
     */
    @RpcService
    List<RecipeBean> findByRecipeAndOrganId( List<Integer> recipeIds, Set<Integer> organIds);

    /**
     * 查询药师审核平台待审核、已审核、或者所有的处方单的总条数
     * @param organ
     * @param flag  标志位
     * @return
     */
    @RpcService
    long  getRecipeCountByFlag( List<Integer> organ,  int flag);


    @RpcService
    List<EnterpriseRecipeDetailResponse> findRecipesPharmaceuticalDetailsByInfoForExcel(EnterpriseRecipeDetailExcelRequest req);

    @RpcService
    List<RecipeAccountCheckDetailResponse> findRecipesAccountCheckDetailsByInfoForExcel(RecipeAccountCheckDetailExcelRequest req);

    @RpcService
    List<RecipeHisAccountCheckResponse> recipeHisAccountCheckList(RecipeReportFormsRequest request);

    @RpcService
    Integer recipeStatusNotice(Map<String, Object> paramMap);

    @RpcService
    Map<String, Object>   findRecipeCanRefundByClinicId(Map<String,String> params);

    @RpcService
    List<RecipeRefundBean> findRefundListByRecipeId(Integer recipeId);

    /**
     * 根据审方环境模型查询待审核处方数据
     * @param checkMode
     * @return
     */
    @RpcService
    List<RecipeBean>  findReadyCheckRecipeByCheckMode(Integer checkMode);

    /**
     * 根据审方环境模型查询待审核处方数据
     * @param organIds
     * @param checkMode
     * @return
     */
    @RpcService
    List<RecipeBean>  findReadyCheckRecipeByOrganIdsCheckMode(List<Integer> organIds,Integer checkMode);

    @RpcService
    void saveRecipeInfoForBjCa(CaSignResultTo caSignResultTo);


    /**
     * 查询处方id
     *
     * @param organIds
     * @return
     */
    @RpcService
    List<Integer> queryRecipeIdByOrgan(List<Integer> organIds,List<Integer>recipeTypes,Integer  type);


    /**
     * 根据机构id 和处方类型 查询处方info
     *
     * @param organIds
     * @return
     */
    @RpcService
    List<RecipeBean> queryRecipeInfoByOrganAndRecipeType(List<Integer> organIds,List<Integer>recipeTypes);


    /**
     * 根据处方id 查询处方详情信息
     *
     * @param recipeIds
     * @return
     */
    @RpcService
    List<RecipeDetailBean> findRecipeDetailsByRecipeIds(List<Integer>recipeIds);

    /**
     * 获取parametervalue
     * @return
     */
    @RpcService
    String getRecipeParameterValue(String paramName);

    @RpcService
    public void retryCaDoctorCallBackToRecipe(CaSignResultUpgradeBean resultVo);

    @RpcService
    public void retryCaPharmacistCallBackToRecipe(CaSignResultUpgradeBean resultVo);

    /**
     * 根据处方签名
     *
     * @param status
     * @param startTime
     * @param endTime
     * @return
     */
    @RpcService
    List<RecipeBean> findRecipeListByStatusAndSignDate(int status, String startTime, String endTime);

    /**
     * 药师端签名接口
     *
     * @param recipeId
     */
    @RpcService
    public void pharmacyToRecipePDF(Integer recipeId);

    /**
     * E签宝 药师签名接口
     *
     * @param recipeId
     * @param checker
     */
    @RpcService
    void pharmacyToRecipePDF(Integer recipeId, Integer checker);


    /**
     * 退款申请结果回调
     *
     * @param refundRequestBean 请求入参
     * @return
     */
    @RpcService
    ThirdResultBean refundResultCallBack(RefundRequestBean refundRequestBean);

    @RpcService
    Boolean getDoctorApplyFlag(Integer recipeId);

    @RpcService
    Map<String, String> getPatientInfo(Integer busId);

    @RpcService
    List<String> findRecipeCodesByRecipeIds(List<Integer> recipeIds);

    /**
     * 获取该机构对应的列表跳转的类型
     * @param organId
     * @return
     */
    @RpcService
    Map<String, String> getItemSkipType(Integer organId);

    @RpcService
    String getGiveModeText(Integer recipeId);

    /**
     * 深圳二院药房工作量统计报表服务
     * @param organId 机构ID
     * @return Map<String, Object>
     * @Author dxx
     */
    @RpcService
    public Map<String, Object> workloadTop(Integer organId,Date startDate, Date endDate, String doctorName, String recipeType, Integer start, Integer limit);

    @RpcService
    public Map<String, Object> pharmacyMonthlyReport(Integer organId, String depart, String recipeType, Date startDate, Date endDate, Integer start, Integer limit);

    @RpcService
    public Map<String, Object> pharmacyTop(Integer organId, Integer drugType, Integer orderStatus, Date startDate, Date endDate, Integer order, Integer start, Integer limit);

    /**
     * 复诊查询处方状态是否有效
     * @param bussSource  业务来源（复诊）
     * @param clinicId  复诊Id
     * @param statusCode  1 类型1  2类型2
     * @return
     */
    @RpcService
    public Boolean judgeRecipeStatus(Integer bussSource,Integer clinicId,Integer statusCode);

    @RpcService
    List<RecipeBean> findToAuditPlatformRecipe();

    /**
     * ca组装参数 供casign调用
     * @param recipeBean
     * @param detailBeanList
     * @return
     */
    @RpcService
    public RegulationRecipeIndicatorsReq getCATaskRecipeReq(RecipeBean recipeBean, List<RecipeDetailBean> detailBeanList);

    /**
     * ca调用接口 供getTaskcode接口调用
     * @param recipeList
     * @param request
     */
    @RpcService
    void splicingBackRecipeDataForCaServer(List<RecipeBean> recipeList, List<RegulationRecipeIndicatorsReq> request);
    /**
     * 深圳二院财务报表  处方费用
     * @param organId
     * @param depart
     * @param createTime
     * @return
     */
    @RpcService
    public List<DepartChargeReportResult> getRecipeFeeDetail(Integer organId, Integer depart, Date createTime, Date endTime);

    /**
     * 处方 医疗费  自费+医保
     * @param organId
     * @param createTime
     * @param endTime
     * @return
     */
    @RpcService
    public  HosBusFundsReportResult getRecipeMedAndCash(Integer organId, Date createTime, Date endTime);


    @RpcService
    public void sendRecipeTagToPatientWithOfflineRecipe(String mpiId, Integer organId, String recipeCode, String cardId, Integer consultId, Integer doctorId) ;

    /**
     * 根据配置项sealDataFrom获取签章图片
     *
     * @param clinicOrgan
     * @param doctorId
     * @param checker
     * @param recipeId
     * @return
     */
    @RpcService
    public  Map<String, String> attachSealPic(Integer clinicOrgan, Integer doctorId, Integer checker, Integer recipeId);

    @RpcService
    public String getOrderCodeByRecipeCode(Integer organId, String recipeCode);

    /**
     * 查询第三方药品库存
     * @param drugInfoReq
     * @return
     */
    @RpcService
    public List<DrugDetailResult> getDrugStockForArea(DrugInfoReq drugInfoReq);

}

