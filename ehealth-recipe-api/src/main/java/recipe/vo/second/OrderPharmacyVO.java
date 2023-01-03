package recipe.vo.second;

import ctd.schema.annotation.ItemProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

/**
 * @description： 订单相关药房信息
 * @author： whf
 * @date： 2023-01-03 9:25
 */
@Getter
@Setter
@ToString
public class OrderPharmacyVO implements Serializable {
    private static final long serialVersionUID = 5114631506635357382L;

    @ItemProperty(alias = "药房id主键")
    private Integer pharmacyId;
    @ItemProperty(alias = "药房名称")
    private String pharmacyName;
}
