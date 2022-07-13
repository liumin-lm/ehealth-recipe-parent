package recipe.atop.patient;

import com.ngari.recipe.entity.OrganDrugsSaleConfig;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.atop.BaseAtop;
import recipe.core.api.IDrugsEnterpriseBusinessService;
import recipe.core.api.IStockBusinessService;
import recipe.vo.greenroom.OrganDrugsSaleConfigVo;
import recipe.vo.patient.CheckAddressReq;
import recipe.vo.patient.CheckAddressRes;
import recipe.vo.patient.MedicineStationVO;

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
    private IStockBusinessService iStockBusinessService;
    @Autowired
    private IDrugsEnterpriseBusinessService enterpriseBusinessService;


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
}
