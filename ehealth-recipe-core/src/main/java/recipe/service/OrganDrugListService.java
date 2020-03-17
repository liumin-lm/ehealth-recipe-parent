package recipe.service;

import com.google.common.collect.Lists;
import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.regulation.entity.RegulationDrugCategoryReq;
import com.ngari.his.regulation.entity.RegulationNotifyDataReq;
import com.ngari.his.regulation.service.IRegulationService;
import com.ngari.jgpt.zjs.service.IMinkeOrganService;
import com.ngari.patient.dto.OrganDTO;
import com.ngari.patient.service.BasicAPI;
import com.ngari.patient.service.OrganService;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.drug.model.DrugListAndOrganDrugListDTO;
import com.ngari.recipe.drug.model.DrugListBean;
import com.ngari.recipe.drug.model.OrganDrugListDTO;
import com.ngari.recipe.drug.model.RegulationOrganDrugListBean;
import com.ngari.recipe.drug.service.IOrganDrugListService;
import com.ngari.recipe.entity.DrugList;
import com.ngari.recipe.entity.DrugProducer;
import com.ngari.recipe.entity.OrganDrugList;
import ctd.persistence.DAOFactory;
import ctd.persistence.bean.QueryResult;
import ctd.persistence.exception.DAOException;
import ctd.persistence.support.hibernate.HqlUtils;
import ctd.spring.AppDomainContext;
import ctd.util.AppContextHolder;
import ctd.util.BeanUtils;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import ctd.util.event.GlobalEventExecFactory;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import recipe.ApplicationUtils;
import recipe.constant.ErrorCode;
import recipe.dao.*;
import recipe.dao.bean.DrugListAndOrganDrugList;
import recipe.drugsenterprise.ByRemoteService;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author zhongzx
 * @date 2016/5/27
 * 机构药品服务
 */
@RpcBean("organDrugListService")
public class OrganDrugListService implements IOrganDrugListService {

    private static Logger logger = Logger.getLogger(OrganDrugListService.class);


    /**
     * 把药品添加到对应医院
     *
     * @param organDrugList
     * @return
     * @author zhongzx
     */
    @RpcService
    public boolean addDrugListForOrgan(OrganDrugList organDrugList) {
        OrganDrugListDAO dao = DAOFactory.getDAO(OrganDrugListDAO.class);
        logger.info("新增机构药品服务[addDrugListForOrgan]:" + JSONUtils.toString(organDrugList));
        if (null == organDrugList) {
            throw new DAOException(DAOException.VALUE_NEEDED, "organDrugList is null");
        }
        DrugListDAO drugListDAO = DAOFactory.getDAO(DrugListDAO.class);
        if (!drugListDAO.exist(organDrugList.getDrugId())) {
            throw new DAOException(DAOException.VALUE_NEEDED, "DrugList not exist");
        }

        //验证药品必要信息
        validate(organDrugList);
        dao.save(organDrugList);
        return true;
    }

    private void validate(OrganDrugList organDrugList) {
        if (null == organDrugList) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "药品信息不能为空");
        }
        if (StringUtils.isEmpty(organDrugList.getOrganDrugCode())) {
            throw new DAOException(DAOException.VALUE_NEEDED, "organDrugCode is needed");
        }
        /*if (StringUtils.isEmpty(organDrugList.getProducerCode())) {
            throw new DAOException(DAOException.VALUE_NEEDED, "producerCode is needed");
        }*/
        if (null == organDrugList.getDrugId()) {
            throw new DAOException(DAOException.VALUE_NEEDED, "drugId is needed");
        }
        if (null == organDrugList.getOrganId()) {
            throw new DAOException(DAOException.VALUE_NEEDED, "organId is needed");
        }
        if (null == organDrugList.getSalePrice()) {
            throw new DAOException(DAOException.VALUE_NEEDED, "salePrice is needed");
        }
        organDrugList.setCreateDt(new Date());
        if (null == organDrugList.getStatus()) {
            organDrugList.setStatus(1);
        }
        organDrugList.setLastModify(new Date());
    }

    private void validateOrganDrugList(OrganDrugList organDrugList) {
        if (null == organDrugList) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "药品信息不能为空");
        }
        if (StringUtils.isEmpty(organDrugList.getOrganDrugCode())) {
            throw new DAOException(DAOException.VALUE_NEEDED, "organDrugCode is needed");
        }
        /*if (StringUtils.isEmpty(organDrugList.getProducerCode())) {
            throw new DAOException(DAOException.VALUE_NEEDED, "producerCode is needed");
        }*/
        if (null == organDrugList.getDrugId()) {
            throw new DAOException(DAOException.VALUE_NEEDED, "drugId is needed");
        }
        if (null == organDrugList.getOrganId()) {
            throw new DAOException(DAOException.VALUE_NEEDED, "organId is needed");
        }
        if (null == organDrugList.getSalePrice()) {
            throw new DAOException(DAOException.VALUE_NEEDED, "salePrice is needed");
        }
    }

    /**
     * 批量导入邵逸夫药品（暂时用）
     *
     * @return
     * @author zhongzx
     */
    public void addDrugListForBatch() {
        DrugListDAO dao = DAOFactory.getDAO(DrugListDAO.class);
        OrganDrugListDAO organDrugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);
        DrugProducerDAO producerDAO = DAOFactory.getDAO(DrugProducerDAO.class);
        List<DrugList> list = dao.findAll();
        for (DrugList d : list) {
            OrganDrugList organDrug = new OrganDrugList();
            organDrug.setDrugId(d.getDrugId());
            organDrug.setOrganId(1);
            organDrug.setCreateDt(new Date());
            organDrug.setLastModify(new Date());
            //把药品产地转换成相应的医院的代码
            List<DrugProducer> producers = producerDAO.findByNameAndOrgan(d.getProducer(), 1);
            if (null != producers && producers.size() > 0) {
                organDrug.setProducerCode(producers.get(0).getCode());
            } else {
                organDrug.setProducerCode("");
            }
            organDrug.setStatus(1);
            organDrugListDAO.save(organDrug);
        }
    }

    /**
     * 删除药品在医院中的信息
     *
     * @param organDrugListId 入参药品参数
     * @param status 入参药品参数
     */
    @RpcService
    public void updateOrganDrugListStatusById(Integer organDrugListId, Integer status) {
        if (organDrugListId == null) {
            throw new DAOException(DAOException.VALUE_NEEDED, "organDrugId is required");
        }
        OrganDrugListDAO organDrugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);
        OrganDrugList organDrugList = organDrugListDAO.get(organDrugListId);
        organDrugList.setStatus(status);
        organDrugList.setLastModify(new Date());
        organDrugListDAO.update(organDrugList);
        IRegulationService iRegulationService = AppDomainContext.getBean("his.regulationService", IRegulationService.class);
        RegulationNotifyDataReq req = new RegulationNotifyDataReq();
        req.setBussType("drug");
        req.setNotifyTime(System.currentTimeMillis()-1000);
        req.setOrganId(organDrugList.getOrganId());
        iRegulationService.notifyData(organDrugList.getOrganId(),req);
    }

    /**
     * 更新药品在医院中的信息
     *
     * @param organDrugList 入参药品参数
     * @return 机构药品信息
     * @author zhongzx
     */
    @RpcService
    public OrganDrugListDTO updateOrganDrugList(OrganDrugList organDrugList) {
        OrganDrugListDAO organDrugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);
        DrugListDAO drugListDAO = DAOFactory.getDAO(DrugListDAO.class);
        if (null == organDrugList.getDrugId()) {
            throw new DAOException(DAOException.VALUE_NEEDED, "drugId is required");
        }
        updateValidate(organDrugList);
        //对药品名、药品代码、院内检索码作唯一性校验
        List<String> drugCodes = new ArrayList<>(1);
        drugCodes.add(organDrugList.getOrganDrugCode());
        if (StringUtils.isEmpty(organDrugList.getSaleName())) {
            organDrugList.setSaleName(organDrugList.getDrugName());
        } else {
            if (StringUtils.isNotEmpty(organDrugList.getDrugName()) && !organDrugList.getSaleName().equals(organDrugList.getDrugName())) {
                organDrugList.setSaleName(organDrugList.getSaleName() + " " + organDrugList.getDrugName());
            }
        }
        IRegulationService iRegulationService = AppDomainContext.getBean("his.regulationService", IRegulationService.class);
        if (organDrugList.getOrganDrugId() == null || organDrugList.getOrganDrugId() == 0) {
            logger.info("新增机构药品服务[updateOrganDrugList]:" + JSONUtils.toString(organDrugList));
            //说明为该机构新增机构药品
            if (!drugListDAO.exist(organDrugList.getDrugId())) {
                throw new DAOException(DAOException.VALUE_NEEDED, "DrugList not exist");
            }
            List<OrganDrugList> organDrugLists = organDrugListDAO.findByOrganIdAndDrugCodes(organDrugList.getOrganId(), drugCodes);
            if (organDrugLists != null && organDrugLists.size() > 0) {
                //说明不唯一了
                throw new DAOException(DAOException.VALUE_NEEDED, "该机构药品代码已经存在");
            }
            //验证药品必要信息
            validate(organDrugList);
            DrugList drugList = drugListDAO.getById(organDrugList.getDrugId());
            organDrugList.setOrganDrugId(null);
            organDrugList.setProducer(drugList.getProducer());
            if (StringUtils.isEmpty(organDrugList.getProducerCode())){
                organDrugList.setProducerCode("");
            }
            OrganDrugList saveOrganDrugList = organDrugListDAO.save(organDrugList);
            addOrganDrugListToBy(saveOrganDrugList);
            uploadOrganDrugListToJg(saveOrganDrugList);
            RegulationNotifyDataReq req = new RegulationNotifyDataReq();
            req.setBussType("drug");
            req.setNotifyTime(System.currentTimeMillis()-1000);
            req.setOrganId(saveOrganDrugList.getOrganId());
            iRegulationService.notifyData(saveOrganDrugList.getOrganId(),req);
            return ObjectCopyUtils.convert(saveOrganDrugList, OrganDrugListDTO.class);
        } else {
            logger.info("修改机构药品服务[updateOrganDrugList]:" + JSONUtils.toString(organDrugList));
            OrganDrugList target = organDrugListDAO.get(organDrugList.getOrganDrugId());
            if (null == target) {
                throw new DAOException(ErrorCode.SERVICE_ERROR, "此药在该医院药品列表中不存在");
            } else {
                //说明为更新机构药品目录,需要校验是否变更编号
                if (!organDrugList.getOrganDrugCode().equals(target.getOrganDrugCode())) {
                    //对药品名、药品代码、院内检索码作唯一性校验
                    List<OrganDrugList> organDrugLists = organDrugListDAO.findByOrganIdAndDrugCodes(organDrugList.getOrganId(), drugCodes);
                    if (organDrugLists != null && organDrugLists.size() > 0) {
                        //说明不唯一了
                        throw new DAOException(DAOException.VALUE_NEEDED, "该机构药品代码已经存在");
                    }
                }
                BeanUtils.map(organDrugList, target);
                if (organDrugList.getUseDose() == null) {
                    target.setUseDose(null);
                }
                if (organDrugList.getRecommendedUseDose() == null) {
                    target.setRecommendedUseDose(null);
                }
                target.setLastModify(new Date());
                validateOrganDrugList(target);
                target = organDrugListDAO.update(target);
                uploadOrganDrugListToJg(target);
            }
            RegulationNotifyDataReq req = new RegulationNotifyDataReq();
            req.setBussType("drug");
            req.setNotifyTime(System.currentTimeMillis()-1000);
            req.setOrganId(target.getOrganId());
            iRegulationService.notifyData(target.getOrganId(),req);
            return ObjectCopyUtils.convert(target, OrganDrugListDTO.class);
        }
    }

    //上海六院的新增药品信息同步到百洋
    private void addOrganDrugListToBy(OrganDrugList organDrugList){
        try{
            if (organDrugList != null) {
                //(异步的过程，不影响主流程)
                GlobalEventExecFactory.instance().getExecutor().submit(new Runnable() {
                    @Override
                    public void run() {
                        RecipeParameterDao recipeParameterDao = DAOFactory.getDAO(RecipeParameterDao.class);
                        String organCode = recipeParameterDao.getByName("sh_baiyang_druglist");
                        if (StringUtils.isNotEmpty(organCode)) {
                            if (Integer.parseInt(organCode) == organDrugList.getOrganId()) {
                                logger.info("同步药品数据到百洋药企：" + JSONUtils.toString(organDrugList));
                                //表示是上海六院的新增药品，需要同步到百洋药企
                                ByRemoteService byRemoteService = ApplicationUtils.getRecipeService(ByRemoteService.class);
                                byRemoteService.corresPondingHospDrugByOrganDrugListHttpRequest(organDrugList);
                            }
                        }
                    }
                });
            }
        }catch(Exception e){
            logger.info("addOrganDrugListToBy 同步到百洋药企药品数据出错："+ e.getMessage());
        }
    }

    /**
     *  上传机构药品到监管平台
     */
    private void uploadOrganDrugListToJg(final OrganDrugList saveOrganDrugList) {
        //机构药品目录保存成功,异步上传到监管平台
        if (saveOrganDrugList != null) {
            //(异步的过程，不影响主流程)
            GlobalEventExecFactory.instance().getExecutor().submit(new Runnable() {
                @Override
                public void run() {
                    uploadDrugToRegulation(saveOrganDrugList);
                }
            });
        }
    }

    /**
     * 两个地方需要上传---1.运营平台添加机构药品 2.药品工具提交机构药品数据
     * @param saveOrganDrugList
     */
    public void uploadDrugToRegulation(OrganDrugList saveOrganDrugList){
        List<RegulationDrugCategoryReq> drugCategoryReqs = new ArrayList<>();
        try{
            //使用最新的上传方式，兼容互联网环境和平台环境上传 旧his.provinceDataUploadService
            IRegulationService hisService =
                    AppDomainContext.getBean("his.regulationService", IRegulationService.class);
            RegulationDrugCategoryReq drugCategoryReq = packingDrugCategoryReq(saveOrganDrugList);
            drugCategoryReqs.add(drugCategoryReq);
            HisResponseTO hisResponseTO = hisService.uploadDrugCatalogue(saveOrganDrugList.getOrganId(),drugCategoryReqs);
            logger.info("hisResponseTO parames:" + JSONUtils.toString(hisResponseTO));
        } catch (Exception e) {
            logger.info("上传药品到监管平台失败,{"+ JSONUtils.toString(drugCategoryReqs)+"},{"+e.getMessage()+"}.");
        }
    }

    /**
     * 包装监管平台数据
     * @param organDrugList
     * @return
     */
    private RegulationDrugCategoryReq packingDrugCategoryReq(OrganDrugList organDrugList){
        OrganService organService = BasicAPI.getService(OrganService.class);
        IMinkeOrganService minkeOrganService = AppContextHolder.getBean("jgpt.minkeOrganService", IMinkeOrganService.class);
        OrganDTO organDTO = organService.getByOrganId(organDrugList.getOrganId());
        DrugListDAO drugListDAO = DAOFactory.getDAO(DrugListDAO.class);
        CompareDrugDAO compareDrugDAO = DAOFactory.getDAO(CompareDrugDAO.class);
        DrugList drugList = drugListDAO.getById(organDrugList.getDrugId());
        RegulationDrugCategoryReq drugCategoryReq = new RegulationDrugCategoryReq();
        String organId = minkeOrganService.getRegisterNumberByUnitId(organDTO.getMinkeUnitID());
        //这俩个字段 在his-server set进去了 平台模式这里可以不传
        drugCategoryReq.setUnitID(organDTO.getMinkeUnitID());
        drugCategoryReq.setOrganID(organId);
        drugCategoryReq.setOrganName(organDTO.getName());
        //如果存在 转换省平台药品id
        Integer targetDrugId = compareDrugDAO.findTargetDrugIdByOriginalDrugId(organDrugList.getDrugId());
        if (targetDrugId != null){
            drugCategoryReq.setPlatDrugCode(targetDrugId.toString());
        }else {
            drugCategoryReq.setPlatDrugCode(organDrugList.getDrugId().toString());
        }
        drugCategoryReq.setPlatDrugName(organDrugList.getDrugName());
        if (StringUtils.isNotEmpty(organDrugList.getOrganDrugCode())) {
            drugCategoryReq.setHospDrugCode(organDrugList.getOrganDrugCode());
        } else {
            drugCategoryReq.setHospDrugCode(organDrugList.getOrganDrugId().toString());
        }
        drugCategoryReq.setDrugPrice(organDrugList.getSalePrice());
        drugCategoryReq.setHospDrugName(organDrugList.getDrugName());
        drugCategoryReq.setHospTradeName(organDrugList.getSaleName());
        if (StringUtils.isNotEmpty(organDrugList.getDrugSpec())) {
            drugCategoryReq.setHospDrugPacking(organDrugList.getDrugSpec());
        } else {
            if (StringUtils.isNotEmpty(drugList.getDrugSpec())) {
                drugCategoryReq.setHospDrugPacking(drugList.getDrugSpec());
            } else {
                drugCategoryReq.setHospDrugPacking("/");
            }
        }
        if (StringUtils.isNotEmpty(organDrugList.getProducer())) {
            drugCategoryReq.setHospDrugManuf(organDrugList.getProducer());
        } else {
            drugCategoryReq.setHospDrugManuf(drugList.getProducer());
        }

        drugCategoryReq.setUseFlag(organDrugList.getStatus()+"");
        drugCategoryReq.setDrugClass(drugList.getDrugClass());
        drugCategoryReq.setUpdateTime(new Date());
        drugCategoryReq.setCreateTime(new Date());
        return drugCategoryReq;
    }

    private void updateValidate(OrganDrugList organDrugList) {
        if (null == organDrugList) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "药品信息不能为空");
        }
        if (StringUtils.isEmpty(organDrugList.getOrganDrugCode())) {
            throw new DAOException(DAOException.VALUE_NEEDED, "organDrugCode is needed");
        }
        if (StringUtils.isEmpty(organDrugList.getDrugName())) {
            throw new DAOException(DAOException.VALUE_NEEDED, "drugName is needed");
        }
        if (StringUtils.isEmpty(organDrugList.getSaleName())) {
            throw new DAOException(DAOException.VALUE_NEEDED, "saleName is needed");
        }
        if (organDrugList.getPack() == null || organDrugList.getPack() <= 0) {
            throw new DAOException(DAOException.VALUE_NEEDED, "pack is needed or not is 0");
        }
        if (null == organDrugList.getOrganId()) {
            throw new DAOException(DAOException.VALUE_NEEDED, "organId is needed");
        }
        if (null == organDrugList.getSalePrice()) {
            throw new DAOException(DAOException.VALUE_NEEDED, "salePrice is needed");
        }
    }

    /**
     * 机构药品查询
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
    public QueryResult<DrugListAndOrganDrugListDTO> queryOrganDrugListByOrganIdAndKeyword(final Integer organId,
                                                                                          final String drugClass,
                                                                                          final String keyword, final Integer status,
                                                                                          final int start, final int limit) {
        OrganDrugListDAO organDrugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);
        QueryResult result = organDrugListDAO.queryOrganDrugListByOrganIdAndKeyword(organId, drugClass, keyword, status, start, limit);
        result.setItems(covertData(result.getItems()));
        return result;
    }

    /**
     * * 运营平台（权限改造）
     *
     * @param organId
     * @param drugClass
     * @param keyword
     * @param status
     * @param start
     * @param limit
     * @return
     */
    @RpcService
    public QueryResult<DrugListAndOrganDrugListDTO> queryOrganDrugListByOrganIdAndKeywordForOp(final Integer organId,
                                                                                               final String drugClass,
                                                                                               final String keyword, final Integer status,
                                                                                               final int start, final int limit) {
        OrganDrugListDAO organDrugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);
        QueryResult result = organDrugListDAO.queryOrganDrugListByOrganIdAndKeyword(organId, drugClass, keyword, status, start, limit);
        result.setItems(covertData(result.getItems()));
        return result;
    }

    @Override
    public List<RegulationOrganDrugListBean> queryRegulationDrugSHET(Map<String, Object> params) {
        List drugList = HqlUtils.execHqlFindList("select a.organDrugId,a.organId,a.drugName,a.status,a.medicalDrugFormCode,a.drugForm," +
                " a.producer,a.baseDrug,b.approvalNumber,b.DrugClass" +
                " from OrganDrugList a, DrugList b where a.drugId = b.drugId and lastModify>=:startDate and lastModify<:endDate and a.OrganID IN :organIds", params);
        List<RegulationOrganDrugListBean> result = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(drugList)){
            logger.info("机构药品信息数据总数：" + drugList.size());
            RegulationOrganDrugListBean bean;
            OrganService organService = AppContextHolder.getBean("basic.organService", OrganService.class);
            for (Object o : drugList) {
                bean = new RegulationOrganDrugListBean();
                Object[] obj =  (Object[]) o;
                Integer organId =(Integer) obj[1];
                OrganDTO organDTO = organService.getByOrganId(organId);
                bean.setOrganID(organId == null ? "" : String.valueOf(organId));
                bean.setOrganName(organDTO == null ? "" : organDTO.getName());
                bean.setHospDrugName(obj[2] == null ? "" : String.valueOf(obj[2]));  //医院药品通用名:drugName--上海儿童-药品注册通用名
                String status = obj[3] == null ? "" : String.valueOf(obj[3]);
                bean.setUseFlag(StringUtils.equals("0", status) ? "1" : "0");  //医院药品包装规格:status--上海儿童-使用标志
                bean.setMedicalDrugFormCode(obj[4] == null ? "" : String.valueOf(obj[4]));  //医保剂型编码:medicalDrugFormCode--上海儿童-剂型代码
                bean.setDrugForm(obj[5] == null ? "" : String.valueOf(obj[5]));  //剂型:drugForm--上海儿童-剂型名称
                String producer = obj[6] == null ? "" : String.valueOf(obj[6]);
                bean.setBaseDrug(obj[7] == null ? "" : String.valueOf(obj[7]));  //是否基药:baseDrug--上海儿童-基药标识
                bean.setApprovalNumber(obj[8] == null ? "" : String.valueOf(obj[8]));  //批准文号:approvalNumber--上海儿童-批准文号
                String drugClass = obj[9] == null ? "" : String.valueOf(obj[9]);
                bean.setHospitalPreparation(StringUtils.indexOf(producer, "新华医院调拨") == -1 ? "0" : "1");  //上海儿童-院内制剂标志
                bean.setKssFlag(drugClass.startsWith("0101") ? "1" : "0");
                bean.setDmjfFlag(drugClass.startsWith("02") || drugClass.startsWith("04") ? "1" : "0");
                bean.setModifyFlag("1");

                result.add(bean);
            }
        }
        return result;
    }

    private List<DrugListAndOrganDrugListDTO> covertData(List<DrugListAndOrganDrugList> dbList) {
        List<DrugListAndOrganDrugListDTO> newList = Lists.newArrayList();
        DrugListAndOrganDrugListDTO backDTO;
        for (DrugListAndOrganDrugList daod : dbList) {
            backDTO = new DrugListAndOrganDrugListDTO();
            backDTO.setDrugList(ObjectCopyUtils.convert(daod.getDrugList(), DrugListBean.class));
            backDTO.setOrganDrugList(ObjectCopyUtils.convert(daod.getOrganDrugList(), OrganDrugListDTO.class));
            newList.add(backDTO);
        }

        return newList;
    }

}
