package recipe.drugsenterprise.bean;

import java.io.Serializable;

/**
 * @author yinsheng
 * @date 2019\10\25 0025 10:44
 */
public class HrStoreBean implements Serializable {
    private static final long serialVersionUID = 8815764246047135200L;

    private String StoreName;
    private String StoreId;
    private StorePosition StorePosition;
    private StoreAddress StoreAddress;
    private Double Distance;

    public String getStoreName() {
        return StoreName;
    }

    public void setStoreName(String storeName) {
        StoreName = storeName;
    }

    public String getStoreId() {
        return StoreId;
    }

    public void setStoreId(String storeId) {
        StoreId = storeId;
    }

    public recipe.drugsenterprise.bean.StorePosition getStorePosition() {
        return StorePosition;
    }

    public void setStorePosition(recipe.drugsenterprise.bean.StorePosition storePosition) {
        StorePosition = storePosition;
    }

    public recipe.drugsenterprise.bean.StoreAddress getStoreAddress() {
        return StoreAddress;
    }

    public void setStoreAddress(recipe.drugsenterprise.bean.StoreAddress storeAddress) {
        StoreAddress = storeAddress;
    }

    public Double getDistance() {
        return Distance;
    }

    public void setDistance(Double distance) {
        Distance = distance;
    }
}
