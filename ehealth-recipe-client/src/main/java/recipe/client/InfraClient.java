package recipe.client;

import com.ngari.infra.logistics.mode.ControlLogisticsOrderDto;
import com.ngari.infra.logistics.mode.LogisticsDistanceDto;
import com.ngari.infra.logistics.mode.OrganLogisticsManageDto;
import com.ngari.infra.logistics.mode.WayBillExceptPriceTO;
import com.ngari.infra.logistics.service.ILogisticsOrderService;
import com.ngari.infra.logistics.service.IOrganLogisticsManageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.aop.LogRecord;

import java.util.List;
import java.util.Map;

/**
 * @description： 物流client
 * @author： whf
 * @date： 2022-03-02 14:22
 */
@Service
public class InfraClient extends BaseClient {

    @Autowired
    private ILogisticsOrderService logisticsOrderService;

    @Autowired
    private IOrganLogisticsManageService organLogisticsManageService;

    /**
     * 获取订单配送是否管制
     * @param controlLogisticsOrderDto
     * @return
     */
    @LogRecord
    public String orderCanSend(ControlLogisticsOrderDto controlLogisticsOrderDto) {
        return logisticsOrderService.controlLogistics(controlLogisticsOrderDto);
    }

    /**
     * 顺丰同城计算距离
     * @param logisticsDistanceDto
     * @return
     */
    @LogRecord
    public Map<String,String> controlLogisticsDistance(LogisticsDistanceDto logisticsDistanceDto) {
        return logisticsOrderService.controlLogisticsDistance(logisticsDistanceDto);
    }

    /**
     * 获取预估价格金额
     * @param wayBillExceptPriceTO
     * @return
     */
    @LogRecord
    public String getExpectPrice(WayBillExceptPriceTO wayBillExceptPriceTO) {
        return logisticsOrderService.getExpectPrice(wayBillExceptPriceTO);
    }

    /**
     *
     * @param depId
     * @param logisticsCompany
     * @param type
     * @param var4
     * @return
     */
    @LogRecord
    public List<OrganLogisticsManageDto> findLogisticsManageByOrganIdAndLogisticsCompanyIdAndAccount(Integer depId,String logisticsCompany,Integer type,Integer var4) {
        return organLogisticsManageService.findLogisticsManageByOrganIdAndLogisticsCompanyIdAndAccount(depId,logisticsCompany,type,var4);
    }



}
