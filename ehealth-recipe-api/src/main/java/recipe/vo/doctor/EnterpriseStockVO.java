package recipe.vo.doctor;

import lombok.Data;

import java.io.Serializable;

@Data
public class EnterpriseStockVO implements Serializable {

    private static final long serialVersionUID = 449666120787641736L;

    private Integer drugId;
    /**
     * 配送药企代码
     */
    private String deliveryCode;
    /**
     * 配送药企名称
     */
    private String deliveryName;

    /**
     *  0默认，1查询医院，2查询药企
     */
    private Integer checkStockFlag;
    /**
     * 是否有库存 true：有 ，F：无
     */
    private Boolean stock;
    /**
     * 库存数量中文
     */
    private String stockAmountChin;
}
