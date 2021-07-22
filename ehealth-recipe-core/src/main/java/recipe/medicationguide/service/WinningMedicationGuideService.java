package recipe.medicationguide.service;


import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.ngari.patient.dto.OrganDTO;
import com.ngari.patient.service.OrganService;
import com.ngari.recipe.entity.MedicationGuide;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeExtend;
import com.ngari.recipe.recipe.model.RecipeBean;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import ctd.dictionary.Dictionary;
import ctd.dictionary.DictionaryController;
import ctd.persistence.DAOFactory;
import ctd.persistence.exception.DAOException;
import ctd.util.BeanUtils;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.ApplicationUtils;
import recipe.constant.ErrorCode;
import recipe.dao.MedicationGuideDAO;
import recipe.dao.RecipeExtendDAO;
import recipe.manager.EmrRecipeManager;
import recipe.medicationguide.bean.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;



/**
 * created by shiyuping on 2019/10/28
 */
@RpcBean
public class WinningMedicationGuideService implements IMedicationGuideService {
    private static final Logger LOGGER = LoggerFactory.getLogger(WinningMedicationGuideService.class);
    private OrganService organService = ApplicationUtils.getBasicService(OrganService.class);
    @Autowired
    private RecipeExtendDAO recipeExtendDAO;

    @Override
    @RpcService
    public Map<String, Object> getHtml5LinkInfo(PatientInfoDTO patient, RecipeBean recipeBean, List<RecipeDetailBean> recipeDetails, Integer reqType) {
        LOGGER.info("WinningMedicationGuideService.getHtml5LinkInfo req patient={} recipeBean={} recipeDetails={} reqType={}",patient,recipeBean,recipeDetails,reqType);
        if (null != recipeBean && null != recipeBean.getRecipeId()) {
            RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipeBean.getRecipeId());

            Recipe recipeNew = new Recipe();
            BeanUtils.copy(recipeBean, recipeNew);
            EmrRecipeManager.getMedicalInfo(recipeNew, recipeExtend);
            recipeBean.setOrganDiseaseName(recipeNew.getOrganDiseaseName());
            recipeBean.setOrganDiseaseId(recipeNew.getOrganDiseaseId());
        }
        //拼接请求参数
        WinningMedicationGuideReqDTO requestParam = assembleRequestParam(patient, recipeBean, recipeDetails, reqType);
        //获取请求url
        MedicationGuideDAO medicationGuideDAO = DAOFactory.getDAO(MedicationGuideDAO.class);
        MedicationGuide guide = medicationGuideDAO.getByCallSys("Winning");
        if (StringUtils.isEmpty(guide.getBusinessUrl())) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "未配置第三方businessUrl");
        }
        String url = getHtml5LinkHttpRequest(guide.getBusinessUrl(), requestParam);
        //获取模板参数
        Map<String,Object> result = Maps.newHashMap();
        result.put("url",url);
        List<DrugUseDTO> drugUseList = requestParam.getDrugUseList();
        StringBuilder drugInfo = new StringBuilder();
        for (DrugUseDTO drugUseDTO : drugUseList) {
            if (StringUtils.isEmpty(drugUseDTO.getDrugUsed())){
                drugInfo.append(drugUseDTO.getDrugName()).append("\n");
            }else {
                drugInfo.append(drugUseDTO.getDrugName()).append("\n").append("用法:").append(drugUseDTO.getDrugUsed()).append("\n");
            }
        }
        result.put("drugInfo",drugInfo);
        return result;
    }

    private WinningMedicationGuideReqDTO assembleRequestParam(PatientInfoDTO patient, RecipeBean recipeBean, List<RecipeDetailBean> recipeDetails, Integer reqType) {
        WinningMedicationGuideReqDTO req = new WinningMedicationGuideReqDTO();
        try {
            req.setPatientInfo(patient);
            //组装医院信息数据
            HospInfoDTO hospParam = new HospInfoDTO();
            hospParam.setId(String.valueOf(recipeBean.getClinicOrgan()));
            OrganDTO organDTO = organService.getByOrganId(recipeBean.getClinicOrgan());
            if (organDTO != null){
                hospParam.setOrganizationalCode(organDTO.getOrganizeCode());
                hospParam.setAreaCode(organDTO.getAddrArea());
            }
            hospParam.setName(recipeBean.getOrganName());
            req.setHospInfo(hospParam);
            //组装药品信息数据
            List<DrugUseDTO> drugUseParam = Lists.newArrayList();
            for (RecipeDetailBean detail : recipeDetails) {
                DrugUseDTO drugUseDTO = new DrugUseDTO();
                drugUseDTO.setDrugName(detail.getDrugName());
                drugUseDTO.setDrugCode(detail.getOrganDrugCode());
                //口服/每次75mg/每日两次
                if (detail.getRecipeDetailId() != null) {
                    Dictionary usingRateDic = DictionaryController.instance().get("eh.cdr.dictionary.UsingRate");
                    Dictionary usePathwaysDic = DictionaryController.instance().get("eh.cdr.dictionary.UsePathways");
                    detail.setUsePathways(detail.getUsePathwaysTextFromHis()!=null?detail.getUsePathwaysTextFromHis():usePathwaysDic.getText(detail.getUsePathways()));
                    detail.setUsingRate(detail.getUsingRateTextFromHis()!=null?detail.getUsingRateTextFromHis():usingRateDic.getText(detail.getUsingRate()));
                }
                if (StringUtils.isEmpty(detail.getUsePathways())||StringUtils.isEmpty(detail.getUsingRate())||detail.getUseDose()==null||StringUtils.isEmpty(detail.getUseDoseUnit())){
                    drugUseDTO.setDrugUsed("");
                }else {
                    drugUseDTO.setDrugUsed(detail.getUsePathways() + "/每次" + detail.getUseDose() + detail.getUseDoseUnit() + "/" + detail.getUsingRate());
                }
                drugUseParam.add(drugUseDTO);
            }

            req.setDrugUseList(drugUseParam);
            //组装诊断信息数据
            List<DiagnosisInfoDTO> diagnosisParam = Lists.newArrayList();
            List<String> icd10Lists = Splitter.on("；").splitToList(recipeBean.getOrganDiseaseId());
            List<String> nameLists = Splitter.on("；").splitToList(recipeBean.getOrganDiseaseName());
            if (icd10Lists.size() == nameLists.size()) {
                for (int i = 0; i < icd10Lists.size(); i++) {
                    DiagnosisInfoDTO diagnosis = new DiagnosisInfoDTO();
                    diagnosis.setDiagnosisCode(icd10Lists.get(i));
                    diagnosis.setDiagnosisName(nameLists.get(i));
                    diagnosisParam.add(diagnosis);
                }
            }
            req.setDiagnosisInfo(diagnosisParam);
            //请求类型
            req.setReqType(reqType);
        } catch (Exception e) {
            LOGGER.error("assembleRequestParam error",e);
        }

        return req;
    }

    private String getHtml5LinkHttpRequest(String url, WinningMedicationGuideReqDTO request) {
        String html5Link = "";
        //推送给卫宁用药指导
        CloseableHttpClient httpClient = HttpClients.createDefault();
        try {
            HttpPost httpPost = new HttpPost(url);

            //组装请求参数
            String requestStr = JSONUtils.toString(request);
            LOGGER.info("getHtml5LinkHttpRequest request={}", requestStr);
            StringEntity requestEntry = new StringEntity(requestStr, ContentType.APPLICATION_JSON);
            httpPost.setEntity(requestEntry);

            //获取响应消息
            CloseableHttpResponse response = httpClient.execute(httpPost);
            HttpEntity httpEntity = response.getEntity();
            String responseStr = EntityUtils.toString(httpEntity);
            LOGGER.info("getHtml5LinkHttpRequest response={}", responseStr);
            if (StringUtils.isNotEmpty(responseStr)&&responseStr.contains("http")){
                html5Link = responseStr.substring(1,responseStr.length()-1);
            }
            //关闭 HttpEntity 输入流
            EntityUtils.consume(httpEntity);
            response.close();
        } catch (Exception e) {
            LOGGER.error("getHtml5LinkHttpRequest error", e);
        } finally {
            try {
                httpClient.close();
            } catch (IOException e) {
                LOGGER.warn("getHtml5LinkHttpRequest httpClient close error", e);
            }
        }
        return html5Link;
    }
}
