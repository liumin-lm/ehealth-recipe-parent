package recipe.util;

/**
* @Description: DistanceUtil 类（或接口）是距离计算的工具类
* @Author: JRK
* @Date: 2019/7/11
*/
public class DistanceUtil {

    private static final double EARTH_RADIUS = 6378.138;//地球半径,单位千米

    private static double rad(double d){
        return d * Math.PI / 180.0;
    }

    /**
     * @method  getDistance
     * @description 获取两个经纬度之间的距离
     * @date: 2019/7/11
     * @author: JRK
     * @param lat1 A纬度
     * @param lng1  A经度
     * @param lat2 B纬度
     * @param lng2 B经度
     * @return double 距离
     */
    public static double getDistance(double lat1,double lng1,double lat2,double lng2){

        double radLat1 = rad(lat1);
        double radLat2 = rad(lat2);
        double a = radLat1 - radLat2;
        double b = rad(lng1) - rad(lng2);
        double s = 2 * Math.asin(Math.sqrt(Math.pow(Math.sin(a/2),2) +
                Math.cos(radLat1)*Math.cos(radLat2)*Math.pow(Math.sin(b/2),2)));
        s = s * EARTH_RADIUS;
        return s;

    }
}