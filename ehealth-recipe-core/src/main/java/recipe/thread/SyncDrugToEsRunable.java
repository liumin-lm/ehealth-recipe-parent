package recipe.thread;

import com.ngari.recipe.entity.DrugList;
import ctd.persistence.DAOFactory;
import ctd.util.AppContextHolder;
import ctd.util.JSONUtils;
import es.api.DrugSearchService;
import es.vo.PatientDrugDetailVO;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import recipe.dao.DrugListDAO;

import java.util.ArrayList;
import java.util.List;

/**
 * @author： 0184/yu_yun
 * @date： 2019/4/15
 * @description： 同步基础库药品至ES
 * @version： 1.0
 */
public class SyncDrugToEsRunable implements Runnable {

    /**
     * logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(SyncDrugToEsRunable.class);

    private int start;

    private int end;

    private int ONCE_NUM = 100;

    public SyncDrugToEsRunable(int start, int end) {
        this.start = start;
        this.end = end;
    }

    @Override
    public void run() {
        LOG.info("SyncDrugToEsRunable start");
        long start = System.currentTimeMillis();
        DrugListDAO drugDao = DAOFactory.getDAO(DrugListDAO.class);
        int total = this.end - this.start;
        if (total > 0) {
            int time = (int) Math.ceil(total / Double.parseDouble(String.valueOf(ONCE_NUM)));
            List<DrugList> drugList = null;
            for (int i = 0; i < time; i++) {
                int subStart = this.start + i * ONCE_NUM;
                drugList = drugDao.findAllForPage(subStart, ONCE_NUM);
                if (CollectionUtils.isNotEmpty(drugList)) {
                    List<PatientDrugDetailVO> updateList = new ArrayList<>(drugList.size());
                    PatientDrugDetailVO detailVo;
                    for (DrugList drug : drugList) {
                        //只更新基础药品库信息
                        if (null == drug.getSourceOrgan()) {
                            detailVo = new PatientDrugDetailVO();
                            BeanUtils.copyProperties(drug, detailVo);
                            detailVo.setDeleteFlag(0);
                            updateList.add(detailVo);
                        }
                    }

                    if (CollectionUtils.isNotEmpty(updateList)) {
                        DrugSearchService searchService = AppContextHolder.getBean("es.drugSearchService",
                                DrugSearchService.class);
                        boolean b = false;
                        try {
                            b = searchService.updatePatientDrugDetail(updateList);
                        } catch (Exception e) {
                            LOG.error("SyncDrugToEsRunable update exception! updateList={}", JSONUtils.toString(updateList), e);
                        }
                        if (!b) {
                            LOG.warn("SyncDrugToEsRunable update error! updateList={}", JSONUtils.toString(updateList));
                        } else {
                            LOG.info("SyncDrugToEsRunable update success. start={}", subStart);
                        }
                    }
                }
            }
        }
        long elapsedTime = System.currentTimeMillis() - start;
        LOG.info("RecipeBusiThreadPool SyncDrugToEsRunable 同步基础库药品至ES 执行时间:{}.", elapsedTime);
    }
}
