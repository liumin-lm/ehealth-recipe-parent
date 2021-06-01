package recipe.drugsenterprise.bean;
/**
 * @Description: 对接上海国药配送信息查询中间对象
 * @Author: JRK
 * @Date: 2020/02/20
 */

import ctd.schema.annotation.Schema;

import java.io.Serializable;
import java.math.BigDecimal;

@Schema
public class CommonSHDistributionReqDto implements Serializable {
    /**
     *当前页
     */
    private int pageIndex;
    /**
     *页大小
     */
    private int pageSize;
    /**
     *处方号
     */
    private String billno;
    /**
     *开始时间
     */
    private String strdate;
    /**
     *结束时间
     */
    private String enddate;

    public int getPageIndex() {
        return pageIndex;
    }

    public void setPageIndex(int pageIndex) {
        this.pageIndex = pageIndex;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public String getBillno() {
        return billno;
    }

    public void setBillno(String billno) {
        this.billno = billno;
    }

    public String getStrdate() {
        return strdate;
    }

    public void setStrdate(String strdate) {
        this.strdate = strdate;
    }

    public String getEnddate() {
        return enddate;
    }

    public void setEnddate(String enddate) {
        this.enddate = enddate;
    }
}
