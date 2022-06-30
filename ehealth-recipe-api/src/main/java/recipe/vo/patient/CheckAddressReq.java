package recipe.vo.patient;

import ctd.schema.annotation.ItemProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * @description： 患者端校验地址入参
 * @author： whf
 * @date： 2022-04-07 18:29
 */
@Getter
@Setter
public class CheckAddressReq implements Serializable {
    @ItemProperty(alias = "药企序号")
    private Integer enterpriseId;

    @ItemProperty(alias = "煎法序号")
    private Integer decoctionId;

    @ItemProperty(alias = "机构id")
    private Integer organId;
    //省
    private String address1;
    //市
    private String address2;
    //区
    private String address3;
    //街道
    private String address4;
}
