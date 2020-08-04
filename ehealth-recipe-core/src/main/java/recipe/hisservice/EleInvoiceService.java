package recipe.hisservice;

import com.ngari.base.property.service.IConfigurationCenterUtilsService;
import com.ngari.common.mode.HisResponseTO;
import com.ngari.consult.common.model.ConsultExDTO;
import com.ngari.consult.common.service.IConsultExService;
import com.ngari.his.recipe.mode.EleInvoiceReqTo;
import com.ngari.his.recipe.service.IRecipeHisService;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.patient.service.PatientService;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.platform.recipe.mode.*;
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
import recipe.dao.RecipeDAO;
import recipe.dao.RecipeDetailDAO;
import recipe.dao.RecipeExtendDAO;
import recipe.dao.RecipeOrderDAO;
import recipe.util.DateConversion;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;


/**
 * @ClassName EleInvoiceService
 * @Description
 * @Author maoLy
 * @Date 2020/5/8
 **/
@RpcBean(value = "eleInvoiceService")
public class EleInvoiceService {

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
        PatientDTO patientDTO = patientService.get(eleInvoiceDTO.getMpiid());

        if ("0".equals(eleInvoiceDTO.getType())) {
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
        EleInvoiceReqTo eleInvoiceReqTo = new EleInvoiceReqTo();
        //处方数据
        if ("1".equals(eleInvoiceDTO.getType())) {
            RecipeDTO recipeDTO = setRecipeDTO(eleInvoiceDTO);
            recipeDTO.setPatientDTO(patientDTO);
            eleInvoiceReqTo.setRecipeDTO(recipeDTO);
        }

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
        eleInvoiceReqTo.setBlh(null);
        if(StringUtils.isNotBlank(eleInvoiceDTO.getCardId())){
            eleInvoiceReqTo.setCardno(eleInvoiceDTO.getCardId());
        }
        if(StringUtils.isNotBlank(eleInvoiceDTO.getCardType())){
            eleInvoiceReqTo.setCxlb(eleInvoiceDTO.getCardType());
        }
        eleInvoiceReqTo.setType(eleInvoiceDTO.getType());

        eleInvoiceReqTo.setKsrq(DateConversion.getPastDate(7));
        eleInvoiceReqTo.setJsrq(DateConversion.getToDayDate());
        if ("0".equals(eleInvoiceDTO.getType())) {
            eleInvoiceReqTo.setCzybz("0");
        }
        if ("1".equals(eleInvoiceDTO.getType())) {
            eleInvoiceReqTo.setCzybz(null);
        }
        if (StringUtils.isNotBlank(eleInvoiceDTO.getGhxh())) {
            eleInvoiceReqTo.setGhxh(eleInvoiceDTO.getGhxh());
        } else {
            throw new DAOException(609, "ghxh is null,无法获取对应电子发票");
        }
        IRecipeHisService hisService = AppDomainContext.getBean("his.iRecipeHisService", IRecipeHisService.class);
        LOGGER.info("EleInvoiceService.findEleInvoice 待推送数据:eleInvoiceReqTo:[{}]", JSONUtils.toString(eleInvoiceReqTo));
        HisResponseTO<String> hisResponse = hisService.queryEleInvoice(eleInvoiceReqTo);
        return stringToList(hisResponse);


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

    /**
     * 处方发票号入参
     * 组织处方相关数据
     *
     * @param eleInvoiceDTO
     * @param
     */
    private RecipeDTO setRecipeDTO(EleInvoiceDTO eleInvoiceDTO) {
        RecipeDTO recipeDTO = new RecipeDTO();
        Integer recipeId = eleInvoiceDTO.getId();

        Recipe recipe = recipeDAO.getByRecipeId(recipeId);
        if (null == recipe) {
            throw new DAOException(609, "recipe is null");
        }
        RecipeBean recipeBean = ObjectCopyUtils.convert(recipe, RecipeBean.class);
        recipeDTO.setRecipeBean(recipeBean);

        RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(recipeId);
        if (null != recipeExtend) {
            RecipeExtendBean recipeExtendBean = ObjectCopyUtils.convert(recipeExtend, RecipeExtendBean.class);
            recipeDTO.setRecipeExtendBean(recipeExtendBean);
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

        RecipeOrder recipeOrder = recipeOrderDAO.getOrderByRecipeId(recipeId);
        if (null != recipeOrder) {
            RecipeOrderBean recipeOrderBean = ObjectCopyUtils.convert(recipeOrder, RecipeOrderBean.class);
            recipeDTO.setRecipeOrderBean(recipeOrderBean);
        }

        List<Recipedetail> recipeDetailList = recipeDetailDAO.findByRecipeId(recipeId);
        if (CollectionUtils.isNotEmpty(recipeDetailList)) {
            List<RecipeDetailBean> recipeDetails = ObjectCopyUtils.convert(recipeDetailList, RecipeDetailBean.class);
            recipeDTO.setRecipeDetails(recipeDetails);
        }
        return recipeDTO;
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

    private List<String> stringToList(HisResponseTO<String> hisResponse) {
        LOGGER.info("EleInvoiceService.stringToList  hisResponseTO={}", JSONUtils.toString(hisResponse));
        if (null == hisResponse) {
            LOGGER.info("EleInvoiceService.stringToList 请求his失败,hisResponseTo is null");
            throw new DAOException(609, "当前系统繁忙，请稍后再试");
        }
        if (!"200".equals(hisResponse.getMsgCode())) {
            LOGGER.info("EleInvoiceService.stringToList 请求his失败，返回信息:msg={}", hisResponse.getMsg());
            throw new DAOException(609, hisResponse.getMsg());
        }
        String result = hisResponse.getData();
        if (StringUtils.isBlank(result)) {
            throw new DAOException(609, "当前系统繁忙，请稍后再试");
        }
        return Arrays.asList(result.split(","));
    }
}
