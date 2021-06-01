package recipe.third;

import ctd.util.annotation.RpcService;

/**
 * @author gaomw
 */
public interface HztServiceInterface {

    /**
     * 获取用户杭州通token
     *
     * @param
     * @return
     */
    @RpcService
    String findSMKTokenForPay(String mpiId, String sendChl);

}
