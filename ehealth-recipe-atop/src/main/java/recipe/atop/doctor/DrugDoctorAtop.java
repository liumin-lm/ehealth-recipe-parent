package recipe.atop.doctor;

import com.alibaba.fastjson.JSONArray;
import com.google.common.collect.Lists;
import com.ngari.recipe.drug.model.SearchDrugDetailDTO;
import com.ngari.recipe.drug.model.UseDoseAndUnitRelationBean;
import com.ngari.recipe.dto.DrugInfoDTO;
import com.ngari.recipe.dto.EnterpriseStock;
import com.ngari.recipe.entity.DrugList;
import com.ngari.recipe.entity.OrganDrugList;
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
import recipe.util.ObjectCopyUtils;
import recipe.vo.doctor.*;

import java.util.*;
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
        List<DrugForGiveModeVO> list = iDrugEnterpriseBusinessService.drugForGiveMode(drugQueryVO);
        Map<String, List<DrugForGiveModeVO>> returnMap = list.stream().collect(Collectors.groupingBy(DrugForGiveModeVO::getGiveModeKey));
        Set<String> strings = returnMap.keySet();
        List<DrugForGiveModeListVO> result = Lists.newArrayList();
        strings.forEach(key -> {
            DrugForGiveModeListVO drugForGiveModeListVO = new DrugForGiveModeListVO();
            drugForGiveModeListVO.setSupportKey(key);
            drugForGiveModeListVO.setSupportKeyText(returnMap.get(key).get(0).getGiveModeKeyText());
            drugForGiveModeListVO.setDrugForGiveModeVOS(returnMap.get(key));
            result.add(drugForGiveModeListVO);
        });
        return result;
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
            Recipedetail recipedetail = ObjectCopyUtils.convert(a, Recipedetail.class);
            recipedetail.setPharmacyId(drugQueryVO.getPharmacyId());
            detailList.add(recipedetail);
        });
        List<EnterpriseStock> result = iDrugEnterpriseBusinessService.drugRecipeStock(drugQueryVO.getOrganId(), detailList);
        logger.info("DrugDoctorAtop drugRecipeStock result={}", JSONArray.toJSONString(result));
        if (CollectionUtils.isEmpty(result)) {
            return false;
        }
        return result.stream().anyMatch(EnterpriseStock::getStock);
    }

    /**
     * 通过es检索药品
     *
     * @param searchDrugReq 药品检索条件
     * @return
     */
    @RpcService
    public List<SearchDrugDetailDTO> searchOrganDrugEs(SearchDrugReqVO searchDrugReq) {
        validateAtop(searchDrugReq, searchDrugReq.getOrganId(), searchDrugReq.getDrugType(), searchDrugReq.getApplyBusiness());
        DrugInfoDTO drugInfoDTO = new DrugInfoDTO();
        drugInfoDTO.setOrganId(searchDrugReq.getOrganId());
        drugInfoDTO.setDrugName(searchDrugReq.getDrugName());
        drugInfoDTO.setDrugType(searchDrugReq.getDrugType());
        drugInfoDTO.setApplyBusiness(searchDrugReq.getApplyBusiness());
        drugInfoDTO.setPharmacyId(searchDrugReq.getPharmacyId());
        List<SearchDrugDetailDTO> drugWithEsByPatient = drugBusinessService.searchOrganDrugEs(drugInfoDTO, searchDrugReq.getStart(), searchDrugReq.getLimit());
        List<Integer> drugIds = drugWithEsByPatient.stream().map(SearchDrugDetailDTO::getDrugId).distinct().collect(Collectors.toList());
        List<DrugList> drugs = drugBusinessService.drugList(drugIds);
        Map<Integer, DrugList> drugMap = drugs.stream().collect(Collectors.toMap(DrugList::getDrugId, a -> a, (k1, k2) -> k1));
        Map<String, OrganDrugList> organDrugMap = drugBusinessService.organDrugMap(searchDrugReq.getOrganId(), drugIds);
        drugWithEsByPatient.forEach(drugList -> {
            //添加es价格空填值逻辑
            DrugList drugListNow = drugMap.get(drugList.getDrugId());
            if (null != drugListNow) {
                drugList.setPrice1(null == drugList.getPrice1() ? drugListNow.getPrice1() : drugList.getPrice1());
                drugList.setPrice2(null == drugList.getPrice2() ? drugListNow.getPrice2() : drugList.getPrice2());
            }
            //添加es单复方字段
            OrganDrugList organDrug = organDrugMap.get(drugList.getDrugId() + drugList.getOrganDrugCode());
            if (null != organDrug) {
                drugList.setUnilateralCompound(organDrug.getUnilateralCompound());
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
            drugList.setUseDoseAndUnitRelation(defaultUseDose(organDrug));
        });
        return drugWithEsByPatient;
    }


    /**
     * 默认药品单位计量 机构关联关系
     *
     * @param organDrug 机构药品
     * @return 默认药品单位计量
     */
    private List<UseDoseAndUnitRelationBean> defaultUseDose(OrganDrugList organDrug) {
        List<UseDoseAndUnitRelationBean> useDoseAndUnitRelationList = new LinkedList<>();
        if (null == organDrug) {
            return useDoseAndUnitRelationList;
        }
        if (StringUtils.isNotEmpty(organDrug.getUseDoseUnit())) {
            useDoseAndUnitRelationList.add(new UseDoseAndUnitRelationBean(organDrug.getRecommendedUseDose(), organDrug.getUseDoseUnit(), organDrug.getUseDose()));
        }
        if (StringUtils.isNotEmpty(organDrug.getUseDoseSmallestUnit())) {
            useDoseAndUnitRelationList.add(new UseDoseAndUnitRelationBean(organDrug.getDefaultSmallestUnitUseDose(), organDrug.getUseDoseSmallestUnit(), organDrug.getSmallestUnitUseDose()));
        }
        return useDoseAndUnitRelationList;
    }
}
