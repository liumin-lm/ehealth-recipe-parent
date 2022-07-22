package recipe.client;

import ca.service.ICaRemoteService;
import ca.service.ISignRecipeInfoService;
import ca.vo.CaSignResultVo;
import ca.vo.model.SignDoctorRecipeInfoDTO;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.ngari.his.ca.model.CaSealRequestTO;
import com.ngari.patient.utils.ObjectCopyUtils;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.Recipedetail;
import com.ngari.upload.service.IFileUploadService;
import eh.utils.params.ParamUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.util.ByteUtils;
import sun.misc.BASE64Decoder;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * 签名相关接口
 *
 * @author yinsheng
 */
@Service
public class CaClient extends BaseClient {
    @Autowired
    private ISignRecipeInfoService signRecipeInfoService;
    @Autowired
    private IConfigurationClient iConfigurationClient;
    @Autowired
    private ICaRemoteService iCaRemoteService;
    @Autowired
    private IFileUploadService iFileUploadService;

    public void signRecipeInfoSave(Integer recipeId, boolean isDoctor, CaSignResultVo signResultVo, Integer organId) {
        String thirdCASign = iConfigurationClient.getValueCatch(organId, "thirdCASign", "");
        try {
            //上海儿童特殊处理
            String value = ParamUtils.getParam("SH_CA_ORGANID_WHITE_LIST");
            List<String> caList = Arrays.asList(value.split(ByteUtils.COMMA));
            if (caList.contains(organId + "")) {
                thirdCASign = "shanghaiCA";
            }
            signRecipeInfoService.saveSignInfo(recipeId, isDoctor, ObjectCopyUtils.convert(signResultVo, ca.vo.CaSignResultVo.class), thirdCASign);
        } catch (Exception e) {
            logger.info("signRecipeInfoService error recipeId[{}] errorMsg[{}]", recipeId, e.getMessage(), e);
        }
    }

    /**
     * 保存sign
     *
     * @param recipe
     * @param details
     */
    public void signUpdate(Recipe recipe, List<Recipedetail> details) {
        SignDoctorRecipeInfoDTO signDoctorRecipeInfo = signRecipeInfoService.get(recipe.getRecipeId());
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("recipeBean", JSONObject.toJSONString(recipe));
        jsonObject.put("details", JSONObject.toJSONString(details));
        signDoctorRecipeInfo.setSignBefText(jsonObject.toJSONString());
        signRecipeInfoService.update(signDoctorRecipeInfo);
    }

    /**
     * 调用ca老二方包接口
     *
     * @param requestSeal
     * @param recipe
     * @param idNumber
     * @param caPassword
     */
    public void oldCommonCASign(CaSealRequestTO requestSeal, Recipe recipe, String idNumber, String caPassword) {
        //签名时的密码从redis中获取
        ca.vo.model.RecipeBean recipeBean = ObjectCopyUtils.convert(recipe, ca.vo.model.RecipeBean.class);
        iCaRemoteService.commonCASignAndSealForRecipe(requestSeal, recipeBean, recipe.getClinicOrgan(), idNumber, caPassword);
        logger.info("CaClient oldCommonCASign requestSeal=[{}]，recipeid={}", JSON.toJSONString(requestSeal), recipeBean.getRecipeId());
    }


    /**
     * 上传文件到oss服务器
     *
     * @param base64   文件
     * @param fileName 文件名
     * @return
     */
    public String signFileByte(String base64, String fileName) {
        if (null == base64) {
            return null;
        }
        BASE64Decoder d = new BASE64Decoder();
        byte[] data = new byte[0];
        try {
            data = d.decodeBuffer(base64);
        } catch (IOException e) {
            logger.info("CreateRecipePdfUtil signFileByte e ", e);
        }
        return signFileByte(data, fileName);
    }

    /**
     * 上传文件到oss服务器
     *
     * @param bytes    文件
     * @param fileName 文件名
     * @return
     */
    private String signFileByte(byte[] bytes, String fileName) {
        String fileId = iFileUploadService.uploadFileWithoutUrt(bytes, fileName);
        if (null == fileId) {
            return "";
        }
        return fileId;
    }

}
