package recipe.ca.vo;

public class CaSignResultVo {

    /**
     * 签章后的pdfbase64文件
     */
    private String pdfBase64;
    /**
     * 签名的时间戳
     */
    private String signCADate;
    /**
     * 电子签名值
     */
    private String signRecipeCode;

    private String fileId;

    private Integer code;

    private String msg;

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getPdfBase64() {
        return pdfBase64;
    }

    public void setPdfBase64(String pdfBase64) {
        this.pdfBase64 = pdfBase64;
    }

    public String getSignCADate() {
        return signCADate;
    }

    public void setSignCADate(String signCADate) {
        this.signCADate = signCADate;
    }

    public String getSignRecipeCode() {
        return signRecipeCode;
    }

    public void setSignRecipeCode(String signRecipeCode) {
        this.signRecipeCode = signRecipeCode;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }
}
