package recipe.openapi.business.request;

import lombok.Data;

import java.io.Serializable;

/**
 * @author yinsheng
 * @date 2021\6\8 0008 20:16
 */
@Data
public class ThirdLogisticsRequest implements Serializable {

    private String appkey;

    private String tid;

    private Integer orderId;
}
