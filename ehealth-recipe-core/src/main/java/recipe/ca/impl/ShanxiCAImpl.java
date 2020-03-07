
package recipe.ca.impl;

import com.ngari.his.ca.model.CaAccountRequestTO;
import com.ngari.his.ca.model.CaPasswordRequestTO;
import com.ngari.his.ca.model.CaSealRequestTO;
import com.ngari.his.ca.model.CaSealResponseTO;
import com.ngari.patient.dto.DoctorDTO;
import com.ngari.patient.service.BasicAPI;
import com.ngari.patient.service.DoctorService;
import ctd.persistence.exception.DAOException;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import eh.base.constant.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import recipe.ca.CAInterface;
import recipe.ca.ICommonCAServcie;
import recipe.ca.vo.CaSignResultVo;

/**
 * CA标准化对接文档
 */
@RpcBean
public class ShanxiCAImpl implements CAInterface {
    private static final Logger LOGGER = LoggerFactory.getLogger(ShanxiCAImpl.class);

    @Autowired
    private ICommonCAServcie iCommonCAServcie;
    /**
     * CA用户注册、申请证书接口
     * @param doctorId
     * @return
     */
    @RpcService
    public boolean caUserLoginAndGetCertificate(Integer doctorId){
        LOGGER.info("base服务 caUserLoginAndGetCertificate start in doctorId={}", doctorId);
        //根据doctorId获取医生信息
        DoctorService doctorDAO = BasicAPI.getService(DoctorService.class);
        DoctorDTO doctor = doctorDAO.getByDoctorId(doctorId);
        if (null == doctor) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "doctorId 找不到改医生");
        }
        CaAccountRequestTO requestTO = new CaAccountRequestTO();
        requestTO.setIdCard(doctor.getIdNumber());
        requestTO.setMobile(doctor.getMobile());
        requestTO.setOrganId(doctor.getOrgan());
        requestTO.setUserAccount(doctor.getIdNumber());
        requestTO.setUserEmail(doctor.getEmail());
        requestTO.setUserName(doctor.getName());
        requestTO.setIdNoType("1");
        try {
            //用户操作类型 * 1.用户注册 * 2.用户修改 * 3.用户查询
            requestTO.setBusType(3);
            boolean isSuccess = iCommonCAServcie.caUserBusiness(requestTO);
            if (!isSuccess) {
                requestTO.setBusType(1);
                isSuccess = iCommonCAServcie.caUserBusiness(requestTO);
                LOGGER.info("base服务 caUserLoginAndGetCertificate end isSuccess={}", isSuccess);
                return isSuccess;
            }
        } catch (Exception e){
            LOGGER.error("base服务 caUserLoginAndGetCertificate 调用前置机失败 requestTO={}", JSONUtils.toString(requestTO));
            e.printStackTrace();
        }
        return false;
    }
    /**
     * CA密码接口
     * @param requestTO
     * @return
     */
    @RpcService
    public boolean caPasswordBusiness(CaPasswordRequestTO requestTO) {
        return iCommonCAServcie.caPasswordBusiness(requestTO);
    }

    /**
     * 标准化CA签名及签章接口
     * @param requestSealTO
     * @param organId
     * @param userAccount
     * @param caPassword
     */
    @RpcService
    public CaSignResultVo commonCASignAndSeal(CaSealRequestTO requestSealTO, Integer organId, String userAccount, String caPassword) {
        LOGGER.info("recipe服务 commonCASignAndSeal start requestSealTO={},organId={},userAccount={},caPassword={}", JSONUtils.toString(requestSealTO), organId, userAccount, caPassword);
        CaSignResultVo signResultVo = new CaSignResultVo();
        //获取处方pdf数据
        requestSealTO.setOrganId(organId);
        requestSealTO.setUserAccount(userAccount);
        requestSealTO.setUserPin(caPassword);
        requestSealTO.setCertMsg(null);
        requestSealTO.setRightX(1);
        requestSealTO.setRightY(1);
        requestSealTO.setKeyWord("");
        requestSealTO.setSzIndexes(0);

        //电子签名
        signResultVo.setSignRecipeCode(null);
        //上传手签图片
        try{
//            iCommonCAServcie.caPictureBusiness();
        } catch (Exception e){
            LOGGER.error("esign 服务 caPictureBusiness 调用前置机失败 requestTO={}", requestSealTO.toString());
            e.printStackTrace();
        }
        //获取时间戳数据
        String signCADate=null;
        signResultVo.setSignCADate(signCADate);
        //电子签章业务
        try {
            CaSealResponseTO responseSealTO = iCommonCAServcie.caSealBusiness(requestSealTO);
            LOGGER.info("esign 服务 commonCASignAndSeal  responseSealTO={}", JSONUtils.toString(responseSealTO).substring(0,100));
            if (responseSealTO != null){
                signResultVo.setPdfBase64(responseSealTO.getPdfBase64File());
            }
        } catch (Exception e){
            LOGGER.error("esign 服务 commonCASignAndSeal 调用前置机失败 requestTO={}", requestSealTO.toString());
            e.printStackTrace();
        }
        return signResultVo;
    }




}
