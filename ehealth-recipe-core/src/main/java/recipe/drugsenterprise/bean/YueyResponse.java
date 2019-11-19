package recipe.drugsenterprise.bean;

import java.io.Serializable;
import java.util.List;

/**
 * @author yinsheng
 * @date 2019\11\19 0019 18:46
 */
public class YueyResponse implements Serializable{
    private static final long serialVersionUID = 4585618365125407103L;

    private String code;
    private List<StoreInventoryResponse> data;
    private String err;
    private String intfcode;
    private String msg;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public List<StoreInventoryResponse> getData() {
        return data;
    }

    public void setData(List<StoreInventoryResponse> data) {
        this.data = data;
    }

    public String getErr() {
        return err;
    }

    public void setErr(String err) {
        this.err = err;
    }

    public String getIntfcode() {
        return intfcode;
    }

    public void setIntfcode(String intfcode) {
        this.intfcode = intfcode;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }
}
