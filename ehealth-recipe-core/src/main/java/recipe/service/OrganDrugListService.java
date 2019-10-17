package recipe.service;

import com.google.common.collect.Lists;
import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.regulation.entity.RegulationDrugCategoryReq;
import com.ngari.his.regulation.service.IRegulationService;
import com.ngari.jgpt.zjs.service.IMinkeOrganService;
import com.ngari.patient.dto.OrganDTO;
import com.ngari.patient.service.BasicAPI;
import com.ngari.patient.service.OrganService;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.platform.sync.mode.DrugCategoryReq;
import com.ngari.platform.sync.service.IProvinceIndicatorsDateUpdateService;
import com.ngari.recipe.drug.model.DrugListAndOrganDrugListDTO;
import com.ngari.recipe.drug.model.DrugListBean;
import com.ngari.recipe.drug.model.OrganDrugListDTO;
import com.ngari.recipe.entity.DrugList;
import com.ngari.recipe.entity.DrugProducer;
import com.ngari.recipe.entity.OrganDrugList;
import ctd.persistence.DAOFactory;
import ctd.persistence.bean.QueryResult;
import ctd.persistence.exception.DAOException;
import ctd.spring.AppDomainContext;
import ctd.util.AppContextHolder;
import ctd.util.BeanUtils;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import ctd.util.event.GlobalEventExecFactory;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import recipe.constant.ErrorCode;
import recipe.dao.DrugListDAO;
import recipe.dao.DrugProducerDAO;
import recipe.dao.OrganDrugListDAO;
import recipe.dao.bean.DrugListAndOrganDrugList;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author zhongzx
 * @date 2016/5/27
 * 机构药品服务
 */
@RpcBean("organDrugListService")
public class OrganDrugListService {

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
            organDrugList.setProducerCode("");
            OrganDrugList saveOrganDrugList = organDrugListDAO.save(organDrugList);
            uploadOrganDrugListToJg(saveOrganDrugList);
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
                target.setLastModify(new Date());
                validateOrganDrugList(target);
                target = organDrugListDAO.update(target);
                uploadOrganDrugListToJg(target);
            }
            return ObjectCopyUtils.convert(target, OrganDrugListDTO.class);
        }
    }

    //上传机构药品到监管平台
    private void uploadOrganDrugListToJg(final OrganDrugList saveOrganDrugList) {
        //机构药品目录保存成功,异步上传到监管平台
        if (saveOrganDrugList != null) {
            //(异步的过程，不影响主流程)
            GlobalEventExecFactory.instance().getExecutor().submit(new Runnable() {
                @Override
                public void run() {
                    List<RegulationDrugCategoryReq> drugCategoryReqs = new ArrayList<>();
                    try{
                        /*IProvinceIndicatorsDateUpdateService hisService =
                                AppDomainContext.getBean("his.provinceDataUploadService", IProvinceIndicatorsDateUpdateService.class);*/
                        //使用最新的上传方式，兼容互联网环境和平台环境上传
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
            });
        }
    }

    //包装监管平台数据
    private RegulationDrugCategoryReq packingDrugCategoryReq(OrganDrugList organDrugList){
        OrganService organService = BasicAPI.getService(OrganService.class);
        IMinkeOrganService minkeOrganService = AppContextHolder.getBean("jgpt.minkeOrganService", IMinkeOrganService.class);
        OrganDTO organDTO = organService.getByOrganId(organDrugList.getOrganId());
        DrugListDAO drugListDAO = DAOFactory.getDAO(DrugListDAO.class);
        DrugList drugList = drugListDAO.getById(organDrugList.getDrugId());
        RegulationDrugCategoryReq drugCategoryReq = new RegulationDrugCategoryReq();
        String organId = minkeOrganService.getRegisterNumberByUnitId(organDTO.getMinkeUnitID());
        //这俩个字段 在his-server set进去了 平台模式这里可以不传
        drugCategoryReq.setUnitID(organDTO.getMinkeUnitID());
        drugCategoryReq.setOrganID(organId);
        drugCategoryReq.setOrganName(organDTO.getName());
        drugCategoryReq.setPlatDrugCode(organDrugList.getDrugId().toString());
        drugCategoryReq.setPlatDrugName(organDrugList.getDrugName());
        if (StringUtils.isNotEmpty(organDrugList.getOrganDrugCode())) {
            drugCategoryReq.setHospDrugCode(organDrugList.getOrganDrugCode());
        } else {
            drugCategoryReq.setHospDrugCode(organDrugList.getOrganDrugId().toString());
        }

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
