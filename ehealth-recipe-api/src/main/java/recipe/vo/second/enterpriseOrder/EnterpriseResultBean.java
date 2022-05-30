package recipe.vo.second.enterpriseOrder;

import java.io.Serializable;

/**
 * 返回药企结果数据
 */
public class EnterpriseResultBean implements Serializable {
    private static final long serialVersionUID = -6384799813576850026L;

    public static final Integer SUCCESS = 200;
    public static final Integer FAIL = 0;

    private Integer code;
    private String msg;
    private Object object;

    public EnterpriseResultBean() {
    }

    public EnterpriseResultBean(Integer code) {
        this.code = code;
    }

    public EnterpriseResultBean(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public static EnterpriseResultBean getSuccess() {
        return new EnterpriseResultBean(SUCCESS);
    }

    public static EnterpriseResultBean getFail() {
        return new EnterpriseResultBean(FAIL);
    }

    public static EnterpriseResultBean getFail(String msg) {
        return new EnterpriseResultBean(FAIL, msg);
    }

    public static EnterpriseResultBean getSuccess(String msg) {
        return new EnterpriseResultBean(SUCCESS, msg);
    }

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

    public Object getObject() {
        return object;
    }

    public void setObject(Object object) {
        this.object = object;
    }
}
