package com.ngari.recipe.drug.service;

import com.ngari.platform.recipe.mode.HisDrugListBean;
import com.ngari.platform.recipe.mode.HisOrganDrugListBean;
import com.ngari.recipe.IBaseService;
import com.ngari.recipe.common.RecipeBussReqTO;
import com.ngari.recipe.common.RecipeListResTO;
import com.ngari.recipe.drug.model.DispensatoryDTO;
import com.ngari.recipe.drug.model.DrugListBean;
import ctd.persistence.bean.QueryResult;
import ctd.util.annotation.RpcService;

import java.util.Date;
import java.util.List;

/**
 * @company: ngarihealth
 * @author: 0184/yu_yun
 * @date:2017/8/1.
 */
public interface IDrugService extends IBaseService<DrugListBean> {


    /**
     * 获取某一药企下所有可配送药品
     *
     * @param depId 药企ID
     * @return RecipeListResTO<List<DrugListBean>> 药品信息
     */
    @RpcService
    RecipeListResTO<DrugListBean> findDrugsByDepId(int depId);

    /**
     * 统计 base_organdruglist 某一机构下的药品数量
     *
     * @param organId 机构ID
     * @return long 数量
     */
    @RpcService
    long countAllDrugsNumByOrganId(int organId);

    /**
     * 获取某一机构某一类药品可开具的数量
     *
     * @param request 查询条件
     *                organId:查询机构ID
     *                drugType:药品种类，参照 drugType.dic
     * @return long 数量
     */
    @RpcService
    long countDrugsNumByOrganId(RecipeBussReqTO request);

    /**
     * 更新 base_organdruglist organId的值
     *
     * @param newOrganId 新值
     * @param oldOrganId 旧值
     */
    @RpcService
    void changeDrugOrganId(int newOrganId, int oldOrganId);

    /**
     * 供organDAO-queryOrganCanRecipe使用 查询能开某个药品的机构
     * @param organIds
     * @param drugId
     * @return List 机构ID
     */
    @RpcService
    List<Integer> queryOrganCanRecipe(List<Integer> organIds,Integer drugId);

    /**
     * 获取重点药品关联的医生ID
     * @param drugId 药品ID
     * @return List 医生ID
     */
    @RpcService
    List<Integer> findPriorityDoctorList(Integer drugId);

    /**
     * 添加药品
     * @param d
     * @return
     */
    @RpcService
    DrugListBean addDrugList(DrugListBean d);

    /**
     * 更新药品
     * @param drugList
     * @return
     */
    @RpcService
    public DrugListBean updateDrugList(DrugListBean drugList);

    /**
     * 运营平台-查询药品
     * @param drugClass
     * @param keyword
     * @param status
     * @param start
     * @param limit
     * @return
     */
    @RpcService(timeout = 600)
    QueryResult<DrugListBean> queryDrugListsByDrugNameAndStartAndLimit(String drugClass, String keyword,
                                                                       Integer status, Integer drugSourcesId,Integer type,Integer isStandardDrug, int start, int limit);
    @RpcService
    DispensatoryDTO getByDrugId(Integer drugId);

    @RpcService
    List<HisDrugListBean> findDrugList(Integer start, Integer limit);

    @RpcService
    Boolean saveCompareDrugListData(List<HisOrganDrugListBean> listBeen);

    @RpcService
    DrugListBean deleteDrugList(Integer drugId);
}
