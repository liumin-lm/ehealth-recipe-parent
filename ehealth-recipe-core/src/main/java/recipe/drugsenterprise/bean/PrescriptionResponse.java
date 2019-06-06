package recipe.drugsenterprise.bean;

import com.ngari.recipe.common.anno.Verify;
import ctd.schema.annotation.Schema;

import java.io.Serializable;
import java.util.List;

/**
 * @author yinsheng
 * @date 2019\3\1 0001 16:11
 */
@Schema
public class PrescriptionResponse implements Serializable{
    private static final long serialVersionUID = -2137249697668944726L;

    @Verify(desc = "处⽅Id")
    private String rxId;

    @Verify(desc = "处⽅状态")
    private Integer status;

    @Verify(desc = "处方能否用于购药，判断能否下单")
    private Boolean canUse;

    @Verify(desc = "处⽅关联的订单列表")
    private List<RxOrderResponse> rxOrderList;

    public String getRxId() {
        return rxId;
    }

    public void setRxId(String rxId) {
        this.rxId = rxId;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Boolean getCanUse() {
        return canUse;
    }

    public void setCanUse(Boolean canUse) {
        this.canUse = canUse;
    }

    public List<RxOrderResponse> getRxOrderList() {
        return rxOrderList;
    }

    public void setRxOrderList(List<RxOrderResponse> rxOrderList) {
        this.rxOrderList = rxOrderList;
    }
}
