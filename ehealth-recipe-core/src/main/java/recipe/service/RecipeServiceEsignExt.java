package recipe.service;

import com.google.common.collect.Maps;
import com.ngari.base.BaseAPI;
import com.ngari.base.esign.service.IESignBaseService;
import com.ngari.common.mode.HisResponseTO;
import com.ngari.his.ca.model.*;
import com.ngari.his.ca.service.ICaHisService;
import com.ngari.patient.dto.DoctorDTO;
import com.ngari.patient.service.DoctorService;
import com.ngari.recipe.recipe.model.RecipeBean;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import com.ngari.recipe.recipe.service.IRecipeService;
import ctd.mvc.upload.FileMetaRecord;
import ctd.mvc.upload.FileService;
import ctd.persistence.exception.DAOException;
import ctd.util.AppContextHolder;
import ctd.util.JSONUtils;
import ctd.util.annotation.RpcService;
import eh.base.constant.ErrorCode;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.ApplicationUtils;
import sun.misc.BASE64Decoder;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * CA标准化对接文档
 */
public class RecipeServiceEsignExt {

    private static final Logger LOGGER = LoggerFactory.getLogger(RecipeServiceEsignExt.class);
    /**
     * 中药和西药的标识
     */
    private static final String TCM_TEMPLATETYPE = "tcm";

    private static IRecipeService recipeService = AppContextHolder.getBean("eh.remoteRecipeService", IRecipeService.class);

    private static IESignBaseService esignService = ApplicationUtils.getBaseService(IESignBaseService.class);


    /**
     * 获取移动端获取pdf文件、用于SDK进行签章
     * @param recipeId
     * @return
     * @throws Exception
     */
    @RpcService
    public static CaSealRequestTO signCreateRecipePDF(Integer recipeId,boolean isDoctor) {
        CaSealRequestTO caBean = new CaSealRequestTO();
        if (null == recipeId) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "recipeId is null");
        }
        RecipeBean recipe = recipeService.getByRecipeId(recipeId);
        List<RecipeDetailBean> details = recipeService.findRecipeDetailsByRecipeId(recipeId);
        //组装生成pdf的参数
        String fileName = "recipe_" + recipeId + ".pdf";
        recipe.setSignDate(DateTime.now().toDate());
        Map<String, Object> paramMap = recipeService.createRecipeParamMapForPDF(recipe.getRecipeType(),recipe,details,fileName);
        String pdf = esignService.createSignRecipePDF(paramMap);
        //中药
        if (TCM_TEMPLATETYPE.equals(recipe.getRecipeType())) {
            //医生还是药师
            if (isDoctor) {
                caBean.setLeftX(55);
                caBean.setLeftY(370);
            } else {
                caBean.setLeftX(390);
                caBean.setLeftY(30);
            }
        //西药
        } else {
            //医生还是药师
            if (isDoctor) {
                caBean.setLeftX(320);
                caBean.setLeftY(735);
            } else{
                caBean.setLeftX(500);
                caBean.setLeftY(90);
            }
        }
        caBean.setSealHeight(40);
        caBean.setSealWidth(40);
        caBean.setPage(1);
        caBean.setPdfBase64Str(pdf);
        caBean.setPdfName(fileName);
        caBean.setPdfMd5("");
        caBean.setMode(1);
        return caBean;
    }
    /**
     * 移动端签章完成后、将签章后的pdf文件绑定处方、并上传文件服务器
     * @param recipeId
     * @return
     * @throws Exception
     */
    @RpcService
    public static String saveSignRecipePDF(String pdfBase64,Integer recipeId, String loginId,String signCADate,
                                    String signRecipeCode,Boolean isDoctor){
        LOGGER.info("saveSignRecipePDF start in pdfBase64={}, recipeId={}, loginId={}", pdfBase64,recipeId,loginId);
        //组装生成pdf的参数
        String fileName = "recipe_" + recipeId + ".pdf";
        String fileId = null;
        if (null != pdfBase64) {
            BASE64Decoder d = new BASE64Decoder();
            byte[] data = new byte[0];
            try {
                data = d.decodeBuffer(pdfBase64);
            } catch (IOException e) {
                e.printStackTrace();
            }
            fileId = uploadRecipeSignFile(data, fileName, loginId);
            if (null == fileId) {
                LOGGER.info( "上传文件失败,fileName=" + fileName);
                return "fail";
            }
        }
        Map<String, Object> attrMap = Maps.newHashMap();

        attrMap.put("signFile", fileId);
        attrMap.put("signDate", new Date());

        if(isDoctor) {
            //医生签名时间戳
            attrMap.put("signCADate", signCADate);
            //医生签名值
            attrMap.put("signRecipeCode", signRecipeCode);
        } else {
            //药师签名时间戳
            attrMap.put("signPharmacistCADate",signCADate);
            //药师签名值
            attrMap.put("signPharmacistCode",signRecipeCode);
        }

        boolean upResult = recipeService.updateRecipeInfoByRecipeId(recipeId, attrMap);

        LOGGER.info("saveSignRecipePDF upResult={}", upResult);
        if (upResult) {
            LOGGER.info("saveSignRecipePDF 签名成功. fileId={}, recipeId={}", fileId, recipeId);
        } else  {
            LOGGER.info("saveSignRecipePDF 签名失败. fileId={}, recipeId={}", fileId, recipeId);
            return "fail";
        }
        return "success";
    }

    /**
     * 上传电子签名文件， 处方在用
     *
     * @param data
     * @param fileName
     * @param userId
     * @return
     */
    private static String uploadRecipeSignFile(byte[] data, String fileName, String userId) {
        if (null == data) {
            throw new DAOException(ErrorCode.SERVICE_ERROR, "byte[] 为空");
        }
        OutputStream fileOutputStream = null;
        File file = null;
        try {
            //先生成本地文件
            file = new File(fileName);
            fileOutputStream = new FileOutputStream(file);
            if (data.length > 0) {
                fileOutputStream.write(data, 0, data.length);
                fileOutputStream.flush();
            }

            FileMetaRecord meta = new FileMetaRecord();
            //TODO 暂时写死
            meta.setManageUnit("eh");
            meta.setOwner(userId);
            meta.setLastModify(new Date());
            meta.setUploadTime(new Date());
            meta.setMode(0);
            meta.setNotes("recipe");
            meta.setCatalog("other-doc"); // 测试
            meta.setContentType("application/pdf");
            meta.setFileName(fileName);
            meta.setFileSize(file.length());
            FileService.instance().upload(meta, file);
            file.delete();
            return meta.getFileId();
        } catch (Exception e) {
            LOGGER.error("uploadRecipeSignFile exception:" + e.getMessage());
        } finally {
            try {
                fileOutputStream.close();
            } catch (Exception e) {
                LOGGER.error("uploadRecipeSignFile exception:" + e.getMessage());
            }
        }
        return null;
    }
}
