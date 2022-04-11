package recipe.vo.patient;

import ctd.schema.annotation.Dictionary;
import ctd.schema.annotation.ItemProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * @description：
 * @author： whf
 * @date： 2022-04-11 10:30
 */
@Getter
@Setter
public class AddressAreaVo implements Serializable {

    @ItemProperty(alias = "地址（省）")
    @Dictionary(id = "eh.base.dictionary.AddrArea")
    private String address1;
}
