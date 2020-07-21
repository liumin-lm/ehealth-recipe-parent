package recipe.ca.remote;

import com.ngari.recipe.entity.Recipe;
import ctd.util.annotation.RpcService;
import recipe.ca.vo.CaSignResultVo;

public interface ICARemoteService {

    /**
     * CA用户注册、申请证书接口
     * @param doctorId
     * @return
     */
    @RpcService
    boolean caUserLoginAndGetCertificate(Integer doctorId);

    /**
     *  CA密码接口
     * @param doctorId
     * @param password
     * @param newPassword
     * @param busType
     * @return
     */
    @RpcService
    boolean caPasswordBusiness(Integer doctorId,String password,String newPassword,int busType);


    @RpcService
    CaSignResultVo commonCASignAndSeal(Integer doctorId,Integer bussId ,Integer bussType);

}
