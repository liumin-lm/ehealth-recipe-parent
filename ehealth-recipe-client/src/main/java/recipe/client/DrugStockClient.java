package recipe.client;

import com.ngari.his.recipe.mode.DrugInfoRequestTO;
import com.ngari.his.recipe.mode.DrugInfoResponseTO;
import com.ngari.his.recipe.mode.DrugInfoTO;
import com.ngari.recipe.entity.OrganDrugList;
import com.ngari.recipe.entity.PharmacyTcm;
import com.ngari.recipe.entity.Recipedetail;
import ctd.util.JSONUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
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

}
