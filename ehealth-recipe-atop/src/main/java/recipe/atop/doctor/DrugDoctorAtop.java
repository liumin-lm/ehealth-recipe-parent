package recipe.atop.doctor;

import com.ngari.recipe.drug.model.SearchDrugDetailDTO;
import com.ngari.recipe.drug.model.UseDoseAndUnitRelationBean;
import com.ngari.recipe.dto.DrugInfoDTO;
import com.ngari.recipe.dto.EnterpriseStock;
import com.ngari.recipe.entity.DrugList;
import com.ngari.recipe.entity.Recipedetail;
import com.ngari.recipe.vo.SearchDrugReqVO;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.atop.BaseAtop;
import recipe.core.api.IDrugBusinessService;
import recipe.core.api.IRecipeBusinessService;
import recipe.core.api.IStockBusinessService;
import recipe.util.ByteUtils;
import recipe.vo.doctor.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    @Autowired
    private IDrugBusinessService drugBusinessService;

    /**
     * 医生端 查询购药方式下有库存的药品
     *
     * @param drugQueryVO
     * @return
     */
    @RpcService
    public List<DrugForGiveModeListVO>  drugForGiveMode(DrugQueryVO drugQueryVO) {
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

    /**
     * 查询药品能否开在一张处方上
     *
     * @param drugQueryVO
     * @return
     */
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

    /**
     * 查询药品
     *
     * @param searchDrugReq
     * @return
     */
    @RpcService
    public List<SearchDrugDetailDTO> searchOrganDrugEs(SearchDrugReqVO searchDrugReq) {
        validateAtop(searchDrugReq, searchDrugReq.getOrganId(), searchDrugReq.getDrugName(), searchDrugReq.getDrugType(), searchDrugReq.getApplyBusiness());
        DrugInfoDTO drugInfoDTO = new DrugInfoDTO();
        drugInfoDTO.setOrganId(Integer.valueOf(searchDrugReq.getOrganId()));
        drugInfoDTO.setDrugName(searchDrugReq.getDrugName());
        drugInfoDTO.setDrugType(searchDrugReq.getDrugType());
        drugInfoDTO.setApplyBusiness(searchDrugReq.getApplyBusiness());
        drugInfoDTO.setPharmacyId(searchDrugReq.getPharmacyId());
        List<SearchDrugDetailDTO> drugWithEsByPatient = drugBusinessService.searchOrganDrugEs(drugInfoDTO, searchDrugReq.getStart(), searchDrugReq.getLimit());
        List<Integer> drugIds = drugWithEsByPatient.stream().map(SearchDrugDetailDTO::getDrugId).distinct().collect(Collectors.toList());
        Map<Integer, DrugList> drugMap = drugBusinessService.drugList(drugIds);
        drugWithEsByPatient.forEach(drugList -> {
            //添加es价格空填值逻辑
            DrugList drugListNow = drugMap.get(drugList.getDrugId());
            if (null != drugListNow) {
                drugList.setPrice1(null == drugList.getPrice1() ? drugListNow.getPrice1() : drugList.getPrice1());
                drugList.setPrice2(null == drugList.getPrice2() ? drugListNow.getPrice2() : drugList.getPrice2());
            }
            //该高亮字段给微信端使用:highlightedField
            drugList.setHospitalPrice(drugList.getSalePrice());
            //该高亮字段给ios前端使用:highlightedFieldForIos
            if (StringUtils.isNotEmpty(drugList.getHighlightedField())) {
                drugList.setHighlightedFieldForIos((ByteUtils.getListByHighlightedField(drugList.getHighlightedField())));
            }
            if (StringUtils.isNotEmpty(drugList.getHighlightedField2())) {
                drugList.setHighlightedFieldForIos2((ByteUtils.getListByHighlightedField(drugList.getHighlightedField2())));
            }
            if (StringUtils.isEmpty(drugList.getUsingRate())) {
                drugList.setUsingRate("");
            }
            if (StringUtils.isEmpty(drugList.getUsePathways())) {
                drugList.setUsePathways("");
            }

            //设置医生端每次剂量和剂量单位联动关系-用药单位不为空时才返回给前端
            List<UseDoseAndUnitRelationBean> useDoseAndUnitRelationList = new ArrayList<>();
            if (StringUtils.isNotEmpty(drugList.getUseDoseUnit())) {
                useDoseAndUnitRelationList.add(new UseDoseAndUnitRelationBean(drugList.getRecommendedUseDose(), drugList.getUseDoseUnit(), drugList.getUseDose()));
            }
            if (StringUtils.isNotEmpty(drugList.getUseDoseSmallestUnit())) {
                useDoseAndUnitRelationList.add(new UseDoseAndUnitRelationBean(drugList.getDefaultSmallestUnitUseDose(), drugList.getUseDoseSmallestUnit(), drugList.getSmallestUnitUseDose()));
            }
            drugList.setUseDoseAndUnitRelation(useDoseAndUnitRelationList);
        });
        return drugWithEsByPatient;
    }
}
