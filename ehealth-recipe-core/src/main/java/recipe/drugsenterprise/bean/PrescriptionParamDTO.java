package recipe.drugsenterprise.bean;

import com.ngari.recipe.common.anno.Verify;
import ctd.schema.annotation.Schema;

import java.io.Serializable;
import java.util.Date;

/**
 * @author yinsheng
 * @date 2019\2\28 0028 15:01
 */
@Schema
public class PrescriptionParamDTO implements Serializable {

    private static final long serialVersionUID = -1417570246875978147L;

    @Verify(isNotNull = true, desc = "处方id", maxLength = 20)
    private String rxId;

    @Verify(isNotNull = true, desc = "处方状态")
    private Integer status;

    @Verify(isNotNull = false, desc = "扩展字段", maxLength = 1024)
    private String attribute;

    @Verify(isNotNull = true, desc = "taobao账户id")
    private Long userId;

    @Verify(isNotNull = true, desc = "osskey处方图片", maxLength = 128)
    private String ossKey;

    @Verify(isNotNull = true, desc = "外部医院编码id", maxLength = 64)
    private String outHospitalId;

    @Verify(isNotNull = true, desc = "开方时间", isDate = true)
    private Date rxCreateTime;

    public String getRxId() {
        return rxId;
    }

    public void setRxId(String rxId) {
        this.rxId = rxId;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getAttribute() {
        return attribute;
    }

    public void setAttribute(String attribute) {
        this.attribute = attribute;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getOssKey() {
        return ossKey;
    }

    public void setOssKey(String ossKey) {
        this.ossKey = ossKey;
    }

    public String getOutHospitalId() {
        return outHospitalId;
    }

    public void setOutHospitalId(String outHospitalId) {
        this.outHospitalId = outHospitalId;
    }

    public Date getRxCreateTime() {
        return rxCreateTime;
    }

    public void setRxCreateTime(Date rxCreateTime) {
        this.rxCreateTime = rxCreateTime;
    }
}
