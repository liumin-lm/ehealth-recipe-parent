package com.ngari.recipe.drugsenterprise.service;

import com.ngari.recipe.IBaseService;
import com.ngari.recipe.drugsenterprise.model.EnterpriseAddressDTO;
import ctd.util.annotation.RpcService;

import java.util.List;

/**
 * @author： 0184/yu_yun
 * @date： 2018/9/7
 * @description： TODO
 * @version： 1.0
 */
public interface IEnterpriseAddressService extends IBaseService<EnterpriseAddressDTO> {

    @RpcService
    EnterpriseAddressDTO addEnterpriseAddress(EnterpriseAddressDTO enterpriseAddress);

    @RpcService
    List<EnterpriseAddressDTO> findByEnterPriseId(Integer enterpriseId);

    @RpcService
    void deleteEnterpriseAddressById(List<Integer> ids);

    @RpcService
    void addEnterpriseAddressList(List<EnterpriseAddressDTO> enterpriseAddressDTOList);
}
