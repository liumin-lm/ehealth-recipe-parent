package recipe.core.api;

import com.ngari.recipe.drugsenterprise.model.EnterpriseDecoctionAddressReq;
import com.ngari.recipe.drugsenterprise.model.EnterpriseDecoctionList;
import com.ngari.recipe.entity.*;
import ctd.persistence.bean.QueryResult;
import recipe.vo.greenroom.DrugsEnterpriseVO;
import recipe.vo.greenroom.OrganDrugsSaleConfigVo;
import recipe.vo.greenroom.OrganEnterpriseRelationVo;
import recipe.vo.greenroom.PharmacyVO;
import recipe.vo.patient.CheckAddressReq;
import recipe.vo.patient.CheckAddressRes;

import java.util.List;

/**
 * 药企药品
 */
public interface ISaleDrugBusinessService {

    /**
     * findSaleDrugListByByDrugIdAndOrganId
     * @param saleDrugList
     * @return
     */
    SaleDrugList findSaleDrugListByDrugIdAndOrganId(SaleDrugList saleDrugList);
}
