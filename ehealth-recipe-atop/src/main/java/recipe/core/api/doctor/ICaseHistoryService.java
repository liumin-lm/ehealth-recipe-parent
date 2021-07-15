package recipe.core.api.doctor;

import com.ngari.recipe.vo.CaseHistoryVO;
import recipe.vo.second.MedicalDetailVO;

/**
 * 电子病历处理实现类
 *
 * @author fuzi
 */
public interface ICaseHistoryService {

    /**
     * 获取电子病历数据
     * actionType，为查看：则先根据clinicId查电子病历，若没有则在根据docIndexId查电子病历信息，返回前端结果
     * actionType，为copy：则recipeId不为空则根据recipeId查docIndexId再调用copy接口，否则用clinicId调用查看接口返回前端结果
     *
     * @param caseHistoryVO 电子病历查询对象
     */
    MedicalDetailVO getDocIndexInfo(CaseHistoryVO caseHistoryVO);
}
