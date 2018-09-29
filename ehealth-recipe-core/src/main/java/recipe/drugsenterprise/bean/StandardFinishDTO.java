package recipe.drugsenterprise.bean;

import com.ngari.recipe.common.anno.Verify;

import java.io.Serializable;

/**
 * @author： 0184/yu_yun
 * @date： 2018/9/28
 * @description： 药企完成标准数据
 * @version： 1.0
 */
public class StandardFinishDTO implements Serializable {

    private static final long serialVersionUID = -5878477949359289507L;

    public static final String SUCCESS = "0";

    public static final String FAIL = "1";

    @Verify(desc = "0:成功，1:失败")
    private String code;

    @Verify(isNotNull = false, desc = "其他信息")
    private String msg;

    @Verify(desc = "组织机构编码")
    private String organId;

    @Verify(desc = "电子处方单号")
    private String recipeCode;

    @Verify(desc = "时间", isDate = true)
    private String date;

    @Verify(isNotNull = false, desc = "完成配送姓名或取药人姓名", maxLength = 30)
    private String sender;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getOrganId() {
        return organId;
    }

    public void setOrganId(String organId) {
        this.organId = organId;
    }

    public String getRecipeCode() {
        return recipeCode;
    }

    public void setRecipeCode(String recipeCode) {
        this.recipeCode = recipeCode;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }
}
