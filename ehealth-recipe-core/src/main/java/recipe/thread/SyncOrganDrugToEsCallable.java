package recipe.thread;

import com.google.common.base.Function;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.ngari.recipe.entity.DrugList;
import com.ngari.recipe.entity.OrganDrugList;
import ctd.persistence.DAOFactory;
import ctd.util.AppContextHolder;
import ctd.util.JSONUtils;
import es.api.DrugSearchService;
import es.vo.DoctorDrugDetailVO;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.util.StringUtils;
import recipe.dao.DrugListDAO;
import recipe.dao.OrganDrugListDAO;
import recipe.util.LocalStringUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

/**
 * @author： 0184/yu_yun
 * @date： 2019/4/15
 * @description： 同步机构药品至ES
 * @version： 1.0
 */
public class SyncOrganDrugToEsCallable implements Callable<String> {

    /**
     * logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(SyncOrganDrugToEsCallable.class);

    private int start;

    private int end;

    private int ONCE_NUM = 100;

    public SyncOrganDrugToEsCallable(int start, int end) {
        this.start = start;
        this.end = end;
    }

    @Override
    public String call() throws Exception {
        OrganDrugListDAO organDrugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);
        int total = this.end - this.start;
        if (total > 0) {
            int time = (int) Math.ceil(total / Double.parseDouble(String.valueOf(ONCE_NUM)));
            List<OrganDrugList> list = null;
            for (int i = 0; i < time; i++) {
                int subStart = this.start + i * ONCE_NUM;
                list = organDrugListDAO.findAllForPage(subStart, ONCE_NUM);
                if (CollectionUtils.isNotEmpty(list)) {
                    List<Integer> drugIdList = new ArrayList<>(list.size());
                    //获取base_druglist信息
                    for (OrganDrugList organDrug : list) {
                        drugIdList.add(organDrug.getDrugId());
                    }

                    DrugListDAO drugListDAO = DAOFactory.getDAO(DrugListDAO.class);
                    List<DrugList> drugList = drugListDAO.findByDrugIdsWithOutStatus(drugIdList);

                    //基础数据为空的话则存在问题
                    if (CollectionUtils.isEmpty(drugList)) {
                        LOG.warn("SyncOrganDrugToEsCallable drugList is empty. drugIds={}",
                                JSONUtils.toString(drugIdList));
                        drugList = Lists.newArrayList();
                    }

                    //处理下基础药品数据信息
                    Map<Integer, DrugList> drugMap = Maps.uniqueIndex(drugList, new Function<DrugList, Integer>() {
                        @Override
                        public Integer apply(DrugList input) {
                            return input.getDrugId();
                        }
                    });


                    DrugSearchService searchService = AppContextHolder.getBean("es.drugSearchService", DrugSearchService.class);
                    DoctorDrugDetailVO detailVo;
                    DrugList drug;
                    List<DoctorDrugDetailVO> updateList = new ArrayList<>(list.size());
                    String searchKey = "";
                    for (OrganDrugList organDrug : list) {
                        detailVo = new DoctorDrugDetailVO();
                        BeanUtils.copyProperties(organDrug, detailVo);
                        detailVo.setDeleteFlag(0);
                        drug = drugMap.get(organDrug.getDrugId());
                        //处理药品基础库相关信息
                        if (null == drug) {
                            detailVo.setStatus(0);
                        } else {
                            //药品用法用量默认使用机构的，无机构数据则使用平台的，两者都无数据则为空
                            if (StringUtils.isEmpty(organDrug.getUsePathways())){
                                detailVo.setUsePathways(drug.getUsePathways());
                            }
                            if (StringUtils.isEmpty(organDrug.getUsingRate())){
                                detailVo.setUsingRate(drug.getUsingRate());
                            }
                            if (StringUtils.isEmpty(organDrug.getUsePathwaysId())){
                                detailVo.setUsePathwaysId(drug.getUsePathwaysId());
                            }
                            if (StringUtils.isEmpty(organDrug.getUsingRateId())){
                                detailVo.setUsingRateId(drug.getUsingRateId());
                            }
                            //重置searchKey
                            //机构药品名+平台商品名+机构商品名+院内别名
                            searchKey = organDrug.getDrugName() + ";" + drug.getSaleName() + ";" + organDrug.getSaleName() + ";" +
                                    LocalStringUtil.toString(organDrug.getRetrievalCode());
                            detailVo.setSearchKey(searchKey.replaceAll(" ", ";"));
                            detailVo.setPlatformSaleName(drug.getSaleName());
                            detailVo.setDrugType(drug.getDrugType());
                            //设置药房id列表
                            if (org.apache.commons.lang3.StringUtils.isNotEmpty(organDrug.getPharmacy())) {
                                try {
                                    List<String> splitToList = Splitter.on(",").splitToList(organDrug.getPharmacy());
                                    List<Integer> pharmacyIds = splitToList.stream().map(Integer::valueOf).collect(Collectors.toList());
                                    detailVo.setPharmacyIds(pharmacyIds);
                                } catch (Exception e) {
                                    LOG.error("pharmacyId transform exception! updateList={}", JSONUtils.toString(organDrug), e);
                                }
                            }
                            //status字段修改注意：先判断基础药品库，再处理机构药品库
                            if (0 == drug.getStatus()) {
                                detailVo.setStatus(0);
                            } else {
                                detailVo.setStatus(organDrug.getStatus());
                            }
                        }
                        updateList.add(detailVo);
                    }

                    if (CollectionUtils.isNotEmpty(updateList)) {
                        boolean b = false;
                        try {
                            b = searchService.updateDoctorDrugDetail(updateList);
                        } catch (Exception e) {
                            LOG.error("SyncOrganDrugToEsCallable update exception! updateList={}", JSONUtils.toString(updateList), e);
                        }
                        if (!b) {
                            LOG.warn("SyncOrganDrugToEsCallable update error! updateList={}", JSONUtils.toString(updateList));
                        } else {
                            LOG.info("SyncOrganDrugToEsCallable update success. start={}", subStart);
                        }
                    }

                }
            }
        }

        return "";
    }
}
