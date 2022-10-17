package com.ngari.recipe.dto;

import ctd.schema.annotation.Desensitizations;
import ctd.schema.annotation.DesensitizationsType;
import ctd.schema.annotation.ItemProperty;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 药品同步字段
 *
 * @author liumin
 */
@Getter
@Setter
public class DrugSyncFieldDTO extends AttachSealPicDTO implements Serializable {

    @ItemProperty(alias = "字段名称")
    private String fieldName;

    @ItemProperty(alias = "字段编码")
    private String fieldCode;

    @ItemProperty(alias = "add是否同步 同步勾选 0否 1是")
    private String addIsSync;

    @ItemProperty(alias = "add是否允许编辑 0不允许 1允许")
    private String addIsAllowEdit;

    @ItemProperty(alias = "update是否同步 同步勾选 0否 1是")
    private String updateIsSync;

    @ItemProperty(alias = "update是否允许编辑 0不允许 1允许")
    private String updateIsAllowEdit;

    public DrugSyncFieldDTO(String fieldName, String fieldCode, String addIsSync, String addIsAllowEdit, String updateIsSync, String updateIsAllowEdit) {
        this.fieldName = fieldName;
        this.fieldCode = fieldCode;
        this.addIsSync = addIsSync;
        this.addIsAllowEdit = addIsAllowEdit;
        this.updateIsSync = updateIsSync;
        this.updateIsAllowEdit = updateIsAllowEdit;
    }
}
