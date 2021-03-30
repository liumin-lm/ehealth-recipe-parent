package recipe.service.manager;

import com.alibaba.fastjson.JSON;
import com.ngari.patient.dto.DoctorDTO;
import com.ngari.patient.service.DoctorService;
import com.ngari.recipe.entity.sign.SignDoctorRecipeInfo;
import com.ngari.recipe.recipe.model.AttachSealPicDTO;
import ctd.util.FileAuth;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.constant.CARecipeTypeConstant;
import recipe.service.client.IConfigurationClient;
import recipe.sign.SignRecipeInfoService;
import recipe.util.ValidateUtil;

/**
 * 签名处理通用类
 *
 * @author fuzi
 */
@Service
public class SignManager {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    /**
     * 第三方手签
     */
    public static final String CA_SEAL_THIRD = "thirdSeal";
    /**
     * 线下手签
     */
    public static final String CA_SEAL_OFFLINE = "offlineSeal";
    /**
     * 平台手签
     */
    public static final String CA_SEAL_PLAT_FORM = "platFormSeal";

    @Autowired
    private IConfigurationClient configurationClient;
    @Autowired
    private DoctorService doctorService;
    @Autowired
    private SignRecipeInfoService signRecipeInfoService;

    /**
     * 获取 发药药师签名图片id
     *
     * @param organId  机构id
     * @param giveUser 发药药师id
     * @param recipeId 处方id
     * @return
     */
    public AttachSealPicDTO giveUser(Integer organId, String giveUser, Integer recipeId) {
        AttachSealPicDTO attachSealPicDTO = new AttachSealPicDTO();
        if (StringUtils.isNotEmpty(giveUser)) {
            Integer giveUserId;
            try {
                giveUserId = Integer.valueOf(giveUser);
            } catch (Exception e) {
                giveUserId = null;
            }
            String signImg = signImg(organId, giveUserId, recipeId, CARecipeTypeConstant.CA_RECIPE_PHA);
            if (StringUtils.isNotEmpty(signImg)) {
                attachSealPicDTO.setGiveUserSignImg(signImg);
            }
            signImg = platFormSeal(giveUserId);
            if (StringUtils.isNotEmpty(signImg)) {
                attachSealPicDTO.setGiveUserSignImg(signImg);
            }
        } else {
            DoctorDTO defaultGiveUser = oragnDefaultDispensingApothecary(organId);
            if (null != defaultGiveUser && StringUtils.isNotEmpty(defaultGiveUser.getSignImage())) {
                attachSealPicDTO.setGiveUserSignImg(defaultGiveUser.getSignImage());
            }
        }
        if (StringUtils.isNotEmpty(attachSealPicDTO.getGiveUserSignImg())) {
            String giveUserSignImgToken = FileAuth.instance().createToken(attachSealPicDTO.getGiveUserSignImg(), 3600L);
            attachSealPicDTO.setGiveUserSignImgToken(giveUserSignImgToken);
        }
        return attachSealPicDTO;
    }

    /**
     * 获取 机构默认发药药师
     *
     * @param organId
     * @return
     */
    public DoctorDTO oragnDefaultDispensingApothecary(Integer organId) {
        String giveUserId = configurationClient.getValueCatch(organId, "oragnDefaultDispensingApothecary", "");
        if (StringUtils.isEmpty(giveUserId)) {
            return null;
        }
        DoctorDTO dispensingApothecary = doctorService.get(Integer.valueOf(giveUserId));
        if (null == dispensingApothecary) {
            return null;
        }
        return dispensingApothecary;
    }

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
        attachSealPicDTO.setDoctorSignImg(signImg(organId, doctorId, recipeId, CARecipeTypeConstant.CA_RECIPE_DOC));
        attachSealPicDTO.setCheckerSignImg(signImg(organId, checker, recipeId, CARecipeTypeConstant.CA_RECIPE_PHA));
        logger.info("SignManager attachSealPic attachSealPicDTO:{}", JSON.toJSONString(attachSealPicDTO));
        return attachSealPicDTO;
    }

    /**
     * 获取手签图片
     *
     * @param organId  机构id
     * @param doctorId 医生/药师id
     * @param recipeId 处方id
     * @param type     职业类型：医生/药师
     * @return
     */
    private String signImg(Integer organId, Integer doctorId, Integer recipeId, Integer type) {
        logger.info("SignManager signImg param organId:{},doctorId:{},recipeId:{},recipeId:{}", organId, doctorId, recipeId, type);
        if (ValidateUtil.integerIsEmpty(organId) || ValidateUtil.integerIsEmpty(doctorId) || ValidateUtil.integerIsEmpty(recipeId)) {
            return null;
        }
        //根据ca配置：判断签章显示是显示第三方的签章还是平台签章还是线下手签，默认使用平台签章
        String sealDataFrom = configurationClient.getValueCatch(organId, "sealDataFrom", CA_SEAL_PLAT_FORM);
        if (CA_SEAL_THIRD.equals(sealDataFrom)) {
            return thirdSeal(recipeId, type);
        } else if (CA_SEAL_OFFLINE.equals(sealDataFrom)) {
            return offlineSeal(doctorId);
        } else {
            return platFormSeal(doctorId);
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
        if (ValidateUtil.integerIsEmpty(doctorId)) {
            return null;
        }
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

    /**
     * 获取第三方手签图片
     *
     * @param recipeId 处方id
     * @return
     */
    private String thirdSeal(Integer recipeId, Integer type) {
        logger.info("thirdSeal 使用第三方签名，recipeId:{},type:{}", recipeId, type);
        if (ValidateUtil.integerIsEmpty(recipeId) || ValidateUtil.integerIsEmpty(type)) {
            return null;
        }
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
        if (ValidateUtil.integerIsEmpty(doctorId)) {
            return null;
        }
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

}
