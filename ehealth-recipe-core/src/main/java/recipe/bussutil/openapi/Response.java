package recipe.bussutil.openapi;

import ngari.openapi.JSONResponseBean;

import java.util.HashMap;
import java.util.Map;

public class Response {
	/**
	 * http 请求状态码
	 */
    private int statusCode;
    private String contentType;
	/**
	 * 请求唯一 ID，请求一旦进入 API 网关应用后，API 网关就会生成请求 ID 并通过响应头返回给客户端，
	 * 建议客户端与后端服务都记录此请求 ID，可用于问题排查与跟踪。
	 */
	private String requestId;
	/**
	 * API网关会将服务端的StringToSign放到HTTP应答的Header中返回到客户端，Key为：X-Ca-Error-Message
	 * 只需要将本地计算的StringToSign与服务端返回的StringToSign进行对比即可找到问题；
	 * 如果服务端与客户端的签名串是一致的，请检查用于签名计算的密钥是否正确；
	 */
	private String caErrorMsg;
	/**
	 * 业务接口返回的错误信息
	 */
	private String errorMessage;
    private Map<String, String> headers;
    private String body;
    private JSONResponseBean jsonResponseBean;

    public boolean isSuccess(){
		return (200==statusCode&&jsonResponseBean!=null&&200==jsonResponseBean.getCode());
	}
    
    public Response() {
		
    }

	public int getStatusCode() {
		return statusCode;
	}

	public void setStatusCode(int statusCode) {
		this.statusCode = statusCode;
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public String getRequestId() {
		return requestId;
	}

	public void setRequestId(String requestId) {
		this.requestId = requestId;
	}

	public String getErrorMessage() {
    	if(jsonResponseBean!=null&&!"200".equals(jsonResponseBean.getCode())){
    		return jsonResponseBean.getMsg();
		}
		return null;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public Map<String, String> getHeaders() {
		return headers;
	}
	
	public String getHeader(String key) {
		if (null != headers) {
			return headers.get(key);
		} else {
			return null;
		}
	}

	public void setHeaders(Map<String, String> headers) {
		this.headers = headers;
	}
	
	public void setHeader(String key, String value) {
		if (null == this.headers) {
			this.headers = new HashMap<String, String>(); 
		}
		this.headers.put(key, value);
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public JSONResponseBean getJsonResponseBean() {
		return jsonResponseBean;
	}

	public void setJsonResponseBean(JSONResponseBean jsonResponseBean) {
		this.jsonResponseBean = jsonResponseBean;
	}

	public String getCaErrorMsg() {
		return caErrorMsg;
	}

	public void setCaErrorMsg(String caErrorMsg) {
		this.caErrorMsg = caErrorMsg;
	}
}
