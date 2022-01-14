package recipe.client;

import com.alibaba.fastjson.JSON;
import com.ngari.patient.dto.DoctorDTO;
import com.ngari.patient.dto.DoctorExtendDTO;
import com.ngari.patient.service.DoctorExtendService;
import com.ngari.patient.service.DoctorService;
import com.ngari.patient.service.EmploymentService;
import com.ngari.recipe.dto.ApothecaryDTO;
import com.ngari.recipe.entity.Recipe;
import ctd.util.JSONUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.util.ByteUtils;
import recipe.util.RecipeUtil;
import recipe.util.ValidateUtil;

/**
 * 获取医生信息处理类
 *
 * @author fuzi
 */
@Service
public class DoctorClient extends BaseClient {
    @Autowired
    private DoctorService doctorService;
    @Autowired
    private IConfigurationClient configurationClient;
    @Autowired
    private DoctorExtendService doctorExtendService;
    @Autowired
    private EmploymentService iEmploymentService;

    /**
     * 获取平台药师信息 无选择发药药师 则获取默认发药药师
     *
     * @param recipe 处方
     * @return
     */
    public ApothecaryDTO getGiveUserDefault(Recipe recipe) {
        ApothecaryDTO apothecaryDTO = getApothecary(recipe);
        if (StringUtils.isNotEmpty(apothecaryDTO.getGiveUserName())) {
            return apothecaryDTO;
        }
        DoctorDTO doctorDTO = oragnDefaultDispensingApothecary(recipe.getClinicOrgan());
        apothecaryDTO.setGiveUserId(doctorDTO.getDoctorId());
        apothecaryDTO.setGiveUserIdCard(doctorDTO.getIdNumber());
        apothecaryDTO.setGiveUserName(doctorDTO.getName());
        apothecaryDTO.setGiveUserSignImg(doctorDTO.getSignImage());
        return apothecaryDTO;
    }

    /**
     * 获取药师信息用于前端展示
     *
     * @param recipe
     * @return
     */
    public ApothecaryDTO getApothecary(Recipe recipe) {
        logger.info("DoctorClient getApothecary recipe:{} ", JSON.toJSONString(recipe));
        ApothecaryDTO apothecaryDTO = getGiveUser(recipe);
        Integer apothecaryId = recipe.getChecker();
        if (!ValidateUtil.integerIsEmpty(apothecaryId)) {
            DoctorDTO doctorDTO = getDoctor(apothecaryId);
            apothecaryDTO.setCheckApothecaryIdCard(doctorDTO.getIdNumber());
            apothecaryDTO.setCheckApothecaryName(doctorDTO.getName());
        }
        logger.info("DoctorClient getApothecary apothecaryVO:{} ", JSONUtils.toString(apothecaryDTO));
        return apothecaryDTO;
    }

    /**
     * 获取 核发药师
     *
     * @param recipe
     * @return
     */
    public ApothecaryDTO getGiveUser(Recipe recipe) {
        logger.info("DoctorClient getGiveUser recipe:{} ", JSONUtils.toString(recipe));
        ApothecaryDTO apothecaryDTO = new ApothecaryDTO();
        apothecaryDTO.setRecipeId(recipe.getRecipeId());
        if (StringUtils.isEmpty(recipe.getGiveUser())) {
            return apothecaryDTO;
        }
        Integer giveUserId = ByteUtils.strValueOf(recipe.getGiveUser());
        if (!ValidateUtil.integerIsEmpty(giveUserId)) {
            DoctorDTO doctorDTO = getDoctor(giveUserId);
            apothecaryDTO.setGiveUserIdCardCleartext(doctorDTO.getIdNumber());
            apothecaryDTO.setGiveUserIdCard(doctorDTO.getIdNumber());
            apothecaryDTO.setGiveUserId(doctorDTO.getDoctorId());
            apothecaryDTO.setGiveUserName(doctorDTO.getName());
            apothecaryDTO.setGiveUserSignImg(doctorDTO.getSignImage());
        }
        logger.info("DoctorClient getGiveUser apothecaryVO:{} ", JSONUtils.toString(apothecaryDTO));
        return apothecaryDTO;
    }

    /**
     * 获取医生信息
     *
     * @param doctorId 医生id
     * @return
     */
    public DoctorDTO getDoctor(Integer doctorId) {
        if (ValidateUtil.integerIsEmpty(doctorId)) {
            logger.info("DoctorClient getDoctor  doctorId:{}", doctorId);
            return new DoctorDTO();
        }
        try {
            DoctorDTO doctorDTO = doctorService.getByDoctorId(doctorId);
            if (null == doctorDTO) {
                return new DoctorDTO();
            }
            return doctorDTO;
        } catch (Exception e) {
            logger.warn("DoctorClient getDoctor doctorId:{}", doctorId, e);
            return new DoctorDTO();
        }
    }

    /**
     * 获取 医生工号
     *
     * @param organId  机构id
     * @param doctorId 医生id
     * @param departId 开方科室id
     * @return 医生工号
     */
    public DoctorDTO jobNumber(Integer organId, Integer doctorId, Integer departId) {
        if (ValidateUtil.validateObjects(organId, doctorId, departId)) {
            logger.info("DoctorClient jobNumber organId:{} ,doctorId:{}, departId:{}", organId, doctorId, departId);
            return new DoctorDTO();
        }
        try {
            DoctorDTO doctorDTO = doctorService.getByDoctorId(doctorId);
            doctorDTO.setJobNumber(iEmploymentService.getJobNumberByDoctorIdAndOrganIdAndDepartment(doctorId, organId, departId));
            return doctorDTO;
        } catch (Exception e) {
            logger.warn("DoctorClient jobNumber doctorId:{}", doctorId, e);
            return new DoctorDTO();
        }
    }


    /**
     * 获取 机构默认发药药师
     *
     * @param organId 机构id
     * @return
     */
    public DoctorDTO oragnDefaultDispensingApothecary(Integer organId) {
        logger.info("DoctorClient oragnDefaultDispensingApothecary organId:{}", organId);
        String giveUserId = configurationClient.getValueCatch(organId, "oragnDefaultDispensingApothecary", "");
        if (StringUtils.isEmpty(giveUserId)) {
            return new DoctorDTO();
        }
        Integer id = ByteUtils.strValueOf(giveUserId);
        if (ValidateUtil.integerIsEmpty(id)) {
            return new DoctorDTO();
        }
        DoctorDTO dispensingApothecary = doctorService.get(id);
        if (null == dispensingApothecary) {
            return new DoctorDTO();
        }
        logger.info("DoctorClient oragnDefaultDispensingApothecary dispensingApothecary:{}", JSON.toJSONString(dispensingApothecary));
        return dispensingApothecary;
    }


    public String getOfflineCaPictureByDocId(Integer doctorId) {
        logger.info("SignRecipeInfoService getOfflineCaPictureByDocId doctorId=[{}]", doctorId);
        DoctorExtendDTO doctorExtend = doctorExtendService.getByDoctorId(doctorId);
        if (doctorExtend == null) {
            return null;
        }
        String fileId;
        if (StringUtils.isEmpty(doctorExtend.getPictureIdCA())) {
            fileId = RecipeUtil.uploadPicture(doctorExtend.geteSignature());
            doctorExtendService.updateCAPictureIdByDocId(fileId, doctorId);
        } else {
            fileId = doctorExtend.getPictureIdCA();
        }
        return fileId;
    }
}
