package recipe.drugsenterprise.bean;

import java.io.Serializable;

/**
* @Description: HdPosition 类（或接口）是 对接华东药店信息的坐标信息对象
* @Author: JRK
* @Date: 2019/7/25
*/
public class YnsPosition implements Serializable {
    private static final long serialVersionUID = -5552067197085371019L;

    //经度
    private String longitude;

    //纬度
    private String latitude;

    public YnsPosition() {
    }

    public YnsPosition(String longitude, String latitude) {
        this.longitude = longitude;
        this.latitude = latitude;
    }

    public String getLongitude() {
        return longitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

    public String getLatitude() {
        return latitude;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public static boolean checkParameter (YnsPosition position) {
        if (null != position && null != position.getLatitude() && null != position.getLongitude()) {
            return true;
        }
        return false;
    }
}