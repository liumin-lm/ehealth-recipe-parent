package recipe.core.api;

import com.ngari.recipe.entity.ImportDrugRecord;
import com.ngari.recipe.entity.ImportDrugRecordMsg;
import com.ngari.recipe.entity.SaleDrugList;
import recipe.vo.greenroom.ImportDrugRecordVO;

import java.util.List;


public interface IFileBusinessService {

    /**
     * 文件导入记录
     * @param organId
     * @return
     */
    List<ImportDrugRecordVO> findImportDrugRecordByOrganId(Integer organId);

    /**
     * 获取文件导入记录
     * @param importDrugRecord
     * @return
     */
    List<ImportDrugRecord> findImportDrugRecord(ImportDrugRecord importDrugRecord);

    /**
     * 获取文件导入错误详情
     * @param importDrugRecord
     * @return
     */
    List<ImportDrugRecordMsg> findImportDrugRecordMsgByImportDrugRecordId(ImportDrugRecord importDrugRecord);

}
