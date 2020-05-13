package recipe.ca.impl;

import com.ngari.his.ca.model.*;
import com.ngari.patient.dto.DoctorDTO;
import com.ngari.patient.dto.DoctorExtendDTO;
import com.ngari.patient.service.BasicAPI;
import com.ngari.patient.service.DoctorExtendService;
import com.ngari.patient.service.DoctorService;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.sign.SignDoctorCaInfo;
import ctd.util.AppContextHolder;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import recipe.ApplicationUtils;
import recipe.ca.CAInterface;
import recipe.ca.ICommonCAServcie;
import recipe.ca.factory.CommonCAFactory;
import recipe.ca.vo.CaSignResultVo;
import recipe.dao.sign.SignDoctorCaInfoDAO;

import java.util.Date;

@RpcBean("tianjinCA")
public class TianjinCAImpl implements CAInterface {
    private static final Logger LOGGER = LoggerFactory.getLogger(TianjinCAImpl.class);
    private ICommonCAServcie iCommonCAServcie= AppContextHolder.getBean("iCommonCAServcie", ICommonCAServcie.class);
    private DoctorService doctorService = ApplicationUtils.getBasicService(DoctorService.class);
    @Autowired
    private SignDoctorCaInfoDAO signDoctorCaInfoDAO;

    @Override
    @RpcService
    public boolean caUserLoginAndGetCertificate(Integer doctorId) {
        LOGGER.info("TianjinCAImpl caUserLoginAndGetCertificate start in doctorId={}", doctorId);
        DoctorDTO doctorDTO = doctorService.getByDoctorId(doctorId);

        CaAccountRequestTO caAccountRequestTO = new CaAccountRequestTO();
        caAccountRequestTO.setOrganId(doctorDTO.getOrgan());
        caAccountRequestTO.setUserName(doctorDTO.getName());
        caAccountRequestTO.setIdCard(doctorDTO.getIdNumber());
        caAccountRequestTO.setIdNoType("1");
        caAccountRequestTO.setMobile(doctorDTO.getMobile());
        caAccountRequestTO.setUserEmail(doctorDTO.getEmail());
        caAccountRequestTO.setUserAccount(doctorDTO.getLoginId());
        try {
            //用户操作类型 * 1.用户注册 * 2.用户修改 * 3.用户查询
            caAccountRequestTO.setBusType(3);
            if (!iCommonCAServcie.caUserBusiness(caAccountRequestTO)) {
                LOGGER.info("account is exist!");
                return true;
            }

            caAccountRequestTO.setBusType(1);
            boolean accountSuccess = iCommonCAServcie.caUserBusiness(caAccountRequestTO);
            LOGGER.info("TianjinCAImpl caUserBusiness end isSuccess={}", accountSuccess);

            if (accountSuccess) {
                CaCertificateRequestTO caCertificateRequestTO = new CaCertificateRequestTO();
                caCertificateRequestTO.setOrganId(doctorDTO.getOrgan());
                caCertificateRequestTO.setUserAccount(doctorDTO.getLoginId());
                caCertificateRequestTO.setBusType(1);
                CaCertificateResponseTO caCertificateResponseTO = iCommonCAServcie.caCertificateBusiness(caCertificateRequestTO);
                LOGGER.info("TianjinCAImpl caCertificateBusiness end response={}", JSONUtils.toString(caCertificateResponseTO));

                SignDoctorCaInfo caInfo = signDoctorCaInfoDAO.getDoctorSerCodeByDoctorIdAndType(doctorId, CommonCAFactory.CA_TYPE_TIANJIN);
                LOGGER.info("TianjinCAImpl getDoctorSerCodeByDoctorIdAndType end response={}", JSONUtils.toString(caInfo));
                if (null == caInfo) {
                    caInfo = new SignDoctorCaInfo();
                    caInfo.setCert_voucher(caCertificateResponseTO.getCretBody());
                    caInfo.setCaType(CommonCAFactory.CA_TYPE_TIANJIN);
                    caInfo.setCreateDate(new Date());
                    caInfo.setLastmodify(new Date());
                    caInfo.setDoctorId(doctorId);
                    caInfo.setIdcard(doctorDTO.getIdNumber());
                    caInfo.setName(doctorDTO.getName());
                }else if (StringUtils.isEmpty(caInfo.getCert_voucher())) {
                    caInfo.setCert_voucher(caCertificateResponseTO.getCretBody());
                }
                signDoctorCaInfoDAO.save(caInfo);
                return true;
            }
        } catch (Exception e){
            LOGGER.error("TianjinCAImpl caUserLoginAndGetCertificate 调用前置机失败 requestTO={},errorInfo={}", JSONUtils.toString(caAccountRequestTO), e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    @Override
    @RpcService
    public boolean caPasswordBusiness(CaPasswordRequestTO requestTO) {
        return iCommonCAServcie.caPasswordBusiness(requestTO);
    }

    @Override
    @RpcService
    public CaSignResultVo commonCASignAndSeal(CaSealRequestTO requestSealTO, Recipe recipe, Integer organId, String userAccount, String caPassword) {
        LOGGER.info("TianjinCAImpl commonCASignAndSeal start requestSealTO={},organId={},userAccount={},caPassword={}",
                JSONUtils.toString(requestSealTO), organId, userAccount, caPassword);
        CaSignResultVo signResultVo = new CaSignResultVo();

        try {
            //电子签名
            CaSignRequestTO caSignRequestTO = new CaSignRequestTO();
            SignDoctorCaInfo caInfo =
                    signDoctorCaInfoDAO.getDoctorSerCodeByDoctorIdAndType(recipe.getDoctor(), CommonCAFactory.CA_TYPE_TIANJIN);
            if (null != caInfo) {
                caSignRequestTO.setCertVoucher(caInfo.getCert_voucher());
            }
            caSignRequestTO.setOrganId(organId);
            caSignRequestTO.setSignMsg(JSONUtils.toString(recipe));
            caSignRequestTO.setUserAccount(userAccount);
            CaSignResponseTO responseTO = iCommonCAServcie.caSignBusiness(caSignRequestTO);
            if (responseTO == null || responseTO.getCode() != 200) {
                signResultVo.setCode(responseTO.getCode());
                signResultVo.setMsg(responseTO.getMsg());
                LOGGER.error("caSignBusiness Romote error, signResultVo={}", JSONUtils.toString(signResultVo));
                return signResultVo;
            }
            signResultVo.setSignRecipeCode(responseTO.getSignValue());

            //获取时间戳数据
            CaSignDateRequestTO caSignDateRequestTO = new CaSignDateRequestTO();
            caSignDateRequestTO.setOrganId(organId);
            caSignDateRequestTO.setUserAccount(userAccount);
            caSignDateRequestTO.setSignMsg(JSONUtils.toString(recipe));

            CaSignDateResponseTO responseDateTO = iCommonCAServcie.caSignDateBusiness(caSignDateRequestTO);
            if (responseDateTO == null || responseDateTO.getCode() != 200) {
                signResultVo.setCode(responseDateTO.getCode());
                signResultVo.setMsg(responseDateTO.getMsg());
                LOGGER.error("caSignDateBusiness Romote error, signResultVo={}", JSONUtils.toString(signResultVo));
                return signResultVo;
            }
            signResultVo.setSignCADate(responseDateTO.getSignDate());

            // 电子签章
            requestSealTO.setOrganId(organId);
            requestSealTO.setUserPin(caPassword);
            requestSealTO.setUserAccount(userAccount);
            requestSealTO.setCertVoucher(caInfo.getCert_voucher());
            DoctorExtendService doctorExtendService = BasicAPI.getService(DoctorExtendService.class);
            DoctorExtendDTO doctorExtendDTO = doctorExtendService.getByDoctorId(recipe.getChecker());
            if (doctorExtendDTO != null && doctorExtendDTO.getSealData() != null) {
                requestSealTO.setSealBase64Str(doctorExtendDTO.getSealData());
            } else {
                requestSealTO.setSealBase64Str("");
            }
            LOGGER.info("caSealBusiness before requestSealTO={}", JSONUtils.toString(requestSealTO));
            CaSealResponseTO responseSealTO = iCommonCAServcie.caSealBusiness(requestSealTO);
            LOGGER.info("caSealBusiness end responseSealTO={}", JSONUtils.toString(responseTO));

            if (responseSealTO == null || responseSealTO.getCode() != 200){
                signResultVo.setCode(responseSealTO.getCode());
                signResultVo.setMsg(responseSealTO.getMsg());
                LOGGER.error("caSealBusiness Romote error, signResultVo={}", JSONUtils.toString(signResultVo));
                return signResultVo;
            }
            signResultVo.setPdfBase64(responseSealTO.getPdfBase64File());
        } catch (Exception e){
            LOGGER.error("shanghaiCA commonCASignAndSeal 调用前置机失败 requestSealTO={},organId={},userAccount={},caPassword={}",
                    JSONUtils.toString(requestSealTO), organId, userAccount, caPassword );
            e.printStackTrace();
        }
        return signResultVo;
    }
}
