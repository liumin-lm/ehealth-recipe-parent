package recipe.service;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.ngari.base.esign.service.IESignBaseService;
import com.ngari.his.ca.model.CaSealRequestTO;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.recipe.model.RecipeBean;
import com.ngari.recipe.recipe.model.RecipeDetailBean;
import com.ngari.recipe.recipe.service.IRecipeService;
import ctd.mvc.upload.FileMetaRecord;
import ctd.mvc.upload.FileService;
import ctd.mvc.upload.exception.FileRegistryException;
import ctd.mvc.upload.exception.FileRepositoryException;
import ctd.persistence.DAOFactory;
import ctd.persistence.exception.DAOException;
import ctd.util.AppContextHolder;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import eh.base.constant.ErrorCode;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.util.encoders.Base64;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.ApplicationUtils;
import recipe.audit.auditmode.AuditModeContext;
import recipe.constant.RecipeStatusConstant;
import recipe.dao.RecipeDAO;
import sun.misc.BASE64Decoder;

import java.io.*;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * CA标准化对接文档
 */
@RpcBean
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
        //组装生成pdf的参数
        String fileName;
        RecipeBean recipe = recipeService.getByRecipeId(recipeId);
        String pdf = "";
        if (isDoctor) {
            fileName = "recipe_" + recipeId + ".pdf";
            List<RecipeDetailBean> details = recipeService.findRecipeDetailsByRecipeId(recipeId);
            recipe.setSignDate(DateTime.now().toDate());
            Map<String, Object> paramMap = recipeService.createRecipeParamMapForPDF(recipe.getRecipeType(), recipe, details, fileName);
            //这里走生成通过的平台模板（易签保开始使用）
            pdf = esignService.createSignRecipePDF(paramMap);
            //中药
            if (TCM_TEMPLATETYPE.equals(recipe.getRecipeType())) {
                caBean.setLeftX(55);
                caBean.setLeftY(370);
            //西药
            } else {
                caBean.setLeftX(320);
                caBean.setLeftY(735);
            }
        } else {
            //药师签名
            //先下载oss服务器上的签名文件
            InputStream is = null;
            BufferedInputStream bis = null;
            ByteArrayOutputStream out = null;
            byte[] byteData = null;
            FileService fileService = AppContextHolder.getBean("fileService", FileService.class);
            try {
                FileMetaRecord fileMetaRecord = fileService.getRegistry().load(recipe.getSignFile());
                if (null != fileMetaRecord) {
                    is = fileService.getRepository().readAsStream(fileMetaRecord);
                    bis = new BufferedInputStream(is);
                }
                if (null != bis) {
                    byte[] byteArray = new byte[1024];
                    int len = 0;
                    out = new ByteArrayOutputStream();
                    while ((len = bis.read(byteArray)) != -1) {
                        out.write(byteArray, 0, len);
                    }
                    byteData = out.toByteArray();
                }
            } catch (FileRegistryException e) {
                LOGGER.error("RecipeServiceEsignExt download signFile occur FileRegistryException signFileId=" + recipe.getSignFile(),e);
            } catch (FileRepositoryException e) {
                LOGGER.error("RecipeServiceEsignExt download signFile occur FileRepositoryException signFileId=" + recipe.getSignFile(),e);
            } catch (IOException e) {
                LOGGER.error("RecipeServiceEsignExt download signFile occur IOException signFileId=" + recipe.getSignFile(),e);
            } finally {
                if (null != bis) {
                    try {
                        bis.close();
                    } catch (IOException e) {
                        LOGGER.error("BeansException copyProperties error.", e);
                    }
                }
                if (null != is) {
                    try {
                        is.close();
                    } catch (IOException e) {
                        LOGGER.error("BeansException copyProperties error.", e);
                    }
                }
                if (null != out) {
                    try {
                        out.close();
                    } catch (IOException e) {
                        LOGGER.error("BeansException copyProperties error.", e);
                    }
                }
            }
            LOGGER.info("signCreateRecipePDF pdf is success");
            if (null != byteData) {
                pdf = new String(Base64.encode(byteData));
            }
            fileName = "recipecheck_" + recipeId + ".pdf";
            //中药
            if (TCM_TEMPLATETYPE.equals(recipe.getRecipeType())) {
                caBean.setLeftX(390);
                caBean.setLeftY(30);
            //西药
            } else {
                caBean.setLeftX(500);
                caBean.setLeftY(90);
            }
        }

        caBean.setSealHeight(40);
        caBean.setSealWidth(40);
        caBean.setPage(1);
        //产生本地的pdf图片
        caBean.setPdfBase64Str(pdf);
        caBean.setPdfName(fileName);
        caBean.setPdfMd5("");
        caBean.setMode(1);
        LOGGER.info("signCreateRecipePDF caBean is [{}]", JSONObject.toJSONString(caBean));
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
                                           String signRecipeCode,Boolean isDoctor, String fileId){
        LOGGER.info("saveSignRecipePDF start in pdfBase64={}, recipeId={}, loginId={},signCADate={},signRecipeCode={},isDoctor={}",
                pdfBase64, recipeId, loginId, signCADate, signRecipeCode, isDoctor);
//        String fileId = null;
        try {
            if (null != pdfBase64) {
                //组装生成pdf的参数
                String fileName = "recipe_" + recipeId + ".pdf";
                BASE64Decoder d = new BASE64Decoder();
                byte[] data = new byte[0];
                try {
                    data = d.decodeBuffer(pdfBase64);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                fileId = uploadRecipeSignFile(data, fileName, loginId);
                if (null == fileId) {
                    LOGGER.info("上传文件失败,fileName=" + fileName);
                }
            }

            Map<String, Object> attrMap = Maps.newHashMap();
            if (isDoctor) {
                //医生签名时间戳
                if (!StringUtils.isEmpty(signCADate)) {
                    attrMap.put("signCADate", signCADate);
                }
                //医生签名值
                if (!StringUtils.isEmpty(signRecipeCode)) {
                    attrMap.put("signRecipeCode", signRecipeCode);
                }
                if (!StringUtils.isEmpty(fileId)) {
                    attrMap.put("signFile", fileId);
                }
                attrMap.put("signDate", new Date());
                attrMap.put("Status", RecipeStatusConstant.CHECK_PASS);
            } else {
                //药师签名时间戳
                if (!StringUtils.isEmpty(signCADate)) {
                    attrMap.put("signPharmacistCADate", signCADate);
                }

                //药师签名值
                if (!StringUtils.isEmpty(signRecipeCode)) {
                    attrMap.put("signPharmacistCode", signRecipeCode);
                }
                if (!StringUtils.isEmpty(fileId)) {
                    attrMap.put("chemistSignFile", fileId);
                }
                attrMap.put("CheckDateYs", new Date());

                RecipeBean recipe =recipeService.get(recipeId);
                AuditModeContext auditModeContext = new AuditModeContext();
                int recipeStatus = auditModeContext.getAuditModes(recipe.getReviewType()).afterAuditRecipeChange();
                if (recipe.canMedicalPay()) {
                    //如果是可医保支付的单子，审核是在用户看到之前，所以审核通过之后变为待处理状态
                    recipeStatus = RecipeStatusConstant.CHECK_PASS;
                }
                attrMap.put("Status", recipeStatus);
            }

            //保存签名值
            boolean upResult = recipeService.updateRecipeInfoByRecipeId(recipeId, attrMap);
            LOGGER.info("saveSignRecipePDF 保存签名  upResult={}=recipeId={}=attrMap={}=", upResult,recipeId,attrMap.toString());
            String reuslt = upResult?"success":"fail";
            return reuslt;
        } catch (Exception e){
            e.printStackTrace();
            LOGGER.error("saveSignRecipePDF 保存签名 ",e);
            return null;
        }
    }

    /**
     * CA专用的处方保存CA相关信息
     * @param recipeId
     * @return
     * @throws Exception
     */
    @RpcService
    public static String saveSignRecipePDFCA(String pdfBase64,Integer recipeId, String loginId,String signCADate,
                                           String signRecipeCode,Boolean isDoctor, String fileId){
        LOGGER.info("saveSignRecipePDFCA start in pdfBase64={}, recipeId={}, loginId={},signCADate={},signRecipeCode={},isDoctor={}",
                pdfBase64, recipeId, loginId, signCADate, signRecipeCode, isDoctor);
//        String fileId = null;
        try {
            if (null != pdfBase64) {
                //组装生成pdf的参数
                String fileName = "recipe_" + recipeId + ".pdf";
                BASE64Decoder d = new BASE64Decoder();
                byte[] data = new byte[0];
                try {
                    data = d.decodeBuffer(pdfBase64);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                fileId = uploadRecipeSignFile(data, fileName, loginId);
                if (null == fileId) {
                    LOGGER.info("saveSignRecipePDFCA上传文件失败,fileName=" + fileName);
                }
            }

            Map<String, Object> attrMap = Maps.newHashMap();
            if (isDoctor) {
                //医生签名时间戳
                if (!StringUtils.isEmpty(signCADate)) {
                    attrMap.put("signCADate", signCADate);
                }
                //医生签名值
                if (!StringUtils.isEmpty(signRecipeCode)) {
                    attrMap.put("signRecipeCode", signRecipeCode);
                }
                if (!StringUtils.isEmpty(fileId)) {
                    attrMap.put("signFile", fileId);
                }
                attrMap.put("signDate", new Date());
            } else {
                //药师签名时间戳
                if (!StringUtils.isEmpty(signCADate)) {
                    attrMap.put("signPharmacistCADate", signCADate);
                }

                //药师签名值
                if (!StringUtils.isEmpty(signRecipeCode)) {
                    attrMap.put("signPharmacistCode", signRecipeCode);
                }
                if (!StringUtils.isEmpty(fileId)) {
                    attrMap.put("chemistSignFile", fileId);
                }
                attrMap.put("CheckDateYs", new Date());

            }
            //保存签名值
            boolean upResult = recipeService.updateRecipeInfoByRecipeId(recipeId, attrMap);
            LOGGER.info("saveSignRecipePDFCA 保存签名  upResult={}=recipeId={}=attrMap={}=", upResult,recipeId,attrMap.toString());
            String reuslt = upResult?"success":"fail";
            return reuslt;
        } catch (Exception e){
            e.printStackTrace();
            LOGGER.error("saveSignRecipePDFCA 保存签名 ",e);
            return null;
        }
    }

    public static String saveSignRecipePDF2(String pdfBase64,Integer recipeId, String loginId,String signCADate,
                                           String signRecipeCode,Boolean isDoctor, String fileId){
        LOGGER.info("saveSignRecipePDF start in pdfBase64={}, recipeId={}, loginId={},signCADate={},signRecipeCode={},isDoctor={}",
                pdfBase64, recipeId, loginId, signCADate, signRecipeCode, isDoctor);
//        String fileId = null;
        try {
            if (!StringUtils.isEmpty(pdfBase64)) {
                //组装生成pdf的参数
                String fileName = "recipe_" + recipeId + ".pdf";
                BASE64Decoder d = new BASE64Decoder();
                byte[] data = new byte[0];
                try {
                    data = d.decodeBuffer(pdfBase64);
                } catch (IOException e) {
                    LOGGER.error("上传文件失败",e);
                    e.printStackTrace();
                }
                fileId = uploadRecipeSignFile(data, fileName, loginId);
                if (null == fileId) {
                    LOGGER.info("上传文件失败,fileName=" + fileName);
                }
            }

            Map<String, Object> attrMap = Maps.newHashMap();
            if (isDoctor) {
                //医生签名时间戳
                if (!StringUtils.isEmpty(signCADate)) {
                    attrMap.put("signCADate", signCADate);
                }
                //医生签名值
                if (!StringUtils.isEmpty(signRecipeCode)) {
                    attrMap.put("signRecipeCode", signRecipeCode);
                }
                if (!StringUtils.isEmpty(fileId)) {
                    attrMap.put("signFile", fileId);
                }
                attrMap.put("signDate", new Date());
            } else {
                //药师签名时间戳
                if (!StringUtils.isEmpty(signCADate)) {
                    attrMap.put("signPharmacistCADate", signCADate);
                }

                //药师签名值
                if (!StringUtils.isEmpty(signRecipeCode)) {
                    attrMap.put("signPharmacistCode", signRecipeCode);
                }
                if (!StringUtils.isEmpty(fileId)) {
                    attrMap.put("chemistSignFile", fileId);
                }
                attrMap.put("CheckDateYs", new Date());
            }

            //保存签名值
            boolean upResult = recipeService.updateRecipeInfoByRecipeId(recipeId, attrMap);
            LOGGER.info("saveSignRecipePDF2 保存签名  upResult={}=recipeId={}=attrMap={}=", upResult,recipeId,attrMap.toString());
            String reuslt = upResult?"success":"fail";
            return reuslt;
        } catch (Exception e){
            LOGGER.error("saveSignRecipePDF2 保存签名",e);
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 上传电子签名文件， 处方在用
     *
     * @param data
     * @param fileName
     * @param userId
     * @return
     */
    public static String uploadRecipeSignFile(byte[] data, String fileName, String userId) {
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
            LOGGER.error("uploadRecipeSignFile exception:" + e.getMessage(),e);
        } finally {
            try {
                fileOutputStream.close();
            } catch (Exception e) {
                LOGGER.error("uploadRecipeSignFile exception:" + e.getMessage(),e);
            }
        }
        return null;
    }

    //判断
    //根据获取当前的签名文件id，更新到recipe表中
    public static void updateInitRecipePDF(Boolean isDoctor, Recipe recipe, String pdfBase64Str) {
        RecipeDAO recipeDAO = DAOFactory.getDAO(RecipeDAO.class);
        Integer recipeId = recipe.getRecipeId();
        String fileName = "recipe_" + recipeId + ".pdf";
        BASE64Decoder d = new BASE64Decoder();
        byte[] data = new byte[0];
        try {
            data = d.decodeBuffer(pdfBase64Str);
        } catch (IOException e) {
            e.printStackTrace();
        }
        String fileId = null;
        fileId = RecipeServiceEsignExt.uploadRecipeSignFile(data, fileName, null);
        if (null == fileId) {
            LOGGER.info("上传文件失败,fileName=" + fileName);
            return;
        }
        if(isDoctor){
            if(StringUtils.isEmpty(recipe.getSignFile())){
                recipeDAO.updateRecipeInfoByRecipeId(recipeId, ImmutableMap.of("signFile", fileId));
            }
        }else{
            if(StringUtils.isEmpty(recipe.getChemistSignFile())){
                recipeDAO.updateRecipeInfoByRecipeId(recipeId, ImmutableMap.of("chemistSignFile", fileId));
            }
        }
    }
}
