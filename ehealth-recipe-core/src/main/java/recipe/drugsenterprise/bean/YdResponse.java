package recipe.drugsenterprise.bean;

import java.io.Serializable;

/**
 * @author yinsheng
 * @date 2019\12\17 0017 13:40
 */
public class YdResponse implements Serializable{
    private static final long serialVersionUID = -7407368203766329090L;

    private String data;
    private String success;
    private String message;

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getSuccess() {
        return success;
    }

    public void setSuccess(String success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
