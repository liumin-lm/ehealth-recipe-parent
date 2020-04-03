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
            HisResponseTO responseTO = iCaHisService.caUserBusiness(requestTO);
            LOGGER.info("CommonCAServiceImpl caUserBusiness userAccount={} responseTO={}",requestTO.getUserAccount(), JSONUtils.toString(responseTO));
            if (CA_RESULT_CODE.equals(responseTO.getMsgCode())) {
                return true;
            }
        } catch (Exception e){
            LOGGER.error("CommonCAServiceImpl caUserBusiness 调用前置机失败 userAccount={} ,requestTO={}", requestTO.getUserAccount(),JSONUtils.toString(requestTO));
            e.printStackTrace();
            return false;
        }
        return false;
    }
    /**
     * CA证书接口
     * @param requestTO
     * @return
     */
    @Override
    public boolean caCertificateBusiness(CaCertificateRequestTO requestTO) {
        try {
            LOGGER.info("CommonCAServiceImpl caCertificateBusiness start userAccount={},requestTO={}",requestTO.getUserAccount(), JSONUtils.toString(requestTO));
            HisResponseTO responseTO = iCaHisService.caCertificateBusiness(requestTO);
            LOGGER.info("CommonCAServiceImpl caCertificateBusiness userAccount={},responseTO={}", requestTO.getUserAccount(),JSONUtils.toString(responseTO));
            if (CA_RESULT_CODE.equals(responseTO.getMsgCode())) {
                return true;
            }
        } catch (Exception e){
            LOGGER.error("CommonCAServiceImpl caCertificateBusiness 调用前置机失败 userAccount={},requestTO={}",requestTO.getUserAccount(), JSONUtils.toString(requestTO));
            e.printStackTrace();
            return false;
        }
        return false;
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
            LOGGER.error("CommonCAServiceImpl caPasswordBusiness 调用前置机失败 userAccount={}, requestTO={}", requestTO.getUserAccount(), JSONUtils.toString(requestTO));
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
            LOGGER.error("CommonCAServiceImpl caPictureBusiness 调用前置机失败 userAccount={}, requestTO={}", requestTO.getUserAccount(),JSONUtils.toString(requestTO));
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
            LOGGER.error("CommonCAServiceImpl caSignBusiness 调用前置机失败 userAccount={}, requestTO={}", requestTO.getUserAccount(), JSONUtils.toString(requestTO));
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
            LOGGER.error("CommonCAServiceImpl caSealBusiness 调用前置机失败 userAccount={}, requestTO={}",requestTO.getUserAccount(), JSONUtils.toString(requestTO));
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
            LOGGER.error("CommonCAServiceImpl caSignDateBusiness 调用前置机失败 userAccount={}, requestTO={}",requestTO.getUserAccount(), JSONUtils.toString(requestTO));
            e.printStackTrace();
            return responseRs;
        }
        return responseRs;
    }



}
