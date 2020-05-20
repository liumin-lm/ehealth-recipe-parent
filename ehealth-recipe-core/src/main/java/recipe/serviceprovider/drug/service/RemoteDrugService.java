package recipe.serviceprovider.drug.service;

import com.google.common.collect.Lists;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.common.RecipeBussReqTO;
import com.ngari.recipe.common.RecipeListResTO;
import com.ngari.recipe.drug.model.DispensatoryDTO;
import com.ngari.recipe.drug.model.DrugListBean;
import com.ngari.recipe.drug.service.IDrugService;
import com.ngari.recipe.entity.Dispensatory;
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
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.ApplicationUtils;
import recipe.dao.DispensatoryDAO;
import recipe.dao.DrugListDAO;
import recipe.dao.OrganDrugListDAO;
import recipe.dao.PriortyDrugsBindDoctorDao;
import recipe.service.DrugListService;
import recipe.serviceprovider.BaseService;
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
        if (StringUtils.isBlank(drugList.getDrugCode())){
            //若药品编码为空，则将主键值（drugId）同步到药品编码字段（drugCode）
            drugList.setDrugCode(String.valueOf(drugList.getDrugId()));
            dao.update(drugList);
        }

        saveDispensatory(d, drugList.getDrugId());

        return getBean(drugList, DrugListBean.class);
    }

    @RpcService
    @Override
    public DrugListBean updateDrugList(DrugListBean d) {
        DrugList drugList = ObjectCopyUtils.convert(d, DrugList.class);
        LOGGER.info("修改药品服务[updateDrugList]:" + JSONUtils.toString(d));
        if (null == d.getDrugId()) {
            throw new DAOException(DAOException.VALUE_NEEDED, "drugId is required");
        }
        DrugListDAO dao = DAOFactory.getDAO(DrugListDAO.class);
        DrugList target = dao.getById(d.getDrugId());
        if (null == target) {
            throw new DAOException(DAOException.ENTITIY_NOT_FOUND, "Can't found drugList");
        } else {
            drugList.setLastModify(new Date());
            if(null == drugList.getAllPyCode()){
                drugList.setAllPyCode(target.getAllPyCode());
            }
            if(null == drugList.getApprovalNumber()){
                drugList.setApprovalNumber(target.getApprovalNumber());
            }
            if(null == drugList.getBaseDrug()){
                drugList.setBaseDrug(target.getBaseDrug());
            }
            if(null == drugList.getCreateDt()){
                drugList.setCreateDt(target.getCreateDt());
            }
            if(null == drugList.getDrugClass()){
                drugList.setDrugClass(target.getDrugClass());
            }
            if(null == drugList.getDrugForm()){
                drugList.setDrugForm(target.getDrugForm());
            }
            if(null == drugList.getDrugId()){
                drugList.setDrugId(target.getDrugId());
            }
            if(null == drugList.getDrugName()){
                drugList.setDrugName(target.getDrugName());
            }
            if(null == drugList.getDrugPic()){
                drugList.setDrugPic(target.getDrugPic());
            }
            if(null == drugList.getDrugSpec()){
                drugList.setDrugSpec(target.getDrugSpec());
            }
            if(null == drugList.getDrugType()){
                drugList.setDrugType(target.getDrugType());
            }
            if(null == drugList.getHighlightedField()){
                drugList.setHighlightedField(target.getHighlightedField());
            }
            if(null == drugList.getHighlightedFieldForIos()){
                drugList.setHighlightedFieldForIos(target.getHighlightedFieldForIos());
            }
            if(null == drugList.getHospitalPrice()){
                drugList.setHospitalPrice(target.getHospitalPrice());
            }
            if(null == drugList.getIndications()){
                drugList.setIndications(target.getIndications());
            }
            if(null == drugList.getLastModify()){
                drugList.setLastModify(target.getLastModify());
            }
            if(null == drugList.getOrganDrugCode()){
                drugList.setOrganDrugCode(target.getOrganDrugCode());
            }
            if(null == drugList.getPack()){
                drugList.setPack(target.getPack());
            }
            if(null == drugList.getPrice1()){
                drugList.setPrice1(target.getPrice1());
            }
            if(null == drugList.getPrice2()){
                drugList.setPrice2(target.getPrice2());
            }
            if(null == drugList.getPyCode()){
                drugList.setPyCode(target.getPyCode());
            }
            if(null == drugList.getSaleName()){
                drugList.setSaleName(target.getSaleName());
            }
            if(null == drugList.getSourceOrgan()){
                drugList.setSourceOrgan(target.getSourceOrgan());
            }
            if(null == drugList.getStandardCode()){
                drugList.setStandardCode(target.getStandardCode());
            }
            if(null == drugList.getStatus()){
                drugList.setStatus(target.getStatus());
            }
            if(null == drugList.getUnit()){
                drugList.setUnit(target.getUnit());
            }
            if(null == drugList.getUseDose()){
                drugList.setUseDose(target.getUseDose());
            }
            if(null == drugList.getUseDoseUnit()){
                drugList.setUseDoseUnit(target.getUseDoseUnit());
            }
            if(null == drugList.getUsePathways()){
                drugList.setUsePathways(target.getUsePathways());
            }
            if(null == drugList.getUsingRate()){
                drugList.setUsingRate(target.getUsingRate());
            }
            if(null == drugList.getProducer()){
                drugList.setProducer(target.getProducer());
            }
            if(null == drugList.getInstructions()){
                drugList.setInstructions(target.getInstructions());
            }

           /*BeanUtils.map(drugList, target);*/
            drugList = dao.update(drugList);
            if(null != d.getDispensatory()) {
                DispensatoryDAO dispensatoryDAO = DAOFactory.getDAO(DispensatoryDAO.class);
                Dispensatory dispensatory = dispensatoryDAO.getByDrugId(drugList.getDrugId());
                if(null == dispensatory){
                    saveDispensatory(d, drugList.getDrugId());
                }else{
                    dispensatory.setLastModifyTime(new Date());
                    BeanUtils.map(d.getDispensatory(), dispensatory);
                    dispensatoryDAO.update(dispensatory);
                }
            }
        }
        return getBean(drugList, DrugListBean.class);
    }

    private void saveDispensatory(DrugListBean d, Integer drugId){
        if(null != d.getDispensatory()) {
            DispensatoryDAO dispensatoryDAO = DAOFactory.getDAO(DispensatoryDAO.class);
            DispensatoryDTO dispensatoryDTO = d.getDispensatory();
            dispensatoryDTO.setDrugId(drugId);
            dispensatoryDTO.setName(d.getDrugName()+"("+d.getSaleName()+")");
            dispensatoryDTO.setDrugName(d.getDrugName());
            dispensatoryDTO.setSaleName(d.getSaleName());
            dispensatoryDTO.setSpecs(d.getDrugSpec());
            Date now = DateTime.now().toDate();
            dispensatoryDTO.setCreateTime(now);
            dispensatoryDTO.setLastModifyTime(now);
            // 来源
            dispensatoryDTO.setSource(2);
            dispensatoryDTO.setPageId("0");

            dispensatoryDAO.save(ObjectCopyUtils.convert(dispensatoryDTO, Dispensatory.class));
        }
    }

    @RpcService
    @Override
    public QueryResult<DrugListBean> queryDrugListsByDrugNameAndStartAndLimit(final Date startTime, final Date endTime,String drugClass, String keyword,
                                                                              Integer status, int start, int limit) {
        DrugListService drugListService = ApplicationUtils.getRecipeService(DrugListService.class);
        QueryResult<DrugListBean> result = drugListService.queryDrugListsByDrugNameAndStartAndLimit(startTime,endTime,drugClass, keyword,
                status, start, limit);
        return result;
    }
}
