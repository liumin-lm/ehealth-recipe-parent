package recipe.atop.doctor;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.ngari.recipe.drug.model.CommonDrugListDTO;
import com.ngari.recipe.drug.model.DispensatoryDTO;
import com.ngari.recipe.drug.model.SearchDrugDetailDTO;
import com.ngari.recipe.drug.model.UseDoseAndUnitRelationBean;
import com.ngari.recipe.dto.*;
import com.ngari.recipe.entity.*;
import com.ngari.recipe.recipe.constant.RecipeTypeEnum;
import com.ngari.recipe.recipe.model.RecipeBean;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import com.ngari.recipe.vo.SearchDrugReqVO;
import ctd.persistence.exception.DAOException;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.atop.BaseAtop;
import recipe.constant.ErrorCode;
import recipe.core.api.IConfigStatusBusinessService;
import recipe.core.api.IDrugBusinessService;
import recipe.core.api.IRecipeBusinessService;
import recipe.core.api.IStockBusinessService;
import recipe.enumerate.type.RecipeDrugFormTypeEnum;
import recipe.enumerate.type.RecipeSupportGiveModeEnum;
import recipe.enumerate.type.StockCheckSourceTypeEnum;
import recipe.util.ByteUtils;
import recipe.util.ObjectCopyUtils;
import recipe.util.RecipeUtil;
import recipe.util.ValidateUtil;
import recipe.vo.doctor.*;

import java.math.BigDecimal;
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
    private IStockBusinessService iStockBusinessService;
    @Autowired
    private IRecipeBusinessService recipeBusinessService;
    @Autowired
    private IDrugBusinessService drugBusinessService;
    @Autowired
    private IConfigStatusBusinessService configStatusBusinessService;


    /**
     * 医生端 查询购药方式下有库存的药品
     * todo 新：giveModeDrugStockList
     *
     * @param drugQueryVO
     * @return
     */
    @RpcService
    @Deprecated
    public List<DrugForGiveModeListVO> drugForGiveMode(DrugQueryVO drugQueryVO) {
        return this.giveModeDrugStockList(drugQueryVO);
    }

    /**
     * 医生端 查询购药方式下有库存的药品
     *
     * @param drugQueryVO
     * @return
     */
    @RpcService
    public List<DrugForGiveModeListVO> giveModeDrugStockList(DrugQueryVO drugQueryVO) {
        RecipeDTO recipeDTO = this.recipeDTO(drugQueryVO);
        //机构够药方式配置
        List<GiveModeButtonDTO> organGiveModeList = organBusinessService.organGiveMode(drugQueryVO.getOrganId());
        List<GiveModeButtonDTO> organGiveModes = organGiveModeList.stream().filter(a -> !RecipeSupportGiveModeEnum.DOWNLOAD_RECIPE.getText().equals(a.getShowButtonKey())).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(organGiveModes)) {
            return null;
        }
        //查询购药方式下有库存的药品
        List<DrugForGiveModeListVO> list = iStockBusinessService.drugForGiveModeV1(recipeDTO, StockCheckSourceTypeEnum.DOCTOR_STOCK.getType());
        if (CollectionUtils.isEmpty(list)) {
            return null;
        }
        Map<String, List<EnterpriseStockVO>> map = list.stream().collect(Collectors.groupingBy(DrugForGiveModeListVO::getSupportKey
                , Collectors.mapping(DrugForGiveModeListVO::getEnterpriseStock, Collectors.toList())));
        logger.info("DrugDoctorAtop giveModeDrugStockList map={}", JSON.toJSONString(map));
        List<DrugForGiveModeListVO> drugForGiveModeList = new ArrayList<>();
        organGiveModes.forEach(a -> {
            DrugForGiveModeListVO drugForGiveMode = new DrugForGiveModeListVO();
            drugForGiveMode.setSupportKey(a.getShowButtonKey());
            drugForGiveMode.setSupportKeyText(a.getShowButtonName());
            List<EnterpriseStockVO> enterpriseStockList = map.get(a.getShowButtonKey());
            if (CollectionUtils.isNotEmpty(enterpriseStockList)) {
                drugForGiveMode.setEnterpriseStockList(enterpriseStockList);
            }
            drugForGiveModeList.add(drugForGiveMode);
        });
        return drugForGiveModeList;
    }

    /**
     * 查询某个药企下 药品库存 的库存数量
     *
     * @param drugQueryVO
     * @return
     */
    @RpcService
    public List<DrugStockVO> giveModeDrugStock(DrugQueryVO drugQueryVO) {
        RecipeDTO recipeDTO = this.recipeDTO(drugQueryVO);
        EnterpriseStock result = iStockBusinessService.enterpriseStockCheckV1(recipeDTO, drugQueryVO.getEnterpriseId(), drugQueryVO.getAppointEnterpriseType(), StockCheckSourceTypeEnum.DOCTOR_STOCK.getType());
        if (null == result) {
            return new ArrayList<>();
        }
        return ObjectCopyUtils.convert(result.getDrugInfoList(), DrugStockVO.class);
    }

    /**
     * 医生端查询药品列表-实时查药品对应所有药企的库存
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
            recipedetail.setUseTotalDose(a.getUseTotalDose());
            if (ValidateUtil.validateObjects(a.getUseTotalDose())) {
                recipedetail.setUseTotalDose(1D);
            }
            detailList.add(recipedetail);
        });
        return iStockBusinessService.stockList(drugQueryVO, detailList, StockCheckSourceTypeEnum.DOCTOR_STOCK.getType());
    }

    /**
     * 医生端 获取患者指定处方药品
     *
     * @param clinicId
     * @return
     */
    @RpcService
    public List<PatientOptionalDrugVO> findPatientOptionalDrugDTO(Integer clinicId) {
        validateAtop(clinicId);
        return recipeBusinessService.findPatientOptionalDrugDTO(clinicId);
    }

    /**
     * 查询药品能否开在一张处方上
     *
     * @param drugQueryVO
     * @return
     */
    @RpcService
    @Deprecated
    public boolean drugRecipeStock(DrugQueryVO drugQueryVO) {
        RecipeDTO recipeDTO = this.recipeDTO(drugQueryVO);
        List<EnterpriseStock> result = iStockBusinessService.drugRecipeStock(recipeDTO, StockCheckSourceTypeEnum.DOCTOR_STOCK.getType());
        logger.info("DrugDoctorAtop drugRecipeStock result={}", JSONArray.toJSONString(result));
        if (CollectionUtils.isEmpty(result)) {
            return false;
        }
        return result.stream().anyMatch(EnterpriseStock::getStock);
    }

    /**
     * 查询药品能支持的够药方式
     *
     * @param drugQueryVO
     * @return
     */
    @RpcService
    public DoSignRecipeDTO validateRecipeGiveMode(DrugQueryVO drugQueryVO) {
        RecipeDTO recipeDTO = this.recipeDTO(drugQueryVO);
        return iStockBusinessService.validateRecipeGiveMode(recipeDTO);
    }

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
        RecipeExtend recipeExtend = ObjectCopyUtils.convert(validateDetailVO.getRecipeExtendBean(), RecipeExtend.class);
        if (null == recipeExtend) {
            recipeExtend = new RecipeExtend();
        }
        RecipeDTO recipeDTO = new RecipeDTO();
        recipeDTO.setRecipe(recipe);
        recipeDTO.setRecipeDetails(detailList);
        recipeDTO.setRecipeExtend(recipeExtend);
        List<EnterpriseStock> result = iStockBusinessService.stockList(recipeDTO);
        result.forEach(a -> {
            a.setDrugsEnterprise(null);
            a.setDrugInfoList(null);
        });
        return result;
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
        if (!ValidateUtil.integerIsEmpty(searchDrugReq.getPharmacyId()) && !ValidateUtil.integerIsEmpty(searchDrugReq.getRecipeDrugForm())) {
            PharmacyTcm pharmacy = organBusinessService.pharmacy(searchDrugReq.getOrganId()).get(searchDrugReq.getPharmacyId());
            validateAtop(pharmacy);
            if (!pharmacy.getDrugFormType().contains(searchDrugReq.getRecipeDrugForm().toString())) {
                logger.info("DrugDoctorAtop searchOrganDrugEs DrugFormType ={}", JSON.toJSONString(pharmacy));
                return Collections.emptyList();
            }
        }

        DrugInfoDTO drugInfoDTO = new DrugInfoDTO();
        drugInfoDTO.setOrganId(searchDrugReq.getOrganId());
        drugInfoDTO.setDrugName(searchDrugReq.getDrugName());
        drugInfoDTO.setDrugType(searchDrugReq.getDrugType());
        drugInfoDTO.setApplyBusiness(searchDrugReq.getApplyBusiness());
        drugInfoDTO.setPharmacyId(searchDrugReq.getPharmacyId());
        if (RecipeUtil.isTcmType(searchDrugReq.getDrugType())) {
            drugInfoDTO.setDrugForm(RecipeDrugFormTypeEnum.getDrugForm(searchDrugReq.getRecipeDrugForm()));
        }
        List<SearchDrugDetailDTO> drugWithEsByPatient = drugBusinessService.searchOrganDrugEs(drugInfoDTO, searchDrugReq.getStart(), searchDrugReq.getLimit());
        List<Integer> drugIds = drugWithEsByPatient.stream().map(SearchDrugDetailDTO::getDrugId).distinct().collect(Collectors.toList());
        List<DrugList> drugs = drugBusinessService.drugList(drugIds);
        Map<Integer, DrugList> drugMap = drugs.stream().collect(Collectors.toMap(DrugList::getDrugId, a -> a, (k1, k2) -> k1));
        Map<String, OrganDrugList> organDrugMap = drugBusinessService.organDrugMap(searchDrugReq.getOrganId(), drugIds);
        Boolean openRecipeHideDrugManufacturer = configStatusBusinessService.getOpenRecipeHideDrugManufacturer(searchDrugReq.getOrganId(), "openRecipeHideDrugManufacturer");
        drugWithEsByPatient.forEach(drugList -> {
            // 中药 药品信息里不能显示生产厂家
            if (openRecipeHideDrugManufacturer && RecipeTypeEnum.RECIPETYPE_TCM.getType().equals(drugList.getDrugType())) {
                drugList.setProducer("****");
            }
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
                drugList.setSmallestSaleMultiple(organDrug.getSmallestSaleMultiple());
                drugList.setTargetedDrugType(organDrug.getTargetedDrugType());
                drugList.setSalePrice(organDrug.getSalePrice());
                drugList.setNationalStandardDrugFlag(organDrug.getNationalStandardDrugFlag());
                drugList.setSkinTestDrugFlag(organDrug.getSkinTestDrugFlag());
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
     * 查询常用药品
     *
     * @param commonDrug
     * @return
     */
    @RpcService
    public List<SearchDrugDetailDTO> commonDrugList(CommonDrugListDTO commonDrug) {
        validateAtop(commonDrug, commonDrug.getDrugType(), commonDrug.getDoctor(), commonDrug.getOrganId());
        return drugBusinessService.commonDrugList(commonDrug);
    }

    @RpcService
    public DispensatoryDTO getOrganDrugList(Integer organId, Integer drugId) {
        return drugBusinessService.getOrganDrugList(organId, drugId);
    }

    /**
     * 查询药品数据
     *
     * @param organId
     * @param organDrugCodes
     * @return
     */
    @RpcService
    public List<DrugsResVo> organDrugList(Integer organId, List<String> organDrugCodes) {
        validateAtop(organId, organDrugCodes);
        List<OrganDrugList> organDrugLists = drugBusinessService.organDrugList(organId, organDrugCodes);
        List<Integer> drugIds = organDrugLists.stream().map(OrganDrugList::getDrugId).collect(Collectors.toList());
        List<DrugList> drugLists = drugBusinessService.findByDrugIdsAndStatus(drugIds);
        Map<Integer, List<DrugList>> drugMap = Optional.ofNullable(drugLists).orElseGet(Collections::emptyList)
                .stream().collect(Collectors.groupingBy(DrugList::getDrugId));
        List<DrugsResVo> collect = organDrugLists.stream().map(organDrugList -> {
            DrugsResVo drugsResVo = new DrugsResVo();
            BeanUtils.copyProperties(organDrugList, drugsResVo);
            List<DrugList> drugList = drugMap.get(organDrugList.getDrugId());
            if (CollectionUtils.isNotEmpty(drugList)) {
                Integer drugType = drugList.get(0).getDrugType();
                drugsResVo.setDrugType(drugType);
            }
            return drugsResVo;
        }).collect(Collectors.toList());
        return collect;
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


    /**
     * 封装查询库存参数
     *
     * @param drugQueryVO
     * @return
     */
    private RecipeDTO recipeDTO(DrugQueryVO drugQueryVO) {
        validateAtop(drugQueryVO, drugQueryVO.getRecipeDetails(), drugQueryVO.getOrganId());
        List<Recipedetail> detailList = new ArrayList<>();
        drugQueryVO.getRecipeDetails().forEach(a -> {
            validateAtop(a.getDrugId(), a.getOrganDrugCode(), a.getUseTotalDose());
            Recipedetail recipedetail = ObjectCopyUtils.convert(a, Recipedetail.class);
            if (null != recipedetail && !ValidateUtil.integerIsEmpty(drugQueryVO.getPharmacyId())) {
                recipedetail.setPharmacyId(drugQueryVO.getPharmacyId());
            }
            detailList.add(recipedetail);
        });
        Recipe recipe = new Recipe();
        recipe.setClinicOrgan(drugQueryVO.getOrganId());
        recipe.setRecipeType(drugQueryVO.getRecipeType());
        RecipeExtend recipeExtend = new RecipeExtend();
        recipeExtend.setDecoctionId(drugQueryVO.getDecoctionId());
        RecipeDTO recipeDTO = new RecipeDTO();
        recipeDTO.setRecipe(recipe);
        recipeDTO.setRecipeDetails(detailList);
        recipeDTO.setRecipeExtend(recipeExtend);
        return recipeDTO;
    }

}
