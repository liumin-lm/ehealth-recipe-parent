package recipe.service;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.ngari.patient.dto.DoctorDTO;
import com.ngari.recipe.entity.*;
import com.ngari.recipe.recipe.model.RecipeBean;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import com.ngari.recipe.recipe.model.RecipeExtendBean;
import com.ngari.recipe.vo.FastRecipeDetailVO;
import com.ngari.recipe.vo.FastRecipeReq;
import com.ngari.recipe.vo.FastRecipeVO;
import ctd.persistence.exception.DAOException;
import ctd.util.BeanUtils;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.event.GlobalEventExecFactory;
import eh.cdr.api.vo.MedicalDetailBean;
import eh.utils.ValidateUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.business.BaseService;
import recipe.client.DocIndexClient;
import recipe.client.IConfigurationClient;
import recipe.client.OperationClient;
import recipe.client.SmsClient;
import recipe.constant.RecipeBussConstant;
import recipe.core.api.IFastRecipeBusinessService;
import recipe.core.api.patient.IPatientBusinessService;
import recipe.dao.*;
import recipe.enumerate.status.RecipeStatusEnum;
import recipe.enumerate.type.BussSourceTypeEnum;
import recipe.enumerate.type.RecipeDrugFormTypeEnum;
import recipe.enumerate.type.RecipeTypeEnum;
import recipe.manager.FastRecipeManager;
import recipe.service.common.RecipeSignService;
import recipe.serviceprovider.recipe.service.RemoteRecipeService;
import recipe.vo.doctor.DrugQueryVO;
import recipe.vo.doctor.RecipeInfoVO;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.stream.Collectors;

/**
 * @Description
 * @Author yzl
 * @Date 2022-08-16
 */
@RpcBean(value = "fastRecipeService")
public class FastRecipeService extends BaseService implements IFastRecipeBusinessService {

    private static final Logger logger = LoggerFactory.getLogger(FastRecipeService.class);

    @Autowired
    private FastRecipeDAO fastRecipeDAO;

    @Autowired
    private FastRecipeDetailDAO fastRecipeDetailDAO;

    @Autowired
    private IPatientBusinessService recipePatientService;

    @Autowired
    private RecipeDAO recipeDAO;

    @Autowired
    private RecipeExtendDAO recipeExtendDAO;

    @Autowired
    private RecipeDetailDAO recipeDetailDAO;

    @Autowired
    private DocIndexClient docIndexClient;

    @Autowired
    private OperationClient operationClient;

    @Autowired
    private RemoteRecipeService remoteRecipeService;

    @Autowired
    private IConfigurationClient configurationClient;

    @Autowired
    private RecipeSignService recipeSignService;

    @Autowired
    private SmsClient smsClient;

    @Resource
    private FastRecipeManager fastRecipeManager;


    /**
     * 快捷购药开方接口（包括自动开方、手动开方）
     *
     * @param recipeInfoVOList
     * @return
     */
    @Override
    public List<Integer> fastRecipeSaveRecipeList(List<RecipeInfoVO> recipeInfoVOList) {
        //快捷购药开方流程模式： "0":"自动开方流程", "1":"手动开方流程"
        Integer fastRecipeMode = configurationClient.getValueCatchReturnInteger(recipeInfoVOList.get(0).getRecipeBean().getClinicOrgan(), "fastRecipeMode", 0);
        List<Integer> resultList;
        if (Integer.valueOf("1").equals(fastRecipeMode)) {
            //医生手动开方流程，调用处方暂存接口
            List<FutureTask<Integer>> futureTasks = new LinkedList<>();
            for (RecipeInfoVO recipeInfoVO : recipeInfoVOList) {
                FutureTask<Integer> futureTask = new FutureTask<>(() -> doctorJoinFastRecipeSaveRecipe(recipeInfoVO));
                futureTasks.add(futureTask);
                GlobalEventExecFactory.instance().getExecutor().submit(futureTask);
            }
            resultList = super.futureTaskCallbackBeanList(futureTasks, 15000);
            if (CollectionUtils.isNotEmpty(resultList)) {
                logger.info("fastRecipeSaveRecipeList fastRecipeApplyToDoctor resultList={}", JSON.toJSONString(resultList));
                //通知医生，只给开方医生推送一条
                smsClient.fastRecipeApplyToDoctor(recipeInfoVOList.get(0).getRecipeBean().getClinicOrgan(),
                        recipeInfoVOList.get(0).getRecipeBean().getDoctor());
            }
        } else {
            //自动开方流程, 先开方再签名
            List<FutureTask<Integer>> futureTasks = new LinkedList<>();
            for (RecipeInfoVO recipeInfoVO : recipeInfoVOList) {
                FutureTask<Integer> futureTask = new FutureTask<>(() -> fastRecipeSaveRecipe(recipeInfoVO));
                futureTasks.add(futureTask);
                GlobalEventExecFactory.instance().getExecutor().submit(futureTask);
            }
            resultList = super.futureTaskCallbackBeanList(futureTasks, 15000);

            ExecutorService singleExecutor = Executors.newSingleThreadExecutor();
            singleExecutor.execute(() -> {
                if (CollectionUtils.isNotEmpty(resultList)) {
                    for (Integer recipeId : resultList) {
                        recipePatientService.fastRecipeCa(recipeId);
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            logger.error("fastRecipeSaveRecipeList sleep error", e);
                        }
                    }
                }
            });
        }
        return resultList;
    }

    /**
     * 手动开方：组装参数调用暂存接口，处方单会显示在对应医生的暂存列表
     *
     * @param recipeInfoVO
     * @return
     */
    private Integer doctorJoinFastRecipeSaveRecipe(RecipeInfoVO recipeInfoVO) {
        try {
            FastRecipe fastRecipe = fastRecipeDAO.get(recipeInfoVO.getMouldId());
            if (Objects.isNull(fastRecipe)) {
                return null;
            }
            //1.根据购买数量计算价格、数量等字段
            int buyNum = ValidateUtil.nullOrZeroInteger(recipeInfoVO.getBuyNum()) ? 1 : recipeInfoVO.getBuyNum();
            packageTotalParamByBuyNum(recipeInfoVO, buyNum);

            //2.recipe参数设置
            RecipeBean recipeBean = recipeInfoVO.getRecipeBean();
            recipeBean.setAuditState(0);
            recipeBean.setBussSource(BussSourceTypeEnum.BUSSSOURCE_REVISIT.getType());
            recipeBean.setFastRecipeFlag(1);
            recipeBean.setFromflag(1);
            recipeBean.setRecipeSourceType(1);
            recipeBean.setRecipeSupportGiveMode(fastRecipe.getRecipeSupportGiveMode());
            DoctorDTO doctorDTO = doctorClient.getDoctor(recipeBean.getDoctor());
            if (Objects.nonNull(doctorDTO)) {
                recipeBean.setDoctorName(doctorDTO.getName());
            }

            //剂型
            recipeBean.setRecipeDrugForm(fastRecipe.getRecipeDrugForm());
            //煎法
            recipeBean.setDecoctionNum(fastRecipe.getDecoctionNum());
            //西药嘱托
            recipeBean.setRecipeMemo(fastRecipe.getRecipeMemo());

            RecipeExtendBean recipeExtendBean = recipeInfoVO.getRecipeExtendBean();
            recipeExtendBean.setMakeMethodId(fastRecipe.getMakeMethodId());
            recipeExtendBean.setMakeMethodText(fastRecipe.getMakeMethodText());
            recipeExtendBean.setJuice(fastRecipe.getJuice());
            recipeExtendBean.setJuiceUnit(fastRecipe.getJuiceUnit());
            recipeExtendBean.setDecoctionId(fastRecipe.getDecoctionId());
            recipeExtendBean.setDecoctionText(fastRecipe.getDecoctionText());
            recipeExtendBean.setDoctorIsDecoction(fastRecipe.getDoctorIsDecoction());
            recipeExtendBean.setSingleOrCompoundRecipe(fastRecipe.getSingleOrCompoundRecipe());
            recipeExtendBean.setAppointEnterpriseType(fastRecipe.getAppointEnterpriseType());
            recipeExtendBean.setDeliveryCode(fastRecipe.getDeliveryCode());
            recipeExtendBean.setMouldId(recipeInfoVO.getMouldId());
            recipeBean.setRecipeExtend(recipeExtendBean);
            recipeExtendBean.setFastRecipeNum(buyNum);
            //3.recipe参数设置
            List<RecipeDetailBean> detailBeanList = recipeInfoVO.getRecipeDetails();
            //4.暂存
            Integer recipeId = recipeSignService.doSignRecipeSave(recipeBean, detailBeanList);
            //5.通知复诊关联处方单
            recipePatientService.updateRecipeIdByConsultId(recipeId, recipeInfoVO.getRecipeBean().getClinicId());
            return recipeId;
        } catch (Exception e) {
            logger.error("doctorJoinFastRecipeSaveRecipe error", e);
            return null;
        }
    }

    /**
     * 自动开方
     * 此处最优方案为前端组装患者信息和需要患者选择的参数，其他参数后端从药方获取，
     * 目前前端去组装的参数，但是没传全，暂时后台查询补全
     *
     * @param recipeInfoVO
     * @return
     */
    private Integer fastRecipeSaveRecipe(RecipeInfoVO recipeInfoVO) {
        try {
            FastRecipe fastRecipe = fastRecipeDAO.get(recipeInfoVO.getMouldId());
            if (Objects.isNull(fastRecipe)) {
                return null;
            }
            List<FastRecipeDetail> fastRecipeDetailList = fastRecipeDetailDAO.findFastRecipeDetailsByFastRecipeId(fastRecipe.getId());
            //1.参数设置默认值
            RecipeBean recipeBean = recipeInfoVO.getRecipeBean();
            recipeBean.setStatus(RecipeStatusEnum.RECIPE_STATUS_UNSIGNED.getType());
            recipeBean.setRecipeSourceType(0);
            recipeBean.setSignDate(new Date());
            recipeBean.setRecipeMode(RecipeBussConstant.RECIPEMODE_NGARIHEALTH);
            recipeBean.setChooseFlag(0);
            recipeBean.setGiveFlag(0);
            recipeBean.setPayFlag(0);
            recipeBean.setPushFlag(0);
            recipeBean.setRemindFlag(0);
            recipeBean.setTakeMedicine(0);
            recipeBean.setPatientStatus(1);
            recipeBean.setStatus(2);
            recipeBean.setFromflag(1);
            recipeBean.setRecipeSourceType(1);
            recipeBean.setReviewType(1);
            recipeBean.setAuditState(5);
            recipeBean.setProcessState(0);
            recipeBean.setSubState(0);
            recipeBean.setSupportMode(0);
            recipeBean.setGiveMode(2);
            recipeBean.setFastRecipeFlag(1);
            recipeBean.setRecipeDrugForm(fastRecipe.getRecipeDrugForm());
            recipeBean.setBussSource(BussSourceTypeEnum.BUSSSOURCE_REVISIT.getType());
            recipeBean.setDecoctionNum(fastRecipe.getDecoctionNum());
            recipeBean.setRecipeSupportGiveMode(fastRecipe.getRecipeSupportGiveMode());
            recipeBean.setRecipeMemo(fastRecipe.getRecipeMemo());
            DoctorDTO doctorDTO = doctorClient.getDoctor(recipeBean.getDoctor());
            if (Objects.nonNull(doctorDTO)) {
                recipeBean.setDoctorName(doctorDTO.getName());
            }
            RecipeExtendBean recipeExtendBean = recipeInfoVO.getRecipeExtendBean();
            recipeExtendBean.setMakeMethodId(fastRecipe.getMakeMethodId());
            recipeExtendBean.setMakeMethodText(fastRecipe.getMakeMethodText());
            recipeExtendBean.setJuice(fastRecipe.getJuice());
            recipeExtendBean.setJuiceUnit(fastRecipe.getJuiceUnit());
            recipeExtendBean.setDecoctionId(fastRecipe.getDecoctionId());
            recipeExtendBean.setDecoctionText(fastRecipe.getDecoctionText());
            recipeExtendBean.setSingleOrCompoundRecipe(fastRecipe.getSingleOrCompoundRecipe());
            recipeExtendBean.setAppointEnterpriseType(fastRecipe.getAppointEnterpriseType());
            recipeExtendBean.setDeliveryCode(fastRecipe.getDeliveryCode());
            recipeExtendBean.setDoctorIsDecoction(fastRecipe.getDoctorIsDecoction());
            recipeExtendBean.setMouldId(recipeInfoVO.getMouldId());

            int buyNum = ValidateUtil.nullOrZeroInteger(recipeInfoVO.getBuyNum()) ? 1 : recipeInfoVO.getBuyNum();
            recipeExtendBean.setFastRecipeNum(buyNum);
            packageTotalParamByBuyNum(recipeInfoVO, buyNum);
            Integer recipeId = recipePatientService.saveRecipe(recipeInfoVO);
            //药方扣减库存
            fastRecipeManager.decreaseStock(recipeInfoVO.getMouldId(), buyNum, recipeInfoVO.getRecipeBean().getClinicOrgan());
            recipePatientService.updateRecipeIdByConsultId(recipeId, recipeInfoVO.getRecipeBean().getClinicId());
            return recipeId;
        } catch (Exception e) {
            logger.error("fastRecipeSaveRecipe error", e);
        }
        return null;
    }


    /**
     * 便捷购药 运营平台添加药方
     *
     * @param recipeId,title
     * @param title
     * @return
     */
    @Override
    public Integer addFastRecipe(Integer recipeId, String title) {
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipeId);
        if (Objects.isNull(recipe) || Objects.isNull(recipeExtend)) {
            throw new DAOException("未找到对应处方单！");
        }
        operationClient.isAuthorisedOrgan(recipe.getClinicOrgan());
        //1.保存药方
        FastRecipe fastRecipe = new FastRecipe();
        fastRecipe.setIntroduce("");
        fastRecipe.setBackgroundImg("");
        fastRecipe.setStatus(1);
        fastRecipe.setMinNum(0);
        fastRecipe.setMaxNum(null);
        fastRecipe.setOrderNum(0);
        fastRecipe.setClinicOrgan(recipe.getClinicOrgan());
        fastRecipe.setOrganName(recipe.getOrganName());
        fastRecipe.setActualPrice(recipe.getActualPrice());
        fastRecipe.setCopyNum(recipe.getCopyNum());
        fastRecipe.setDecoctionId(recipeExtend.getDecoctionId());
        fastRecipe.setDecoctionPrice(recipeExtend.getDecoctionPrice());
        fastRecipe.setDecoctionText(recipeExtend.getDecoctionText());
        fastRecipe.setDecoctionExhibitionFlag(recipeExtend.getDecoctionExhibitionFlag());
        fastRecipe.setDecoctionNum(recipe.getDecoctionNum());
        fastRecipe.setAppointEnterpriseType(0);
        fastRecipe.setSaleNum(0);
        if (Objects.nonNull(recipe.getRecipeDrugForm())) {
            fastRecipe.setRecipeDrugForm(recipe.getRecipeDrugForm());
        }
        if (Objects.nonNull(recipeExtend.getDocIndexId())) {
            MedicalDetailBean medicalDetailBean = docIndexClient.getEmrMedicalDetail(recipeExtend.getDocIndexId());
            if (Objects.nonNull(medicalDetailBean) && CollectionUtils.isNotEmpty(medicalDetailBean.getDetailList())) {
                fastRecipe.setDocText(JSONUtils.toString(medicalDetailBean.getDetailList()));
            }
        }
        fastRecipe.setFromFlag(recipeExtend.getFromFlag());
        fastRecipe.setGiveMode(recipe.getGiveMode());
        fastRecipe.setJuice(recipeExtend.getJuice());
        fastRecipe.setJuiceUnit(recipeExtend.getJuiceUnit());
        fastRecipe.setMakeMethodId(recipeExtend.getMakeMethodId());
        fastRecipe.setMakeMethodText(recipeExtend.getMakeMethodText());
        fastRecipe.setMemo(recipe.getMemo());
        fastRecipe.setMinor(recipeExtend.getMinor());
        fastRecipe.setMinorUnit(recipeExtend.getMinorUnit());
        fastRecipe.setOfflineRecipeName(recipe.getOfflineRecipeName());
        fastRecipe.setRecipeType(recipe.getRecipeType());
        fastRecipe.setSymptomId(recipeExtend.getSymptomId());
        fastRecipe.setSymptomName(recipeExtend.getSymptomName());
        fastRecipe.setTitle(title);
        fastRecipe.setTotalMoney(recipe.getTotalMoney());
        fastRecipe.setEveryTcmNumFre(recipeExtend.getEveryTcmNumFre());
        fastRecipe.setRequirementsForTakingId(recipeExtend.getRequirementsForTakingId());
        fastRecipe.setRequirementsForTakingCode(recipeExtend.getRequirementsForTakingCode());
        fastRecipe.setRequirementsForTakingText(recipeExtend.getRequirementsForTakingText());
        fastRecipe.setDoctorIsDecoction(recipeExtend.getDoctorIsDecoction());
        fastRecipe.setNeedQuestionnaire(0);
        fastRecipe.setRecipeMemo(recipe.getRecipeMemo());
        fastRecipe.setSingleOrCompoundRecipe(recipeExtend.getSingleOrCompoundRecipe());
        FastRecipe fastRecipeResult = fastRecipeDAO.save(fastRecipe);

        //2.保存药方详情
        List<Recipedetail> recipeDetailList = recipeDetailDAO.findByRecipeId(recipeId);
        if (CollectionUtils.isNotEmpty(recipeDetailList)) {
            for (Recipedetail recipedetail : recipeDetailList) {
                if (RecipeTypeEnum.RECIPETYPE_TCM.getType().equals(recipe.getRecipeType())) {
                    recipedetail.setDrugForm(RecipeDrugFormTypeEnum.getDrugForm(recipe.getRecipeDrugForm()));
                }
                FastRecipeDetail fastRecipeDetail = BeanUtils.map(recipedetail, FastRecipeDetail.class);
                fastRecipeDetail.setFastRecipeId(fastRecipeResult.getId());
                fastRecipeDetailDAO.save(fastRecipeDetail);
            }
        }
        return fastRecipeResult.getId();
    }


    /**
     * 根据购买数量处理总价，剂量等数据
     *
     * @param recipeInfoVO
     * @param buyNum
     */
    private void packageTotalParamByBuyNum(RecipeInfoVO recipeInfoVO, int buyNum) {
        logger.info("packageTotalParamByBuyNum recipeInfoVO = {}, buyNum = [{}] ", JSON.toJSONString(recipeInfoVO), buyNum);
        if (buyNum == 1) {
            return;
        }
        //1. 处理recipe表相关字段
        RecipeBean recipeBean = recipeInfoVO.getRecipeBean();
        //中药剂数
        if (ValidateUtil.notNullAndZeroInteger(recipeBean.getCopyNum())) {
            recipeBean.setCopyNum(recipeBean.getCopyNum() * buyNum);
        }
        //处方金额
        if (Objects.nonNull(recipeBean.getTotalMoney())) {
            recipeBean.setTotalMoney(recipeBean.getTotalMoney().multiply(BigDecimal.valueOf(buyNum)));
        }
        //最后需支付费用
        if (Objects.nonNull(recipeBean.getActualPrice())) {
            recipeBean.setActualPrice(recipeBean.getActualPrice().multiply(BigDecimal.valueOf(buyNum)));
        }

        //2. 处理recipeDetail表相关字段
        List<RecipeDetailBean> recipeDetailBeanList = recipeInfoVO.getRecipeDetails();
        if (CollectionUtils.isNotEmpty(recipeDetailBeanList)) {
            for (RecipeDetailBean recipeDetailBean : recipeDetailBeanList) {
                //药物使用总数量
                if (Objects.nonNull(recipeDetailBean.getUseTotalDose())) {
                    recipeDetailBean.setUseTotalDose(recipeDetailBean.getUseTotalDose() * buyNum);
                }
                //药物发放数量
                if (Objects.nonNull(recipeDetailBean.getSendNumber())) {
                    recipeDetailBean.setSendNumber(recipeDetailBean.getSendNumber() * buyNum);
                }
                //药物使用天数
                if (Objects.nonNull(recipeDetailBean.getUseDays())) {
                    recipeDetailBean.setUseDays(recipeDetailBean.getUseDays() * buyNum);
                }
                //药物金额
                if (Objects.nonNull(recipeDetailBean.getDrugCost())) {
                    recipeDetailBean.setDrugCost(recipeDetailBean.getDrugCost().multiply(BigDecimal.valueOf(buyNum)));
                }
            }
        }

    }

    /**
     * 查询药方列表
     *
     * @param fastRecipeReq
     * @return
     */
    @Override
    public List<FastRecipe> findFastRecipeListByParam(FastRecipeReq fastRecipeReq) {
        return fastRecipeDAO.findFastRecipeListByParam(fastRecipeReq);
    }

    @Override
    public List<FastRecipeDetail> findFastRecipeDetailsByFastRecipeId(Integer fastRecipeId) {
        return fastRecipeDetailDAO.findFastRecipeDetailsByFastRecipeId(fastRecipeId);
    }

    @Override
    public Boolean simpleUpdateFastRecipe(FastRecipeVO fastRecipeVO) {
        FastRecipe fastRecipe = fastRecipeDAO.get(fastRecipeVO.getId());
        if (Objects.isNull(fastRecipe)) {
            throw new DAOException("未找到对应药方单！");
        } else {
            if (!operationClient.isAuthorisedOrgan(fastRecipe.getClinicOrgan())) {
                throw new DAOException("您没有修改该药方的权限！");
            }
            if (Objects.nonNull(fastRecipeVO.getOrderNum())) {
                fastRecipe.setOrderNum(fastRecipeVO.getOrderNum());
            }
            if (Objects.nonNull(fastRecipeVO.getMaxNum())) {
                fastRecipe.setMaxNum(fastRecipeVO.getMaxNum());
            }
            if (Objects.nonNull(fastRecipeVO.getMinNum())) {
                fastRecipe.setMinNum(fastRecipeVO.getMinNum());
            }
            if (Objects.nonNull(fastRecipeVO.getStatus())) {
                fastRecipe.setStatus(fastRecipeVO.getStatus());
            }
            fastRecipe.setStockNum(fastRecipeVO.getStockNum());

            fastRecipeDAO.update(fastRecipe);
        }
        return true;
    }

    @Override
    public Boolean fullUpdateFastRecipe(FastRecipeVO fastRecipeVO) {
        //1.更新药方 TODO:入参对象直接复制，需要单独处理的字段拎出来处理
        FastRecipe fastRecipe = fastRecipeDAO.get(fastRecipeVO.getId());
        if (Objects.isNull(fastRecipe)) {
            throw new DAOException("未找到对应药方单！");
        }
        if (!operationClient.isAuthorisedOrgan(fastRecipe.getClinicOrgan())) {
            throw new DAOException("您没有修改该药方的权限！");
        }
        List<FastRecipeDetailVO> fastRecipeDetailVOList = fastRecipeVO.getFastRecipeDetailList();
        if (CollectionUtils.isEmpty(fastRecipeDetailVOList)) {
            throw new DAOException("最少添加一种药品！");
        }

        fastRecipe.setTitle(fastRecipeVO.getTitle());
        fastRecipe.setOfflineRecipeName(fastRecipeVO.getTitle());
        fastRecipe.setBackgroundImg(fastRecipeVO.getBackgroundImg());
        fastRecipe.setIntroduce(fastRecipeVO.getIntroduce());
        fastRecipe.setNeedQuestionnaire(fastRecipeVO.getNeedQuestionnaire());
        fastRecipe.setQuestionnaireUrl(fastRecipeVO.getQuestionnaireUrl());
        fastRecipe.setRecipeSupportGiveMode(fastRecipeVO.getRecipeSupportGiveMode());
        fastRecipe.setStockNum(fastRecipeVO.getStockNum());
        if (StringUtils.isNotBlank(fastRecipeVO.getRecipeSupportGiveMode())) {
            fastRecipe.setAppointEnterpriseType(2);
        } else {
            fastRecipe.setAppointEnterpriseType(0);
        }
        fastRecipe.setDeliveryCode(fastRecipeVO.getDeliveryCode());
        BigDecimal totalMoney = new BigDecimal("0");
        for (FastRecipeDetailVO fastRecipeDetailVO : fastRecipeDetailVOList) {
            if (Objects.nonNull(fastRecipeDetailVO.getDrugCost())) {
                totalMoney = totalMoney.add(fastRecipeDetailVO.getDrugCost());
            }
        }
        //更新价格
        fastRecipe.setTotalMoney(totalMoney);
        fastRecipe.setActualPrice(totalMoney);
        fastRecipeDAO.update(fastRecipe);

        //1.更新药方详情（目前只能删除药品，修改药品随后版本做）
        List<Integer> fastRecipeDetailIds = fastRecipeDetailVOList.stream().map(FastRecipeDetailVO::getId).collect(Collectors.toList());
        List<FastRecipeDetail> fastRecipeDetailList = fastRecipeDetailDAO.findFastRecipeDetailsByFastRecipeId(fastRecipe.getId());
        if (CollectionUtils.isEmpty(fastRecipeDetailList)) {
            return true;
        }
        for (FastRecipeDetail fastRecipeDetail : fastRecipeDetailList) {
            if (!fastRecipeDetailIds.contains(fastRecipeDetail.getId())) {
                //更新为删除
                fastRecipeDetailDAO.updateStatusById(fastRecipeDetail.getId(), 0);
            }
        }
        //更新保密方标识
        if (Integer.valueOf(1).equals(fastRecipeVO.getSecrecyFlag())) {
            fastRecipeDetailDAO.updateTypeByFastRecipeId(fastRecipe.getId(), 3);
        } else {
            fastRecipeDetailDAO.updateTypeByFastRecipeId(fastRecipe.getId(), 1);
        }
        return true;
    }

    @Override
    public Map<String, Object> findRecipeAndDetailsByRecipeIdAndOrgan(Integer recipeId, Integer organId) {
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        if (Objects.nonNull(recipe) && organId.equals(recipe.getClinicOrgan())) {
            return remoteRecipeService.findRecipeAndDetailsAndCheckById(recipeId);
        } else {
            throw new DAOException("无法找到该处方单");
        }
    }

    @Override
    public List<Integer> checkFastRecipeStock(List<RecipeInfoVO> recipeInfoVOList) {
        List<Integer> recipeIdList = Lists.newArrayList();
        if (CollectionUtils.isEmpty(recipeInfoVOList)) {
            return recipeIdList;
        }

        for (RecipeInfoVO recipeInfoVO : recipeInfoVOList) {
            Integer buyNum = recipeInfoVO.getBuyNum();
            Integer mouldId = recipeInfoVO.getMouldId();
            if (Objects.isNull(buyNum) || Objects.isNull(mouldId)) {
                continue;
            }
            FastRecipe fastRecipe = fastRecipeDAO.get(mouldId);
            if (Objects.nonNull(fastRecipe) && Objects.nonNull(fastRecipe.getStockNum()) && buyNum > fastRecipe.getStockNum()) {
                recipeIdList.add(fastRecipe.getId());
            }
        }
        return recipeIdList;
    }

}

