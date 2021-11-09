package recipe.atop.patient;

import com.ngari.recipe.basic.ds.PatientVO;
import com.ngari.recipe.dto.RecipeInfoDTO;
import com.ngari.recipe.recipe.model.RecipeBean;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import ctd.persistence.exception.DAOException;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import eh.utils.DateConversion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import recipe.atop.BaseAtop;
import recipe.constant.ErrorCode;
import recipe.core.api.doctor.ITherapyRecipeBusinessService;
import recipe.util.ObjectCopyUtils;
import recipe.util.ValidateUtil;
import recipe.vo.doctor.RecipeInfoVO;
import recipe.vo.doctor.RecipeTherapyVO;
import recipe.vo.doctor.TherapyRecipePageVO;

import java.util.LinkedList;
import java.util.List;

/**
 * 诊疗处方服务患者端入口类
 *
 * @author yinsheng
 */
@RpcBean(value = "therapyRecipePatientAtop")
public class TherapyRecipePatientAtop extends BaseAtop {

    @Autowired
    private ITherapyRecipeBusinessService therapyRecipeBusinessService;

    /**
     * 获取诊疗处方列表
     *
     * @param recipeTherapyVO 诊疗处方对象
     * @param start           页数
     * @param limit           每页条数
     * @return key 复诊id
     */
    @RpcService
    public List<RecipeInfoVO> therapyRecipeList(RecipeTherapyVO recipeTherapyVO, int start, int limit) {
        validateAtop(recipeTherapyVO);
        if (ValidateUtil.validateObjects(recipeTherapyVO.getMpiId())) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "入参错误");
        }
        List<RecipeInfoDTO> recipeInfoList = therapyRecipeBusinessService.therapyRecipeListForPatient(recipeTherapyVO.getMpiId(), start, limit);
        List<RecipeInfoVO> result = new LinkedList<>();

        recipeInfoList.forEach(a -> {
            RecipeInfoVO recipeInfoVO = new RecipeInfoVO();
            recipeInfoVO.setPatientVO(ObjectCopyUtils.convert(a.getPatientBean(), PatientVO.class));
            recipeInfoVO.setRecipeTherapyVO(ObjectCopyUtils.convert(a.getRecipeTherapy(), RecipeTherapyVO.class));

            RecipeBean recipeBean = new RecipeBean();
            recipeBean.setRecipeId(a.getRecipe().getRecipeId());
            recipeBean.setClinicId(a.getRecipe().getClinicId());
            recipeBean.setOrganDiseaseName(a.getRecipe().getOrganDiseaseName());
            recipeBean.setCreateDate(a.getRecipe().getCreateDate());
            if (null != recipeBean.getCreateDate()) {
                recipeBean.setWxDisplayTime(DateConversion.convertRequestDateForBussNew(recipeBean.getCreateDate()));
            }
            recipeInfoVO.setRecipeBean(recipeBean);

            List<RecipeDetailBean> recipeDetails = new LinkedList<>();
            if (!CollectionUtils.isEmpty(a.getRecipeDetails())) {
                a.getRecipeDetails().forEach(b -> {
                    RecipeDetailBean recipeDetailBean = new RecipeDetailBean();
                    recipeDetailBean.setDrugName(b.getDrugName());
                    recipeDetailBean.setType(b.getType());
                    recipeDetails.add(recipeDetailBean);
                });
                recipeInfoVO.setRecipeDetails(recipeDetails);
            }
            result.add(recipeInfoVO);
        });
        return result;
    }
}
