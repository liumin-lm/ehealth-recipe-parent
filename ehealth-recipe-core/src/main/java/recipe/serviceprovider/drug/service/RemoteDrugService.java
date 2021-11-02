package recipe.serviceprovider.drug.service;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.ngari.base.dto.UsePathwaysDTO;
import com.ngari.base.dto.UsingRateDTO;
import com.ngari.base.property.service.IConfigurationCenterUtilsService;
import com.ngari.bus.op.service.IUsePathwaysService;
import com.ngari.bus.op.service.IUsingRateService;
import com.ngari.opbase.base.service.IBusActionLogService;
import com.ngari.patient.dto.OrganDTO;
import com.ngari.patient.service.BasicAPI;
import com.ngari.patient.service.OrganService;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.platform.recipe.mode.HisDrugListBean;
import com.ngari.platform.recipe.mode.HisOrganDrugListBean;
import com.ngari.recipe.RecipeAPI;
import com.ngari.recipe.common.RecipeBussReqTO;
import com.ngari.recipe.common.RecipeListResTO;
import com.ngari.recipe.drug.model.DispensatoryDTO;
import com.ngari.recipe.drug.model.DrugListBean;
import com.ngari.recipe.drug.service.IDrugService;
import com.ngari.recipe.drug.service.IOrganDrugListService;
import com.ngari.recipe.drug.service.ISaleDrugListService;
import com.ngari.recipe.entity.Dispensatory;
import com.ngari.recipe.entity.DrugList;
import com.ngari.recipe.entity.DrugListMatch;
import com.ngari.recipe.entity.OrganDrugList;
import com.squareup.moshi.Json;
import ctd.account.UserRoleToken;
import ctd.persistence.DAOFactory;
import ctd.persistence.bean.QueryResult;
import ctd.persistence.exception.DAOException;
import ctd.spring.AppDomainContext;
import ctd.util.AppContextHolder;
import ctd.util.BeanUtils;
import ctd.util.JSONUtils;
import ctd.util.PyConverter;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.omg.CORBA.TIMEOUT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import recipe.ApplicationUtils;
import recipe.dao.DispensatoryDAO;
import recipe.dao.DrugListDAO;
import recipe.dao.OrganDrugListDAO;
import recipe.dao.PriortyDrugsBindDoctorDao;
import recipe.service.DrugListService;
import recipe.service.OrganDrugListService;
import recipe.service.RecipePreserveService;
import recipe.serviceprovider.BaseService;
import recipe.thread.RecipeBusiThreadPool;
import recipe.util.MapValueUtil;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

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

        DrugListDAO drugListDAO = DAOFactory.getDAO(DrugListDAO.class);
        List<DrugList> drugLists = drugListDAO.findRepeatDrugListNoOrgan(d.getDrugName(),d.getSaleName(),d.getDrugType(),d.getProducer(),d.getDrugSpec());
        if(!CollectionUtils.isEmpty(drugLists)){
            throw new DAOException(DAOException.VALIDATE_FALIED, "此药品已经存在，对应药品为【"+drugLists.get(0).getDrugCode()+"】【"+d.getDrugName()+"】，请勿重复添加。");
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
         /*   if(null == drugList.getDrugPic()){
                drugList.setDrugPic(target.getDrugPic());
            }*/
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
            if(null == drugList.getUsePathwaysId()){
                drugList.setUsePathwaysId(target.getUsePathwaysId());
            }
            if(null == drugList.getUsingRateId()){
                drugList.setUsingRateId(target.getUsingRateId());
            }
            if(null == drugList.getProducer()){
                drugList.setProducer(target.getProducer());
            }
            if(null == drugList.getInstructions()){
                drugList.setInstructions(target.getInstructions());
            }

            List<DrugList> drugLists = dao.findRepeatDrugListNoOrgan(d.getDrugName(),d.getSaleName(),d.getDrugType(),d.getProducer(),d.getDrugSpec());
            if(!CollectionUtils.isEmpty(drugLists)){
                boolean flag = true;
                for (DrugList drg : drugLists){
                    if(drg.getDrugId().equals(drugList.getDrugId())){
                        flag = false;
                    }
                }
                if(flag){
                    throw new DAOException(DAOException.VALIDATE_FALIED, "此药品已经存在，对应药品为【"+drugLists.get(0).getDrugCode()+"】【"+d.getDrugName()+"】，请勿修改。");
                }
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

    @Override
    public QueryResult<DrugListBean> queryDrugListsByDrugNameAndStartAndLimit(String drugClass, String keyword,
                                                                              Integer status,final Integer drugSourcesId,Integer type, Integer isStandardDrug, int start, int limit) {
        DrugListService drugListService = ApplicationUtils.getRecipeService(DrugListService.class);
        QueryResult<DrugListBean> result = drugListService.queryDrugListsByDrugNameAndStartAndLimit(drugClass, keyword,
                status,drugSourcesId,type,isStandardDrug, start, limit);
        return result;
    }

    @Override
    public DispensatoryDTO getByDrugId(Integer drugId) {
        DispensatoryDAO dispensatoryDAO = DAOFactory.getDAO(DispensatoryDAO.class);
        Dispensatory dispensatory = dispensatoryDAO.getByDrugId(drugId);
        return getBean(dispensatory,DispensatoryDTO.class);
    }

    @Override
    public List<HisDrugListBean> findDrugList(Integer start, Integer limit) {
        DrugListDAO drugListDAO = DAOFactory.getDAO(DrugListDAO.class);
        List<HisDrugListBean> hisDrugListBeen = Lists.newArrayList();
        QueryResult<DrugList> queryResult = drugListDAO.queryDrugListsByDrugNameAndStartAndLimit("","",1,null,null,null,start,limit);
        List<DrugList> lists = queryResult.getItems();
        if (CollectionUtils.isEmpty(lists)){
            return hisDrugListBeen;
        }
        for (DrugList drugList : lists){
            HisDrugListBean hisDrugListBean = new HisDrugListBean();
            hisDrugListBean.setDrugId(drugList.getDrugId());
            hisDrugListBean.setDrugName(drugList.getDrugName());
            hisDrugListBean.setSaleName(drugList.getSaleName());
            hisDrugListBean.setDrugSpec(drugList.getDrugSpec());
            hisDrugListBean.setPack(drugList.getPack());
            hisDrugListBean.setUnit(drugList.getUnit());
            hisDrugListBean.setDrugType(drugList.getDrugType());
            hisDrugListBean.setDrugClass(drugList.getDrugClass());
            hisDrugListBean.setUseDose(drugList.getUseDose());
            hisDrugListBean.setUseDoseUnit(drugList.getUseDoseUnit());
            hisDrugListBean.setUsingRate(drugList.getUsingRate());
            hisDrugListBean.setUsePathways(drugList.getUsePathways());
            hisDrugListBean.setProducer(drugList.getProducer());
            hisDrugListBean.setIndications("");
            hisDrugListBean.setStatus(drugList.getStatus());
            hisDrugListBean.setPyCode(drugList.getPyCode());
            hisDrugListBean.setCreateDt(drugList.getCreateDt());
            hisDrugListBean.setLastModify(drugList.getLastModify());
            hisDrugListBean.setLicenseNumber(drugList.getApprovalNumber());
            hisDrugListBean.setDrugForm(drugList.getDrugForm());
            hisDrugListBean.setStandardCode(drugList.getStandardCode());
            hisDrugListBean.setPackingMaterials("");
            hisDrugListBeen.add(hisDrugListBean);
        }
        return hisDrugListBeen;
    }

    @Override
    public Boolean saveCompareDrugListData(List<HisOrganDrugListBean> listBeen) {
        if (CollectionUtils.isEmpty(listBeen)){
            return Boolean.TRUE;
        }
        IConfigurationCenterUtilsService configurationCenterUtilsService = (IConfigurationCenterUtilsService)AppContextHolder.getBean("eh.configurationCenterUtils");
        OrganDrugListDAO organDrugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);
        OrganDrugListService organDrugListService= AppContextHolder.getBean("organDrugListService", OrganDrugListService.class);
        HisOrganDrugListBean drugListMatch = listBeen.get(0);
        Boolean organDrugFromHis = (Boolean) configurationCenterUtilsService.getConfiguration(drugListMatch.getOrganId(),"organDrugFromHis");
        if (!organDrugFromHis){
            LOGGER.info("His接口返回"+JSONUtils.toString(listBeen));
            return Boolean.TRUE;
        }
        OrganService organService = BasicAPI.getService(OrganService.class);
        OrganDTO organDTO = organService.getByOrganId(drugListMatch.getOrganId());
        //【机构名称】批量新增药品【编码-药品名】……
        StringBuilder saveMsg = new StringBuilder("【"+organDTO.getName()+"】his批量新增药品");
        //【机构名称】更新药品【编码-药品名】……
        StringBuilder updateMsg = new StringBuilder("【"+organDTO.getName()+"】his更新药品");
        for (HisOrganDrugListBean bean : listBeen){
            if (bean.getDrugId() != null) {
                OrganDrugList organDrugList = new OrganDrugList();
                organDrugList.setDrugId(bean.getDrugId());
                organDrugList.setOrganDrugCode(bean.getOrganDrugCode());
                organDrugList.setOrganId(bean.getOrganId());
                if (bean.getSalePrice() == null) {
                    organDrugList.setSalePrice(new BigDecimal(0));
                } else {
                    organDrugList.setSalePrice(bean.getSalePrice());
                }
                organDrugList.setDrugName(bean.getDrugName());
                if (StringUtils.isEmpty(bean.getSaleName())) {
                    organDrugList.setSaleName(bean.getDrugName());
                } else {
                    if (bean.getSaleName().equals(bean.getDrugName())) {
                        organDrugList.setSaleName(bean.getSaleName());
                    } else {
                        organDrugList.setSaleName(bean.getSaleName() + " " + bean.getDrugName());
                    }

                }

                organDrugList.setUsingRate(bean.getUsingRate());
                organDrugList.setUsePathways(bean.getUsePathways());
                organDrugList.setProducer(bean.getProducer());
                organDrugList.setUseDose(bean.getUseDose());
                organDrugList.setPack(bean.getPack());
                organDrugList.setUnit(bean.getUnit());
                organDrugList.setUseDoseUnit(bean.getUseDoseUnit());
                organDrugList.setDrugSpec(bean.getDrugSpec());
                organDrugList.setDrugForm(bean.getDrugForm());
                organDrugList.setLicenseNumber(bean.getLicenseNumber());
                organDrugList.setTakeMedicine(0);
                organDrugList.setStatus(bean.getStatus());
                organDrugList.setProducerCode("");
                organDrugList.setLastModify(new Date());

                Boolean isSuccess = organDrugListDAO.updateData(organDrugList);
                if (!isSuccess) {
                    organDrugListDAO.save(organDrugList);
                    saveMsg.append("【"+organDrugList.getDrugId()+"-"+organDrugList.getDrugName()+"】");
                    //同步药品到监管备案
                    RecipeBusiThreadPool.submit(() -> {
                        organDrugListService.uploadDrugToRegulation(organDrugList);
                        return null;
                    });
                }else {
                    //更新
                    updateMsg.append("【"+organDrugList.getDrugId()+"-"+organDrugList.getDrugName()+"】");
                }
            }
        }
        LOGGER.info(updateMsg.toString());
        LOGGER.info(saveMsg.toString());
        return Boolean.TRUE;
    }

    @RpcService(timeout = 600)
    public void dealUsingRate(){
        RecipePreserveService recipePreserveService = AppContextHolder.getBean("eh.recipePreserveService",RecipePreserveService.class);
        IUsingRateService usingRateService = AppContextHolder.getBean("eh.usingRateService",IUsingRateService.class);
        OrganDrugListDAO organDrugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);
        //查询含有药品的机构
        List<Integer> haveDrugOrgans = organDrugListDAO.findOrganIds();
        LOGGER.info("查询含有药品的机构{}",JSONUtils.toString(haveDrugOrgans));
        //已存在的对照同步处理
        Set<Integer> contrastOrganIds = Sets.newHashSet();
        List<UsingRateDTO> usingRateDTOs = recipePreserveService.findUsingRateRelationFromRedis();
        if (!CollectionUtils.isEmpty(usingRateDTOs)){
            contrastOrganIds = usingRateDTOs.stream().map(x ->x.getOrganId()).collect(Collectors.toSet());
        }
        LOGGER.info("查询含有对照的机构{}",contrastOrganIds);
        List<Integer> contrastOrganIdList = new ArrayList<>(contrastOrganIds);
        usingRateService.saveUsingRateBatch(usingRateDTOs);
        //没有对照的机构处理
        List<Integer> noContrastOrganIds = haveDrugOrgans.stream().filter(item -> !contrastOrganIdList.contains(item) && item > 0).collect(Collectors.toList());
        LOGGER.info("查询没有对照的机构{}",noContrastOrganIds);
        usingRateService.syncPlatToOrgan(noContrastOrganIds);
        //处理平台药品库
        this.dealDrugListUsingRate();
        //处理机构药品库
        this.dealOrganDrugListUsingRate();
    }

    //处理平台药品库
    @RpcService(timeout = 600)
    public void dealDrugListUsingRate(){
        DrugListDAO drugListDAO = DAOFactory.getDAO(DrugListDAO.class);
        List<String> usingRates = drugListDAO.findUsingRateOfAll();
        IUsingRateService usingRateService = AppContextHolder.getBean("eh.usingRateService",IUsingRateService.class);
        if (!CollectionUtils.isEmpty(usingRates)){
            usingRates.stream().filter(item -> !StringUtils.isEmpty(item)).forEach(item -> {
                UsingRateDTO usingRateDTO = usingRateService.findUsingRateDTOByOrganAndKey(0,item);
                if (usingRateDTO != null){
                    //存在就替换
                    drugListDAO.updateUsingRateByUsingRate(item,String.valueOf(usingRateDTO.getId()));
                }else {
                    //不存在就移除
                    drugListDAO.updateUsingRateByUsingRate(item,null);
                }
            });
        }
    }

    @RpcService(timeout = 600)
    public void dealOrganDrugListUsingRate(){
        OrganDrugListDAO organDrugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);
        List<Map<String,Object>> usingRates = organDrugListDAO.findAllUsingRate();
        IUsingRateService usingRateService = AppContextHolder.getBean("eh.usingRateService",IUsingRateService.class);
        if (!CollectionUtils.isEmpty(usingRates)){
            usingRates.forEach(item -> {
                UsingRateDTO usingRateDTO = null;
                Integer organId = (Integer) item.get("organId");
                String usingRate = (String) item.get("usingRate");
                try {
                    usingRateDTO  = usingRateService.findUsingRateDTOByOrganAndKey(organId,usingRate);
                }catch (Exception e){
                    LOGGER.error("查询失败",organId+"----"+usingRate);
                }

                if (usingRateDTO != null){
                    organDrugListDAO.updateUsingRateByUsingRate(organId,usingRate,String.valueOf(usingRateDTO.getId()));
                }
            });
        }
    }

    @RpcService(timeout = 600)
    public void dealUsePathways(){
        RecipePreserveService recipePreserveService = AppContextHolder.getBean("eh.recipePreserveService",RecipePreserveService.class);
        IUsePathwaysService usePathwaysService = AppContextHolder.getBean("eh.usePathwaysService",IUsePathwaysService.class);
        OrganDrugListDAO organDrugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);
        //查询含有药品的机构
        List<Integer> haveDrugOrgans = organDrugListDAO.findOrganIds();
        LOGGER.info("查询含有药品的机构{}",haveDrugOrgans);
        //已存在的对照同步处理
        Set<Integer> contrastOrganIds = Sets.newHashSet();
        List<UsePathwaysDTO> usePathwaysDTOs = recipePreserveService.findUsePathwaysRelationFromRedis();
        if (!CollectionUtils.isEmpty(usePathwaysDTOs)){
            contrastOrganIds = usePathwaysDTOs.stream().map(x ->x.getOrganId()).collect(Collectors.toSet());
        }
        List<Integer> contrastOrganIdList = new ArrayList<>(contrastOrganIds);
        usePathwaysService.saveUsePathwaysBatch(usePathwaysDTOs);
        //没有对照的机构处理
        List<Integer> noContrastOrganIds = haveDrugOrgans.stream().filter(item -> !contrastOrganIdList.contains(item) && item > 0).collect(Collectors.toList());
        usePathwaysService.syncPlatToOrgan(noContrastOrganIds);
        //处理平台药品库
        this.dealDrugListUsePathways();
        //处理机构药品库
        this.dealOrganDrugListUsePathways();
    }

    //处理平台药品库
    @RpcService(timeout = 600)
    public void dealDrugListUsePathways(){
        DrugListDAO drugListDAO = DAOFactory.getDAO(DrugListDAO.class);
        List<String> usePathways = drugListDAO.findUsePathwaysOfAll();
        IUsePathwaysService usePathwaysService = AppContextHolder.getBean("eh.usePathwaysService",IUsePathwaysService.class);
        if (!CollectionUtils.isEmpty(usePathways)){
            usePathways.stream().filter(item -> !StringUtils.isEmpty(item)).forEach(item -> {
                 UsePathwaysDTO usePathwaysDTO = usePathwaysService.findUsePathwaysByOrganAndKey(0,item);
                if (usePathwaysDTO != null){
                    //存在就替换
                    drugListDAO.updateUsePathwaysByUsePathways(item,String.valueOf(usePathwaysDTO.getId()));
                }else {
                    //不存在就移除
                    drugListDAO.updateUsePathwaysByUsePathways(item,null);
                }
            });
        }
    }

    @RpcService(timeout = 600)
    public void dealOrganDrugListUsePathways(){
        OrganDrugListDAO organDrugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);
        List<Map<String,Object>> usePathways = organDrugListDAO.findAllUsePathways();
        IUsePathwaysService usePathwaysService = AppContextHolder.getBean("eh.usePathwaysService",IUsePathwaysService.class);
        if (!CollectionUtils.isEmpty(usePathways)){
            usePathways.forEach(item -> {
                Integer organId = (Integer) item.get("organId");
                String usePathway = (String) item.get("usePathways");
                LOGGER.info("dealOrganDrugListUsePathways---4");
                UsePathwaysDTO usePathwaysDTO = usePathwaysService.findUsePathwaysByOrganAndKey(organId,usePathway);
                LOGGER.info("dealOrganDrugListUsePathways---"+organId+"--"+usePathway);
                if (usePathwaysDTO != null){
                    organDrugListDAO.updateUsePathwaysByUsePathways(organId,usePathway,String.valueOf(usePathwaysDTO.getId()));
                }
            });
        }
    }

    @RpcService
    @Override
    public DrugListBean deleteDrugList(Integer drugId) {
        if (null == drugId) {
            throw new DAOException(DAOException.VALUE_NEEDED, "drugId is required");
        }
        Long organNum = AppContextHolder.getBean("eh.organDrugListService",IOrganDrugListService.class).getCountByDrugId(drugId);
        Long saleNum = AppContextHolder.getBean("eh.saleDrugListService",ISaleDrugListService.class).getCountByDrugId(drugId);
        if(organNum>0 || saleNum>0){
            throw new DAOException(DAOException.VALIDATE_FALIED, "该通用药品存在关联的机构药品或者药企药品，不支持删除。");
        }

        DrugListDAO dao = DAOFactory.getDAO(DrugListDAO.class);
        IBusActionLogService busActionLogService = AppDomainContext.getBean("opbase.busActionLogService", IBusActionLogService.class);
        DrugList target = dao.getById(drugId);
        if (null == target) {
            throw new DAOException(DAOException.ENTITIY_NOT_FOUND, "Can't found drugList");
        }
        target.setStatus(0);
        UserRoleToken urt = UserRoleToken.getCurrent();
        DrugList drugList = dao.update(target);
        busActionLogService.recordBusinessLogRpcNew("通用药品管理", "", "DrugList", "【" + urt.getUserName() + "】删除药品【" + target.getDrugName()
                +"】","平台通用药品");
        return getBean(drugList, DrugListBean.class);

    }


}
