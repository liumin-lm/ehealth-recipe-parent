
package recipe.ca.impl;

import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.ca.model.CaPasswordRequestTO;
import com.ngari.his.ca.model.CaSealRequestTO;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
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
public class ShanghaiCAImpl implements CAInterface {
    private static final Logger LOGGER = LoggerFactory.getLogger(ShanghaiCAImpl.class);

    @Autowired
    private ICommonCAServcie iCommonCAServcie;
    /**
     * CA用户注册、申请证书接口
     * @param doctorId
     * @return
     */
    @RpcService
    public boolean caUserLoginAndGetCertificate(Integer doctorId){
       return true;
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
        requestSealTO.setCertMsg(null);
        requestSealTO.setRightX(1);
        requestSealTO.setRightY(1);
        requestSealTO.setKeyWord("");
        requestSealTO.setSzIndexes(0);
        //电子签名
        signResultVo.setSignRecipeCode(null);
        //获取时间戳数据
        String signCADate = null;
        signResultVo.setSignCADate(signCADate);
        return signResultVo;
    }
}
