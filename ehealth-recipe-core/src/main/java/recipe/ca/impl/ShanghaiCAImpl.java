
package recipe.ca.impl;

import com.ngari.his.ca.model.*;
import com.ngari.patient.dto.DoctorDTO;
import com.ngari.patient.dto.DoctorExtendDTO;
import com.ngari.patient.service.BasicAPI;
import com.ngari.patient.service.DoctorExtendService;
import com.ngari.patient.service.DoctorService;
import com.ngari.recipe.entity.Recipe;
import ctd.util.AppContextHolder;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.ApplicationUtils;
import recipe.ca.CAInterface;
import recipe.ca.ICommonCAServcie;
import recipe.ca.vo.CaSignResultVo;

/**
 * CA标准化对接文档
 */
@RpcBean("shanghaiCA")
public class ShanghaiCAImpl implements CAInterface {
    private static final Logger LOGGER = LoggerFactory.getLogger(ShanghaiCAImpl.class);

    private ICommonCAServcie iCommonCAServcie= AppContextHolder.getBean("iCommonCAServcie", ICommonCAServcie.class);

    private DoctorService doctorService = ApplicationUtils.getBasicService(DoctorService.class);
    /**
     * CA用户注册、申请证书接口
     * @param doctorId
     * @return
     */
    @RpcService
    public boolean caUserLoginAndGetCertificate(Integer doctorId){
        DoctorDTO doctorDTO = doctorService.getByDoctorId(doctorId);

        CaAccountRequestTO caAccountRequestTO = new CaAccountRequestTO();
        caAccountRequestTO.setOrganId(doctorDTO.getOrgan());
        caAccountRequestTO.setUserName(doctorDTO.getName());
        return iCommonCAServcie.caUserBusiness(caAccountRequestTO);
    }

    /**
     * CA密码接口
     * @param requestTO
     * @return
     */
    @RpcService
    public boolean caPasswordBusiness(CaPasswordRequestTO requestTO) {
        return true;
    }

    /**
     * 标准化CA签名及签章接口
     * @param requestSealTO
     * @param organId
     * @param userAccount
     * @param caPassword
     */
    @RpcService
    public CaSignResultVo commonCASignAndSeal(CaSealRequestTO requestSealTO, Recipe recipe, Integer organId, String userAccount, String caPassword) {
        LOGGER.info("shanghaiCA commonCASignAndSeal start requestSealTO={},organId={},userAccount={},caPassword={}",
                JSONUtils.toString(requestSealTO), organId, userAccount, caPassword);
        CaSignResultVo signResultVo = new CaSignResultVo();
        try {
            //电子签名
            CaSignRequestTO caSignRequestTO = new CaSignRequestTO();
            caSignRequestTO.setCretMsg(null);
            caSignRequestTO.setOrganId(organId);
            caSignRequestTO.setSignMsg(JSONUtils.toString(recipe));
            caSignRequestTO.setUserAccount(userAccount);
            CaSignResponseTO responseTO = iCommonCAServcie.caSignBusiness(caSignRequestTO);
            if (responseTO == null || responseTO.getCode() != 200) {
                signResultVo.setCode(responseTO.getCode());
                signResultVo.setMsg(responseTO.getMsg());
                return signResultVo;
            }
            signResultVo.setSignRecipeCode(responseTO.getSignValue());
            //上传手签图片(暂不实现)

            //获取时间戳数据
            CaSignDateRequestTO caSignDateRequestTO = new CaSignDateRequestTO();
            caSignDateRequestTO.setOrganId(organId);
            caSignDateRequestTO.setUserAccount(userAccount);
            caSignDateRequestTO.setSignMsg(JSONUtils.toString(recipe));
            String signId = "CA_" + recipe.getRecipeId() + "_" + "";

            CaSignDateResponseTO responseDateTO = iCommonCAServcie.caSignDateBusiness(caSignDateRequestTO);
            if (responseDateTO == null || responseDateTO.getCode() != 200) {
                signResultVo.setCode(responseDateTO.getCode());
                signResultVo.setMsg(responseDateTO.getMsg());
                return signResultVo;
            }
            signResultVo.setSignCADate(responseDateTO.getSignDate());
            signResultVo.setCode(200);

            // 电子签章
            requestSealTO.setOrganId(organId);
            requestSealTO.setUserPin(caPassword);
            requestSealTO.setUserAccount(userAccount);
            DoctorExtendService doctorExtendService = BasicAPI.getService(DoctorExtendService.class);
            DoctorExtendDTO doctorExtendDTO = doctorExtendService.getByDoctorId(recipe.getChecker());
            if (doctorExtendDTO != null && doctorExtendDTO.getSealData() != null) {
                requestSealTO.setSealBase64Str(doctorExtendDTO.getSealData());
            } else {
                requestSealTO.setSealBase64Str("");
            }
            CaSealResponseTO responseSealTO = iCommonCAServcie.caSealBusiness(requestSealTO);

            if (responseSealTO == null || (responseSealTO.getCode() != 200
                    && requestSealTO.getCode() != 404 && requestSealTO.getCode() != 405)){
                signResultVo.setCode(responseSealTO.getCode());
                signResultVo.setMsg(responseSealTO.getMsg());
                return signResultVo;
            }
            signResultVo.setPdfBase64(responseSealTO.getPdfBase64File());
        } catch (Exception e){
            LOGGER.error("shanghaiCA commonCASignAndSeal 调用前置机失败 requestSealTO={},organId={},userAccount={},caPassword={}",
                    JSONUtils.toString(requestSealTO), organId, userAccount, caPassword );
            e.printStackTrace();
        }
        LOGGER.info("commonCASignAndSeal params: {}", JSONUtils.toString(signResultVo));
        return signResultVo;
    }
}
