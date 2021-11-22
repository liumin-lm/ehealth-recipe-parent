package recipe.client;

import com.alibaba.fastjson.JSON;
import com.google.common.base.Splitter;
import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.recipe.mode.DrugInfoRequestTO;
import com.ngari.his.recipe.mode.DrugInfoResponseTO;
import com.ngari.his.recipe.mode.DrugInfoTO;
import com.ngari.his.recipe.service.IRecipeEnterpriseService;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.platform.recipe.mode.*;
import com.ngari.recipe.dto.DrugInfoDTO;
import com.ngari.recipe.dto.DrugStockAmountDTO;
import com.ngari.recipe.entity.*;
import com.ngari.revisit.common.model.RevisitExDTO;
import ctd.persistence.exception.DAOException;
import ctd.util.JSONUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import recipe.constant.ErrorCode;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * @description： 药品库存查询类
 * @author： whf
 * @date： 2021-07-19 9:25
 */
@Service
public class DrugStockClient extends BaseClient {

    @Resource
    private IRecipeEnterpriseService recipeEnterpriseService;

    @Resource
    private RevisitClient revisitClient;


    /**
     * 组织加减库存接口参数
     *
     * @param recipe           处方
     * @param recipeDetailList 处方明细
     * @param recipeOrderBill  处方订单
     * @return
     */
    public RecipeDrugInventoryDTO recipeDrugInventory(Recipe recipe, List<Recipedetail> recipeDetailList, RecipeOrderBill recipeOrderBill) {
        RecipeDrugInventoryDTO request = new RecipeDrugInventoryDTO();
        request.setOrganId(recipe.getClinicOrgan());
        request.setRecipeId(recipe.getRecipeId());
        request.setRecipeType(recipe.getRecipeType());
        if (null != recipeOrderBill) {
            request.setInvoiceNumber(recipeOrderBill.getBillNumber());
        }
        if (org.springframework.util.CollectionUtils.isEmpty(recipeDetailList)) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "药品列表为空");
        }
        List<RecipeDrugInventoryInfoDTO> infoList = new LinkedList<>();
        recipeDetailList.forEach(a -> {
            RecipeDrugInventoryInfoDTO info = new RecipeDrugInventoryInfoDTO();
            info.setCreateDt(a.getCreateDt());
            info.setDrugCost(a.getDrugCost());
            info.setDrugId(a.getDrugId());
            info.setOrganDrugCode(a.getOrganDrugCode());
            info.setUseTotalDose(a.getUseTotalDose());
            info.setPharmacyId(a.getPharmacyId());
            info.setProducerCode(a.getProducerCode());
            info.setDrugBatch(a.getDrugBatch());
            info.setSalePrice(a.getSalePrice());
            infoList.add(info);
        });
        request.setInfo(infoList);
        logger.info("HisInventoryClient RecipeDrugInventoryDTO request= {}", JSON.toJSONString(request));
        return request;
    }

    /**
     * 增减库存
     *
     * @param request
     */
    public void drugInventory(RecipeDrugInventoryDTO request) {
        logger.info("HisInventoryClient drugInventory request= {}", JSON.toJSONString(request));
        try {
            HisResponseTO<Boolean> hisResponse = recipeHisService.drugInventory(request);
            Boolean result = getResponse(hisResponse);
            if (!result) {
                throw new DAOException(ErrorCode.SERVICE_ERROR, "his库存操作失败");
            }
        } catch (Exception e) {
            logger.error("HisInventoryClient drugInventory hisResponse", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        }
    }


    /**
     * 调用his接口查询医院库存
     *
     * @param detailList
     * @param organId
     * @param organDrugList
     * @param pharmacyTcms
     * @return
     */
    public List<DrugInfoDTO> scanDrugStock(List<Recipedetail> detailList, int organId, List<OrganDrugList> organDrugList, List<PharmacyTcm> pharmacyTcms) {
        Map<Integer, PharmacyTcm> pharmacyTcmMap = pharmacyTcms.stream().collect(Collectors.toMap(PharmacyTcm::getPharmacyId, a -> a, (k1, k2) -> k1));
        Map<String, Recipedetail> detailMap = detailList.stream().collect(Collectors.toMap(k -> k.getDrugId() + k.getOrganDrugCode(), a -> a, (k1, k2) -> k1));
        List<DrugInfoTO> data = new LinkedList<>();
        organDrugList.forEach(a -> {
            DrugInfoTO drugInfo = new DrugInfoTO(a.getOrganDrugCode());
            drugInfo.setPack(String.valueOf(a.getPack()));
            drugInfo.setManfcode(a.getProducerCode());
            drugInfo.setDrname(a.getDrugName());
            drugInfo.setDrugId(a.getDrugId());
            Recipedetail recipedetail = detailMap.get(a.getDrugId() + a.getOrganDrugCode());
            if (null == recipedetail) {
                data.add(drugInfo);
                return;
            }
            drugInfo.setPackUnit(recipedetail.getDrugUnit());
            drugInfo.setUseTotalDose(recipedetail.getUseTotalDose());
            PharmacyTcm tcm = pharmacyTcmMap.get(recipedetail.getPharmacyId());
            if (null != tcm) {
                drugInfo.setPharmacyCode(tcm.getPharmacyCode());
                drugInfo.setPharmacy(tcm.getPharmacyName());
            }
            data.add(drugInfo);
        });

        DrugInfoRequestTO request = new DrugInfoRequestTO();
        request.setOrganId(organId);
        request.setData(data);
        logger.info("DrugStockClient scanDrugStock request={}", JSONUtils.toString(request));
        try {
            DrugInfoResponseTO response = recipeHisService.scanDrugStock(request);
            logger.info("DrugStockClient scanDrugStock response={}", JSONUtils.toString(response));
            //老版本代码兼容 his未配置该服务则还是可以通过
            if (null == response || CollectionUtils.isEmpty(response.getData())) {
                response = new DrugInfoResponseTO();
                response.setMsgCode(0);
                data.forEach(a -> a.setStockAmount(a.getUseTotalDose()));
                return getDrugInfoDTO(data);
            }
            //老版本代码兼容
            if (StringUtils.isNotEmpty(response.getMsg())) {
                List<String> noStock = Splitter.on(",").splitToList(response.getMsg());
                Map<String, String> noStockMap = noStock.stream().collect(Collectors.toMap(a -> a, a -> a, (k1, k2) -> k1));
                response.getData().forEach(a -> {
                    if (StringUtils.isNotEmpty(noStockMap.get(a.getDrcode()))) {
                        a.setStockAmount(0d);
                    }
                });
            }
            List<DrugInfoDTO> list = getDrugInfoDTO(response.getData());
            logger.info(" DrugStockClient scanDrugStock  list={}", JSONUtils.toString(list));
            return list;
        } catch (Exception e) {
            logger.error(" DrugStockClient scanDrugStock error ", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, " recipeHisService.scanDrugStock error");
        }
    }

    /**
     * 查询由前置机对接的药企库存信息
     *
     * @param recipe
     * @param drugsEnterprise
     * @param recipeDetails
     * @param saleDrugLists
     * @return
     */
    public DrugStockAmountDTO scanEnterpriseDrugStock(Recipe recipe, DrugsEnterprise drugsEnterprise, List<Recipedetail> recipeDetails, List<SaleDrugList> saleDrugLists) {
        String channelCode = null;
        try {
            if (null != recipe.getClinicId()) {
                RevisitExDTO revisitExDTO = revisitClient.getByClinicId(recipe.getClinicId());
                if (revisitExDTO != null) {
                    channelCode = revisitExDTO.getProjectChannel();
                }
            }
        } catch (Exception e) {
            logger.error("queryPatientChannelId error:", e);
        }
        String finalChannelCode = channelCode;
        Map<Integer, List<SaleDrugList>> saleDrugListMap = saleDrugLists.stream().collect(Collectors.groupingBy(SaleDrugList::getDrugId));
        List<ScanDrugListBean> scanDrugListBeans = new ArrayList<>();
        recipeDetails.forEach(recipedetail -> {
            ScanDrugListBean scanDrugListBean = new ScanDrugListBean();
            List<SaleDrugList> saleDrugLists1 = saleDrugListMap.get(recipedetail.getDrugId());
            if (CollectionUtils.isNotEmpty(saleDrugLists1)) {
                scanDrugListBean.setDrugCode(saleDrugLists1.get(0).getOrganDrugCode());
            }
            scanDrugListBean.setDrugId(recipedetail.getDrugId());
            scanDrugListBean.setTotal(recipedetail.getUseTotalDose().toString());
            scanDrugListBean.setUnit(recipedetail.getDrugUnit());
            scanDrugListBean.setDrugSpec(recipedetail.getDrugSpec());
            scanDrugListBean.setProducerCode(recipedetail.getProducerCode());
            scanDrugListBean.setPharmacyCode(String.valueOf(recipedetail.getPharmacyId()));
            scanDrugListBean.setPharmacy(recipedetail.getPharmacyName());
            scanDrugListBean.setProducer(recipedetail.getProducer());
            scanDrugListBean.setName(recipedetail.getSaleName());
            scanDrugListBean.setGname(recipedetail.getDrugName());
            scanDrugListBean.setChannelCode(finalChannelCode);
            scanDrugListBeans.add(scanDrugListBean);
        });
        ScanRequestBean scanRequestBean = new ScanRequestBean();
        scanRequestBean.setDrugsEnterpriseBean(ObjectCopyUtils.convert(drugsEnterprise, DrugsEnterpriseBean.class));
        scanRequestBean.setScanDrugListBeans(scanDrugListBeans);
        scanRequestBean.setOrganId(recipe.getClinicOrgan());
        logger.info("DrugStockClient scanEnterpriseDrugStock-scanStock scanRequestBean:{}.", JSON.toJSONString(scanRequestBean));
        HisResponseTO<List<ScanDrugListBean>> responseTO = recipeEnterpriseService.scanStock(scanRequestBean);
        logger.info("DrugStockClient scanEnterpriseDrugStock recipeId={},前置机调用查询结果={}", JSON.toJSONString(recipe), JSON.toJSONString(responseTO));
        DrugStockAmountDTO drugStockAmountDTO = new DrugStockAmountDTO();
        drugStockAmountDTO.setResult(false);
        if (null != responseTO && responseTO.isSuccess()) {
            drugStockAmountDTO.setResult(true);
            drugStockAmountDTO.setDrugInfoList(getScanDrugInfoDTO(responseTO.getData()));
        }
        return drugStockAmountDTO;
    }

    private List<DrugInfoDTO> getDrugInfoDTO(List<DrugInfoTO> DrugInfoTOList) {
        List<DrugInfoDTO> list = new ArrayList<>();
        if (CollectionUtils.isEmpty(DrugInfoTOList)) {
            return list;
        }
        DrugInfoTOList.forEach(a -> {
            DrugInfoDTO drugInfoDTO = new DrugInfoDTO();
            drugInfoDTO.setOrganId(a.getOrganId());
            drugInfoDTO.setOrganDrugCode(a.getDrcode());
            drugInfoDTO.setDrugId(a.getDrugId());
            drugInfoDTO.setDrugName(a.getDrname());
            drugInfoDTO.setStockAmount((int) Math.ceil(a.getStockAmount()));
            if (StringUtils.isNotEmpty(a.getNoInventoryTip())) {
                drugInfoDTO.setStockAmountChin(a.getNoInventoryTip());
            } else {
                drugInfoDTO.setStockAmountChin(String.valueOf(drugInfoDTO.getStockAmount()));
            }
            drugInfoDTO.setPharmacyCode(a.getPharmacyCode());
            drugInfoDTO.setPharmacy(a.getPharmacy());
            drugInfoDTO.setProducerCode(a.getProducerCode());
            if (0 == drugInfoDTO.getStockAmount()) {
                drugInfoDTO.setStock(false);
            } else {
                drugInfoDTO.setStock(true);
            }
            list.add(drugInfoDTO);
        });
        return list;
    }


    private List<DrugInfoDTO> getScanDrugInfoDTO(List<ScanDrugListBean> scanDrugList) {
        List<DrugInfoDTO> list = new ArrayList<>();
        if (CollectionUtils.isEmpty(scanDrugList)) {
            return list;
        }
        scanDrugList.forEach(a -> {
            DrugInfoDTO drugInfoDTO = new DrugInfoDTO();
            drugInfoDTO.setOrganDrugCode(a.getDrugCode());
            drugInfoDTO.setDrugName(a.getGname());
            drugInfoDTO.setDrugId(a.getDrugId());
            drugInfoDTO.setStockAmount(a.getStockAmount());
            drugInfoDTO.setPharmacyCode(a.getPharmacyCode());
            drugInfoDTO.setPharmacy(a.getPharmacy());
            drugInfoDTO.setProducerCode(a.getProducerCode());
            drugInfoDTO.setStockAmountChin(String.valueOf(drugInfoDTO.getStockAmount()));
            if (0 == drugInfoDTO.getStockAmount()) {
                drugInfoDTO.setStock(false);
            } else {
                drugInfoDTO.setStock(true);
            }
            list.add(drugInfoDTO);
        });
        return list;
    }
}
