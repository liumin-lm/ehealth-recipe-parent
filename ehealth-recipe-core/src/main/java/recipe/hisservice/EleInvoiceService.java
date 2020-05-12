package recipe.hisservice;

import com.ngari.base.property.service.IConfigurationCenterUtilsService;
import com.ngari.common.mode.HisResponseTO;
import com.ngari.consult.common.model.ConsultExDTO;
import com.ngari.consult.common.service.IConsultExService;
import com.ngari.his.recipe.mode.EleInvoiceReqTo;
import com.ngari.his.recipe.service.IRecipeHisService;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.patient.service.BasicAPI;
import com.ngari.patient.service.PatientService;
import com.ngari.recipe.entity.RecipeExtend;
import ctd.persistence.DAOFactory;
import ctd.persistence.exception.DAOException;
import ctd.spring.AppDomainContext;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.ApplicationUtils;
import recipe.bean.EleInvoiceDTO;
import recipe.dao.RecipeExtendDAO;
import recipe.util.DateConversion;

import java.util.ArrayList;
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


    @RpcService
    public List<String> findEleInvoice(EleInvoiceDTO eleInvoiceDTO){
        LOGGER.info("EleInvoiceService.findEleInvoice 入参eleInvoiceDTO=[{}]",JSONUtils.toString(eleInvoiceDTO));
        validateParam(eleInvoiceDTO);
        if("0".equals(eleInvoiceDTO.getType())){
            IConsultExService iConsultExService = AppDomainContext.getBean("consult.consultExService", IConsultExService.class);
            ConsultExDTO consultExDTO = iConsultExService.getByConsultId(eleInvoiceDTO.getId());
            if(StringUtils.isNotBlank(consultExDTO.getRegisterNo())){
                eleInvoiceDTO.setGhxh(consultExDTO.getRegisterNo());
            }
            if(StringUtils.isNotBlank(consultExDTO.getCardId())){
                eleInvoiceDTO.setCardId(consultExDTO.getCardId());
            }
            if(StringUtils.isNotBlank(consultExDTO.getCardType())){
                eleInvoiceDTO.setCardType(consultExDTO.getCardType());
            }

        }
        if("1".equals(eleInvoiceDTO.getType())){
            RecipeExtendDAO recipeExtendDAO = DAOFactory.getDAO(RecipeExtendDAO.class);
            RecipeExtend recipeExtend = recipeExtendDAO.getByRecipeId(eleInvoiceDTO.getId());
            if(StringUtils.isNotBlank(recipeExtend.getRegisterID())){
                eleInvoiceDTO.setGhxh(recipeExtend.getRegisterID());
            }
            if(StringUtils.isNotBlank(recipeExtend.getCardType())){
                eleInvoiceDTO.setCardType(recipeExtend.getCardType());
            }
            if(StringUtils.isNotBlank(recipeExtend.getCardNo())){
                eleInvoiceDTO.setCardId(recipeExtend.getCardNo());
            }
        }

        PatientService patientService = BasicAPI.getService(PatientService.class);
        PatientDTO patientDTO = patientService.get(eleInvoiceDTO.getMpiid());
        EleInvoiceReqTo eleInvoiceReqTo = new EleInvoiceReqTo();
        eleInvoiceReqTo.setOrganId(eleInvoiceDTO.getOrganId());
        if(StringUtils.isNotBlank(patientDTO.getPatientName())){
            eleInvoiceReqTo.setHzxm(patientDTO.getPatientName());
        }
        if(StringUtils.isNotBlank(patientDTO.getGuardianCertificate())){
            eleInvoiceReqTo.setLxrzjh(patientDTO.getGuardianCertificate());
        }
        if(StringUtils.isNotBlank(patientDTO.getMobile())){
            eleInvoiceReqTo.setLxrdh(patientDTO.getMobile());
            eleInvoiceReqTo.setPhone(patientDTO.getMobile());
        }
        if(StringUtils.isNotBlank(patientDTO.getCertificate())){
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
        if("0".equals(eleInvoiceDTO.getType())){
            eleInvoiceReqTo.setCzybz("0");
        }
        if("1".equals(eleInvoiceDTO.getType())){
            eleInvoiceReqTo.setCzybz(null);
        }
        if(StringUtils.isNotBlank(eleInvoiceDTO.getGhxh())){
            eleInvoiceReqTo.setGhxh(eleInvoiceDTO.getGhxh());
        }else{
            throw new DAOException(609,"ghxh is null,无法获取对应电子发票");
        }
        IRecipeHisService hisService = AppDomainContext.getBean("his.iRecipeHisService", IRecipeHisService.class);
        HisResponseTO<String> hisResponseTO = null;

        LOGGER.info("EleInvoiceService.findEleInvoice 待推送数据:eleInvoiceReqTo:[{}]", JSONUtils.toString(eleInvoiceReqTo));
        hisResponseTO = hisService.queryEleInvoice(eleInvoiceReqTo);
        if(hisResponseTO != null){
            if(hisResponseTO.getMsgCode().equals("200")){
                String result = hisResponseTO.getData();
                if(StringUtils.isNotBlank(result)){
                    LOGGER.info("EleInvoiceService.findEleInvoice :result={}",result);
                   return stringToList(result);
                }else{
                    throw new DAOException(609,"当前系统繁忙，请稍后再试");
                }
            }else{
                LOGGER.info("EleInvoiceService.findEleInvoice 请求his失败，返回信息:msg={}",hisResponseTO.getMsg());
                throw new DAOException(609,hisResponseTO.getMsg());
            }
        }else{
            LOGGER.info("EleInvoiceService.findEleInvoice 请求his失败,hisResponseTo is null");
            throw new DAOException(609,"当前系统繁忙，请稍后再试");
        }

    }

    private void validateParam(EleInvoiceDTO eleInvoiceDTO){
        if(eleInvoiceDTO.getId() == null){
            throw new DAOException(609,"id is null");
        }
        if(StringUtils.isBlank(eleInvoiceDTO.getMpiid())){
            throw new DAOException(609,"mpiid is null");
        }
        if(eleInvoiceDTO.getOrganId() == null){
            throw new DAOException(609,"organId is null");
        }
        if(StringUtils.isBlank(eleInvoiceDTO.getType())){
            throw new DAOException(609,"type is null");
        }
    }

    @RpcService
    public String getEleInvoiceEnable(Integer organId,String type){
        if(organId == null){
            throw new DAOException(609,"organId is null");
        }
        if(StringUtils.isBlank(type)){
            throw new DAOException(609,"type is null");
        }
        IConfigurationCenterUtilsService configurationCenterUtils = ApplicationUtils.getBaseService(IConfigurationCenterUtilsService.class);
        String result = "";
        if("0".equals(type)){
            result = (String)configurationCenterUtils.getConfiguration(organId, "EleInvoiceFzSwitch");
        }
        if("1".equals(type)){
            result = (String)configurationCenterUtils.getConfiguration(organId, "EleInvoiceCfSwitch");
        }
        if(StringUtils.isBlank(result)){
           result = "0";
        }
        return result;
    }

    private List<String> stringToList(String str){
        List<String> result2 = new ArrayList<String>();
        if(StringUtils.isNotBlank(str)){
            Arrays.asList(str.split(","));
            Arrays.asList(StringUtils.split(str, ","));
            String[] strings = str.split(",");
            for (String string : strings) {
                result2.add(string);
            }
        }
        return  result2;
    }



}
