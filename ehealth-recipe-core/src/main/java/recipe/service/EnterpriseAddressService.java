package recipe.service;

import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.drugsenterprise.model.EnterpriseAddressDTO;
import com.ngari.recipe.entity.DrugsEnterprise;
import com.ngari.recipe.entity.EnterpriseAddress;
import com.ngari.recipe.entity.Recipe;
import ctd.persistence.DAOFactory;
import ctd.persistence.bean.QueryResult;
import ctd.persistence.exception.DAOException;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import recipe.ApplicationUtils;
import recipe.constant.ErrorCode;
import recipe.constant.RecipeBussConstant;
import recipe.dao.DrugsEnterpriseDAO;
import recipe.dao.EnterpriseAddressDAO;
import recipe.dao.RecipeDAO;
import recipe.serviceprovider.BaseService;

import java.util.List;

/**
 * @author zhongzx
 * @date 2016/6/8
 */
@RpcBean("enterpriseAddressService")
public class EnterpriseAddressService extends BaseService<EnterpriseAddressDTO> {

    private static final Log LOGGER = LogFactory.getLog(EnterpriseAddressService.class);

    /**
     * zhongzx
     * 判断每个地址key值  是否可以配送
     *
     * @param list    药企可以配送的地址集
     * @param address 地址key值
     * @return
     */
    public boolean addressCanSend(List<EnterpriseAddress> list, String address) {
        boolean flag = false;
        if (StringUtils.isEmpty(address)) {
            return flag;
        }
        for (EnterpriseAddress e : list) {
            if (e.getAddress().startsWith(address)) {
                flag = true;
                break;
            }
        }
        return flag;
    }

    /**
     * zhongzx
     * 判断默认地址在不在配送区域内
     *
     * @param recipeId 处方单序号
     * @param address1 省份的key值
     * @param address2 城市的key值
     * @param address3 区域的key值
     * @return
     */
    @Deprecated
    @RpcService
    public int allAddressCanSend(Integer recipeId, String address1, String address2, String address3) {
        EnterpriseAddressDAO dao = DAOFactory.getDAO(EnterpriseAddressDAO.class);
        RecipeDAO rdao = DAOFactory.getDAO(RecipeDAO.class);
        RecipeService service = ApplicationUtils.getRecipeService(RecipeService.class);

        Recipe recipe = rdao.getByRecipeId(recipeId);
        if (null == recipe) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "allAddressCanSend 处方单不存在！recipeId:" + recipeId);
        }
        //查询对应药企配送的地址
        if (null == recipe.getEnterpriseId()) {
            Integer result = service.getDrugsEpsIdByOrganId(recipe.getRecipeId(), RecipeBussConstant.PAYMODE_ONLINE, null);
            if (-1 == result) {
                throw new DAOException(ErrorCode.SERVICE_ERROR, "allAddressCanSend 没有对应药企进行配送！depId:" + recipe.getEnterpriseId());
            }
        }
        recipe = rdao.getByRecipeId(recipeId);
        List<EnterpriseAddress> list = dao.findByEnterPriseId(recipe.getEnterpriseId());
        if (null == list || list.isEmpty()) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "allAddressCanSend 该药企没有配送地址！depId:" + recipe.getEnterpriseId());
        }
        //0-能配送 1-省不能配送 2-市不能配送 3-区域不能配送
        int flag = 0;
        if (!addressCanSend(list, address1)) {
            LOGGER.error("allAddressCanSend address1不能配送！depId:" + recipe.getEnterpriseId() + ",address1:" + address1);
            flag = 1;
            return flag;
        }
        if (!addressCanSend(list, address2)) {
            LOGGER.error("allAddressCanSend address2不能配送！depId:" + recipe.getEnterpriseId() + ",address2:" + address2);
            flag = 2;
            return flag;
        }
        if (!addressCanSend(list, address3)) {
            LOGGER.error("allAddressCanSend address3不能配送！depId:" + recipe.getEnterpriseId() + ",address3:" + address3);
            flag = 3;
            return flag;
        }
        return flag;
    }

    /**
     * 订单地址是否可配送
     *
     * @param depId
     * @param address1
     * @param address2
     * @param address3
     * @return
     */
    @RpcService
    public int allAddressCanSendForOrder(Integer depId, String address1, String address2, String address3) {
        EnterpriseAddressDAO enterpriseAddressDAO = DAOFactory.getDAO(EnterpriseAddressDAO.class);
        DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        //查询对应药企配送的地址
        //没有子订单而且配送药企为空，则提示
        if (null == depId) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "药企ID为空");
        }
        DrugsEnterprise drugsEnterprise = drugsEnterpriseDAO.getById(depId);
        if (drugsEnterprise != null && drugsEnterprise.getOrderType() == 0) {
            //标识跳转到第三方支付,不需要对配送地址进行校验
            return 0;
        }
        List<EnterpriseAddress> list = enterpriseAddressDAO.findByEnterPriseId(depId);
        if (CollectionUtils.isEmpty(list)) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "该药企没有配送地址");
        }
        //0-能配送 1-省不能配送 2-市不能配送 3-区域不能配送
        int flag = 0;
        if (!addressCanSend(list, address1)) {
            LOGGER.error("address1不能配送！depId:" + depId + ",address1:" + address1);
            flag = 1;
            return flag;
        }
        if (!addressCanSend(list, address2)) {
            LOGGER.error("address2不能配送！depId:" + depId + ",address2:" + address2);
            flag = 2;
            return flag;
        }
        if (!addressCanSend(list, address3)) {
            LOGGER.error("address3不能配送！depId:" + depId + ",address3:" + address3);
            flag = 3;
            return flag;
        }
        return flag;
    }

    /**
     * 添加药企配送地址
     *
     * @param enterpriseAddress
     * @return
     */
    @RpcService
    public EnterpriseAddressDTO addEnterpriseAddress(EnterpriseAddress enterpriseAddress) {
        EnterpriseAddressDAO addressDAO = DAOFactory.getDAO(EnterpriseAddressDAO.class);
        EnterpriseAddress address = addressDAO.addEnterpriseAddress(enterpriseAddress);
        return getBean(address, EnterpriseAddressDTO.class);
    }

    /**
     * 更新药企配送地址
     *
     * @param addressList
     * @return
     */
    @RpcService
    public List<EnterpriseAddressDTO> updateListEnterpriseAddress(final List<EnterpriseAddress> addressList) {
        EnterpriseAddressDAO addressDAO = DAOFactory.getDAO(EnterpriseAddressDAO.class);
        List<EnterpriseAddress> list = addressDAO.updateListEnterpriseAddress(addressList);
        return getList(list, EnterpriseAddressDTO.class);
    }

    /**
     * 查询药企配送地址
     *
     * @param enterpriseId 药企内码
     * @param status       配送地址状态
     * @param start
     * @param limit
     * @return
     */
    @RpcService
    public QueryResult<EnterpriseAddressDTO> queryEnterpriseAddressByLimitAndStart(final Integer enterpriseId,
                                                                                   final Integer status,
                                                                                   final int start, final int limit) {
        EnterpriseAddressDAO dao = DAOFactory.getDAO(EnterpriseAddressDAO.class);
        QueryResult result = dao.queryEnterpriseAddressByLimitAndStart(enterpriseId, status, start, limit);
        List<EnterpriseAddressDTO> list = getList(result.getItems(), EnterpriseAddressDTO.class);
        result.setItems(list);
        return result;
    }

    /**
     * 根据药企Id 查询能够配送的地址
     *
     * @param enterpriseId 药企Id
     * @return
     */
    @RpcService(timeout = 60)
    public List<EnterpriseAddressDTO> findByEnterPriseId(Integer enterpriseId) {
        EnterpriseAddressDAO dao = DAOFactory.getDAO(EnterpriseAddressDAO.class);
        List<EnterpriseAddressDTO> result = ObjectCopyUtils.convert(dao.findByEnterpriseIdAndStatus(enterpriseId, 1), EnterpriseAddressDTO.class);
        return result;
    }

    /**
     * 删除 药企配送地址
     *
     * @param ids
     * @return
     */
    @RpcService
    public void deleteEnterpriseAddressById(List<Integer> ids) {
        EnterpriseAddressDAO addressDAO = DAOFactory.getDAO(EnterpriseAddressDAO.class);
        addressDAO.deleteEnterpriseAddressById(ids);
    }

}
