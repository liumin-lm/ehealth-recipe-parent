///**
// * PAWebServiceSoap12Stub.java
// *
// * This file was auto-generated from WSDL
// * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
// */
//
//package recipe.audit.pawebservice;
//
///**
// * @author jiangtingfeng
// */
//public class PAWebServiceSoap12Stub extends org.apache.axis.client.Stub implements PAWebServiceSoap_PortType {
//    private java.util.Vector cachedSerClasses = new java.util.Vector();
//    private java.util.Vector cachedSerQNames = new java.util.Vector();
//    private java.util.Vector cachedSerFactories = new java.util.Vector();
//    private java.util.Vector cachedDeserFactories = new java.util.Vector();
//
//    static org.apache.axis.description.OperationDesc [] _operations;
//
//    static {
//        _operations = new org.apache.axis.description.OperationDesc[6];
//        _initOperationDesc1();
//    }
//
//    private static void _initOperationDesc1(){
//        org.apache.axis.description.OperationDesc oper;
//        org.apache.axis.description.ParameterDesc param;
//        oper = new org.apache.axis.description.OperationDesc();
//        oper.setName("CRMS_WEBSRV");
//        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("WinningPAWebservice", "funId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "int"), int.class, false, false);
//        oper.addParameter(param);
//        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("WinningPAWebservice", "baseData"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, false, false);
//        param.setOmittable(true);
//        oper.addParameter(param);
//        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("WinningPAWebservice", "detailsData"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, false, false);
//        param.setOmittable(true);
//        oper.addParameter(param);
//        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("WinningPAWebservice", "CRMS_WEBSRVResult"), org.apache.axis.description.ParameterDesc.OUT, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "int"), int.class, false, false);
//        oper.addParameter(param);
//        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("WinningPAWebservice", "uiResults"), org.apache.axis.description.ParameterDesc.OUT, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, false, false);
//        param.setOmittable(true);
//        oper.addParameter(param);
//        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("WinningPAWebservice", "hisResults"), org.apache.axis.description.ParameterDesc.OUT, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, false, false);
//        param.setOmittable(true);
//        oper.addParameter(param);
//        oper.setReturnType(org.apache.axis.encoding.XMLType.AXIS_VOID);
//        oper.setStyle(org.apache.axis.constants.Style.WRAPPED);
//        oper.setUse(org.apache.axis.constants.Use.LITERAL);
//        _operations[0] = oper;
//
//        oper = new org.apache.axis.description.OperationDesc();
//        oper.setName("IMDS_WEBSRV");
//        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("WinningPAWebservice", "funId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "int"), int.class, false, false);
//        oper.addParameter(param);
//        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("WinningPAWebservice", "baseData"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, false, false);
//        param.setOmittable(true);
//        oper.addParameter(param);
//        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("WinningPAWebservice", "detailsData"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, false, false);
//        param.setOmittable(true);
//        oper.addParameter(param);
//        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("WinningPAWebservice", "IMDS_WEBSRVResult"), org.apache.axis.description.ParameterDesc.OUT, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "int"), int.class, false, false);
//        oper.addParameter(param);
//        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("WinningPAWebservice", "uiResults"), org.apache.axis.description.ParameterDesc.OUT, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, false, false);
//        param.setOmittable(true);
//        oper.addParameter(param);
//        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("WinningPAWebservice", "hisResults"), org.apache.axis.description.ParameterDesc.OUT, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, false, false);
//        param.setOmittable(true);
//        oper.addParameter(param);
//        oper.setReturnType(org.apache.axis.encoding.XMLType.AXIS_VOID);
//        oper.setStyle(org.apache.axis.constants.Style.WRAPPED);
//        oper.setUse(org.apache.axis.constants.Use.LITERAL);
//        _operations[1] = oper;
//
//        oper = new org.apache.axis.description.OperationDesc();
//        oper.setName("GetPAResults");
//        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("WinningPAWebservice", "funId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "int"), int.class, false, false);
//        oper.addParameter(param);
//        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("WinningPAWebservice", "baseData"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, false, false);
//        param.setOmittable(true);
//        oper.addParameter(param);
//        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("WinningPAWebservice", "detailsData"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, false, false);
//        param.setOmittable(true);
//        oper.addParameter(param);
//        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("WinningPAWebservice", "GetPAResultsResult"), org.apache.axis.description.ParameterDesc.OUT, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "int"), int.class, false, false);
//        oper.addParameter(param);
//        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("WinningPAWebservice", "uiResults"), org.apache.axis.description.ParameterDesc.OUT, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, false, false);
//        param.setOmittable(true);
//        oper.addParameter(param);
//        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("WinningPAWebservice", "hisResults"), org.apache.axis.description.ParameterDesc.OUT, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, false, false);
//        param.setOmittable(true);
//        oper.addParameter(param);
//        oper.setReturnType(org.apache.axis.encoding.XMLType.AXIS_VOID);
//        oper.setStyle(org.apache.axis.constants.Style.WRAPPED);
//        oper.setUse(org.apache.axis.constants.Use.LITERAL);
//        _operations[2] = oper;
//
//        oper = new org.apache.axis.description.OperationDesc();
//        oper.setName("GetPAResultsEx");
//        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("WinningPAWebservice", "funId"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "int"), int.class, false, false);
//        oper.addParameter(param);
//        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("WinningPAWebservice", "baseData"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, false, false);
//        param.setOmittable(true);
//        oper.addParameter(param);
//        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("WinningPAWebservice", "detailsData"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, false, false);
//        param.setOmittable(true);
//        oper.addParameter(param);
//        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("WinningPAWebservice", "timeOut"), org.apache.axis.description.ParameterDesc.IN, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "unsignedInt"), org.apache.axis.types.UnsignedInt.class, false, false);
//        oper.addParameter(param);
//        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("WinningPAWebservice", "GetPAResultsExResult"), org.apache.axis.description.ParameterDesc.OUT, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "int"), int.class, false, false);
//        oper.addParameter(param);
//        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("WinningPAWebservice", "uiResults"), org.apache.axis.description.ParameterDesc.OUT, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, false, false);
//        param.setOmittable(true);
//        oper.addParameter(param);
//        param = new org.apache.axis.description.ParameterDesc(new javax.xml.namespace.QName("WinningPAWebservice", "hisResults"), org.apache.axis.description.ParameterDesc.OUT, new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"), String.class, false, false);
//        param.setOmittable(true);
//        oper.addParameter(param);
//        oper.setReturnType(org.apache.axis.encoding.XMLType.AXIS_VOID);
//        oper.setStyle(org.apache.axis.constants.Style.WRAPPED);
//        oper.setUse(org.apache.axis.constants.Use.LITERAL);
//        _operations[3] = oper;
//
//        oper = new org.apache.axis.description.OperationDesc();
//        oper.setName("GetAdminAccounts");
//        oper.setReturnType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
//        oper.setReturnClass(String.class);
//        oper.setReturnQName(new javax.xml.namespace.QName("WinningPAWebservice", "GetAdminAccountsResult"));
//        oper.setStyle(org.apache.axis.constants.Style.WRAPPED);
//        oper.setUse(org.apache.axis.constants.Use.LITERAL);
//        _operations[4] = oper;
//
//        oper = new org.apache.axis.description.OperationDesc();
//        oper.setName("GetClientVersion");
//        oper.setReturnType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
//        oper.setReturnClass(String.class);
//        oper.setReturnQName(new javax.xml.namespace.QName("WinningPAWebservice", "GetClientVersionResult"));
//        oper.setStyle(org.apache.axis.constants.Style.WRAPPED);
//        oper.setUse(org.apache.axis.constants.Use.LITERAL);
//        _operations[5] = oper;
//
//    }
//
//    public PAWebServiceSoap12Stub() throws org.apache.axis.AxisFault {
//         this(null);
//    }
//
//    public PAWebServiceSoap12Stub(java.net.URL endpointURL, javax.xml.rpc.Service service) throws org.apache.axis.AxisFault {
//         this(service);
//         super.cachedEndpoint = endpointURL;
//    }
//
//    public PAWebServiceSoap12Stub(javax.xml.rpc.Service service) throws org.apache.axis.AxisFault {
//        if (service == null) {
//            super.service = new org.apache.axis.client.Service();
//        } else {
//            super.service = service;
//        }
//        ((org.apache.axis.client.Service)super.service).setTypeMappingVersion("1.2");
//    }
//
//    protected org.apache.axis.client.Call createCall() throws java.rmi.RemoteException {
//        try {
//            org.apache.axis.client.Call _call = super._createCall();
//            if (super.maintainSessionSet) {
//                _call.setMaintainSession(super.maintainSession);
//            }
//            if (super.cachedUsername != null) {
//                _call.setUsername(super.cachedUsername);
//            }
//            if (super.cachedPassword != null) {
//                _call.setPassword(super.cachedPassword);
//            }
//            if (super.cachedEndpoint != null) {
//                _call.setTargetEndpointAddress(super.cachedEndpoint);
//            }
//            if (super.cachedTimeout != null) {
//                _call.setTimeout(super.cachedTimeout);
//            }
//            if (super.cachedPortName != null) {
//                _call.setPortName(super.cachedPortName);
//            }
//            java.util.Enumeration keys = super.cachedProperties.keys();
//            while (keys.hasMoreElements()) {
//                String key = (String) keys.nextElement();
//                _call.setProperty(key, super.cachedProperties.get(key));
//            }
//            return _call;
//        }
//        catch (Throwable _t) {
//            throw new org.apache.axis.AxisFault("Failure trying to get the Call object", _t);
//        }
//    }
//
//    public void CRMS_WEBSRV(int funId, String baseData, String detailsData, javax.xml.rpc.holders.IntHolder CRMS_WEBSRVResult, javax.xml.rpc.holders.StringHolder uiResults, javax.xml.rpc.holders.StringHolder hisResults) throws java.rmi.RemoteException {
//        if (super.cachedEndpoint == null) {
//            throw new org.apache.axis.NoEndPointException();
//        }
//        org.apache.axis.client.Call _call = createCall();
//        _call.setOperation(_operations[0]);
//        _call.setUseSOAPAction(true);
//        _call.setSOAPActionURI("WinningPAWebservice/CRMS_WEBSRV");
//        _call.setEncodingStyle(null);
//        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
//        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
//        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP12_CONSTANTS);
//        _call.setOperationName(new javax.xml.namespace.QName("WinningPAWebservice", "CRMS_WEBSRV"));
//
//        setRequestHeaders(_call);
//        setAttachments(_call);
// try {        Object _resp = _call.invoke(new Object[] {new Integer(funId), baseData, detailsData});
//
//        if (_resp instanceof java.rmi.RemoteException) {
//            throw (java.rmi.RemoteException)_resp;
//        }
//        else {
//            extractAttachments(_call);
//            java.util.Map _output;
//            _output = _call.getOutputParams();
//            try {
//                CRMS_WEBSRVResult.value = ((Integer) _output.get(new javax.xml.namespace.QName("WinningPAWebservice", "CRMS_WEBSRVResult"))).intValue();
//            } catch (Exception _exception) {
//                CRMS_WEBSRVResult.value = ((Integer) org.apache.axis.utils.JavaUtils.convert(_output.get(new javax.xml.namespace.QName("WinningPAWebservice", "CRMS_WEBSRVResult")), int.class)).intValue();
//            }
//            try {
//                uiResults.value = (String) _output.get(new javax.xml.namespace.QName("WinningPAWebservice", "uiResults"));
//            } catch (Exception _exception) {
//                uiResults.value = (String) org.apache.axis.utils.JavaUtils.convert(_output.get(new javax.xml.namespace.QName("WinningPAWebservice", "uiResults")), String.class);
//            }
//            try {
//                hisResults.value = (String) _output.get(new javax.xml.namespace.QName("WinningPAWebservice", "hisResults"));
//            } catch (Exception _exception) {
//                hisResults.value = (String) org.apache.axis.utils.JavaUtils.convert(_output.get(new javax.xml.namespace.QName("WinningPAWebservice", "hisResults")), String.class);
//            }
//        }
//  } catch (org.apache.axis.AxisFault axisFaultException) {
//  throw axisFaultException;
//}
//    }
//
//    public void IMDS_WEBSRV(int funId, String baseData, String detailsData, javax.xml.rpc.holders.IntHolder IMDS_WEBSRVResult, javax.xml.rpc.holders.StringHolder uiResults, javax.xml.rpc.holders.StringHolder hisResults) throws java.rmi.RemoteException {
//        if (super.cachedEndpoint == null) {
//            throw new org.apache.axis.NoEndPointException();
//        }
//        org.apache.axis.client.Call _call = createCall();
//        _call.setOperation(_operations[1]);
//        _call.setUseSOAPAction(true);
//        _call.setSOAPActionURI("WinningPAWebservice/IMDS_WEBSRV");
//        _call.setEncodingStyle(null);
//        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
//        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
//        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP12_CONSTANTS);
//        _call.setOperationName(new javax.xml.namespace.QName("WinningPAWebservice", "IMDS_WEBSRV"));
//
//        setRequestHeaders(_call);
//        setAttachments(_call);
// try {        Object _resp = _call.invoke(new Object[] {new Integer(funId), baseData, detailsData});
//
//        if (_resp instanceof java.rmi.RemoteException) {
//            throw (java.rmi.RemoteException)_resp;
//        }
//        else {
//            extractAttachments(_call);
//            java.util.Map _output;
//            _output = _call.getOutputParams();
//            try {
//                IMDS_WEBSRVResult.value = ((Integer) _output.get(new javax.xml.namespace.QName("WinningPAWebservice", "IMDS_WEBSRVResult"))).intValue();
//            } catch (Exception _exception) {
//                IMDS_WEBSRVResult.value = ((Integer) org.apache.axis.utils.JavaUtils.convert(_output.get(new javax.xml.namespace.QName("WinningPAWebservice", "IMDS_WEBSRVResult")), int.class)).intValue();
//            }
//            try {
//                uiResults.value = (String) _output.get(new javax.xml.namespace.QName("WinningPAWebservice", "uiResults"));
//            } catch (Exception _exception) {
//                uiResults.value = (String) org.apache.axis.utils.JavaUtils.convert(_output.get(new javax.xml.namespace.QName("WinningPAWebservice", "uiResults")), String.class);
//            }
//            try {
//                hisResults.value = (String) _output.get(new javax.xml.namespace.QName("WinningPAWebservice", "hisResults"));
//            } catch (Exception _exception) {
//                hisResults.value = (String) org.apache.axis.utils.JavaUtils.convert(_output.get(new javax.xml.namespace.QName("WinningPAWebservice", "hisResults")), String.class);
//            }
//        }
//  } catch (org.apache.axis.AxisFault axisFaultException) {
//  throw axisFaultException;
//}
//    }
//
//    public void getPAResults(int funId, String baseData, String detailsData, javax.xml.rpc.holders.IntHolder getPAResultsResult, javax.xml.rpc.holders.StringHolder uiResults, javax.xml.rpc.holders.StringHolder hisResults) throws java.rmi.RemoteException {
//        if (super.cachedEndpoint == null) {
//            throw new org.apache.axis.NoEndPointException();
//        }
//        org.apache.axis.client.Call _call = createCall();
//        _call.setOperation(_operations[2]);
//        _call.setUseSOAPAction(true);
//        _call.setSOAPActionURI("WinningPAWebservice/GetPAResults");
//        _call.setEncodingStyle(null);
//        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
//        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
//        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP12_CONSTANTS);
//        _call.setOperationName(new javax.xml.namespace.QName("WinningPAWebservice", "GetPAResults"));
//
//        setRequestHeaders(_call);
//        setAttachments(_call);
//        try {
//            Object _resp =
//                _call.invoke(new Object[] {new Integer(funId), baseData,
//                    detailsData});
//
//            if (_resp instanceof java.rmi.RemoteException) {
//                throw (java.rmi.RemoteException)_resp;
//            }
//            else {
//                extractAttachments(_call);
//                java.util.Map _output;
//                _output = _call.getOutputParams();
//                try {
//                    getPAResultsResult.value =
//                        ((Integer)_output.get(new javax.xml.namespace.QName(
//                            "WinningPAWebservice",
//                            "GetPAResultsResult"))).intValue();
//                }
//                catch (Exception _exception) {
//                    getPAResultsResult.value =
//                        ((Integer)org.apache.axis.utils.JavaUtils.convert(
//                            _output.get(new javax.xml.namespace.QName(
//                                "WinningPAWebservice",
//                                "GetPAResultsResult")),
//                            int.class)).intValue();
//                }
//                try {
//                    uiResults.value =
//                        (String)_output.get(new javax.xml.namespace.QName(
//                            "WinningPAWebservice",
//                            "uiResults"));
//                }
//                catch (Exception _exception) {
//                    uiResults.value =
//                        (String)org.apache.axis.utils.JavaUtils.convert(_output.get(
//                            new javax.xml.namespace.QName("WinningPAWebservice",
//                                "uiResults")), String.class);
//                }
//                try {
//                    hisResults.value =
//                        (String)_output.get(new javax.xml.namespace.QName(
//                            "WinningPAWebservice",
//                            "hisResults"));
//                }
//                catch (Exception _exception) {
//                    hisResults.value =
//                        (String)org.apache.axis.utils.JavaUtils.convert(_output.get(
//                            new javax.xml.namespace.QName("WinningPAWebservice",
//                                "hisResults")), String.class);
//                }
//            }
//        } catch (org.apache.axis.AxisFault axisFaultException) {
//                throw axisFaultException;
//        }
//    }
//
//    public void getPAResultsEx(int funId, String baseData, String detailsData, org.apache.axis.types.UnsignedInt timeOut, javax.xml.rpc.holders.IntHolder getPAResultsExResult, javax.xml.rpc.holders.StringHolder uiResults, javax.xml.rpc.holders.StringHolder hisResults) throws java.rmi.RemoteException {
//        if (super.cachedEndpoint == null) {
//            throw new org.apache.axis.NoEndPointException();
//        }
//        org.apache.axis.client.Call _call = createCall();
//        _call.setOperation(_operations[3]);
//        _call.setUseSOAPAction(true);
//        _call.setSOAPActionURI("WinningPAWebservice/GetPAResultsEx");
//        _call.setEncodingStyle(null);
//        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
//        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
//        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP12_CONSTANTS);
//        _call.setOperationName(new javax.xml.namespace.QName("WinningPAWebservice", "GetPAResultsEx"));
//
//        setRequestHeaders(_call);
//        setAttachments(_call);
// try {        Object _resp = _call.invoke(new Object[] {new Integer(funId), baseData, detailsData, timeOut});
//
//        if (_resp instanceof java.rmi.RemoteException) {
//            throw (java.rmi.RemoteException)_resp;
//        }
//        else {
//            extractAttachments(_call);
//            java.util.Map _output;
//            _output = _call.getOutputParams();
//            try {
//                getPAResultsExResult.value = ((Integer) _output.get(new javax.xml.namespace.QName("WinningPAWebservice", "GetPAResultsExResult"))).intValue();
//            } catch (Exception _exception) {
//                getPAResultsExResult.value = ((Integer) org.apache.axis.utils.JavaUtils.convert(_output.get(new javax.xml.namespace.QName("WinningPAWebservice", "GetPAResultsExResult")), int.class)).intValue();
//            }
//            try {
//                uiResults.value = (String) _output.get(new javax.xml.namespace.QName("WinningPAWebservice", "uiResults"));
//            } catch (Exception _exception) {
//                uiResults.value = (String) org.apache.axis.utils.JavaUtils.convert(_output.get(new javax.xml.namespace.QName("WinningPAWebservice", "uiResults")), String.class);
//            }
//            try {
//                hisResults.value = (String) _output.get(new javax.xml.namespace.QName("WinningPAWebservice", "hisResults"));
//            } catch (Exception _exception) {
//                hisResults.value = (String) org.apache.axis.utils.JavaUtils.convert(_output.get(new javax.xml.namespace.QName("WinningPAWebservice", "hisResults")), String.class);
//            }
//        }
//  } catch (org.apache.axis.AxisFault axisFaultException) {
//  throw axisFaultException;
//}
//    }
//
//    public String getAdminAccounts() throws java.rmi.RemoteException {
//        if (super.cachedEndpoint == null) {
//            throw new org.apache.axis.NoEndPointException();
//        }
//        org.apache.axis.client.Call _call = createCall();
//        _call.setOperation(_operations[4]);
//        _call.setUseSOAPAction(true);
//        _call.setSOAPActionURI("WinningPAWebservice/GetAdminAccounts");
//        _call.setEncodingStyle(null);
//        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
//        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
//        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP12_CONSTANTS);
//        _call.setOperationName(new javax.xml.namespace.QName("WinningPAWebservice", "GetAdminAccounts"));
//
//        setRequestHeaders(_call);
//        setAttachments(_call);
// try {        Object _resp = _call.invoke(new Object[] {});
//
//        if (_resp instanceof java.rmi.RemoteException) {
//            throw (java.rmi.RemoteException)_resp;
//        }
//        else {
//            extractAttachments(_call);
//            try {
//                return (String) _resp;
//            } catch (Exception _exception) {
//                return (String) org.apache.axis.utils.JavaUtils.convert(_resp, String.class);
//            }
//        }
//  } catch (org.apache.axis.AxisFault axisFaultException) {
//  throw axisFaultException;
//}
//    }
//
//    public String getClientVersion() throws java.rmi.RemoteException {
//        if (super.cachedEndpoint == null) {
//            throw new org.apache.axis.NoEndPointException();
//        }
//        org.apache.axis.client.Call _call = createCall();
//        _call.setOperation(_operations[5]);
//        _call.setUseSOAPAction(true);
//        _call.setSOAPActionURI("WinningPAWebservice/GetClientVersion");
//        _call.setEncodingStyle(null);
//        _call.setProperty(org.apache.axis.client.Call.SEND_TYPE_ATTR, Boolean.FALSE);
//        _call.setProperty(org.apache.axis.AxisEngine.PROP_DOMULTIREFS, Boolean.FALSE);
//        _call.setSOAPVersion(org.apache.axis.soap.SOAPConstants.SOAP12_CONSTANTS);
//        _call.setOperationName(new javax.xml.namespace.QName("WinningPAWebservice", "GetClientVersion"));
//
//        setRequestHeaders(_call);
//        setAttachments(_call);
// try {        Object _resp = _call.invoke(new Object[] {});
//
//        if (_resp instanceof java.rmi.RemoteException) {
//            throw (java.rmi.RemoteException)_resp;
//        }
//        else {
//            extractAttachments(_call);
//            try {
//                return (String) _resp;
//            } catch (Exception _exception) {
//                return (String) org.apache.axis.utils.JavaUtils.convert(_resp, String.class);
//            }
//        }
//  } catch (org.apache.axis.AxisFault axisFaultException) {
//  throw axisFaultException;
//}
//    }
//
//}
