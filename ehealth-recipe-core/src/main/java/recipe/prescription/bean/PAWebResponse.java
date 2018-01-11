package recipe.prescription.bean;

import java.util.List;

/**
 * Created by jiangtingfeng on 2017/11/15.
 * @author jiangtingfeng
 */
public class PAWebResponse {
    private String UINotify;

    private List<PAWebMedicines> medicines;

    private String hospCode;

    private String hospFlag;

    private PAWebPatient patient;

    private String id;

    private String result;

    private String msg;

    private String brief;

    public String getUINotify() {
        return UINotify;
    }

    public void setUINotify(String UINotify) {
        this.UINotify = UINotify;
    }

    public List<PAWebMedicines> getMedicines() {
        return medicines;
    }

    public void setMedicines(List<PAWebMedicines> medicines) {
        this.medicines = medicines;
    }

    public String getHospCode() {
        return hospCode;
    }

    public void setHospCode(String hospCode) {
        this.hospCode = hospCode;
    }

    public String getHospFlag() {
        return hospFlag;
    }

    public void setHospFlag(String hospFlag) {
        this.hospFlag = hospFlag;
    }

    public PAWebPatient getPatient() {
        return patient;
    }

    public void setPatient(PAWebPatient patient) {
        this.patient = patient;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getBrief() {
        return brief;
    }

    public void setBrief(String brief) {
        this.brief = brief;
    }
}
