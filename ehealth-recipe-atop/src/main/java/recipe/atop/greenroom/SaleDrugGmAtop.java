package recipe.atop.greenroom;

import com.ngari.recipe.drugsenterprise.model.DrugsEnterpriseBean;
import com.ngari.recipe.drugsenterprise.model.EnterpriseDecoctionAddressDTO;
import com.ngari.recipe.drugsenterprise.model.EnterpriseDecoctionAddressReq;
import com.ngari.recipe.drugsenterprise.model.EnterpriseDecoctionList;
import com.ngari.recipe.entity.*;
import com.ngari.recipe.organdrugsep.model.OrganAndDrugsepRelationBean;
import ctd.persistence.bean.QueryResult;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import eh.utils.BeanCopyUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.atop.BaseAtop;
import recipe.core.api.IDrugsEnterpriseBusinessService;
import recipe.core.api.ISaleDrugBusinessService;
import recipe.enumerate.type.RecipeSupportGiveModeEnum;
import recipe.util.ObjectCopyUtils;
import recipe.util.ValidateUtil;
import recipe.vo.greenroom.DrugsEnterpriseVO;
import recipe.vo.greenroom.OrganDrugsSaleConfigVo;
import recipe.vo.greenroom.OrganEnterpriseRelationVo;
import recipe.vo.greenroom.PharmacyVO;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @description： 运营平台药企药品
 * @author： 刘敏
 * @date： 2022-05-23 9:45
 */
@RpcBean(value = "saleDrugGmAtop")
public class SaleDrugGmAtop extends BaseAtop {

    @Autowired
    private ISaleDrugBusinessService enterpriseBusinessService;


    /**
     * 根据OrganId、DrugId获取药企药品
     *
     * @param saleDrugList
     */
    @RpcService
    public SaleDrugList getSaleDrugListByDrugIdAndOrganId(SaleDrugList saleDrugList) {
        validateAtop(saleDrugList.getOrganId(),saleDrugList.getDrugId());
        SaleDrugList res = enterpriseBusinessService.findSaleDrugListByDrugIdAndOrganId(saleDrugList);
        return res;
    }
    /**
     * 保存药企销售策略
     *
     * @param saleDrugList
     */
    @RpcService
    public void saveSaleDrugSalesStrategy(SaleDrugList saleDrugList){
        validateAtop(saleDrugList.getOrganId(),saleDrugList.getDrugId());
        enterpriseBusinessService.saveSaleDrugSalesStrategy(saleDrugList);
    }



}
