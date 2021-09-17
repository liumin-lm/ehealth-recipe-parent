package com.ngari.recipe.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 获取跳转第三方链接和appKey
 *
 * @author yinsheng
 * @date 2020\12\9 0009 19:21
 */
@Data
public class SkipThirdDTO implements Serializable {
    private static final long serialVersionUID = 3897701688143179541L;

    private String url;
    private String appKey;

    private Integer code;
    private String msg;
}
