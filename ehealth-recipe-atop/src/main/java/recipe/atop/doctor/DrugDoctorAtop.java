package recipe.atop.doctor;

import com.ngari.recipe.dto.EnterpriseStock;
import com.ngari.recipe.entity.Recipedetail;
import ctd.util.annotation.RpcBean;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.atop.BaseAtop;
import recipe.core.api.IOrganBusinessService;
import recipe.core.api.patient.IDrugEnterpriseBusinessService;
import recipe.enumerate.type.AppointEnterpriseTypeEnum;
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
 * @author fuzi
 */
@RpcBean(value = "drugDoctorAtop")
public class DrugDoctorAtop extends BaseAtop {
    @Autowired
    private IOrganBusinessService organBusinessService;
    @Autowired
    private IDrugEnterpriseBusinessService iDrugEnterpriseBusinessService;


    public List<DrugEnterpriseStockVO> findDrugWithEsByPatient(DrugQueryVO drugQueryVO) {
        validateAtop(drugQueryVO, drugQueryVO.getRecipeDetails(),drugQueryVO.getOrganId(),drugQueryVO.getPharmacyId());
        List<Recipedetail> detailList = new ArrayList<>();
        drugQueryVO.getRecipeDetails().forEach(a->{
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
    private  List<EnterpriseStockVO>  getEnterpriseStockVO( EnterpriseStock organStock,  List<EnterpriseStock> result ){
        List<EnterpriseStockVO> enterpriseStockList = new LinkedList<>();
        result.forEach(a -> {
            if (AppointEnterpriseTypeEnum.ORGAN_APPOINT.getType().equals(a.getCheckStockFlag()) && null != organStock) {
                a.setDrugName(organStock.getDrugName());
                a.setStock(organStock.getStock());
                a.setDrugInfoList(organStock.getDrugInfoList());
            }
            if(CollectionUtils.isEmpty( a.getDrugInfoList())){
                return;
            }
            a.getDrugInfoList().forEach(b->{
                EnterpriseStockVO enterpriseStockVO = new EnterpriseStockVO();
                enterpriseStockVO.setDrugId(b.getDrugId());
                enterpriseStockVO.setCheckStockFlag(a.getCheckStockFlag());
                enterpriseStockVO.setDeliveryCode(a.getDeliveryCode());
                enterpriseStockVO.setDeliveryName(a.getDeliveryName());
                if(!a.getCheckDrugStock()){
                    enterpriseStockVO.setStock(true);
                }else {
                    if(!a.getStock()){
                        enterpriseStockVO.setStock(false);
                    }else {
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
            enterpriseStockVO.setCheckStockFlag(organStock.getCheckStockFlag());
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
}
