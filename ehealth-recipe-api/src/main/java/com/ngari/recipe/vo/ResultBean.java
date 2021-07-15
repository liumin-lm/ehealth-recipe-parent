package com.ngari.recipe.vo;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

/**
 * 废弃 与原有架构不符
 *
 * @author fuzi
 */
@Setter
@Getter
@Deprecated
public class ResultBean {

    private Integer code;

    private String msg;

    private Object data;

    public ResultBean() {
    }


    public ResultBean(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public ResultBean(Integer code, String msg, Object data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public static ResultBean getResult(CodeEnum codeEnum) {
        return new ResultBean(codeEnum.getCode(), codeEnum.getName());
    }

    public static ResultBean getResult(CodeEnum codeEnum, Object data) {
        return new ResultBean(codeEnum.getCode(), codeEnum.getName(), data);
    }

    public static ResultBean succeed(Object data) {
        return new ResultBean(CodeEnum.SERVICE_SUCCEED.getCode(), CodeEnum.SERVICE_SUCCEED.getName(), data);
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
