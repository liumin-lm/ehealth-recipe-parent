package recipe.atop.doctor;

import com.ngari.recipe.dto.OutPatientRecordResDTO;
import com.ngari.recipe.dto.WriteDrugRecipeDTO;
import com.ngari.recipe.recipe.model.RecipeBean;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.atop.BaseAtop;
import recipe.core.api.IRecipeBusinessService;
import recipe.core.api.IRevisitBusinessService;
import recipe.vo.doctor.ValidateDetailVO;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 开处方服务入口类
 *
 * @author fuzi
 */
@RpcBean("writeRecipeDoctorAtop")
public class WriteRecipeDoctorAtop extends BaseAtop {

    @Autowired
    private IRevisitBusinessService iRevisitBusinessService;
    @Autowired
    private IRecipeBusinessService recipeBusinessService;

    /**
     * 获取院内门诊
     *
     * @param mpiId    患者唯一标识
     * @param organId  机构ID
     * @param doctorId 医生ID
     * @return 院内门诊处方列表
     */
    @RpcService
    public List<WriteDrugRecipeDTO> findWriteDrugRecipeByRevisitFromHis(String mpiId, Integer organId, Integer doctorId) {
        return iRevisitBusinessService.findWriteDrugRecipeByRevisitFromHis(mpiId, organId, doctorId);
    }

    /**
     * 医生二次确认药师审核结果-不通过
     *
     * @param recipeId 处方id
     * @return
     */
    @RpcService
    public Boolean confirmAgain(Integer recipeId) {
        return recipeBusinessService.confirmAgain(recipeId);
    }

    /**
     * 获取有效门诊记录
     *
     * @param mpiId    患者唯一标识
     * @param organId  机构ID
     * @param doctorId 医生ID
     * @return 门诊记录
     */
    @RpcService
    public OutPatientRecordResDTO findOutPatientRecordFromHis(String mpiId, Integer organId, Integer doctorId) {
        return recipeBusinessService.findOutPatientRecordFromHis(mpiId, organId, doctorId);
    }

    /**
     * 靶向药拆方，无靶向药 返回空
     *
     * @param validateDetailVO 处方信息
     * @return 处方组号
     */
    @RpcService
    public String splitDrugRecipe(ValidateDetailVO validateDetailVO) {
        validateAtop(validateDetailVO, validateDetailVO.getRecipeBean(), validateDetailVO.getRecipeDetails());
        List<RecipeDetailBean> detailBeanList = validateDetailVO.getRecipeDetails();
        List<RecipeDetailBean> targetDrugList = detailBeanList.stream().filter(a -> Integer.valueOf(1).equals(a.getTargetedDrugType())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(targetDrugList)) {
            return "";
        }
        String uuid = UUID.randomUUID().toString();
        RecipeBean recipeBean = validateDetailVO.getRecipeBean();
        recipeBean.setGroupCode(uuid);
        recipeBean.setRecipeExtend(validateDetailVO.getRecipeExtendBean());
        //靶向药
        targetDrugList.forEach(a -> recipeBusinessService.saveRecipeData(recipeBean, Collections.singletonList(a)));
        //非靶向药
        List<RecipeDetailBean> details = detailBeanList.stream().filter(a -> Integer.valueOf(0).equals(a.getTargetedDrugType())).collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(details)) {
            recipeBusinessService.saveRecipeData(recipeBean, details);
        }
        return uuid;
    }

    /**
     * 查询同组处方
     *
     * @param groupCode 处方组号
     * @param type      0： 默认全部 1：查询暂存，2查询可撤销
     * @return 处方id集合
     */
    @RpcService
    public List<Integer> recipeByGroupCode(String groupCode, Integer type) {
        validateAtop(groupCode);
        return recipeBusinessService.recipeByGroupCode(groupCode, type);
    }
}
