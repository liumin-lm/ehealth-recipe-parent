package recipe.client;

import com.ngari.infra.logistics.mode.ControlLogisticsOrderDto;
import com.ngari.infra.logistics.service.ILogisticsOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.aop.LogRecord;

/**
 * @description： 物流client
 * @author： whf
 * @date： 2022-03-02 14:22
 */
@Service
public class InfraClient extends BaseClient {

    @Autowired
    private ILogisticsOrderService logisticsOrderService;

    /**
     * 获取订单配送是否管制
     * @param controlLogisticsOrderDto
     * @return
     */
    @LogRecord
    public String orderCanSend(ControlLogisticsOrderDto controlLogisticsOrderDto) {
        return logisticsOrderService.controlLogistics(controlLogisticsOrderDto);
    }
}
