package recipe.ca;

import com.ngari.his.ca.model.CaPasswordRequestTO;
import com.ngari.his.ca.model.CaSealRequestTO;
import com.ngari.recipe.entity.Recipe;
import ctd.util.annotation.RpcService;
import recipe.ca.vo.CaSignResultVo;

public interface CAInterface {

    /**
     * CA用户注册、申请证书接口
     * @param doctorId
     * @return
     */
    @RpcService
    boolean caUserLoginAndGetCertificate(Integer doctorId);

    /**
     * CA密码接口
     * @param requestTO
     * @return
     */
    @RpcService
    boolean caPasswordBusiness(CaPasswordRequestTO requestTO);

    /**
     * 标准化CA签名及签章接口
     * @param requestSealTO
     * @param organId
     * @param userAccount
     * @param caPassword
     */
    @RpcService
    CaSignResultVo commonCASignAndSeal(CaSealRequestTO requestSealTO, Recipe recipe, Integer organId, String userAccount, String caPassword);
}
