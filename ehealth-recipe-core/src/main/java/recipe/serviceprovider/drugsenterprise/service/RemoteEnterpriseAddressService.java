package recipe.serviceprovider.drugsenterprise.service;

import com.ngari.recipe.drugsenterprise.model.EnterpriseAddressDTO;
import com.ngari.recipe.drugsenterprise.service.IEnterpriseAddressService;
import com.ngari.recipe.entity.EnterpriseAddress;
import ctd.persistence.DAOFactory;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import eh.utils.ValidateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.ApplicationUtils;
import recipe.dao.EnterpriseAddressDAO;
import recipe.service.EnterpriseAddressService;
import recipe.serviceprovider.BaseService;
import recipe.serviceprovider.recipeorder.service.RemoteRecipeOrderService;

import java.util.List;

/**
 * @author： 0184/yu_yun
 * @date： 2018/9/7
 * @description： TODO
 * @version： 1.0
 */
@RpcBean("remoteEnterpriseAddressService")
public class RemoteEnterpriseAddressService extends BaseService<EnterpriseAddressDTO> implements IEnterpriseAddressService {
    private static final Logger LOGGER = LoggerFactory.getLogger(RemoteRecipeOrderService.class);

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

    @RpcService(timeout = 600000)
    @Override
    public void addEnterpriseAddressList(List<EnterpriseAddressDTO> enterpriseAddressDTOList) {
        EnterpriseAddressDAO addressDAO = DAOFactory.getDAO(EnterpriseAddressDAO.class);
        if(ValidateUtil.notBlankList(enterpriseAddressDTOList)) {
            Integer enterpriseId=enterpriseAddressDTOList.get(0).getEnterpriseId();
            LOGGER.info("addEnterpriseAddressList EnterpriseId=[{}],size=[{}]",enterpriseId,enterpriseAddressDTOList.size());

            addressDAO.deleteByEnterpriseId(enterpriseId);
            for (EnterpriseAddressDTO enterpriseAddressDTO : enterpriseAddressDTOList) {
                EnterpriseAddress enterpriseAddress = getBean(enterpriseAddressDTO, EnterpriseAddress.class);
                EnterpriseAddress address = addressDAO.addEnterpriseAddress(enterpriseAddress);
            }
        }
    }

    @Override
    public int allAddressCanSendForOrder(Integer depId, String address1, String address2, String address3) {
        EnterpriseAddressService service = ApplicationUtils.getRecipeService(EnterpriseAddressService.class);
        return service.allAddressCanSendForOrder(depId,address1,address2,address3);
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
