package recipe.serviceprovider.drug.service;

import com.google.common.collect.Lists;
import com.ngari.recipe.common.RecipeBussReqTO;
import com.ngari.recipe.common.RecipeListResTO;
import com.ngari.recipe.drug.model.DrugListBean;
import com.ngari.recipe.drug.service.IDrugService;
import com.ngari.recipe.entity.DrugList;
import ctd.persistence.DAOFactory;
import ctd.persistence.bean.QueryResult;
import ctd.persistence.exception.DAOException;
import ctd.util.BeanUtils;
import ctd.util.JSONUtils;
import ctd.util.PyConverter;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.dao.DrugListDAO;
import recipe.dao.OrganDrugListDAO;
import recipe.dao.PriortyDrugsBindDoctorDao;
import recipe.service.DrugListService;
import recipe.serviceprovider.BaseService;
import recipe.util.ApplicationUtils;
import recipe.util.MapValueUtil;

import java.util.Date;
import java.util.List;

/**
 * company: ngarihealth
 * @author: 0184/yu_yun
 * @date:2017/8/1.
 */
@RpcBean("remoteDrugService")
public class RemoteDrugService extends BaseService<DrugListBean> implements IDrugService {

    /** logger */
    private static final Logger LOGGER = LoggerFactory.getLogger(RemoteDrugService.class);

    @RpcService
    @Override
    public DrugListBean get(Object id) {
        DrugListDAO drugListDAO = DAOFactory.getDAO(DrugListDAO.class);
        DrugList drugList = drugListDAO.get(id);
        return getBean(drugList, DrugListBean.class);
    }

    @RpcService
    @Override
    public RecipeListResTO<DrugListBean> findDrugsByDepId(int depId) {
        DrugListDAO drugListDAO = DAOFactory.getDAO(DrugListDAO.class);
        List<DrugList> drugLists = drugListDAO.findDrugsByDepId(depId);
        List<DrugListBean> backList = getList(drugLists, DrugListBean.class);
        return RecipeListResTO.getSuccessResponse(backList);
    }

    @RpcService
    @Override
    public long countDrugsNumByOrganId(RecipeBussReqTO request) {
        Integer organId = MapValueUtil.getInteger(request.getConditions(), "organId");
        if (null == organId) {
            return 0L;
        }

        Long num = 0L;
        DrugListDAO drugListDAO = DAOFactory.getDAO(DrugListDAO.class);
        Integer drugType = MapValueUtil.getInteger(request.getConditions(), "drugType");
        if (null == drugType) {
            num = drugListDAO.getEffectiveDrugNum(organId);
        } else {
            num = drugListDAO.getSpecifyNum(organId, drugType);
        }

        return num.longValue();
    }

    @RpcService
    @Override
    public void changeDrugOrganId(int newOrganId, int oldOrganId) {
        OrganDrugListDAO organDrugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);
        organDrugListDAO.updateOrganIdByOrganId(newOrganId, oldOrganId);
    }

    @RpcService
    @Override
    public long countAllDrugsNumByOrganId(int organId) {
        OrganDrugListDAO organDrugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);
        return organDrugListDAO.getCountByOrganId(organId);
    }

    @RpcService
    @Override
    public List<Integer> queryOrganCanRecipe(List<Integer> organIds, Integer drugId) {
        OrganDrugListDAO organDrugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);
        return organDrugListDAO.queryOrganCanRecipe(organIds, drugId);
    }

    @RpcService
    @Override
    public List<Integer> findPriorityDoctorList(Integer drugId) {
        if (null == drugId){
            return Lists.newArrayList();
        }
        PriortyDrugsBindDoctorDao priortyDrugsBindDoctorDao = DAOFactory.getDAO(PriortyDrugsBindDoctorDao.class);
        return priortyDrugsBindDoctorDao.findPriortyDrugBindDoctors(drugId);
    }

    @RpcService
    @Override
    public DrugListBean addDrugList(DrugListBean d) {
        LOGGER.info("新增药品服务[addDrugList]:" + JSONUtils.toString(d));
        if (null == d) {
            throw new DAOException(DAOException.VALUE_NEEDED, "drugList is null");
        }
        //根据saleName 判断改药品是否已添加
        if (StringUtils.isEmpty(d.getDrugName())) {
            throw new DAOException(DAOException.VALUE_NEEDED, "drugName is needed");
        }
        if (StringUtils.isEmpty(d.getSaleName())) {
            throw new DAOException(DAOException.VALUE_NEEDED, "saleName is needed");
        }
        if (null == d.getPrice1()) {
            throw new DAOException(DAOException.VALUE_NEEDED, "price1 is needed");
        }
        if (null == d.getPrice2()) {
            throw new DAOException(DAOException.VALUE_NEEDED, "price2 is needed");
        }
        if (null == d.getDrugType()) {
            throw new DAOException(DAOException.VALUE_NEEDED, "drugType is needed");
        }
        if (StringUtils.isEmpty(d.getDrugClass())) {
            throw new DAOException(DAOException.VALUE_NEEDED, "drugClass is needed");
        }
        if (null == d.getStatus()) {
            d.setStatus(1);
        }
        d.setCreateDt(new Date());
        d.setLastModify(new Date());
        d.setAllPyCode(PyConverter.getPinYinWithoutTone(d.getSaleName()));
        d.setPyCode(PyConverter.getFirstLetter(d.getSaleName()));
        DrugListDAO dao = DAOFactory.getDAO(DrugListDAO.class);
        DrugList drugList = getBean(d, DrugList.class);
        drugList = dao.save(drugList);
        return getBean(drugList, DrugListBean.class);
    }

    @RpcService
    @Override
    public DrugListBean updateDrugList(DrugListBean d) {
        LOGGER.info("修改药品服务[updateDrugList]:" + JSONUtils.toString(d));
        if (null == d.getDrugId()) {
            throw new DAOException(DAOException.VALUE_NEEDED, "drugId is required");
        }
        DrugListDAO dao = DAOFactory.getDAO(DrugListDAO.class);
        DrugList target = dao.getById(d.getDrugId());
        if (null == target) {
            throw new DAOException(DAOException.ENTITIY_NOT_FOUND, "Can't found drugList");
        } else {
            d.setLastModify(new Date());
            BeanUtils.map(d, target);
            target = dao.update(target);
        }
        return getBean(target, DrugListBean.class);
    }

    @RpcService
    @Override
    public QueryResult<DrugListBean> queryDrugListsByDrugNameAndStartAndLimit(String drugClass, String keyword,
                                                                              Integer status, int start, int limit) {
        DrugListService drugListService = ApplicationUtils.getRecipeService(DrugListService.class);
        QueryResult<DrugList> result = drugListService.queryDrugListsByDrugNameAndStartAndLimit(drugClass, keyword,
                status, start, limit);
        List<DrugList> list = result.getItems();
        List<DrugListBean> beanList = getList(list, DrugListBean.class);
        QueryResult<DrugListBean> backList = new QueryResult<DrugListBean>();
        backList.setTotal(result.getTotal());
        backList.setStart(result.getStart());
        backList.setLimit((int)result.getLimit());
        backList.setItems(beanList);
        return backList;
    }
}
