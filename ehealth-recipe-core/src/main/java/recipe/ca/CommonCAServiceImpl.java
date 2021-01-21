package recipe.ca;

import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.ca.model.*;
import com.ngari.his.ca.service.ICaHisService;
import ctd.util.AppContextHolder;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RpcBean("iCommonCAServcie")
public class CommonCAServiceImpl implements ICommonCAServcie {

    private static Logger LOGGER = LoggerFactory.getLogger(CommonCAServiceImpl.class);

    private static final String CA_RESULT_CODE = "200";

    private static ICaHisService iCaHisService = AppContextHolder.getBean("his.iCaHisService",ICaHisService.class);

    /**
     * CA用户接口
     * @param requestTO
     * @return
     */
    @Override
    public boolean caUserBusiness(CaAccountRequestTO requestTO) {
        try {
            LOGGER.info("CommonCAServiceImpl caUserBusiness start userAccount={},requestTO={}",requestTO.getUserAccount(), JSONUtils.toString(requestTO));
            HisResponseTO<CaAccountResponseTO> responseTO = iCaHisService.caUserBusiness(requestTO);
            LOGGER.info("CommonCAServiceImpl caUserBusiness userAccount={} responseTO={}",requestTO.getUserAccount(), JSONUtils.toString(responseTO));
            if (CA_RESULT_CODE.equals(responseTO.getMsgCode())) {
                return true;
            }
        } catch (Exception e){
            LOGGER.error("CommonCAServiceImpl caUserBusiness 调用前置机失败 userAccount={} ,requestTO={},errorInfo={}", requestTO.getUserAccount(),JSONUtils.toString(requestTO),e.getMessage(),e);
            e.printStackTrace();
            return false;
        }
        return false;
    }

    @Override
    public CaAccountResponseTO caUserBusinessNew(CaAccountRequestTO requestTO) {
        CaAccountResponseTO response = new CaAccountResponseTO();
        try {
            LOGGER.info("CommonCAServiceImpl caUserBusinessNew requestTO=[{}]",JSONUtils.toString(requestTO));
            HisResponseTO<CaAccountResponseTO> responseTO = iCaHisService.caUserBusiness(requestTO);
            LOGGER.info("CommonCAServiceImpl caUserBusinessNew responseTO={}",JSONUtils.toString(responseTO));
            if (CA_RESULT_CODE.equals(responseTO.getMsgCode())){
                //response = responseTO.getData();
               response.setUserAccount(responseTO.getData().getUserAccount());
            }
            response.setCode(Integer.valueOf(responseTO.getMsgCode()));
            response.setMsg(responseTO.getMsg());
        } catch (Exception e) {
            LOGGER.error("CommonCAServiceImpl caUserBusinessNew 调用前置机失败 errorInfo=[{}]",e);
        }
        return response;
    }

    /**
     * CA证书接口
     * @param requestTO
     * @return
     */
    @Override
    public CaCertificateResponseTO caCertificateBusiness(CaCertificateRequestTO requestTO) {
        CaCertificateResponseTO responseRs = new CaCertificateResponseTO();
        try {
            LOGGER.info("CommonCAServiceImpl caCertificateBusiness start userAccount={},requestTO={}",requestTO.getUserAccount(), JSONUtils.toString(requestTO));
            HisResponseTO<CaCertificateResponseTO> responseTO = iCaHisService.caCertificateBusiness(requestTO);
            LOGGER.info("CommonCAServiceImpl caCertificateBusiness userAccount={},responseTO={}", requestTO.getUserAccount(),JSONUtils.toString(responseTO));
            if (CA_RESULT_CODE.equals(responseTO.getMsgCode())) {
                responseRs.setCretBody(responseTO.getData().getCretBody());
                responseRs.setUserAccount(responseTO.getData().getUserAccount());
                responseRs.setCretSerial(responseTO.getData().getCretSerial());
            }
            responseRs.setCode(Integer.valueOf(responseTO.getMsgCode()));
            responseRs.setMsg(responseTO.getMsg());
        } catch (Exception e){
            LOGGER.error("CommonCAServiceImpl caCertificateBusiness 调用前置机失败 userAccount={},requestTO={},errorInfo={}",requestTO.getUserAccount(), JSONUtils.toString(requestTO), e.getMessage(),e);
            e.printStackTrace();
        }
        return responseRs;
    }

    /**
     * CA密码接口
     * @param requestTO
     * @return
     */
    public boolean caPasswordBusiness(CaPasswordRequestTO requestTO){
        try {
            LOGGER.info("CommonCAServiceImpl caPasswordBusiness start userAccount={}, requestTO={}",requestTO.getUserAccount(), JSONUtils.toString(requestTO));
            HisResponseTO responseTO = iCaHisService.caPasswordBusiness(requestTO);
            LOGGER.info("CommonCAServiceImpl caPasswordBusiness  userAccount={}, responseTO={}",requestTO.getUserAccount(), JSONUtils.toString(responseTO));
            if (CA_RESULT_CODE.equals(responseTO.getMsgCode())) {
                return true;
            }
        } catch (Exception e){
            LOGGER.error("CommonCAServiceImpl caPasswordBusiness 调用前置机失败 userAccount={}, requestTO={},errorInfo={}", requestTO.getUserAccount(), JSONUtils.toString(requestTO),e.getMessage(),e);
            e.printStackTrace();
            return false;
        }
        return false;

    }

    /**
     * CA手签图片接口
     * @param requestTO
     * @return
     */
    public boolean caPictureBusiness(CaPictureRequestTO requestTO){
        try {
            LOGGER.info("CommonCAServiceImpl caPictureBusiness start userAccount={}, requestTO={}",requestTO.getUserAccount(), JSONUtils.toString(requestTO));
            HisResponseTO responseTO = iCaHisService.caPictureBusiness(requestTO);
            LOGGER.info("CommonCAServiceImpl caPictureBusiness userAccount={}, responseTO={}", requestTO.getUserAccount(),JSONUtils.toString(responseTO));
            if (CA_RESULT_CODE.equals(responseTO.getMsgCode())) {
                return true;
            }
        } catch (Exception e){
            LOGGER.error("CommonCAServiceImpl caPictureBusiness 调用前置机失败 userAccount={}, requestTO={}", requestTO.getUserAccount(),JSONUtils.toString(requestTO),e);
            e.printStackTrace();
            return false;
        }
        return false;
    }

    /**
     * CA电子签名接口
     * @param requestTO
     * @return
     */
    public CaSignResponseTO caSignBusiness(CaSignRequestTO requestTO) {
        CaSignResponseTO responseRs = new CaSignResponseTO();
        try {
            LOGGER.info("CommonCAServiceImpl caSignBusiness start userAccount={}, requestTO={}", requestTO.getUserAccount(), JSONUtils.toString(requestTO));
            HisResponseTO<CaSignResponseTO> responseTO = iCaHisService.caSignBusiness(requestTO);
            LOGGER.info("CommonCAServiceImpl caSignBusiness  userAccount={}, responseTO={}",requestTO.getUserAccount(), JSONUtils.toString(responseTO));
            if (CA_RESULT_CODE.equals(responseTO.getMsgCode())) {
                responseRs.setSignValue(responseTO.getData().getSignValue());
                responseRs.setUserAccount(responseTO.getData().getUserAccount());
            }
            responseRs.setCode(Integer.valueOf(responseTO.getMsgCode()));
            responseRs.setMsg(responseTO.getMsg());
        } catch (Exception e){
            LOGGER.error("CommonCAServiceImpl caSignBusiness 调用前置机失败 userAccount={}, requestTO={},errorInfo={}", requestTO.getUserAccount(), JSONUtils.toString(requestTO), e.getMessage(),e);
            e.printStackTrace();
            return responseRs;
        }
        return responseRs;
    }

    /**
     * CA电子签章接口
     * @param requestTO
     * @return
     */
    public CaSealResponseTO caSealBusiness(CaSealRequestTO requestTO) {
        CaSealResponseTO responseRs = new CaSealResponseTO();
        try {
            LOGGER.info("CommonCAServiceImpl caSealBusiness start userAccount={}, requestTO={}",requestTO.getUserAccount(), JSONUtils.toString(requestTO));
            HisResponseTO<CaSealResponseTO> responseTO = iCaHisService.caSealBusiness(requestTO);
            LOGGER.info("CommonCAServiceImpl caSealBusiness  userAccount={}, responseTO={}",requestTO.getUserAccount(), JSONUtils.toString(responseTO));
            if (CA_RESULT_CODE.equals(responseTO.getMsgCode())) {
                responseRs.setPdfBase64File(responseTO.getData().getPdfBase64File());
                responseRs.setUserAccount(responseTO.getData().getUserAccount());
            }
            responseRs.setCode(Integer.valueOf(responseTO.getMsgCode()));
            responseRs.setMsg(responseTO.getMsg());
        } catch (Exception e){
            LOGGER.error("CommonCAServiceImpl caSealBusiness 调用前置机失败 userAccount={}, requestTO={},errorInfo={}",requestTO.getUserAccount(), JSONUtils.toString(requestTO), e.getMessage(),e);
            e.printStackTrace();
            return responseRs;
        }
        return responseRs;
    }
    /**
     * CA时间戳获取接口
     * @param requestTO
     * @return
     */
    public CaSignDateResponseTO caSignDateBusiness(CaSignDateRequestTO requestTO) {
        CaSignDateResponseTO responseRs = new CaSignDateResponseTO();
        try {
            LOGGER.info("CommonCAServiceImpl caSignDateBusiness start userAccount={}, requestTO={}",requestTO.getUserAccount(), JSONUtils.toString(requestTO));
            HisResponseTO<CaSignDateResponseTO> responseTO = iCaHisService.caSignDateBusiness(requestTO);
            LOGGER.info("CommonCAServiceImpl caSignDateBusiness userAccount={}, responseTO={}", requestTO.getUserAccount(),JSONUtils.toString(responseTO));
            if (CA_RESULT_CODE.equals(responseTO.getMsgCode())) {
                responseRs.setSignDate(responseTO.getData().getSignDate());
                responseRs.setUserAccount(responseTO.getData().getUserAccount());
            }

            responseRs.setCode(Integer.valueOf(responseTO.getMsgCode()));
            responseRs.setMsg(responseTO.getMsg());
        } catch (Exception e){
            LOGGER.error("CommonCAServiceImpl caSignDateBusiness 调用前置机失败 userAccount={}, requestTO={},errorInfo={}",requestTO.getUserAccount(), JSONUtils.toString(requestTO), e.getMessage(),e);
            e.printStackTrace();
            return responseRs;
        }
        return responseRs;
    }

    /**
     * 深圳Ca根据pin获取token
     * @param requestTO
     * @return
     */
    @Override
    public CaPasswordResponseTO caTokenBusiness(CaPasswordRequestTO requestTO) {
        CaPasswordResponseTO caPasswordResponseTO = new CaPasswordResponseTO();
        try {
            LOGGER.info("CommonCAServiceImpl caTokenBusiness start userAccount={}", requestTO.getUserAccount());
            HisResponseTO<CaPasswordResponseTO> responseTO = iCaHisService.caPasswordBusiness(requestTO);
            LOGGER.info("CommonCAServiceImpl caTokenBusiness userAccount={},responseTO={}", requestTO.getUserAccount(), JSONUtils.toString(responseTO));
            if (CA_RESULT_CODE.equals(responseTO.getMsgCode())) {
                caPasswordResponseTO.setValue(responseTO.getData().getValue());
            }
            caPasswordResponseTO.setCode(Integer.valueOf(responseTO.getMsgCode()));
            caPasswordResponseTO.setMsg(responseTO.getMsg());
        } catch (Exception e) {
            LOGGER.error("CommonCAServiceImpl getTokenByAccountPin error={}", e);
            e.getMessage();
            return caPasswordResponseTO;
        }
        return caPasswordResponseTO;
    }

    @Override
    public CaPictureResponseTO newCaPictureBusiness(CaPictureRequestTO requestTO) {
        CaPictureResponseTO caPictureResponseTO = new CaPictureResponseTO();
        try {
            LOGGER.info("CommonCAServiceImpl newCaPictureBusiness start userAccount={}, requestTO={}", requestTO.getUserAccount(), JSONUtils.toString(requestTO));
            HisResponseTO<CaPictureResponseTO> responseTO = iCaHisService.caPictureBusiness(requestTO);
            LOGGER.info("CommonCAServiceImpl newCaPictureBusiness userAccount={}, responseTO={}", requestTO.getUserAccount(), JSONUtils.toString(responseTO));
            if (CA_RESULT_CODE.equals(responseTO.getMsgCode())) {
                caPictureResponseTO.setCaPicture(responseTO.getData().getCaPicture());
            }
            caPictureResponseTO.setCode(Integer.valueOf(responseTO.getMsgCode()));
            caPictureResponseTO.setMsg(responseTO.getMsg());
        } catch (Exception e) {
            LOGGER.error("CommonCAServiceImpl caPictureBusiness 调用前置机失败 userAccount={}, requestTO={}", requestTO.getUserAccount(), JSONUtils.toString(requestTO), e);
            e.printStackTrace();
            return caPictureResponseTO;
        }
        return caPictureResponseTO;
    }

    @Override
    public CaTokenResponseTo newCaTokenBussiness(CaTokenRequestTo requestTo) {
        CaTokenResponseTo caTokenResponseTo = new CaTokenResponseTo();
        try {
            LOGGER.info("CommonCAServiceImpl newCaTokenBussiness start  requestTO={}", JSONUtils.toString(requestTo));
            HisResponseTO<CaTokenResponseTo> responseTO = iCaHisService.caTokenBusiness(requestTo);
            LOGGER.info("CommonCAServiceImpl newCaPictureBusiness  responseTO={}",JSONUtils.toString(responseTO));
            if (CA_RESULT_CODE.equals(responseTO.getMsgCode())) {
                caTokenResponseTo.setToken(responseTO.getData().getToken());
                caTokenResponseTo.setExpireTime(responseTO.getData().getExpireTime());
            }
            caTokenResponseTo.setCode(Integer.valueOf(responseTO.getMsgCode()));
            caTokenResponseTo.setMsg(responseTO.getMsg());
        } catch (Exception e) {
            LOGGER.error("CommonCAServiceImpl caPictureBusiness 调用前置机失败 errorInfo={}", e);
            e.printStackTrace();
            return caTokenResponseTo;
        }
        return caTokenResponseTo;
    }

    @Override
    public CaAutoSignResponseTO caAutoSignBusiness(CaAutoSignRequestTO requestTO) {
        CaAutoSignResponseTO result = new CaAutoSignResponseTO();
        try {
            LOGGER.info("CommonCAServiceImpl CaAutoSignBusiness start request=[{}]",JSONUtils.toString(requestTO));
            HisResponseTO<CaAutoSignResponseTO> response = iCaHisService.caAutoSignBusiness(requestTO);
            LOGGER.info("CommonCAServiceImpl CaAutoSignBusiness start response=[{}]",JSONUtils.toString(response));

            if (CA_RESULT_CODE.equals(response.getMsgCode())){
                result=response.getData();
//                result.setAutoSign(response.getData().getAutoSign());
//                result.setAutoSignPicture(response.getData().getAutoSignPicture());
            }
            result.setCode(Integer.valueOf(response.getMsgCode()));
            result.setMsg(response.getMsg());
        } catch (NumberFormatException e) {
            LOGGER.error("CommonCAServiceImpl caAutoSignBusiness 调用前置机失败 errorInfo={}", e);
            e.printStackTrace();
        }
        return result;
    }
}
