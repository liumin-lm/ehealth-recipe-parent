package recipe.logistics;

import java.io.Serializable;
import java.util.List;

public class LogisticsTraceResponse implements Serializable {
    private static final long serialVersionUID = 2459704364437343485L;
    private String LogisticCode;
    private String ShipperCode;
    private List<LogisticsTrace> Traces;
    private String State;
    private boolean Success;
    private String EBusinessID;

    public LogisticsTraceResponse() {
    }

    public String getLogisticCode() {
        return this.LogisticCode;
    }

    public void setLogisticCode(String logisticCode) {
        this.LogisticCode = logisticCode;
    }

    public String getShipperCode() {
        return this.ShipperCode;
    }

    public void setShipperCode(String shipperCode) {
        this.ShipperCode = shipperCode;
    }

    public List<LogisticsTrace> getTraces() {
        return this.Traces;
    }

    public void setTraces(List<LogisticsTrace> traces) {
        this.Traces = traces;
    }

    public String getState() {
        return this.State;
    }

    public void setState(String state) {
        this.State = state;
    }

    public boolean isSuccess() {
        return this.Success;
    }

    public void setSuccess(boolean success) {
        this.Success = success;
    }

    public String getEBusinessID() {
        return this.EBusinessID;
    }

    public void setEBusinessID(String EBusinessID) {
        this.EBusinessID = EBusinessID;
    }
}