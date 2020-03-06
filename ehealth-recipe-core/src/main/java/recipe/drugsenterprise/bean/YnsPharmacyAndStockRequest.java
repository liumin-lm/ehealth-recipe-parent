package recipe.drugsenterprise.bean;

import java.io.Serializable;
import java.util.List;

/**
* @Description: HdPharmacyAndStockRequest 类（或接口）是 华东请求药店列表和库存的请求对象
* @Author: JRK
* @Date: 2019/7/24
*/
public class YnsPharmacyAndStockRequest implements Serializable {

    private static final long serialVersionUID = -2057303969906458294L;

    /**
     * 当前用户位置信息
     */
    private HdPosition position;
    /**
     *距离信息
     * 获取库存信息（必须）
     */
    private String range;
    /**
     *药品列表
     * 获取库存信息（必须）
     */
    private List<HdDrugRequestData> drugList;

    public HdPosition getPosition() {
        return position;
    }

    public void setPosition(HdPosition position) {
        this.position = position;
    }

    public String getRange() {
        return range;
    }

    public void setRange(String range) {
        this.range = range;
    }

    public List<HdDrugRequestData> getDrugList() {
        return drugList;
    }

    public void setDrugList(List<HdDrugRequestData> drugList) {
        this.drugList = drugList;
    }
}