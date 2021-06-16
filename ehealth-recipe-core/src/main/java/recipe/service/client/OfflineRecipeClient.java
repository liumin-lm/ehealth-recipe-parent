package recipe.service.client;

import com.ngari.common.mode.HisResponseTO;
import com.ngari.recipe.commonrecipe.model.CommonDTO;
import com.ngari.recipe.commonrecipe.model.CommonRecipeDTO;
import com.ngari.recipe.commonrecipe.model.CommonRecipeDrugDTO;
import com.ngari.recipe.commonrecipe.model.CommonRecipeExtDTO;
import ctd.persistence.exception.DAOException;
import org.springframework.stereotype.Service;
import recipe.constant.ErrorCode;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * his处方 交互处理类
 *
 * @author fuzi
 */
@Service
public class OfflineRecipeClient extends BaseClient {
    /**
     * @param doctorId  医生id
     * @param jobNumber 医生工号
     * @param name      医生名称
     * @return 线下常用方对象
     */
    public List<CommonDTO> offlineCommonRecipe(Integer doctorId, String jobNumber, String name) {
        logger.info("OfflineRecipeClient offlineCommonRecipe doctorId:{}，jobNumber:{}，name:{}", doctorId, jobNumber, name);
        try {
            HisResponseTO<List<CommonDTO>> hisResponse = test(doctorId);//recipeHisService.offlineCommonRecipe(jobNumber, name, "0");
            List<CommonDTO> result = getResponse(hisResponse);
            return result;
        } catch (Exception e) {
            logger.error("OfflineRecipeClient offlineCommonRecipe hisResponse", e);
            throw new DAOException(ErrorCode.SERVICE_ERROR, e.getMessage());
        }
    }

    private HisResponseTO<List<CommonDTO>> test(Integer doctorId) {
        HisResponseTO<List<CommonDTO>> hisResponse = new HisResponseTO<>();
        hisResponse.setMsgCode("200");
        List<CommonDTO> commonDTOList = new ArrayList<>();
        CommonDTO commonDTO = new CommonDTO();
        CommonRecipeDTO commonRecipeDTO = new CommonRecipeDTO();
        commonRecipeDTO.setDoctorId(doctorId);
        commonRecipeDTO.setCommonRecipeName("协定方");
        commonRecipeDTO.setCommonRecipeCode("code124");
        commonRecipeDTO.setCommonRecipeType(2);
        commonRecipeDTO.setRecipeType(1);
        commonRecipeDTO.setOrganId(1);
        commonRecipeDTO.setPharmacyCode("123");
        commonRecipeDTO.setPharmacyName("测试药房");
        commonDTO.setCommonRecipeDTO(commonRecipeDTO);

        CommonRecipeExtDTO commonRecipeExt = new CommonRecipeExtDTO();
        commonRecipeExt.setCommonRecipeCode("code124");
        commonRecipeExt.setDecoctionCode("煎法code");
        commonRecipeExt.setMakeMethod("制法code");
        commonRecipeExt.setJuice("0.23");
        commonRecipeExt.setJuiceUnit("ml");
        commonRecipeExt.setMinor("10.00");
        commonRecipeExt.setMinorUnit("ml");
        commonRecipeExt.setCopyNum(7);
        commonRecipeExt.setEntrust("测试备注");
        commonDTO.setCommonRecipeExt(commonRecipeExt);

        List<CommonRecipeDrugDTO> commonRecipeDrugList = new LinkedList<>();
        CommonRecipeDrugDTO commonRecipeDrugDTO = new CommonRecipeDrugDTO();
        commonRecipeDrugDTO.setCommonRecipeCode("code124");
        commonRecipeDrugDTO.setPharmacyCode("123");
        commonRecipeDrugDTO.setPharmacyCategray(Arrays.asList("西药", "中药"));
        commonRecipeDrugDTO.setOrganDrugCode("OrganDrugCode");
        commonRecipeDrugDTO.setDrugEntrustCode("药品嘱托编码");
        commonRecipeDrugDTO.setMemo("药品嘱托test");
        commonRecipeDrugDTO.setUsingRate("频率代码");
        commonRecipeDrugDTO.setUsePathways("途径代码");


        commonRecipeDrugDTO.setDrugName("阿莫西林");
        commonRecipeDrugDTO.setSaleName("阿莫西林");
        commonRecipeDrugDTO.setDrugUnit("30mg*7片");
        commonRecipeDrugDTO.setDrugSpec("盒");
        commonRecipeDrugDTO.setUseTotalDose(2.000);
        commonRecipeDrugDTO.setDefaultUseDose(2.0);
        commonRecipeDrugDTO.setSalePrice(new BigDecimal(29.46000));
        commonRecipeDrugDTO.setPrice1(29.46000);
        commonRecipeDrugDTO.setDrugCost(new BigDecimal(29.46000));


        commonRecipeDrugList.add(commonRecipeDrugDTO);
        commonDTO.setCommonRecipeDrugList(commonRecipeDrugList);
        commonDTOList.add(commonDTO);
        hisResponse.setData(commonDTOList);
        return hisResponse;
    }
}
