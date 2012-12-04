/*
 * Copyright Evan Summers
 * 
 */
package crocserver.app;

import bizstat.entity.Contact;
import crocserver.gtalk.GtalkConnection;
import crocserver.httphandler.access.AccessHttpHandler;
import crocserver.httphandler.access.WebHandler;
import crocserver.httphandler.insecure.InsecureHttpHandler;
import crocserver.httphandler.persona.PersonaApi;
import crocserver.httphandler.secure.SecureHttpHandler;
import crocserver.httpserver.HttpExchangeInfo;
import crocserver.httpserver.HttpServerConfig;
import crocserver.storage.adminuser.AdminUser;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import org.h2.tools.Server;
import vellum.config.ConfigMap;
import vellum.config.ConfigParser;
import vellum.config.PropertiesMap;
import vellum.datatype.SimpleEntityCache;
import vellum.logr.Logr;
import vellum.logr.LogrFactory;
import vellum.logr.LogrLevel;
import vellum.storage.DataSourceConfig;
import vellum.storage.SimpleConnectionPool;
import vellum.util.Streams;
import vellum.util.Threads;
import crocserver.storage.schema.CrocSchema;
import crocserver.storage.common.CrocStorage;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.Date;
import vellum.exception.EnumException;
import vellum.httpserver.VellumHttpServer;
import vellum.httpserver.VellumHttpsServer;
import vellum.parameter.StringMap;
import vellum.security.DefaultKeyStores;
import vellum.security.GeneratedRsaKeyPair;
import vellum.util.DefaultDateFormats;

/**
 *
 * @author evan
 */
public class CrocApp {

    Logr logger = LogrFactory.getLogger(getClass());
    CrocStorage storage;
    DataSourceConfig dataSourceConfig;
    PropertiesMap configProperties;
    Thread serverThread;
    String confFileName;
    ConfigMap configMap;
    Server h2Server;
    VellumHttpServer httpServer;
    VellumHttpsServer publicHttpsServer;
    VellumHttpsServer privateHttpsServer;
    CrocTrustManager trustManager;
    GtalkConnection gtalkConnection;
    Contact adminContact;
    String serverKeyAlias = System.getProperty("serverKeyAlias");
    X509Certificate serverCert;
    GoogleApi googleApi;
    String serverUrl;
    String secureUrl;
    String serverName = "croc.linuxd.org";
    String homePage = "/bindex.html";
    WebHandler webHandler = new WebHandler(this);

    public void init() throws Exception {
        initConfig();
        sendShutdown();
        if (configProperties.getBoolean("startH2TcpServer")) {
            h2Server = Server.createTcpServer().start();
        }
        String adminContactName = configProperties.getString("adminContact");
        if (adminContactName != null) {
            adminContact = new Contact(configMap.get("Contact", adminContactName));
        }
        dataSourceConfig = new DataSourceConfig(configMap.get("DataSource",
                configProperties.getString("dataSource")).getProperties());
        storage = new CrocStorage(new SimpleEntityCache(), new SimpleConnectionPool(dataSourceConfig));
        storage.init();
        trustManager = new CrocTrustManager(this);
        trustManager.init();
        new CrocSchema(storage).verifySchema();
        String httpServerConfigName = configProperties.findString("httpServer");
        if (httpServerConfigName != null) {
            HttpServerConfig httpServerConfig = new HttpServerConfig(
                    configMap.find("HttpServer", httpServerConfigName).getProperties());
            if (httpServerConfig.isEnabled()) {
                httpServer = new VellumHttpServer(httpServerConfig);
            }
        }
        String publicHttpsServerConfigName = configProperties.getString("publicHttpsServer");
        if (publicHttpsServerConfigName != null) {
            if (false) {
                System.setProperty("java.protocol.handler.pkgs", "com.sun.net.ssl.internal.www.protocol");
                Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());
            }
            HttpServerConfig httpsServerConfig = new HttpServerConfig(
                    configMap.find("HttpsServer", publicHttpsServerConfigName).getProperties());
            if (httpsServerConfig.isEnabled()) {
                publicHttpsServer = new VellumHttpsServer(httpsServerConfig);
                publicHttpsServer.init(DefaultKeyStores.createSSLContext());
            }
        }
        String privateHttpsServerConfigName = configProperties.findString("privateHttpsServer");
        if (privateHttpsServerConfigName != null) {
            HttpServerConfig httpsServerConfig = new HttpServerConfig(
                    configMap.find("HttpsServer", privateHttpsServerConfigName).getProperties());
            if (httpsServerConfig.isEnabled()) {
                privateHttpsServer = new VellumHttpsServer(httpsServerConfig);
                privateHttpsServer.init(DefaultKeyStores.createSSLContext(trustManager));
            }
        }
        String gtalkConfigName = configProperties.getString("gtalk");
        if (gtalkConfigName != null) {
            PropertiesMap gtalkProps = configMap.find("Gtalk", gtalkConfigName).getProperties();
            if (gtalkProps.getBoolean("enabled", false)) {
                gtalkConnection = new GtalkConnection(gtalkProps);
            }
        }
        secureUrl = configProperties.getString("secureUrl");
        serverUrl = configProperties.getString("serverUrl");
        googleApi = new GoogleApi(serverUrl, serverUrl + "/oauth", configMap.get("GoogleApi", "default").getProperties());
        logger.info("googleApi", googleApi);
        webHandler.init();
    }

    public WebHandler getWebHandler() {
        return webHandler;
    }

    public String getServerName() {
        return serverName;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public String getSecureUrl() {
        return secureUrl;
    }

    public GoogleApi getGoogleApi() {
        return googleApi;
    }

    private void initConfig() throws Exception {
        serverCert = DefaultKeyStores.getCert(serverKeyAlias);
        confFileName = getString("conf");
        File confFile = new File(confFileName);
        logger.info("conf", confFileName, confFile);
        configMap = ConfigParser.newInstance(new FileInputStream(confFile));
        configProperties = configMap.find("Config", "default").getProperties();
        String logLevelName = configProperties.get("logLevel");
        if (logLevelName != null) {
            LogrFactory.setDefaultLevel(LogrLevel.valueOf(logLevelName));
        }
    }

    public void start() throws Exception {
        if (httpServer != null) {
            httpServer.start();
            httpServer.startContext("/", new InsecureHttpHandler(this));
            logger.info("HTTP server started");
        }
        if (publicHttpsServer != null) {
            publicHttpsServer.start();
            publicHttpsServer.startContext("/", new AccessHttpHandler(this));
            logger.info("public HTTPS secure server started");
        }
        if (privateHttpsServer != null) {
            privateHttpsServer.start();
            privateHttpsServer.startContext("/", new SecureHttpHandler(this));
            logger.info("private HTTPS secure server started");
        }
        if (gtalkConnection != null) {
            gtalkConnection.open();
        }
        if (configProperties.getBoolean("testPost", false)) {
            try {
                testPost();
                Threads.sleep(16000);
            } finally {
                stop();
            }
        }
    }

    public void sendShutdown() {
        String shutdownUrl = configProperties.getString("shutdownUrl");
        logger.info("sendShutdown", shutdownUrl);
        try {
            URL url = new URL(shutdownUrl);
            URLConnection connection = url.openConnection();
            String response = Streams.readString(connection.getInputStream());
            connection.getInputStream().close();
            logger.info(response);
        } catch (Exception e) {
            logger.warn(e.getMessage());
        }
    }

    private void testPost() throws IOException {
        URL url = new URL(configProperties.getString("testPostUrl"));
        URLConnection connection = url.openConnection();
        connection.setDoOutput(true);
        PrintWriter out = new PrintWriter(connection.getOutputStream());
        out.println("hello");
        out.close();
        String response = Streams.readString(connection.getInputStream());
        logger.info(response);
    }

    public void stop() throws Exception {
        if (httpServer != null) {
            httpServer.stop();
        }
        if (publicHttpsServer != null) {
            publicHttpsServer.stop();
        }
        if (privateHttpsServer != null) {
            privateHttpsServer.stop();
        }
        if (h2Server != null) {
            h2Server.stop();
        }
        if (gtalkConnection != null) {
            gtalkConnection.close();
        }
    }

    private String getString(String name) {
        String string = System.getProperty(name);
        if (string == null) {
            throw new RuntimeException(name);
        }
        return string;
    }

    public CrocStorage getStorage() {
        return storage;
    }

    public GtalkConnection getGtalkConnection() {
        return gtalkConnection;
    }

    public String getServerKeyAlias() {
        return serverKeyAlias;
    }

    public GeneratedRsaKeyPair generateSignedKeyPair(String subject) throws Exception {
        GeneratedRsaKeyPair keyPair = new GeneratedRsaKeyPair();
        keyPair.generate(subject, new Date(), 999);
        keyPair.sign(DefaultKeyStores.getPrivateKey(serverKeyAlias), serverCert);
        return keyPair;
    }

    public X509Certificate getServerCert() {
        return serverCert;
    }

    public void sendAdminGtalkMessage(String message) {
        logger.warn("notifyAdmin", message, adminContact);
        if (gtalkConnection != null && adminContact != null && adminContact.isEnabled() && adminContact.isGtalk()) {
            try {
                gtalkConnection.sendMessage(adminContact.getIm(), message);
            } catch (Exception e) {
                logger.warn(e, "sendAdminGtalkMessage", adminContact);
            }
        }
    }

    public void sendGtalkMessage(String im, String message) throws Exception {
        if (gtalkConnection != null && im != null && message != null) {
            try {
                gtalkConnection.sendMessage(adminContact.getIm(), message);
            } catch (Exception e) {
                logger.warn(e, "sendGtalkMessage", adminContact);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        try {
            CrocApp starter = new CrocApp();
            starter.init();
            starter.start();
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }

    public String getHomePage() {
        return homePage;
    }

    public AdminUser getUser(HttpExchangeInfo httpExchangeInfo, boolean auth) throws Exception {
        StringMap cookieMap = httpExchangeInfo.getCookieMap();
        String email = cookieMap.get("email");
        if (email == null) {
            throw new EnumException(CrocExceptionType.NO_COOKIE);
        } else if (email.isEmpty()) {
            throw new EnumException(CrocExceptionType.EXPIRED_COOKIE);
        } else {
            CrocCookie cookie = new CrocCookie(cookieMap);
            AdminUser user = storage.getUserStorage().get(cookie.getEmail());
            if (user.getLoginTime().getTime() != cookie.getLoginMillis()) {
                logger.warn("getUser cookie millis", DefaultDateFormats.dateFormat.format(user.getLoginTime()), 
                        DefaultDateFormats.formatTime(cookie.getLoginMillis()),
                        new Date(cookie.getLoginMillis()));
                throw new EnumException(CrocExceptionType.STALE_COOKIE);
            }
            if (auth) {
                googleApi.getUserInfo(cookie.getAccessToken());
            }
            return user;
        }
    }

    public GoogleUserInfo getGoogleUserInfo(HttpExchangeInfo httpExchangeInfo) throws Exception {
        StringMap cookieMap = httpExchangeInfo.getCookieMap();
        String accessToken = cookieMap.get("accessToken");
        return googleApi.getUserInfo(accessToken);
    }
}
