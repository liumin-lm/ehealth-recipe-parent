package recipe.bean;

import java.io.Serializable;
import java.util.List;

/**
 * 对接医院HIS结果对象
 * company: ngarihealth
 * @author: 0184/yu_yun
 * date:2017/4/18.
 */
public class HosRecipeResult implements Serializable{

    public static final String SUCCESS="0";

    public static final String FAIL="-1";

    private static final long serialVersionUID = 4939853811564294840L;

    /**
     * 交易成功标志 0交易成功 -1交易失败
     */
    private String msgCode;

    /**
     * 返回信息 如果交易异常返回异常消息
     */
    private String msg;

    private List<? extends Object> data;

    public HosRecipeResult() {
    }

    public HosRecipeResult(String msgCode) {
        this.msgCode = msgCode;
    }

    public static HosRecipeResult getSuccess(){
        return new HosRecipeResult(SUCCESS);
    }

    public static HosRecipeResult getFail(){
        return new HosRecipeResult(FAIL);
    }

    public String getMsgCode() {
        return msgCode;
    }

    public void setMsgCode(String msgCode) {
        this.msgCode = msgCode;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public List<? extends Object> getData() {
        return data;
    }

    public void setData(List<? extends Object> data) {
        this.data = data;
    }
}
