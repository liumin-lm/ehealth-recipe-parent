package recipe.medicationguide.bean;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

/**
 * created by shiyuping on 2019/10/28
 * @author shiyuping
 */
public class HospInfoDTO implements Serializable {
    private static final long serialVersionUID = 3516506344151855307L;
    /**
     * 医院唯一ID
     */
    @JsonProperty("ID")
    private String id;
    /**
     * 组织机构代码
     */
    @JsonProperty("OrganizationalCode")
    private String organizationalCode;
    /**
     * 区域代码
     */
    @JsonProperty("AreaCode")
    private String areaCode;
    /**
     * 医院名称
     */
    @JsonProperty("Name")
    private String name;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOrganizationalCode() {
        return organizationalCode;
    }

    public void setOrganizationalCode(String organizationalCode) {
        this.organizationalCode = organizationalCode;
    }

    public String getAreaCode() {
        return areaCode;
    }

    public void setAreaCode(String areaCode) {
        this.areaCode = areaCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
