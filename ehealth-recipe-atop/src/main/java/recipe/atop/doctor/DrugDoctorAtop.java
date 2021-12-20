package recipe.atop.doctor;

import com.ngari.recipe.dto.EnterpriseStock;
import com.ngari.recipe.entity.Recipedetail;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.atop.BaseAtop;
import recipe.core.api.IRecipeBusinessService;
import recipe.core.api.IStockBusinessService;
import recipe.vo.doctor.DrugEnterpriseStockVO;
import recipe.vo.doctor.DrugForGiveModeVO;
import recipe.vo.doctor.DrugQueryVO;
import recipe.vo.doctor.PatientOptionalDrugVO;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 医生端药品查询
 *
 * @author fuzi
 */
@RpcBean(value = "drugDoctorAtop")
public class DrugDoctorAtop extends BaseAtop {

    @Autowired
    private IStockBusinessService iDrugEnterpriseBusinessService;
    @Autowired
    private IRecipeBusinessService recipeBusinessService;

    /**
     * 医生端 查询购药方式下有库存的药品
     *
     * @param drugQueryVO
     * @return
     */
    @RpcService
    public Map<String, List<DrugForGiveModeVO>> drugForGiveMode(DrugQueryVO drugQueryVO) {
        validateAtop(drugQueryVO, drugQueryVO.getRecipeDetails(), drugQueryVO.getOrganId());
        return iDrugEnterpriseBusinessService.drugForGiveMode(drugQueryVO);
    }

    /**
     * 医生端查询药品列表-实时查单个药品 所有药企的库存
     *
     * @param drugQueryVO
     * @return
     */
    @RpcService
    public List<DrugEnterpriseStockVO> drugEnterpriseStock(DrugQueryVO drugQueryVO) {
        validateAtop(drugQueryVO, drugQueryVO.getRecipeDetails(), drugQueryVO.getOrganId());
        List<Recipedetail> detailList = new ArrayList<>();
        drugQueryVO.getRecipeDetails().forEach(a -> {
            Recipedetail recipedetail = new Recipedetail();
            recipedetail.setDrugId(a.getDrugId());
            recipedetail.setOrganDrugCode(a.getOrganDrugCode());
            recipedetail.setPharmacyId(drugQueryVO.getPharmacyId());
            recipedetail.setUseTotalDose(1D);
            detailList.add(recipedetail);
        });
        return iDrugEnterpriseBusinessService.stockList(drugQueryVO.getOrganId(), detailList);
    }

    /**
     * 医生端 获取患者指定处方药品
     *
     * @param clinicId
     * @return
     */
    @RpcService
    public List<PatientOptionalDrugVO> findPatientOptionalDrugDTO(Integer clinicId) {
        logger.info("OffLineRecipeAtop findPatientOptionalDrugDTO clinicId={}", clinicId);
        validateAtop(clinicId);
        List<PatientOptionalDrugVO> result = recipeBusinessService.findPatientOptionalDrugDTO(clinicId);
        logger.info("OffLineRecipeAtop findPatientOptionalDrugDTO result = {}", JSONUtils.toString(result));
        return result;

    }

    @RpcService
    public boolean drugRecipeStock(DrugQueryVO drugQueryVO) {
        validateAtop(drugQueryVO, drugQueryVO.getRecipeDetails(), drugQueryVO.getOrganId());
        List<Recipedetail> detailList = new ArrayList<>();
        drugQueryVO.getRecipeDetails().forEach(a -> {
            validateAtop(a.getDrugId(), a.getOrganDrugCode(), a.getUseTotalDose());
            Recipedetail recipedetail = new Recipedetail();
            recipedetail.setDrugId(a.getDrugId());
            recipedetail.setOrganDrugCode(a.getOrganDrugCode());
            recipedetail.setPharmacyId(drugQueryVO.getPharmacyId());
            recipedetail.setUseTotalDose(a.getUseTotalDose());
            detailList.add(recipedetail);
        });
        List<EnterpriseStock> result = iDrugEnterpriseBusinessService.drugRecipeStock(drugQueryVO.getOrganId(), detailList);
        if (CollectionUtils.isEmpty(result)) {
            return false;
        }
        return result.stream().anyMatch(EnterpriseStock::getStock);
    }
}
