package recipe.service.hospitalrecipe;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.ngari.base.BaseAPI;
import com.ngari.base.organ.model.OrganBean;
import com.ngari.base.organ.service.IOrganService;
import com.ngari.base.patient.model.PatientBean;
import com.ngari.base.patient.service.IPatientExtendService;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.common.RecipeCommonBaseTO;
import com.ngari.recipe.common.RecipeStandardResTO;
import com.ngari.recipe.common.utils.VerifyUtils;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.recipe.model.RecipeBean;
import ctd.persistence.DAOFactory;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.dao.RecipeDAO;
import recipe.service.hospitalrecipe.dto.HosRecipeListRequest;

import java.util.List;
import java.util.Map;

/**
 * company: ngarihealth
 * author: 0184/yu_yun
 * date:2018/10/16
 */
@RpcBean(value = "hosRecipeListService", mvc_authentication = false)
public class HosRecipeListService {

    private static final Logger LOG = LoggerFactory.getLogger(HosRecipeListService.class);

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
            PatientBean patient = null;
            try {
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
                    patient = patList.get(0);
                }
            } catch (Exception e) {
                LOG.warn("findHistroyRecipeList 处理就诊人异常，doctorId={}, clinicOrgan={}",
                        request.getDoctorId(), clinicOrgan, e);
            } finally {
                if (null == patient || StringUtils.isEmpty(patient.getMpiId())) {
                    LOG.warn("findHistroyRecipeList 患者创建失败，doctorId={}, clinicOrgan={}",
                            request.getDoctorId(), clinicOrgan);
                    response.setMsg("患者创建失败");
                    return response;
                } else {
                    recipeInfo.put("mpiId", patient.getMpiId());
                }
            }

            RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
            List<Recipe> recipeList = recipeDAO.findHosRecipe(request.getDoctorId(), patient.getMpiId(), clinicOrgan,
                    request.getStart(), request.getLimit());
            List<RecipeBean> backList = Lists.newArrayList();
            if (CollectionUtils.isNotEmpty(recipeList)) {
                backList = ObjectCopyUtils.convert(recipeList, RecipeBean.class);
            }
            recipeInfo.put("list", backList);
            response.setCode(RecipeCommonBaseTO.SUCCESS);
        } else {
            response.setMsg("请求对象为空");
            return response;
        }

        return response;
    }
}
