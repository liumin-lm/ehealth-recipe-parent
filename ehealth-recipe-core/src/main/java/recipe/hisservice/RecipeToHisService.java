package recipe.hisservice;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.recipe.mode.*;
import com.ngari.his.recipe.service.IRecipeHisService;
import com.ngari.patient.dto.EmploymentDTO;
import com.ngari.patient.service.BasicAPI;
import com.ngari.patient.service.EmploymentService;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.entity.*;
import com.ngari.recipe.hisprescription.model.HosPatientDTO;
import com.ngari.recipe.hisprescription.model.HosPatientRecipeDTO;
import com.ngari.recipe.hisprescription.model.HosRecipeDTO;
import com.ngari.recipe.hisprescription.model.HosRecipeDetailDTO;
import ctd.persistence.DAOFactory;
import ctd.persistence.exception.DAOException;
import ctd.spring.AppDomainContext;
import ctd.util.JSONUtils;
import eh.recipeaudit.api.IRecipeAuditService;
import eh.recipeaudit.util.RecipeAuditAPI;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ObjectUtils;
import recipe.constant.ErrorCode;
import recipe.constant.RecipeStatusConstant;
import recipe.dao.OrganDrugListDAO;
import recipe.dao.PharmacyTcmDAO;
import recipe.dao.RecipeDAO;
import recipe.dao.SyncDrugExcDAO;
import recipe.service.HisCallBackService;
import recipe.service.RecipeLogService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * company: ngarihealth
 * @author: 0184/yu_yun
 * @date:2017/9/12.
 */
public class RecipeToHisService {

    /**
     * LOGGER
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(RecipeToHisService.class);

    @Autowired
    private PharmacyTcmDAO pharmacyTcmDAO;

    @Autowired
    private SyncDrugExcDAO syncDrugExcDAO;

    public void recipeSend(RecipeSendRequestTO request) {
        IRecipeHisService hisService = AppDomainContext.getBean("his.iRecipeHisService", IRecipeHisService.class);
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        Integer recipeId = Integer.valueOf(request.getRecipeID());
        LOGGER.info("recipeSend recipeId={}, request={}", recipeId, JSONUtils.toString(request));

        //放在异步发送前就更新状态
//        recipeDAO.updateRecipeInfoByRecipeId(recipeId, RecipeStatusConstant.CHECKING_HOS, null);

        try {
            hisService.recipeSend(request);
            LOGGER.info("recipeSend 调用前置机处方写入服务成功! recipeId=" + request.getRecipeID());
        } catch (Exception e) {
            LOGGER.error("recipeSend HIS接口调用失败. request={}", JSONUtils.toString(request), e);
            Recipe recipe= recipeDAO.get(recipeId);
            if(recipe != null && recipe.getStatus() != null && recipe.getStatus() == RecipeStatusConstant.CHECKING_HOS){
                //失败发送系统消息
                recipeDAO.updateStatusByRecipeIdAndStatus(recipeId, RecipeStatusConstant.HIS_FAIL, RecipeStatusConstant.CHECKING_HOS);
                //日志记录
                RecipeLogService.saveRecipeLog(recipeId, RecipeStatusConstant.CHECKING_HOS,
                    RecipeStatusConstant.HIS_FAIL, "his写入失败，调用前置机处方写入服务失败");
                LOGGER.error("recipeSend recipeId={}, 调用BASE 处方写入服务异常!，更改处方状态", recipeId);
            } else{
                //非医院确认中的状态不做更改（避免已经回调成功了，这再报超时异常）
                if(recipe != null){
                    //日志记录
                    RecipeLogService.saveRecipeLog(recipeId, recipe.getStatus(),
                        recipe.getStatus(), "his写入失败，调用前置机处方写入服务失败,状态不变");
                    LOGGER.error("recipeSend recipeId={}, 调用BASE 处方写入服务异常! status={}", recipeId, recipe.getStatus());
                }
            }

        }
    }

    public Integer listSingleQuery(List<RecipeListQueryReqTO> request) {
        IRecipeHisService hisService = AppDomainContext.getBean("his.iRecipeHisService", IRecipeHisService.class);
        LOGGER.info("listSingleQuery request={}", JSONUtils.toString(request));
        try {
            RecipeListQueryResTO response = hisService.listQuery(request);
            LOGGER.info("listSingleQuery response={}", JSONUtils.toString(response));
            Integer busStatus = null;
            //有可能前置机没实现这个接口 返回null 保证流程走通
            if (null == response || CollectionUtils.isEmpty(response.getData())) {
                return RecipeStatusConstant.CHECK_PASS;
            }
            if (null == response.getMsgCode()) {
                return busStatus;
            }

            List<QueryRepTO> list = response.getData();
            if (StringUtils.isNotEmpty(response.getOrganID()) && CollectionUtils.isNotEmpty(list)) {
                List<String> payList = new ArrayList<>();
                List<String> finishList = new ArrayList<>();

                QueryRepTO rep = list.get(0);
                if (null != rep) {
                    Integer organId = Integer.valueOf(response.getOrganID());
                    Integer isPay = StringUtils.isEmpty(rep.getIsPay()) ? Integer.valueOf(0) : Integer.valueOf(rep.getIsPay());
                    Integer recipeStatus = StringUtils.isEmpty(rep.getRecipeStatus())  ? Integer.valueOf(0) : Integer.valueOf(rep.getRecipeStatus());
                    Integer phStatus = StringUtils.isEmpty(rep.getPhStatus()) ? Integer.valueOf(0) : Integer.valueOf(rep.getPhStatus());
                    if (recipeStatus == 1) {
                        busStatus = RecipeStatusConstant.CHECK_PASS;
                        //有效的处方单已支付 未发药 为已支付状态
                        if (isPay == 1 && phStatus == 0) {
                            busStatus = RecipeStatusConstant.HAVE_PAY;
                            payList.add(rep.getRecipeNo());
                            HisCallBackService.havePayRecipesFromHis(payList, organId);
                        }
                        //有效的处方单已支付 已发药 为已完成状态
                        if (isPay == 1 && phStatus == 1) {
                            busStatus = RecipeStatusConstant.FINISH;
                            finishList.add(rep.getRecipeNo());
                            HisCallBackService.finishRecipesFromHis(finishList, organId);
                        }
                    }
                }
            }
            return busStatus;
        } catch (Exception e) {
            LOGGER.error("listSingleQuery error ", e);
        }
        return null;
    }


    public void listQuery(List<RecipeListQueryReqTO> request) {
        IRecipeHisService hisService = AppDomainContext.getBean("his.iRecipeHisService", IRecipeHisService.class);
        LOGGER.info("listQuery request={}", JSONUtils.toString(request));
        try {
            RecipeListQueryResTO response = hisService.listQuery(request);
            EmploymentService employmentService = BasicAPI.getService(EmploymentService.class);
            IRecipeAuditService recipeAuditService= RecipeAuditAPI.getService(IRecipeAuditService.class,"recipeAuditServiceImpl");
            RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
            LOGGER.info("listQuery response={}", JSONUtils.toString(response));
            if (null == response || null == response.getMsgCode() || CollectionUtils.isEmpty(response.getData())) {
                return;
            }
            List<QueryRepTO> list = response.getData();
            List<String> payList = new ArrayList<>();
            List<String> finishList = new ArrayList<>();
            Integer organId = Integer.valueOf(response.getOrganID());
            Recipe recipe;
            Map<String, EmploymentDTO> employmentMap = Maps.newHashMap();
            EmploymentDTO employmentDTO;
            Map<String,Object> checkParam = Maps.newHashMap();
            if (list.size() > 0) {
                for (QueryRepTO rep : list) {
                    Integer isPay = Integer.valueOf(rep.getIsPay());
                    Integer recipeStatus = Integer.valueOf(rep.getRecipeStatus());
                    Integer phStatus = Integer.valueOf(rep.getPhStatus());
                    if (recipeStatus == 1) {
                        //有效的处方单已支付 未发药 为已支付状态
                        if (isPay == 1 && phStatus == 0) {
                            payList.add(rep.getRecipeNo());
                        }
                        //有效的处方单已支付 已发药 为已完成状态
                        if (isPay == 1 && phStatus == 1) {
                            finishList.add(rep.getRecipeNo());
                        }
                        //连云港二院处理
                        if (StringUtils.isNotEmpty(rep.getAuditDoctorNo())){
                            recipe = recipeDAO.getByRecipeCodeAndClinicOrgan(rep.getRecipeNo(), organId);
                            if (recipe != null && recipe.getChecker() == null){
                                //审核医生信息处理
                                employmentDTO = employmentMap.get(rep.getRecipeNo()+organId);
                                if (null == employmentDTO) {
                                    employmentDTO = employmentService.getByJobNumberAndOrganId(
                                            rep.getAuditDoctorNo(), organId);
                                    employmentMap.put(rep.getRecipeNo()+organId, employmentDTO);
                                }
                                if (null != employmentDTO) {
                                    recipe.setChecker(employmentDTO.getDoctorId());
                                    recipeDAO.update(recipe);
                                    //生成药师电子签名
                                    checkParam.put("recipeId",recipe.getRecipeId());
                                    //审核成功
                                    checkParam.put("result",1);
                                    checkParam.put("checkOrgan",organId);
                                    checkParam.put("checker",recipe.getChecker());
                                    //是否是线下药师审核标记
                                    checkParam.put("hosAuditFlag",1);
                                    recipeAuditService.saveCheckResult(checkParam);
                                    LOGGER.info("线下审方生成线上药师电子签名--end");
                                } else {
                                    LOGGER.warn("listQuery 审核医生[{}]在平台没有执业点", rep.getAuditDoctorName());
                                }
                            }else {
                                LOGGER.warn("listQuery 查询不到未审核处方单,organId={},recipeCode={}",organId,rep.getRecipeNo());
                            }
                        }
                    }
                }
            } else{
                LOGGER.warn("listQuery MsgCode存在查询不到未审核处方单,organId={},recipeCode={}",organId);
            }
            if (CollectionUtils.isNotEmpty(payList)) {
                HisCallBackService.havePayRecipesFromHis(payList, organId);
            }

            if (CollectionUtils.isNotEmpty(finishList)) {
                HisCallBackService.finishRecipesFromHis(finishList, organId);
            }

        } catch (Exception e) {
            LOGGER.error("listQuery error ", e);
        }
    }


    public RecipeRefundResTO recipeRefund(RecipeRefundReqTO request) {
        IRecipeHisService hisService = AppDomainContext.getBean("his.iRecipeHisService", IRecipeHisService.class);
        LOGGER.info("recipeRefund request={}", JSONUtils.toString(request));
        RecipeRefundResTO response = null;
        try {
            response = hisService.recipeRefund(request);
            LOGGER.info("recipeRefund response={}", JSONUtils.toString(response));
        } catch (Exception e) {
            LOGGER.error("recipeRefund error ", e);
        }
        return response;
    }

    @Deprecated
    public PayNotifyResTO payNotify(PayNotifyReqTO request) {
        IRecipeHisService hisService = AppDomainContext.getBean("his.iRecipeHisService", IRecipeHisService.class);
        LOGGER.info("payNotify request={}", JSONUtils.toString(request));
        try {
            PayNotifyResTO response = null;
            if("1".equals(request.getIsMedicalSettle())){
                response = hisService.recipeMedicalSettle(request);
            } else{
                response = hisService.payNotify(request);
            }
            LOGGER.info("payNotify response={}", JSONUtils.toString(response));
            return response;
        } catch (Exception e) {
            LOGGER.error("payNotify error ", e);
        }
        return null;
    }

    /**
     * 查询药品在医院里的信息
     *
     * @param
     * @return
     */
    public List<DrugInfoTO> queryDrugInfo(List<DrugInfoTO> drugInfoList, int organId) {
        IRecipeHisService hisService = AppDomainContext.getBean("his.iRecipeHisService", IRecipeHisService.class);
        DrugInfoRequestTO request = new DrugInfoRequestTO();
        request.setOrganId(organId);
        if (CollectionUtils.isEmpty(drugInfoList)) {
            //查询全部药品信息，返回的是医院所有有效的药品信息
            request.setData(Lists.newArrayList());
            request.setDrcode(Lists.newArrayList());
        } else {
            //查询限定范围内容的药品数据，返回的是该医院 无效的药品信息
            request.setData(drugInfoList);
            List<String> drugIdList = drugInfoList.stream().map(DrugInfoTO::getDrcode).collect(Collectors.toList());
            request.setDrcode(drugIdList);
        }
        LOGGER.info("queryDrugInfo request={}", JSONUtils.toString(request));

        try {
            DrugInfoResponseTO response = hisService.queryDrugInfo(request);
            LOGGER.info("queryDrugInfo response={}", JSONUtils.toString(response));
            if (null != response && Integer.valueOf(200).equals(response.getMsgCode())) {
                return (null != response.getData()) ? response.getData() : new ArrayList<>();
            }else if (ObjectUtils.isEmpty(response)){
                if (drugInfoList.size()>0 && drugInfoList!=null){
                    syncDrugExcDAO.save(convertSyncExc(drugInfoList.get(0),organId));
                }
            }
        } catch (Exception e) {
            if (drugInfoList.size()>0 && drugInfoList!=null){
                syncDrugExcDAO.save(convertSyncExc(drugInfoList.get(0),organId));
            }
            LOGGER.error("queryDrugInfo error ", e);
        }
        return null;
    }

    public SyncDrugExc convertSyncExc(DrugInfoTO drug , Integer organId ){
        if (null == drug) {
            throw new DAOException(DAOException.VALUE_NEEDED, "定时异常数据转换对象为空!");
        }
        SyncDrugExc syncDrugExc=new SyncDrugExc();
        //获取金额
        if (StringUtils.isNotEmpty(drug.getDrugPrice())) {
            BigDecimal drugPrice = new BigDecimal(drug.getDrugPrice());
            syncDrugExc.setSalePrice(drugPrice);
        }
        //药品规格
        if (StringUtils.isNotEmpty(drug.getDrmodel())) {
            syncDrugExc.setDrugSpec(drug.getDrmodel());
        }
        //转换系数
        if (StringUtils.isNotEmpty(drug.getPack())) {
            syncDrugExc.setPack(Integer.valueOf(drug.getPack()));
        }
        //生产厂家
        if (StringUtils.isNotEmpty(drug.getProducer())) {
            syncDrugExc.setProducer(drug.getProducer());
        }
        //商品名称
        if (StringUtils.isNotEmpty(drug.getTradename())) {
            syncDrugExc.setSaleName(drug.getTradename());
        }
        //通用名
        if (StringUtils.isNotEmpty(drug.getDrname())) {
            syncDrugExc.setDrugName(drug.getDrname());
        }
        //药品包装单位
        if (StringUtils.isNotEmpty(drug.getPackUnit())) {
            syncDrugExc.setUnit(drug.getPackUnit());
        }
        //实际单次剂量（规格单位）
        if (!ObjectUtils.isEmpty(drug.getUseDose())) {
            syncDrugExc.setUseDose(drug.getUseDose());
        }

        //单次剂量单位（规格单位）
        if (!ObjectUtils.isEmpty(drug.getUseDoseUnit())) {
            syncDrugExc.setUseDoseUnit(drug.getUseDoseUnit());
        }

        //使用状态 0 无效 1 有效
        if (!ObjectUtils.isEmpty(drug.getStatus())) {
            syncDrugExc.setStatus(drug.getStatus());
        }
        //生产厂家代码
        if (!ObjectUtils.isEmpty(drug.getProducerCode())) {
            syncDrugExc.setProducerCode(drug.getProducerCode());
        }
        //外带药标志 1:外带药
        if (!ObjectUtils.isEmpty(drug.getTakeMedicine())) {
            syncDrugExc.setTakeMedicine(drug.getTakeMedicine());
        }
        //药房
        if (!ObjectUtils.isEmpty(drug.getPharmacyCode())) {
            String pharmacyCode = drug.getPharmacyCode();
            PharmacyTcm byPharmacyAndOrganId = pharmacyTcmDAO.getByPharmacyAndOrganId(pharmacyCode, organId);
            if (byPharmacyAndOrganId != null) {
                syncDrugExc.setPharmacy(byPharmacyAndOrganId.getPharmacyId().toString());
            }
        }
        //医院药房名字
        if (!ObjectUtils.isEmpty(drug.getPharmacy())) {
            syncDrugExc.setPharmacyName(drug.getPharmacy());
        }
        //剂型
        if (!ObjectUtils.isEmpty(drug.getDrugForm())) {
            syncDrugExc.setDrugForm(drug.getDrugForm());
        }
        //是否基药
        if (!ObjectUtils.isEmpty(drug.getBaseDrug())) {
            syncDrugExc.setBaseDrug(drug.getBaseDrug());
        }
        //批准文号
        if (!ObjectUtils.isEmpty(drug.getLicenseNumber())) {
            syncDrugExc.setLicenseNumber(drug.getLicenseNumber());
        }

        syncDrugExc.setExcType("未同步更新");
        syncDrugExc.setSyncType(2);
        return syncDrugExc;
    }

    public Boolean drugTakeChange(DrugTakeChangeReqTO request) {
        IRecipeHisService hisService = AppDomainContext.getBean("his.iRecipeHisService", IRecipeHisService.class);
        LOGGER.info("drugTakeChange request={}", JSONUtils.toString(request));
        Boolean response = false;
        try {
            response = hisService.drugTakeChange(request);
            LOGGER.info("drugTakeChange response={}", JSONUtils.toString(response));
        } catch (Exception e) {
            LOGGER.error("drugTakeChange error ", e);
        }
        return response;
    }

    public Boolean recipeUpdate(RecipeStatusUpdateReqTO request) {
        IRecipeHisService hisService = AppDomainContext.getBean("his.iRecipeHisService", IRecipeHisService.class);
        LOGGER.info("recipeUpdate request={}", JSONUtils.toString(request));
        Boolean response = false;
        try {
            response = hisService.recipeUpdate(request);
            LOGGER.info("recipeUpdate response={}", JSONUtils.toString(response));
        } catch (Exception e) {
            LOGGER.error("recipeUpdate error ", e);
        }
        return response;
    }


    public DrugInfoResponseTO scanDrugStock(List<Recipedetail> detailList, int organId) {
        if (CollectionUtils.isEmpty(detailList)) {
            return null;
        }
        OrganDrugListDAO drugDao = DAOFactory.getDAO(OrganDrugListDAO.class);
        PharmacyTcmDAO pharmacyTcmDAO = DAOFactory.getDAO(PharmacyTcmDAO.class);
        IRecipeHisService hisService = AppDomainContext.getBean("his.iRecipeHisService", IRecipeHisService.class);

        DrugInfoRequestTO request = new DrugInfoRequestTO();
        request.setOrganId(organId);
        List<Integer> drugIdList = detailList.stream().map(Recipedetail::getDrugId).collect(Collectors.toList());

        List<OrganDrugList> organDrugList = drugDao.findByOrganIdAndDrugIds(organId, drugIdList);
        Map<String, List<OrganDrugList>> drugIdAndProduce =
                organDrugList.stream().collect(Collectors.groupingBy(OrganDrugList::getOrganDrugCode));

        List<DrugInfoTO> data = new ArrayList<>(detailList.size());
        detailList.forEach(a -> {
            DrugInfoTO drugInfo = new DrugInfoTO(a.getOrganDrugCode());
            drugInfo.setPack(a.getPack().toString());
            drugInfo.setPackUnit(a.getDrugUnit());
            drugInfo.setUseTotalDose(a.getUseTotalDose());
            List<OrganDrugList> organDrugs = drugIdAndProduce.get(a.getOrganDrugCode());
            if (CollectionUtils.isNotEmpty(organDrugs)) {
                Map<Integer, String> producerCodeMap = organDrugs.stream().collect(Collectors.toMap(OrganDrugList::getDrugId, OrganDrugList::getProducerCode));
                String producerCode = producerCodeMap.get(a.getDrugId());
                if (StringUtils.isNotEmpty(producerCode)) {
                    drugInfo.setManfcode(producerCode);
                }
            }
            //药房
            if (a.getPharmacyId() != null){
                PharmacyTcm pharmacyTcm = pharmacyTcmDAO.get(a.getPharmacyId());
                if (pharmacyTcm != null){
                    drugInfo.setPharmacyCode(pharmacyTcm.getPharmacyCode());
                    drugInfo.setPharmacy(pharmacyTcm.getPharmacyName());
                }
            }
            data.add(drugInfo);
        });
        request.setData(data);

        DrugInfoResponseTO response;
        LOGGER.info("scanDrugStock request={}", JSONUtils.toString(request));
        try {
            response = hisService.scanDrugStock(request);
            LOGGER.info("scanDrugStock response={}", JSONUtils.toString(response));
        } catch (Exception e) {
            LOGGER.error("scanDrugStock error ", e);
            //抛异常流程不应该继续下去
            response = new DrugInfoResponseTO();
            response.setMsgCode(1);
        }
        return response;
    }

    public RecipeQueryResTO recipeQuery(RecipeQueryReqTO request) {
        IRecipeHisService hisService = AppDomainContext.getBean("his.iRecipeHisService", IRecipeHisService.class);
        LOGGER.info("recipeQuery request={}", JSONUtils.toString(request));
        RecipeQueryResTO response = null;
        try {
            response = hisService.recipeQuery(request);
            LOGGER.info("recipeQuery response={}", JSONUtils.toString(response));
        } catch (Exception e) {
            LOGGER.error("recipeQuery error ", e);
        }
        return response;
    }

    public DetailQueryResTO detailQuery(DetailQueryReqTO request) {
        IRecipeHisService hisService = AppDomainContext.getBean("his.iRecipeHisService", IRecipeHisService.class);
        LOGGER.info("detailQuery request={}", JSONUtils.toString(request));
        DetailQueryResTO response = null;
        try {
            response = hisService.detailQuery(request);
            LOGGER.info("detailQuery response={}", JSONUtils.toString(response));
        } catch (Exception e) {
            LOGGER.error("detailQuery error ", e);
        }
        return response;
    }

    public HisResponseTO recipeAudit(RecipeAuditReqTO request){
        IRecipeHisService hisService = AppDomainContext.getBean("his.iRecipeHisService", IRecipeHisService.class);
        return hisService.recipeAudit(request);
    }

    /**
     * 武昌模块 推送his电子病历
     * @param request
     * @return
     */
    public HisResponseTO<DocIndexToHisResTO> docIndexToHis(DocIndexToHisReqTO request){
        IRecipeHisService hisService = AppDomainContext.getBean("his.iRecipeHisService", IRecipeHisService.class);
        LOGGER.info("docIndexToHis request={}", JSONUtils.toString(request));
        HisResponseTO<DocIndexToHisResTO> response = null;
        try {
            response = hisService.docIndexToHis(request);
            LOGGER.info("docIndexToHis response={}", JSONUtils.toString(response));
        } catch (Exception e) {
            LOGGER.error("docIndexToHis error ", e);
        }
        return response;
    }

    /**互联网his接口**/
    /**
     * his处方校验接口
     * （HIS系统对互联网医院待新增处方进行医保校验）
     */
    public HisResponseTO hisCheckRecipe(HisCheckRecipeReqTO request){
        IRecipeHisService hisService = AppDomainContext.getBean("his.iRecipeHisService", IRecipeHisService.class);
        return hisService.hisCheckRecipe(request);

    }

    /**
     * 更新患者取药方式
     */
    public HisResponseTO updateTakeDrugWay(UpdateTakeDrugWayReqTO request) {
        IRecipeHisService hisService = AppDomainContext.getBean("his.iRecipeHisService", IRecipeHisService.class);
        return hisService.updateTakeDrugWay(request);
    }

    public HisResponseTO syncDrugListToHis(SyncDrugListToHisReqTO request) {
        LOGGER.info("syncDrugListToHis request={}", JSONUtils.toString(request));
        IRecipeHisService hisService = AppDomainContext.getBean("his.iRecipeHisService", IRecipeHisService.class);
        HisResponseTO response = null;
        try {
            response = hisService.syncDrugListToHis(request);
            LOGGER.info("syncDrugListToHis response={}", JSONUtils.toString(response));
        } catch (Exception e) {
            LOGGER.error("syncDrugListToHis error ", e);
        }
        return response;
    }

    public HosPatientRecipeDTO queryHisPatientRecipeInfo(String organId, String qrInfo){
        LOGGER.info("queryHisPatientRecipeInfo organId={},qrInfo={}", organId,qrInfo);
        IRecipeHisService hisService = AppDomainContext.getBean("his.iRecipeHisService", IRecipeHisService.class);
        HisResponseTO<HosPatientRecipeBean> response;
        HosPatientRecipeBean hosPatientRecipeBean;
        HosPatientRecipeDTO hosPatientRecipeDTO = null;
        try {
            QueryHisPatientRecipeInfoReq req = new QueryHisPatientRecipeInfoReq();
            req.setOrganId(Integer.valueOf(organId));
            req.setQrInfo(qrInfo);
            response = hisService.queryHisPatientRecipeInfo(req);
            hosPatientRecipeBean = response.getData();
            LOGGER.info("queryHisPatientRecipeInfo response={}", JSONUtils.toString(response));
            hosPatientRecipeDTO = ObjectCopyUtils.convert(hosPatientRecipeBean, HosPatientRecipeDTO.class);
            HosPatientDTO patientDTO = ObjectCopyUtils.convert(hosPatientRecipeBean.getPatient(), HosPatientDTO.class);
            HosRecipeDTO recipeDTO = ObjectCopyUtils.convert(hosPatientRecipeBean.getRecipe(), HosRecipeDTO.class);
            if (recipeDTO != null){
                List<HosRecipeDetailDTO> recipeDateil = ObjectCopyUtils.convert(hosPatientRecipeBean.getRecipe().getDetailData(), HosRecipeDetailDTO.class);
                recipeDTO.setDetailData(recipeDateil);
                hosPatientRecipeDTO.setRecipe(recipeDTO);
            }
            hosPatientRecipeDTO.setPatient(patientDTO);
        } catch (Exception e) {
            LOGGER.error("queryHisPatientRecipeInfo error ", e);
        }
        return hosPatientRecipeDTO;
    }


    /**
     * 处方预结算
     */
    public HisResponseTO recipeMedicalPreSettle(MedicalPreSettleReqTO request) {
        IRecipeHisService hisService = AppDomainContext.getBean("his.iRecipeHisService", IRecipeHisService.class);
        return hisService.recipeMedicalPreSettle(request);
    }

    /**
     * 处方预结算(新)
     */
    public HisResponseTO<RecipeMedicalPreSettleInfo> recipeMedicalPreSettleN(MedicalPreSettleReqNTO request) {
        IRecipeHisService hisService = AppDomainContext.getBean("his.iRecipeHisService", IRecipeHisService.class);
        return hisService.recipeMedicalPreSettleN(request);
    }

    /**
     * 处方自费预结算
     */
    public HisResponseTO<RecipeCashPreSettleInfo> recipeCashPreSettleHis(RecipeCashPreSettleReqTO request) {
        IRecipeHisService hisService = AppDomainContext.getBean("his.iRecipeHisService", IRecipeHisService.class);
        return hisService.recipeCashPreSettle(request);
    }

    /**
     * 处方药品配送信息查询接口
     *
     * @param recipeSendMsgRequestTO p配送信息请求获取
     * @return
     */
    private RecipeSendMsgResTO recipeSendMsg(RecipeSendMsgRequestTO recipeSendMsgRequestTO) {
        IRecipeHisService hisService = AppDomainContext.getBean("his.iRecipeHisService", IRecipeHisService.class);
        return hisService.recipeSendMsg(recipeSendMsgRequestTO);
    }

    public HisResponseTO<MedicInsurSettleApplyResTO> recipeMedicInsurPreSettle(MedicInsurSettleApplyReqTO reqTO){
        IRecipeHisService hisService = AppDomainContext.getBean("his.iRecipeHisService", IRecipeHisService.class);
        return hisService.recipeMedicInsurPreSettle(reqTO);
    }

    /**
     * 获取患者特慢病病种列表
     * @return
     */
    public HisResponseTO<PatientChronicDiseaseRes> findPatientChronicDiseaseList(ChronicDiseaseListReqTO request){
        IRecipeHisService hisService = AppDomainContext.getBean("his.iRecipeHisService", IRecipeHisService.class);
        LOGGER.info("findPatientChronicDiseaseList request={}", JSONUtils.toString(request));
        HisResponseTO<PatientChronicDiseaseRes> response = null;
        try {
            response = hisService.findPatientChronicDiseaseList(request);
            LOGGER.info("findPatientChronicDiseaseList response={}", JSONUtils.toString(response));
        } catch (Exception e) {
            LOGGER.error("findPatientChronicDiseaseList error ", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, "接口返回异常");
        }
        return response;
    }
    @Deprecated
    public void findPatientDiagnose(PatientDiagnoseTO request) {
        IRecipeHisService hisService = AppDomainContext.getBean("his.iRecipeHisService", IRecipeHisService.class);
        LOGGER.info("findPatientDiagnose request={}", JSONUtils.toString(request));
        HisResponseTO<String> response;
        try {
            response = hisService.findPatientDiagnose(request);
            LOGGER.info("findPatientDiagnose response={}", JSONUtils.toString(response));
        } catch (Exception e) {
            LOGGER.error("findPatientDiagnose error ", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        }
        if (null == response) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "接口返回异常");
        } else {
            if (!"200".equals(response.getMsgCode())) {
                throw new DAOException(ErrorCode.SERVICE_ERROR, response.getMsg());
            }
        }
    }
}
