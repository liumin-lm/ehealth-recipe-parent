package recipe.sign;

import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;

import java.util.HashMap;
import java.util.Map;

/**
 * @ClassName SignToThirdService
 * @Description
 * @Author maoLy
 * @Date 2020/6/4
 **/
@RpcBean
public class SignToThirdService {

    @RpcService
    public Map<String,Object> getCaSignToThird(){
        Map<String,Object> returnMap = new HashMap<>();
        returnMap.put("signValue","sjflsajkldfjajlfajklfjsdakljfkldjaf");
        return returnMap;
    }
}