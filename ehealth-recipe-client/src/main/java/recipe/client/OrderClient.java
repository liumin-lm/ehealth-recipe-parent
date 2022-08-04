package recipe.client;

import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.recipe.service.IRecipeHisService;
import com.ngari.platform.recipe.mode.InvoiceInfoReqTO;
import com.ngari.platform.recipe.mode.InvoiceInfoResTO;
import ctd.persistence.exception.DAOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.constant.ErrorCode;

@Service
public class OrderClient extends BaseClient{

    @Autowired
    private IRecipeHisService iRecipeHisService;

    public InvoiceInfoResTO makeUpInvoice(InvoiceInfoReqTO invoiceInfoReqTO) {
        HisResponseTO<InvoiceInfoResTO> hisResponseTO = iRecipeHisService.makeUpInvoice(invoiceInfoReqTO);
        try {
            return getResponse(hisResponseTO);
        } catch (Exception e) {
            logger.error("OrderClient makeUpInvoice error", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        }
    }
}
