package recipe.drugsenterprise.bean;

import java.io.Serializable;

/**
 * @author yinsheng
 * @date 2019\10\25 0025 10:49
 * 对接药企 无需脱敏
 */
public class StoreAddress implements Serializable {
    private static final long serialVersionUID = -3855595066266874301L;

    private String Province;
    private String City;
    private String District;
    private String Street;
    private String Detail;
    private String Description;
    private String address;

    public String getProvince() {
        return Province;
    }

    public void setProvince(String province) {
        Province = province;
    }

    public String getCity() {
        return City;
    }

    public void setCity(String city) {
        City = city;
    }

    public String getDistrict() {
        return District;
    }

    public void setDistrict(String district) {
        District = district;
    }

    public String getStreet() {
        return Street;
    }

    public void setStreet(String street) {
        Street = street;
    }

    public String getDetail() {
        return Detail;
    }

    public void setDetail(String detail) {
        Detail = detail;
    }

    public String getDescription() {
        return Description;
    }

    public void setDescription(String description) {
        Description = description;
    }

    public String getAddress() {
        return this.Province + this.City + this.District + this.Street + this.Detail;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
