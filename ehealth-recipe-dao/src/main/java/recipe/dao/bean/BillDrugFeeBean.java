package recipe.dao.bean;

import lombok.Data;

import java.util.Date;
/**
 * @author fuzi
 */
@Data
public class BillDrugFeeBean {
    private Integer id;
    private String acctMonth;
    private String acctDate;
    private Integer organId;
    private Integer drugType;
    private String drugTypeName;
    private Integer drugCompany;
    private String drugCompanyName;
    private Double amount;
    private Date createTime;
    private Date updateTime;
}
