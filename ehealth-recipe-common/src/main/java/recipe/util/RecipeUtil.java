package recipe.util;

import com.ngari.recipe.entity.Recipedetail;
import ctd.mvc.upload.FileMetaRecord;
import ctd.mvc.upload.FileService;
import ctd.util.JSONUtils;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.constant.RecipeBussConstant;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Date;

/**
 * 电子处方工具类
 *
 * @author jiangtingfeng
 * @date 2017/6/14.
 */
public class RecipeUtil {
    private static final Logger logger = LoggerFactory.getLogger(RecipeUtil.class);

    /**
     * 判断是否中药类处方
     *
     * @return
     */
    public static boolean isTcmType(int recipeType) {
        if (RecipeBussConstant.RECIPETYPE_TCM.equals(recipeType) || RecipeBussConstant.RECIPETYPE_HP.equals(recipeType)) {
            return true;
        }
        return false;
    }

    /**
     * 获取中药展示名称
     *
     * @param detail 处方明细
     * @return
     */
    public static String drugChineShowName(Recipedetail detail) {
        String dTotal;
        if (StringUtils.isNotEmpty(detail.getUseDoseStr())) {
            dTotal = detail.getUseDoseStr();
        } else {
            dTotal = detail.getUseDose() + detail.getUseDoseUnit();
        }
        String memo = "";
        if (!StringUtils.isEmpty(detail.getMemo()) && !"无特殊煎法".equals(detail.getMemo())) {
            memo = "(" + detail.getMemo() + ")";
        }
        return detail.getDrugName() + memo + " " + dTotal;
    }

    /**
     * 伤处图片文件
     *
     * @param picture
     * @return
     */
    public static String uploadPicture(String picture) {
        byte[] data = Base64.decodeBase64(picture.getBytes());
        if (data == null) {
            return null;
        }
        OutputStream fileOutputStream = null;
        try {
            //先生成本地文件
            String prefix = picture.substring(0, 4);
            String fileName = "caPicture_" + prefix + ".jpeg";
            File file = new File(fileName);
            fileOutputStream = new FileOutputStream(file);
            if (data.length > 0) {
                fileOutputStream.write(data, 0, data.length);
                fileOutputStream.flush();
            }
            FileMetaRecord meta = new FileMetaRecord();
            meta.setManageUnit("eh");
            meta.setLastModify(new Date());
            meta.setUploadTime(new Date());
            //0需要验证 31不需要
            meta.setMode(0);
            meta.setCatalog("other-doc");
            meta.setContentType("image/jpeg");
            meta.setFileName(fileName);
            meta.setFileSize(file.length());
            logger.info("uploadPicture.meta=[{}]", JSONUtils.toString(meta));
            FileService.instance().upload(meta, file);
            file.delete();
            return meta.getFileId();
        } catch (Exception e) {
            logger.error("RecipeUtil uploadRecipeFile exception:" + e.getMessage());
        } finally {
            try {
                fileOutputStream.close();
            } catch (Exception e) {
                logger.error("RecipeUtil uploadRecipeFile exception:" + e.getMessage());
            }
        }
        return null;
    }


}
