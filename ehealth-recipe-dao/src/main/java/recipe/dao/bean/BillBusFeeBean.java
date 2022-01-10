package recipe.dao.bean;

import lombok.Data;

import java.util.Date;
/**
 * @author fuzi
 */
@Data
public class BillBusFeeBean {
    private Integer id;
    private String acctMonth;
    private String acctDate;
    private Integer organId;
    private Integer feeType;
    private String feeTypeName;
    private Integer payCount;
    private Double payAmount;
    private Integer refundCount;
    private Double refundAmount;
    private Double aggregateAmount;
    private Date createTime;
    private Date updateTime;

}
