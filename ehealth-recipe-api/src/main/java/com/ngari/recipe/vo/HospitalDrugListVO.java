package com.ngari.recipe.vo;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Setter
@Getter
public class HospitalDrugListVO implements Serializable {
    private static final long serialVersionUID = -8382937824399340035L;

    //药品名称
    private String drugName;
    //药品商品名
    private String saleName;
    //药品代码
    private String drugCode;
    //医保代码
    private String medicalDrugCode;
    //医保名称
    private String medicalDrugName;
    //单价
    private String price;
    //药品类别名称
    private String drugTypeName;
    //药品规格
    private String drugSpec;
    //库存数量
    private String total;
    //互联网药品标志
    private String internetFlag;
    //生产厂家
    private String producer;
    //药品单位
    private String unit;

}
