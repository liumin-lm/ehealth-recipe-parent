package recipe.logistics;

import java.io.Serializable;

public class LogisticsTrace implements Serializable {
    private static final long serialVersionUID = -3399180299799960104L;
    private String AcceptStation;
    private String AcceptTime;

    public LogisticsTrace() {
    }

    public String getAcceptStation() {
        return this.AcceptStation;
    }

    public void setAcceptStation(String acceptStation) {
        this.AcceptStation = acceptStation;
    }

    public String getAcceptTime() {
        return this.AcceptTime;
    }

    public void setAcceptTime(String acceptTime) {
        this.AcceptTime = acceptTime;
    }
}