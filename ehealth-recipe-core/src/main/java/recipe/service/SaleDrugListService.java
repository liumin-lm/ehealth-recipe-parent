package recipe.service;

import com.ngari.recipe.entity.SaleDrugList;
import ctd.persistence.DAOFactory;
import ctd.persistence.bean.QueryResult;
import ctd.persistence.exception.DAOException;
import ctd.util.BeanUtils;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.log4j.Logger;
import recipe.bean.DrugListAndSaleDrugList;
import recipe.constant.ErrorCode;
import recipe.dao.DrugListDAO;
import recipe.dao.DrugsEnterpriseDAO;
import recipe.dao.SaleDrugListDAO;

import java.util.Date;

/**
 * @author houxr
 * @date 2016/7/14
 * 销售药品管理服务
 */
@RpcBean("saleDrugListService")
public class SaleDrugListService {

    private static Logger logger = Logger.getLogger(SaleDrugListService.class);

    private void validateSaleDrugList(SaleDrugList saleDrugList) {
        if (null == saleDrugList) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "药品信息不能为空");
        }
//        if (StringUtils.isEmpty(saleDrugList.getOrganDrugCode())) {
//            throw new DAOException(DAOException.VALUE_NEEDED, "organDrugCode is needed");
//        }
//        if (null == saleDrugList.getRatePrice()) {
//            throw new DAOException(DAOException.VALUE_NEEDED, "ratePrice is needed");
//        }
        if (null == saleDrugList.getDrugId()) {
            throw new DAOException(DAOException.VALUE_NEEDED, "drugId is needed");
        }
        if (null == saleDrugList.getOrganId()) {
            throw new DAOException(DAOException.VALUE_NEEDED, "organId is needed");
        }
        if (null == saleDrugList.getPrice()) {
            throw new DAOException(DAOException.VALUE_NEEDED, "price is needed");
        }
    }

    /**
     * 新增销售机构药品服务
     *
     * @param saleDrugList
     * @return
     * @author houxr
     */
    @RpcService
    public boolean addSaleDrugList(SaleDrugList saleDrugList) {
        logger.info("新增销售机构药品服务[addSaleDrugList]:" + JSONUtils.toString(saleDrugList));
        SaleDrugListDAO dao = DAOFactory.getDAO(SaleDrugListDAO.class);
        if (null == saleDrugList) {
            throw new DAOException(DAOException.VALUE_NEEDED, "saleDrugList is null");
        }
        DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        if (!drugsEnterpriseDAO.exist(saleDrugList.getOrganId())) {
            throw new DAOException(DAOException.VALUE_NEEDED, "DrugsEnterprise not exist");
        }
        DrugListDAO drugListDAO = DAOFactory.getDAO(DrugListDAO.class);
        if (!drugListDAO.exist(saleDrugList.getDrugId())) {
            throw new DAOException(DAOException.VALUE_NEEDED, "DrugList not exist");
        }

        //验证药品必要信息
        validateSaleDrugList(saleDrugList);
        saleDrugList.setCreateDt(new Date());
        saleDrugList.setStatus(1);
        dao.save(saleDrugList);
        return true;
    }


    /**
     * 更新销售机构药品信息
     *
     * @param saleDrugList
     * @return
     * @author houxr
     */
    @RpcService
    public SaleDrugList updateSaleDrugList(SaleDrugList saleDrugList) {
        logger.info("修改销售机构药品服务[updateSaleDrugList]:" + JSONUtils.toString(saleDrugList));
        if (null == saleDrugList.getDrugId()) {
            throw new DAOException(DAOException.VALUE_NEEDED, "drugId is required");
        }
        SaleDrugListDAO saleDrugListDAO = DAOFactory.getDAO(SaleDrugListDAO.class);
        SaleDrugList target = saleDrugListDAO.get(saleDrugList.getOrganDrugId());
        if (null == target) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "此药在该医院药品列表中不存在");
        } else {
            BeanUtils.map(saleDrugList, target);
            validateSaleDrugList(target);
            target.setLastModify(new Date());
            target = saleDrugListDAO.update(target);
        }
        return target;
    }

    /**
     * 销售机构药品查询
     *
     * @param organId   机构
     * @param drugClass 药品分类
     * @param keyword   查询关键字:药品序号 or 药品名称 or 生产厂家 or 商品名称 or 批准文号
     * @param start
     * @param limit
     * @return
     * @author houxr
     */
    @RpcService
    public QueryResult<DrugListAndSaleDrugList> querySaleDrugListByOrganIdAndKeyword(final Integer organId,
                                                                                     final String drugClass,
                                                                                     final String keyword, final Integer status,
                                                                                     final int start, final int limit) {
        SaleDrugListDAO saleDrugListDAO = DAOFactory.getDAO(SaleDrugListDAO.class);
        return saleDrugListDAO.querySaleDrugListByOrganIdAndKeyword(organId, drugClass, keyword, status, start, limit);
    }

}
