/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package recipe.bussutil.openapi;

import ngari.openapi.constant.SystemHeader;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Request
 */
public class Request {

    public Request() {
    }

    public Request(String serviceId,String method,List<Object> bodys) {
        this.appKey = appKey;
        this.appSecret = appSecret;
        this.bodys=bodys;
        headers = new HashMap<>();
        headers.put(SystemHeader.X_CA_KEY, appKey);
        headers.put(SystemHeader.X_SERVICE_ID, serviceId);
        headers.put(SystemHeader.X_SERVICE_METHOD, method);
    }
    /**
     * （必选）服务请求地址
     */
    private String apiUrl;



    /**
     * （必选)APP KEY
     */
    private String appKey;

    /**
     * （必选）APP密钥
     */
    private String appSecret;

    private String encodingAesKey;


    /**
     * （必选） HTTP头
     */
    private Map<String, String> headers;
    


    /**
     * （必选）请求参数
     */
    private List<Object> bodys;

    /**
     * （可选）字符串Body体
     */
    private String stringBody;


    public String getAppKey() {
        return appKey;
    }

    public void setAppKey(String appKey) {
        this.appKey = appKey;
    }

    public String getAppSecret() {
        return appSecret;
    }

    public void setAppSecret(String appSecret) {
        this.appSecret = appSecret;
    }


    public Map<String, String> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public List<Object> getBodys() {
        return bodys;
    }

    public void setBodys(List<Object> bodys) {
        this.bodys = bodys;
    }

    public String getStringBody() {
        return stringBody;
    }

    public void setStringBody(String stringBody) {
        this.stringBody = stringBody;
    }

    public String getEncodingAesKey() {
        return encodingAesKey;
    }

    public void setEncodingAesKey(String encodingAesKey) {
        this.encodingAesKey = encodingAesKey;
    }

    public String getApiUrl() {
        return apiUrl;
    }

    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }
    public void addHeader(String name,String value){
        if(headers==null){
            headers = new HashMap<>();
        }
        headers.put(name, value);
    }
}
