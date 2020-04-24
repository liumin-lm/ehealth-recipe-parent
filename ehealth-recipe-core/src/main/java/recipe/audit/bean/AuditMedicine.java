package recipe.audit.bean;

/**
 * 藥品信息
 * Created by jiangtingfeng on 2017/11/2.
 * @author jiangtingfeng
 */
public class AuditMedicine {
    /**
     * 药品序号
     */
    private String ordinal;

    /**
     * 商品名
     */
    private String name;

    /**
     * 医院药品代码
     */
    private String hisCode;

    /**
     * 医保代码
     */
    private String medicareCode;

    /**
     * 批准文号
     */
    private String approvalNo;

    /**
     * 规格
     */
    private String spec;

    /**
     * 组号
     */
    private String group;

    /**
     * 用药理由
     */
    private String reason;

    /**
     * 单次量单位
     */
    private String unit;

    /**
     * 单次量
     */
    private String dose;

    /**
     * 频次代码
     */
    private String freq;

    /**
     * 给药途径代码
     */
    private String path;

    /**
     * 用药开始时间
     */
    private String begin;

    /**
     * 用药结束时间
     */
    private String end;

    /**
     * 服药天数
     */
    private String days;

    /**
     * 配液单号
     */
    private String pydNo;

    /**
     * 接管组号
     */
    private String linkGroup;

    /**
     * 需要预警分析
     */
    private String needAlert;

    /**
     * 发药单位
     */
    private String dispenseUnit;

    /**
     * 发药数量
     */
    private String dispenseAmount;

    public String getOrdinal() {
        return ordinal;
    }

    public void setOrdinal(String ordinal) {
        this.ordinal = ordinal;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getHisCode() {
        return hisCode;
    }

    public void setHisCode(String hisCode) {
        this.hisCode = hisCode;
    }

    public String getMedicareCode() {
        return medicareCode;
    }

    public void setMedicareCode(String medicareCode) {
        this.medicareCode = medicareCode;
    }

    public String getApprovalNo() {
        return approvalNo;
    }

    public void setApprovalNo(String approvalNo) {
        this.approvalNo = approvalNo;
    }

    public String getSpec() {
        return spec;
    }

    public void setSpec(String spec) {
        this.spec = spec;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getDose() {
        return dose;
    }

    public void setDose(String dose) {
        this.dose = dose;
    }

    public String getFreq() {
        return freq;
    }

    public void setFreq(String freq) {
        this.freq = freq;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getBegin() {
        return begin;
    }

    public void setBegin(String begin) {
        this.begin = begin;
    }

    public String getEnd() {
        return end;
    }

    public void setEnd(String end) {
        this.end = end;
    }

    public String getDays() {
        return days;
    }

    public void setDays(String days) {
        this.days = days;
    }

    public String getPydNo() {
        return pydNo;
    }

    public void setPydNo(String pydNo) {
        this.pydNo = pydNo;
    }

    public String getLinkGroup() {
        return linkGroup;
    }

    public void setLinkGroup(String linkGroup) {
        this.linkGroup = linkGroup;
    }

    public String getNeedAlert() {
        return needAlert;
    }

    public void setNeedAlert(String needAlert) {
        this.needAlert = needAlert;
    }

    public String getDispenseUnit() {
        return dispenseUnit;
    }

    public void setDispenseUnit(String dispenseUnit) {
        this.dispenseUnit = dispenseUnit;
    }

    public String getDispenseAmount() {
        return dispenseAmount;
    }

    public void setDispenseAmount(String dispenseAmount) {
        this.dispenseAmount = dispenseAmount;
    }
}
