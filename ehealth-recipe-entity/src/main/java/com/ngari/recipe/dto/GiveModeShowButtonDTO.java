package com.ngari.recipe.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author yinsheng
 * @date 2020\12\3 0003 13:49
 */
@Data
public class GiveModeShowButtonDTO implements Serializable{
    private static final long serialVersionUID = 4475425739208562010L;

    //按钮是否可点击
    private Boolean optional;
    //按钮的展示形势(互联网+平台)
    private Integer buttonType;
    //详情页的按钮总开关
    private Boolean showButton;
    //列表选项
    private GiveModeButtonDTO listItem;
    //购药按钮
    private List<GiveModeButtonDTO> giveModeButtons;

}
