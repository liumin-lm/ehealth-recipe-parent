package recipe.client;

import com.alibaba.fastjson.JSONObject;
import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.recipe.mode.DrugInfoRequestTO;
import com.ngari.his.recipe.mode.DrugInfoResponseTO;
import com.ngari.his.recipe.mode.DrugInfoTO;
import com.ngari.his.recipe.service.IRecipeEnterpriseService;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.platform.recipe.mode.DrugsEnterpriseBean;
import com.ngari.platform.recipe.mode.ScanDrugListBean;
import com.ngari.platform.recipe.mode.ScanRequestBean;
import com.ngari.recipe.entity.*;
import ctd.persistence.DAOFactory;
import ctd.util.JSONUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * @description： 药品库存查询类
 * @author： whf
 * @date： 2021-07-19 9:25
 */
@Service
public class DrugStockClient extends BaseClient {

    @Resource
    private IRecipeEnterpriseService recipeEnterpriseService;

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
        // 拼装请求参数
        DrugInfoRequestTO request = scanDrugStockRequest(detailList, organId, organDrugList, pharmacyTcms);
        logger.info("scanDrugStock request={}", JSONUtils.toString(request));
        DrugInfoResponseTO response = null;
        try {
            response = recipeHisService.scanDrugStock(request);
            logger.info("scanDrugStock response={}", JSONUtils.toString(response));
        } catch (Exception e) {
            logger.error("scanDrugStock error ", e);
            //抛异常流程不应该继续下去
            response = new DrugInfoResponseTO();
            response.setMsgCode(1);
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
        ScanRequestBean scanRequestBean = getScanRequestBean(recipe, drugsEnterprise, recipeDetails, saleDrugLists);
        logger.info("findUnSupportDrugEnterprise-scanStock scanRequestBean:{}.", JSONUtils.toString(scanRequestBean));
        HisResponseTO responseTO = recipeEnterpriseService.scanStock(scanRequestBean);
        logger.info("findUnSupportDrugEnterprise recipeId={},前置机调用查询结果={}", recipe.getRecipeId(), JSONObject.toJSONString(responseTO));
        return responseTO;
    }


    /**
     * 拼装 his 库存请求参数
     *
     * @param detailList
     * @param organId
     * @return
     */
    private DrugInfoRequestTO scanDrugStockRequest(List<Recipedetail> detailList, int organId, List<OrganDrugList> organDrugList, List<PharmacyTcm> pharmacyTcms) {
        if (CollectionUtils.isEmpty(detailList)) {
            return null;
        }
        DrugInfoRequestTO request = new DrugInfoRequestTO();
        request.setOrganId(organId);

        Map<String, List<OrganDrugList>> drugIdAndProduce = organDrugList.stream().collect(Collectors.groupingBy(OrganDrugList::getOrganDrugCode));
        Map<Integer, List<PharmacyTcm>> pharmacyTcmMap = null;
        if (CollectionUtils.isNotEmpty(pharmacyTcms)) {
            pharmacyTcmMap = pharmacyTcms.stream().collect(Collectors.groupingBy(PharmacyTcm::getPharmacyId));
        }
        List<DrugInfoTO> data = new ArrayList<>(detailList.size());
        Map<Integer, List<PharmacyTcm>> finalPharmacyTcmMap = pharmacyTcmMap;
        detailList.forEach(a -> {
            DrugInfoTO drugInfo = new DrugInfoTO(a.getOrganDrugCode());
            drugInfo.setPack(a.getPack().toString());
            drugInfo.setPackUnit(a.getDrugUnit());
            drugInfo.setUseTotalDose(a.getUseTotalDose());
            List<OrganDrugList> organDrugs = drugIdAndProduce.get(a.getOrganDrugCode());
            if (CollectionUtils.isNotEmpty(organDrugs)) {
                Map<Integer, String> producerCodeMap = organDrugs.stream().collect(Collectors.toMap(OrganDrugList::getDrugId, OrganDrugList::getProducerCode));
                String producerCode = producerCodeMap.get(a.getDrugId());
                if (StringUtils.isNotEmpty(producerCode)) {
                    drugInfo.setManfcode(producerCode);
                }
            }
            //药房
            if (a.getPharmacyId() != null) {
                PharmacyTcm tcm = finalPharmacyTcmMap.get(a.getPharmacyId()).get(0);
                if (tcm != null) {
                    drugInfo.setPharmacyCode(tcm.getPharmacyCode());
                    drugInfo.setPharmacy(tcm.getPharmacyName());
                }
            }
            data.add(drugInfo);
        });
        request.setData(data);

        return request;
    }

    /**
     * 拼装 前置机请求 request
     *
     * @param recipe
     * @param drugsEnterprise
     * @return
     */
    private ScanRequestBean getScanRequestBean(Recipe recipe, DrugsEnterprise drugsEnterprise,
                                               List<Recipedetail> recipedetails, List<SaleDrugList> saleDrugLists) {
        ScanRequestBean scanRequestBean = new ScanRequestBean();
        Map<Integer, List<SaleDrugList>> saleDrugListMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(saleDrugLists)) {
            saleDrugListMap = saleDrugLists.stream().collect(Collectors.groupingBy(SaleDrugList::getDrugId));
        }
        Map<Integer, List<SaleDrugList>> finalSaleDrugListMap = saleDrugListMap;
        List<ScanDrugListBean> scanDrugListBeans = recipedetails.stream().map(recipedetail -> {
            ScanDrugListBean scanDrugListBean = new ScanDrugListBean();
            List<SaleDrugList> saleDrugLists1 = finalSaleDrugListMap.get(recipedetail.getDrugId());
            if (CollectionUtils.isNotEmpty(saleDrugLists1) && Objects.nonNull(saleDrugLists1.get(0))) {
                SaleDrugList saleDrugList = saleDrugLists1.get(0);
                scanDrugListBean.setDrugCode(saleDrugList.getOrganDrugCode());
                scanDrugListBean.setTotal(recipedetail.getUseTotalDose().toString());
                scanDrugListBean.setUnit(recipedetail.getDrugUnit());
            }
            return scanDrugListBean;
        }).collect(Collectors.toList());

        scanRequestBean.setDrugsEnterpriseBean(ObjectCopyUtils.convert(drugsEnterprise, DrugsEnterpriseBean.class));
        scanRequestBean.setScanDrugListBeans(scanDrugListBeans);
        scanRequestBean.setOrganId(recipe.getClinicOrgan());
        logger.info("getScanRequestBean scanRequestBean:{}.", JSONUtils.toString(scanRequestBean));
        return scanRequestBean;
    }

}
