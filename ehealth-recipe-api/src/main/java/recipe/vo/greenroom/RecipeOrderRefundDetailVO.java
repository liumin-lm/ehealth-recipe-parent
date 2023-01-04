package recipe.vo.greenroom;

import com.ngari.recipe.drugsenterprise.model.DrugsEnterpriseBean;
import com.ngari.recipe.recipe.model.RecipeBean;
import com.ngari.recipe.recipeorder.model.RecipeOrderVoNoDS;
import com.ngari.recipe.vo.PatientBeanNoDS;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
public class RecipeOrderRefundDetailVO implements Serializable {
    private static final long serialVersionUID = 4052678532528362339L;

    private RecipeOrderVoNoDS recipeOrderBean;

    private List<RecipeBean> recipeBeanList;

    private DrugsEnterpriseBean drugsEnterpriseBean;

    private PatientBeanNoDS patientDTO;

    private OrderRefundInfoVO orderRefundInfoVO;

    private InvoiceRecordVO invoiceRecordVO;

    //发票号
    private String billNumber;

    //获取三级分拣码
    private String logisticsOrderSortCode;

    //获取物流运单条形码
    private String logisticsOrderNo;

    //获取物流详情
    private LogisticsOrderDetailsVO logisticsOrderDetailsVO;

}
