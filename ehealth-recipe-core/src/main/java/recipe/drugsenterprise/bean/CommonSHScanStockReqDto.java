package recipe.drugsenterprise.bean;
/**
 * @Description: 对接上海国药库存校验中间对象
 * @Author: JRK
 * @Date: 2020/02/20
 */

import ctd.schema.annotation.Schema;

import java.io.Serializable;
import java.math.BigDecimal;

@Schema
public class CommonSHScanStockReqDto implements Serializable {
    /**
     *当前页
     */
    private int pageIndex;
    /**
     *页大小
     */
    private int pageSize;
    /**
     *药品编号
     */
    private String Goods;

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

    public String getGoods() {
        return Goods;
    }

    public void setGoods(String goods) {
        Goods = goods;
    }
}
