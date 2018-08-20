package recipe.thread;

import com.google.common.collect.Maps;
import com.ngari.recipe.entity.DrugsEnterprise;
import ctd.persistence.DAOFactory;
import ctd.util.JSONUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.dao.DrugsEnterpriseDAO;
import recipe.util.HttpHelper;
import recipe.util.MapValueUtil;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * 更新药企token Callable
 * company: ngarihealth
 * @author: 0184/yu_yun
 * @date:2016/6/15.
 */
public class UpdateDrugsEpCallable implements Callable<String> {

    /** logger */
    private static final Logger logger = LoggerFactory.getLogger(UpdateDrugsEpCallable.class);

    private Integer _drugsEnterpriseId;

    public UpdateDrugsEpCallable(Integer drugsEnterpriseId) {
        this._drugsEnterpriseId = drugsEnterpriseId;
    }

    @Override
    public String call() throws Exception {

        if (null == this._drugsEnterpriseId) {
            return null;
        }

        DrugsEnterpriseDAO drugsEnterpriseDAO = DAOFactory.getDAO(DrugsEnterpriseDAO.class);
        DrugsEnterprise drugsEnterprise = drugsEnterpriseDAO.getById(this._drugsEnterpriseId);
        if (null != drugsEnterprise && StringUtils.isNotEmpty(drugsEnterprise.getAuthenUrl())) {
            String logPrefix = "UpdateDrugsEpCallable 更新药企token功能，药企ID:" + this._drugsEnterpriseId + "(" + drugsEnterprise.getName() + ")***";
            Map<String, Object> map = Maps.newHashMap();
            map.put("userid", drugsEnterprise.getUserId());
            map.put("password", drugsEnterprise.getPassword());

            try {
                if(-1 != drugsEnterprise.getAuthenUrl().indexOf("http:")) {
                    String backMsg = HttpHelper.doPost(drugsEnterprise.getAuthenUrl(), JSONUtils.toString(map));
                    logger.info(logPrefix + "药企返回：" + backMsg);
                    if (StringUtils.isNotEmpty(backMsg)) {
                        Map backMap = JSONUtils.parse(backMsg, Map.class);
                        // code 1成功
                        String code = "code";
                        if (1 == MapValueUtil.getInteger(backMap, code)) {
                            //成功
                            String token = MapValueUtil.getString(backMap, "access_token");
                            if (StringUtils.isNotEmpty(token)) {
                                drugsEnterpriseDAO.updateTokenById(this._drugsEnterpriseId, token);
                            }
                        } else {
                            logger.warn(logPrefix + "更新返回失败. msg={}", MapValueUtil.getString(backMap, "message"));
                        }
                    }
                }
            } catch (IOException e) {
                logger.warn(logPrefix + "更新异常. exception={}", e);
            }
        } else {
//            logger.warn("UpdateDrugsEpCallable 更新药企token功能，药企ID:" + this._drugsEnterpriseId + " 药企 AuthenUrl为空");
        }

        return null;
    }
}
