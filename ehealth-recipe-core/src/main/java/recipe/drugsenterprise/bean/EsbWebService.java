package recipe.drugsenterprise.bean;

import org.apache.axis.client.Call;

import java.util.HashMap;
import java.util.Map;

/**
 * @author yinsheng
 * @date 2020\4\26 0026 15:06
 */
public class EsbWebService {

    static org.apache.axis.description.OperationDesc [] _operations;

    private static String METHOD_NAME ="HXCFZT";
    private String url;
    private String param;
    private String code;

    static {
        _operations = new org.apache.axis.description.OperationDesc[1];
        _initOperationDesc1();
    }

    private static void _initOperationDesc1(){
        _operations[0] = getOperDesc(METHOD_NAME);
    }

    private static org.apache.axis.description.OperationDesc getOperDesc(String name) {
        org.apache.axis.description.OperationDesc oper;
        org.apache.axis.description.ParameterDesc param;
        oper = new org.apache.axis.description.OperationDesc();
        oper.setName(name);
        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("http://tempuri.org/", "xml"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, false, false);
        param.setOmittable(true);
        oper.addParameter(param);
        oper.setReturnType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        oper.setReturnClass(String.class);
        oper.setReturnQName(new javax.xml.namespace.QName("http://tempuri.org/", "HXCFZTResult"));
        oper.setStyle(org.apache.axis.constants.Style.WRAPPED);
        oper.setUse(org.apache.axis.constants.Use.LITERAL);
        _operations[0] = oper;
        return oper;
    }

    public void initConfig(Map<String, String> params) {
        this.url = params.get("url");
    }

    public String HXCFZT(String xml, String method) throws Exception {
        String result = null;
        org.apache.axis.client.Service service = new org.apache.axis.client.Service();
        Call _call = (Call) service.createCall();
        _call.setTargetEndpointAddress(new java.net.URL(url));
        _call.setOperation(_operations[0]);
        _call.setUseSOAPAction(true);
        _call.setSOAPActionURI("http://tempuri.org/" + method);//
        _call.setEncodingStyle(null);
        _call.setProperty(Call.SEND_TYPE_ATTR, Boolean.FALSE);
        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP11_CONSTANTS);
        _call.setOperationName(new javax.xml.namespace.QName("http://tempuri.org/", method));

        result = (String) _call.invoke(new Object[]{xml});

        return result;
    }

    public static void main(String[] args) {
        EsbWebService xkyyHelper = new EsbWebService();
        String url =  "http://180.167.90.54:8031/ShangYaoInferface.asmx?wsdl";
        Map<String, String> param=new HashMap<String, String>();
        param.put("url", url);
        xkyyHelper.initConfig(param);
        String xml = "<root><body><params><sku>13131</sku><pageNo>1</pageNo><pageSize>100</pageSize></params></body></root>";
        try{
            String result = xkyyHelper.HXCFZT(xml, "getMedicineStock");
            System.out.println("result:"+result);
        }catch (Exception ex){
            System.out.println("result:"+ex);
        }

    }
}
