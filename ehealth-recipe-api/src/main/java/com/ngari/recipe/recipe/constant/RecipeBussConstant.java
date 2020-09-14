package com.ngari.recipe.recipe.constant;

/**
 * company: ngarihealth
 * @author: 0184/yu_yun
 * date:2017/3/13.
 */
public class RecipeBussConstant {


    /**
     * 配送到家
     */
    public static Integer GIVEMODE_SEND_TO_HOME = 1;

    /**
     * 医院取药
     */
    public static Integer GIVEMODE_TO_HOS = 2;

    /**
     * 药店取药
     */
    public static Integer GIVEMODE_TFDS = 3;

    /**
     * 患者自由选择
     */
    public static Integer GIVEMODE_FREEDOM = 4;

    /**
     * 支持多种购药方式
     */
    public static Integer PAYMODE_COMPLEX = 0;

    /**
     * 线上支付
     */
    public static Integer PAYMODE_ONLINE = 1;

    /**
     * 货到付款
     */
    public static Integer PAYMODE_COD = 2;

    /**
     * 到院支付
     */
    public static Integer PAYMODE_TO_HOS = 3;

    /**
     * 药店取药(take from drug store)
     */
    public static Integer PAYMODE_TFDS = 4;

    /**
     * 医保支付
     */
    public static Integer PAYMODE_MEDICAL_INSURANCE = 5;

    /**
     * 药企配送模式-都不支持
     */
    public static Integer DEP_SUPPORT_UNKNOW = 0;

    /**
     * 药企配送模式-线上支付后配送
     */
    public static Integer DEP_SUPPORT_ONLINE = 1;

    /**
     * 药企配送模式-货到付款
     */
    public static Integer DEP_SUPPORT_COD = 2;

    /**
     * 药企配送模式-药店取药
     */
    public static Integer DEP_SUPPORT_TFDS = 3;

    /**
     * 药企配送模式-配送到家+药店取药
     */
    public static Integer DEP_SUPPORT_ONLINE_TFDS = 7;

    /**
     * 药企配送模式-货到付款+药店取药
     */
    public static Integer DEP_SUPPORT_COD_TFDS = 8;

    /**
     * 药企配送模式-都支持
     */
    public static Integer DEP_SUPPORT_ALL = 9;

    /**
     * 处方类型-西药  Western medicine
     */
    public static Integer RECIPETYPE_WM = 1;

    /**
     * 处方类型-中成药  chinese patent medicine.
     */
    public static Integer RECIPETYPE_CPM = 2;

    /**
     * 处方类型-中药  traditional Chinese medicine
     */
    public static Integer RECIPETYPE_TCM = 3;

    /**
     * 处方类型-膏方 Herbal Paste
     */
    public static Integer RECIPETYPE_HP = 4;

    /**
     * HIS同步过来的处方
     */
    public static Integer FROMFLAG_HIS = 0;

    /**
     * 平台开具的处方
     */
    public static Integer FROMFLAG_PLATFORM = 1;

    /**
     * HIS同步过来的处方-医生处理及可进行审方
     */
    public static Integer FROMFLAG_HIS_USE = 2;


    /**
     * 流转模式-平台模式
     */
    public static String RECIPEMODE_NGARIHEALTH = "ngarihealth";

    /**
     * 流转模式-浙江省互联网医院平台
     */
    public static String RECIPEMODE_ZJJGPT = "zjjgpt";

    /**
     * 下载处方支付场景
     */
    public static Integer PAYMODE_DOWNLOAD_RECIPE = 6;

    /**
     * 下载处方购药方式
     */
    public static Integer GIVEMODE_DOWNLOAD_RECIPE = 5;

    /**reqType 请求类型（1：二维码扫码推送详情 2：自动推送详情链接跳转请求 ）*/
    public static Integer REQ_TYPE_QRCODE = 1;
    public static Integer REQ_TYPE_AUTO = 2;

    /**
     * 处方业务来源-无-医生直接开处方
     */
    public static Integer BUSS_SOURCE_NONE = 0;
    /**
     * 处方业务来源-问诊
     */
    public static Integer BUSS_SOURCE_WZ = 1;
    /**
     * 处方业务来源-在线复诊
     */
    public static Integer BUSS_SOURCE_FZ = 2;
    /**
     * 处方业务来源-网络咨询
     */
    public static Integer BUSS_SOURCE_WLZX = 3;

}
