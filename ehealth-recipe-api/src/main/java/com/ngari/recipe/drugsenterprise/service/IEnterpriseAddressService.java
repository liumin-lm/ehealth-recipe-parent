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

    @RpcService(timeout = 60)
    void addEnterpriseAddressList(List<EnterpriseAddressDTO> enterpriseAddressDTOList, Integer enterpriseId);

    @RpcService
    public int allAddressCanSendForOrder(Integer depId, String address1, String address2, String address3) ;
}
