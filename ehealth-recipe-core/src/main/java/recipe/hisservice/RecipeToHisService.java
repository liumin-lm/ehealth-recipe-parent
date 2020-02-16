package recipe.hisservice;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.recipe.mode.*;
import com.ngari.his.recipe.service.IRecipeHisService;
import com.ngari.patient.dto.EmploymentDTO;
import com.ngari.patient.service.BasicAPI;
import com.ngari.patient.service.EmploymentService;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.entity.OrganDrugList;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.Recipedetail;
import com.ngari.recipe.hisprescription.model.*;
import ctd.persistence.DAOFactory;
import ctd.spring.AppDomainContext;
import ctd.util.JSONUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.ApplicationUtils;
import recipe.constant.RecipeStatusConstant;
import recipe.dao.OrganDrugListDAO;
import recipe.dao.RecipeDAO;
import recipe.service.HisCallBackService;
import recipe.service.RecipeCheckService;
import recipe.service.RecipeLogService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
            //失败发送系统消息
            recipeDAO.updateRecipeInfoByRecipeId(recipeId, RecipeStatusConstant.HIS_FAIL, null);
            //日志记录
            RecipeLogService.saveRecipeLog(recipeId, RecipeStatusConstant.CHECKING_HOS,
                    RecipeStatusConstant.HIS_FAIL, "his写入失败，调用前置机处方写入服务失败");
            LOGGER.error("recipeSend recipeId={}, 调用BASE 处方写入服务错误!", recipeId);
        }
    }

    public Integer listSingleQuery(RecipeListQueryReqTO request) {
        IRecipeHisService hisService = AppDomainContext.getBean("his.iRecipeHisService", IRecipeHisService.class);
        LOGGER.info("listSingleQuery request={}", JSONUtils.toString(request));
        try {
            RecipeListQueryResTO response = hisService.listQuery(request);
            LOGGER.info("listSingleQuery response={}", JSONUtils.toString(response));
            Integer busStatus = null;
            //有可能前置机没实现这个接口 返回null 保证流程走通
            if (null == response){
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


    public void listQuery(RecipeListQueryReqTO request) {
        IRecipeHisService hisService = AppDomainContext.getBean("his.iRecipeHisService", IRecipeHisService.class);
        LOGGER.info("listQuery request={}", JSONUtils.toString(request));
        try {
            RecipeListQueryResTO response = hisService.listQuery(request);
            EmploymentService employmentService = BasicAPI.getService(EmploymentService.class);
            RecipeCheckService recipeCheckService = ApplicationUtils.getRecipeService(RecipeCheckService.class);
            RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
            LOGGER.info("listQuery response={}", JSONUtils.toString(response));
            if (null == response || null == response.getMsgCode()) {
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
                                recipeCheckService.saveCheckResult(checkParam);
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
            request.setData(Lists.<DrugInfoTO>newArrayList());
            request.setDrcode(Lists.<String>newArrayList());
        } else {
            //查询限定范围内容的药品数据，返回的是该医院 无效的药品信息
            request.setData(drugInfoList);
            List<String> drugIdList = FluentIterable.from(drugInfoList).transform(new Function<DrugInfoTO, String>() {
                @Override
                public String apply(DrugInfoTO input) {
                    return input.getDrcode();
                }
            }).toList();
            request.setDrcode(drugIdList);
        }
        LOGGER.info("queryDrugInfo request={}", JSONUtils.toString(request));

        try {
            DrugInfoResponseTO response = hisService.queryDrugInfo(request);
            LOGGER.info("queryDrugInfo response={}", JSONUtils.toString(response));
            if (null != response && Integer.valueOf(200).equals(response.getMsgCode())) {
                return (null != response.getData()) ? response.getData() : new ArrayList<DrugInfoTO>();
            }
        } catch (Exception e) {
            LOGGER.error("queryDrugInfo error ", e);
        }
        return null;
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
        IRecipeHisService hisService = AppDomainContext.getBean("his.iRecipeHisService", IRecipeHisService.class);

        DrugInfoRequestTO request = new DrugInfoRequestTO();
        request.setOrganId(organId);
        List<Integer> drugIdList = FluentIterable.from(detailList).transform(new Function<Recipedetail, Integer>() {
            @Override
            public Integer apply(Recipedetail input) {
                return input.getDrugId();
            }
        }).toList();

        List<OrganDrugList> organDrugList = drugDao.findByOrganIdAndDrugIds(organId, drugIdList);
        Map<String, OrganDrugList> drugIdAndProduce = Maps.uniqueIndex(organDrugList, new Function<OrganDrugList, String>() {
            @Override
            public String apply(OrganDrugList input) {
                return input.getOrganDrugCode();
            }
        });

        List<DrugInfoTO> data = new ArrayList<>(detailList.size());
        DrugInfoTO drugInfo;
        OrganDrugList organDrug;
        for (Recipedetail detail : detailList) {
            drugInfo = new DrugInfoTO(detail.getOrganDrugCode());
            drugInfo.setPack(detail.getPack().toString());
            drugInfo.setPackUnit(detail.getDrugUnit());
            organDrug = drugIdAndProduce.get(detail.getOrganDrugCode());
            if (null != organDrug) {
                drugInfo.setManfcode(organDrug.getProducerCode());
            }
            data.add(drugInfo);
        }
        request.setData(data);

        DrugInfoResponseTO response = null;
        LOGGER.info("scanDrugStock request={}", JSONUtils.toString(request));
        try {
            response = hisService.scanDrugStock(request);
            LOGGER.info("scanDrugStock response={}", JSONUtils.toString(response));
        } catch (Exception e) {
            LOGGER.error("scanDrugStock error ", e);
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

}
