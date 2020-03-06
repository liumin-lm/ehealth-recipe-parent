package recipe.bussutil.openapi.request.province;
import javax.validation.constraints.NotNull;
import java.util.Date;

public class OrganProtocolReq implements java.io.Serializable{

    private static final long serialVersionUID = 894310264942757457L;

    @NotNull
    private String  organID;
    @NotNull
    private String  unitID;
    @NotNull
    private String  organName;
    @NotNull
    private String  protocolType;
    private String  status;
    @NotNull
    private String  protocolContent;
    @NotNull
    private Date    releaseDate;
    @NotNull
    private Date    updateTime;

    public String getOrganID() {
        return organID;
    }

    public void setOrganID(String organID) {
        this.organID = organID;
    }
    public String getUnitID() {
        return unitID;
    }

    public void setUnitID(String unitID) {
        this.unitID = unitID;
    }
    public String getOrganName() {
        return organName;
    }

    public void setOrganName(String organName) {
        this.organName = organName;
    }
    public String getProtocolType() {
        return protocolType;
    }

    public void setProtocolType(String protocolType) {
        this.protocolType = protocolType;
    }
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
    public String getProtocolContent() {
        return protocolContent;
    }

    public void setProtocolContent(String protocolContent) {
        this.protocolContent = protocolContent;
    }
    public Date getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(Date releaseDate) {
        this.releaseDate = releaseDate;
    }
    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }
}
