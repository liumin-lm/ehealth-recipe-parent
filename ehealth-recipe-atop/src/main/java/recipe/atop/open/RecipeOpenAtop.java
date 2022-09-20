package recipe.atop.open;

import com.alibaba.fastjson.JSONArray;
import com.ngari.common.mode.HisResponseTO;
import com.ngari.platform.recipe.mode.QueryRecipeInfoHisDTO;
import com.ngari.recipe.vo.FastRecipeReq;
import com.ngari.recipe.entity.FastRecipe;
import com.ngari.recipe.entity.FastRecipeDetail;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.Symptom;
import com.ngari.recipe.hisprescription.model.RegulationRecipeIndicatorsDTO;
import com.ngari.recipe.offlinetoonline.model.FindHisRecipeDetailReqVO;
import com.ngari.recipe.recipe.model.RecipeBean;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import com.ngari.recipe.recipe.model.SymptomDTO;
import com.ngari.recipe.vo.FastRecipeDetailVO;
import com.ngari.recipe.vo.FastRecipeVO;
import ctd.persistence.exception.DAOException;
import ctd.util.BeanUtils;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import eh.utils.BeanCopyUtils;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.aop.LogRecord;
import recipe.api.open.IRecipeAtopService;
import recipe.atop.BaseAtop;
import recipe.constant.ErrorCode;
import recipe.core.api.IClinicCartBusinessService;
import recipe.core.api.IFastRecipeBusinessService;
import recipe.core.api.IRecipeBusinessService;
import recipe.core.api.IRevisitBusinessService;
import recipe.core.api.patient.IOfflineRecipeBusinessService;
import recipe.enumerate.status.RecipeAuditStateEnum;
import recipe.enumerate.status.RecipeStateEnum;
import recipe.enumerate.status.SignEnum;
import recipe.util.ObjectCopyUtils;
import recipe.vo.doctor.RecipeInfoVO;
import recipe.vo.patient.PatientOptionalDrugVo;
import recipe.vo.second.RecipePayHISCallbackReq;
import recipe.vo.second.RevisitRecipeTraceVo;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

/**
 * 处方服务入口类
 *
 * @author zhaoh
 * @date 2021/7/19
 */
@RpcBean("recipeOpenAtop")
public class RecipeOpenAtop extends BaseAtop implements IRecipeAtopService {

    @Autowired
    private IRecipeBusinessService recipeBusinessService;

    @Autowired
    private IRevisitBusinessService revisitRecipeTrace;

    @Autowired
    private IOfflineRecipeBusinessService offlineToOnlineService;

    @Resource
    private IClinicCartBusinessService clinicCartService;

    @Resource
    private IFastRecipeBusinessService fastRecipeService;


    @Override
    public Boolean existUncheckRecipe(Integer bussSource, Integer clinicId) {
        logger.info("existUncheckRecipe bussSource={} clinicID={}", bussSource, clinicId);
        validateAtop(bussSource, clinicId);
        try {
            //接口结果 True存在 False不存在
            Boolean result = recipeBusinessService.existUncheckRecipe(bussSource, clinicId);
            logger.info("RecipeOpenAtop existUncheckRecipe result = {}", result);
            return result;
        } catch (DAOException e1) {
            logger.error("RecipeOpenAtop existUncheckRecipe error", e1);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e1.getMessage());
        } catch (Exception e) {
            logger.error("RecipeOpenAtop existUncheckRecipe error e", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        }
    }

    /**
     * 复诊处方追溯
     *
     * @param recipeId 处方ID
     * @param clinicId 复诊ID
     * @return
     */
    @Override
    public List<RevisitRecipeTraceVo> revisitRecipeTrace(Integer recipeId, Integer clinicId) {
        logger.info("RecipeOpenAtop revisitRecipeTrace bussSource={} clinicID={}", recipeId, clinicId);
        if (clinicId == null && recipeId == null) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "请传入业务ID或复诊ID");
        }
        try {
            List<RevisitRecipeTraceVo> result = revisitRecipeTrace.revisitRecipeTrace(recipeId, clinicId);
            logger.info("RecipeOpenAtop existUncheckRecipe result = {}", result);
            return result;
        } catch (DAOException e1) {
            logger.error("RecipeOpenAtop existUncheckRecipe error", e1);
            throw new DAOException(e1.getCode(), e1.getMessage());
        } catch (Exception e) {
            logger.error("RecipeOpenAtop existUncheckRecipe error e", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        }
    }

    /**
     * 复诊处方追溯列表数据处理
     *
     * @param startTime
     * @param endTime
     * @param recipeIds
     * @param organId
     */
    @Override
    public void handDealRevisitTraceRecipe(String startTime, String endTime, List<Integer> recipeIds, Integer organId) {
        logger.info("RecipeOpenAtop handDealRevisitTraceRecipe startTime={} endTime={} recipeIds={} organId={}", startTime, endTime, JSONUtils.toString(recipeIds), organId);
        try {
            revisitRecipeTrace.handDealRevisitTraceRecipe(startTime, endTime, recipeIds, organId);
            logger.info("RecipeOpenAtop handDealRevisitTraceRecipe end");
        } catch (DAOException e1) {
            logger.error("RecipeOpenAtop handDealRevisitTraceRecipe error", e1);
            throw new DAOException(e1.getCode(), e1.getMessage());
        } catch (Exception e) {
            logger.error("RecipeOpenAtop handDealRevisitTraceRecipe error e", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        }
    }

    @Override
    public RecipeBean getByRecipeId(Integer recipeId) {
        logger.info("RecipeOpenAtop getByRecipeId recipeId={}", recipeId);
        validateAtop(recipeId);
        try {
            Recipe recipe = recipeBusinessService.getByRecipeId(recipeId);
            RecipeBean result = ObjectCopyUtils.convert(recipe, RecipeBean.class);
            logger.info("RecipeOpenAtop getByRecipeId  result = {}", JSONUtils.toString(result));
            return result;
        } catch (DAOException e1) {
            logger.warn("RecipeOpenAtop getByRecipeId error", e1);
            throw new DAOException(e1.getCode(), e1.getMessage());
        } catch (Exception e) {
            logger.error("RecipeOpenAtop getByRecipeId error e", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        }
    }

    @Override
    public List<RecipeBean> findRecipesByStatusAndInvalidTime(List<Integer> status, Date invalidTime) {
        List<Recipe> recipes = recipeBusinessService.findRecipesByStatusAndInvalidTime(status, invalidTime);
        return ObjectCopyUtils.convert(recipes, RecipeBean.class);
    }

    @Override
    public void savePatientDrug(PatientOptionalDrugVo patientOptionalDrugVo) {
        logger.info("RecipeOpenAtop savePatientDrug patientOptionalDrugVo={}", JSONArray.toJSONString(patientOptionalDrugVo));
        validateAtop(patientOptionalDrugVo.getClinicId(), patientOptionalDrugVo.getDrugId(), patientOptionalDrugVo.getOrganDrugCode(), patientOptionalDrugVo.getOrganId(), patientOptionalDrugVo.getPatientDrugNum());
        recipeBusinessService.savePatientDrug(patientOptionalDrugVo);
    }

    @Override
    public RegulationRecipeIndicatorsDTO regulationRecipe(Integer recipeId) {
        validateAtop(recipeId);
        return recipeBusinessService.regulationRecipe(recipeId);
    }

    @Override
    public void offlineToOnlineForRecipe(FindHisRecipeDetailReqVO request) {
        logger.info("recipeOpenAtop findHisRecipeDetail request:{}", ctd.util.JSONUtils.toString(request));
        validateAtop(request, request.getOrganId(), request.getMpiId());
        offlineToOnlineService.offlineToOnlineForRecipe(request);
    }

    @Override
    public Boolean updateAuditState(Integer recipeId, Integer state) {
        return recipeBusinessService.updateAuditState(recipeId, RecipeAuditStateEnum.getRecipeAuditStateEnum(state));
    }

    @Override
    public Boolean updateRecipeState(Integer recipeId, Integer processState, Integer subState) {
        return recipeBusinessService.updateRecipeState(recipeId, RecipeStateEnum.getRecipeStateEnum(processState), RecipeStateEnum.getRecipeStateEnum(subState));
    }

    @Override
    public Boolean updateCheckerSignState(Integer recipeId, Integer checkerSignState) {
        return recipeBusinessService.updateCheckerSignState(recipeId, SignEnum.getSignEnum(checkerSignState));
    }

    @Override
    public RecipeBean getByRecipeCodeAndRegisterIdAndOrganId(String recipeCode, String registerId, int organId) {
        validateAtop(recipeCode, organId);
        return recipeBusinessService.getByRecipeCodeAndRegisterIdAndOrganId(recipeCode, registerId, organId);
    }

    @Override
    public FastRecipeVO getFastRecipeById(Integer id) {
        FastRecipeReq fastRecipeReq = new FastRecipeReq();
        fastRecipeReq.setFastRecipeId(id);
        List<FastRecipe> fastRecipeList = fastRecipeService.findFastRecipeListByParam(fastRecipeReq);
        if (CollectionUtils.isEmpty(fastRecipeList)) {
            return null;
        }
        FastRecipeVO fastRecipeVO = BeanUtils.map(fastRecipeList.get(0), FastRecipeVO.class);
        List<FastRecipeDetail> fastRecipeDetailList = fastRecipeService.findFastRecipeDetailsByFastRecipeId(fastRecipeVO.getId());
        if (CollectionUtils.isNotEmpty(fastRecipeDetailList)) {
            fastRecipeVO.setFastRecipeDetailList(BeanCopyUtils.copyList(fastRecipeDetailList, FastRecipeDetailVO::new));
        }
        return fastRecipeVO;
    }

    @Override
    public List<QueryRecipeInfoHisDTO> findRecipeByIds(List<Integer> recipeIds) {
        validateAtop(recipeIds);
        return recipeBusinessService.findRecipeByIds(recipeIds);
    }

    @Override
    public SymptomDTO symptomId(Integer id) {
        validateAtop(id);
        Symptom symptom = recipeBusinessService.symptomId(id);
        if (null == symptom) {
            return null;
        }
        return ObjectCopyUtils.convert(symptom, SymptomDTO.class);
    }

    @Override
    public HisResponseTO abolishOffLineRecipe(Integer organId, List<String> recipeCode) {
        HisResponseTO response = offlineToOnlineService.abolishOffLineRecipe(organId, recipeCode);
        return response;
    }

    @Override
    public HisResponseTO recipeListQuery(Integer organId, List<String> recipeCodes) {
        HisResponseTO hisResponseTO = new HisResponseTO();
        recipeBusinessService.recipeListQuery(organId, recipeCodes);
        return hisResponseTO;
    }

    @Override
    public List<RecipeBean> recipeListByClinicId(Integer clinicId, Integer bussSource) {
        return recipeBusinessService.recipeListByClinicId(clinicId, bussSource);
    }

    @Override
    public List<RecipeDetailBean> findRecipeDetailByRecipeId(Integer recipeId) {
        validateAtop(recipeId);
        return recipeBusinessService.findRecipeDetailByRecipeId(recipeId);
    }

    /**
     * 获取某处方单关联处方（同一个患者同一次就诊）
     *
     * @param recipeId
     * @param doctorId
     * @return
     */
    @Override
    public List<RecipeInfoVO> findRelatedRecipeRecordByRegisterNo(Integer recipeId, Integer doctorId,
                                                                  List<Integer> recipeTypeList, List<Integer> organIds) {
        return recipeBusinessService.findRelatedRecipeRecordByRegisterNo(recipeId, doctorId, recipeTypeList, organIds);
    }

    /**
     * 药师签名 只是获取药师手签更新PDF
     * @param recipeId
     */
    @Override
    public void pharmacyToRecipePDF(Integer recipeId) {
        recipeBusinessService.pharmacyToRecipePDF(recipeId);
    }

    /**
     * 药师签名并进行CA操作
     * @param recipeId
     * @param checker
     */
    @Override
    public void pharmacyToRecipePDFAndCa(Integer recipeId, Integer checker) {
        recipeBusinessService.pharmacyToRecipePDFAndCa(recipeId, checker);
    }

    @Override
    public Boolean deleteClinicCartByIds(List<Integer> ids) {
        return clinicCartService.deleteClinicCartByIds(ids);
    }

    @Override
    @LogRecord
    public List<RecipeBean> findRecipeByMpiidAndrecipeStatus(String mpiid, List<Integer> recipeStatus,Integer terminalType,Integer organId) {
        return com.ngari.patient.utils.ObjectCopyUtils.convert(recipeBusinessService.findRecipeByMpiidAndrecipeStatus(mpiid,recipeStatus,terminalType,organId), RecipeBean.class);
    }

    @Override
    public HisResponseTO recipePayHISCallback(RecipePayHISCallbackReq recipePayHISCallbackReq) {
        recipeBusinessService.recipePayHISCallback(recipePayHISCallbackReq);
        HisResponseTO hisResponseTO = new HisResponseTO();
        return hisResponseTO;
    }

}
