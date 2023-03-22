package recipe.manager;

import ca.vo.CaSignResultBean;
import ca.vo.CaSignResultVo;
import com.alibaba.fastjson.JSON;
import com.ngari.base.BaseAPI;
import com.ngari.base.property.service.IConfigurationCenterUtilsService;
import com.ngari.his.ca.model.CaSealRequestTO;
import com.ngari.patient.dto.DoctorDTO;
import com.ngari.recipe.dto.ApothecaryDTO;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.Recipedetail;
import com.ngari.recipe.entity.sign.SignDoctorRecipeInfo;
import ctd.util.FileAuth;
import ctd.util.JSONUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.aop.LogRecord;
import recipe.client.CaClient;
import recipe.client.DoctorClient;
import recipe.client.RecipeAuditClient;
import recipe.constant.CARecipeTypeConstant;
import recipe.constant.CaConstant;
import recipe.dao.RecipeParameterDao;
import recipe.dao.sign.SignDoctorRecipeInfoDAO;
import recipe.enumerate.status.RecipeStatusEnum;
import recipe.enumerate.status.SignEnum;
import recipe.util.ByteUtils;
import recipe.util.DateConversion;
import recipe.util.RedisClient;
import recipe.util.ValidateUtil;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * CA
 *
 * @author liumin
 * @date 2022\6\17 0030 14:21
 */
@Service
public class CaManager extends BaseManager {
    private static final Integer CA_OLD_TYPE = new Integer(0);
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
    /**
     * 运营平台机构CA配置
     */
    private String THIRD_CA_SIGN = "thirdCASign";

    /**
     * 陕西CA
     */
    private String CA_TYPE_SHANXI = "shanxiCA";

    /**
     * 陕西CA密码前缀
     */
    private String SHANXI_CA_PASSWORD = "SHANXI_CA_PASSWORD";
    @Autowired
    private DoctorClient doctorClient;
    @Autowired
    private SignDoctorRecipeInfoDAO signDoctorRecipeInfoDAO;
    @Autowired
    private RecipeAuditClient recipeAuditClient;
    @Autowired
    private RedisClient redisClient;
    @Autowired
    private CaClient caClient;
    @Autowired
    private RecipeParameterDao recipeParameterDao;

    /**
     * 设置CA密码
     *
     * @param clinicOrgan
     * @param doctor
     * @param caPassword
     */
    @LogRecord
    public void setCaPassWord(Integer clinicOrgan, Integer doctor, String caPassword) {
        try {
            //将密码放到redis中
            IConfigurationCenterUtilsService configurationService = BaseAPI.getService(IConfigurationCenterUtilsService.class);
            String thirdCASign = (String) configurationService.getConfiguration(clinicOrgan, THIRD_CA_SIGN);
            if (CA_TYPE_SHANXI.equals(thirdCASign)) {
                redisClient.set(SHANXI_CA_PASSWORD + clinicOrgan + doctor, caPassword);
            } else {
                redisClient.set("caPassword", caPassword);
            }
        } catch (Exception e) {
            logger.error("setCaPassWord error", e);
        }
    }

    /**
     * 获取CA密码
     *
     * @param clinicOrgan
     * @param doctor
     * @return
     */
    @LogRecord
    public String getCaPassWord(Integer clinicOrgan, Integer doctor) {
        String caPassword = "";
        IConfigurationCenterUtilsService configurationService = BaseAPI.getService(IConfigurationCenterUtilsService.class);
        String thirdCASign = (String) configurationService.getConfiguration(clinicOrgan, THIRD_CA_SIGN);
        if (CA_TYPE_SHANXI.equals(thirdCASign)) {
            caPassword = redisClient.get(SHANXI_CA_PASSWORD + clinicOrgan + doctor);
        } else {
            caPassword = redisClient.get("caPassword");
        }
        caPassword = null == caPassword ? "" : caPassword;
        return caPassword;
    }

    /**
     * 获取ca新老流程状态
     *
     * @param recipe
     * @return
     */
    public Integer caProcessType(Recipe recipe) {
        if (null == recipe) {
            logger.warn("当前处方不存在!");
            return null;
        }
        //处方签名中 点击撤销按钮 如果处方单状态处于已取消 则不走下面逻辑
        if (RecipeStatusEnum.RECIPE_STATUS_REVOKE.getType().equals(recipe.getStatus())) {
            logger.info("retryDoctorSignCheck 处方单已经撤销，recipeid：{}", recipe.getRecipeId());
            return null;
        }
        Integer caType = configurationClient.getValueCatchReturnInteger(recipe.getClinicOrgan(), "CAProcessType", CA_OLD_TYPE);
        logger.info("RecipeService retryDoctorSignCheck CANewOldWay = {}", caType);
        return caType;
    }

    /**
     * 老流程保存sign，新流程已经移动至CA保存
     *
     * @param recipe
     * @param resultVo
     * @param isDoctor
     */
    public void signRecipeInfoSave(Recipe recipe, CaSignResultVo resultVo, boolean isDoctor) {
        caClient.signRecipeInfoSave(recipe.getRecipeId(), isDoctor, resultVo, recipe.getClinicOrgan());
    }

    /**
     * 老流程保存sign，
     *
     * @param recipe
     * @param details
     * @param resultVo
     * @param isDoctor
     */
    public void oldCaCallBack(Recipe recipe, List<Recipedetail> details, CaSignResultVo resultVo, boolean isDoctor, String base64) {
        boolean usePlatform = configurationClient.getValueBooleanCatch(recipe.getClinicOrgan(), "recipeUsePlatformCAPDF", true);
        if (!usePlatform && StringUtils.isNotEmpty(base64)) {
            try {
                String fileId = caClient.signFileByte(base64, "recipe_" + recipe.getRecipeId() + ".pdf");
                Recipe recipeUpdate = new Recipe();
                recipeUpdate.setRecipeId(recipe.getRecipeId());
                recipeUpdate.setSignFile(fileId);
                recipeDAO.updateNonNullFieldByPrimaryKey(recipeUpdate);
                resultVo.setFileId(fileId);
            } catch (Exception e) {
                logger.error("CaManager oldCaCallBack error recipeId={}", recipe.getRecipeId(), e);
            }
        }
        caClient.signRecipeInfoSave(recipe.getRecipeId(), isDoctor, resultVo, recipe.getClinicOrgan());
        caClient.signUpdate(recipe, details);
        logger.info("CaManager oldCaCallBack recipeId={}", recipe.getRecipeId());
    }

    /**
     * 保存E签宝调用记录
     * @param recipe 处方信息
     * @param isDoctor true:医生 false药师
     */
    public void saveESignResult(Recipe recipe, boolean isDoctor){
        try {
            CaSignResultBean caSignResult=new CaSignResultBean();
            caSignResult.setBussId(recipe.getRecipeId());
            caSignResult.setBusstype(isDoctor ? 1:3); //1 处方 3药师
            caSignResult.setDoctorId(isDoctor?recipe.getDoctor():recipe.getChecker());
            caSignResult.setOrganId(recipe.getClinicOrgan());
            caSignResult.setSignDate((null==recipe.getSignDate()?DateConversion.formatDateTimeWithSec(new Date()):DateConversion.formatDateTimeWithSec(recipe.getSignDate())));
//            caSignResult.setSignFileDoc("签名文件")//文件id
//            caSignResult.setCertificate("ca证书base64");//对应SignRemarkDoc字段
            caClient.saveCaSignResult(caSignResult);
        } catch (Exception e) {
            logger.info("saveESignResult error recipeId[{}] errorMsg[{}]", recipe.getRecipeId(), e.getMessage(), e);
        }
    }

    /**
     * 调用ca老二方包签名接口-ca回调接口为retryCaDoctorCallBackToRecipe
     *
     * @param requestSeal ca入参数
     * @param recipe
     */
    public void oldCommonCASign(CaSealRequestTO requestSeal, Recipe recipe) {
        //签名时的密码从redis中获取
        String caPassword = this.getCaPassWord(recipe.getClinicOrgan(), recipe.getDoctor());
        DoctorDTO doctor = doctorClient.getDoctor(recipe.getDoctor());
        caClient.oldCommonCASign(requestSeal, recipe, doctor.getIdNumber(), caPassword);
        logger.info("generateRecipePdfAndSign 签名成功. 标准对接CA模式, recipeId={}", recipe.getRecipeId());
    }


    /**
     * 获取全部药师签名信息
     *
     * @param organId  机构id
     * @param doctorId 医生id
     * @param checker  审方药师id
     * @param giveUser 发药药师id
     * @param recipeId 处方id
     * @return
     */
    public ApothecaryDTO attachSealPic(Integer organId, Integer doctorId, Integer checker, String giveUser, Integer recipeId) {
        ApothecaryDTO apothecaryDTO = attachSealPic(organId, doctorId, checker, recipeId);
        ApothecaryDTO giveUserApothecary = giveUser(organId, giveUser, recipeId);
        apothecaryDTO.setGiveUserId(giveUserApothecary.getGiveUserId());
        apothecaryDTO.setGiveUserName(giveUserApothecary.getGiveUserName());
        apothecaryDTO.setGiveUserSignImg(giveUserApothecary.getGiveUserSignImg());
        apothecaryDTO.setGiveUserSignImgToken(giveUserApothecary.getGiveUserSignImgToken());
        return apothecaryDTO;
    }

    /**
     * 获取医生药师签名信息
     *
     * @param organId  机构id
     * @param doctorId 医生id
     * @param checker  审方药师id
     * @param recipeId 处方id
     * @return
     */
    public ApothecaryDTO attachSealPic(Integer organId, Integer doctorId, Integer checker, Integer recipeId) {
        logger.info("SignManager attachSealPic param organId:{},doctorId:{},checker:{},recipeId:{}", organId, doctorId, checker, recipeId);
        ApothecaryDTO attachSealPicDTO = new ApothecaryDTO();
        attachSealPicDTO.setRecipeId(recipeId);
        attachSealPicDTO.setDoctorSignImg(signImg(organId, doctorId, recipeId, CARecipeTypeConstant.CA_RECIPE_DOC));
        if (StringUtils.isNotEmpty(attachSealPicDTO.getDoctorSignImg())) {
            attachSealPicDTO.setDoctorId(doctorId);
            attachSealPicDTO.setDoctorSignImgToken(FileAuth.instance().createToken(attachSealPicDTO.getDoctorSignImg(), 3600L));
        }
        if (recipeAuditClient.isShowCheckCA(recipeId)) {
            attachSealPicDTO.setCheckerSignImg(signImg(organId, checker, recipeId, CARecipeTypeConstant.CA_RECIPE_PHA));
            if (StringUtils.isNotEmpty(attachSealPicDTO.getCheckerSignImg())) {
                attachSealPicDTO.setCheckerId(checker);
                attachSealPicDTO.setCheckerSignImgToken(FileAuth.instance().createToken(attachSealPicDTO.getCheckerSignImg(), 3600L));
            }
        }
        logger.info("SignManager attachSealPic attachSealPicDTO:{}", JSON.toJSONString(attachSealPicDTO));
        return attachSealPicDTO;
    }


    /**
     * 获取 发药药师签名图片id
     *
     * @param organId  机构id
     * @param giveUser 发药药师id
     * @param recipeId 处方id
     * @return
     */
    public ApothecaryDTO giveUser(Integer organId, String giveUser, Integer recipeId) {
        logger.info("SignManager giveUser organId:{} giveUser:{} recipeId:{}", organId, giveUser, recipeId);
        Integer giveUserId = ByteUtils.strValueOf(giveUser);
        ApothecaryDTO apothecaryDTO;
        if (!ValidateUtil.integerIsEmpty(giveUserId)) {
            apothecaryDTO = giveUser(organId, giveUserId, recipeId);
        } else {
            DoctorDTO doctorDTO = doctorClient.oragnDefaultDispensingApothecary(organId);
            apothecaryDTO = giveUser(organId, doctorDTO.getDoctorId(), recipeId);
        }
        if (StringUtils.isNotEmpty(apothecaryDTO.getGiveUserSignImg())) {
            String giveUserSignImgToken = FileAuth.instance().createToken(apothecaryDTO.getGiveUserSignImg(), 3600L);
            apothecaryDTO.setGiveUserSignImgToken(giveUserSignImgToken);
        }
        logger.info("SignManager giveUser attachSealPicDTO:{}", JSON.toJSONString(apothecaryDTO));
        return apothecaryDTO;
    }

    /**
     * 签名图片取值规则：根据运营平台-机构配置里面"处方单和处方笺签名取值配置"来定，拿不到在拿平台
     *
     * @param organId  机构id
     * @param doctorId 药师id
     * @param recipeId 处方id
     * @return
     */
    private ApothecaryDTO giveUser(Integer organId, Integer doctorId, Integer recipeId) {
        ApothecaryDTO apothecaryDTO = new ApothecaryDTO();
        apothecaryDTO.setGiveUserId(doctorId);
        Recipe recipe = new Recipe();
        recipe.setRecipeId(recipeId);
        recipe.setGiveUser(ByteUtils.objValueOf(doctorId));
        recipe.setClinicOrgan(organId);
        //获取签名图片
        String signImg = getSignImg(organId, doctorId, recipeId, CARecipeTypeConstant.CA_RECIPE_PHA);
        if (StringUtils.isNotEmpty(signImg)) {
            apothecaryDTO.setGiveUserSignImg(signImg);
        } else {
            ApothecaryDTO giveUserDefault = doctorClient.getGiveUser(recipe);
            apothecaryDTO.setGiveUserSignImg(giveUserDefault.getGiveUserSignImg());
        }
        return apothecaryDTO;
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
        logger.info("SignManager signImg param organId:{},doctorId:{},recipeId:{},type:{}", organId, doctorId, recipeId, type);
        if (ValidateUtil.integerIsEmpty(organId)) {
            return null;
        }
        if (new Integer(CARecipeTypeConstant.CA_RECIPE_PHA).equals(type)) {
            Recipe recipe = recipeDAO.get(recipeId);
            if (recipe == null) {
                return null;
            }
            if (new Integer(5).equals(recipe.getCheckMode())) {
                return thirdSeal(recipeId, type);
            }
        }
        //根据ca配置：判断签章显示是显示第三方的签章还是平台签章还是线下手签，默认使用平台签章
        String sealDataFrom = configurationClient.getValueCatch(organId, "sealDataFrom", CA_SEAL_PLAT_FORM);
        if (CA_SEAL_THIRD.equals(sealDataFrom)) {
            return thirdSeal(recipeId, type);
        } else if (CA_SEAL_OFFLINE.equals(sealDataFrom)) {
            return offlineSeal(doctorId);
        } else {
            //平台手签
            return doctorClient.getDoctor(doctorId).getSignImage();
        }
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
    private String getSignImg(Integer organId, Integer doctorId, Integer recipeId, Integer type) {
        logger.info("SignManager giveUser param organId:{},doctorId:{},recipeId:{},type:{}", organId, doctorId, recipeId, type);
        if (ValidateUtil.integerIsEmpty(organId)) {
            return null;
        }
        //根据ca配置：判断签章显示是显示第三方的签章还是平台签章还是线下手签，默认使用平台签章
        String sealDataFrom = configurationClient.getValueCatch(organId, "sealDataFrom", CA_SEAL_PLAT_FORM);
        if (CA_SEAL_THIRD.equals(sealDataFrom)) {
            return thirdSealV1(recipeId, doctorId, type);
        } else if (CA_SEAL_OFFLINE.equals(sealDataFrom)) {
            return offlineSeal(doctorId);
        } else {
            //平台手签
            return doctorClient.getDoctor(doctorId).getSignImage();
        }
    }


    /**
     * todo 新方法：thirdSealV1
     * 获取第三方手签图片
     *
     * @param recipeId 处方id
     * @return
     */
    @Deprecated
    private String thirdSeal(Integer recipeId, Integer type) {
        if (ValidateUtil.integerIsEmpty(recipeId) || ValidateUtil.integerIsEmpty(type)) {
            logger.info("SignManager thirdSeal 使用第三方签名，recipeId:{},type:{}", recipeId, type);
            return null;
        }
        try {
            SignDoctorRecipeInfo docInfo = getSignInfoByRecipeIdAndServerType(recipeId, type);
            if (null == docInfo) {
                return null;
            }
            return docInfo.getSignPictureDoc();
        } catch (Exception e) {
            logger.warn("SignManager thirdSeal 使用第三方签名，recipeId:{},type:{}", recipeId, type, e);
            return null;
        }
    }

    private String thirdSealV1(Integer recipeId, Integer doctorId, Integer type) {
        if (ValidateUtil.integerIsEmpty(recipeId) || ValidateUtil.integerIsEmpty(type) || ValidateUtil.integerIsEmpty(doctorId)) {
            logger.info("SignManager thirdSealV1 使用第三方签名，recipeId:{},type:{},doctorId:{}", recipeId, type, doctorId);
            return null;
        }
        try {
            SignDoctorRecipeInfo docInfo = getSignInfoByRecipeIdAndDoctorId(recipeId, doctorId, type);
            if (null == docInfo) {
                return null;
            }
            return docInfo.getSignPictureDoc();
        } catch (Exception e) {
            logger.warn("SignManager thirdSealV1 使用第三方签名，recipeId:{},type:{}", recipeId, type, e);
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
        if (ValidateUtil.integerIsEmpty(doctorId)) {
            logger.info("SignManager offlineSeal 如果线上处方设置成线下手签，doctorId:{}", doctorId);
            return null;
        }
        try {
            String signImgId = doctorClient.getOfflineCaPictureByDocId(doctorId);
            logger.info("SignManager offlineSeal 如果线上处方设置成线下手签，signImgId:{},doctorId:{}", signImgId, doctorId);
            if (StringUtils.isEmpty(signImgId)) {
                return null;
            }
            return signImgId;
        } catch (Exception e) {
            logger.warn("SignManager offlineSeal 如果线上处方设置成线下手签，doctorId:{}", doctorId, e);
            return null;
        }
    }


    private SignDoctorRecipeInfo getSignInfoByRecipeIdAndServerType(Integer recipeId, Integer serverType) {
        List<SignDoctorRecipeInfo> resultList = signDoctorRecipeInfoDAO.findRecipeInfoByRecipeIdAndServerType(recipeId, serverType);
        if (CollectionUtils.isNotEmpty(resultList)) {
            return resultList.get(0);
        } else {
            return null;
        }
    }

    private SignDoctorRecipeInfo getSignInfoByRecipeIdAndDoctorId(Integer recipeId, Integer doctorId, Integer serverType) {
        List<SignDoctorRecipeInfo> resultList = signDoctorRecipeInfoDAO.findSignInfoByRecipeIdAndDoctorId(recipeId, doctorId, serverType);
        if (CollectionUtils.isNotEmpty(resultList)) {
            return resultList.get(0);
        } else {
            return null;
        }
    }

    /**
     * 获取快捷购药ca方式
     *
     * @param recipe
     * @return
     */
    @LogRecord
    public String obtainFastRecipeCaParam(Recipe recipe) {
        String fastRecipeParameter = recipeParameterDao.getByName("fastRecipeParameter");
        List<Map<String, String>> fastRecipeParameterList = JSONUtils.parse(fastRecipeParameter, ArrayList.class);
        String ca = CaConstant.ESIGN;
        if (!CollectionUtils.isEmpty(fastRecipeParameterList)) {
            for (Map<String, String> map : fastRecipeParameterList) {
                if (String.valueOf(recipe.getClinicOrgan()).equals(map.get("organId"))) {
                    ca = map.get("ca");
                    return ca;
                }
            }
        }
        return ca;
    }

    /**
     * 便捷够药-药师签名
     *
     * @param recipe
     */
    public void caSignChecker(Recipe recipe) {
        String fastRecipeChecker = configurationClient.getValueCatch(recipe.getClinicOrgan(), "fastRecipeChecker", "");
        Integer checker = Integer.parseInt(fastRecipeChecker);
        DoctorDTO doctorDTO = doctorClient.getDoctor(checker);
        recipe.setChecker(checker);
        recipe.setCheckerText(doctorDTO.getName());
        recipe.setCheckDate(new Date());
        recipe.setCheckDateYs(new Date());
        recipe.setCheckOrgan(doctorDTO.getOrgan());
        recipe.setCheckFlag(1);
        recipe.setCheckerSignState(SignEnum.SIGN_STATE_SUBMIT.getType());
        recipeDAO.update(recipe);
        //调用药师签名
        recipeAuditClient.caSignChecker(recipe);
    }
}
