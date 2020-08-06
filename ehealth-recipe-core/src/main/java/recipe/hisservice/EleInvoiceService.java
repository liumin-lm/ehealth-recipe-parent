package recipe.hisservice;

import com.ngari.base.property.service.IConfigurationCenterUtilsService;
import com.ngari.common.mode.HisResponseTO;
import com.ngari.consult.common.model.ConsultExDTO;
import com.ngari.consult.common.service.IConsultExService;
import com.ngari.his.recipe.mode.EleInvoiceReqTo;
import com.ngari.his.recipe.mode.RecipeInvoiceTO;
import com.ngari.his.recipe.service.IRecipeHisService;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.patient.service.PatientService;
import com.ngari.platform.recipe.mode.InvoiceDTO;
import com.ngari.platform.recipe.mode.InvoiceItemDTO;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeExtend;
import com.ngari.recipe.entity.RecipeOrder;
import com.ngari.recipe.entity.Recipedetail;
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
import recipe.comment.DictionaryUtil;
import recipe.dao.RecipeDAO;
import recipe.dao.RecipeDetailDAO;
import recipe.dao.RecipeExtendDAO;
import recipe.dao.RecipeOrderDAO;
import recipe.util.DateConversion;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;


/**
 * @ClassName EleInvoiceService
 * @Description
 * @Author maoLy
 * @Date 2020/5/8
 **/
@RpcBean(value = "eleInvoiceService")
public class EleInvoiceService {
    private static final Integer RECIPE_TYPE = 1;
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

    @RpcService
    public List<String> findEleInvoice(EleInvoiceDTO eleInvoiceDTO) {
        LOGGER.info("EleInvoiceService.findEleInvoice 入参eleInvoiceDTO=[{}]", JSONUtils.toString(eleInvoiceDTO));
        validateParam(eleInvoiceDTO);
        EleInvoiceReqTo eleInvoiceReqTo = new EleInvoiceReqTo();
        if ("0".equals(eleInvoiceDTO.getType())) {
            eleInvoiceReqTo.setCzybz("0");
            IConsultExService iConsultExService = AppDomainContext.getBean("consult.consultExService", IConsultExService.class);
            ConsultExDTO consultExDTO = iConsultExService.getByConsultId(eleInvoiceDTO.getId());
            if (StringUtils.isNotBlank(consultExDTO.getRegisterNo())) {
                eleInvoiceDTO.setGhxh(consultExDTO.getRegisterNo());
            }
            if (StringUtils.isNotBlank(consultExDTO.getCardId())) {
                eleInvoiceDTO.setCardId(consultExDTO.getCardId());
            }
            if (StringUtils.isNotBlank(consultExDTO.getCardType())) {
                eleInvoiceDTO.setCardType(consultExDTO.getCardType());
            }
        }
        //处方数据
        if (RECIPE_TYPE.toString().equals(eleInvoiceDTO.getType())) {
            setRecipeDTO(eleInvoiceReqTo, eleInvoiceDTO);
        }

        if (StringUtils.isBlank(eleInvoiceDTO.getGhxh())) {
            throw new DAOException(609, "ghxh is null,无法获取对应电子发票");
        }

        PatientDTO patientDTO = patientService.get(eleInvoiceDTO.getMpiid());
        eleInvoiceReqTo.setPatientDTO(patientDTO);
        eleInvoiceReqTo.setOrganId(eleInvoiceDTO.getOrganId());
        if (StringUtils.isNotBlank(patientDTO.getPatientName())) {
            eleInvoiceReqTo.setHzxm(patientDTO.getPatientName());
        }
        if (StringUtils.isNotBlank(patientDTO.getGuardianCertificate())) {
            eleInvoiceReqTo.setLxrzjh(patientDTO.getGuardianCertificate());
        }
        if (StringUtils.isNotBlank(patientDTO.getMobile())) {
            eleInvoiceReqTo.setLxrdh(patientDTO.getMobile());
            eleInvoiceReqTo.setPhone(patientDTO.getMobile());
        }
        if (StringUtils.isNotBlank(patientDTO.getCertificate())) {
            eleInvoiceReqTo.setSfzh(patientDTO.getCertificate());
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
            throw new DAOException(609, "organId is null");
        }
        if (StringUtils.isBlank(type)) {
            throw new DAOException(609, "type is null");
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
            throw new DAOException(609, "recipe is null");
        }

        eleInvoiceReqTo.setCreateDate(recipe.getCreateDate());
        eleInvoiceReqTo.setDeptId(recipe.getDepart());
        eleInvoiceReqTo.setDeptName(DictionaryUtil.getDictionary("eh.base.dictionary.Depart", recipe.getDepart()));

        InvoiceDTO invoiceDTO = new InvoiceDTO();
        invoiceDTO.setPayId(recipe.getRecipeId());
        RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipeId);
        if (null != recipeExtend) {
            invoiceDTO.setInvoiceNumber(recipeExtend.getEinvoiceNumber());
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
        List<InvoiceItemDTO> invoiceItem = new LinkedList<>();
        if (StringUtils.isNotEmpty(recipe.getOrderCode())) {
            RecipeOrder recipeOrder = recipeOrderDAO.getByOrderCode(recipe.getOrderCode());
            invoiceDTO.setPayAmount(recipeOrder.getActualPrice());
            invoiceDTO.setPayWay(recipeOrder.getWxPayWay());
            invoiceDTO.setPayTime(recipeOrder.getPayTime());
            invoiceDTO.setFundAmount(recipeOrder.getFundAmount());
            invoiceDTO.setMedicalSettleCode(recipeOrder.getMedicalSettleCode());

            invoiceItem.add(getInvoiceItemDTO(recipe, recipeOrder.getOrderId(), "挂号费",
                    recipeOrder.getRegisterFee(), "", 1D));
            invoiceItem.add(getInvoiceItemDTO(recipe, recipeOrder.getOrderId(), "配送费",
                    recipeOrder.getExpressFee(), "", 1D));
        }

        List<Recipedetail> recipeDetailList = recipeDetailDAO.findByRecipeId(recipeId);
        if (CollectionUtils.isNotEmpty(recipeDetailList)) {
            recipeDetailList.forEach(a -> invoiceItem.add(getInvoiceItemDTO(recipe, a.getRecipeDetailId(),
                    a.getDrugName(), a.getDrugCost(), a.getDrugUnit(), a.getUseTotalDose())));
        }
    }

    private InvoiceItemDTO getInvoiceItemDTO(Recipe recipe, Integer code, String name, BigDecimal amount, String unit, Double quantity) {
        InvoiceItemDTO invoiceItemDTO = new InvoiceItemDTO();
        invoiceItemDTO.setRelatedCode(recipe.getRecipeId());
        invoiceItemDTO.setRelatedName(DictionaryUtil.getDictionary("eh.cdr.dictionary.RecipeType", recipe.getRecipeType()));
        invoiceItemDTO.setCode(code);
        invoiceItemDTO.setName(name);
        invoiceItemDTO.setAmount(amount);
        invoiceItemDTO.setUnit(unit);
        invoiceItemDTO.setQuantity(quantity);
        return invoiceItemDTO;
    }

    private List<String> response(HisResponseTO<RecipeInvoiceTO> hisResponse) {
        LOGGER.info("EleInvoiceService.stringToList  hisResponseTO={}", JSONUtils.toString(hisResponse));
        if (null == hisResponse) {
            LOGGER.info("EleInvoiceService.stringToList 请求his失败,hisResponseTo is null");
            throw new DAOException(609, "获取出错，请稍后再试");
        }
        if (!"200".equals(hisResponse.getMsgCode())) {
            LOGGER.info("EleInvoiceService.stringToList 请求his失败，返回信息:msg={}", hisResponse.getMsg());
            throw new DAOException(609, hisResponse.getMsg());
        }
        RecipeInvoiceTO result = hisResponse.getData();
        if (null == result) {
            throw new DAOException(609, "获取数据出错，请稍后再试");
        }
        if (StringUtils.isBlank(result.getInvoiceUrl())) {
            throw new DAOException(609, "获取地址出错，请稍后再试");
        }
        if (null != result.getInvoiceType() && RECIPE_TYPE.equals(result.getInvoiceType())
                && StringUtils.isNotEmpty(result.getInvoiceNumber()) && null != result.getRequestId()) {
            Map<String, String> map = new HashMap(1);
            map.put("einvoiceNumber", result.getInvoiceNumber());
            recipeExtendDAO.updateRecipeExInfoByRecipeId(result.getRequestId(), map);
        }
        return Arrays.asList(result.getInvoiceUrl().split(","));
    }
}
