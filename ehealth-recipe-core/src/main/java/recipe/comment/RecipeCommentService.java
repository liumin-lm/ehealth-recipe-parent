package recipe.comment;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.ngari.his.regulation.entity.RegulationNotifyDataReq;
import com.ngari.his.regulation.service.IRegulationService;
import com.ngari.patient.dto.*;
import com.ngari.patient.service.*;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.platform.regulation.mode.QueryRegulationUnitReq;
import com.ngari.recipe.comment.model.*;
import com.ngari.recipe.dto.RecipeInfoDTO;
import com.ngari.recipe.entity.OrganDrugList;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeExtend;
import com.ngari.recipe.entity.Recipedetail;
import com.ngari.recipe.entity.comment.RecipeComment;
import ctd.spring.AppDomainContext;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import com.ngari.recipe.comment.service.IRecipeCommentService;
import recipe.client.DepartClient;
import recipe.dao.OrganDrugListDAO;
import recipe.dao.RecipeDAO;
import recipe.dao.comment.RecipeCommentDAO;
import recipe.manager.RecipeManager;
import recipe.presettle.condition.DoctorForceCashHandler;

import java.util.Date;
import java.util.List;
import java.util.Objects;

@RpcBean
public class RecipeCommentService implements IRecipeCommentService {
    private static final Logger logger = LoggerFactory.getLogger(DoctorForceCashHandler.class);

    @Autowired
    private RecipeCommentDAO recipeCommentDAO;

    @Autowired
    private RecipeDAO recipeDAO;

    @Autowired
    private RecipeManager recipeManager;

    @Autowired
    private OrganService organService;

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private DepartClient departClient;

    @Autowired
    private EmploymentService iEmploymentService;

    @Autowired
    private DoctorService doctorService;

    @Autowired
    private PatientService patientService;

    @Autowired
    private OrganDrugListDAO organDrugListDAO;


    @Deprecated
    @RpcService
    public void saveRecipeComment(Integer recipeId, String commentResult, String commentRemrk) {
        RecipeComment recipeComment = new RecipeComment();
        recipeComment.setRecipeId(recipeId);
        recipeComment.setCommentResult(commentResult);
        recipeComment.setCommentRemark(commentRemrk);
        Date now = new Date();
        recipeComment.setCreateDate(now);
        recipeComment.setLastModify(now);
        recipeCommentDAO.save(recipeComment);
    }

    @Override
    @RpcService
    public RecipeCommentTO getRecipeCommentByRecipeId(Integer recipeId) {
        List<RecipeComment> recipeCommentList = recipeCommentDAO.findRecipeCommentByRecipeId(recipeId);
        if (CollectionUtils.isNotEmpty(recipeCommentList)) {
            return ObjectCopyUtils.convert(recipeCommentList.get(0), RecipeCommentTO.class);
        }
        return null;
    }

    @Override
    public List<RegulationRecipeCommentBean> queryRegulationRecipeComment(QueryRegulationUnitReq req) {
        if (Objects.isNull(req.getBussId())) {
            return Lists.newArrayList();
        }
        RecipeComment recipeComment = recipeCommentDAO.get(Integer.valueOf(req.getBussId()));
        RecipeInfoDTO recipeInfoDTO = recipeManager.getRecipeInfoDTO(recipeComment.getRecipeId());
        RegulationRecipeCommentBean regulationRecipeCommentBean = packageParam(recipeComment, recipeInfoDTO);
        return Lists.newArrayList(regulationRecipeCommentBean);
    }

    private RegulationRecipeCommentBean packageParam(RecipeComment recipeComment, RecipeInfoDTO recipeInfoDTO) {
        RegulationRecipeCommentBean result = new RegulationRecipeCommentBean();
        Recipe recipe = recipeInfoDTO.getRecipe();
        RecipeExtend recipeExt = recipeInfoDTO.getRecipeExtend();
        List<Recipedetail> recipeDetailList = recipeInfoDTO.getRecipeDetails();
        OrganDTO organDTO = organService.getByOrganId(recipe.getClinicOrgan());

        //1.基本信息
        result.setOrganId(recipe.getClinicOrgan());
        result.setOrganName(recipe.getOrganName());
        result.setUnitId(organDTO.getMinkeUnitID());
        result.setUpdateTime(new Date());

        result.setAntibacterialDrugNum(0);
        result.setBaseDrugNum(0);
        result.setCommonNameNum(0);
        result.setHormoneNum(0);
        result.setInjectionNum(0);

        //2.处方信息
        CommentRecipeBean commentRecipeBean = new CommentRecipeBean();
        commentRecipeBean.setRecipeId(recipe.getRecipeId());
        commentRecipeBean.setRecipeCode(recipe.getRecipeCode());
        commentRecipeBean.setVisitId(Objects.toString(recipe.getClinicId(), ""));
        commentRecipeBean.setRegisterNo(recipeExt.getRegisterID());
        DepartmentDTO departmentDTO = departmentService.getByDeptId(recipe.getDepart());
        commentRecipeBean.setDepartmentId(Objects.toString(recipe.getDepart(), ""));
        if (Objects.nonNull(departmentDTO)) {
            commentRecipeBean.setDepartmentCode(departmentDTO.getCode());
            commentRecipeBean.setDepartmentName(departmentDTO.getName());
        }
        AppointDepartDTO appointDepartDTO = departClient.getAppointDepartByOrganIdAndDepart(recipe.getClinicOrgan(), recipe.getDepart());
        if (Objects.nonNull(appointDepartDTO)) {
            commentRecipeBean.setAppointDepartmentCode(appointDepartDTO.getAppointDepartCode());
            commentRecipeBean.setAppointDepartmentName(appointDepartDTO.getAppointDepartName());
        }
        result.setRecipeMsg(commentRecipeBean);

        //3.点评信息
        CommentBean commentBean = new CommentBean();
        commentBean.setCommentId(recipeComment.getId());
        commentBean.setCommentRemark(recipeComment.getCommentRemark());
        commentBean.setCommentResult(recipeComment.getCommentResult());
        commentBean.setCommentResultCode(recipeComment.getCommentResultCode());
        commentBean.setCommentTime(recipeComment.getCreateDate());
        commentBean.setCommentUserName(recipeComment.getCommentUserName());
        commentBean.setCommentUserType(recipeComment.getCommentUserType());
        result.setCommentMsg(commentBean);

        //4.医生信息
        CommentDoctorBean commentDoctorBean = new CommentDoctorBean();
        commentDoctorBean.setDoctorId(recipe.getDoctor());
        commentDoctorBean.setDoctorName(recipe.getDoctorName());
        commentDoctorBean.setDoctorJobNumber(iEmploymentService.getJobNumberByDoctorIdAndOrganIdAndDepartment(recipe.getDoctor(), recipe.getClinicOrgan(), recipe.getDepart()));
        DoctorDTO doctorDTO = doctorService.getByDoctorId(recipe.getDoctor());
        if (Objects.nonNull(doctorDTO)) {
            //commentDoctorBean.setDoctorTitleId(doctorDTO.getDoctorTitles());
            //commentDoctorBean.setDoctorTitle(doctorDTO.getDoctorTitles());
            commentDoctorBean.setDoctorMobile(doctorDTO.getMobile());
        }

        result.setDoctorMsg(commentDoctorBean);

        //5.患者信息
        CommentPatientBean commentPatientBean = new CommentPatientBean();
        commentPatientBean.setCardNo(recipeExt.getCardNo());
        commentPatientBean.setCardType(recipeExt.getCardType());
        commentPatientBean.setPatientName(recipe.getPatientName());
        PatientDTO patientDTO = patientService.getPatientByMpiId(recipe.getMpiid());
        if (Objects.nonNull(patientDTO)) {
            commentPatientBean.setCertId(patientDTO.getCertificate());
            commentPatientBean.setCertType(patientDTO.getCertificateType());
            commentPatientBean.setPatientSex(Integer.valueOf(patientDTO.getPatientSex()));
            commentPatientBean.setPatientAge(patientDTO.getAge().toString());
        }
        result.setPatientMsg(commentPatientBean);

        //6.药品详情信息
        List<CommentRecipeDetailBean> commentRecipeDetailBeanList = Lists.newArrayList();
        recipeDetailList.forEach(recipeDetail -> {
            CommentRecipeDetailBean commentRecipeDetailBean = new CommentRecipeDetailBean();
            OrganDrugList organDrugList = organDrugListDAO.getByOrganIdAndOrganDrugCodeAndDrugId(recipe.getClinicOrgan(),
                    recipeDetail.getOrganDrugCode(), recipeDetail.getDrugId());
            commentRecipeDetailBean.setDrugId(recipeDetail.getDrugId());
            commentRecipeDetailBean.setDrugName(recipeDetail.getDrugName());
            commentRecipeDetailBean.setDrugPack(recipeDetail.getPack().toString());
            commentRecipeDetailBean.setDrugPackUnit(recipeDetail.getDrugUnit());
            commentRecipeDetailBean.setDrugPlace(recipeDetail.getProducer());
            commentRecipeDetailBean.setDrugDose(Objects.toString(recipeDetail.getUseDose(), ""));
            commentRecipeDetailBean.setDrugUsage(recipeDetail.getUsePathways());
            commentRecipeDetailBean.setDrugSpec(recipeDetail.getDrugSpec());
            commentRecipeDetailBean.setDrugBatch(recipeDetail.getDrugBatch());
            commentRecipeDetailBean.setRemark(recipeDetail.getMemo());
            commentRecipeDetailBean.setUseDays(recipeDetail.getUseDays());
            commentRecipeDetailBean.setUsePathways(recipeDetail.getUsePathways());
            commentRecipeDetailBean.setUseTotalDose(Objects.toString(recipeDetail.getUseTotalDose(), ""));
            commentRecipeDetailBean.setUseTotalDoseUnit(recipeDetail.getUseDoseUnit());
            commentRecipeDetailBean.setUsingRate(recipeDetail.getUsingRate());
            if (Objects.nonNull(organDrugList)) {
                commentRecipeDetailBean.setDrugCode(organDrugList.getRegulationDrugCode());
                commentRecipeDetailBean.setHospDrugCode(organDrugList.getOrganDrugCode());
                commentRecipeDetailBean.setOtcMark(organDrugList.getBaseDrug());
            }
            commentRecipeDetailBeanList.add(commentRecipeDetailBean);
        });
        result.setRecipeDetailList(commentRecipeDetailBeanList);
        return result;
    }

    @Override
    public List<RecipeCommentTO> findCommentByRecipeIds(List<Integer> recipeIds) {
        if (CollectionUtils.isEmpty(recipeIds)) {
            return Lists.newArrayList();
        }
        List<RecipeComment> list = recipeCommentDAO.findCommentByRecipeIds(recipeIds);
        return ObjectCopyUtils.convert(list, RecipeCommentTO.class);
    }

    @Override
    public Integer addRecipeComment(RecipeCommentTO recipeCommentTO) {
        RecipeComment recipeComment = ObjectCopyUtils.convert(recipeCommentTO, RecipeComment.class);
        Recipe recipe = recipeDAO.getByRecipeId(recipeComment.getRecipeId());
        Integer id = recipeCommentDAO.save(recipeComment).getId();
        IRegulationService iRegulationService = AppDomainContext.getBean("his.regulationService", IRegulationService.class);
        RegulationNotifyDataReq req = new RegulationNotifyDataReq();
        req.setBussId(id.toString());
        req.setBussType("recipeComment");
        req.setNotifyTime(System.currentTimeMillis() - 1000);
        req.setOrganId(recipe.getClinicOrgan());
        logger.info("addRecipeComment notifyData req = {}", JSON.toJSONString(req));
        iRegulationService.notifyData(recipe.getClinicOrgan(), req);
        return id;
    }
}
