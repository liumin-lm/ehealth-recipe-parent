package recipe.drugsenterprise.bean;

import com.ngari.recipe.common.anno.Verify;
import ctd.schema.annotation.Schema;

import java.io.Serializable;

/**
 * @author yinsheng
 * @date 2019\2\28 0028 15:26
 * 对接药企 无需脱敏
 */
@Schema
public class PatientParamDTO implements Serializable {

    private static final long serialVersionUID = -6256585787778201872L;

    @Verify(isNotNull = true, desc = "患者编号", maxLength = 64)
    private String number;

    @Verify(isNotNull = true, desc = "患者名称", maxLength = 64)
    private String name;

    @Verify(isNotNull = true, desc = "患者性别，只能填 F, M", maxLength = 10)
    private String sex;

    @Verify(isNotNull = true, desc = "患者年龄")
    private Integer age;

    @Verify(isNotNull = false, desc = "患者⼿机号", maxLength = 11)
    private String telphone;

    @Verify(isNotNull = false, desc = "患者地址", maxLength = 100)
    private String address;

    @Verify(isNotNull = false, desc = "身份证号", maxLength = 20)
    private String idCardNum;

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getTelphone() {
        return telphone;
    }

    public void setTelphone(String telphone) {
        this.telphone = telphone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getIdCardNum() {
        return idCardNum;
    }

    public void setIdCardNum(String idCardNum) {
        this.idCardNum = idCardNum;
    }
}
