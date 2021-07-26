package com.ngari.recipe.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

/**
 * @author yinsheng
 * @date 2021\7\20 0020 16:49
 */
@Setter
@Getter
@NoArgsConstructor
public class DiseaseInfoDTO implements Serializable{
    private static final long serialVersionUID = -6504440504778268155L;

    //诊断编号
    private String diseaseId;
    //诊断代码 必填
    private String diseaseCode;
    //诊断名称 必填
    private String diseaseName;
    //诊断说明
    private String diseaseMemo;
}
