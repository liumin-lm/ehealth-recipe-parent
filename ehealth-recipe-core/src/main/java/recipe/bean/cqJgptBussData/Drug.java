package recipe.bean.cqJgptBussData;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamOmitField;
import lombok.Data;

/**
 * @program: regulation-front-cqs
 * @description: 处方药品明细
 * @author: liumin
 * @create: 2020-05-20 09:24
 **/
@Data
@XStreamAlias("drug")
public class Drug {
    @XStreamOmitField
    private String recipeSerialNum;//	处方流水号	非空
    @XStreamOmitField
    private String groupNo;//	配伍组号	非空

    private String hospitalDrugCode;//	药品编号	非空

    private String drugCommonName;//	药品通用名	非空
    @XStreamOmitField
    private String drugBrandName;//	药品商品名
    @XStreamOmitField
    private String capacity;//	装量
    @XStreamOmitField
    private String capacityUnit;//	装量单位
    @XStreamOmitField
    private String drugDose;//	单次给药剂量	非空
    @XStreamOmitField
    private String drugDoseUnit;//	单次给药剂量单位	非空
    @XStreamOmitField
    private String medicationRoute;//	给药途径	非空
    @XStreamOmitField
    private String frequency;//	给药频率	非空
    @XStreamOmitField
    private String drugUsingOpporunity;//	给药时机
    @XStreamOmitField
    private String drugUsingAim;//	给药目的
    @XStreamOmitField
    private String drugTreatmentCourse;//	疗程
    @XStreamOmitField
    private String formulation;//	剂型   非空
    @XStreamOmitField
    private String spec;//	规格

    private String price;//	单价

    private String deliverNum;//	发药数量	非空

    private String deliverNumUnit;//	数量单位	非空

    private String money;//	金额	非空
    @XStreamOmitField
    private String specialPrompt;//	特殊要求
    @XStreamOmitField
    private String skinTestFlag;//	皮试标志
    @XStreamOmitField
    private String skinTestInfo;//	皮试结果描述
    @XStreamOmitField
    private String cancelFlag;//	是否退药标志
    @XStreamOmitField
    private String productFactory;//	药品厂家

    private String drugCategory;//药品类型
}
