package recipe.vo.second.enterpriseOrder;

import ctd.schema.annotation.ItemProperty;
import lombok.Getter;
import lombok.Setter;
import recipe.vo.base.BaseOrderVO;

import java.io.Serializable;

/**
 * @description： 第三方下载订单信息
 * @author： yinsheng
 * @date： 2021-12-08 15:50
 */
@Getter
@Setter
public class DownOrderVO extends BaseOrderVO implements Serializable {
    private static final long serialVersionUID = 2024121452428641435L;
}
