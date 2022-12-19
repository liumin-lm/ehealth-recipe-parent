package recipe.enumerate.type;

import org.apache.commons.lang3.StringUtils;

/**
 * 医保类型
 */
public enum MedicalTypeEnum {
    SELF_PAY(0, "自费"),
    MEDICAL_PAY(1, "医保支付");

    private Integer type;
    private String name;

    MedicalTypeEnum(Integer type, String name) {
        this.type = type;
        this.name = name;
    }

    public Integer getType() {
        return type;
    }


    public static String getOldMedicalTypeText(String patientType) {
        if (StringUtils.isNotEmpty(patientType)) {
            return null;
        }
        if (patientType.equals("2")) {
            return "普通医保";
        }
        if (patientType.equals("3")) {
            return "慢病医保";
        }
        return null;
    }
}
