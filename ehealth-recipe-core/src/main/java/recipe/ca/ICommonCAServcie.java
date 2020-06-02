package recipe.ca;

import com.ngari.his.ca.model.*;
import ctd.util.annotation.RpcService;


public interface ICommonCAServcie {

    /**
     * CA用户接口
     * @param requestTO
     * @return
     */
    @RpcService
    boolean caUserBusiness(CaAccountRequestTO requestTO);

    /**
     * CA证书接口
     * @param requestTO
     * @return
     */
    @RpcService
    CaCertificateResponseTO caCertificateBusiness(CaCertificateRequestTO requestTO);


    /**
     * CA密码接口
     * @param requestTO
     * @return
     */
    @RpcService
    boolean caPasswordBusiness(CaPasswordRequestTO requestTO);


    /**
     * CA签章接口-上传电子手签图片
     * @param requestTO
     * @return
     */
    @RpcService
    boolean caPictureBusiness(CaPictureRequestTO requestTO);

    /**
     * CA电子签名接口
     * @param requestTO
     * @return
     */
    @RpcService
    CaSignResponseTO caSignBusiness(CaSignRequestTO requestTO);

    /**
     * CA电子签章接口-业务数据添加签章
     * @param requestTO
     * @return
     */
    @RpcService
    CaSealResponseTO caSealBusiness(CaSealRequestTO requestTO);


    /**
     * CA时间戳获取接口
     * @param requestTO
     * @return
     */
    @RpcService
    CaSignDateResponseTO caSignDateBusiness(CaSignDateRequestTO requestTO);


}
