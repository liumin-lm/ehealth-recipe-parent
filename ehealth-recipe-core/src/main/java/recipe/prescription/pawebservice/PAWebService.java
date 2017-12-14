/**
 * PAWebService.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package recipe.prescription.pawebservice;

public interface PAWebService extends javax.xml.rpc.Service {
    public String getPAWebServiceSoap12Address();

    public PAWebServiceSoap_PortType getPAWebServiceSoap12() throws javax.xml.rpc.ServiceException;

    public PAWebServiceSoap_PortType getPAWebServiceSoap12(
        java.net.URL portAddress) throws javax.xml.rpc.ServiceException;
    public String getPAWebServiceSoapAddress();

    public PAWebServiceSoap_PortType getPAWebServiceSoap() throws javax.xml.rpc.ServiceException;

    public PAWebServiceSoap_PortType getPAWebServiceSoap(
        java.net.URL portAddress) throws javax.xml.rpc.ServiceException;
}
