package recipe.atop.patient;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.ngari.base.property.service.IConfigurationCenterUtilsService;
import com.ngari.recipe.dto.DrugSpecificationInfoDTO;
import com.ngari.recipe.dto.EnterpriseStock;
import com.ngari.recipe.dto.PatientDrugWithEsDTO;
import com.ngari.recipe.dto.RecipeDTO;
import com.ngari.recipe.entity.OrganDrugList;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeExtend;
import com.ngari.recipe.entity.Recipedetail;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import com.ngari.recipe.vo.HospitalDrugListReqVO;
import com.ngari.recipe.vo.HospitalDrugListVO;
import com.ngari.recipe.vo.SearchDrugReqVO;
import ctd.persistence.exception.DAOException;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import recipe.atop.BaseAtop;
import recipe.constant.ErrorCode;
import recipe.core.api.IClinicCartBusinessService;
import recipe.core.api.IDrugBusinessService;
import recipe.core.api.IFastRecipeBusinessService;
import recipe.core.api.IStockBusinessService;
import recipe.enumerate.type.StockCheckSourceTypeEnum;
import recipe.util.ByteUtils;
import recipe.util.ObjectCopyUtils;
import recipe.util.ValidateUtil;
import recipe.vo.doctor.DrugQueryVO;
import recipe.vo.doctor.DrugsResVo;
import recipe.vo.doctor.RecipeInfoVO;
import recipe.vo.patient.PatientContinueRecipeCheckDrugReq;
import recipe.vo.patient.PatientContinueRecipeCheckDrugRes;
import recipe.vo.second.ClinicCartVO;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @description： 患者药品查询入口
 * @author： whf
 * @date： 2021-08-23 18:05
 */
@RpcBean(value = "drugPatientAtop", mvc_authentication = false)
public class DrugPatientAtop extends BaseAtop {

    @Resource
    private IDrugBusinessService drugBusinessService;

    @Resource
    private IStockBusinessService stockBusinessService;

    @Resource
    private IClinicCartBusinessService clinicCartService;

    @Resource
    IFastRecipeBusinessService fastRecipeService;

    /**
     * 患者端获取药品列表
     *
     * @param searchDrugReqVo
     * @return
     */
    @RpcService
    public List<PatientDrugWithEsDTO> findDrugWithEsByPatient(SearchDrugReqVO searchDrugReqVo) {
        logger.info("DrugPatientAtop findDrugWithEsByPatient outPatientReqVO:{}", JSON.toJSONString(searchDrugReqVo));
        validateAtop(searchDrugReqVo, searchDrugReqVo.getOrganId());
        try {
            List<PatientDrugWithEsDTO> drugWithEsByPatient = drugBusinessService.findDrugWithEsByPatient(searchDrugReqVo);
            logger.info("DrugPatientAtop findDrugWithEsByPatient result:{}", JSONArray.toJSONString(drugWithEsByPatient));
            return drugWithEsByPatient;
        } catch (DAOException e1) {
            logger.error("DrugPatientAtop findDrugWithEsByPatient error", e1);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e1.getMessage());
        } catch (Exception e) {
            logger.error("DrugPatientAtop findDrugWithEsByPatient error e", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        }
    }

    /**
     * 查询药品说明书
     *
     * @param organId          机构id
     * @param recipeDetailBean 药品数据
     * @return
     */
    @RpcService
    public DrugSpecificationInfoDTO hisDrugBook(Integer organId, RecipeDetailBean recipeDetailBean) {
        validateAtop(organId, recipeDetailBean, recipeDetailBean.getDrugId(), recipeDetailBean.getOrganDrugCode());
        Recipedetail recipedetail = ObjectCopyUtils.convert(recipeDetailBean, Recipedetail.class);
        return drugBusinessService.hisDrugBook(organId, recipedetail);
    }

    /**
     * 下单时获取药品库存
     *
     * @param recipeIds
     * @param enterpriseId
     * @return
     */
    @RpcService
    public Boolean getOrderStockFlag(List<Integer> recipeIds, Integer enterpriseId, String giveModeKey) {
        validateAtop(recipeIds, giveModeKey);
        return stockBusinessService.getOrderStockFlag(recipeIds, enterpriseId, giveModeKey);
    }

    @RpcService
    public List<HospitalDrugListVO> findHospitalDrugList(HospitalDrugListReqVO hospitalDrugListReqVO) {
        return drugBusinessService.findHospitalDrugList(hospitalDrugListReqVO);
    }

    /**
     * 患者端续方药品信息校验
     *
     * @param patientContinueRecipeCheckDrugReq
     * @return PatientContinueRecipeCheckDrugRes
     */
    @RpcService
    public PatientContinueRecipeCheckDrugRes patientContinueRecipeCheckDrug(PatientContinueRecipeCheckDrugReq patientContinueRecipeCheckDrugReq) {
        validateAtop(patientContinueRecipeCheckDrugReq, patientContinueRecipeCheckDrugReq.getOrganId());
        return drugBusinessService.patientContinueRecipeCheckDrug(patientContinueRecipeCheckDrugReq);
    }

    /**
     * 查询患者指定用药库存 - 一个药品的库存情况
     */
    @RpcService
    public Boolean designatedDrugStock(DrugsResVo drugsRes) {
        validateAtop(drugsRes, drugsRes.getOrganId(), drugsRes.getDrugId(), drugsRes.getOrganDrugCode());
        OrganDrugList organDrug = drugBusinessService.getOrganDrugList(drugsRes.getOrganId(), drugsRes.getOrganDrugCode(), drugsRes.getDrugId());
        if (null == organDrug) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "无机构药品");
        }
        Recipedetail recipedetail = new Recipedetail();
        recipedetail.setDrugId(drugsRes.getDrugId());
        recipedetail.setOrganDrugCode(drugsRes.getOrganDrugCode());
        recipedetail.setUseTotalDose(1D);
        if (StringUtils.isNotEmpty(organDrug.getPharmacy())) {
            String pharmacyId = organDrug.getPharmacy().split(ByteUtils.COMMA)[0];
            recipedetail.setPharmacyId(Integer.valueOf(pharmacyId));
        }
        RecipeDTO recipeDTO = new RecipeDTO();
        Recipe recipe = new Recipe();
        recipe.setClinicOrgan(drugsRes.getOrganId());
        recipeDTO.setRecipe(recipe);
        recipeDTO.setRecipeExtend(new RecipeExtend());
        recipeDTO.setRecipeDetails(Collections.singletonList(recipedetail));
        List<EnterpriseStock> drugsStock = stockBusinessService.drugsStock(recipeDTO, StockCheckSourceTypeEnum.PATIENT_STOCK.getType());
        return drugsStock.stream().anyMatch(EnterpriseStock::getStock);
    }


    /**
     * 保留几个版本后删除
     *
     * @param drugQueryVO
     * @return
     */
    @Deprecated
    @RpcService
    public boolean searchDrugRecipeStock(DrugQueryVO drugQueryVO) {
        return true;
    }

    @RpcService
    public List<Integer> queryFastRecipePlatStock(List<RecipeInfoVO> recipeInfoVOList) {
        return fastRecipeService.checkFastRecipeStock(recipeInfoVOList);
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

    /**
     * 方便门诊购物车列表查询
     *
     * @param organId
     * @param userId
     * @return
     */
    @RpcService

    public List<ClinicCartVO> findClinicCartsByOrganIdAndUserId(Integer organId, String userId, Integer workType) {
        validateAtop(organId, userId, workType);
        return clinicCartService.findClinicCartsByOrganIdAndUserId(organId, userId, workType);
    }

    /**
     * 方便门诊购物车新增
     *
     * @param clinicCartVO
     * @return
     */
    @RpcService
    public Integer addClinicCart(ClinicCartVO clinicCartVO) {
        return clinicCartService.addClinicCart(clinicCartVO);
    }


    /**
     * 方便门诊购物车删除
     *
     * @param ids
     * @return
     */
    @RpcService
    public Boolean deleteClinicCartByIds(List<Integer> ids) {
        validateAtop(ids);
        return clinicCartService.deleteClinicCartByIds(ids);
    }

    /**
     * 方便门诊购物车 修改某一条记录（目前只需要修改数量）
     *
     * @param clinicCartVO
     * @return
     */
    @RpcService
    public Boolean updateClinicCartById(ClinicCartVO clinicCartVO) {
        validateAtop(clinicCartVO);
        return clinicCartService.updateClinicCartById(clinicCartVO);
    }

    /**
     * 购物车根据用户Id,机构id和业务场景删除数据
     *
     * @param clinicCartVO
     * @return
     */
    @RpcService
    public Boolean deleteClinicCartByUserId(ClinicCartVO clinicCartVO) {
        validateAtop(clinicCartVO.getUserId(), clinicCartVO.getOrganId(), clinicCartVO.getWorkType());
        return clinicCartService.deleteClinicCartByUserId(clinicCartVO);
    }

}
