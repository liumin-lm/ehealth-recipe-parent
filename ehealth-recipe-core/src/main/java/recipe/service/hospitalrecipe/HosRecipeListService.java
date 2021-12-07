package recipe.service.hospitalrecipe;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.ngari.base.BaseAPI;
import com.ngari.base.organ.model.OrganBean;
import com.ngari.base.organ.service.IOrganService;
import com.ngari.base.patient.model.PatientBean;
import com.ngari.base.patient.service.IPatientExtendService;
import com.ngari.patient.dto.DoctorDTO;
import com.ngari.patient.dto.HealthCardDTO;
import com.ngari.patient.service.DoctorService;
import com.ngari.patient.service.HealthCardService;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.common.RecipeCommonBaseTO;
import com.ngari.recipe.common.RecipeStandardResTO;
import com.ngari.recipe.common.utils.VerifyUtils;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.recipe.model.RecipeBean;
import com.ngari.recipe.vo.PatientBeanNoDS;
import ctd.account.UserRoleToken;
import ctd.persistence.DAOFactory;
import ctd.persistence.exception.DAOException;
import ctd.schema.exception.ValidateException;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import recipe.ApplicationUtils;
import recipe.constant.ErrorCode;
import recipe.dao.RecipeDAO;
import recipe.service.common.RecipeSingleService;
import recipe.service.hospitalrecipe.dto.HosRecipeListRequest;
import recipe.util.ChinaIDNumberUtil;

import javax.annotation.Nullable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * company: ngarihealth
 * author: 0184/yu_yun
 * date:2018/10/16
 */
@RpcBean(value = "hosRecipeListService")
public class HosRecipeListService {

    private static final Logger LOG = LoggerFactory.getLogger(HosRecipeListService.class);

    @Autowired
    @Qualifier("recipeSingleService")
    private RecipeSingleService singleService;

    /**
     * 查询HOS所有处方
     *
     * @param request
     * @return
     */
    @RpcService
    public RecipeStandardResTO<Map> findHistroyRecipeList(HosRecipeListRequest request) {
        RecipeStandardResTO<Map> response = RecipeStandardResTO.getRequest(Map.class);
        response.setCode(RecipeCommonBaseTO.FAIL);
        if (null != request) {
            //校验参数
            try {
                Multimap<String, String> detailVerifyMap = VerifyUtils.verify(request);
                if (!detailVerifyMap.keySet().isEmpty()) {
                    response.setMsg(detailVerifyMap.toString());
                    return response;
                }
            } catch (Exception e) {
                LOG.warn("findHistroyRecipeList 请求对象异常数据. HosRecipeListRequest={}", JSONUtils.toString(request), e);
                response.setMsg("请求对象异常数据");
                return response;
            }

            Map<String, Object> recipeInfo = Maps.newHashMap();
            response.setData(recipeInfo);

            //需要转换组织机构编码
            String organId = request.getOrganId();
            Integer clinicOrgan = null;
            try {
                IOrganService organService = BaseAPI.getService(IOrganService.class);
                List<OrganBean> organList = organService.findByOrganizeCode(organId);
                if (CollectionUtils.isNotEmpty(organList)) {
                    clinicOrgan = organList.get(0).getOrganId();
                }
            } catch (Exception e) {
                LOG.warn("findHistroyRecipeList 平台未匹配到该组织机构编码. organId={}", organId, e);
            } finally {
                if (null == clinicOrgan) {
                    response.setMsg("平台未匹配到该组织机构编码");
                    return response;
                }

                recipeInfo.put("clinicOrgan", clinicOrgan);
            }

            //先查询就诊人是否存在
            boolean patientExist = false;
            PatientBean patient = null;
            try {
                Map<String, Object> map = setPatientInfo(request);
                patient = (PatientBean) map.get("patient");
                patientExist = (Boolean) map.get("patientExist");
            } catch (Exception e) {
                LOG.warn("findHistroyRecipeList 处理就诊人异常，doctorId={}, clinicOrgan={}",
                        request.getDoctorId(), clinicOrgan, e);
            } finally {
                if (null == patient || StringUtils.isEmpty(patient.getMpiId())) {
                    LOG.warn("findHistroyRecipeList 患者创建失败，doctorId={}, clinicOrgan={}",
                            request.getDoctorId(), clinicOrgan);
                    response.setMsg("患者创建失败");
                    patientExist = false;
                    return response;
                } else {
                    recipeInfo.put("mpiId", patient.getMpiId());
                }
            }
            List<RecipeBean> backList = Lists.newArrayList();
            //患者存在再去进行查询
            LOG.info("findHistroyRecipeList patientExist={}, mpiId={}, doctorId={}, clinicOrgan={}",
                    patientExist, patient.getMpiId(), request.getDoctorId(), clinicOrgan);
            if (patientExist) {
                RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
                List<Recipe> recipeList = recipeDAO.findHosRecipe(request.getDoctorId(), patient.getMpiId(), clinicOrgan,
                        request.getStart(), request.getLimit());
                Map<Integer, Recipe> dbMap = Maps.uniqueIndex(recipeList.iterator(), new Function<Recipe, Integer>() {
                    @Nullable
                    @Override
                    public Integer apply(@Nullable Recipe input) {
                        return input.getRecipeId();
                    }
                });
                if (CollectionUtils.isNotEmpty(recipeList)) {
                    backList = ObjectCopyUtils.convert(recipeList, RecipeBean.class);
                    //处理数据
                    // 分为 -1:查不到处方 0：未签名 1: 其他状态展示详情页  2：药店取药已签名  3: 配送到家已签名-未支付  4:配送到家已签名-已支付 5:审核不通过  6:作废
                    for (RecipeBean recipeBean : backList) {
                        recipeBean.setNotation(singleService.getNotation(dbMap.get(recipeBean.getRecipeId())));
                    }
                }

                //是否存在下一页
                List<Recipe> nextList = recipeDAO.findHosRecipe(request.getDoctorId(), patient.getMpiId(), clinicOrgan,
                        request.getStart() + request.getLimit(), 1);
                if (CollectionUtils.isEmpty(nextList)) {
                    recipeInfo.put("hasNext", false);
                } else {
                    recipeInfo.put("hasNext", true);
                }

            }
            recipeInfo.put("list", backList);
            response.setCode(RecipeCommonBaseTO.SUCCESS);
        } else {
            response.setMsg("请求对象为空");
            return response;
        }

        return response;
    }

    @RpcService
    public PatientBeanNoDS addPatientForDoctorNoDS(HosRecipeListRequest request) {
        PatientBean patientBean = addPatientForDoctor(request);
        LOG.info("addPatientForDoctorNoDS patientBean:{}", JSONUtils.toString(patientBean));
        PatientBeanNoDS patientBeanNoDS = ObjectCopyUtils.convert(patientBean, PatientBeanNoDS.class);
        LOG.info("addPatientForDoctorNoDS patientBeanNoDS:{}", JSONUtils.toString(patientBeanNoDS));
        return patientBeanNoDS;
    }

    /**
     * 为医生添加患者信息
     *
     * @param request 医院传来的医生患者信息
     * @return 结果
     */
    @RpcService
    public PatientBean addPatientForDoctor(HosRecipeListRequest request) {
        validate(request);
        PatientBean patient = null;
        try {
            //获取患者性别
            String sex = ChinaIDNumberUtil.getSexFromIDNumber(request.getCertificate());
            request.setPatientSex(sex);
        } catch (ValidateException e) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "患者身份证件不正确");
        }
        UserRoleToken urt = UserRoleToken.getCurrent();
        Integer doctorId = urt.getDoctorId();
        LOG.info("addPatientForDoctor, isDoctor:{},doctorId:{}", urt.isDoctor(), urt.getDoctorId());
        request.setDoctorId(doctorId);
        try {
            Map<String, Object> map = setPatientInfo(request);
            patient = (PatientBean) map.get("patient");
            saveHealthCardForOrgan(request, patient, doctorId);
        } catch (Exception e) {
            LOG.warn("addPatientForDoctor 处理就诊人异常，doctorId={}",
                    request.getDoctorId(), e);
        } finally {
            if (null == patient || StringUtils.isEmpty(patient.getMpiId())) {
                LOG.warn("addPatientForDoctor 患者创建失败，doctorId={}",
                        request.getDoctorId());

            }
        }
        return patient;
    }

    /**
     * 保存健康卡信息
     *
     * @param request  链接请求参数
     * @param patient  患者信息
     * @param doctorId 医生ID
     */
    private void saveHealthCardForOrgan(HosRecipeListRequest request, PatientBean patient, Integer doctorId) {
        HealthCardService healthCardService = ApplicationUtils.getBasicService(HealthCardService.class);
        List<HealthCardDTO> healthCardDTOS = healthCardService.findByMpiId(patient.getMpiId());
        DoctorService doctorService = ApplicationUtils.getBasicService(DoctorService.class);
        DoctorDTO doctorDTO = doctorService.getByDoctorId(doctorId);
        if (CollectionUtils.isEmpty(healthCardDTOS)) {
            HealthCardDTO healthCardDTO = new HealthCardDTO();
            healthCardDTO.setMpiId(patient.getMpiId());
            healthCardDTO.setCardId(request.getCardNo());
            healthCardDTO.setCardType("1");
            healthCardDTO.setCardOrgan(doctorDTO.getOrgan());
            healthCardDTO.setCardStatus(1);
            healthCardDTO.setInitialCardID(request.getCardNo());
            healthCardDTO.setCardSource("remote");
            healthCardDTO.setCreateDate(new Date());
            if (!healthCardService.saveHealthCardForRecipe(healthCardDTO)) {
                LOG.warn("addPatientForDoctor 就诊卡保存失败,healthCardDTO:{}.", JSONUtils.toString(healthCardDTO));
            }
        }
    }

    private void validate(HosRecipeListRequest request) {
        LOG.info("HosRecipeListRequest,岳阳绑定患者信息:{}.", JSONUtils.toString(request));
        if (request.getDoctorId() == null) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "医生id不能为空");
        }
        if (StringUtils.isEmpty(request.getCertificate())) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "患者身份证不能为空");
        }
        if (StringUtils.isEmpty(request.getPatientName())) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "患者姓名不能为空");
        }
        if (StringUtils.isEmpty(request.getPatientTel())) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "患者手机号不能为空");
        }
        if (request.getCertificateType() == null) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "患者证件类型不能为空");
        }
        if (StringUtils.isEmpty(request.getCardNo())) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "患者就诊卡号不能为空");
        }
    }

    private Map<String, Object> setPatientInfo(HosRecipeListRequest request) {
        boolean patientExist = false;
        PatientBean patient;
        IPatientExtendService patientExtendService = BaseAPI.getService(IPatientExtendService.class);
        List<PatientBean> patList = patientExtendService.findPatient4Doctor(request.getDoctorId(), request.getCertificate());
        if (CollectionUtils.isEmpty(patList)) {
            //不存在该患者则需要添加
            patient = new PatientBean();
            patient.setPatientName(request.getPatientName());
            patient.setPatientSex(request.getPatientSex());
            patient.setCertificateType(Integer.valueOf(request.getCertificateType()));
            patient.setCertificate(request.getCertificate());
            patient.setMobile(request.getPatientTel());
            //创建就诊人
            patient = patientExtendService.addPatient4DoctorApp(patient, 0, request.getDoctorId());
        } else {
            patientExist = true;
            patient = patList.get(0);
        }
        Map<String, Object> map = new HashMap<>(2);
        map.put("patient", patient);
        map.put("patientExist", patientExist);
        return map;
    }
}
