package recipe.api.web;

import com.ngari.recipe.vo.ConfigStatusCheckVO;

import java.util.List;


public interface IConfigStatusDoctorAtop {

    List<ConfigStatusCheckVO> getConfigStatus(Integer location);
}
