package recipe.vo.greenroom;

import com.ngari.patient.dto.OrganDTO;
import ctd.schema.annotation.ItemProperty;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * @description： 机构配置vo
 * @author： lium
 * @date： 2022-05-09 11:10
 */
@Data
public class OrganConfigVO extends OrganDTO implements Serializable {

    private List<OrganDrugListSyncFieldVo> organDrugListSyncFieldList;

}
