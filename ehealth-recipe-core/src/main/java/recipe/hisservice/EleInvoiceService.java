package recipe.hisservice;

import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.recipe.mode.EleInvoiceReqTo;
import com.ngari.his.recipe.service.IRecipeHisService;
import com.ngari.patient.dto.PatientDTO;
import com.ngari.patient.service.BasicAPI;
import com.ngari.patient.service.PatientService;
import ctd.persistence.exception.DAOException;
import ctd.spring.AppDomainContext;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.bean.EleInvoiceDTO;
import recipe.util.DateConversion;



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
    public String findEleInvoice(EleInvoiceDTO eleInvoiceDTO){
        LOGGER.info("EleInvoiceService.findEleInvoice 入参eleInvoiceDTO=[{}]",JSONUtils.toString(eleInvoiceDTO));
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
        }else{
            eleInvoiceReqTo.setCzybz(null);
        }
        if(StringUtils.isBlank(eleInvoiceDTO.getGhxh())){
            eleInvoiceReqTo.setGhxh("0");
        }
        IRecipeHisService hisService = AppDomainContext.getBean("his.iRecipeHisService", IRecipeHisService.class);
        HisResponseTO<String> hisResponseTO = null;
        try{
            LOGGER.info("EleInvoiceService.findEleInvoice 待推送数据:eleInvoiceReqTo:[{}]", JSONUtils.toString(eleInvoiceReqTo));
            hisResponseTO = hisService.queryEleInvoice(eleInvoiceReqTo);
            if(hisResponseTO != null){
                if(hisResponseTO.getMsgCode().equals("200")){
                    String result = hisResponseTO.getData();
                    LOGGER.info("EleInvoiceService.findEleInvoice :result={}",result);
                    return result;
                }else{
                    LOGGER.info("EleInvoiceService.findEleInvoice 请求his失败，返回信息:msg={}",hisResponseTO.getMsg());
                }
            }else{
                LOGGER.info("EleInvoiceService.findEleInvoice 请求his失败,hisResponseTo is null");
            }
        }catch (Exception e){
            LOGGER.error("EleInvoiceService.findEleInvoice:e:{}",e);
        }
         return "";
    }

    private void validateParam(EleInvoiceDTO eleInvoiceDTO){
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

}
