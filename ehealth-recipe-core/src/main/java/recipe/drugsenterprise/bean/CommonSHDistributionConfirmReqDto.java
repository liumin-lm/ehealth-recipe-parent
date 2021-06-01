package recipe.drugsenterprise.bean;
/**
 * @Description: 对接上海国药配送信息下载确认中间对象
 * @Author: JRK
 * @Date: 2020/02/20
 */

import ctd.schema.annotation.Schema;

import java.io.Serializable;

@Schema
public class CommonSHDistributionConfirmReqDto implements Serializable {
    /**
     *PkId
     */
    private String ITEMID;
    /**
     *批处理号
     */
    private String TransId;

    public String getITEMID() {
        return ITEMID;
    }

    public void setITEMID(String ITEMID) {
        this.ITEMID = ITEMID;
    }

    public String getTransId() {
        return TransId;
    }

    public void setTransId(String transId) {
        TransId = transId;
    }
}
