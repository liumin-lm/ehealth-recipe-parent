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

    /**
     * 1 查询url ，2推送处方返回url
     */
    private Integer type;
    /**
     * 0 失败 1成功
     */
    private Integer code;
    private String msg;

    private String prescId;
}
