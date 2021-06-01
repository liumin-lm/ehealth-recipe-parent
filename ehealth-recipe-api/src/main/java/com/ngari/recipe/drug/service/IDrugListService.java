package com.ngari.recipe.drug.service;

import com.ngari.recipe.drug.model.DrugListBean;
import com.ngari.recipe.drug.model.OrganDrugListBean;
import ctd.dictionary.DictionaryItem;
import ctd.util.annotation.RpcService;

import java.util.List;

public interface IDrugListService {
    @RpcService
    DrugListBean get(final int drugId);

    @RpcService
    List<DictionaryItem> getDrugClass(String parentKey, int sliceType);

    @RpcService
    DrugListBean getDrugListBeanByDrugId(Integer drugId);

    @RpcService
    OrganDrugListBean getOrganDrugListByOrganDrugId(Integer organDrugId);

    @RpcService
    void saveDrugList(DrugListBean drugListBean);

    /**
     * 查询药品信息
     * @param drugIds
     * @return
     */
    @RpcService
    List<DrugListBean> findByDrugIds(List<Integer> drugIds);
}
