package recipe.vo.doctor;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * @description： 前端药品库存出参
 * @author： whf
 * @date： 2022-04-27 22:52
 */
@Getter
@Setter
public class DrugStockVO extends DrugsResVo implements Serializable {
    private static final long serialVersionUID = 5507977508389477207L;
    /**
     * 通用名
     */
    private String drugName;
    /**
     * 库存数量
     */
    private int stockAmount;
    /**
     * 库存数量中文
     */
    private String stockAmountChin;

    /**
     * 是否有库存 true：有 ，F：无
     */
    private Boolean stock;

    /**
     * 药物使用总数量
     */
    private String useTotalDose;
    /**
     * 仅给前端展示使用
     */
    private String showUnit;
}
