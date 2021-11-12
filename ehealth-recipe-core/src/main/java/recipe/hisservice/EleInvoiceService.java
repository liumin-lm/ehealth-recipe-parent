package recipe.hisservice;

import com.ngari.base.property.service.IConfigurationCenterUtilsService;
import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.recipe.mode.EleInvoiceReqTo;
import com.ngari.his.recipe.mode.RecipeInvoiceTO;
import com.ngari.his.recipe.service.IRecipeHisService;
import com.ngari.patient.dto.DepartmentDTO;
import com.ngari.patient.dto.OrganDTO;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.patient.service.DepartmentService;
import com.ngari.patient.service.OrganService;
import com.ngari.patient.service.PatientService;
import com.ngari.platform.recipe.mode.InvoiceDTO;
import com.ngari.platform.recipe.mode.InvoiceItemDTO;
import com.ngari.recipe.entity.*;
import com.ngari.revisit.RevisitAPI;
import com.ngari.revisit.RevisitBean;
import com.ngari.revisit.common.model.RevisitExDTO;
import com.ngari.revisit.common.model.RevisitInvoiceDTO;
import com.ngari.revisit.common.service.IRevisitExService;
import com.ngari.revisit.common.service.IRevisitService;
import ctd.dictionary.DictionaryController;
import ctd.persistence.exception.DAOException;
import ctd.spring.AppDomainContext;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.ApplicationUtils;
import recipe.bean.EleInvoiceDTO;
import recipe.constant.ErrorCode;
import recipe.constant.MedicalChargesEnum;
import recipe.dao.*;
import recipe.util.DateConversion;
import recipe.util.DictionaryUtil;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;


/**
 * @ClassName EleInvoiceService
 * @Description
 * @Author maoLy
 * @Date 2020/5/8
 **/
@RpcBean(value = "eleInvoiceService")
public class EleInvoiceService {
    private static final String RECIPE_TYPE = "1";
    private static final Logger LOGGER = LoggerFactory.getLogger(EleInvoiceService.class);
    @Autowired
    private PatientService patientService;
    @Autowired
    private RecipeDAO recipeDAO;
    @Autowired
    private RecipeExtendDAO recipeExtendDAO;
    @Resource
    private RecipeOrderDAO recipeOrderDAO;
    @Autowired
    private RecipeDetailDAO recipeDetailDAO;
    @Autowired
    private DepartmentService departmentService;
    @Autowired
    private OrganService organService;
    @Autowired
    private RecipeOrderBillDAO recipeOrderBillDAO;

    @RpcService
    public List<String> findEleInvoice(EleInvoiceDTO eleInvoiceDTO) {
        try {
            return eleInvoice(eleInvoiceDTO);
        } catch (DAOException e) {
            throw new DAOException(e.getCode(), e.getMessage());
        } catch (Exception e) {
            LOGGER.info("EleInvoiceService.findEleInvoice error ", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, "无法获取对应电子发票");
        }
    }

    public List<String> eleInvoice(EleInvoiceDTO eleInvoiceDTO) {
        LOGGER.info("EleInvoiceService.findEleInvoice 入参eleInvoiceDTO=[{}]", JSONUtils.toString(eleInvoiceDTO));
        validateParam(eleInvoiceDTO);
        EleInvoiceReqTo eleInvoiceReqTo = new EleInvoiceReqTo();
        if ("0".equals(eleInvoiceDTO.getType())) {
            eleInvoiceReqTo.setCzybz("0");
            setRecipeConsultDTO(eleInvoiceReqTo, eleInvoiceDTO);

        }
        //处方数据
        if (RECIPE_TYPE.equals(eleInvoiceDTO.getType())) {
            setRecipeDTO(eleInvoiceReqTo, eleInvoiceDTO);
        }

        if (StringUtils.isBlank(eleInvoiceDTO.getGhxh())) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "ghxh is null,无法获取对应电子发票");
        }

        PatientDTO patientDTO = patientService.get(eleInvoiceDTO.getMpiid());
        eleInvoiceReqTo.setPatientDTO(patientDTO);
        eleInvoiceReqTo.setOrganId(eleInvoiceDTO.getOrganId());
        if (StringUtils.isNotBlank(patientDTO.getPatientName())) {
            eleInvoiceReqTo.setHzxm(patientDTO.getPatientName());
        }
        if (StringUtils.isNotBlank(patientDTO.getGuardianCertificate())) {
            eleInvoiceReqTo.setLxrzjh(patientDTO.getGuardianCertificate());
            eleInvoiceReqTo.setGuardianCertificateType(patientDTO.getGuardianCertificateType());
        }
        if (StringUtils.isNotBlank(patientDTO.getMobile())) {
            eleInvoiceReqTo.setLxrdh(patientDTO.getMobile());
            eleInvoiceReqTo.setPhone(patientDTO.getMobile());
        }
        if (StringUtils.isNotBlank(patientDTO.getCertificate())) {
            eleInvoiceReqTo.setSfzh(patientDTO.getCertificate());
            eleInvoiceReqTo.setCertificateType(patientDTO.getCertificateType());
        }
        if (StringUtils.isNotBlank(eleInvoiceDTO.getCardId())) {
            eleInvoiceReqTo.setCardno(eleInvoiceDTO.getCardId());
        }
        if (StringUtils.isNotBlank(eleInvoiceDTO.getCardType())) {
            eleInvoiceReqTo.setCxlb(eleInvoiceDTO.getCardType());
        }
        eleInvoiceReqTo.setType(eleInvoiceDTO.getType());
        eleInvoiceReqTo.setKsrq(DateConversion.getPastDate(7));
        eleInvoiceReqTo.setJsrq(DateConversion.getToDayDate());
        eleInvoiceReqTo.setGhxh(eleInvoiceDTO.getGhxh());

        IRecipeHisService hisService = AppDomainContext.getBean("his.iRecipeHisService", IRecipeHisService.class);
        LOGGER.info("EleInvoiceService.findEleInvoice 待推送数据:eleInvoiceReqTo:[{}]", JSONUtils.toString(eleInvoiceReqTo));
        HisResponseTO<RecipeInvoiceTO> hisResponse = hisService.queryEleInvoice(eleInvoiceReqTo);
        return response(hisResponse);
    }

    private void validateParam(EleInvoiceDTO eleInvoiceDTO) {
        if (eleInvoiceDTO.getId() == null) {
            throw new DAOException(609, "id is null");
        }
        if (StringUtils.isBlank(eleInvoiceDTO.getMpiid())) {
            throw new DAOException(609, "mpiid is null");
        }
        if (eleInvoiceDTO.getOrganId() == null) {
            throw new DAOException(609, "organId is null");
        }
        if (StringUtils.isBlank(eleInvoiceDTO.getType())) {
            throw new DAOException(609, "type is null");
        }
    }

    @RpcService
    public String getEleInvoiceEnable(Integer organId, String type) {
        if (organId == null) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "organId is null");
        }
        if (StringUtils.isBlank(type)) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "type is null");
        }
        IConfigurationCenterUtilsService configurationCenterUtils = ApplicationUtils.getBaseService(IConfigurationCenterUtilsService.class);
        String result = "";
        if ("0".equals(type)) {
            result = (String) configurationCenterUtils.getConfiguration(organId, "EleInvoiceFzSwitchNew");
        }
        if ("1".equals(type)) {
            result = (String) configurationCenterUtils.getConfiguration(organId, "EleInvoiceCfSwitch");
        }
        if (StringUtils.isBlank(result)) {
            result = "0";
        }
        return result;
    }

    /**
     * 复诊相关数据
     *
     * @param eleInvoiceReqTo
     * @param eleInvoiceDTO
     * @return
     */
    private void setRecipeConsultDTO(EleInvoiceReqTo eleInvoiceReqTo, EleInvoiceDTO eleInvoiceDTO){


        //复诊
        IRevisitService iRevisitService = RevisitAPI.getService(IRevisitService.class);
        RevisitBean revisitBean = iRevisitService.getById(eleInvoiceDTO.getId());
        if (null == revisitBean) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "consultBean is null");
        }

        //复诊ex
        IRevisitExService iRevisitExService = RevisitAPI.getService(IRevisitExService.class);
        RevisitExDTO consultExDTO = iRevisitExService.getByConsultId(eleInvoiceDTO.getId());
        if (null == consultExDTO) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "consultExDTO is null");
        }
        //机构
        OrganDTO organDTO = organService.getByOrganId(revisitBean.getConsultOrgan());
        if (null == organDTO) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "organDTO is null");
        }
        //门诊
        DepartmentDTO departmentDTO = departmentService.getByDeptId(revisitBean.getConsultDepart());
        if (null == departmentDTO) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "departmentDTO is null");
        }


        if (StringUtils.isNotBlank(consultExDTO.getRegisterNo())) {
            eleInvoiceDTO.setGhxh(consultExDTO.getRegisterNo());
        }
        if (StringUtils.isNotBlank(consultExDTO.getCardType())) {
            eleInvoiceDTO.setCardType(consultExDTO.getCardType());
        }
        if (StringUtils.isNotBlank(consultExDTO.getCardId())) {
            eleInvoiceDTO.setCardId(consultExDTO.getCardId());
        }

        try {
            eleInvoiceReqTo.setRequestId(revisitBean.getConsultId());
            eleInvoiceReqTo.setCreateDate(revisitBean.getPaymentDate());
            eleInvoiceReqTo.setDeptId(revisitBean.getConsultDepart());
            eleInvoiceReqTo.setDeptName(DictionaryController.instance().get("eh.base.dictionary.Depart").getText(revisitBean.getConsultDepart()));
            InvoiceDTO invoiceDTO = new InvoiceDTO();
            if (consultExDTO.getInvoiceNumber()!=null){
                invoiceDTO.setInvoiceNumber(consultExDTO.getInvoiceNumber());
            }
            invoiceDTO.setPayId(revisitBean.getConsultId());
            invoiceDTO.setPayAmount(revisitBean.getConsultPrice());
            invoiceDTO.setPayWay(revisitBean.getPayWay());
            invoiceDTO.setPayTime(revisitBean.getPaymentDate());
            invoiceDTO.setFundAmount(revisitBean.getFundAmount());
            invoiceDTO.setMedicalSettleCode(consultExDTO.getInsureTypeCode());

            List<InvoiceItemDTO> invoiceItem = new LinkedList<>();
            InvoiceItemDTO invoiceItemDTO = getInvoiceItemDTO(MedicalChargesEnum.CONSULT.getCode(), MedicalChargesEnum.CONSULT.getName(), revisitBean.getConsultId().toString(), "复诊咨询费", BigDecimal.valueOf(revisitBean.getConsultCost()), "元", 1D);
            invoiceItem.add(invoiceItemDTO);
            invoiceDTO.setInvoiceItem(invoiceItem);
            eleInvoiceReqTo.setInvoiceDTO(invoiceDTO);

        } catch (Exception e) {
            LOGGER.error("EleInvoiceService ", e);
        }

    }


    /**
     * 处方发票号入参
     * 组织处方相关数据
     *
     * @param eleInvoiceDTO
     * @param
     */
    private void setRecipeDTO(EleInvoiceReqTo eleInvoiceReqTo, EleInvoiceDTO eleInvoiceDTO) {
        Integer recipeId = eleInvoiceDTO.getId();
        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        if (null == recipe) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "recipe is null");
        }
        //如果是合并处方处理
        List<Integer> recipeIds = eleInvoiceDTO.getRecipeIds();
        if (CollectionUtils.isEmpty(recipeIds)) {
            recipeIds = Arrays.asList(recipeId);
        }
        eleInvoiceReqTo.setRequestId(recipe.getRecipeId());
        eleInvoiceReqTo.setCreateDate(recipe.getCreateDate());
        eleInvoiceReqTo.setDeptId(recipe.getDepart());
        eleInvoiceReqTo.setDeptName(DictionaryUtil.getDictionary("eh.base.dictionary.Depart", recipe.getDepart()));

        InvoiceDTO invoiceDTO = new InvoiceDTO();
        invoiceDTO.setPayId(recipe.getRecipeId());
        RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipeId);
        String invoiceNumber = "";
        if (null != recipeExtend) {
            invoiceNumber = recipeExtend.getEinvoiceNumber();
            if (StringUtils.isNotBlank(recipeExtend.getRegisterID())) {
                eleInvoiceDTO.setGhxh(recipeExtend.getRegisterID());
            }
            if (StringUtils.isNotBlank(recipeExtend.getCardType())) {
                eleInvoiceDTO.setCardType(recipeExtend.getCardType());
            }
            if (StringUtils.isNotBlank(recipeExtend.getCardNo())) {
                eleInvoiceDTO.setCardId(recipeExtend.getCardNo());
            }
        }
        if (StringUtils.isBlank(invoiceNumber) && StringUtils.isNotBlank(recipe.getOrderCode())){
            RecipeOrderBill recipeOrderBill = recipeOrderBillDAO.getRecipeOrderBillByOrderCode(recipe.getOrderCode());
            if (null != recipeOrderBill){
                invoiceNumber = recipeOrderBill.getBillNumber();
            }
        }
        invoiceDTO.setInvoiceNumber(invoiceNumber);
        List<InvoiceItemDTO> invoiceItem = new LinkedList<>();
        if (StringUtils.isNotEmpty(recipe.getOrderCode())) {
            RecipeOrder recipeOrder = recipeOrderDAO.getByOrderCode(recipe.getOrderCode());
            invoiceDTO.setTradeNumber(recipeOrder.getTradeNo());
            invoiceDTO.setPayAmount(recipeOrder.getActualPrice());
            invoiceDTO.setPayWay(recipeOrder.getWxPayWay());
            invoiceDTO.setPayTime(recipeOrder.getPayTime());
            invoiceDTO.setFundAmount(recipeOrder.getFundAmount() == null ? 0D : recipeOrder.getFundAmount());
            invoiceDTO.setMedicalSettleCode(recipeOrder.getMedicalSettleCode());
            //获取挂号费用
            invoiceItem.add(getInvoiceItemDTO(MedicalChargesEnum.REGISTRATION.getCode(), MedicalChargesEnum.REGISTRATION.getName()
                    , recipeOrder.getOrderCode(), MedicalChargesEnum.REGISTRATION.getName(), recipeOrder.getRegisterFee(), "", 1D));
            if (null != recipeOrder.getExpressFeePayWay() && recipeOrder.getExpressFeePayWay().equals(1)) {
                invoiceItem.add(getInvoiceItemDTO(MedicalChargesEnum.DISTRIBUTION.getCode(), MedicalChargesEnum.DISTRIBUTION.getName()
                        , recipeOrder.getOrderCode(), MedicalChargesEnum.DISTRIBUTION.getName(), recipeOrder.getExpressFee(), "", 1D));
            }
        }

        List<Recipedetail> recipeDetailList = recipeDetailDAO.findByRecipeIdList(recipeIds);
        //获取处方药品明细费用
        if (CollectionUtils.isNotEmpty(recipeDetailList)) {
            recipeDetailList.forEach(a -> invoiceItem.add(getInvoiceItemDTO(recipe, a.getOrganDrugCode(), a.getDrugName(), a.getActualSalePrice() == null ? a.getSalePrice() : a.getActualSalePrice(), a.getDrugUnit(), a.getUseTotalDose())));
        }
        if (CollectionUtils.isNotEmpty(invoiceItem)) {
            List<InvoiceItemDTO> item = invoiceItem.stream().filter(Objects::nonNull).collect(Collectors.toList());
            invoiceDTO.setInvoiceItem(item);
        }
        eleInvoiceReqTo.setInvoiceDTO(invoiceDTO);
    }

    /**
     * 组装 发票明细
     *
     * @param recipe
     * @param code
     * @param name
     * @param amount
     * @param unit
     * @param quantity
     * @return
     */
    private InvoiceItemDTO getInvoiceItemDTO(Recipe recipe, String code, String name, BigDecimal amount, String unit, Double quantity) {
        String recipeType = DictionaryUtil.getDictionary("eh.cdr.dictionary.RecipeType", recipe.getRecipeType());
        return getInvoiceItemDTO(recipe.getRecipeType(), recipeType, code, name, amount, unit, quantity);
    }

    private InvoiceItemDTO getInvoiceItemDTO(Integer relatedCode, String relatedName, String code, String name
            , BigDecimal amount, String unit, Double quantity) {
        if (null == amount) {
            return null;
        }
        InvoiceItemDTO invoiceItemDTO = new InvoiceItemDTO();
        invoiceItemDTO.setRelatedCode(relatedCode);
        invoiceItemDTO.setRelatedName(relatedName);
        invoiceItemDTO.setCode(code);
        invoiceItemDTO.setName(name);
        invoiceItemDTO.setAmount(amount);
        invoiceItemDTO.setUnit(unit);
        invoiceItemDTO.setQuantity(quantity);
        return invoiceItemDTO;
    }

    /**
     * 出参解析
     *
     * @param hisResponse
     * @return
     */
    private List<String> response(HisResponseTO<RecipeInvoiceTO> hisResponse) {
        try {
            LOGGER.info("EleInvoiceService.stringToList  hisResponseTO={}", JSONUtils.toString(hisResponse));
            if (null == hisResponse) {
                LOGGER.info("EleInvoiceService.stringToList 请求his失败,hisResponseTo is null");
                throw new DAOException(ErrorCode.SERVICE_ERROR, "获取出错，请稍后再试");
            }
            if (!"200".equals(hisResponse.getMsgCode())) {
                LOGGER.info("EleInvoiceService.stringToList 请求his失败，返回信息:msg={}", hisResponse.getMsg());
                throw new DAOException(ErrorCode.SERVICE_ERROR, hisResponse.getMsg());
            }
            RecipeInvoiceTO result = hisResponse.getData();
            if (null == result) {
                throw new DAOException(ErrorCode.SERVICE_ERROR, "获取数据出错，请稍后再试");
            }
            if (StringUtils.isBlank(result.getInvoiceUrl())) {
                throw new DAOException(ErrorCode.SERVICE_ERROR, "获取地址出错，请稍后再试");
            }
            if (null != result.getInvoiceType() && RECIPE_TYPE.equals(result.getInvoiceType())
                    && StringUtils.isNotEmpty(result.getBizCode()) && null != result.getRequestId()) {
//                Map<String, String> map = new HashMap(1);
//                map.put("einvoiceNumber", result.getBizCode());
//                recipeExtendDAO.updateRecipeExInfoByRecipeId(result.getRequestId(), map);
                Recipe recipe = recipeDAO.getByRecipeId(result.getRequestId());
                RecipeOrderBill orderBill = new RecipeOrderBill();
                orderBill.setCreateTime(new Date());
                orderBill.setBillNumber(result.getBizCode());
                orderBill.setRecipeOrderCode(recipe.getOrderCode());
                recipeOrderBillDAO.save(orderBill);
            }
            //复诊(通过bizCode来判断是否开票)
            if (null !=result.getInvoiceType() && "0".equals(result.getInvoiceType()) && StringUtils.isNotEmpty(result.getInvoiceNumber()) && null != result.getRequestId()){
                IRevisitExService consultExService = RevisitAPI.getService(IRevisitExService.class);
                consultExService.updateInvoiceNumberByConsultId(result.getBizCode(), result.getRequestId());

                IRevisitExService iRevisitExService = AppDomainContext.getBean("revisit.revisitExService", IRevisitExService.class);
                RevisitInvoiceDTO reDTO = iRevisitExService.findRevisitInvoiceByConsultId(result.getRequestId());
                if (null == reDTO){
                    RevisitInvoiceDTO revisitInvoiceDTO = new RevisitInvoiceDTO();
                    revisitInvoiceDTO.setConsultId(result.getRequestId());
                    revisitInvoiceDTO.setInvoiceNumber(result.getInvoiceNumber());
                    revisitInvoiceDTO.setBizCode(StringUtils.defaultString(result.getBizCode(),""));
                    revisitInvoiceDTO.setInvoiceCode(StringUtils.defaultString(result.getInvoiceCode(),""));
                    revisitInvoiceDTO.setInvoiceRandom(StringUtils.defaultString(result.getInvoiceRandom(),""));
                    consultExService.saveRevisitInvoice(revisitInvoiceDTO);
                }
            }
            List<String> list = Arrays.asList(result.getInvoiceUrl().split(","));
            LOGGER.info("EleInvoiceService.stringToList list :{}", JSONUtils.toString(list));
            return list;
        } catch (Exception e) {
            LOGGER.info("EleInvoiceService.stringToList error ", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        }
    }
}
