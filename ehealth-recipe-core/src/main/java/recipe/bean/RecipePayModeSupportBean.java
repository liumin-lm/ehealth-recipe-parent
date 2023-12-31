package recipe.bean;

/**
 * company: ngarihealth
 * @author: 0184/yu_yun
 * date:2017/5/24.
 */
public class RecipePayModeSupportBean {

    private boolean supportOnlinePay = false;
    /**
     * 医保支付标志位
     */
    private boolean supportMedicalInsureance = false;
    /**
     * 货到付款标志位
     */
    private boolean supportCOD = false;
    /**
     * 药店取药标志位
     */
    private boolean supportTFDS = false;
    /**
     * 下载处方标志
     */
    private boolean supportDownload = false;
    /**
     * 到院取药标志
     */
    private boolean supportToHos = false;
    /**
     * 支持多种购药方式
     */
    private boolean supportComplex = false;

    public RecipePayModeSupportBean(){
        this.supportOnlinePay = false;
        this.supportMedicalInsureance = false;
        this.supportCOD = false;
        this.supportTFDS = false;
        this.supportDownload = false;
        this.supportToHos = false;
        this.supportComplex = false;
    }

    public boolean isSupportOnlinePay() {
        return supportOnlinePay;
    }

    public void setSupportOnlinePay(boolean supportOnlinePay) {
        this.supportOnlinePay = supportOnlinePay;
    }

    public boolean isSupportMedicalInsureance() {
        return supportMedicalInsureance;
    }

    public void setSupportMedicalInsureance(boolean supportMedicalInsureance) {
        this.supportMedicalInsureance = supportMedicalInsureance;
    }

    public boolean isSupportCOD() {
        return supportCOD;
    }

    public void setSupportCOD(boolean supportCOD) {
        this.supportCOD = supportCOD;
    }

    public boolean isSupportTFDS() {
        return supportTFDS;
    }

    public void setSupportTFDS(boolean supportTFDS) {
        this.supportTFDS = supportTFDS;
    }

    public boolean isSupportDownload() {
        return supportDownload;
    }

    public void setSupportDownload(boolean supportDownload) {
        this.supportDownload = supportDownload;
    }

    public boolean isSupportToHos() {
        return supportToHos;
    }

    public void setSupportToHos(boolean supportToHos) {
        this.supportToHos = supportToHos;
    }

    public boolean isSupportComplex() {
        return supportComplex;
    }

    public void setSupportComplex(boolean supportComplex) {
        this.supportComplex = supportComplex;
    }
}
