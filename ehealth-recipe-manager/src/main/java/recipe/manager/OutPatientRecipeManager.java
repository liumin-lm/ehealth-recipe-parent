package recipe.manager;

import com.alibaba.fastjson.JSON;
import com.ngari.his.recipe.mode.OutPatientRecipeReq;
import com.ngari.his.recipe.mode.OutRecipeDetailReq;
import com.ngari.recipe.dto.DiseaseInfoDTO;
import com.ngari.recipe.dto.OutPatientRecipeDTO;
import com.ngari.recipe.dto.OutRecipeDetailDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.client.OfflineRecipeClient;
import recipe.util.ValidateUtil;

import java.util.ArrayList;
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
     * @return  诊断列表
     */
    public List<DiseaseInfoDTO> getOutRecipeDisease(Integer organId, String patientName, String registerID, String patientId){
        logger.info("OutPatientRecipeManager getOutRecipeDisease organId:{}, patientName:{},registerID:{},patientId:{}.",organId, patientName, registerID, patientId);
        if (ValidateUtil.validateObjects(organId, patientName, registerID, patientId)) {
            return new ArrayList<>();
        }
        List<DiseaseInfoDTO> response = offlineRecipeClient.queryPatientDisease(organId, patientName, registerID, patientId);
        logger.info("OutPatientRecipeManager getOutRecipeDisease response:{}.", JSON.toJSONString(response));
        return response;
    }

    /**
     * 查询门诊处方信息
     * @param outPatientRecipeReq 患者信息
     * @return  门诊处方列表
     */
    public List<OutPatientRecipeDTO> queryOutPatientRecipe(OutPatientRecipeReq outPatientRecipeReq){
        logger.info("OutPatientRecipeManager queryOutPatientRecipe outPatientRecipeReq:{}.", JSON.toJSONString(outPatientRecipeReq));
        if (ValidateUtil.validateObjects(outPatientRecipeReq, outPatientRecipeReq.getOrganId(), outPatientRecipeReq.getPatientName())){
            return new ArrayList<>();
        }
        List<OutPatientRecipeDTO> response = offlineRecipeClient.queryOutPatientRecipe(outPatientRecipeReq);
        logger.info("OutPatientRecipeManager queryOutPatientRecipe response:{}.", JSON.toJSONString(response));
        return response;
    }

    /**
     * 获取门诊处方详情信息
     * @param outRecipeDetailReq 门诊处方信息
     * @return 图片或者PDF链接等
     */
    public OutRecipeDetailDTO queryOutRecipeDetail(OutRecipeDetailReq outRecipeDetailReq) {
        logger.info("OutPatientRecipeManager queryOutPatientRecipe queryOutRecipeDetail:{}.", JSON.toJSONString(outRecipeDetailReq));
        if (ValidateUtil.validateObjects(outRecipeDetailReq, outRecipeDetailReq.getOrganId(), outRecipeDetailReq.getRecipeCode())) {
            return null;
        }
        OutRecipeDetailDTO response = offlineRecipeClient.queryOutRecipeDetail(outRecipeDetailReq);
        logger.info("OutPatientRecipeManager queryOutPatientRecipe response:{}.", JSON.toJSONString(response));
        return response;
    }

}
