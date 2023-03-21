package com.ngari.recipe.offlinetoonline.model;


import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @Author liumin
 * @Date 2023/3/21 上午11:42
 * @Description 线下转线上
 */
@Data
public class BatchOfflineToOnlineReqVO implements Serializable {
    private static final long serialVersionUID = -7727248592234567484L;

    List<OfflineToOnlineReqVO> offlineToOnlineReqVOS;


}


