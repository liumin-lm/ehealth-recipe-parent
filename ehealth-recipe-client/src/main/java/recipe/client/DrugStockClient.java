package recipe.client;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.recipe.mode.DrugInfoRequestTO;
import com.ngari.his.recipe.mode.DrugInfoResponseTO;
import com.ngari.his.recipe.mode.DrugInfoTO;
import com.ngari.his.recipe.service.IRecipeEnterpriseService;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.platform.recipe.mode.*;
import com.ngari.recipe.entity.*;
import com.ngari.revisit.common.model.RevisitExDTO;
import ctd.persistence.exception.DAOException;
import ctd.util.JSONUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import recipe.constant.ErrorCode;

import javax.annotation.Resource;
import java.util.*;
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
    public DrugInfoResponseTO scanDrugStock(List<Recipedetail> detailList, int organId, List<OrganDrugList> organDrugList, List<PharmacyTcm> pharmacyTcms) {
        Map<Integer, List<PharmacyTcm>> pharmacyTcmMap = Optional.ofNullable(pharmacyTcms).orElseGet(Collections::emptyList).stream().collect(Collectors.groupingBy(PharmacyTcm::getPharmacyId));
        Map<String, OrganDrugList> organDrugListMap = organDrugList.stream().collect(Collectors.toMap(k -> k.getOrganDrugCode() + k.getDrugId(), a -> a, (k1, k2) -> k1));
        List<DrugInfoTO> data = new LinkedList<>();
        detailList.forEach(a -> {
            DrugInfoTO drugInfo = new DrugInfoTO(a.getOrganDrugCode());
            drugInfo.setPack(a.getPack().toString());
            drugInfo.setPackUnit(a.getDrugUnit());
            drugInfo.setUseTotalDose(a.getUseTotalDose());
            OrganDrugList organDrugs = organDrugListMap.get(a.getOrganDrugCode() + a.getDrugId());
            if (null != organDrugs && StringUtils.isNotEmpty(organDrugs.getProducerCode())) {
                drugInfo.setManfcode(organDrugs.getProducerCode());
            }
            //药房
            List<PharmacyTcm> tcm = pharmacyTcmMap.get(a.getPharmacyId());
            if (CollectionUtils.isNotEmpty(tcm)) {
                drugInfo.setPharmacyCode(tcm.get(0).getPharmacyCode());
                drugInfo.setPharmacy(tcm.get(0).getPharmacyName());
            }
            data.add(drugInfo);
        });
        DrugInfoRequestTO request = new DrugInfoRequestTO();
        request.setOrganId(organId);
        request.setData(data);
        logger.info("scanDrugStock request={}", JSONUtils.toString(request));
        DrugInfoResponseTO response;
        try {
            response = recipeHisService.scanDrugStock(request);
            logger.info("scanDrugStock response={}", JSONUtils.toString(response));
        } catch (Exception e) {
            logger.error("scanDrugStock error ", e);
            //抛异常流程不应该继续下去
            response = new DrugInfoResponseTO();
            response.setMsgCode(1);
        }
        //his未配置该服务则还是可以通过
        if (null == response) {
            response = new DrugInfoResponseTO();
            response.setMsgCode(0);
        }
        return response;
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
    public HisResponseTO scanEnterpriseDrugStock(Recipe recipe, DrugsEnterprise drugsEnterprise, List<Recipedetail> recipeDetails, List<SaleDrugList> saleDrugLists) {
        String channelCode = null;
        try {
            RevisitExDTO revisitExDTO = revisitClient.getByClinicId(recipe.getClinicId());
            if (revisitExDTO != null) {
                channelCode = revisitExDTO.getProjectChannel();
            }
        } catch (Exception e) {
            logger.error("queryPatientChannelId error:", e);
        }
        String finalChannelCode = channelCode;
        Map<Integer, List<SaleDrugList>> saleDrugListMap = saleDrugLists.stream().collect(Collectors.groupingBy(SaleDrugList::getDrugId));
        List<ScanDrugListBean> scanDrugListBeans = recipeDetails.stream().map(recipedetail -> {
            ScanDrugListBean scanDrugListBean = new ScanDrugListBean();
            List<SaleDrugList> saleDrugLists1 = saleDrugListMap.get(recipedetail.getDrugId());
            if (CollectionUtils.isNotEmpty(saleDrugLists1)) {
                scanDrugListBean.setDrugCode(saleDrugLists1.get(0).getOrganDrugCode());
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
            }
            return scanDrugListBean;
        }).collect(Collectors.toList());
        ScanRequestBean scanRequestBean = new ScanRequestBean();
        scanRequestBean.setDrugsEnterpriseBean(ObjectCopyUtils.convert(drugsEnterprise, DrugsEnterpriseBean.class));
        scanRequestBean.setScanDrugListBeans(scanDrugListBeans);
        scanRequestBean.setOrganId(recipe.getClinicOrgan());
        logger.info("findUnSupportDrugEnterprise-scanStock scanRequestBean:{}.", JSONUtils.toString(scanRequestBean));
        HisResponseTO responseTO = recipeEnterpriseService.scanStock(scanRequestBean);
        logger.info("findUnSupportDrugEnterprise recipeId={},前置机调用查询结果={}", recipe.getRecipeId(), JSONObject.toJSONString(responseTO));
        return responseTO;
    }
}
