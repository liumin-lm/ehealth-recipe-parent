package com.ngari.recipe.hisprescription.model;

import ctd.schema.annotation.Desensitizations;
import ctd.schema.annotation.DesensitizationsType;
import ctd.schema.annotation.ItemProperty;

import java.io.Serializable;
import java.util.Date;

/**
 * @author Created by liuxiaofeng on 2020/11/5.
 */
public class UserInfoDTO implements Serializable {
    private static final long serialVersionUID = -7145474926866174754L;

    @Desensitizations(type = DesensitizationsType.MOBILE)
    @ItemProperty(alias = "就诊人手机号")
    private String userMobile;

    @ItemProperty(alias = "就诊人性别")
    private String userSex;

    @ItemProperty(alias = "就诊人名称")
    private String usernName;

    @ItemProperty(alias = "就诊人出生日期")
    private Date userBirthDay;

    public String getUserMobile() {
        return userMobile;
    }

    public void setUserMobile(String userMobile) {
        this.userMobile = userMobile;
    }

    public String getUserSex() {
        return userSex;
    }

    public void setUserSex(String userSex) {
        this.userSex = userSex;
    }

    public String getUsernName() {
        return usernName;
    }

    public void setUsernName(String usernName) {
        this.usernName = usernName;
    }

    public Date getUserBirthDay() {
        return userBirthDay;
    }

    public void setUserBirthDay(Date userBirthDay) {
        this.userBirthDay = userBirthDay;
    }
}
