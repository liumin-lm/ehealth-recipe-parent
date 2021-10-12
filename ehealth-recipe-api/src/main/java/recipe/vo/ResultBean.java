package recipe.vo;

import com.ngari.recipe.vo.CodeEnum;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * @author fuzi
 */
@Setter
@Getter
public class ResultBean<T> {

    @Deprecated
    private Integer code;

    private boolean bool;

    private String msg;

    private List<String> msgList;

    private T data;

    public ResultBean() {
    }

    public ResultBean(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public static ResultBean succeed() {
        return new ResultBean(CodeEnum.SERVICE_SUCCEED.getCode(), CodeEnum.SERVICE_SUCCEED.getName());
    }

    public static ResultBean serviceError(String name, Object data) {
        ResultBean resultBean = serviceError(name);
        resultBean.setData(data);
        return resultBean;
    }

    public static ResultBean serviceError(String name) {
        if (StringUtils.isEmpty(name)) {
            return new ResultBean(CodeEnum.SERVICE_ERROR.getCode(), CodeEnum.SERVICE_ERROR.getName());
        }
        return new ResultBean(CodeEnum.SERVICE_ERROR.getCode(), name);
    }
}
