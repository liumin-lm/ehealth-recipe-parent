package com.ngari.recipe.drugdistributionprice.service;

import com.ngari.recipe.drugdistributionprice.model.DrugDistributionPriceBean;
import ctd.util.annotation.RpcService;

import java.util.List;

public interface IDrugDistributionPriceService {

    @RpcService
    public DrugDistributionPriceBean saveOrUpdatePrice(DrugDistributionPriceBean price);

    @RpcService
    public void deleteByEnterpriseId(Integer enterpriseId);

    @RpcService
    public void deleteById(Integer id);

    @RpcService
    public List<DrugDistributionPriceBean> findByEnterpriseId(Integer enterpriseId);

    @RpcService
    DrugDistributionPriceBean getByEnterpriseIdAndAddrArea(Integer enterpriseId, String addrArea);

    @RpcService
    void savePriceList(List<DrugDistributionPriceBean> priceList);

    @RpcService
    DrugDistributionPriceBean getDistributionPriceByEnterpriseIdAndAddrArea(Integer enterpriseId, String addrArea);

}
