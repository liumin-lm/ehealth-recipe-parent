package recipe.drugsenterprise.bean;

import java.io.Serializable;
import java.util.List;

/**
* @Description: 华东药店推送处方响应对象
* @Author: JRK
* @Date: 2019/7/24
*/
public class HdPushRecipeResponse implements Serializable {
    private static final long serialVersionUID = -4657984898446309843L;
    /**
     * 访问是否成功
     */
    private Boolean success;
    /**
     * 响应状态
     */
    private String code;
    /**
     * 响应信息
     */
    private String message;
    /**
     * 响应信息结果
     */
    private Object data;

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }
}