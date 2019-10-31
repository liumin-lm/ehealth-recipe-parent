package recipe.drugsenterprise.bean;

import java.io.Serializable;

/**
 * @author yinsheng
 * @date 2019\10\28 0028 16:24
 */
public class HrPatient implements Serializable{
    private static final long serialVersionUID = 8578251773285040016L;

    private String PatientId;
    private String Name;
    private String IdentityType;
    private String IdentityNo;
    private Integer Sex;
    private String Birthday;
    private String Mobile;
    private LinkAddress LinkAddress;

    public String getPatientId() {
        return PatientId;
    }

    public void setPatientId(String patientId) {
        PatientId = patientId;
    }

    public String getName() {
        return Name;
    }

    public void setName(String name) {
        Name = name;
    }

    public String getIdentityType() {
        return IdentityType;
    }

    public void setIdentityType(String identityType) {
        IdentityType = identityType;
    }

    public String getIdentityNo() {
        return IdentityNo;
    }

    public void setIdentityNo(String identityNo) {
        IdentityNo = identityNo;
    }

    public Integer getSex() {
        return Sex;
    }

    public void setSex(Integer sex) {
        Sex = sex;
    }

    public String getBirthday() {
        return Birthday;
    }

    public void setBirthday(String birthday) {
        Birthday = birthday;
    }

    public String getMobile() {
        return Mobile;
    }

    public void setMobile(String mobile) {
        Mobile = mobile;
    }

    public recipe.drugsenterprise.bean.LinkAddress getLinkAddress() {
        return LinkAddress;
    }

    public void setLinkAddress(recipe.drugsenterprise.bean.LinkAddress linkAddress) {
        LinkAddress = linkAddress;
    }
}
