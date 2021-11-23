package recipe.atop.doctor;

import com.ngari.recipe.dto.PatientOptionalDrugDTO;
import com.ngari.recipe.entity.Recipedetail;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.atop.BaseAtop;
import recipe.core.api.IRecipeBusinessService;
import recipe.core.api.patient.IDrugEnterpriseBusinessService;
import recipe.vo.doctor.DrugEnterpriseStockVO;
import recipe.vo.doctor.DrugQueryVO;

import java.util.ArrayList;
import java.util.List;

/**
 * 医生端药品查询
 *
 * @author fuzi
 */
@RpcBean(value = "drugDoctorAtop")
public class DrugDoctorAtop extends BaseAtop {

    @Autowired
    private IDrugEnterpriseBusinessService iDrugEnterpriseBusinessService;
    @Autowired
    private IRecipeBusinessService recipeBusinessService;

    @RpcService
    public List<DrugEnterpriseStockVO> drugEnterpriseStock(DrugQueryVO drugQueryVO) {
        validateAtop(drugQueryVO, drugQueryVO.getRecipeDetails(), drugQueryVO.getOrganId(), drugQueryVO.getPharmacyId());
        List<Recipedetail> detailList = new ArrayList<>();
        drugQueryVO.getRecipeDetails().forEach(a -> {
            Recipedetail recipedetail = new Recipedetail();
            recipedetail.setDrugId(a.getDrugId());
            recipedetail.setOrganDrugCode(a.getOrganDrugCode());
            recipedetail.setPharmacyId(drugQueryVO.getPharmacyId());
            recipedetail.setUseTotalDose(10D);
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
    public List<PatientOptionalDrugDTO> findPatientOptionalDrugDTO(Integer clinicId) {
        logger.info("OffLineRecipeAtop findPatientOptionalDrugDTO clinicId={}", clinicId);
        validateAtop(clinicId);
        List<PatientOptionalDrugDTO> result = recipeBusinessService.findPatientOptionalDrugDTO(clinicId);
        logger.info("OffLineRecipeAtop findPatientOptionalDrugDTO result = {}", JSONUtils.toString(result));
        return result;

    }
}
