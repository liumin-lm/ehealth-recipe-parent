package recipe.service;

import com.alibaba.druid.util.StringUtils;
import com.ngari.bus.busactionlog.service.IBusActionLogService;
import com.ngari.recipe.entity.DrugDistributionPrice;
import ctd.persistence.DAOFactory;
import ctd.persistence.exception.DAOException;
import ctd.util.BeanUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import recipe.dao.DrugDistributionPriceDAO;
import recipe.util.ApplicationUtils;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author jianghc
 * @create 2017-02-07 14:51
 **/
@RpcBean("drugDistributionPriceService")
public class DrugDistributionPriceService {
    private IBusActionLogService iBusActionLogService =
            ApplicationUtils.getBaseService(IBusActionLogService.class);

    /**
     * 地域编码长度
     */
    private static final int ADDR_LENGTH = 2;

    /**
     * 保存或更新配送价格
     *
     * @param price
     * @return
     */
    @RpcService
    public DrugDistributionPrice saveOrUpdatePrice(DrugDistributionPrice price) {
        if (price == null) {
            throw new DAOException(DAOException.VALUE_NEEDED, "price is requrie");
        }
        if (price.getEnterpriseId() == null) {
            throw new DAOException(DAOException.VALUE_NEEDED, "price is enterpriseId");
        }
        if (price.getAddrArea() == null) {
            throw new DAOException(DAOException.VALUE_NEEDED, "price is addrArea");
        }
        if (price.getDistributionPrice() == null) {
            price.setDistributionPrice(new BigDecimal(0));
        }
        DrugDistributionPriceDAO drugDistributionPriceDAO = DAOFactory.getDAO(DrugDistributionPriceDAO.class);

        DrugDistributionPrice oldPrice = drugDistributionPriceDAO.getByEnterpriseIdAndAddrArea(price.getEnterpriseId(), price.getAddrArea());
        StringBuffer logMsg = new StringBuffer();
        if (price.getId() == null) {
            //新增
            if (oldPrice != null) {
                throw new DAOException("price is exist");
            }
            price = drugDistributionPriceDAO.save(price);
            logMsg.append(" 新增:").append(price.toString());

        } else {
            //更新
            if (oldPrice == null) {
                throw new DAOException("price is not exist");
            }
            if (!oldPrice.getId().equals(price.getId())) {
                throw new DAOException("price is exist and not this id");
            }
            BeanUtils.map(price, oldPrice);
            price = drugDistributionPriceDAO.update(oldPrice);
            logMsg.append(" 更新：原").append(oldPrice.toString()).append("更新为").append(price.toString());
        }
        iBusActionLogService.saveBusinessLog("药企配送价格管理", price.getId().toString(), "DrugDistributionPrice", logMsg.toString());

        return price;
    }

    @RpcService
    public void deleteByEnterpriseId(Integer enterpriseId) {
        if (enterpriseId == null) {
            throw new DAOException(DAOException.VALUE_NEEDED, "price is enterpriseId");
        }
        DrugDistributionPriceDAO drugDistributionPriceDAO = DAOFactory.getDAO(DrugDistributionPriceDAO.class);

        DrugDistributionPrice price = drugDistributionPriceDAO.get(enterpriseId);
        if (price == null) {
            throw new DAOException("this enterpriseId is not exist");
        }

        iBusActionLogService.saveBusinessLog("药企配送价格管理", price.getId().toString(), "DrugDistributionPrice", "删除：" + price.toString());
        drugDistributionPriceDAO.deleteByEnterpriseId(enterpriseId);
    }

    @RpcService
    public void deleteById(Integer id) {
        if (id == null) {
            throw new DAOException(DAOException.VALUE_NEEDED, "price is enterpriseId");
        }
        DAOFactory.getDAO(DrugDistributionPriceDAO.class).deleteById(id);
    }

    @RpcService
    public List<DrugDistributionPrice> findByEnterpriseId(Integer enterpriseId) {
        if (enterpriseId == null) {
            throw new DAOException(DAOException.VALUE_NEEDED, "enterpriseId is enterpriseId");
        }
        return DAOFactory.getDAO(DrugDistributionPriceDAO.class).findByEnterpriseId(enterpriseId);
    }

    @RpcService
    public DrugDistributionPrice getDistributionPriceByEnterpriseIdAndAddrArea(Integer enterpriseId, String addrArea) {
        if (enterpriseId == null) {
            throw new DAOException(DAOException.VALUE_NEEDED, "enterpriseId is enterpriseId");
        }
        if (addrArea == null || StringUtils.isEmpty(addrArea.trim())) {
            throw new DAOException(DAOException.VALUE_NEEDED, "addrArea is enterpriseId");
        }
        DrugDistributionPriceDAO drugDistributionPriceDAO = DAOFactory.getDAO(DrugDistributionPriceDAO.class);

        //获取地域编码长度
        int length = addrArea.length();
        DrugDistributionPrice price = null;

        while (length >= ADDR_LENGTH) {
            price = drugDistributionPriceDAO.getByEnterpriseIdAndAddrArea(enterpriseId, addrArea.substring(0, length));
            if (price != null) {
                break;
            }
            length -= 2;
        }
        if (price == null) {
            price = drugDistributionPriceDAO.getByEnterpriseIdAndAddrArea(enterpriseId, null);
        }
        return price;
    }


}
