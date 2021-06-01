package recipe.thread;

import com.google.common.collect.Maps;
import com.ngari.recipe.entity.DrugsEnterprise;
import ctd.persistence.DAOFactory;
import ctd.util.JSONUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.dao.SaleDrugListDAO;
import recipe.util.HttpHelper;
import recipe.util.MapValueUtil;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * 同步药品库存
 * company: ngarihealth
 * @author: 0184/yu_yun
 * @date:2017/2/28.
 */
public class CommonSyncDrugCallable implements Callable<String> {

    private Logger logger = LoggerFactory.getLogger(CommonSyncDrugCallable.class);

    private DrugsEnterprise drugsEnterprise;

    private List<Integer> drugIdList;

    public CommonSyncDrugCallable(DrugsEnterprise drugsEnterprise, List<Integer> drugIdList) {
        this.drugsEnterprise = drugsEnterprise;
        this.drugIdList = drugIdList;
    }

    @Override
    public String call() throws Exception {
        if (null == drugsEnterprise || CollectionUtils.isEmpty(drugIdList)) {
            return null;
        }
        String logInfo = "药企id={" + this.drugsEnterprise.getId() + "},名称={"
                + drugsEnterprise.getName() + "},药品数量={" + this.drugIdList.size() + "}";
        logger.info("SyncDrugCallable start " + logInfo);
        Map<String, Object> sendMap = Maps.newHashMap();
        Map<String, Object> detailMap = Maps.newHashMap();

        sendMap.put("access_token", this.drugsEnterprise.getToken());
        sendMap.put("action", "xxxxxx");
        sendMap.put("data", detailMap);
        detailMap.put("dtl", drugIdList);

        String sendInfoStr = JSONUtils.toString(sendMap);
        logger.info("发送{}内容：" + sendInfoStr, this.drugsEnterprise.getName());

        String backMsg;
        try {
            backMsg = HttpHelper.doPost(this.drugsEnterprise.getBusinessUrl(), sendInfoStr);
            logger.info("药企返回信息: " + backMsg);
        } catch (IOException e) {
            logger.error("IOException " + e.getMessage() + "，详细数据：" + sendInfoStr,e);
            backMsg = null;
        }

        if (StringUtils.isNotEmpty(backMsg)) {
            Map backMap = JSONUtils.parse(backMsg, Map.class);
            // code 1成功
            Integer code = MapValueUtil.getInteger(backMap, "code");
            if (1 == code) {
                SaleDrugListDAO saleDrugListDAO = DAOFactory.getDAO(SaleDrugListDAO.class);
                Integer depId = this.drugsEnterprise.getId();

                Object drugInfoObj = backMap.get("dtl");
                List<Map<String, Integer>> drugInfo;
                if (drugInfoObj instanceof List) {
                    drugInfo = (List<Map<String, Integer>>) drugInfoObj;
                    if (CollectionUtils.isNotEmpty(drugInfo)) {
                        Integer goodid;
                        BigDecimal num;
                        for (Map map : drugInfo) {
                            goodid = MapValueUtil.getInteger(map, "goodid");
                            num = MapValueUtil.getBigDecimal(map, "num");
                            saleDrugListDAO.updateDrugInventory(goodid, depId, num);
                        }
                    }
                }
            } else {
                logger.error("药企返回错误信息,message={}", MapValueUtil.getString(backMap, "message"));
            }
        }

        logger.info("SyncDrugCallable end " + logInfo);
        return null;
    }
}
