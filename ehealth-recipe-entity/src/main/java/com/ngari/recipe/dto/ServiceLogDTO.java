package com.ngari.recipe.dto;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 日志记录对象
 * @author fuzi
 */
@Setter
@Getter
public class ServiceLogDTO implements Serializable {
    private static final long serialVersionUID = 8286144528594112550L;
    /**
     * 业务id
     */
    private Integer id;
    /**
     * id类型 1机构，2药企
     */
    private Integer type;
    /**
     * 类别代表不同操作
     */
    private String category;
    /**
     * 执行时间
     */
    private Long time;
    /**
     * 执行数量
     */
    private Integer size;
    /**
     * 项目名称 如处方：recipe
     */
    private String source;

    /**
     * 业务类型
     */
    private String name;

}
