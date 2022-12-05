package recipe.atop.open;

import com.alibaba.fastjson.JSONArray;
import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.recipe.mode.RecipeInfoTO;
import com.ngari.patient.dto.DoctorDTO;
import com.ngari.platform.recipe.mode.OutpatientPaymentRecipeDTO;
import com.ngari.platform.recipe.mode.QueryRecipeInfoHisDTO;
import com.ngari.recipe.dto.PatientDTO;
import com.ngari.recipe.dto.ServiceLogDTO;
import com.ngari.recipe.entity.*;
import com.ngari.recipe.hisprescription.model.RegulationRecipeIndicatorsDTO;
import com.ngari.recipe.offlinetoonline.model.FindHisRecipeDetailReqVO;
import com.ngari.recipe.recipe.model.RecipeBean;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import com.ngari.recipe.recipe.model.SymptomDTO;
import com.ngari.recipe.vo.FastRecipeDetailVO;
import com.ngari.recipe.vo.FastRecipeReq;
import com.ngari.recipe.vo.FastRecipeVO;
import ctd.persistence.exception.DAOException;
import ctd.util.BeanUtils;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import eh.utils.BeanCopyUtils;
import eh.utils.ValidateUtil;
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
import recipe.vo.PageGenericsVO;
import recipe.vo.doctor.RecipeInfoVO;
import recipe.vo.patient.PatientOptionalDrugVo;
import recipe.vo.second.*;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

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
    public List<OutpatientPaymentRecipeDTO> findOutpatientPaymentRecipes(Integer organId, String mpiId) {
        validateAtop(organId,mpiId);
        return recipeBusinessService.findOutpatientPaymentRecipes(organId,mpiId);
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
    public List<RecipeBean> recipeAllByClinicId(Integer clinicId, Integer bussSource) {
        List<Recipe> list = recipeBusinessService.recipeAllByClinicId(clinicId, bussSource);
        return ObjectCopyUtils.convert(list, RecipeBean.class);
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
    public List<RecipeBean> findRecipeByMpiidAndrecipeStatus(String mpiid, List<Integer> recipeStatus,Integer terminalType,Integer organId) {
        return com.ngari.patient.utils.ObjectCopyUtils.convert(recipeBusinessService.findRecipeByMpiidAndrecipeStatus(mpiid, recipeStatus, terminalType, organId), RecipeBean.class);
    }

    @Override
    public AutomatonCountVO findRecipeCountForAutomaton(AutomatonVO automatonVO) {
        AutomatonCountVO automatonCountVO=new AutomatonCountVO();
        automatonCountVO.setCount(recipeBusinessService.findRecipeCountForAutomaton(automatonVO));
        return automatonCountVO;
    }

    @Override
    public List<AutomatonCountVO> findRecipeEveryDayForAutomaton(AutomatonVO automatonVO) {
        List<AutomatonCountVO> automatonCountVOS=recipeBusinessService.findRecipeEveryDayForAutomaton(automatonVO);
        return automatonCountVOS;
    }

    @Override
    public List<AutomatonCountVO> findRecipeTop5ForAutomaton(AutomatonVO automatonVO) {
        return recipeBusinessService.findRecipeTop5ForAutomaton(automatonVO);
    }


    @Override
    public HisResponseTO recipePayHISCallback(RecipePayHISCallbackReq recipePayHISCallbackReq) {
        recipeBusinessService.recipePayHISCallback(recipePayHISCallbackReq);
        HisResponseTO hisResponseTO = new HisResponseTO();
        return hisResponseTO;
    }

    @Override
    public PageGenericsVO<AutomatonResultVO> automatonList(AutomatonVO automatonVO) {
        validateAtop(automatonVO, automatonVO.getTerminalType(), automatonVO.getStartTime(),
                automatonVO.getEndTime(), automatonVO.getStart(), automatonVO.getLimit());
        //出参
        int start = automatonVO.getStart();
        PageGenericsVO<AutomatonResultVO> result = new PageGenericsVO<>();
        result.setStart(start);
        result.setLimit(automatonVO.getLimit());
        result.setDataList(Collections.emptyList());
        //查询对象
        Recipe recipe = new Recipe();
        recipe.setRecipeId(automatonVO.getRecipeId());
        recipe.setClinicOrgan(automatonVO.getOrganId());
        recipe.setPayFlag(automatonVO.getPayFlag());
        recipe.setMedicalFlag(automatonVO.getMedicalFlag());
        //总条数
        Integer count = recipeBusinessService.automatonCount(automatonVO, recipe);
        result.setTotal(count);
        if (ValidateUtil.nullOrZeroInteger(count)) {
            return result;
        }
        //列表数据
        automatonVO.setStart((automatonVO.getStart() - 1) * automatonVO.getLimit());
        List<AutomatonResultVO> list = recipeBusinessService.automatonList(automatonVO, recipe);
        if (CollectionUtils.isEmpty(list)) {
            return result;
        }
        //返回列表数据组织
        Set<Integer> enterpriseIds = new HashSet<>();
        Set<Integer> doctorIds = new HashSet<>();
        List<String> mpiIdList = list.stream().map(a -> {
            enterpriseIds.add(a.getEnterpriseId());
            doctorIds.add(a.getDoctor());
            return a.getMpiid();
        }).distinct().collect(Collectors.toList());
        //药企
        Map<Integer, DrugsEnterprise> drugsEnterpriseMap = enterpriseBusinessService.findDrugsEnterpriseByIds(new ArrayList<>(enterpriseIds));
        //医生
        Map<Integer, DoctorDTO> doctorMap = iDoctorBusinessService.findByDoctorIds(new ArrayList<>(doctorIds));
        //患者
        Map<String, PatientDTO> patientMap = recipePatientService.findPatientByMpiIds(mpiIdList);
        list.forEach(a -> {
            DrugsEnterprise drugsEnterprise = drugsEnterpriseMap.get(a.getEnterpriseId());
            a.setEnterpriseName(null == drugsEnterprise ? null : drugsEnterprise.getName());
            DoctorDTO doctor = doctorMap.get(a.getDoctor());
            a.setDoctorMobile(null == doctor ? null : doctor.getMobile());
            PatientDTO patient = patientMap.get(a.getMpiid());
            a.setPatientMobile(null == patient ? null : patient.getMobile());
        });
        result.setDataList(list);
        return result;
    }

    @Override
    public PageGenericsVO<List<SelfServiceMachineResVo>> findRecipeToZiZhuJi(SelfServiceMachineReqVO selfServiceMachineReqVO) {
        return recipeBusinessService.findRecipeToZiZhuJi(selfServiceMachineReqVO);
    }

    @Override
    public List<RecipeInfoTO> patientOfflineRecipe(Integer organId, String patientId, String patientName, Date startTime, Date endTime) {
        validateAtop(organId, patientId);
        return offlineToOnlineService.patientOfflineRecipe(organId, patientId, patientName, startTime, endTime);
    }


    @Override
    public void serviceTimeLog(ServiceLogVO serviceLog) {
        organBusinessService.serviceTimeLog(ObjectCopyUtils.convert(serviceLog, ServiceLogDTO.class));
    }

    @Override
    public List<RecipeToGuideResVO> findRecipeByClinicId(Integer clinicId) {
        return recipeBusinessService.findRecipeByClinicId(clinicId);
    }
}
