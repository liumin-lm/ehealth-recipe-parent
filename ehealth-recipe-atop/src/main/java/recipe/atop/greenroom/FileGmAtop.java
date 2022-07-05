package recipe.atop.greenroom;

import com.ngari.recipe.entity.ImportDrugRecord;
import com.ngari.recipe.entity.ImportDrugRecordMsg;
import com.ngari.upload.service.IFileUploadService;
import ctd.account.UserRoleToken;
import ctd.mvc.controller.OutputSupportMVCController;
import ctd.mvc.controller.util.UserRoleTokenUtils;
import ctd.security.exception.SecurityException;
import ctd.util.annotation.RpcBean;
import ctd.util.annotation.RpcService;
import ctd.util.context.ContextUtils;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import recipe.atop.BaseAtop;
import recipe.core.api.IFileBusinessService;
import recipe.core.api.ISaleDrugBusinessService;
import recipe.vo.greenroom.ImportDrugRecordVO;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @description： 文件
 * @author： 刘敏
 * @date： 2022-05-23 9:45
 */
@RpcBean(value = "fileGmAtop")
public class FileGmAtop extends OutputSupportMVCController {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    private ServletFileUpload uploader;

    @Autowired
    private IFileUploadService fileUploadService;

    @Autowired
    private IFileBusinessService fileBusinessService;

    /**
     * 查找
     *
     * @param organId
     * @return
     */
    @RpcService
    public List<ImportDrugRecordVO> findImportDrugRecordByOrganId(Integer organId) {
        return fileBusinessService.findImportDrugRecordByOrganId(organId);
    }

    /**
     * 导入结果查看
     * @param importDrugRecord
     * @return
     */
    @RpcService
    public List<ImportDrugRecord> findImportDrugRecord(ImportDrugRecord importDrugRecord) {
        return fileBusinessService.findImportDrugRecord(importDrugRecord);
    }

    /**
     * 错误原因查看
     * @param importDrugRecord
     * @return
     */
    @RpcService
    public List<ImportDrugRecordMsg> findImportDrugRecordMsgByImportDrugRecordId(ImportDrugRecord importDrugRecord) {
        return fileBusinessService.findImportDrugRecordMsgByImportDrugRecordId(importDrugRecord);
    }



//    /**
//     * 文件上传至oss服务器
//     * @param request
//     * @param response
//     * @return
//     */
//    @RequestMapping(value = "api/AsyncImportOrganDrug", method = RequestMethod.POST)
//    @ResponseBody
//    public Map<String, Object> asyncImportOrganDrug(HttpServletRequest request, HttpServletResponse response) throws Exception {
//        logger.info("机构药品导入进入");
//        request.setCharacterEncoding("UTF-8");
//        UserRoleToken e = null;
//        e = UserRoleTokenUtils.getUserRoleToken(request);
//        ContextUtils.put("$ur", e);
//        response.setContentType("application/json;charset=UTF-8");
//        response.setHeader("X-Coded-JSON-Message","true");
//        List items = this.uploader.parseRequest(request);
//        logger.info("机构药品导入进入—-----1");
//        Iterator gzip = items.iterator();
//        FileItem itemTemp = null;
//        byte[] bytes = new byte[0];
//        Integer organId = null;
//        String operator = "";
//        String originalFilename = "";
//        while (gzip.hasNext()) {
//            itemTemp = (FileItem) gzip.next();
//            if (("file").equals(itemTemp.getFieldName())){
//                bytes = itemTemp.get();
//                originalFilename = itemTemp.getName();
//                continue;
//            }
//            if("organId".equals(itemTemp.getFieldName())){
//                organId = Integer.valueOf(itemTemp.getString("UTF-8"));
//            }
//            if("operator".equals(itemTemp.getFieldName())){
//                operator = itemTemp.getString("UTF-8");
//            }
//        }
//        logger.info("api/OrganDrug"+originalFilename+" : "+bytes.length);
//        String fileId = fileUploadService.uploadFileWithoutUrt(bytes, originalFilename);
//        logger.info("fileId:{}",fileId);
//
//        return null;
//    }
//
//
//    /**
//     * 文件上传至oss服务器
//     * @param request
//     * @param response
//     * @return
//     */
//    @RpcService
//    public Map<String, Object> asyncImportOrganDrug2(HttpServletRequest request, HttpServletResponse response) throws Exception {
//        logger.info("机构药品导入进入");
//        request.setCharacterEncoding("UTF-8");
//        UserRoleToken e = null;
//        e = UserRoleTokenUtils.getUserRoleToken(request);
//        ContextUtils.put("$ur", e);
//        response.setContentType("application/json;charset=UTF-8");
//        response.setHeader("X-Coded-JSON-Message","true");
//        List items = this.uploader.parseRequest(request);
//        logger.info("机构药品导入进入—-----1");
//        Iterator gzip = items.iterator();
//        FileItem itemTemp = null;
//        byte[] bytes = new byte[0];
//        Integer organId = null;
//        String operator = "";
//        String originalFilename = "";
//        while (gzip.hasNext()) {
//            itemTemp = (FileItem) gzip.next();
//            if (("file").equals(itemTemp.getFieldName())){
//                bytes = itemTemp.get();
//                originalFilename = itemTemp.getName();
//                continue;
//            }
//            if("organId".equals(itemTemp.getFieldName())){
//                organId = Integer.valueOf(itemTemp.getString("UTF-8"));
//            }
//            if("operator".equals(itemTemp.getFieldName())){
//                operator = itemTemp.getString("UTF-8");
//            }
//        }
//        logger.info("api/OrganDrug"+originalFilename+" : "+bytes.length);
//        String fileId = fileUploadService.uploadFileWithoutUrt(bytes, originalFilename);
//        logger.info("fileId:{}",fileId);
//
//        return null;
//    }

}
