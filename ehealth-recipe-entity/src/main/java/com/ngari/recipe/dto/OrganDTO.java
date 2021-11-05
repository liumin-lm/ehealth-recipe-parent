package com.ngari.recipe.dto;

import ctd.schema.annotation.Dictionary;
import ctd.schema.annotation.ItemProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;


/**
 * 机构信息对象
 *
 * @author fuzi
 */
@Getter
@Setter
public class OrganDTO implements Serializable {
    private static final long serialVersionUID = 4530887375164839450L;
    private Integer organId;
    private String organizeCode;
    private String name;
    private String shortName;
    private String pyCode;
    @ItemProperty(alias = "机构类型")
    @Dictionary(id = "eh.base.dictionary.Type")
    private String type;
    @ItemProperty(alias = "机构等级")
    @Dictionary(id = "eh.base.dictionary.Grade")
    private String grade;
    private String representative;
    @ItemProperty(alias = "行政归属")
    @Dictionary(id = "eh.base.dictionary.AdminArea")
    private String adminArea;
    @ItemProperty(alias = "属地区域")
    @Dictionary(id = "eh.base.dictionary.AddrArea")
    private String addrArea;
    private String address;
    private Double longituder;
    private Double latitude;
    private String busLine;
    private String hostCode;
    private String manageUnit;
}

