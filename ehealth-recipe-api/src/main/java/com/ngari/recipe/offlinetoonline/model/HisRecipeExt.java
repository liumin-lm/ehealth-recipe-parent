package com.ngari.recipe.offlinetoonline.model;

import ctd.schema.annotation.ItemProperty;
import lombok.Data;

/**
 * @Author liumin
 * @Date 2021/7/20 下午4:55
 * @Description
 */
@Data
public class HisRecipeExt {
    @ItemProperty(alias = "处方扩展序号")
    private Integer hisRecipeExtID; // int(11) NOT NULL AUTO_INCREMENT,
    @ItemProperty(alias = "his处方序号")
    private Integer hisRecipeId; // int(11) NOT NULL COMMENT 'his处方序号',
    @ItemProperty(alias = "文本描述")
    private String  extendText; // varchar(50) DEFAULT NULL COMMENT '文本描述',
    @ItemProperty(alias = "文本值")
    private String  extendValue; // varchar(100) DEFAULT NULL COMMENT '文本值',
    @ItemProperty(alias = "文本值类型")
    private Integer valueType; // tinyint(1) DEFAULT NULL COMMENT '1 数值 2 链接 3 文本 4 图片',
    @ItemProperty(alias = "排序值")
    private Integer sort; // tinyint(1) DEFAULT NULL COMMENT '排序值',
}
