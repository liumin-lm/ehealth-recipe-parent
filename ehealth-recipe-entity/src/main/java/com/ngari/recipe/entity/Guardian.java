package com.ngari.recipe.entity;

/**
 * 监护人信息
 * @author yinsheng
 * @date 2019\2\19 0019 14:55
 */
public class Guardian {
    private String name;
    private Integer age;
    private String sex;

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
}
