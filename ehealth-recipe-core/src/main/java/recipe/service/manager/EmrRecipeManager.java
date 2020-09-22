package recipe.service.manager;

import com.ngari.patient.dto.DepartmentDTO;
import com.ngari.patient.service.DepartmentService;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeExtend;
import com.ngari.recipe.recipe.model.RecipeExtendBean;
import ctd.persistence.DAOFactory;
import ctd.util.JSONUtils;
import eh.cdr.api.service.IDocIndexService;
import eh.cdr.api.vo.DocIndexBean;
import eh.cdr.api.vo.DocIndexExtBean;
import eh.cdr.api.vo.MedicalDetailBean;
import eh.cdr.api.vo.MedicalInfoBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import recipe.bean.EmrDetailDTO;
import recipe.bean.EmrDetailValueDTO;
import recipe.dao.RecipeExtendDAO;
import recipe.util.ByteUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 * @author yinsheng
 * @date 2020\8\18 0018 08:57
 */
@Service
public class EmrRecipeManager {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    @Resource
    private IDocIndexService docIndexService;
    @Resource
    private DepartmentService departmentService;

    /**
     * 保存或更新电子病历
     *
     * @param recipeExt
     */
    public void saveMedicalInfo(Recipe recipe, RecipeExtendBean recipeExt) {
        logger.info("EmrRecipeManager saveMedicalInfo recipe:{},recipeExt:{}", JSONUtils.toString(recipe), JSONUtils.toString(recipeExt));
        try {
            if (recipeExt.getDocIndexId() == null) {
                //保存电子病历
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
                //业务类型 1 处方 2 复诊 3 检查 4 检验
                docIndexExtBean.setBussType(1);
                docIndexExtBean.setBussId(recipe.getRecipeId());
                docIndexExtBeanList.add(docIndexExtBean);
                medicalInfoBean.setDocIndexExtBeanList(docIndexExtBeanList);
                //设置病历详情
                MedicalDetailBean medicalDetailBean = new MedicalDetailBean();
                setMedicalDetailBean(recipe, recipeExt, medicalDetailBean);
                medicalInfoBean.setMedicalDetailBean(medicalDetailBean);
                logger.info("doWithSavaOrUpdateEmr 新增电子病历 medicalDetailBean 入参:{}.", JSONUtils.toString(medicalInfoBean));
                Integer docId = docIndexService.saveMedicalInfo(medicalInfoBean);
                recipeExt.setDocIndexId(docId);
            } else {
                //更新电子病历
                RecipeExtendDAO recipeExtendDAO = DAOFactory.getDAO(RecipeExtendDAO.class);
                RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipeExt.getRecipeId());
                MedicalDetailBean medicalDetailBean = new MedicalDetailBean();
                medicalDetailBean.setDocIndexId(recipeExtend.getDocIndexId());
                setMedicalDetailBean(recipe, recipeExt, medicalDetailBean);
                logger.info("doWithSavaOrUpdateEmr 更新电子病历 medicalDetailBean 入参:{}.", JSONUtils.toString(medicalDetailBean));
                docIndexService.updateMedicalDetail(medicalDetailBean);
            }
        } catch (Exception e) {
            logger.error("doWithSavaOrUpdateEmr 电子病历保存或更新失败", e);
        }
        logger.info("EmrRecipeManager saveMedicalInfo end recipeExt={}", recipeExt.getDocIndexId());
    }

    /**
     * 组织电子病历明细数据 主要为了兼容老版本
     *
     * @param recipe
     * @param recipeExt
     * @param medicalDetailBean
     */
    private void setMedicalDetailBean(Recipe recipe, RecipeExtendBean recipeExt, MedicalDetailBean medicalDetailBean) {
        List<EmrDetailDTO> detail = new ArrayList<>();
        //设置主诉
        detail.add(new EmrDetailDTO("complain", "主诉", "textArea", isEmpty(recipeExt.getMainDieaseDescribe()), true));
        //设置现病史
        detail.add(new EmrDetailDTO("currentMedicalHistory", "现病史", "textArea", isEmpty(recipeExt.getCurrentMedical()), false));
        //设置既往史
        detail.add(new EmrDetailDTO("pastMedicalHistory", "既往史", "textArea", isEmpty(recipeExt.getHistroyMedical()), false));
        //设置过敏史
        detail.add(new EmrDetailDTO("allergyHistory", "过敏史", "textArea", isEmpty(recipeExt.getAllergyMedical()), false));
        //设置体格检查
        detail.add(new EmrDetailDTO("physicalExamination", "体格检查", "textArea", isEmpty(recipeExt.getPhysicalCheck()), false));
        //设置处理方法
        detail.add(new EmrDetailDTO("processingMethod", "处理方法", "textArea", isEmpty(recipeExt.getHandleMethod()), false));
        //设置注意事项
        detail.add(new EmrDetailDTO("memo", "注意事项", "textArea", isEmpty(recipe.getMemo()), false));
        //设置诊断
        String[] diseaseNames = recipe.getOrganDiseaseName().split(ByteUtils.SEMI_COLON_CH);
        String[] diseaseIds = recipe.getOrganDiseaseId().split(ByteUtils.SEMI_COLON_CH);
        detail.add(new EmrDetailDTO("diagnosis", "诊断", "multiSearch", getEmrDetailValueDTO(diseaseNames, diseaseIds), true));
        //设置中医证候
        String[] symptomNames = recipeExt.getSymptomName().split(ByteUtils.SEMI_COLON_EN);
        String[] symptomIds = recipeExt.getSymptomId().split(ByteUtils.SEMI_COLON_EN);
        detail.add(new EmrDetailDTO("tcmSyndrome", "中医证候", "multiSearch", getEmrDetailValueDTO(symptomNames, symptomIds), false));

        medicalDetailBean.setDetail(JSONUtils.toString(detail));
    }

    /**
     * 组织特殊value字段
     *
     * @param names
     * @param ids
     * @return
     */
    private String getEmrDetailValueDTO(String[] names, String[] ids) {
        List<EmrDetailValueDTO> diagnosisValues = new LinkedList<>();
        if (null == names || null == ids || 0 == names.length || 0 == ids.length) {
            return "";
        }
        for (int i = 0; i < names.length; i++) {
            EmrDetailValueDTO diagnosisValue = new EmrDetailValueDTO();
            diagnosisValue.setName(names[i]);
            diagnosisValue.setCode(ids[i]);
            diagnosisValues.add(diagnosisValue);
        }
        return JSONUtils.toString(diagnosisValues);
    }


    private String isEmpty(String parame) {
        if (StringUtils.isEmpty(parame)) {
            return "";
        } else {
            return parame;
        }
    }


}
