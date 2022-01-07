package recipe.dao.bean;

import lombok.Data;

import java.util.Date;

/**
 * @author fuzi
 */
@Data
public class RecipeBillBean {
    private String acctDate;
    private Date startTime;
    private Date endTime;
    private int start = 0;
    private int pageSize = 100;
}
