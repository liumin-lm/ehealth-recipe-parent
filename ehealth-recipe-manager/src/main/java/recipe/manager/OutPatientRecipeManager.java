package recipe.manager;

import com.ngari.his.recipe.mode.DiseaseInfo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.client.OfflineRecipeClient;
import recipe.util.ValidateUtil;

import java.util.List;

/**
 * 门诊病人处方处理
 * @author yinsheng
 * @date 2021\7\16 0016 14:53
 */
@Service
public class OutPatientRecipeManager extends BaseManager{

    @Autowired
    private OfflineRecipeClient offlineRecipeClient;

    /**
     * 查询线下门诊处方诊断信息
     * @param organId 机构ID
     * @param patientName 患者名称
     * @param registerID 挂号序号
     * @param patientId 病历号
     * @return  诊断名称
     */
    public String getOutRecipeDisease(Integer organId, String patientName, String registerID, String patientId){
        logger.info("OutPatientRecipeManager getOutRecipeDisease organId:{}, patientName:{},registerID:{},patientId:{}.",organId, patientName, registerID, patientId);
        if (ValidateUtil.integerIsEmpty(organId) || StringUtils.isEmpty(patientName) || StringUtils.isEmpty(registerID) || StringUtils.isEmpty(patientId)) {
            return "";
        }
        final StringBuilder diseaseName = new StringBuilder();
        List<DiseaseInfo> response = offlineRecipeClient.queryPatientDisease(organId, patientName, registerID, patientId);
        response.forEach(diseaseInfo ->
            diseaseName.append(diseaseInfo.getDiseaseName()).append(";")
        );
        StringBuilder result = diseaseName.deleteCharAt(diseaseName.lastIndexOf(";"));
        logger.info("OutPatientRecipeManager diseaseName:{}.", result);
        return result.toString();
    }
}
