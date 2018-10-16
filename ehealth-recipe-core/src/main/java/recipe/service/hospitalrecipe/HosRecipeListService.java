package recipe.service.hospitalrecipe;

import com.google.common.collect.Multimap;
import com.ngari.base.BaseAPI;
import com.ngari.base.patient.model.PatientBean;
import com.ngari.base.patient.service.IPatientExtendService;
import com.ngari.recipe.common.RecipeCommonBaseTO;
import com.ngari.recipe.common.RecipeStandardResTO;
import com.ngari.recipe.common.utils.VerifyUtils;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.service.hospitalrecipe.dto.HosRecipeListRequest;
import recipe.util.MapValueUtil;

import java.util.List;
import java.util.Map;

/**
 * company: ngarihealth
 * author: 0184/yu_yun
 * date:2018/10/16
 */
@RpcBean("hosRecipeListService")
public class HosRecipeListService {

    private static final Logger LOG = LoggerFactory.getLogger(HosRecipeListService.class);

    @RpcService
    public RecipeStandardResTO<Map> findHistroyRecipeList(HosRecipeListRequest request){
        RecipeStandardResTO<Map> response = RecipeStandardResTO.getRequest(Map.class);
        response.setCode(RecipeCommonBaseTO.FAIL);
        if(null != request) {
            //校验参数
            try {
                Multimap<String, String> detailVerifyMap = VerifyUtils.verify(request);
                if (!detailVerifyMap.keySet().isEmpty()) {
                    response.setMsg(detailVerifyMap.toString());
                    return response;
                }
            } catch (Exception e) {
                LOG.warn("HosRecipeListService 请求对象异常数据. HosRecipeListRequest={}", JSONUtils.toString(request), e);
                response.setMsg("请求对象异常数据");
                return response;
            }

            //先查询就诊人是否存在
            PatientBean patient = null;
            try {
//                IPatientExtendService patientExtendService = BaseAPI.getService(IPatientExtendService.class);
//                List<PatientBean> patList = patientExtendService.findPatient4Doctor(request.getDoctorId(), request.getCertificate());
//                if (CollectionUtils.isEmpty(patList)) {
                    //不存在该患者则需要添加
//                    patient = new PatientBean();
//                    patient.setPatientName(hospitalRecipeDTO.getPatientName());
//                    patient.setPatientSex(hospitalRecipeDTO.getPatientSex());
//                    patient.setCertificateType(Integer.valueOf(hospitalRecipeDTO.getCertificateType()));
//                    patient.setCertificate(hospitalRecipeDTO.getCertificate());
//                    patient.setAddress(hospitalRecipeDTO.getPatientAddress());
//                    patient.setMobile(hospitalRecipeDTO.getPatientTel());
//                    //创建就诊人
//                    patient = patientExtendService.addPatient4DoctorApp(patient, 0, recipe.getDoctor());
//                } else {
//                    patient = patList.get(0);
//                }
            } catch (Exception e) {
//                LOG.warn("createPrescription 处理就诊人异常，doctorNumber={}, clinicOrgan={}",
//                        hospitalRecipeDTO.getDoctorNumber(), clinicOrgan, e);
            } finally {
//                if (null == patient || StringUtils.isEmpty(patient.getMpiId())) {
//                    LOG.warn("createPrescription 患者创建失败，doctorNumber={}, clinicOrgan={}",
//                            hospitalRecipeDTO.getDoctorNumber(), clinicOrgan);
//                    result.setMsg("患者创建失败");
//                    return result;
//                } else {
//                    recipe.setPatientName(patient.getPatientName());
//                    recipe.setPatientStatus(1); //有效
//                    recipe.setMpiid(patient.getMpiId());
//                }
            }


        }else{
            response.setMsg("请求对象为空");
            return response;
        }

        return response;
    }
}
