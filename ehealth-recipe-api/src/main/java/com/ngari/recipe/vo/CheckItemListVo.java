package com.ngari.recipe.vo;

import ctd.schema.annotation.ItemProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

/**
 * 诊疗项目出参
 *
 * @author 刘敏
 * @date 2021/11/12 0021 09:24
 */
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class CheckItemListVo implements Serializable {
    private static final long serialVersionUID = 8715047453386510666L;

    @ItemProperty(alias = "原因")
    private List<String> cause;
    @ItemProperty(alias = "结果")
    private boolean result = false;
}
