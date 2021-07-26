package com.ngari.recipe.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * @author yinsheng
 * @date 2021\7\23 0023 16:28
 */
@Setter
@Getter
@NoArgsConstructor
public class OutRecipeDetailDTO implements Serializable{
    private static final long serialVersionUID = -6924482088213725432L;
    /**
     * 具体数据
     */
    private String data;
    /**
     * 什么类型的data
     * h5
     * img
     * 例如 "h5"表示h5链接地址，"img"表示图片，"editor"表示富文本，"json"表示json字符串
     */
    private String type;
}
