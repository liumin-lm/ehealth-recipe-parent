package com.ngari.recipe.recipe.model;

import ctd.schema.annotation.Desensitizations;
import ctd.schema.annotation.DesensitizationsType;
import ctd.schema.annotation.Schema;

import java.io.Serializable;

/**
 * 监护人信息
 *
 * @author yinsheng
 * @date 2019\2\19 0019 14:55
 */
@Schema
public class GuardianBean implements Serializable {

    private static final long serialVersionUID = -8882418262625511814L;
    private String name;
    private Integer age;
    private String sex;
    /**
     * 证件号
     */
    @Desensitizations(type = DesensitizationsType.IDCARD)
    private String guardianCertificate;
    @Desensitizations(type = DesensitizationsType.MOBILE)
    private String mobile;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public String getGuardianCertificate() {
        return guardianCertificate;
    }

    public void setGuardianCertificate(String guardianCertificate) {
        this.guardianCertificate = guardianCertificate;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }
}
