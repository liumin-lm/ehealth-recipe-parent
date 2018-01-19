package recipe.prescription.bean;

import java.util.List;

/**
 * Created by jiangtingfeng on 2017/11/15.
 * @author jiangtingfeng
 */
public class PAWebMedicines {

    private String Name;

    private String Code;

    private String HospCode;

    private List<Issue> Issues;

    public String getName() {
        return Name;
    }

    public void setName(String name) {
        Name = name;
    }

    public String getCode() {
        return Code;
    }

    public void setCode(String code) {
        Code = code;
    }

    public String getHospCode() {
        return HospCode;
    }

    public void setHospCode(String hospCode) {
        HospCode = hospCode;
    }

    public List<Issue> getIssues() {
        return Issues;
    }

    public void setIssues(List<Issue> issues) {
        Issues = issues;
    }
}
