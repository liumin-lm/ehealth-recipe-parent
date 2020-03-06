package recipe.bussutil.openapi.request.province;

import javax.validation.constraints.NotNull;
import java.util.Date;

/**
 * 科室指标数据导入
 * 校验是否存在
 */
public class DeptIndicatorsReq implements java.io.Serializable {

    private static final long serialVersionUID = 7359125152450918061L;
    @NotNull
    private String organID;
    @NotNull
    private String  organName;
    @NotNull
    private String subjectCode; // 所属诊疗科目代码
    @NotNull
    private String subjectName; // 所属诊疗科目名称

    private String deptID;//开展诊疗的临床科室代码
    private String deptName;//开展诊疗的临床科室代码
    private String deptType;//1临床科室 2药剂科室 3 护理科室、4医技科室
    private String desc; // 诊疗科目描述
    private String note; // 诊疗科目备注
    @NotNull
    private Date   updateTime;//最后更新时间
    @NotNull
    private String  unitID;



    public String getUnitID() {
        return unitID;
    }

    public void setUnitID(String unitID) {
        this.unitID = unitID;
    }

    public String getOrganID() {
        return organID;
    }

    public void setOrganID(String organID) {
        this.organID = organID;
    }

    public String getOrganName() {
        return organName;
    }

    public void setOrganName(String organName) {
        this.organName = organName;
    }

    public String getSubjectCode() {
        return subjectCode;
    }

    public void setSubjectCode(String subjectCode) {
        this.subjectCode = subjectCode;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public String getDeptID() {
        return deptID;
    }

    public void setDeptID(String deptID) {
        this.deptID = deptID;
    }

    public String getDeptName() {
        return deptName;
    }

    public void setDeptName(String deptName) {
        this.deptName = deptName;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public String getDeptType() {
        return deptType;
    }

    public void setDeptType(String deptType) {
        this.deptType = deptType;
    }
}
