package com.ngari.recipe.ca;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * pdf 返回值
 *
 * @author fuzi
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
public class PdfSignResultDTO {
    /**
     * 图片
     */
    private String imgFileId;
    /**
     * 1：E签宝失败， 0：E签宝成功， 2：高州CA ，100：标准CA
     */
    private Integer code;
    private String fileId;
}
