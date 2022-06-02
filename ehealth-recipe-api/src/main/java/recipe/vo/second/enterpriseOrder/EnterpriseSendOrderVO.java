package recipe.vo.second.enterpriseOrder;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Setter
@Getter
public class EnterpriseSendOrderVO implements Serializable {
    private static final long serialVersionUID = 9078330874662954562L;

    private String orderCode;
    private String sendDate;
    private String sender;
    private String logisticsCompany;
    private String trackingNumber;
}
