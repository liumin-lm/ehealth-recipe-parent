package recipe.atop.patient;

import com.alibaba.fastjson.JSON;
import com.ngari.recipe.dto.EnterpriseStock;
import com.ngari.recipe.dto.RecipeDTO;
import com.ngari.recipe.entity.OrganDrugsSaleConfig;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeExtend;
import com.ngari.recipe.entity.Recipedetail;
import com.ngari.recipe.recipe.model.RecipeBean;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import ctd.persistence.exception.DAOException;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.atop.BaseAtop;
import recipe.constant.ErrorCode;
import recipe.core.api.IEnterpriseBusinessService;
import recipe.core.api.IStockBusinessService;
import recipe.util.ObjectCopyUtils;
import recipe.util.RecipeUtil;
import recipe.vo.doctor.EnterpriseStockVO;
import recipe.vo.doctor.ValidateDetailVO;
import recipe.vo.greenroom.OrganDrugsSaleConfigVo;
import recipe.vo.patient.CheckAddressReq;
import recipe.vo.patient.CheckAddressRes;
import recipe.vo.patient.FTYSendTimeReq;
import recipe.vo.patient.MedicineStationVO;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 药企相关入口
 *
 * @author fuzi
 */
@RpcBean(value = "drugEnterprisePatientAtop")
public class DrugEnterprisePatientAtop extends BaseAtop {

    @Autowired
    private IStockBusinessService iStockBusinessService;
    @Autowired
    private IEnterpriseBusinessService enterpriseBusinessService;


    /**
     * 校验煎法是否可以配送地址
     * @param checkAddressReq
     * @return
     */
    @RpcService
    public CheckAddressRes checkEnterpriseDecoctionAddress(CheckAddressReq checkAddressReq){
        validateAtop(checkAddressReq, checkAddressReq.getOrganId(), checkAddressReq.getEnterpriseId(),checkAddressReq.getDecoctionId(),checkAddressReq.getAddress3());
        return enterpriseBusinessService.checkEnterpriseDecoctionAddress(checkAddressReq);
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
            List<MedicineStationVO> medicineStationList = enterpriseBusinessService.getMedicineStationList(medicineStationVO);
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
        OrganDrugsSaleConfig organDrugsSaleConfig = enterpriseBusinessService.getOrganDrugsSaleConfig(organId, drugsEnterpriseId);
        OrganDrugsSaleConfigVo organDrugsSaleConfigVo = new OrganDrugsSaleConfigVo();
        BeanUtils.copyProperties(organDrugsSaleConfig,organDrugsSaleConfigVo);
        if (StringUtils.isNotEmpty(organDrugsSaleConfig.getStorePaymentWay())) {
            List<String> storePaymentWayList = JSON.parseArray(organDrugsSaleConfig.getStorePaymentWay(), String.class);
            organDrugsSaleConfigVo.setStorePaymentWay(storePaymentWayList);
        }
        if (StringUtils.isNotEmpty(organDrugsSaleConfig.getTakeOneselfPaymentWay())) {
            List<String> takeOneselfPaymentWayList = JSON.parseArray(organDrugsSaleConfig.getTakeOneselfPaymentWay(), String.class);
            organDrugsSaleConfigVo.setTakeOneselfPaymentWay(takeOneselfPaymentWayList);
        }
        return organDrugsSaleConfigVo;
    }

    /**
     * 患者端获取机构药企销售配置
     * @param organId 机构id
     * @param drugsEnterpriseId 药企id
     */
    @RpcService
    public OrganDrugsSaleConfig getOrganDrugsSaleConfigOfPatient(Integer organId , Integer drugsEnterpriseId){
        validateAtop(drugsEnterpriseId);
        return enterpriseBusinessService.getOrganDrugsSaleConfigOfPatient(organId, drugsEnterpriseId);
    }

    /**
     * 腹透液配送时间获取
     *
     * @param ftySendTimeREQ
     * @return
     */
    @RpcService
    public List<String> getFTYSendTime(FTYSendTimeReq ftySendTimeREQ) {
        validateAtop(ftySendTimeREQ, ftySendTimeREQ.getOrganId(), ftySendTimeREQ.getProvince(), ftySendTimeREQ.getCity(), ftySendTimeREQ.getDistrict(), ftySendTimeREQ.getStartDate(), ftySendTimeREQ.getEndDate());
        return enterpriseBusinessService.getFTYSendTime(ftySendTimeREQ);
    }


    /**
     * 医生指定药企列表
     * todo :drugDoctorAtop
     *
     * @param validateDetailVO
     * @return
     */
    @RpcService
    @Deprecated
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
     * 购物清单中校验煎法是否可以配送地址
     * @param checkAddressReqList
     * @return
     */
    @RpcService
    public CheckAddressRes shoppingCheckEnterpriseDecoctionAddress(List<CheckAddressReq> checkAddressReqList){
        logger.info("shoppingCheckEnterpriseDecoctionAddress checkAddressReqList={} ", JSONUtils.toString(checkAddressReqList));
        List<CheckAddressRes> checkAddressResList = new ArrayList<>();
        checkAddressReqList.forEach(checkAddressReq -> {
            validateAtop(checkAddressReq, checkAddressReq.getOrganId(), checkAddressReq.getEnterpriseId(),checkAddressReq.getDecoctionId(),checkAddressReq.getAddress3());
            CheckAddressRes checkAddressRes = enterpriseBusinessService.checkEnterpriseDecoctionAddress(checkAddressReq);
            checkAddressResList.add(checkAddressRes);
        });
        CheckAddressRes checkAddressRes = new CheckAddressRes();
        if(CollectionUtils.isNotEmpty(checkAddressResList)){
            List<CheckAddressRes> collect = checkAddressResList.stream().filter(a -> !a.getSendFlag()).collect(Collectors.toList());
            if (CollectionUtils.isNotEmpty(collect)) {
                checkAddressRes.setSendFlag(false);
                checkAddressRes.setAreaList(collect.get(0).getAreaList());
            } else {
                checkAddressRes.setSendFlag(true);
                checkAddressRes.setAreaList(checkAddressResList.get(0).getAreaList());
            }
        }
        return checkAddressRes;
    }

    /**
     * 根据机构获取 配置下的药企+ 到院自取的机构 返回前端列表
     *
     * @param organId 机构id
     * @return deliveryCode + deliveryName list返回前端
     */
    @RpcService
    public List<EnterpriseStockVO> enterprisesList(Integer organId) {
        List<EnterpriseStock> list = enterpriseBusinessService.enterprisesList(organId);
        return ObjectCopyUtils.convert(list, EnterpriseStockVO.class);
    }

}
