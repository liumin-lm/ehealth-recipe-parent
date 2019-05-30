package com.ngari.recipe.drug.service;

import com.ngari.recipe.drug.model.DrugListBean;
import ctd.dictionary.DictionaryItem;
import ctd.util.annotation.RpcService;

import java.util.List;

public interface IDrugListService {
    @RpcService
    DrugListBean get(final int drugId);

    @RpcService
    List<DictionaryItem> getDrugClass(String parentKey, int sliceType);

    @RpcService
    void saveDrugList(DrugListBean drugListBean);
}
