package recipe.drugsenterprise.bean;

import com.ngari.recipe.common.anno.Verify;
import ctd.schema.annotation.Schema;

import java.io.Serializable;

/**
 * @author yinsheng
 * @date 2019\2\28 0028 15:14
 */
@Schema
public class DoctorParamDTO implements Serializable{

    private static final long serialVersionUID = 1487739977676938455L;

    @Verify(isNotNull = true, desc = "医院编号Id", maxLength = 64)
    private String hospitalId;

    @Verify(isNotNull = true, desc = "医院名称", maxLength = 64)
    private String hospitalName;

    @Verify(isNotNull = true, desc = "科室名称", maxLength = 64)
    private String deptName;

    @Verify(isNotNull = true, desc = "医⽣名称", maxLength = 10)
    private String doctorName;

    @Verify(isNotNull = false, desc = "医⽣执业资格证号", maxLength = 64)
    private String doctorQualificationNum;

    public String getHospitalId() {
        return hospitalId;
    }

    public void setHospitalId(String hospitalId) {
        this.hospitalId = hospitalId;
    }

    public String getHospitalName() {
        return hospitalName;
    }

    public void setHospitalName(String hospitalName) {
        this.hospitalName = hospitalName;
    }

    public String getDeptName() {
        return deptName;
    }

    public void setDeptName(String deptName) {
        this.deptName = deptName;
    }

    public String getDoctorName() {
        return doctorName;
    }

    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }

    public String getDoctorQualificationNum() {
        return doctorQualificationNum;
    }

    public void setDoctorQualificationNum(String doctorQualificationNum) {
        this.doctorQualificationNum = doctorQualificationNum;
    }
}
