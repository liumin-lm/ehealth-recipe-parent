package com.ngari.recipe.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * @author yinsheng
 * @date 2020\12\9 0009 19:21
 */
@Data
public class SkipThirdBean implements Serializable{
    private static final long serialVersionUID = 3897701688143179541L;

    private String url;
    private String appKey;
}
