package recipe.vo.second;

import ctd.schema.annotation.Dictionary;
import ctd.schema.annotation.ItemProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * @description： 校验地址Vo
 * @author： whf
 * @date： 2022-05-24 14:17
 */
@Getter
@Setter
@ToString
public class CheckAddressVo implements Serializable {

    @ItemProperty(alias = "机构id")
    private Integer organId;

    @ItemProperty(alias = "地址（省）")
    @Dictionary(id = "eh.base.dictionary.AddrArea")
    private String address1;

    @ItemProperty(alias = "地址（市）")
    @Dictionary(id = "eh.base.dictionary.AddrArea")
    private String address2;

    @ItemProperty(alias = "地址（区县）")
    @Dictionary(id = "eh.base.dictionary.AddrArea")
    private String address3;
}
