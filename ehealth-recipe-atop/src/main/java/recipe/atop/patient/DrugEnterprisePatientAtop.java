package recipe.atop.patient;

import com.ngari.recipe.dto.EnterpriseStock;
import com.ngari.recipe.entity.OrganDrugsSaleConfig;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.Recipedetail;
import com.ngari.recipe.recipe.model.RecipeBean;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import ctd.persistence.exception.DAOException;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.atop.BaseAtop;
import recipe.constant.ErrorCode;
import recipe.core.api.IStockBusinessService;
import recipe.util.ObjectCopyUtils;
import recipe.util.RecipeUtil;
import recipe.vo.doctor.ValidateDetailVO;
import recipe.vo.greenroom.OrganDrugsSaleConfigVo;
import recipe.vo.patient.MedicineStationVO;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

/**
 * 药企相关入口
 *
 * @author fuzi
 */
@RpcBean(value = "drugEnterprisePatientAtop")
public class DrugEnterprisePatientAtop extends BaseAtop {

    @Autowired
    private IStockBusinessService iDrugEnterpriseBusinessService;


    /**
     * 医生指定药企列表
     *
     * @param validateDetailVO
     * @return
     */
    @RpcService
    public List<EnterpriseStock> enterpriseStockList(ValidateDetailVO validateDetailVO) {
        validateAtop(validateDetailVO, validateDetailVO.getRecipeBean(), validateDetailVO.getRecipeDetails());
        RecipeBean recipeBean = validateDetailVO.getRecipeBean();
        validateAtop(recipeBean.getRecipeType(), recipeBean.getClinicOrgan());
        List<RecipeDetailBean> recipeDetails = validateDetailVO.getRecipeDetails();
        boolean organDrugCode = recipeDetails.stream().anyMatch(a -> StringUtils.isEmpty(a.getOrganDrugCode()));
        if (organDrugCode) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "医院配置药品存在编号为空的数据");
        }
        List<Recipedetail> detailList = ObjectCopyUtils.convert(validateDetailVO.getRecipeDetails(), Recipedetail.class);
        if (RecipeUtil.isTcmType(recipeBean.getRecipeType())) {
            validateAtop(recipeBean.getCopyNum());
            detailList.forEach(a -> {
                if (a.getUseDose() != null) {
                    a.setUseTotalDose(BigDecimal.valueOf(recipeBean.getCopyNum()).multiply(BigDecimal.valueOf(a.getUseDose())).doubleValue());
                }
            });
        }
        Recipe recipe = ObjectCopyUtils.convert(recipeBean, Recipe.class);
        List<EnterpriseStock> result = iDrugEnterpriseBusinessService.stockList(recipe, detailList);
        result.forEach(a -> {
            a.setDrugsEnterprise(null);
            a.setDrugInfoList(null);
        });
        return result;
    }

    /**
     * 获取药企配送的站点
     * @param medicineStationVO 取药站点的信息
     * @return 可以取药站点的列表
     */
    @RpcService
    public List<MedicineStationVO> getMedicineStationList(MedicineStationVO medicineStationVO){
        validateAtop(medicineStationVO, medicineStationVO.getOrganId(), medicineStationVO.getEnterpriseId());
        try {
            List<MedicineStationVO> medicineStationList = iDrugEnterpriseBusinessService.getMedicineStationList(medicineStationVO);
            //对站点由近到远排序
            Collections.sort(medicineStationList, (o1,o2)-> o1.getDistance() >= o2.getDistance() ? 0 : -1);
            return medicineStationList;
        } catch (Exception e) {
            logger.error("DrugEnterprisePatientAtop getMedicineStationList error ", e);
            return Collections.emptyList();
        }
    }

    /**
     * 获取机构药企销售配置
     * @param organId 机构id
     * @param drugsEnterpriseId 药企id
     */
    @RpcService
    public OrganDrugsSaleConfigVo getOrganDrugsSaleConfig(Integer organId , Integer drugsEnterpriseId){
        validateAtop(organId);
        OrganDrugsSaleConfig organDrugsSaleConfig = iDrugEnterpriseBusinessService.getOrganDrugsSaleConfig(organId,drugsEnterpriseId);
        OrganDrugsSaleConfigVo organDrugsSaleConfigVo = new OrganDrugsSaleConfigVo();
        BeanUtils.copyProperties(organDrugsSaleConfig,organDrugsSaleConfigVo);
        return organDrugsSaleConfigVo;
    }

}
