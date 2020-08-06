package com.ngari.recipe.drug.service;

import com.ngari.recipe.IBaseService;
import com.ngari.recipe.drug.model.DecoctionWayBean;
import com.ngari.recipe.drug.model.DrugMakingMethodBean;
import ctd.util.annotation.RpcService;

import java.util.List;

/**
 * @company: ngarihealth
 * @author: gaomw
 * @date:2020/8/5.
 */
public interface IDrugExtService extends IBaseService {


    /**
     * 获取机构下所有药品制法
     *
     * @param organId 机构编码
     * @return List<DrugMakingMethod> 药品信息
     */
    @RpcService
    List<DrugMakingMethodBean> findAllDrugMakingMethodByOrganId(Integer organId);

    /**
     * 药品制法法存储
     *
     * @param drugMakingMethodBean 制法信息
     * @return
     */
    @RpcService
    Integer saveDrugMakingMethod(DrugMakingMethodBean drugMakingMethodBean);
    /**
     * 获取机构下所有药品煎法
     *
     * @param organId 机构编码
     * @return List<DecoctionWayBean> 药品信息
     */
    @RpcService
    List<DecoctionWayBean> findAllDecoctionWayByOrganId(Integer organId);

    /**
     * 药品制法存储
     *
     * @param decoctionWayBean 煎法信息
     * @return
     */
    @RpcService
    public Integer saveDrugDecoctionWay(DecoctionWayBean decoctionWayBean);
}
