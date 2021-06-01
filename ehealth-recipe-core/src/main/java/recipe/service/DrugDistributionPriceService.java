package recipe.service;

import com.alibaba.druid.util.StringUtils;
import com.ngari.bus.busactionlog.service.IBusActionLogService;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.drugdistributionprice.model.DrugDistributionPriceBean;
import com.ngari.recipe.drugdistributionprice.service.IDrugDistributionPriceService;
import com.ngari.recipe.entity.DrugDistributionPrice;
import ctd.persistence.DAOFactory;
import ctd.persistence.exception.DAOException;
import ctd.persistence.support.hibernate.template.AbstractHibernateStatelessResultAction;
import ctd.persistence.support.hibernate.template.HibernateSessionTemplate;
import ctd.persistence.support.hibernate.template.HibernateStatelessResultAction;
import ctd.util.AppContextHolder;
import ctd.util.BeanUtils;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import eh.utils.ValidateUtil;
import org.hibernate.StatelessSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.ApplicationUtils;
import recipe.dao.DrugDistributionPriceDAO;
import recipe.serviceprovider.BaseService;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * @author jianghc
 * @create 2017-02-07 14:51
 **/
@RpcBean("drugDistributionPriceService")
public class DrugDistributionPriceService extends BaseService<DrugDistributionPrice> implements IDrugDistributionPriceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DrugDistributionPriceService.class);

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
    @Override
    public DrugDistributionPriceBean saveOrUpdatePrice(DrugDistributionPriceBean price) {
        LOGGER.info("DrugDistributionPriceBean 实体信息： [{}]", price);
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
            DrugDistributionPrice bean = getBean(price, DrugDistributionPrice.class);
            bean.setCreateTime(new Date());
            bean.setLastModify(new Date());
            bean = drugDistributionPriceDAO.save(bean);
            BeanUtils.map(bean, price);
            logMsg.append(" 新增:").append(bean.toString());
            LOGGER.info("新增药企配送价格：[{}]", logMsg);
        } else {
            //更新
            if (oldPrice == null) {
                throw new DAOException("price is not exist");
            }
            if (!oldPrice.getId().equals(price.getId())) {
                throw new DAOException("price is exist and not this id");
            }
            BeanUtils.map(price, oldPrice);
            oldPrice.setLastModify(new Date());
            oldPrice = drugDistributionPriceDAO.update(oldPrice);
            logMsg.append(" 更新：原").append(oldPrice.toString()).append("更新为").append(oldPrice.toString());
        }

        try{
            com.ngari.opbase.base.service.IBusActionLogService iBusActionLogService1 = AppContextHolder.getBean("opbase.busActionLogService", com.ngari.opbase.base.service.IBusActionLogService.class);
            iBusActionLogService1.recordBusinessLogRpcNew("药企配送价格管理", price.getId().toString(), "DrugDistributionPrice", logMsg.toString(), com.ngari.opbase.base.service.IBusActionLogService.defaultSubjectName);
        } catch (Exception e) {
            LOGGER.error("业务日志记录失败： errorMessage[{}]", e.getMessage(), e);
        }
        return price;
    }

    @Override
    public void savePriceList(List<DrugDistributionPriceBean> priceList){
        LOGGER.info("savePriceList input： [{}]", JSONUtils.toString(priceList));
        if(ValidateUtil.notBlankList(priceList)) {
            //DrugDistributionPriceDAO drugDistributionPriceDAO = DAOFactory.getDAO(DrugDistributionPriceDAO.class);
            HibernateStatelessResultAction<Integer> action = new AbstractHibernateStatelessResultAction<Integer>() {
                @Override
                public void execute(StatelessSession ss) throws Exception {
                    for(DrugDistributionPriceBean priceBean : priceList){
                       /* drugDistributionPriceDAO.deleteByEnterpriseIdAddr(priceBean.getEnterpriseId(),priceBean.getAddrArea());
                        StringBuffer logMsg = new StringBuffer();
                        DrugDistributionPrice price = getBean(priceBean,DrugDistributionPrice.class);
                        price = drugDistributionPriceDAO.save(price);
                        logMsg.append(" 新增:").append(price.toString());
                        try{
                            com.ngari.opbase.base.service.IBusActionLogService iBusActionLogService1 = AppContextHolder.getBean("opbase.busActionLogService", com.ngari.opbase.base.service.IBusActionLogService.class);
                            iBusActionLogService1.recordBusinessLogRpcNew("药企配送价格管理", price.getId().toString(), "DrugDistributionPrice", logMsg.toString(), com.ngari.opbase.base.service.IBusActionLogService.defaultSubjectName);
                        } catch (Exception e) {
                            LOGGER.error("业务日志记录失败： errorMessage[{}]", e.getMessage(), e);
                        }*/
                        try {
                            saveOrUpdatePrice(priceBean);
                        } catch (Exception e) {
                            LOGGER.error("savePriceList error：", e);
                        }
                    }
                }
            };
            HibernateSessionTemplate.instance().executeTrans(action);
        }
    }

    @Override
    public void deleteByEnterpriseId(Integer enterpriseId) {
        if (enterpriseId == null) {
            throw new DAOException(DAOException.VALUE_NEEDED, "price is enterpriseId");
        }
        DrugDistributionPriceDAO drugDistributionPriceDAO = DAOFactory.getDAO(DrugDistributionPriceDAO.class);

        DrugDistributionPrice price = drugDistributionPriceDAO.get(enterpriseId);
        if (price == null) {
            throw new DAOException("this enterpriseId is not exist");
        }

        com.ngari.opbase.base.service.IBusActionLogService iBusActionLogService1 = AppContextHolder.getBean("opbase.busActionLogService", com.ngari.opbase.base.service.IBusActionLogService.class);
        iBusActionLogService1.recordBusinessLogRpcNew("药企配送价格管理", price.getId().toString(), "DrugDistributionPrice", "删除：" + price.toString(), com.ngari.opbase.base.service.IBusActionLogService.defaultSubjectName);
        drugDistributionPriceDAO.deleteByEnterpriseId(enterpriseId);
    }

    @Override
    public void deleteById(Integer id) {
        if (id == null) {
            throw new DAOException(DAOException.VALUE_NEEDED, "price is enterpriseId");
        }
        DAOFactory.getDAO(DrugDistributionPriceDAO.class).deleteById(id);
    }

    @RpcService
    @Override
    public List<DrugDistributionPriceBean> findByEnterpriseId(Integer enterpriseId) {
        if (enterpriseId == null) {
            throw new DAOException(DAOException.VALUE_NEEDED, "enterpriseId is enterpriseId");
        }
        List<DrugDistributionPrice> res = DAOFactory.getDAO(DrugDistributionPriceDAO.class).findByEnterpriseId(enterpriseId);
        return ObjectCopyUtils.convert(res, DrugDistributionPriceBean.class);
    }

    @RpcService
    @Override
    public DrugDistributionPriceBean getByEnterpriseIdAndAddrArea(Integer enterpriseId, String addrArea) {
        if (enterpriseId == null || StringUtils.isEmpty(addrArea)) {
            throw new DAOException(DAOException.VALUE_NEEDED, "enterpriseId is enterpriseId");
        }
        DrugDistributionPrice price = DAOFactory.getDAO(DrugDistributionPriceDAO.class).getByEnterpriseIdAndAddrArea(enterpriseId, addrArea);
        return getBean(price, DrugDistributionPriceBean.class);
    }

    @RpcService
    @Override
    public DrugDistributionPriceBean getDistributionPriceByEnterpriseIdAndAddrArea(Integer enterpriseId, String addrArea) {
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
        return getBean(price, DrugDistributionPriceBean.class);
    }


}
