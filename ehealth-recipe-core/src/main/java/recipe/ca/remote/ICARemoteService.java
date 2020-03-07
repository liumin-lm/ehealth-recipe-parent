package recipe.ca.remote;

import com.ngari.his.ca.model.CaPasswordRequestTO;
import ctd.util.annotation.RpcService;

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

}
