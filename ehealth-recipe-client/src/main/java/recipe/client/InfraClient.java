package recipe.client;

import com.alibaba.fastjson.JSON;
import com.ngari.infra.logistics.mode.*;
import com.ngari.infra.logistics.service.ILogisticsOrderService;
import com.ngari.infra.logistics.service.IOrganLogisticsManageService;
import com.ngari.infra.statistics.dto.EventLogDTO;
import com.ngari.recipe.dto.ServiceLogDTO;
import com.ngari.recipe.entity.RecipeOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.aop.LogRecord;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
     *
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
     * 获取预估价格金额 新接口
     * @param logisticsEmsPriceDto
     * @return
     */
    @LogRecord
    public LogisticsEmsPriceInfoDto getLogisticsEstimatedPrice(LogisticsEmsPriceDto logisticsEmsPriceDto) {
        return logisticsOrderService.getLogisticsEstimatedPrice(logisticsEmsPriceDto);
    }

    /**
     * @param depId
     * @param logisticsCompany
     * @param type
     * @param var4
     * @return
     */
    @LogRecord
    public List<OrganLogisticsManageDto> findLogisticsManageByOrganIdAndLogisticsCompanyIdAndAccount(Integer depId, String logisticsCompany, Integer type, Integer var4) {
        return organLogisticsManageService.findLogisticsManageByOrganIdAndLogisticsCompanyIdAndAccount(depId, logisticsCompany, type, var4);
    }


    /**
     * 增加时间日志分析
     *
     * @param serviceLog 日志分析对象
     */
    public void serviceTimeLog(ServiceLogDTO serviceLog) {
        logger.info("InfraClient serviceLog serviceLog={}", serviceLog);
        EventLogDTO eventLog = new EventLogDTO();
        eventLog.setMilli_timestamp(String.valueOf(System.currentTimeMillis()));
        eventLog.setEvent_uuid(UUID.randomUUID().toString());
        eventLog.setSource(serviceLog.getSource());
        eventLog.setName(serviceLog.getName());
        ServiceLogDTO serviceLog1 = new ServiceLogDTO();
        serviceLog1.setId(serviceLog.getId());
        serviceLog1.setType(serviceLog.getType());
        serviceLog1.setCategory(serviceLog.getCategory());
        serviceLog1.setSize(serviceLog.getSize());
        serviceLog1.setTime(serviceLog.getTime());
        eventLog.setData(serviceLog1);
        try {
            eventLogService.serviceLog(Collections.singletonList(eventLog));
        } catch (Exception e) {
            logger.error("InfraClient serviceLog error", e);
        }
    }

    public void serviceTimeLog(String name, Integer id, Integer type, Integer size, Long time) {
        logger.info("InfraClient serviceLog name={},id={},type={},size={},time={},", name, id, type, size, time);
        super.serviceLog(name, id, type, size, time);
    }

    /**
     * 获取物流编码
     *
     * @param orderCode
     */
    public String logisticsOrderNo(String orderCode) {
        try {
            return logisticsOrderService.waybillBarCodeByLogisticsOrderNo(1, orderCode);
        } catch (Exception e) {
            logger.error("InfraClient logisticsOrderNo orderCode={}", orderCode, e);
            return "";
        }
    }

    /**
     * 取消物流单
     * @param recipeOrder 订单号
     * @param isCancelLogistics 是否取消物流
     * @return
     */
    public Boolean cancelLogisticsOrder(RecipeOrder recipeOrder, Boolean isCancelLogistics){
        logger.info("InfraClient cancelLogisticsOrder recipeOrder:{},isCancelLogistics:{}", JSON.toJSONString(recipeOrder), isCancelLogistics);
        try {
            LogisticsCancelDto logisticsCancelDto = new LogisticsCancelDto();
            logisticsCancelDto.setBusinessType(1);
            logisticsCancelDto.setBusinessNo(recipeOrder.getOrderCode());
            logisticsCancelDto.setWaybillNo(recipeOrder.getTrackingNumber());
            logger.info("InfraClient cancelLogisticsOrder logisticsCancelDto:{}", JSON.toJSONString(logisticsCancelDto));
            LogisticsCancelRespDto logisticsCancelRespDto;
            if (isCancelLogistics) {
                logisticsCancelRespDto = logisticsOrderService.cancelOrder(logisticsCancelDto);
            } else {
                logisticsCancelRespDto = logisticsOrderService.ifCancelOrder(logisticsCancelDto);
            }
            logger.info("InfraClient cancelLogisticsOrder logisticsCancelRespDto:{}", JSON.toJSONString(logisticsCancelRespDto));
            return logisticsCancelRespDto.getIfRefund();
        } catch (Exception e) {
            logger.error("InfraClient cancelLogisticsOrder error", e);
        }
        return true;
    }
}
