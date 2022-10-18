package recipe.drugsenterprise.bean;

import ctd.schema.annotation.Schema;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * @author yinsheng
 * @date 2019\3\14 0014 15:46
 */
@Schema
@Getter
@Setter
public class JztDrugDTO implements Serializable{
    private static final long serialVersionUID = 1268999311294084487L;

    private String drugCode;

    private String drugName;

    private String specification;

    private String producer;

    private String total;

    private String useDose;

    private String useDoseUnit;

    private String drugFee;

    private String drugTotalFee;

    private int uesDays;

    private String usingRate;

    private String usePathways;

    private String memo;

    private String drugForm;


}
