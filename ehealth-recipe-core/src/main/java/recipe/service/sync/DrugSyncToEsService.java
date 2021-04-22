package recipe.service.sync;

import ctd.persistence.DAOFactory;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.dao.DrugListDAO;
import recipe.dao.OrganDrugListDAO;
import recipe.thread.RecipeBusiThreadPool;
import recipe.thread.SyncDrugToEsRunable;
import recipe.thread.SyncOrganDrugToEsCallable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author： 0184/yu_yun
 * @date： 2019/4/15
 * @description： 同步 base_druglist 和 base_organdruglist
 * @version： 1.0
 */
@RpcBean(value = "drugSyncToEsService", mvc_authentication = false)
public class DrugSyncToEsService {

    /**
     * logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(DrugSyncToEsService.class);

    /**
     * 单线程数据处理量
     **/
    protected static final int ONCETIME_DEAL_NUM = 200;

    /**
     * 同步base_drugList基础库数据，用于患者检索
     */
    @RpcService
    public long syncDrugList() {
        DrugListDAO drugDao = DAOFactory.getDAO(DrugListDAO.class);
        long total = drugDao.getTotalWithBase();
        LOG.info("syncDrugList 同步数量-" + total);
        if (total > 0) {
            int time = (int) Math.ceil(total / Double.parseDouble(String.valueOf(ONCETIME_DEAL_NUM)));
            for (int i = 0; i < time; i++) {
                int start = i * ONCETIME_DEAL_NUM;
                int end = start + ONCETIME_DEAL_NUM;
                if (end > total) {
                    end = (int) total;
                }
                LOG.info("syncDrugList start={}, end={}", start, end);
                try {
                    RecipeBusiThreadPool.execute(new SyncDrugToEsRunable(start, end));
                } catch (Exception e) {
                    LOG.error("syncDrugList 线程池异常",e);
                }
            }
        }

        return total;
    }

    /**
     * 同步base_organDrugList数据，用于医生端药品检索
     */
    @RpcService
    public long syncOrganDrugList() {
        OrganDrugListDAO organDrugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);
        long total = organDrugListDAO.getUsefulTotal();
        LOG.info("syncDrugList 同步数量-" + total);
        if (total > 0) {
            int time = (int) Math.ceil(total / Double.parseDouble(String.valueOf(ONCETIME_DEAL_NUM)));
            List<SyncOrganDrugToEsCallable> callableList = new ArrayList<>(time);
            for (int i = 0; i < time; i++) {
                int start = i * ONCETIME_DEAL_NUM;
                int end = start + ONCETIME_DEAL_NUM;
                if (end > total) {
                    end = (int) total;
                }
                LOG.info("syncOrganDrugList start={}, end={}", start, end);
                callableList.add(new SyncOrganDrugToEsCallable(start, end));
            }

            if (CollectionUtils.isNotEmpty(callableList)) {
                try {
                    RecipeBusiThreadPool.submitList(callableList);
                } catch (InterruptedException e) {
                    LOG.error("syncOrganDrugList 线程池异常",e);
                }
            }
        }

        return total;
    }
}
