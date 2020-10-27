package com.ngari.recipe.vo;

import lombok.Getter;
import lombok.Setter;

/**
 * @author fuzi
 */
@Setter
@Getter
public class ResultBean<T> {

    private Integer code;

    private String msg;

    private T data;

    public ResultBean(Integer code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }
}
