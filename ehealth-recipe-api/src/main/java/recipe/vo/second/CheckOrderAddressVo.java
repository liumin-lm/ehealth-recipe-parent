package recipe.vo.second;

import ctd.schema.annotation.Dictionary;
import ctd.schema.annotation.ItemProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * @description： 端校验地址入参
 * @author： whf
 * @date： 2022-07-14 16:29
 */
@Getter
@Setter
@ToString
public class CheckOrderAddressVo implements Serializable {
    private static final long serialVersionUID = 6085755010478778897L;

    @ItemProperty(alias = "药企id")
    private Integer enterpriseId;

    @ItemProperty(alias = "地址（省）")
    @Dictionary(id = "eh.base.dictionary.AddrArea")
    private String address1;

    @ItemProperty(alias = "地址（市）")
    @Dictionary(id = "eh.base.dictionary.AddrArea")
    private String address2;

    @ItemProperty(alias = "地址（区县）")
    @Dictionary(id = "eh.base.dictionary.AddrArea")
    private String address3;

    @ItemProperty(alias = "街道")
    @Dictionary(id = "eh.base.dictionary.AddrArea")
    private String address4;
}
