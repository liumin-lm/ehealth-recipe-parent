///**
// * PAWebServiceLocator.java
// *
// * This file was auto-generated from WSDL
// * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
// */
//
//package recipe.audit.pawebservice;
//
//import recipe.audit.bean.PrescriptionConstants;
//
///**
// * @author jiangtingfeng
// */
//public class PAWebServiceLocator extends org.apache.axis.client.Service implements PAWebService {
//
//    private static final long serialVersionUID = 1938957651929348159L;
//
//
//    /**
//     * Use to get a proxy class for PAWebServiceSoap12
//     */
//    private String PAWebServiceSoap12_address = PrescriptionConstants.getWeiningPaAddress();
//
//    public String getPAWebServiceSoap12Address() {
//        return PAWebServiceSoap12_address;
//    }
//
//    /**
//     * The WSDD service name defaults to the port name.
//     */
//    private String PAWebServiceSoap12WSDDServiceName = "PAWebServiceSoap12";
//
//    public String getPAWebServiceSoap12WSDDServiceName() {
//        return PAWebServiceSoap12WSDDServiceName;
//    }
//
//
//    public PAWebServiceSoap_PortType getPAWebServiceSoap12() throws javax.xml.rpc.ServiceException {
//       java.net.URL endpoint;
//        try {
//            endpoint = new java.net.URL(PAWebServiceSoap12_address);
//        }
//        catch (java.net.MalformedURLException e) {
//            throw new javax.xml.rpc.ServiceException(e);
//        }
//        return getPAWebServiceSoap12(endpoint);
//    }
//
//    public PAWebServiceSoap_PortType getPAWebServiceSoap12(java.net.URL portAddress) throws javax.xml.rpc.ServiceException {
//        try {
//           PAWebServiceSoap12Stub stub = new PAWebServiceSoap12Stub(portAddress, this);
//            stub.setPortName(getPAWebServiceSoap12WSDDServiceName());
//            return stub;
//        }
//        catch (org.apache.axis.AxisFault e) {
//            return null;
//        }
//    }
//
//
//
//    /**
//     * Use to get a proxy class for PAWebServiceSoap
//     */
//    private String PAWebServiceSoap_address =  PrescriptionConstants.getWeiningPaAddress();
//
//    public String getPAWebServiceSoapAddress() {
//        return PAWebServiceSoap_address;
//    }
//
//    /**
//     * The WSDD service name defaults to the port name.
//     */
//    private String PAWebServiceSoapWSDDServiceName = "PAWebServiceSoap";
//
//    public String getPAWebServiceSoapWSDDServiceName() {
//        return PAWebServiceSoapWSDDServiceName;
//    }
//
//
//    public PAWebServiceSoap_PortType getPAWebServiceSoap() throws javax.xml.rpc.ServiceException {
//       java.net.URL endpoint;
//        try {
//            endpoint = new java.net.URL(PAWebServiceSoap_address);
//        }
//        catch (java.net.MalformedURLException e) {
//            throw new javax.xml.rpc.ServiceException(e);
//        }
//        return getPAWebServiceSoap(endpoint);
//    }
//
//    public PAWebServiceSoap_PortType getPAWebServiceSoap(java.net.URL portAddress) throws javax.xml.rpc.ServiceException {
//        try {
//            PAWebServiceSoap_BindingStub stub = new PAWebServiceSoap_BindingStub(portAddress, this);
//            stub.setPortName(getPAWebServiceSoapWSDDServiceName());
//            return stub;
//        }
//        catch (org.apache.axis.AxisFault e) {
//            return null;
//        }
//    }
//
//
//    /**
//     * For the given interface, get the stub implementation.
//     * If this service has no port for the given interface,
//     * then ServiceException is thrown.
//     * This service has multiple ports for a given interface;
//     * the proxy implementation returned may be indeterminate.
//     */
//    public java.rmi.Remote getPort(Class serviceEndpointInterface) throws javax.xml.rpc.ServiceException {
//        try {
//            if ( PAWebServiceSoap_PortType.class.isAssignableFrom(serviceEndpointInterface)) {
//                PAWebServiceSoap12Stub stub = new PAWebServiceSoap12Stub(new java.net.URL(PAWebServiceSoap12_address), this);
//                stub.setPortName(getPAWebServiceSoap12WSDDServiceName());
//                return stub;
//            }
//            if (PAWebServiceSoap_PortType.class.isAssignableFrom(serviceEndpointInterface)) {
//                PAWebServiceSoap_BindingStub stub = new PAWebServiceSoap_BindingStub(new java.net.URL(PAWebServiceSoap_address), this);
//                stub.setPortName(getPAWebServiceSoapWSDDServiceName());
//                return stub;
//            }
//        }
//        catch (Throwable t) {
//            throw new javax.xml.rpc.ServiceException(t);
//        }
//        throw new javax.xml.rpc.ServiceException("There is no stub implementation for the interface:  " + (serviceEndpointInterface == null ? "null" : serviceEndpointInterface.getName()));
//    }
//
//    /**
//     * For the given interface, get the stub implementation.
//     * If this service has no port for the given interface,
//     * then ServiceException is thrown.
//     */
//    public java.rmi.Remote getPort(javax.xml.namespace.QName portName, Class serviceEndpointInterface) throws javax.xml.rpc.ServiceException {
//        if (portName == null) {
//            return getPort(serviceEndpointInterface);
//        }
//        String inputPortName = portName.getLocalPart();
//        if (getPAWebServiceSoap12WSDDServiceName().equals(inputPortName)) {
//            return getPAWebServiceSoap12();
//        }
//        else if (getPAWebServiceSoapWSDDServiceName().equals(inputPortName)) {
//            return getPAWebServiceSoap();
//        }
//        else  {
//            java.rmi.Remote stub = getPort(serviceEndpointInterface);
//            ((org.apache.axis.client.Stub) stub).setPortName(portName);
//            return stub;
//        }
//    }
//
//    public javax.xml.namespace.QName getServiceName() {
//        return new javax.xml.namespace.QName(PrescriptionConstants.NAMESPACE, "PAWebService");
//    }
//
//    private java.util.HashSet ports = null;
//
//    public java.util.Iterator getPorts() {
//        if (ports == null) {
//            ports = new java.util.HashSet();
//            ports.add(new javax.xml.namespace.QName(PrescriptionConstants.NAMESPACE, "PAWebServiceSoap12"));
//            ports.add(new javax.xml.namespace.QName(PrescriptionConstants.NAMESPACE, "PAWebServiceSoap"));
//        }
//        return ports.iterator();
//    }
//
//}
