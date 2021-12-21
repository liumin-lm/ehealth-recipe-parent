package recipe.drugsenterprise.bean;

import java.io.Serializable;

/**
 * @author yinsheng
 * @date 2019\10\28 0028 16:20
 * 对接药企 无需脱敏
 */
public class ReceiveAddress implements Serializable {
    private static final long serialVersionUID = -9088079972837447392L;

    private String Phone;
    private String Receiver;
    private String Province;
    private String City;
    private String District;
    private String Street;
    private String Detail;
    private String Description;

    public String getPhone() {
        return Phone;
    }

    public void setPhone(String phone) {
        Phone = phone;
    }

    public String getReceiver() {
        return Receiver;
    }

    public void setReceiver(String receiver) {
        Receiver = receiver;
    }

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
}
