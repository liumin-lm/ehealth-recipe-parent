package recipe.mq;

import com.google.common.base.Function;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.ngari.recipe.RecipeAPI;
import com.ngari.recipe.entity.DrugList;
import com.ngari.recipe.entity.OrganDrugList;
import ctd.net.broadcast.Observer;
import ctd.persistence.DAOFactory;
import ctd.util.AppContextHolder;
import ctd.util.JSONUtils;
import es.api.DrugSearchService;
import es.vo.DoctorDrugDetailVO;
import es.vo.PatientDrugDetailVO;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.util.StringUtils;
import recipe.dao.DrugListDAO;
import recipe.dao.OrganDrugListDAO;
import recipe.service.RecipeHisService;
import recipe.thread.RecipeBusiThreadPool;
import recipe.util.LocalStringUtil;
import shadow.message.ShadowMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author： 0184/yu_yun
 * @date： 2019/4/16
 * @description： 对base_druglist表修改消息处理
 * @version： 1.0
 */
public class DrugSyncObserver implements Observer<ShadowMessage> {

    /**
     * logger
     */
    private static final Logger LOG = LoggerFactory.getLogger(DrugSyncObserver.class);

    private static final String DRUG_LIST = "base_druglist";

    private static final String ORGAN_DRUG_LIST = "base_organdruglist";

    @Override
    public void onMessage(ShadowMessage shadowMessage) {
        LOG.info("DrugSyncObserver message=" + JSONUtils.toString(shadowMessage));
        String tableName = shadowMessage.getTableName();
        DrugSearchService searchService = AppContextHolder.getBean("es.drugSearchService", DrugSearchService.class);
        //删除标记设置
        int deleteFlag = (3 == shadowMessage.getEventType()) ? 1 : 0;
        if (DRUG_LIST.equals(tableName)) {
            //需要修改2个索引的数据
            List<ShadowMessage.RowData> rowDataList = shadowMessage.getRowDatas();
            if (CollectionUtils.isEmpty(rowDataList)) {
                LOG.info("DrugSyncObserver " + DRUG_LIST + " update rowlist is empty.");
                return;
            }

            List<Integer> rowIdList = new ArrayList<>(rowDataList.size());
            for (ShadowMessage.RowData rowData : rowDataList) {
                rowIdList.add(Integer.valueOf(rowData.getFieldValue()));
            }

            //base_druglist表信息处理
            DrugListDAO drugListDAO = DAOFactory.getDAO(DrugListDAO.class);
            List<PatientDrugDetailVO> updateList = new ArrayList<>(rowIdList.size());
            List<DrugList> drugList = null;
            //非删除情况
            if (0 == deleteFlag) {
                drugList = drugListDAO.findByDrugIds(rowIdList);
                //武昌模式--更新或者新增药品同步到his
                List<DrugList> finalDrugList = drugList;
                RecipeBusiThreadPool.submit(()->{
                    RecipeHisService recipeHisService = RecipeAPI.getService(RecipeHisService.class);
                    recipeHisService.syncDrugListToHis(finalDrugList);
                    return null;
                });
                PatientDrugDetailVO detailVo;
                for (DrugList drug : drugList) {
                    //只更新基础药品库信息
                    if (null == drug.getSourceOrgan()) {
                        detailVo = new PatientDrugDetailVO();
                        BeanUtils.copyProperties(drug, detailVo);
                        detailVo.setDeleteFlag(deleteFlag);
                        updateList.add(detailVo);
                    }
                }
            } else {
                //删除情况增加效率
                PatientDrugDetailVO detailVo;
                for (Integer drugId : rowIdList) {
                    detailVo = new PatientDrugDetailVO();
                    detailVo.setDeleteFlag(deleteFlag);
                    detailVo.setDrugId(drugId);
                    updateList.add(detailVo);
                }
            }

            if (CollectionUtils.isNotEmpty(updateList)) {
                boolean b = false;
                try {
                    b = searchService.updatePatientDrugDetail(updateList);
                } catch (Exception e) {
                    LOG.warn("DrugSyncObserver " + DRUG_LIST + " update error! drugId={}", JSONUtils.toString(rowIdList), e);
                    //让MQ重新过段时间进行投递
                    throw new RuntimeException();
                }
                if (!b) {
                    LOG.warn("DrugSyncObserver " + DRUG_LIST + " update error! drugId={}", JSONUtils.toString(rowIdList));
                    //让MQ重新过段时间进行投递
                    throw new RuntimeException();
                } else {
                    LOG.info("DrugSyncObserver " + DRUG_LIST + " update success. drugId={}", JSONUtils.toString(rowIdList));
                }
            }

            //再处理base_organdruglist信息
            OrganDrugListDAO organDrugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);
            //数据量存在不确定性
            List<Integer> organDrugIdList = organDrugListDAO.findOrganDrugIdByDrugIds(rowIdList);
            LOG.info("DrugSyncObserver " + DRUG_LIST + " modify organDrugList size={}", organDrugIdList.size());

            updateOrganDrugList(organDrugIdList, deleteFlag, drugList);
        } else if (ORGAN_DRUG_LIST.equals(tableName)) {
            List<ShadowMessage.RowData> rowDataList = shadowMessage.getRowDatas();
            if (CollectionUtils.isEmpty(rowDataList)) {
                LOG.info("DrugSyncObserver " + ORGAN_DRUG_LIST + " update rowlist is empty.");
                return;
            }

            List<Integer> rowIdList = new ArrayList<>(rowDataList.size());
            for (ShadowMessage.RowData rowData : rowDataList) {
                rowIdList.add(Integer.valueOf(rowData.getFieldValue()));
            }

            updateOrganDrugList(rowIdList, deleteFlag, null);
        }
    }

    private void updateOrganDrugList(List<Integer> rowIdList, int deleteFlag, List<DrugList> drugList) {
        if (CollectionUtils.isEmpty(rowIdList)) {
            return;
        }

        DrugSearchService searchService = AppContextHolder.getBean("es.drugSearchService", DrugSearchService.class);
        OrganDrugListDAO organDrugListDAO = DAOFactory.getDAO(OrganDrugListDAO.class);
        List<DoctorDrugDetailVO> updateList = new ArrayList<>(rowIdList.size());
        //非删除情况
        if (0 == deleteFlag) {
            List<OrganDrugList> organDrugList = organDrugListDAO.findByOrganDrugIds(rowIdList);
            List<Integer> drugIdList = new ArrayList<>(rowIdList.size());
            //获取base_druglist信息
            if (CollectionUtils.isEmpty(drugList)) {
                for (OrganDrugList organDrug : organDrugList) {
                    drugIdList.add(organDrug.getDrugId());
                }

                DrugListDAO drugListDAO = DAOFactory.getDAO(DrugListDAO.class);
                drugList = drugListDAO.findByDrugIds(drugIdList);
            }

            //基础数据为空的话则存在问题
            if (CollectionUtils.isEmpty(drugList)) {
                LOG.warn("DrugSyncObserver " + ORGAN_DRUG_LIST + " drugList is empty. drugIds={}",
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

            DoctorDrugDetailVO detailVo;
            DrugList drug;
            String searchKey = "";
            for (OrganDrugList organDrug : organDrugList) {
                detailVo = new DoctorDrugDetailVO();
                BeanUtils.copyProperties(organDrug, detailVo);
                detailVo.setDeleteFlag(deleteFlag);
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
                    searchKey = organDrug.getDrugName()+";"+drug.getSaleName() + ";" + organDrug.getSaleName() + ";" +
                            LocalStringUtil.toString(organDrug.getRetrievalCode()) + ";" + organDrug.getChemicalName();
                    detailVo.setSearchKey(searchKey.replaceAll(" ", ";"));
                    detailVo.setPlatformSaleName(drug.getSaleName());
                    detailVo.setDrugType(drug.getDrugType());
                    //status字段修改注意：先判断基础药品库，再处理机构药品库
                    if (0 == drug.getStatus()) {
                        detailVo.setStatus(0);
                    } else {
                        detailVo.setStatus(organDrug.getStatus());
                    }
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
                }
                updateList.add(detailVo);
            }
        } else {
            //删除情况增加效率
            DoctorDrugDetailVO detailVo;
            for (Integer organDrugId : rowIdList) {
                detailVo = new DoctorDrugDetailVO();
                detailVo.setDeleteFlag(deleteFlag);
                detailVo.setOrganDrugId(organDrugId);
                updateList.add(detailVo);
            }
        }

        if (CollectionUtils.isEmpty(updateList)) {
            return;
        }
        boolean b = false;
        try {
            b = searchService.updateDoctorDrugDetail(updateList);
        } catch (Exception e) {
            LOG.warn("DrugSyncObserver " + ORGAN_DRUG_LIST + " update error! OrganDrugId={}", JSONUtils.toString(rowIdList), e);
            //让MQ重新过段时间进行投递
            throw new RuntimeException();
        }
        if (!b) {
            LOG.warn("DrugSyncObserver " + ORGAN_DRUG_LIST + " update error! OrganDrugId={}", JSONUtils.toString(rowIdList));
            //让MQ重新过段时间进行投递
            throw new RuntimeException();
        } else {
            LOG.info("DrugSyncObserver " + ORGAN_DRUG_LIST + " update success. OrganDrugId={}", JSONUtils.toString(rowIdList));
        }
    }
}
