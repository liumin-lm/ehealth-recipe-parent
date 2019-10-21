package recipe.third;

import ctd.mvc.upload.FileMetaRecord;
import ctd.util.annotation.RpcService;

import java.io.InputStream;

/**
 * @author yinsheng
 * @date 2019\3\7 0007 14:24
 */
public interface IFileDownloadService {
    @RpcService
    public String downloadImg(String ossId);

    @RpcService
    byte[] downloadAsByte(String ossId);

    @RpcService
    FileMetaRecord downloadAsRecord(String ossId);
}
