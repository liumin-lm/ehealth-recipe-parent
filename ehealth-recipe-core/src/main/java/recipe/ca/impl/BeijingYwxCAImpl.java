package recipe.ca.impl;

import com.alibaba.fastjson.JSONObject;
import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.ca.model.*;
import com.ngari.his.ca.service.ICaHisService;
import com.ngari.his.common.service.ICommonHisService;
import com.ngari.recipe.entity.Recipe;
import ctd.util.AppContextHolder;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.ca.CAInterface;
import recipe.ca.ICommonCAServcie;
import recipe.ca.vo.CaSignResultVo;
import recipe.util.RedisClient;

@RpcBean("BeijingYCA")
public class BeijingYwxCAImpl{

    private RedisClient redisClient = AppContextHolder.getBean("redisClient", RedisClient.class);
    private ICommonCAServcie iCommonCAServcie = AppContextHolder.getBean("iCommonCAServcie", ICommonCAServcie.class);
    private static ICaHisService iCaHisService = AppContextHolder.getBean("his.iCaHisService",ICaHisService.class);
    private Logger logger = LoggerFactory.getLogger(BeijingYwxCAImpl.class);
    private String AccessToken_KEY = "BeijingCa_AccessToken";


    /**
     * 获取Token接口
     * @param organId
     * @return
     */
    @RpcService
    public String CaTokenBussiness(Integer organId) {
        if (null == redisClient.get(AccessToken_KEY)) {
            CaTokenRequestTo requestTO = new CaTokenRequestTo();
            requestTO.setOrganId(organId);
            CaTokenResponseTo responseTO = iCommonCAServcie.newCaTokenBussiness(requestTO);
            if (StringUtils.isNotEmpty(responseTO.getToken()) && StringUtils.isNotEmpty(responseTO.getExpireTime())) {
                Long timeOut = Long.parseLong(responseTO.getExpireTime()) * 1000;
                redisClient.setEX(AccessToken_KEY, timeOut, responseTO.getToken());
            }
        }
        return redisClient.get(AccessToken_KEY);
    }

    @RpcService
    public CaAccountResponseTO getDocStatus(String openId,String token,Integer organId){
      CaAccountRequestTO requestTO = new CaAccountRequestTO();
      CaAccountResponseTO responseTO = new CaAccountResponseTO();
      requestTO.setOrganId(organId);
      requestTO.setUserAccount(openId);
      requestTO.setUserName(token);
      requestTO.setBusType(0);

      HisResponseTO<CaAccountResponseTO> responseTO1 =  iCaHisService.caUserBusiness(requestTO);
        logger.info("getDocStatus result info={}", JSONObject.toJSONString(responseTO1));
        if (null != responseTO1 && "200".equals(responseTO1.getMsgCode())) {

            responseTO.setUserAccount(responseTO1.getData().getUserAccount());
            responseTO.setUserStatus(responseTO1.getData().getUserStatus());
            responseTO.setExtraValue(responseTO1.getData().getExtraValue());
            responseTO.setStamp(responseTO1.getData().getStamp());
            return responseTO;

        }else {
            logger.error("前置机未返回数据");
        }

     return null;

    }
}
