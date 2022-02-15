package recipe.vo.greenroom;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * @description： 机构药企销售配置
 * @author： whf
 * @date： 2022-01-10 15:38
 */
@Getter
@Setter
public class OrganDrugsSaleConfigVo implements Serializable {
    /**
     * 自增主键
     */
    private Integer id;

    /**
     * 机构id
     */
    private Integer organId;

    /**
     * 药企id
     */
    private Integer drugsEnterpriseId;

    /**
     * 是否支持配送到站 0 不支持 1支持
     */
    private Integer isSupportSendToStation;

    /**
     * 自取支付方式 1 在线支付 2 线下支付
     */
    private Integer takeOneselfPayment;

    /**
     * 自取支付通道 1平台支付 2卫宁支付
     */
    private Integer takeOneselfPaymentChannel;

    /**
     * 自取文案提示
     */
    private String takeOneselfContent;

    /**
     * 自取预约时间 0 不预约 1预约当天 3 预约3天内 7预约7天内 15预约15天内
     */
    private Integer takeOneselfPlanDate;

    /**
     * 取药预约取药时间段配置：上午时间段
     */
    private String takeOneselfPlanAmTime;

    /**
     * 取药预约取药时间段配置：下午时间段
     */
    private String takeOneselfPlanPmTime;

    /**
     * 取药凭证 0不展示 1就诊卡号 2挂号序号 3病历号 4his单号 5发药流水号 6取药流水号
     */
    private Integer takeDrugsVoucher;

    /**
     * 医保患者特殊提示
     */
    private String specialTips;

}
