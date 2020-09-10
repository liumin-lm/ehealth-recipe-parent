package recipe.bean;

import com.ngari.recipe.common.anno.Verify;

import java.io.Serializable;
import java.util.List;

/**
 * @author Created by dell on 2020/9/4.
 *         增加医生用药频次、途径使用次数入参
 */
public class DoctorDrugUsageRequest implements Serializable {
    private static final long serialVersionUID = 3116680937100142670L;


    @Verify(desc = "机构id", isInt = true)
    private Integer organId;

    @Verify(desc = "医生id", isInt = true)
    private Integer doctorId;

    @Verify(desc = "医生科室id", isNotNull = false)
    private String deptId;

    @Verify(desc = "药品id列表", isNotNull = false)
    private List<Integer> drugIds;

    @Verify(desc = "用药频次id", isNotNull = false, isInt = true)
    private Integer usingRateId;

    @Verify(desc = "用药途径id", isNotNull = false, isInt = true)
    private Integer usePathwayId;
    
    @Verify(desc = "使用次数", isNotNull = false, isInt = true)
    private Integer addCount = 1;

    public Integer getOrganId() {
        return organId;
    }

    public void setOrganId(Integer organId) {
        this.organId = organId;
    }

    public Integer getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(Integer doctorId) {
        this.doctorId = doctorId;
    }

    public String getDeptId() {
        return deptId;
    }

    public void setDeptId(String deptId) {
        this.deptId = deptId;
    }

    public List<Integer> getDrugIds() {
        return drugIds;
    }

    public void setDrugIds(List<Integer> drugIds) {
        this.drugIds = drugIds;
    }

    public Integer getUsingRateId() {
        return usingRateId;
    }

    public void setUsingRateId(Integer usingRateId) {
        this.usingRateId = usingRateId;
    }

    public Integer getUsePathwayId() {
        return usePathwayId;
    }

    public void setUsePathwayId(Integer usePathwayId) {
        this.usePathwayId = usePathwayId;
    }

    public Integer getAddCount() {
        return addCount;
    }

    public void setAddCount(Integer addCount) {
        this.addCount = addCount;
    }
}
