
package recipe.ca.impl;

import com.ngari.his.ca.model.*;
import com.ngari.recipe.entity.Recipe;
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
@RpcBean("shanghaiCA")
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
        LOGGER.info("recipe服务 commonCASignAndSeal start requestSealTO={},organId={},userAccount={},caPassword={}",
                JSONUtils.toString(requestSealTO), organId, userAccount, caPassword);
        CaSignResultVo signResultVo = new CaSignResultVo();
        try {
            //电子签名
          /*  CaSignRequestTO caSignRequestTO = new CaSignRequestTO();
            caSignRequestTO.setCretMsg(null);
            caSignRequestTO.setOrganId(organId);
            caSignRequestTO.setSignMsg(JSONUtils.toString(recipe));
            caSignRequestTO.setUserAccount(userAccount);
            CaSignResponseTO responseTO = iCommonCAServcie.caSignBusiness(caSignRequestTO);
            if (responseTO != null) {
                signResultVo.setSignRecipeCode(responseTO.getSignValue());
            }*/
            //上传手签图片(暂不实现)

            //获取时间戳数据
            CaSignDateRequestTO caSignDateRequestTO = new CaSignDateRequestTO();
//            caSignDateRequestTO.setOrganId(organId);
//            caSignDateRequestTO.setUserAccount(userAccount);
//            caSignDateRequestTO.setSignMsg(JSONUtils.toString(recipe));
            caSignDateRequestTO.setOrganId(1000899);
            caSignDateRequestTO.setUserAccount("342921199308101118");
            caSignDateRequestTO.setSignMsg("thisistest");

            CaSignDateResponseTO responseDateTO = iCommonCAServcie.caSignDateBusiness(caSignDateRequestTO);
            if (responseDateTO != null) {
                signResultVo.setSignCADate(responseDateTO.getSignDate());
            }

            //电子签章（暂不实现）
        } catch (Exception e){
            LOGGER.error("recipe 服务 commonCASignAndSeal 调用前置机失败 requestSealTO={},organId={},userAccount={},caPassword={}",
                    JSONUtils.toString(requestSealTO), organId, userAccount, caPassword );
            e.printStackTrace();
        }
        return signResultVo;
    }
}
