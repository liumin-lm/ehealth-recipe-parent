package recipe.vo.second;

import com.ngari.recipe.recipe.model.RecipeDetailBean;
import ctd.schema.annotation.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author liumin
 */
@Schema
@Data
@NoArgsConstructor
public class RevisitRecipeTraceVo implements Serializable {

    private static final long serialVersionUID = -8882418262625511818L;


    /**
     * 处方
     */
    private Recipe recipe;

    private List<AuditMedicines> auditMedicinesList;

    /**
     * 药品详情
     */
    private List<RecipeDetailBean> detailData;

    /**
     * 审方
     */
    private AuditCheck auditCheck;

    /**
     * 获取审核不通过详情
     */
    private List<Map<String, Object>> reasonAndDetails;

    /**
     * 发药药师
     */
    private GiveUser giveUser;

    /**
     * 患者够药
     */
    private Order order;

    /**
     * 物流
     */
    private Logistics logistics;

    /**
     * 退费
     */
    private RecipeRefund recipeRefund;

    /**
     * 处方取消
     */
    private RecipeCancel recipeCancel;

    //医生开方
    @Data
    @Schema
    public static class Recipe implements Serializable {

        @ItemProperty(alias = "处方状态")
        @Dictionary(id = "eh.cdr.dictionary.RecipeStatus")
        private Integer status;

        @ItemProperty(alias = "处方签")
        private String recipeSignFile;

        @ItemProperty(alias = "签名的处方PDF")
        private String signFile;

        @ItemProperty(alias = "药师签名的处方PDF")
        private String chemistSignFile;

        @ItemProperty(alias = "开方时间")
        private Date createDate;

        @ItemProperty(alias = "开方医生")
        private String doctorName;

        @ItemProperty(alias = "临床诊断")
        private String organDiseaseName;

        private String recipeId;

        @ItemProperty(alias = "来源标志")
        @Dictionary(id = "eh.cdr.dictionary.FromFlag")
        private Integer fromflag;


        @ItemProperty(alias = "医生签名")
        private String doctorSign;

        //TODO
        @ItemProperty(alias = "医生签名时间")
        private String signCADate;

        //TODO
        //@ItemProperty(alias = "医生签名摘要")


        @ItemProperty(alias = "药师处方数字签名可信服务器时间")
        private String signPharmacistCADate;

        /**
         * 患者医保类型（编码）
         */
        private String medicalType;

        /**
         * 患者医保类型（名称）
         */
        private String medicalTypeText;

        @ItemProperty(alias = "处方金额")
        private BigDecimal totalMoney;

    }


    //Audit
    @Data
    @Schema
    public static class AuditCheck implements Serializable {


        @ItemProperty(alias = "审核日期")
        private Date checkDate;

        //        @Dictionary(id = "eh.cdr.dictionary.RecipeCheckStatus")
        @ItemProperty(alias = "审核结果")
        private Integer checkStatus;

        private String checkStatusText;

        @ItemProperty(alias = "审核人姓名")
        private String checkerName;

        @ItemProperty(alias = "审核人身份证号")
        private String checkIdCard;

        @ItemProperty(alias = "药师签名")
        private String checkSign;

        @ItemProperty(alias = "审核备注信息")
        private String memo;

        public String getCheckStatusText() {
            if (0 == checkStatus) {
                this.checkStatusText = "审核不通过";
            } else if (1 == checkStatus) {
                this.checkStatusText = "审核通过";
            }
            return this.checkStatusText;
        }

        public void setCheckStatusText(String checkStatusText) {
            this.checkStatusText = checkStatusText;
        }
    }

    @Data
    @Schema
    public static class GiveUser {

        /**
         * 发药药师姓名
         */
        private String giveUserName;

        /**
         * 发药药师身份证
         */
        @Desensitizations(type = DesensitizationsType.ADDRESS)
        private String giveUserIdCard;

        /**
         * 核发药师签名图片
         */
        private String giveUserSignImg;

        /**
         * 已发药时间
         */
        private Date dispensingTime;

    }

    @Data
    @Schema
    public static class Order {

        @ItemProperty(alias = "电子票据h5地址")
        private String billPictureUrl;

//        @ItemProperty(alias = "购药方式")
//        @Dictionary(id = "eh.cdr.dictionary.GiveMode")
//        private Integer giveMode;

        @ItemProperty(alias = "订单所属配送方式")
        private String giveModeKey;

        @ItemProperty(alias = "订单所属配送方式的文案")
        private String giveModeText;

        @ItemProperty(alias = "收货人姓名")
        private String receiver;

        @Desensitizations(type = DesensitizationsType.MOBILE)
        @ItemProperty(alias = "收货人手机号")
        private String recMobile;

        @Desensitizations(type = DesensitizationsType.ADDRESS)
        @ItemProperty(alias = "详细地址")
        private String address;

        @ItemProperty(alias = "支付时间")
        private Date payTime;

        @ItemProperty(alias = "订单退款标识 0未退费 1已退费")
        private Integer refundFlag;

        @ItemProperty(alias = "支付状态 0未支付，1已支付，2退款中，3退款成功，4支付失败")
        @Dictionary(id = "eh.bus.dictionary.PayFlag")
        private Integer payFlag;

        @ItemProperty(alias = "支付方式")
        @Dictionary(id = "eh.bus.dictionary.PayWay")
        private String wxPayWay;

        @ItemProperty(alias = "交易流水号")
        private String tradeNo;

        @ItemProperty(alias = "商户订单号")
        private String outTradeNo;

        @ItemProperty(alias = "处方预结算返回支付总金额")
        private String preSettleTotalAmount;

        @ItemProperty(alias = "处方预结算返回医保支付金额")
        private String fundAmount;

        @ItemProperty(alias = "处方预结算返回自费金额")
        private String cashAmount;

        @ItemProperty(alias = "配送主体类型 1医院配送 2药企配送")
        private Integer sendType;


    }

    /**
     * 物流 查看物流进度 前端直接调基础服务的接口
     */
    @Data
    @Schema
    public static class Logistics {

        @ItemProperty(alias = "物流公司")
        @Dictionary(id = "eh.cdr.dictionary.LogisticsCompany")
        private Integer logisticsCompany;

        @ItemProperty(alias = "快递单号")
        private String trackingNumber;

        @ItemProperty(alias = "发货时间")
        private Date sendTime;

        @ItemProperty(alias = "订单编号")
        private String orderCode;
    }

    /**
     * 退费
     */
    @Data
    @Schema
    public static class RecipeRefund {

        @ItemProperty(alias = "申请时间")
        private Date applyTime;

        @ItemProperty(alias = "订单金额")
        private Double price;

        @ItemProperty(alias = "流水号")
        private String tradeNo;

        @ItemProperty(alias = "处方退费当前节点状态")
        @Dictionary(id = "eh.cdr.dictionary.RecipeRefundNodeStatus")
        private Integer refundNodeStatus;
    }

    /**
     * @Author liumin
     * @Date 2021/8/20 下午3:19
     * @Description 医生撤销
     */
    @Data
    @Schema
    public static class RecipeCancel {
        @ItemProperty(alias = "原因")
        private String cancelReason;

        @ItemProperty(alias = "时间")
        private Date cancelDate;
    }

    @Data
    @Schema
    public static class AuditMedicines {
        @ItemProperty(
                alias = "药品名"
        )
        private String name;

        @ItemProperty(
                alias = "创建时间"
        )
        private Date createTime;
    }

    public String getRecipeSignFile() {
        if (StringUtils.isNotEmpty(this.recipe.chemistSignFile)) {
            return this.recipe.chemistSignFile;
        } else {
            return this.recipe.signFile;
        }
    }


}
