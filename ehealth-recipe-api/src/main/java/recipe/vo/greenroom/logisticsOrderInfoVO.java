package recipe.vo.greenroom;

import lombok.Data;

import java.io.Serializable;

/**
 * @Author zgy
 * @Date 2023-01-04
 * 物流详情
 */
@Data
public class logisticsOrderInfoVO implements Serializable {
    private static final long serialVersionUID = -7814912818811915815L;

    //获取三级分拣码
    private String logisticsOrderSortCode;

    //获取物流运单条形码
    private String logisticsOrderNo;

    //获取物流详情
    private LogisticsOrderDetailsVO logisticsOrderDetailsVO;

}
