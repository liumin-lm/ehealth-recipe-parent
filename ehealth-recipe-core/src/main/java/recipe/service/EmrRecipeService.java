package recipe.service;

import com.ngari.patient.dto.DepartmentDTO;
import com.ngari.patient.service.BasicAPI;
import com.ngari.patient.service.DepartmentService;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeExtend;
import com.ngari.recipe.recipe.model.RecipeExtendBean;
import ctd.persistence.DAOFactory;
import ctd.util.AppContextHolder;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import eh.cdr.api.service.IDocIndexService;
import eh.cdr.api.vo.DocIndexBean;
import eh.cdr.api.vo.DocIndexExtBean;
import eh.cdr.api.vo.MedicalDetailBean;
import eh.cdr.api.vo.MedicalInfoBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import recipe.dao.RecipeExtendDAO;

import java.util.*;

/**
 * @author yinsheng
 * @date 2020\8\18 0018 08:57
 */
@RpcBean("emrRecipeService")
public class EmrRecipeService {
    private static final Logger LOGGER = LoggerFactory.getLogger(EmrRecipeService.class);

    /**
     * 保存或更新电子病历
     * @param recipeExt
     */
    @RpcService
    public void doWithSavaOrUpdateEmr(Recipe recipe, RecipeExtendBean recipeExt){
        try{
            if (recipeExt.getDocIndexId() == null) {
                //保存电子病历
                IDocIndexService docIndexService = AppContextHolder.getBean("ecdr.docIndexService", IDocIndexService.class);
                if (ObjectUtils.isEmpty(recipeExt.getRecipeId())) {
                    MedicalInfoBean medicalInfoBean = new MedicalInfoBean();
                    //设置病历索引信息
                    DocIndexBean docIndexBean = new DocIndexBean();
                    docIndexBean.setClinicId(recipe.getClinicId());
                    docIndexBean.setMpiid(recipe.getMpiid());
                    docIndexBean.setDocClass(11);
                    docIndexBean.setDocType("0");
                    docIndexBean.setDocTitle("电子处方病历");
                    docIndexBean.setDocSummary("电子处方病历");
                    docIndexBean.setCreateOrgan(recipe.getClinicOrgan());
                    docIndexBean.setCreateDepart(recipe.getDepart());
                    DepartmentService departmentService = BasicAPI.getService(DepartmentService.class);
                    DepartmentDTO department = departmentService.get(recipe.getDepart());
                    if (department != null) {
                        docIndexBean.setDepartName(department.getName());
                    }
                    docIndexBean.setCreateDoctor(recipe.getDoctor());
                    docIndexBean.setDoctorName(recipe.getDoctorName());
                    docIndexBean.setCreateDate(new Date());
                    docIndexBean.setGetDate(new Date());
                    docIndexBean.setDoctypeName("电子处方病历");
                    docIndexBean.setDocStatus(4);
                    docIndexBean.setDocFlag(0);
                    docIndexBean.setOrganNameByUser(recipe.getOrganName());
                    docIndexBean.setClinicPersonName(recipe.getPatientName());
                    docIndexBean.setLastModify(new Date());
                    medicalInfoBean.setDocIndexBean(docIndexBean);
                    //设置病历索引扩展信息
                    List<DocIndexExtBean> docIndexExtBeanList = new ArrayList<>();
                    DocIndexExtBean docIndexExtBean = new DocIndexExtBean();
                    docIndexExtBean.setBussType(1);
                    docIndexExtBean.setBussId(recipe.getRecipeId());
                    docIndexExtBeanList.add(docIndexExtBean);
                    medicalInfoBean.setDocIndexExtBeanList(docIndexExtBeanList);
                    //设置病历详情
                    MedicalDetailBean medicalDetailBean = new MedicalDetailBean();
                    setMedicalDetailBean(recipe, recipeExt, medicalDetailBean);
                    medicalInfoBean.setMedicalDetailBean(medicalDetailBean);

                    LOGGER.info("doWithSavaOrUpdateEmr 新增电子病历 medicalDetailBean 入参:{}.", JSONUtils.toString(medicalInfoBean));
                    Integer docId = docIndexService.saveMedicalInfo(medicalInfoBean);
                    recipeExt.setDocIndexId(docId);
                } else {
                    //更新电子病历
                    RecipeExtendDAO recipeExtendDAO = DAOFactory.getDAO(RecipeExtendDAO.class);
                    RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipeExt.getRecipeId());
                    if (recipeExtend.getDocIndexId() != null) {
                        MedicalDetailBean medicalDetailBean = new MedicalDetailBean();
                        medicalDetailBean.setDocIndexId(recipeExtend.getDocIndexId());
                        setMedicalDetailBean(recipe, recipeExt, medicalDetailBean);
                        LOGGER.info("doWithSavaOrUpdateEmr 更新电子病历 medicalDetailBean 入参:{}.", JSONUtils.toString(medicalDetailBean));
                        docIndexService.updateMedicalDetail(medicalDetailBean);
                    }
                }
            }
        }catch(Exception e){
            LOGGER.info("doWithSavaOrUpdateEmr 电子病历保存或更新失败, mgs:{}.", e.getMessage(), e);
        }
    }

    private void setMedicalDetailBean(Recipe recipe, RecipeExtendBean recipeExt, MedicalDetailBean medicalDetailBean) {
        List<Map<String, Object>> detail = new ArrayList<>();
        //设置主诉
        Map<String, Object> complain = new HashMap<>();
        Map<String, String> complainValue = new HashMap<>();
        complainValue.put("type", "text");
        complainValue.put("name", "主诉");
        complainValue.put("value", isEmpty(recipeExt.getMainDieaseDescribe()));
        complain.put("complain", complainValue);
        detail.add(complain);
        //设置现病史
        Map<String, Object> currentMedicalHistory = new HashMap<>();
        Map<String, String> currentMedicalHistoryValue = new HashMap<>();
        currentMedicalHistoryValue.put("type", "text");
        currentMedicalHistoryValue.put("name", "现病史");
        currentMedicalHistoryValue.put("value", isEmpty(recipeExt.getCurrentMedical()));
        currentMedicalHistory.put("currentMedicalHistory", currentMedicalHistoryValue);
        detail.add(currentMedicalHistory);
        //设置既往史
        Map<String, Object> pastMedicalHistory = new HashMap<>();
        Map<String, String> pastMedicalHistoryValue = new HashMap<>();
        pastMedicalHistoryValue.put("type", "text");
        pastMedicalHistoryValue.put("name", "既往史");
        pastMedicalHistoryValue.put("value", isEmpty(recipeExt.getHistroyMedical()));
        pastMedicalHistory.put("pastMedicalHistory", pastMedicalHistoryValue);
        detail.add(pastMedicalHistory);
        //设置过敏史
        Map<String, Object> allergyHistory = new HashMap<>();
        Map<String, String> allergyHistoryValue = new HashMap<>();
        allergyHistoryValue.put("type", "text");
        allergyHistoryValue.put("name", "过敏史");
        allergyHistoryValue.put("value", isEmpty(recipeExt.getAllergyMedical()));
        allergyHistory.put("allergyHistory", allergyHistoryValue);
        detail.add(allergyHistory);
        //设置体格检查
        Map<String, Object> physicalExamination = new HashMap<>();
        Map<String, String> physicalExaminationValue = new HashMap<>();
        physicalExaminationValue.put("type", "text");
        physicalExaminationValue.put("name", "体格检查");
        physicalExaminationValue.put("value", isEmpty(recipeExt.getPhysicalCheck()));
        physicalExamination.put("physicalExamination", physicalExaminationValue);
        detail.add(physicalExamination);
        //设置诊断
        Map<String, Object> diagnosis = new HashMap<>();
        Map<String, String> diagnosisValue = new HashMap<>();
        diagnosisValue.put("type", "text");
        diagnosisValue.put("name", "诊断");
        diagnosisValue.put("value", isEmpty(recipe.getOrganDiseaseName()));
        diagnosis.put("diagnosis", diagnosisValue);
        detail.add(diagnosis);
        //设置中医证候
        Map<String, Object> tcmSyndrome = new HashMap<>();
        Map<String, String> tcmSyndromeValue = new HashMap<>();
        tcmSyndromeValue.put("type", "text");
        tcmSyndromeValue.put("name", "中医证候");
        tcmSyndromeValue.put("value", isEmpty(recipeExt.getSymptomName()));
        tcmSyndrome.put("tcmSyndrome", tcmSyndromeValue);
        detail.add(tcmSyndrome);
        //设置处理方法
        Map<String, Object> processingMethod = new HashMap<>();
        Map<String, String> processingMethodValue = new HashMap<>();
        processingMethodValue.put("type", "text");
        processingMethodValue.put("name", "处理方法");
        processingMethodValue.put("value", isEmpty(recipeExt.getHandleMethod()));
        processingMethod.put("processingMethod", processingMethodValue);
        detail.add(processingMethod);
        //设置注意事项
        Map<String, Object> memo = new HashMap<>();
        Map<String, String> memoValue = new HashMap<>();
        memoValue.put("type", "text");
        memoValue.put("name", "注意事项");
        memoValue.put("value", isEmpty(recipe.getMemo()));
        memo.put("memo", memoValue);
        detail.add(memo);

        medicalDetailBean.setDetail(JSONUtils.toString(detail));
    }

    private String isEmpty(String parame){
        if (StringUtils.isEmpty(parame)) {
            return "";
        } else {
            return parame;
        }
    }


}
