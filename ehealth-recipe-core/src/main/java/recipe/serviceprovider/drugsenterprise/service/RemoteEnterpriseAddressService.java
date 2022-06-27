package recipe.serviceprovider.drugsenterprise.service;

import com.alibaba.druid.util.StringUtils;
import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;
import com.ngari.recipe.drugsenterprise.model.EnterpriseAddressDTO;
import com.ngari.recipe.drugsenterprise.service.IEnterpriseAddressService;
import com.ngari.recipe.entity.EnterpriseAddress;
import ctd.persistence.DAOFactory;
import ctd.persistence.exception.DAOException;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import ctd.util.event.GlobalEventExecFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import recipe.ApplicationUtils;
import recipe.dao.EnterpriseAddressDAO;
import recipe.service.EnterpriseAddressService;
import recipe.serviceprovider.BaseService;
import recipe.serviceprovider.recipeorder.service.RemoteRecipeOrderService;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.FutureTask;

/**
 * @author： 0184/yu_yun
 * @date： 2018/9/7
 * @description： TODO
 * @version： 1.0
 */
@RpcBean("remoteEnterpriseAddressService")
public class RemoteEnterpriseAddressService extends BaseService<EnterpriseAddressDTO> implements IEnterpriseAddressService {
    private static final Logger LOGGER = LoggerFactory.getLogger(RemoteRecipeOrderService.class);
    @Autowired
    private EnterpriseAddressDAO enterpriseAddressDAO;

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

    @RpcService(timeout = 60)
    @Override
    public void addEnterpriseAddressList(List<EnterpriseAddressDTO> enterpriseAddressDTOList, Integer enterpriseId) {
        LOGGER.info("addEnterpriseAddressList enterpriseId=[{}],size=[{}]", enterpriseId, enterpriseAddressDTOList.size());
        EnterpriseAddressDAO addressDAO = DAOFactory.getDAO(EnterpriseAddressDAO.class);
        addressDAO.deleteByEnterpriseId(enterpriseId);
        if (CollectionUtils.isEmpty(enterpriseAddressDTOList)) {
            return;
        }
        List<FutureTask<String>> futureTasks = new LinkedList<>();
        List<List<EnterpriseAddressDTO>> groupList = Lists.partition(enterpriseAddressDTOList, 500);
        groupList.forEach(a -> {
            FutureTask<String> ft = new FutureTask<>(() -> batchAddEnterpriseAddress(a));
            futureTasks.add(ft);
            GlobalEventExecFactory.instance().getExecutor().submit(ft);
        });
        String result = "";
        try {
            for (FutureTask<String> futureTask : futureTasks) {
                String str = futureTask.get();
                if (!"200".equals(str)) {
                    result = str;
                    break;
                }
            }
        } catch (Exception e) {
            LOGGER.error("addEnterpriseAddressList error ", e);
            throw new DAOException(DAOException.VALUE_NEEDED, "address error");
        }

        if (!StringUtils.isEmpty(result)) {
            throw new DAOException(DAOException.VALUE_NEEDED, result);
        }
    }

    private String batchAddEnterpriseAddress(List<EnterpriseAddressDTO> enterpriseAddressDTOList) {
        for (EnterpriseAddressDTO enterpriseAddressDTO : enterpriseAddressDTOList) {
            EnterpriseAddress enterpriseAddress = getBean(enterpriseAddressDTO, EnterpriseAddress.class);
            try {
                enterpriseAddressDAO.addEnterpriseAddress(enterpriseAddress);
            } catch (Exception e) {
                LOGGER.warn("addEnterpriseAddressList error enterpriseAddress = {}", JSON.toJSONString(enterpriseAddress), e);
                return e.getMessage();
            }
        }
        return "200";
    }

    @Override
    public int allAddressCanSendForOrder(Integer depId, String address1, String address2, String address3) {
        EnterpriseAddressService service = ApplicationUtils.getRecipeService(EnterpriseAddressService.class);
        return service.allAddressCanSendForOrder(depId, address1, address2, address3);
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
