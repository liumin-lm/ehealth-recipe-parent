package recipe.util;

import com.ngari.recipe.dto.RecipeInvalidDTO;
import com.ngari.recipe.entity.Recipe;
import com.ngari.recipe.entity.RecipeOrder;
import com.ngari.recipe.entity.Recipedetail;
import ctd.mvc.upload.FileMetaRecord;
import ctd.mvc.upload.FileService;
import ctd.util.JSONUtils;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import recipe.constant.RecipeBussConstant;
import recipe.enumerate.status.RecipeOrderStatusEnum;
import recipe.enumerate.status.RecipeStatusEnum;
import recipe.enumerate.type.DrugBelongTypeEnum;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Calendar;
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
        if (DrugBelongTypeEnum.SECRECY_DRUG.getType().equals(detail.getType())) {
            return detail.getDrugName();
        }
        if (StringUtils.isNotEmpty(detail.getUseDoseStr())) {
            dTotal = detail.getUseDoseStr();
        } else {
            dTotal = detail.getUseDose() + detail.getUseDoseUnit();
        }
        String memo = "";
        if (!StringUtils.isEmpty(detail.getMemo())) {
            memo = "(" + detail.getMemo().trim() + ")";
        }
        return detail.getDrugName().replace("（", "(").replace("）", ")") + memo + " " + dTotal;
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

    /**
     * 状态文字提示（患者端）
     *
     * @param recipe 处方
     * @param order  订单
     * @return
     */
    public static String getTipsByStatusForPatient(Recipe recipe, RecipeOrder order) {
        Integer status = recipe.getStatus();
        Integer payMode = order.getPayMode();
        Integer payFlag = recipe.getPayFlag();
        Integer giveMode = recipe.getGiveMode();
        Integer orderStatus = order.getStatus();
        String tips = "";
        switch (RecipeStatusEnum.getRecipeStatusEnum(status)) {
            case RECIPE_STATUS_HIS_FAIL:
                tips = "已取消";
                break;
            case RECIPE_STATUS_FINISH:
                tips = "已完成";
                break;
            case RECIPE_STATUS_IN_SEND:
                tips = "配送中";
                break;
            case RECIPE_STATUS_CHECK_PASS:
                if (null == payMode || null == giveMode) {
                    tips = "待处理";
                } else if (RecipeBussConstant.GIVEMODE_TO_HOS.equals(giveMode)) {
                    if (new Integer(1).equals(recipe.getRecipePayType()) && payFlag == 1) {
                        tips = "已支付";
                    } else if (payFlag == 0) {
                        tips = "待支付";
                    } else {
                        tips = "待取药";
                    }
                } else if (RecipeBussConstant.GIVEMODE_SEND_TO_HOME.equals(giveMode)) {
                    if (StringUtils.isNotEmpty(recipe.getOrderCode())) {
                        if (payFlag == 0) {
                            tips = "待支付";
                        } else {
                            if (RecipeOrderStatusEnum.ORDER_STATUS_AWAIT_SHIPPING.getType().equals(orderStatus)) {
                                tips = "待配送";
                            } else if (RecipeOrderStatusEnum.ORDER_STATUS_PROCEED_SHIPPING.getType().equals(orderStatus)) {
                                tips = "配送中";
                            } else if (RecipeOrderStatusEnum.ORDER_STATUS_DONE.getType().equals(orderStatus)) {
                                tips = "已完成";
                            }
                        }
                    }

                } else if (RecipeBussConstant.GIVEMODE_TFDS.equals(giveMode) && StringUtils.isNotEmpty(recipe.getOrderCode())) {
                    if (payFlag == 0) {
                        tips = "待支付";
                    } else {
                        tips = "待取药";
                    }
                } else if (RecipeBussConstant.GIVEMODE_DOWNLOAD_RECIPE.equals(giveMode)) {
                    tips = "已完成";
                } else if (RecipeOrderStatusEnum.ORDER_STATUS_DONE_DISPENSING.getType().equals(orderStatus)) {
                    tips = RecipeOrderStatusEnum.ORDER_STATUS_DONE_DISPENSING.getName();
                }
                break;
            case RECIPE_STATUS_REVOKE:
                if (RecipeOrderStatusEnum.ORDER_STATUS_DRUG_WITHDRAWAL.getType().equals(orderStatus)) {
                    tips = RecipeOrderStatusEnum.ORDER_STATUS_DRUG_WITHDRAWAL.getName();
                } else if (RecipeOrderStatusEnum.ORDER_STATUS_DECLINE.getType().equals(orderStatus)) {
                    tips = RecipeOrderStatusEnum.ORDER_STATUS_DECLINE.getName();
                } else {
                    tips = "已取消";
                }
                break;
            case RECIPE_STATUS_DONE_DISPENSING:
                tips = RecipeStatusEnum.RECIPE_STATUS_DONE_DISPENSING.getName();
                break;
            case RECIPE_STATUS_DECLINE:
                tips = RecipeStatusEnum.RECIPE_STATUS_DECLINE.getName();
                break;
            case RECIPE_STATUS_DRUG_WITHDRAWAL:
                tips = RecipeStatusEnum.RECIPE_STATUS_DRUG_WITHDRAWAL.getName();
                break;
            case REVIEW_DRUG_FAIL:
                tips = RecipeStatusEnum.REVIEW_DRUG_FAIL.getName();
                break;
            default:
                tips = "待取药";
        }
        return tips;
    }


    public static RecipeInvalidDTO getRecipeInvalidInfo(Date signDate, String recipeInvalidTime) {
        RecipeInvalidDTO invalidDTO = new RecipeInvalidDTO();
        try {
            if (StringUtils.isNotEmpty(recipeInvalidTime)) {
                // 配置格式：签名当天后某天24点前=d2-天数;签名后大于24小时=d1-小时数;签名后小于一天=h-小时数
                // 签名后小于一天用延迟队列取消处方，其余由定时任务取消
                String[] invalidArr = recipeInvalidTime.split("-");
                invalidDTO.setInvalidType(invalidArr[0]);
                Double invalidValue = Double.parseDouble(invalidArr[1]);
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(signDate);
                Date invalidDate = null;
                switch (invalidArr[0]) {
                    case "d1":
                        // 签名时间往后推invalidValue小时
                        calendar.add(Calendar.HOUR, invalidValue.intValue());
                        invalidDate = calendar.getTime();
                        break;
                    case "d2":
                        // 签名时间往后推invalidValue天的最大时间
                        calendar.add(Calendar.DATE, invalidValue.intValue());
                        Date afterDate = calendar.getTime();
                        LocalDateTime localDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(afterDate.getTime()), ZoneId.systemDefault());

                        LocalDateTime endOfDay = localDateTime.with(LocalTime.MAX);
                        invalidDate = Date.from(endOfDay.atZone(ZoneId.systemDefault()).toInstant());
                        break;
                    case "h":
                        // 签名时间往后推invalidValue*60 分钟
                        int minute = (int) (invalidValue * 60);
                        calendar.add(Calendar.MINUTE, minute);
                        invalidDate = calendar.getTime();
                        break;
                    default:
                        break;
                }
                invalidDTO.setInvalidDate(invalidDate);
            }
        } catch (Exception e) {
            logger.error("机构处方失效时间-计算失效失效异常 recipeInvalidTime={}", recipeInvalidTime, e);
        }
        return invalidDTO;
    }

}
