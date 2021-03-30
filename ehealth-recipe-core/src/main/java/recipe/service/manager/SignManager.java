package recipe.service.manager;

import com.alibaba.fastjson.JSON;
import com.ngari.patient.dto.DoctorDTO;
import com.ngari.patient.service.DoctorService;
import com.ngari.recipe.entity.sign.SignDoctorRecipeInfo;
import com.ngari.recipe.recipe.model.AttachSealPicDTO;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.constant.CARecipeTypeConstant;
import recipe.service.client.IConfigurationClient;
import recipe.sign.SignRecipeInfoService;

/**
 * 签名处理通用类
 *
 * @author fuzi
 */
@Service
public class SignManager {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    @Autowired
    private IConfigurationClient configurationClient;
    @Autowired
    private DoctorService doctorService;
    @Autowired
    private SignRecipeInfoService signRecipeInfoService;

    /**
     * 根据配置项sealDataFrom获取签章图片
     *
     * @param doctorId
     * @param
     * @Author liumin
     */
    public AttachSealPicDTO attachSealPic(Integer organId, Integer doctorId, Integer checker, Integer recipeId) {
        logger.info("SignManager attachSealPic param organId:{},doctorId:{},checker:{},recipeId:{}", organId, doctorId, checker, recipeId);
        AttachSealPicDTO attachSealPicDTO = new AttachSealPicDTO();
        //根据ca配置：判断签章显示是显示第三方的签章还是平台签章还是线下手签，默认使用平台签章
        String sealDataFrom = configurationClient.getValueCatch(organId, "sealDataFrom", "platFormSeal");
        if ("thirdSeal".equals(sealDataFrom)) {
            //获取第三方手签图片
            attachSealPicDTO.setDoctorSignImg(thirdSeal(recipeId, CARecipeTypeConstant.CA_RECIPE_DOC));
            attachSealPicDTO.setCheckerSignImg(thirdSeal(recipeId, CARecipeTypeConstant.CA_RECIPE_PHA));
        } else if ("offlineSeal".equals(sealDataFrom)) {
            //线下手签
            attachSealPicDTO.setDoctorSignImg(offlineSeal(doctorId));
            attachSealPicDTO.setCheckerSignImg(offlineSeal(checker));
        } else {
            //平台手签
            attachSealPicDTO.setDoctorSignImg(platFormSeal(doctorId));
            attachSealPicDTO.setCheckerSignImg(platFormSeal(checker));
        }
        logger.info("SignManager attachSealPic attachSealPicDTO:{}", JSON.toJSONString(attachSealPicDTO));
        return attachSealPicDTO;
    }

    /**
     * 获取第三方手签图片
     *
     * @param recipeId 处方id
     * @return
     */
    private String thirdSeal(Integer recipeId, Integer type) {
        logger.info("thirdSeal 使用第三方签名，recipeId:{},type:{}", recipeId, type);
        try {
            SignDoctorRecipeInfo docInfo = signRecipeInfoService.getSignInfoByRecipeIdAndServerType(recipeId, type);
            if (null == docInfo) {
                return null;
            }
            return docInfo.getSignPictureDoc();
        } catch (Exception e) {
            logger.warn("thirdSeal 使用第三方签名，recipeId:{},type:{}", recipeId, type, e);
            return null;
        }
    }

    /**
     * 如果线上处方设置成线下手签
     *
     * @param doctorId 医生/药师id
     * @return
     */
    private String offlineSeal(Integer doctorId) {
        logger.info("offlineSeal 如果线上处方设置成线下手签，doctorId:{}", doctorId);
        try {
            String signImgId = signRecipeInfoService.getOfflineCaPictureByDocId(doctorId);
            if (StringUtils.isEmpty(signImgId)) {
                return null;
            }
            return signImgId;
        } catch (Exception e) {
            logger.warn("offlineSeal 如果线上处方设置成线下手签，doctorId:{}", doctorId, e);
            return null;
        }
    }

    /**
     * 平台手签
     *
     * @param doctorId 医生/药师id
     * @return
     */
    private String platFormSeal(Integer doctorId) {
        logger.info("(Integer doctorId 平台手签，doctorId:{}", doctorId);
        try {
            DoctorDTO doctorDTO = doctorService.getByDoctorId(doctorId);
            if (null == doctorDTO) {
                return null;
            }
            return doctorDTO.getSignImage();
        } catch (Exception e) {
            logger.warn("(Integer doctorId 平台手签，doctorId:{}", doctorId, e);
            return null;
        }
    }
}
