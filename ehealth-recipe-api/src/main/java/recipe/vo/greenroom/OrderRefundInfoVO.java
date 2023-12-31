package recipe.vo.greenroom;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class OrderRefundInfoVO implements Serializable {
    private static final long serialVersionUID = -4199810987374797264L;

    private String channel;
    private String refundStatusText;
    private String refundNodeStatusText;
    private boolean forceApplyFlag;
    private Integer auditNodeType;
    private boolean retryFlag;
    private String applyReason;
    private String applyTime;
    private String orderStatusText;
    private Integer refundNodeStatus;
    private String refuseReason;
}
