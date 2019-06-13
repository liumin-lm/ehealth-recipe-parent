package com.ngari.recipe.filedownload.service;

import ctd.util.annotation.RpcService;

/**
 * @author yinsheng
 * @date 2019\3\7 0007 14:24
 */
public interface IFileDownloadService {
    @RpcService
    public String downloadImg(String ossId);
}
