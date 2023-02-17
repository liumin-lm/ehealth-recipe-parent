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

    //是否可以打印快递面单
    private Boolean printWaybillByLogisticsOrderNo;

    //打印面单图片
    private String logisticsOrderPrintWaybill;

    //打印发药清单之前是否需要先发药
    private Boolean isNeedSendDrugWithPrientBefore;

}
