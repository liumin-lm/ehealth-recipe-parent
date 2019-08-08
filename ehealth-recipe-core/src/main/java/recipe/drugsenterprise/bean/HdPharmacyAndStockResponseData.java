package recipe.drugsenterprise.bean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
/**
* @Description: HdPharmacyAndStockResponseData 类（或接口）是 药店列表可库存情况的响应数据
* @Author: JRK
* @Date: 2019/7/24
*/
public class HdPharmacyAndStockResponseData implements Serializable {
    private static final long serialVersionUID = -8430277165096927654L;

    private static final Logger LOGGER = LoggerFactory.getLogger(HdPharmacyAndStockResponseData.class);
    /**
     * 药房ID
     */
    private String pharmacyId;
    /**
     * 药房code
     */
    private String pharmacyCode;
    /**
     * 药房name
     */
    private String pharmacyName;
    /**
     * 药店的经纬度
     */
    private HdPosition position;
    /**
     * 药店地址
     */
    private String address;
    /**
     * 药店下药品的库存信息
     */
    private List<HdDrugResponseData> drugInvs;
    /**
     * 药店处方的总价格
     */
    private BigDecimal totalFee;

    /**
     * 初始化操作
     */
    public boolean init(){
        boolean result = true;
        this.totalFee = new BigDecimal(0.0d);
        for (HdDrugResponseData drugsFee : drugInvs) {
            if(null == drugsFee.getPrice() || null == drugsFee.getInvQty()){
                LOGGER.warn("HdRemoteService初始化处方单金额:[{}][{}]药店下[{}]药品价格信息不全."
                        , this.pharmacyId, this.pharmacyCode, drugsFee.getDrugCode());
                result = false;
                return result;
            }
            this.totalFee.add(new BigDecimal(drugsFee.getPrice()).multiply(new BigDecimal(drugsFee.getInvQty())));
        }
        return result;
    }

    public String getPharmacyId() {
        return pharmacyId;
    }

    public void setPharmacyId(String pharmacyId) {
        this.pharmacyId = pharmacyId;
    }

    public String getPharmacyCode() {
        return pharmacyCode;
    }

    public void setPharmacyCode(String pharmacyCode) {
        this.pharmacyCode = pharmacyCode;
    }

    public String getPharmacyName() {
        return pharmacyName;
    }

    public void setPharmacyName(String pharmacyName) {
        this.pharmacyName = pharmacyName;
    }

    public HdPosition getPosition() {
        return position;
    }

    public void setPosition(HdPosition position) {
        this.position = position;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public List<HdDrugResponseData> getDrugInvs() {
        return drugInvs;
    }

    public void setDrugInvs(List<HdDrugResponseData> drugInvs) {
        this.drugInvs = drugInvs;
    }

    public BigDecimal getTotalFee() {
        return totalFee;
    }

    public void setTotalFee(BigDecimal totalFee) {
        this.totalFee = totalFee;
    }
}