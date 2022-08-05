package com.ngari.recipe.drug.service;

import com.ngari.recipe.IBaseService;
import com.ngari.recipe.drug.model.DecoctionWayBean;
import com.ngari.recipe.drug.model.DrugMakingMethodBean;
import ctd.persistence.bean.QueryResult;
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
     * 获取机构下指定制法
     *
     * @param organId 机构编码
     * @return List<DecoctionWayBean> 药品信息
     */
    @RpcService
    QueryResult<DrugMakingMethodBean> findDrugMakingMethodByOrganIdAndName(Integer organId, String methodText, Integer start, Integer limit);

    /**
     * 药品制法法存储
     *
     * @param drugMakingMethodBean 制法信息
     * @return
     */
    @RpcService
    Integer saveDrugMakingMethod(DrugMakingMethodBean drugMakingMethodBean);

    /**
     * 药品制法更新
     *
     * @param drugMakingMethodBean 制法
     * @return
     */
    @RpcService
    Integer updateDrugMakingMethod(DrugMakingMethodBean drugMakingMethodBean);

    /**
     * 药品制法删除
     *
     * @param methodId 制法ID
     * @return
     */
    @RpcService
    void deleteDrugMakingMethodByMethodId(Integer methodId);




    /**
     * 获取机构下所有药品煎法
     *
     * @param organId 机构编码
     * @return List<DecoctionWayBean> 药品信息
     */
    @RpcService
    List<DecoctionWayBean> findAllDecoctionWayByOrganId(Integer organId);

    /**
     * 获取机构下指定煎法
     *
     * @param organId 机构编码
     * @return List<DecoctionWayBean> 药品信息
     */
    @RpcService
    QueryResult<DecoctionWayBean> findDecoctionWayByOrganIdAndName(Integer organId, String decoctionText, Integer start, Integer limit);

    /**
     * 药品制法存储
     *
     * @param decoctionWayBean 煎法信息
     * @return
     */
    @RpcService
    public Integer saveDrugDecoctionWay(DecoctionWayBean decoctionWayBean);


    /**
     * 药品制法更新
     *
     * @param decoctionWayBean 煎法信息
     * @return
     */
    @RpcService
    public Integer updateDrugDecoctionWay(DecoctionWayBean decoctionWayBean);

    /**
     * 药品制法删除
     *
     * @param decoctionId 煎法ID
     * @return
     */
    @RpcService
    public void deleteDrugDecoctionWay(Integer decoctionId);

    /**
     * 机构字典查询数量
     * @param organId
     * @return
     */
    @RpcService
    Integer getCountOfOrgan(Integer organId,Integer type);
}
