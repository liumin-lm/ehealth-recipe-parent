package recipe.service;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.ngari.recipe.dto.FastRecipeReq;
import com.ngari.recipe.entity.*;
import com.ngari.recipe.recipe.model.RecipeBean;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import com.ngari.recipe.recipe.model.RecipeExtendBean;
import com.ngari.recipe.vo.FastRecipeDetailVO;
import com.ngari.recipe.vo.FastRecipeVO;
import ctd.persistence.exception.DAOException;
import ctd.util.BeanUtils;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import eh.cdr.api.vo.MedicalDetailBean;
import eh.utils.ValidateUtil;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.client.DocIndexClient;
import recipe.constant.RecipeBussConstant;
import recipe.core.api.IFastRecipeBusinessService;
import recipe.core.api.patient.IPatientBusinessService;
import recipe.dao.*;
import recipe.enumerate.status.RecipeStatusEnum;
import recipe.enumerate.type.BussSourceTypeEnum;
import recipe.hisservice.RecipeToHisCallbackService;
import recipe.vo.doctor.RecipeInfoVO;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @Description
 * @Author yzl
 * @Date 2022-08-16
 */
@RpcBean(value = "fastRecipeService")
public class FastRecipeService implements IFastRecipeBusinessService {

    private static final Logger logger = LoggerFactory.getLogger(RecipeToHisCallbackService.class);

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


    @Override
    public List<Integer> fastRecipeSaveRecipe(List<RecipeInfoVO> recipeInfoVOList) {

        List<Integer> recipeIds = Lists.newArrayList();
        for (RecipeInfoVO recipeInfoVO : recipeInfoVOList) {
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
            recipeBean.setBussSource(BussSourceTypeEnum.BUSSSOURCE_REVISIT.getType());

            //2.获取药方配置的字段
            FastRecipe fastRecipe = fastRecipeDAO.get(recipeInfoVO.getMouldId());
            logger.info("fastRecipeSaveRecipe fastRecipe={}", JSON.toJSONString(fastRecipe));
            List<FastRecipeDetail> fastRecipeDetailList = fastRecipeDetailDAO.findFastRecipeDetailsByFastRecipeId(recipeInfoVO.getMouldId());
            logger.info("fastRecipeSaveRecipe fastRecipeDetailList={}", JSON.toJSONString(fastRecipeDetailList));

            RecipeExtendBean recipeExtendBean = new RecipeExtendBean();
            //ToDo recipeExtendBean赋值
            if (Objects.nonNull(recipeInfoVO.getRecipeExtendBean()) && Objects.nonNull(recipeInfoVO.getRecipeExtendBean().getDocIndexId())) {
                recipeExtendBean.setDocIndexId(recipeInfoVO.getRecipeExtendBean().getDocIndexId());
            }

            Integer copyNum = fastRecipe.getCopyNum();
            if (Objects.nonNull(fastRecipe.getCopyNum())) {
                recipeInfoVO.getRecipeBean().setCopyNum(copyNum);
            }
            List<RecipeDetailBean> recipeDetailBeanList = convertToList(fastRecipeDetailList);
            recipeInfoVO.setRecipeDetails(recipeDetailBeanList);
            recipeInfoVO.setRecipeExtendBean(recipeExtendBean);
            int buyNum = ValidateUtil.nullOrZeroInteger(recipeInfoVO.getBuyNum()) ? 1 : recipeInfoVO.getBuyNum();
            packageTotalParamByBuyNum(recipeInfoVO, buyNum);
            Integer recipeId = recipePatientService.saveRecipe(recipeInfoVO);
            recipePatientService.esignRecipeCa(recipeId);
            recipePatientService.updateRecipeIdByConsultId(recipeId, recipeInfoVO.getRecipeBean().getClinicId());
            recipeIds.add(recipeId);
        }
        return recipeIds;
    }

    //@Override
    //public List<FastRecipeVO> findFastRecipeListByOrganId(Integer organId) {
    //    List<FastRecipe> fastRecipeList = fastRecipeDAO.findFastRecipeListByOrganId(organId);
    //    if (CollectionUtils.isEmpty(fastRecipeList)) {
    //        return Lists.newArrayList();
    //    }
    //    List<FastRecipeVO> fastRecipeVOList = BeanCopyUtils.copyList(fastRecipeList, FastRecipeVO::new);
    //    for (FastRecipeVO fastRecipeVO : fastRecipeVOList) {
    //        List<FastRecipeDetail> fastRecipeDetailList = fastRecipeDetailDAO.findFastRecipeDetailsByFastRecipeId(fastRecipeVO.getId());
    //        if (CollectionUtils.isNotEmpty(fastRecipeDetailList)) {
    //            List<FastRecipeDetailVO> fastRecipeDetailVOList = BeanCopyUtils.copyList(fastRecipeDetailList, FastRecipeDetailVO::new);
    //            fastRecipeVO.setFastRecipeDetailList(fastRecipeDetailVOList);
    //        }
    //    }
    //    return fastRecipeVOList;
    //}

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

        //1.保存药方
        FastRecipe fastRecipe = new FastRecipe();
        fastRecipe.setIntroduce("");
        fastRecipe.setBackgroundImg("");
        fastRecipe.setStatus(1);
        fastRecipe.setMinNum(0);
        fastRecipe.setMaxNum(0);
        fastRecipe.setOrderNum(0);
        fastRecipe.setClinicOrgan(recipe.getClinicOrgan());
        fastRecipe.setOrganName(recipe.getOrganName());
        fastRecipe.setActualPrice(recipe.getActualPrice());
        fastRecipe.setCopyNum(recipe.getCopyNum());
        fastRecipe.setDecoctionId(recipeExtend.getDecoctionId());
        fastRecipe.setDecoctionPrice(recipeExtend.getDecoctionPrice());
        fastRecipe.setDecoctionText(recipeExtend.getDecoctionText());
        if (Objects.nonNull(recipeExtend.getDocIndexId())) {
            MedicalDetailBean medicalDetailBean = docIndexClient.getEmrMedicalDetail(recipeExtend.getDocIndexId());
            if (Objects.nonNull(medicalDetailBean) && Objects.nonNull(medicalDetailBean.getDetailList())) {
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
        FastRecipe fastRecipeResult = fastRecipeDAO.save(fastRecipe);

        //2.保存药方详情
        List<Recipedetail> recipedetailList = recipeDetailDAO.findByRecipeId(recipeId);
        if (CollectionUtils.isNotEmpty(recipedetailList)) {
            for (Recipedetail recipedetail : recipedetailList) {
                FastRecipeDetail fastRecipeDetail = BeanUtils.map(recipedetail, FastRecipeDetail.class);
                fastRecipeDetailDAO.save(fastRecipeDetail);
            }
        }
        return fastRecipeResult.getId();
    }


    private List<RecipeDetailBean> convertToList(List<FastRecipeDetail> fastRecipeDetailList) {
        return null;
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
            fastRecipe.setOrderNum(fastRecipeVO.getOrderNum());
            fastRecipe.setMaxNum(fastRecipeVO.getMaxNum());
            fastRecipe.setMinNum(fastRecipeVO.getMinNum());
            fastRecipe.setStatus(fastRecipeVO.getStatus());
        }
        return true;
    }

    @Override
    public Boolean fullUpdateFastRecipe(FastRecipeVO fastRecipeVO) {
        //1.更新药方
        FastRecipe fastRecipe = fastRecipeDAO.get(fastRecipeVO.getId());
        if (Objects.isNull(fastRecipe)) {
            throw new DAOException("未找到对应药方单！");
        } else {
            fastRecipe.setBackgroundImg(fastRecipeVO.getBackgroundImg());
            fastRecipe.setIntroduce(fastRecipeVO.getIntroduce());
        }
        //1.更新药方详情（目前只能删除药品，修改药品随后版本做）
        List<FastRecipeDetailVO> fastRecipeDetailVOList = fastRecipeVO.getFastRecipeDetailList();
        if (CollectionUtils.isEmpty(fastRecipeDetailVOList)) {
            throw new DAOException("最少添加一种药品！");
        }
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
        return true;
    }
}
