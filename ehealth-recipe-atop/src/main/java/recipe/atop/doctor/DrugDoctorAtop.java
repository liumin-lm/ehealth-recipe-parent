package recipe.atop.doctor;

import com.ngari.recipe.dto.EnterpriseStock;
import com.ngari.recipe.dto.PatientOptionalDrugDTO;
import com.ngari.recipe.entity.Recipedetail;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.atop.BaseAtop;
import recipe.core.api.IOrganBusinessService;
import recipe.core.api.IRecipeBusinessService;
import recipe.core.api.patient.IDrugEnterpriseBusinessService;
import recipe.vo.doctor.DrugEnterpriseStockVO;
import recipe.vo.doctor.DrugQueryVO;
import recipe.vo.doctor.EnterpriseStockVO;

import java.util.ArrayList;
import java.util.LinkedList;
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
    private IOrganBusinessService organBusinessService;
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
        EnterpriseStock organStock = organBusinessService.organStock(drugQueryVO.getOrganId(), detailList);
        List<EnterpriseStock> result = iDrugEnterpriseBusinessService.enterpriseStockCheck(drugQueryVO.getOrganId(), detailList);
        List<EnterpriseStockVO> enterpriseStockList = getEnterpriseStockVO(organStock,result);
        Map<Integer, List<EnterpriseStockVO>> enterpriseStockGroup = enterpriseStockList.stream().collect(Collectors.groupingBy(EnterpriseStockVO::getDrugId));

        List<DrugEnterpriseStockVO> drugEnterpriseStockList = new LinkedList<>();
        enterpriseStockGroup.forEach((k,v)->{
            DrugEnterpriseStockVO drugEnterpriseStock = new  DrugEnterpriseStockVO();
            drugEnterpriseStock.setDrugId(k);
            drugEnterpriseStock.setEnterpriseStockList(v);
            boolean stock = v.stream().anyMatch(EnterpriseStockVO::getStock);
            drugEnterpriseStock.setAllStock(stock);
        });
        return drugEnterpriseStockList;
    }

    /***
     * 组装药企药品一对一库存关系
     * @param organStock 医院库存
     * @param result 药企库存
     * @return
     */
    private List<EnterpriseStockVO> getEnterpriseStockVO(EnterpriseStock organStock, List<EnterpriseStock> result) {
        List<EnterpriseStockVO> enterpriseStockList = new LinkedList<>();
        result.forEach(a -> {
            if (CollectionUtils.isEmpty(a.getDrugInfoList())) {
                return;
            }
            a.getDrugInfoList().forEach(b -> {
                EnterpriseStockVO enterpriseStockVO = new EnterpriseStockVO();
                enterpriseStockVO.setDrugId(b.getDrugId());
                enterpriseStockVO.setAppointEnterpriseType(a.getAppointEnterpriseType());
                enterpriseStockVO.setDeliveryCode(a.getDeliveryCode());
                enterpriseStockVO.setDeliveryName(a.getDeliveryName());
                if (!a.getCheckDrugStock()) {
                    enterpriseStockVO.setStock(true);
                } else {
                    if (!a.getStock()) {
                        enterpriseStockVO.setStock(false);
                    } else {
                        enterpriseStockVO.setStock(b.getStock());
                        enterpriseStockVO.setStockAmountChin(b.getStockAmountChin());
                    }
                }
                enterpriseStockList.add(enterpriseStockVO);
            });
        });
        if(CollectionUtils.isEmpty(organStock.getDrugInfoList())){
            return enterpriseStockList;
        }
        organStock.getDrugInfoList().forEach(a->{
            EnterpriseStockVO enterpriseStockVO = new EnterpriseStockVO();
            enterpriseStockVO.setDrugId(a.getDrugId());
            enterpriseStockVO.setAppointEnterpriseType(organStock.getAppointEnterpriseType());
            enterpriseStockVO.setDeliveryCode(organStock.getDeliveryCode());
            enterpriseStockVO.setDeliveryName(organStock.getDeliveryName());
                if(!a.getStock()){
                    enterpriseStockVO.setStock(false);
                }else {
                    enterpriseStockVO.setStock(a.getStock());
                    enterpriseStockVO.setStockAmountChin(a.getStockAmountChin());
                }
            enterpriseStockList.add(enterpriseStockVO);
        });
        return enterpriseStockList;
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
