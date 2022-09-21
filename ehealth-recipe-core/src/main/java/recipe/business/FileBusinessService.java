package recipe.business;

import com.ngari.recipe.entity.ImportDrugRecord;
import com.ngari.recipe.entity.ImportDrugRecordMsg;
import com.ngari.recipe.entity.SaleDrugList;
import ctd.persistence.bean.QueryContext;
import ctd.util.JSONUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import recipe.core.api.IFileBusinessService;
import recipe.dao.*;
import recipe.util.ObjectCopyUtils;
import recipe.vo.greenroom.ImportDrugRecordMsgVO;
import recipe.vo.greenroom.ImportDrugRecordVO;

import java.util.ArrayList;
import java.util.List;

/*import recipe.manager.SaleDrugListManager;*/

/**
 * @description： 药企药品
 * @author： 刘敏
 * @date： 2022-05-23 9:45
 */
@Service
public class FileBusinessService extends BaseService implements IFileBusinessService {

    @Autowired
    private ImportDrugRecordDAO importDrugRecordDAO;

    @Autowired
    private ImportDrugRecordMsgDAO importDrugRecordMsgDAO;

    @Override
    public List<ImportDrugRecordVO> findImportDrugRecordByOrganId(Integer organId) {
        List<ImportDrugRecordVO> res=new ArrayList<ImportDrugRecordVO>();
        List<ImportDrugRecord> importDrugRecords=importDrugRecordDAO.findImportDrugRecordByOrganId(organId);
        res=ObjectCopyUtils.convert(importDrugRecords,ImportDrugRecordVO.class);
        res.forEach(importDrugRecordVO -> {
            List<ImportDrugRecordMsg> importDrugRecordMsgs=importDrugRecordMsgDAO.findImportDrugRecordMsgByImportdrugRecordId(importDrugRecordVO.getRecordId());
            importDrugRecordVO.setImportDrugRecordMsg(ObjectCopyUtils.convert(importDrugRecordMsgs, ImportDrugRecordMsgVO.class));
        });
        return res;
    }

    @Override
    public List<ImportDrugRecord> findImportDrugRecord(ImportDrugRecord importDrugRecord) {
        return importDrugRecordDAO.findImportDrugRecordByOrganId(importDrugRecord.getOrganId());
    }

    @Override
    public List<ImportDrugRecordMsg> findImportDrugRecordMsgByImportDrugRecordId(ImportDrugRecord importDrugRecord) {
        List<ImportDrugRecordMsg> importDrugRecordMsgs =new ArrayList<>();
        ImportDrugRecord importDrugRecordDb=importDrugRecordDAO.get(importDrugRecord.getRecordId());
        importDrugRecordMsgs=importDrugRecordMsgDAO.findImportDrugRecordMsgByImportdrugRecordId(importDrugRecord.getRecordId());
        if(importDrugRecordDb!=null&& StringUtils.isNotEmpty(importDrugRecordDb.getErrMsg())){
            ImportDrugRecordMsg importDrugRecordMsg=new ImportDrugRecordMsg();
            importDrugRecordMsg.setErrMsg(importDrugRecordDb.getErrMsg());
            importDrugRecordMsg.setErrLocaction("见错误详情");
            importDrugRecordMsgs.add(importDrugRecordMsg);
        }
        return importDrugRecordMsgs;
    }
}
