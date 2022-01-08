package recipe.vo.patient;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * @description： 患者取药站点
 * @author： yins
 * @date： 2022-01-07 14:00
 */
@Getter
@Setter
public class MedicineStationVO implements Serializable {
    private static final long serialVersionUID = 973193096658297055L;

    /**
     * 处方ID
     */
    private Integer recipeId;
    /**
     * 机构ID
     */
    private Integer organId;
    /**
     * 药企ID
     */
    private Integer enterpriseId;
    /**
     * 站点名称
     */
    private String stationName;
    /**
     * 所在省编码
     */
    private String provinceCode;
    /**
     * 所在省名称
     */
    private String provinceName;
    /**
     * 所在市编码
     */
    private String cityCode;
    /**
     * 所在市名称
     */
    private String cityName;
    /**
     * 所在区编码
     */
    private String areaCode;
    /**
     * 所在区名称
     */
    private String areaName;
    /**
     * 详细地址
     */
    private String address;
    /**
     * 纬度
     */
    private String lat;
    /**
     * 经度
     */
    private String lng;
    /**
     * 距离
     */
    private double distance;

    private Integer start;
    private Integer limit;
}
