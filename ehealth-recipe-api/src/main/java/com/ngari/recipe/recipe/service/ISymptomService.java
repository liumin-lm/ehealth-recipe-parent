package com.ngari.recipe.recipe.service;

import com.ngari.recipe.recipe.model.SymptomDTO;
import ctd.persistence.bean.QueryResult;
import ctd.util.annotation.RpcService;

import java.util.List;
import java.util.Map;

public interface ISymptomService {

    /**
     * 新增症候
     * @param symptom
     * @return
     */
    @RpcService
    boolean addSymptomForOrgan(SymptomDTO symptom);

    /**
     * 根据机构ID和症候名称模糊查询症候
     * @param organId
     * @param input
     * @param start
     * @param limit
     * @return
     */
    @RpcService
    QueryResult<SymptomDTO> querSymptomByOrganIdAndName(Integer organId , String input, final int start, final int limit);

    /**
     *根据机构Id查找对应症候
     * @param organId
     * @return
     */
    @RpcService
    List<SymptomDTO> querSymptomByOrganId(Integer organId );


    /**根据机构Id 和 症候ID查询中医症候
     *
     * @param organId
     * @param symptomId
     * @return
     */
    @RpcService
    SymptomDTO querSymptomByOrganIdAndSymptomId(Integer organId , Integer symptomId );

    /**
     * 症候批量导入
     * @param buf
     * @param originalFilename
     * @param organId
     * @param operator
     * @param ossId
     * @return
     */
    @RpcService
    Map<String, Object> readSymptomExcel(byte[] buf, String originalFilename, int organId, String operator, String ossId,String manageUnit);

}
