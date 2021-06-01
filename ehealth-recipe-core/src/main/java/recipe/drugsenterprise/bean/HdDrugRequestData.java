package recipe.drugsenterprise.bean;

import java.io.Serializable;
/**
* @Description: HdDrugRequestData 类（或接口）是 华东药请求数据
* @Author: JRK
* @Date: 2019/7/24
*/
public class HdDrugRequestData implements Serializable {

    private static final long serialVersionUID = -9026148023566862644L;
    /**
     * 药品编码（处方药平台编码)
     */
    private String drugCode;
    /**
     * 开药总数
     */
    private String total;
    /**
     * 单位
     */
    private String unit;

    public String getDrugCode() {
        return drugCode;
    }

    public void setDrugCode(String drugCode) {
        this.drugCode = drugCode;
    }

    public String getTotal() {
        return total;
    }

    public void setTotal(String total) {
        this.total = total;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof HdDrugRequestData)) return false;

        HdDrugRequestData that = (HdDrugRequestData) o;

        return getDrugCode().equals(that.getDrugCode());

    }

    @Override
    public int hashCode() {
        return getDrugCode().hashCode();
    }
}