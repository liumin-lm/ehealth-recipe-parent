package recipe.vo.greenroom;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Setter
@Getter
public class AuditRefundVO implements Serializable {
    private static final long serialVersionUID = 2016504773576708794L;

    private String orderCode;
    private String reason;
    private Boolean result;
    private String rejectReason;
    private String time;
}
