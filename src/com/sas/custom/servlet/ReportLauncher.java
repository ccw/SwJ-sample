package com.sas.custom.servlet;

import com.sas.SpecialValues;
import com.sas.custom.listeners.SessionBindingListener;
import com.sas.framework.config.ConfigurationServiceInterface;
import com.sas.report.models.ReportUtils;
import com.sas.report.models.prompteditem.PromptInformation;
import com.sas.services.discovery.DiscoveryService;
import com.sas.services.discovery.LocalDiscoveryServiceInterface;
import com.sas.services.discovery.ServiceTemplate;
import com.sas.services.session.SessionContextInterface;
import com.sas.services.session.SessionServiceInterface;
import com.sas.services.user.UserContextInterface;
import com.sas.services.user.UserServiceInterface;
import com.sas.svcs.authentication.client.AuthenticationException;
import com.sas.svcs.authentication.client.AuthenticationServiceInterface;
import com.sas.svcs.authentication.client.SecurityContext;
import com.sas.svcs.webapp.servlet.SecurityContextSessionBindingListener;
import com.sas.webapp.contextsharing.WebappContextParams;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Date;
import java.util.*;

public class ReportLauncher extends HttpServlet {
    private static final Logger log = Logger.getLogger(ReportLauncher.class);
    private static final long serialVersionUID = 1L;
    private static final String WRS_REPORT_NAME = "_report";
    private static final String ACCEPT_MISSING_PROMPTS_VALUES = "_useDefaults";
    private static final String SAS_LOCAL_SESSION_CONTEXT = "sas_localSessionContext_ReportLauncher";
    private static final String LOG_PROPERTIES_FILE = "/WEB-INF/conf/log4j.properties";
    private static final String DOMAIN_KEY = "metadata-domain";
    private static final String USER_KEY = "metadata-userid";
    private static final String PASSWORD_KEY = "metadata-password";
    private static final String VIEWER_KEY = "viewer-uri";
    private static final String CONFIG_APPLICATION_NAME = "application-name";

    private static final String DOMAIN_DEFAULT = "DefaultAuth";
    private static final String VIEWER_DEFAULT = "/SASWebReportStudio";

    private UserContextInterface secureUC;
    private ConfigurationServiceInterface configurationService;
    private AuthenticationServiceInterface authService;
    private SessionServiceInterface sessionService;

    private String domain;
    private String username;
    private String password;
    private String viewer;

    public ReportLauncher() {
        this.secureUC = null;
        this.authService = null;
        this.sessionService = null;
        this.username = null;
        this.password = null;
    }

    public void setSecureUC(UserContextInterface paramUserContextInterface) {
        try {
            this.secureUC = paramUserContextInterface;
            log.debug(new StringBuilder().append("*-*-*-* Setting OMRID securedUser to user:").append(this.secureUC.getName()).toString());
        } catch (Exception localException) {
            log.error("Couldn't enableLocalAdminMode for admin UC", localException);
        }
    }

    public void setAuthService(AuthenticationServiceInterface paramAuthenticationServiceInterface) {
        this.authService = paramAuthenticationServiceInterface;
    }

    public void setSessionService(SessionServiceInterface paramSessionServiceInterface) {
        this.sessionService = paramSessionServiceInterface;
    }

    public void setConfigurationService(ConfigurationServiceInterface paramConfigurationServiceInterface) {
        this.configurationService = paramConfigurationServiceInterface;
    }

    public void init() {
        initializeLogger();

        ServletContext localServletContext = getServletContext();
        String str = localServletContext.getInitParameter("application-name");

        new ReportLauncherInit(this, str, this.configurationService).start();
        log.debug("SAS Report Launcher 4.3 is waiting for WIP to initialize before proceeding.");
    }

    protected void completeInit() {
        ServletContext localServletContext = getServletContext();
        try {
            String str = localServletContext.getInitParameter("application-name");
            Properties localProperties = this.configurationService.getSettings(str);

            this.domain = localProperties.getProperty("metadata-domain");
            this.username = localProperties.getProperty("metadata-userid");
            this.password = localProperties.getProperty("metadata-password");
            this.viewer = localProperties.getProperty("viewer-uri");

            if (domain == null || domain.trim().length() == 0) {
                domain = DOMAIN_DEFAULT;
            }

            if (viewer == null || viewer.trim().length() == 0) {
                viewer = VIEWER_DEFAULT;
            }

            log.debug(new StringBuilder().append("Launcher properties loaded [user:").append(this.username)
                                         .append(", password:").append(password).append(", domain:").append(this.domain)
                                         .append(", viewer: ").append(this.viewer).append("]").toString());

            log.debug("SAS Report Launcher initialization completed successfully.");
        } catch (Exception localException) {
            log.error("Error while reading launcher properties file.", localException);
            localException.printStackTrace();
        }
    }

    public void doPost(HttpServletRequest paramHttpServletRequest, HttpServletResponse paramHttpServletResponse) throws ServletException, IOException {
        doGet(paramHttpServletRequest, paramHttpServletResponse);
    }

    public void doGet(HttpServletRequest paramHttpServletRequest, HttpServletResponse paramHttpServletResponse) throws ServletException, IOException {
        HttpSession localHttpSession = paramHttpServletRequest.getSession();

        synchronized (localHttpSession) {
            SessionContextInterface localSessionContextInterface1 = (SessionContextInterface) localHttpSession.getAttribute("sas_localSessionContext_ReportLauncher");
            try {
                if (localSessionContextInterface1 == null) {
                    LocalDiscoveryServiceInterface localLocalDiscoveryServiceInterface = DiscoveryService.defaultInstance();

                    Object localObject1 = (UserServiceInterface) localLocalDiscoveryServiceInterface.findService(new ServiceTemplate(new Class[]{UserServiceInterface.class}));
                    log.debug(new StringBuilder().append("To verify with given configurations [user:").append(this.username)
                                                 .append(", password:").append(password).append(", domain:").append(this.domain)
                                                 .append(", viewer: ").append(this.viewer).append("]").toString());
                    Object localObject2 = ((UserServiceInterface) localObject1).newUser(this.username, this.password, this.domain);

                    if (localHttpSession != null) {
                        SessionBindingListener.getInstance(localHttpSession).addUserContext((UserContextInterface) localObject2);
                    }

                    Object localObject3 = (SessionServiceInterface) localLocalDiscoveryServiceInterface.findService(new ServiceTemplate(new Class[]{SessionServiceInterface.class}));

                    localSessionContextInterface1 = ((SessionServiceInterface) localObject3).newSessionContext((UserContextInterface) localObject2);
                    ((UserContextInterface) localObject2).setSessionContext(localSessionContextInterface1);
                    if (localHttpSession != null) {
                        localHttpSession.setAttribute("sas_localSessionContext_ReportLauncher", localSessionContextInterface1);
                        log.debug(new StringBuilder().append("Local session created for user ").append(this.username).append(" - session key=").append(localSessionContextInterface1.getEntityKey()).toString());
                        SessionBindingListener.getInstance(localHttpSession).addSessionContext(localSessionContextInterface1, "ReportViewerLauncher_lock");
                    }
                }
            } catch (Exception localException1) {
                showErrorMessage(paramHttpServletRequest, paramHttpServletResponse, localException1.getMessage());
                localException1.printStackTrace();
                return;
            }
            if (localSessionContextInterface1 != null) {
                SessionContextInterface localSessionContextInterface2 = null;
                try {
                    Object localObject1 = null;

                    String sessionId = paramHttpServletRequest.getParameter("saspfs_sessionid");
                    if ((sessionId != null) && (sessionId.length() > 1)) {
                        localObject1 = new SecurityContext(sessionId);
                        localObject1 = this.authService.reconnect((SecurityContext) localObject1);
                        if (localObject1 == null) {
                            showErrorMessage(paramHttpServletRequest, paramHttpServletResponse, new StringBuilder().append("Invalid session id '").append(sessionId).append("' passed. Authentication failed.").toString());
                            return;
                        }
                    } else if ((this.domain == null) || ("".equals(this.domain))) {
                        log.debug(new StringBuilder().append("To auth with [user:").append(this.username)
                                                     .append(", password:").append(password)
                                                     .append(", domain:").append(this.domain).append("]").toString());
                        localObject1 = this.authService.logon(this.username, this.password);
                    } else {
                        log.debug(new StringBuilder().append("To auth with [user:").append(this.username)
                                                     .append(", password:").append(password)
                                                     .append(", domain:").append(this.domain).append("]").toString());
                        localObject1 = this.authService.logon(this.username, this.password, this.domain);
                    }
                    localHttpSession.setAttribute("waf_security_context_listener", new SecurityContextSessionBindingListener((SecurityContext) localObject1, this.authService));
                    localHttpSession.setAttribute("sas.framework.SessionEntityKey", ((SecurityContext) localObject1).getKey());
                    localHttpSession.setAttribute("waf_security", localObject1);
                    localSessionContextInterface2 = this.sessionService.getSessionContext(this.secureUC, ((SecurityContext) localObject1).getKey());
                } catch (AuthenticationException localAuthenticationException) {
                    showErrorMessage(paramHttpServletRequest, paramHttpServletResponse, new StringBuilder().append("Error while authenticating user: ").append(localAuthenticationException.getMessage()).toString());
                    localAuthenticationException.printStackTrace();
                    return;
                }
                try {
                    String str1 = paramHttpServletRequest.getParameter("_report");
                    if (str1 == null)
                        throw new Exception("Parameter '_report' not found. Please specify the URL of the report you want to view.");
                    log.debug(new StringBuilder().append("Received open-report request for: ").append(str1).toString());

                    boolean bool = "true".equalsIgnoreCase(paramHttpServletRequest.getParameter("_useDefaults"));

                    String str2 = !this.viewer.endsWith("/") ? "/" : "";
                    String str3 = new StringBuilder().append(this.viewer).append(str2).append("logoff.do").toString();
                    String str4 = new StringBuilder().append(this.viewer).append(str2).append("main.do").toString();

                    PromptInformation[] arrayOfPromptInformation = null;
                    try {
                        arrayOfPromptInformation = ReportUtils.getPromptInformation(str1, localSessionContextInterface1);
                    } catch (Exception localException3) {
                        showErrorMessage(paramHttpServletRequest, paramHttpServletResponse, localException3.getMessage());
                        localException3.printStackTrace();
                        return;
                    }

                    HashMap localHashMap = new HashMap();

                    if (arrayOfPromptInformation != null) {
                        for (int i = 0; i < arrayOfPromptInformation.length; i++) {
                            Object localObject4 = arrayOfPromptInformation[i];
                            Object localObject5 = ((PromptInformation) localObject4).getLabel();
                            int j = ((PromptInformation) localObject4).getPromptDefinition().getPromptDataType().getSQLType();

                            if (((PromptInformation) localObject4).getPromptDefinition().isDefaultValueSet()) {
                                Object localObject6 = ((PromptInformation) localObject4).getPromptDefinition().getDefaultValue();
                                if ((localObject6 instanceof ArrayList)) {
                                    Object localObject7 = (ArrayList) localObject6;
                                    for (int k = 0; k < ((ArrayList) localObject7).size(); k++)
                                        log.debug(new StringBuilder().append("Default value is: ").append(((ArrayList) localObject7).get(k).getClass()).toString());
                                }
                                log.debug(new StringBuilder().append("Default value is: ").append(localObject6.getClass()).toString());
                            } else {
                                log.debug("No default value specified.");
                            }

                            log.debug(new StringBuilder().append("Prompt '").append((String) localObject5).append("' sqlType=").append(j).toString());
                            String[] paramValues = paramHttpServletRequest.getParameterValues((String) localObject5);
                            if ((paramValues == null) || (paramValues.length == 0)) {
                                if ((!((PromptInformation) localObject4).getPromptDefinition().isDefaultValueSet()) && (!bool)) {
                                    throw new Exception(new StringBuilder().append("Parameter '").append((String) localObject5).append("' not specified and no default value found.\nPlease specify the values for prompt '").append((String) localObject5).append("'!").toString());
                                }

                                log.debug(new StringBuilder().append("No value for prompt '").append((String) localObject5).append("' passed but default value found.").toString());
                            } else {
                                try {
                                    if (j == 12) {
                                        Object localObject7 = new ArrayList();
                                        for (int k = 0; k < paramValues.length; k++) {
                                            if ("<ALL>".equals(paramValues[k])) {
                                                ((ArrayList) localObject7).add(SpecialValues.ALL.toString());
                                                log.debug(new StringBuilder().append("SpecialValues value is: ").append(SpecialValues.ALL.toString()).toString());
                                            } else if ("<OTHER>".equals(paramValues[k])) {
                                                ((ArrayList) localObject7).add(SpecialValues.OTHER);
                                            } else {
                                                ((ArrayList) localObject7).add(paramValues[k]);
                                            }
                                        }
                                        localHashMap.put(((PromptInformation) localObject4).getIDString(), localObject7);
                                    } else if (j == 2) {
                                        Object localObject7 = new Double[paramValues.length];
                                        for (int k = 0; k < paramValues.length; k++)
                                            ((Double[]) (Double[]) localObject7)[k] = Double.valueOf(paramValues[k]);
                                        localHashMap.put(((PromptInformation) localObject4).getIDString(), localObject7);
                                    } else if (j == 91) {
                                        Object localObject7 = new Date[paramValues.length];
                                        for (int k = 0; k < paramValues.length; k++)
                                            ((Date[]) (Date[]) localObject7)[k] = Date.valueOf(paramValues[k]);
                                        localHashMap.put(((PromptInformation) localObject4).getIDString(), localObject7);
                                    } else if (j == 2000) {
                                        localHashMap.put(((PromptInformation) localObject4).getIDString(), paramValues);
                                    } else {
                                        log.debug(new StringBuilder().append("SpecialValues value is: ").append(SpecialValues.ALL.toString()).toString());
                                    }
                                } catch (Exception localException5) {
                                    throw new Exception("Invalid parameter values specified. Please check the parameter type (character, numeric, etc) and try again.\nNote, that date values need to be specified in the format yyyy-dd-mm.");
                                }
                            }
                        }
                    }
                    log.debug(new StringBuilder().append("Submitting ").append(localHashMap.size()).append(" prompt values to the report.").toString());
                    try {
                        ArrayList localArrayList = new ArrayList();
                        localArrayList.add(buildReturnHttpUrl(paramHttpServletRequest, "logoff.do", true));
                        Object localObject4 = new ArrayList();
                        ((List) localObject4).add("Return");
                        validate(localArrayList, (List) localObject4);

                        Object localObject5 = getWRSDirective();
                        String str5 = ((WRSDirective) localObject5).getUrl();
                        log.debug(new StringBuilder().append("Using WRS directive: ").append(str5).toString());
                        Object localObject6 = new WebappContextParams(localSessionContextInterface2, str5);
                        ((WebappContextParams) localObject6).setParamRequestType("saspfs_request_type_processentity");
                        ((WebappContextParams) localObject6).setParamRequestAction("saspfs_request_action_displayentity");

                        ((WebappContextParams) localObject6).setParamRequestPathUrl(str1);

                        ((WebappContextParams) localObject6).setParamRequestSource("IDPortal");
                        ((WebappContextParams) localObject6).setParamRequestAuthDomain(this.domain);

                        ((WebappContextParams) localObject6).setParamRequestLogoffurl(str3);
                        ((WebappContextParams) localObject6).setParamRequestTimeoutBackurl(str4);

                        Map localMap = ((WebappContextParams) localObject6).getRequestParams();
                        localMap.put("saspfs_request_report_prompts", localHashMap);

                        String str6 = buildReturnHttpUrl(paramHttpServletRequest, ((WebappContextParams) localObject6).getURL(), false);
                        log.debug(new StringBuilder().append("Redirecting request to WRS: ").append(str6).toString());
                        paramHttpServletResponse.sendRedirect(str6);
                    } catch (Exception localException4) {
                        showErrorMessage(paramHttpServletRequest, paramHttpServletResponse, new StringBuilder().append("Problem creating parameters object: ").append(localException4.getMessage()).toString());
                        localException4.printStackTrace();
                    }
                } catch (Exception localException2) {
                    showErrorMessage(paramHttpServletRequest, paramHttpServletResponse, localException2.getMessage());
                    localException2.printStackTrace();
                }
                return;
            }
            showErrorMessage(paramHttpServletRequest, paramHttpServletResponse, "No guest local session established. Check server configuration.");
            return;
        }
    }

    public static String buildReturnHttpUrl(HttpServletRequest paramHttpServletRequest, String paramString, boolean paramBoolean) {
        StringBuilder localStringBuilder = new StringBuilder(100);
        if ((!paramBoolean) && ((paramString.length() <= 3) || (!"http".equalsIgnoreCase(paramString.substring(0, 4))))) {
            localStringBuilder.append(paramHttpServletRequest.getScheme()).append("://");
            localStringBuilder.append(paramHttpServletRequest.getServerName()).append(':').append(paramHttpServletRequest.getServerPort());
            localStringBuilder.append(paramHttpServletRequest.getContextPath());
            localStringBuilder.append('/');
        }

        localStringBuilder.append(paramString);
        return localStringBuilder.toString();
    }

    protected WRSDirective getWRSDirective() {
        return new WRSDirective();
    }

    protected void makeAdjustments(WebappContextParams paramWebappContextParams) {
    }

    protected void validate(List<String> paramList1, List<String> paramList2) throws IllegalArgumentException {
        int i = paramList1 == null ? 0 : paramList1.size();
        int j = paramList2 == null ? 0 : paramList2.size();
        if (i != j) throw new IllegalArgumentException("Return URLs and return Labels list must be the same size");
    }

    private void showErrorMessage(HttpServletRequest paramHttpServletRequest, HttpServletResponse paramHttpServletResponse, String paramString) {
        paramHttpServletRequest.getSession().setAttribute("ERROR_MSG", paramString);
        RequestDispatcher localRequestDispatcher = getServletContext().getRequestDispatcher("/index.jsp");
        try {
            localRequestDispatcher.forward(paramHttpServletRequest, paramHttpServletResponse);
        } catch (ServletException localServletException) {
            localServletException.printStackTrace();
        } catch (IOException localIOException) {
            localIOException.printStackTrace();
        }
    }

    private void initializeLogger() {
        Properties localProperties = new Properties();
        try {
            localProperties.load(new FileInputStream(new StringBuilder().append(getServletContext().getRealPath("/")).append("/WEB-INF/conf/log4j.properties").toString()));
            PropertyConfigurator.configure(localProperties);
            log.info("Logging initialized.");
        } catch (IOException localIOException) {
            localIOException.printStackTrace();
        }
    }

    private class ReportLauncherInit extends Thread {
        private String compName;
        private ConfigurationServiceInterface configurationService;
        private ReportLauncher servlet;

        public ReportLauncherInit(ReportLauncher paramString, String paramConfigurationServiceInterface, ConfigurationServiceInterface arg4) {
            this.servlet = paramString;
            this.compName = paramConfigurationServiceInterface;
            this.configurationService = arg4;
        }

        public void run() {
            try {
                Properties localProperties = null;
                while ((localProperties == null) || (localProperties.isEmpty()))
                    try {
                        localProperties = this.configurationService.getSettings(this.compName);
                    } catch (Exception localException1) {
                        ReportLauncher.log.debug("SAS Report Launcher is waiting for WIP to initialize before proceeding.");

                        Thread.sleep(10000L);
                    }
                try {
                    if ((localProperties == null) || (localProperties.isEmpty())) {
                        throw new Exception("SAS Report Launcher application properties were not initialized properly: " + localProperties);
                    }
                    this.servlet.completeInit();
                } catch (Exception localException2) {
                    ReportLauncher.log.error("EventGenerator initialization failed.", localException2);
                }
            } catch (InterruptedException localInterruptedException) {
                System.out.println("WaitForWip interrupted...");
                return;
            }
        }
    }
}