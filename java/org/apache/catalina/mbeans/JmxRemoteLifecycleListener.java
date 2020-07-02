/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.catalina.mbeans;

import java.io.IOException;
import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.rmi.AccessException;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.util.HashMap;
import java.util.Map;

import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.rmi.RMIConnectorServer;
import javax.management.remote.rmi.RMIJRMPServerImpl;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSessionContext;
import javax.rmi.ssl.SslRMIClientSocketFactory;
import javax.rmi.ssl.SslRMIServerSocketFactory;

import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.util.net.SSLHostConfig;
import org.apache.tomcat.util.net.SSLHostConfigCertificate;
import org.apache.tomcat.util.net.jsse.JSSEUtil;
import org.apache.tomcat.util.res.StringManager;

/**
 * This listener fixes the port used by JMX/RMI Server making things much
 * simpler if you need to connect jconsole or similar to a remote Tomcat
 * instance that is running behind a firewall. Only the ports are configured via
 * the listener. The remainder of the configuration is via the standard system
 * properties for configuring JMX.
 *
 * @deprecated The features provided by this listener are now available in the
 *             remote JMX capability included with the JRE.
 *             This listener will be removed in Tomcat 10 and may be removed
 *             from Tomcat 9.0.x some time after 2020-12-31.
 */
@Deprecated
public class JmxRemoteLifecycleListener extends SSLHostConfig implements LifecycleListener {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(JmxRemoteLifecycleListener.class);

    protected static final StringManager sm =
            StringManager.getManager(JmxRemoteLifecycleListener.class);

    protected String rmiBindAddress = null;
    protected int rmiRegistryPortPlatform = -1;
    protected int rmiServerPortPlatform = -1;
    protected boolean rmiRegistrySSL = true;
    protected boolean rmiServerSSL = true;
    protected boolean authenticate = true;
    protected String passwordFile = null;
    protected String loginModuleName = null;
    protected String accessFile = null;
    protected boolean useLocalPorts = false;

    protected transient JMXConnectorServer csPlatform = null;

    /**
     * Get the inet address on which the Platform RMI server is exported.
     * @return The textual representation of inet address
     */
    public String getRmiBindAddress() {
        return rmiBindAddress;
    }

    /**
     * Set the inet address on which the Platform RMI server is exported.
     * @param theRmiBindAddress The textual representation of inet address
     */
    public void setRmiBindAddress(String theRmiBindAddress) {
        rmiBindAddress = theRmiBindAddress;
    }

    /**
     * Get the port on which the Platform RMI server is exported. This is the
     * port that is normally chosen by the RMI stack.
     * @return The port number
     */
    public int getRmiServerPortPlatform() {
        return rmiServerPortPlatform;
    }

    /**
     * Set the port on which the Platform RMI server is exported. This is the
     * port that is normally chosen by the RMI stack.
     * @param theRmiServerPortPlatform The port number
     */
    public void setRmiServerPortPlatform(int theRmiServerPortPlatform) {
        rmiServerPortPlatform = theRmiServerPortPlatform;
    }

    /**
     * Get the port on which the Platform RMI registry is exported.
     * @return The port number
     */
    public int getRmiRegistryPortPlatform() {
        return rmiRegistryPortPlatform;
    }

    /**
     * Set the port on which the Platform RMI registry is exported.
     * @param theRmiRegistryPortPlatform The port number
     */
    public void setRmiRegistryPortPlatform(int theRmiRegistryPortPlatform) {
        rmiRegistryPortPlatform = theRmiRegistryPortPlatform;
    }

    /**
     * Get the flag that indicates that local ports should be used for all
     * connections. If using SSH tunnels, or similar, this should be set to
     * true to ensure the RMI client uses the tunnel.
     * @return <code>true</code> if local ports should be used
     */
    public boolean getUseLocalPorts() {
        return useLocalPorts;
    }

    /**
     * Set the flag that indicates that local ports should be used for all
     * connections. If using SSH tunnels, or similar, this should be set to
     * true to ensure the RMI client uses the tunnel.
     * @param useLocalPorts Set to <code>true</code> if local ports should be
     *                      used
     */
    public void setUseLocalPorts(boolean useLocalPorts) {
        this.useLocalPorts = useLocalPorts;
    }

    /**
     * @return the rmiRegistrySSL
     */
    public boolean isRmiRegistrySSL() {
        return rmiRegistrySSL;
    }

    /**
     * @param rmiRegistrySSL the rmiRegistrySSL to set
     */
    public void setRmiRegistrySSL(boolean rmiRegistrySSL) {
        this.rmiRegistrySSL = rmiRegistrySSL;
    }

    /**
     * @return the rmiServerSSL
     */
    public boolean isRmiServerSSL() {
        return rmiServerSSL;
    }

    /**
     * @param rmiServerSSL the rmiServerSSL to set
     */
    public void setRmiServerSSL(boolean rmiServerSSL) {
        this.rmiServerSSL = rmiServerSSL;
    }

    /**
     * @return the authenticate
     */
    public boolean isAuthenticate() {
        return authenticate;
    }

    /**
     * @param authenticate the authenticate to set
     */
    public void setAuthenticate(boolean authenticate) {
        this.authenticate = authenticate;
    }

    /**
     * @return the passwordFile
     */
    public String getPasswordFile() {
        return passwordFile;
    }

    /**
     * @param passwordFile the passwordFile to set
     */
    public void setPasswordFile(String passwordFile) {
        this.passwordFile = passwordFile;
    }

    /**
     * @return the loginModuleName
     */
    public String getLoginModuleName() {
        return loginModuleName;
    }

    /**
     * @param loginModuleName the loginModuleName to set
     */
    public void setLoginModuleName(String loginModuleName) {
        this.loginModuleName = loginModuleName;
    }

    /**
     * @return the accessFile
     */
    public String getAccessFile() {
        return accessFile;
    }

    /**
     * @param accessFile the accessFile to set
     */
    public void setAccessFile(String accessFile) {
        this.accessFile = accessFile;
    }

    protected void init() {
        // Get all the other parameters required from the standard system
        // properties. Only need to get the parameters that affect the creation
        // of the server port.
        String rmiRegistrySSLValue = System.getProperty("com.sun.management.jmxremote.registry.ssl");
        if (rmiRegistrySSLValue != null) {
            setRmiRegistrySSL(Boolean.parseBoolean(rmiRegistrySSLValue));
        }

        String rmiServerSSLValue = System.getProperty("com.sun.management.jmxremote.ssl");
        if (rmiServerSSLValue != null) {
            setRmiServerSSL(Boolean.parseBoolean(rmiServerSSLValue));
        }

        String protocolsValue = System.getProperty("com.sun.management.jmxremote.ssl.enabled.protocols");
        if (protocolsValue != null) {
            setEnabledProtocols(protocolsValue.split(","));
        }

        String ciphersValue = System.getProperty("com.sun.management.jmxremote.ssl.enabled.cipher.suites");
        if (ciphersValue != null) {
            setCiphers(ciphersValue);
        }

        String clientAuthValue = System.getProperty("com.sun.management.jmxremote.ssl.need.client.auth");
        if (clientAuthValue != null) {
            setCertificateVerification(clientAuthValue);
        }

        String authenticateValue = System.getProperty("com.sun.management.jmxremote.authenticate");
        if (authenticateValue != null) {
            setAuthenticate(Boolean.parseBoolean(authenticateValue));
        }

        String passwordFileValue = System.getProperty("com.sun.management.jmxremote.password.file");
        if (passwordFileValue != null) {
            setPasswordFile(passwordFileValue);
        }

        String accessFileValue = System.getProperty("com.sun.management.jmxremote.access.file");
        if (accessFileValue != null) {
            setAccessFile(accessFileValue);
        }

        String loginModuleNameValue = System.getProperty("com.sun.management.jmxremote.login.config");
        if (loginModuleNameValue != null) {
            setLoginModuleName(loginModuleNameValue);
        }
    }


    @Override
    public void lifecycleEvent(LifecycleEvent event) {
        if (Lifecycle.BEFORE_INIT_EVENT.equals(event.getType())) {
            log.warn(sm.getString("jmxRemoteLifecycleListener.deprecated"));
        } else  if (Lifecycle.START_EVENT.equals(event.getType())) {
            // When the server starts, configure JMX/RMI

            // Configure using standard JMX system properties
            init();

            SSLContext sslContext = null;
            // Create SSL context if properties were set to define a certificate
            if (getCertificates().size() > 0) {
                SSLHostConfigCertificate certificate = getCertificates().iterator().next();
                // This can only support JSSE
                JSSEUtil sslUtil = new JSSEUtil(certificate);
                try {
                    sslContext = javax.net.ssl.SSLContext.getInstance(getSslProtocol());
                    setEnabledProtocols(sslUtil.getEnabledProtocols());
                    setEnabledCiphers(sslUtil.getEnabledCiphers());
                    sslContext.init(sslUtil.getKeyManagers(), sslUtil.getTrustManagers(), null);
                    SSLSessionContext sessionContext = sslContext.getServerSessionContext();
                    if (sessionContext != null) {
                        sslUtil.configureSessionContext(sessionContext);
                    }
                } catch (Exception e) {
                    log.error(sm.getString("jmxRemoteLifecycleListener.invalidSSLConfiguration"), e);
                }
            }

            // Prevent an attacker guessing the RMI object ID
            System.setProperty("java.rmi.server.randomIDs", "true");

            // Create the environment
            Map<String,Object> env = new HashMap<>();

            RMIClientSocketFactory registryCsf = null;
            RMIServerSocketFactory registrySsf = null;

            RMIClientSocketFactory serverCsf = null;
            RMIServerSocketFactory serverSsf = null;

            // Configure registry socket factories
            if (rmiRegistrySSL) {
                registryCsf = new SslRMIClientSocketFactory();
                if (rmiBindAddress == null) {
                    registrySsf = new SslRMIServerSocketFactory(sslContext,
                            getEnabledCiphers(), getEnabledProtocols(),
                            getCertificateVerification() == CertificateVerification.REQUIRED);
                } else {
                    registrySsf = new SslRmiServerBindSocketFactory(sslContext,
                            getEnabledCiphers(), getEnabledProtocols(),
                            getCertificateVerification() == CertificateVerification.REQUIRED,
                            rmiBindAddress);
                }
            } else {
                if (rmiBindAddress != null) {
                    registrySsf = new RmiServerBindSocketFactory(rmiBindAddress);
                }
            }

            // Configure server socket factories
            if (rmiServerSSL) {
                serverCsf = new SslRMIClientSocketFactory();
                if (rmiBindAddress == null) {
                    serverSsf = new SslRMIServerSocketFactory(sslContext,
                            getEnabledCiphers(), getEnabledProtocols(),
                            getCertificateVerification() == CertificateVerification.REQUIRED);
                } else {
                    serverSsf = new SslRmiServerBindSocketFactory(sslContext,
                            getEnabledCiphers(), getEnabledProtocols(),
                            getCertificateVerification() == CertificateVerification.REQUIRED,
                            rmiBindAddress);
                }
            } else {
                if (rmiBindAddress != null) {
                    serverSsf = new RmiServerBindSocketFactory(rmiBindAddress);
                }
            }

            // By default, the registry will pick an address to listen on.
            // Setting this property overrides that and ensures it listens on
            // the configured address.
            if (rmiBindAddress != null) {
                System.setProperty("java.rmi.server.hostname", rmiBindAddress);
            }

            // Force the use of local ports if required
            if (useLocalPorts) {
                registryCsf = new RmiClientLocalhostSocketFactory(registryCsf);
                serverCsf = new RmiClientLocalhostSocketFactory(serverCsf);
            }

            env.put("jmx.remote.rmi.server.credential.types", new String[] {
                    String[].class.getName(),
                    String.class.getName() });

            // Populate the env properties used to create the server
            if (serverCsf != null) {
                env.put(RMIConnectorServer.RMI_CLIENT_SOCKET_FACTORY_ATTRIBUTE, serverCsf);
                env.put("com.sun.jndi.rmi.factory.socket", registryCsf);
            }
            if (serverSsf != null) {
                env.put(RMIConnectorServer.RMI_SERVER_SOCKET_FACTORY_ATTRIBUTE, serverSsf);
            }

            // Configure authentication
            if (authenticate) {
                env.put("jmx.remote.x.password.file", passwordFile);
                env.put("jmx.remote.x.access.file", accessFile);
                env.put("jmx.remote.x.login.config", loginModuleName);
            }

            // Create the Platform server
            csPlatform = createServer("Platform", rmiBindAddress, rmiRegistryPortPlatform,
                    rmiServerPortPlatform, env, registryCsf, registrySsf, serverCsf, serverSsf);

        } else if (Lifecycle.STOP_EVENT.equals(event.getType())) {
            destroyServer("Platform", csPlatform);
        }
    }


    private JMXConnectorServer createServer(String serverName,
            String bindAddress, int theRmiRegistryPort, int theRmiServerPort,
            Map<String,Object> theEnv,
            RMIClientSocketFactory registryCsf, RMIServerSocketFactory registrySsf,
            RMIClientSocketFactory serverCsf, RMIServerSocketFactory serverSsf) {

        if (bindAddress == null) {
            bindAddress = "localhost";
        }

        String url = "service:jmx:rmi://" + bindAddress;
        JMXServiceURL serviceUrl;
        try {
            serviceUrl = new JMXServiceURL(url);
        } catch (MalformedURLException e) {
            log.error(sm.getString("jmxRemoteLifecycleListener.invalidURL", serverName, url), e);
            return null;
        }

        RMIConnectorServer cs = null;
        try {
            RMIJRMPServerImpl server = new RMIJRMPServerImpl(
                    rmiServerPortPlatform, serverCsf, serverSsf, theEnv);
            cs = new RMIConnectorServer(serviceUrl, theEnv, server,
                    ManagementFactory.getPlatformMBeanServer());
            cs.start();
            Remote jmxServer = server.toStub();
            // Create the RMI registry
            try {
                /*
                 * JmxRegistry is registered as a side-effect of creation.
                 * This object is here so we can tell the IDE it is OK for it
                 * not to be used.
                 */
                @SuppressWarnings("unused")
                JmxRegistry unused = new JmxRegistry(theRmiRegistryPort, registryCsf, registrySsf, "jmxrmi", jmxServer);
            } catch (RemoteException e) {
                log.error(sm.getString(
                        "jmxRemoteLifecycleListener.createRegistryFailed",
                        serverName, Integer.toString(theRmiRegistryPort)), e);
                return null;
            }
            log.info(sm.getString("jmxRemoteLifecycleListener.start",
                    Integer.toString(theRmiRegistryPort),
                    Integer.toString(theRmiServerPort), serverName));
        } catch (IOException e) {
            log.error(sm.getString(
                    "jmxRemoteLifecycleListener.createServerFailed",
                    serverName), e);
        }
        return cs;
    }


    private void destroyServer(String serverName,
            JMXConnectorServer theConnectorServer) {
        if (theConnectorServer != null) {
            try {
                theConnectorServer.stop();
            } catch (IOException e) {
                log.error(sm.getString(
                        "jmxRemoteLifecycleListener.destroyServerFailed",
                        serverName),e);
            }
        }
    }


    public static class RmiClientLocalhostSocketFactory
            implements RMIClientSocketFactory, Serializable {

        private static final long serialVersionUID = 1L;

        private static final String FORCED_HOST = "localhost";

        private final RMIClientSocketFactory factory;

        public RmiClientLocalhostSocketFactory(RMIClientSocketFactory theFactory) {
            factory = theFactory;
        }

        @Override
        public Socket createSocket(String host, int port) throws IOException {
            if (factory == null) {
                return new Socket(FORCED_HOST, port);
            } else {
                return factory.createSocket(FORCED_HOST, port);
            }
        }
    }


    public static class RmiServerBindSocketFactory implements RMIServerSocketFactory {

        private final InetAddress bindAddress;

        public RmiServerBindSocketFactory(String address) {
            InetAddress bindAddress = null;
            try {
                bindAddress = InetAddress.getByName(address);
            } catch (UnknownHostException e) {
                log.error(sm.getString(
                        "jmxRemoteLifecycleListener.invalidRmiBindAddress", address), e);
                // bind address will be null which means any/all local addresses
                // which should be safe
            }
            this.bindAddress = bindAddress;
        }

        @Override
        public ServerSocket createServerSocket(int port) throws IOException  {
            return new ServerSocket(port, 0, bindAddress);
        }
    }


    public static class SslRmiServerBindSocketFactory extends SslRMIServerSocketFactory {

        private final InetAddress bindAddress;
        private final SSLContext sslContext;

        public SslRmiServerBindSocketFactory(SSLContext sslContext, String[] enabledCipherSuites,
                String[] enabledProtocols, boolean needClientAuth, String address) {
            super(sslContext, enabledCipherSuites, enabledProtocols, needClientAuth);
            this.sslContext = sslContext;
            InetAddress bindAddress = null;
            try {
                bindAddress = InetAddress.getByName(address);
            } catch (UnknownHostException e) {
                log.error(sm.getString(
                        "jmxRemoteLifecycleListener.invalidRmiBindAddress", address), e);
                // bind address will be null which means any/all local addresses
                // which should be safe
            }
            this.bindAddress = bindAddress;
        }

        @Override
        public ServerSocket createServerSocket(int port) throws IOException {
            SSLServerSocketFactory sslServerSocketFactory = (sslContext == null)
                    ? (SSLServerSocketFactory) SSLServerSocketFactory.getDefault()
                    : sslContext.getServerSocketFactory();
            SSLServerSocket sslServerSocket =
                    (SSLServerSocket) sslServerSocketFactory.createServerSocket(port, 0, bindAddress);
            if (getEnabledCipherSuites() != null) {
                sslServerSocket.setEnabledCipherSuites(getEnabledCipherSuites());
            }
            if (getEnabledProtocols() != null) {
                sslServerSocket.setEnabledProtocols(getEnabledProtocols());
            }
            sslServerSocket.setNeedClientAuth(getNeedClientAuth());
            return sslServerSocket;
        }

        // Super class defines hashCode() and equals(). Probably not used in
        // Tomcat but for safety, override them here.
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = super.hashCode();
            result = prime * result + ((bindAddress == null) ? 0 : bindAddress.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (!super.equals(obj))
                return false;
            if (getClass() != obj.getClass())
                return false;
            SslRmiServerBindSocketFactory other = (SslRmiServerBindSocketFactory) obj;
            if (bindAddress == null) {
                if (other.bindAddress != null)
                    return false;
            } else if (!bindAddress.equals(other.bindAddress))
                return false;
            return true;
        }
    }


    /*
     * Better to use the internal API than re-invent the wheel.
     */
    @SuppressWarnings("restriction")
    private static class JmxRegistry extends sun.rmi.registry.RegistryImpl {
        private static final long serialVersionUID = -3772054804656428217L;
        private final String jmxName;
        private final Remote jmxServer;
        public JmxRegistry(int port, RMIClientSocketFactory csf,
                RMIServerSocketFactory ssf, String jmxName, Remote jmxServer) throws RemoteException {
            super(port, csf, ssf);
            this.jmxName = jmxName;
            this.jmxServer = jmxServer;
        }
//        @Override
        public Remote lookup(String name)
                throws RemoteException, NotBoundException {
            return (jmxName.equals(name)) ? jmxServer : null;
        }
//        @Override
        public void bind(String name, Remote obj)
                throws RemoteException, AlreadyBoundException, AccessException {
        }
//        @Override
        public void unbind(String name)
                throws RemoteException, NotBoundException, AccessException {
        }
//        @Override
        public void rebind(String name, Remote obj)
                throws RemoteException, AccessException {
        }
//        @Override
        public String[] list() throws RemoteException {
            return new String[] { jmxName };
        }
    }

}
