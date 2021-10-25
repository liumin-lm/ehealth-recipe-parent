package recipe.factoryManager.button;

import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.vo.CaseHistoryVO;
import eh.cdr.api.vo.MedicalDetailBean;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import recipe.core.api.doctor.ICaseHistoryBusinessService;
import recipe.manager.EmrRecipeManager;
import recipe.util.ValidateUtil;
import recipe.vo.second.EmrConfigVO;
import recipe.vo.second.MedicalDetailVO;

import java.util.List;

/**
 * 电子病历处理实现类
 *
 * @author fuzi
 */
@Service
public class CaseHistoryBusinessService implements ICaseHistoryBusinessService {
    /**
     * 操作类型 1：查看，2：copy
     */
    private static final Integer DOC_ACTION_TYPE_INFO = 1;
    private static final Integer DOC_ACTION_TYPE_COPY = 2;

    @Autowired
    private EmrRecipeManager emrRecipeManager;

    /**
     * 获取电子病历数据
     * actionType，为查看：则先根据clinicId查电子病历，若没有则在根据docIndexId查电子病历信息，返回前端结果
     * actionType，为copy：则recipeId不为空则根据recipeId查docIndexId再调用copy接口，否则用clinicId调用查看接口返回前端结果
     *
     * @param caseHistoryVO 电子病历查询对象
     */
    @Override
    public MedicalDetailVO getDocIndexInfo(CaseHistoryVO caseHistoryVO) {
        MedicalDetailBean medicalDetailBean = null;
        //查看
        if (DOC_ACTION_TYPE_INFO.equals(caseHistoryVO.getActionType())) {
            MedicalDetailBean emrDetails = emrRecipeManager.getEmrDetailsByClinicId(caseHistoryVO.getClinicId());
            if (!StringUtils.isEmpty(emrDetails)) {
                medicalDetailBean = emrDetails;
            } else {
                medicalDetailBean = emrRecipeManager.getEmrDetails(caseHistoryVO.getDocIndexId());
            }
        }
        //copy
        if (DOC_ACTION_TYPE_COPY.equals(caseHistoryVO.getActionType())) {
            if (ValidateUtil.integerIsEmpty(caseHistoryVO.getRecipeId())) {
                medicalDetailBean = emrRecipeManager.getEmrDetailsByClinicId(caseHistoryVO.getClinicId());
            } else {
                medicalDetailBean = emrRecipeManager.copyEmrDetails(caseHistoryVO.getRecipeId(), caseHistoryVO.getClinicId());
            }
        }
        if (null == medicalDetailBean) {
            return null;
        }
        MedicalDetailVO medicalDetailVO = new MedicalDetailVO();
        BeanUtils.copyProperties(medicalDetailBean, medicalDetailVO);
        List<EmrConfigVO> detailList = ObjectCopyUtils.convert(medicalDetailBean.getDetailList(), EmrConfigVO.class);
        medicalDetailVO.setDetailList(detailList);
        return medicalDetailVO;
    }
}
