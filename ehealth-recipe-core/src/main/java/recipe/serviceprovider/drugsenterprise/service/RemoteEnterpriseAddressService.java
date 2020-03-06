package recipe.serviceprovider.drugsenterprise.service;

import com.ngari.recipe.drugsenterprise.model.EnterpriseAddressDTO;
import com.ngari.recipe.drugsenterprise.service.IEnterpriseAddressService;
import com.ngari.recipe.entity.EnterpriseAddress;
import ctd.persistence.DAOFactory;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import eh.utils.ValidateUtil;
import recipe.dao.EnterpriseAddressDAO;
import recipe.serviceprovider.BaseService;

import java.util.List;

/**
 * @author： 0184/yu_yun
 * @date： 2018/9/7
 * @description： TODO
 * @version： 1.0
 */
@RpcBean("remoteEnterpriseAddressService")
public class RemoteEnterpriseAddressService extends BaseService<EnterpriseAddressDTO> implements IEnterpriseAddressService {

    @RpcService
    @Override
    public EnterpriseAddressDTO get(Object id) {
        EnterpriseAddressDAO addressDAO = DAOFactory.getDAO(EnterpriseAddressDAO.class);
        EnterpriseAddress address = addressDAO.get(id);
        return getBean(address, EnterpriseAddressDTO.class);
    }

    @RpcService
    @Override
    public EnterpriseAddressDTO addEnterpriseAddress(EnterpriseAddressDTO edDto) {
        EnterpriseAddressDAO addressDAO = DAOFactory.getDAO(EnterpriseAddressDAO.class);
        EnterpriseAddress enterpriseAddress = getBean(edDto, EnterpriseAddress.class);
        EnterpriseAddress address = addressDAO.addEnterpriseAddress(enterpriseAddress);
        return getBean(address, EnterpriseAddressDTO.class);
    }

    @RpcService
    @Override
    public void addEnterpriseAddressList(List<EnterpriseAddressDTO> enterpriseAddressDTOList) {
        EnterpriseAddressDAO addressDAO = DAOFactory.getDAO(EnterpriseAddressDAO.class);
        if(ValidateUtil.notBlankList(enterpriseAddressDTOList)) {
            addressDAO.deleteEnterpriseAddress(enterpriseAddressDTOList.get(0).getEnterpriseId());
            for (EnterpriseAddressDTO enterpriseAddressDTO : enterpriseAddressDTOList) {
                EnterpriseAddress enterpriseAddress = getBean(enterpriseAddressDTO, EnterpriseAddress.class);
                EnterpriseAddress address = addressDAO.addEnterpriseAddress(enterpriseAddress);
            }
        }
    }

    @RpcService
    @Override
    public List<EnterpriseAddressDTO> findByEnterPriseId(Integer enterpriseId) {
        EnterpriseAddressDAO dao = DAOFactory.getDAO(EnterpriseAddressDAO.class);
        return getList(dao.findByEnterPriseId(enterpriseId), EnterpriseAddressDTO.class);
    }

    @RpcService
    @Override
    public void deleteEnterpriseAddressById(List<Integer> ids) {
        EnterpriseAddressDAO addressDAO = DAOFactory.getDAO(EnterpriseAddressDAO.class);
        addressDAO.deleteEnterpriseAddressById(ids);
    }


}
