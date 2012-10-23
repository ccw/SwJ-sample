package com.sas.custom.listeners;

import com.sas.iquery.dataretrieval.QueryConnector;
import com.sas.iquery.execution2.ExecutionException;
import com.sas.services.discovery.ServiceTemplate;
import com.sas.services.logging.LoggerInterface;
import com.sas.services.logging.LoggingServiceInterface;
import com.sas.services.session.LockingException;
import com.sas.services.session.SessionContextInterface;
import com.sas.services.user.UserContextInterface;
import com.sas.storage.iquery.BusinessQueryAdapter;
import com.sas.storage.iquery.BusinessQueryToOLAPDataSetAdapter;
import com.sas.storage.jdbc.JDBCAdapter;
import com.sas.storage.jdbc.JDBCConnection;
import com.sas.storage.olap.OLAPException;
import com.sas.text.Message;
import com.sas.util.logging.GeneralizedDiscoveryService;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;
import java.rmi.RemoteException;
import java.util.*;

public class SessionBindingListener
        implements HttpSessionBindingListener {
    private static final String SESSION_BINDING_LISTENER = "sas_examples_session_binding_listener";
    private static final String BUSINESSQUERY_ADAPTER_CLOSE_EXCEPTION = "WARNING: An error occurred while closing Business Query adapter: \"{0}\"";
    private static final String UNKNOWN_ADAPTER_CLOSE_EXCEPTION = "WARNING: Unable to close instance of unexpected adapter class: \"{0}\"";
    private static final String JDBC_CONNECTION_CLOSE_EXCEPTION = "WARNING: An error occurred while closing JDBC connection: \"{0}\"";
    private static final String SESSIONCONTEXT_LOCK_EXCEPTION = "WARNING: An error occurred while attempting to lock the local SessionContext for: \"{0}\"";
    private static final String SESSIONCONTEXT_DESTROY_EXCEPTION = "WARNING: Error occurred during destruction of local SessionContext: \"{0}\"";
    private static final String SESSIONCONTEXT_LOCKED = "WARNING: Local SessionContext \"{0}\" was locked.  Unable to ensure SessionContext and its UserContext will be cleaned up.";
    private static final String USERCONTEXT_DESTROY_EXCEPTION = "WARNING: Error occurred during destruction of local UserContext for: \"{0}\"";
    private static final String REMOTE_SESSIONCONTEXT_LOCK_EXCEPTION = "WARNING: An error occurred while attempting to lock the remote SessionContext for: \"{0}\"";
    private static final String REMOTE_SESSIONCONTEXT_DESTROY_EXCEPTION = "WARNING: Error occurred during destruction of remote SessionContext: \"{0}\"";
    private static final String REMOTE_SESSIONCONTEXT_LOCKED = "WARNING: Remote SessionContext \"{0}\" was locked.  Unable to ensure SessionContext and its UserContext will be cleaned up.";
    private static final String REMOTE_USERCONTEXT_DESTROY_EXCEPTION = "WARNING: Error occurred during destruction of remote UserContext for: \"{0}\"";
    private static final String CLOSE_QUERY_CONNECTIONS_EXCEPTION = "WARNING: Error occurred closing query connections for local SessionContext \"{0}\"";
    private static final String LOG_EXPIRING_HTTP_SESSION = "Expiring HTTP session: {0}";
    private static final String LOG_CLOSING_JDBC_ADAPTER = "\tClosing JDBC adapter containing query: \"{0}\"";
    private static final String LOG_CLOSING_BQ_ADAPTER = "\tClosing Business Query adapter: \"{0}\"";
    private static final String LOG_CLOSING_JDBC_CONNECTION = "\tClosing JDBC connection to: \"{0}\"";
    private static final String LOG_DESTROYING_SESSIONCONTEXT = "\tDestroying local SessionContext: \"{0}\"";
    private static final String LOG_DESTROYING_USERCONTEXT = "\tDestroying local UserContext for: \"{0}\"";
    private static final String LOG_DESTROYING_REMOTE_SESSIONCONTEXT = "\tDestroying remote SessionContext: \"{0}\"";
    private static final String LOG_DESTROYING_REMOTE_USERCONTEXT = "\tDestroying remote UserContext for: \"{0}\"";
    List adapters = Collections.synchronizedList(new ArrayList());

    List jdbcConnections = Collections.synchronizedList(new ArrayList());

    List userContexts = Collections.synchronizedList(new ArrayList());

    List sessionContexts = Collections.synchronizedList(new ArrayList());

    List remoteUserContexts = Collections.synchronizedList(new ArrayList());

    List remoteSessionContexts = Collections.synchronizedList(new ArrayList());

    Map sessionContextLocks = Collections.synchronizedMap(new HashMap());

    private LoggerInterface logger = getLogger();

    public static SessionBindingListener getInstance(HttpSession paramHttpSession) {
        if (null != paramHttpSession) {
            synchronized (paramHttpSession) {
                SessionBindingListener localSessionBindingListener = (SessionBindingListener) paramHttpSession.getAttribute("sas_examples_session_binding_listener");
                if (null == localSessionBindingListener) {
                    localSessionBindingListener = new SessionBindingListener();
                    paramHttpSession.setAttribute("sas_examples_session_binding_listener", localSessionBindingListener);
                }
                return localSessionBindingListener;
            }
        }
        return new SessionBindingListener();
    }

    public void addAdapter(Object paramObject) {
        this.adapters.add(paramObject);
    }

    public void addJDBCConnection(JDBCConnection paramJDBCConnection) {
        this.jdbcConnections.add(paramJDBCConnection);
    }

    public void addUserContext(UserContextInterface paramUserContextInterface) {
        this.userContexts.add(paramUserContextInterface);
    }

    public void addSessionContext(SessionContextInterface paramSessionContextInterface, String paramString) {
        this.sessionContexts.add(paramSessionContextInterface);
        try {
            this.sessionContextLocks.put(paramSessionContextInterface, paramSessionContextInterface.lock(paramString));
        } catch (RemoteException localRemoteException) {
            logWarn(Message.format("WARNING: An error occurred while attempting to lock the local SessionContext for: \"{0}\"", paramString), localRemoteException);
        }
    }

    public void addRemoteUserContext(UserContextInterface paramUserContextInterface) {
        this.remoteUserContexts.add(paramUserContextInterface);
    }

    public void addRemoteSessionContext(SessionContextInterface paramSessionContextInterface, String paramString) {
        this.remoteSessionContexts.add(paramSessionContextInterface);
        try {
            this.sessionContextLocks.put(paramSessionContextInterface, paramSessionContextInterface.lock(paramString));
        } catch (RemoteException localRemoteException) {
            logWarn(Message.format("WARNING: An error occurred while attempting to lock the remote SessionContext for: \"{0}\"", paramString), localRemoteException);
        }
    }

    public void valueBound(HttpSessionBindingEvent paramHttpSessionBindingEvent) {
    }

    public void valueUnbound(HttpSessionBindingEvent paramHttpSessionBindingEvent) {
        if (isInfoLoggingEnabled())
            logInfo(Message.format("Expiring HTTP session: {0}", paramHttpSessionBindingEvent.getSession().getId()));
        Iterator localIterator;
        Object localObject1;
        if (!this.adapters.isEmpty()) {
            localIterator = this.adapters.iterator();
            while (localIterator.hasNext()) {
                localObject1 = localIterator.next();
                Object localObject2;
                if ((localObject1 instanceof JDBCAdapter)) {
                    localObject2 = (JDBCAdapter) localObject1;
                    if (isDebugLoggingEnabled()) {
                        logDebug(Message.format("\tClosing JDBC adapter containing query: \"{0}\"", ((JDBCAdapter) localObject2).getQueryStatement()));
                    }
                    ((JDBCAdapter) localObject2).close();
                } else if ((localObject1 instanceof BusinessQueryAdapter)) {
                    localObject2 = (BusinessQueryAdapter) localObject1;
                    if (isDebugLoggingEnabled())
                        logDebug(Message.format("\tClosing Business Query adapter: \"{0}\"", localObject2));
                    try {
                        ((BusinessQueryAdapter) localObject2).close();
                    } catch (ExecutionException localExecutionException) {
                        logWarn(Message.format("WARNING: An error occurred while closing Business Query adapter: \"{0}\"", localObject2), localExecutionException);
                    }
                } else if ((localObject1 instanceof BusinessQueryToOLAPDataSetAdapter)) {
                    localObject2 = (BusinessQueryToOLAPDataSetAdapter) localObject1;
                    if (isDebugLoggingEnabled())
                        logDebug(Message.format("\tClosing Business Query adapter: \"{0}\"", localObject2));
                    try {
                        ((BusinessQueryToOLAPDataSetAdapter) localObject2).close();
                    } catch (OLAPException localOLAPException) {
                        logWarn(Message.format("WARNING: An error occurred while closing Business Query adapter: \"{0}\"", localObject2), localOLAPException);
                    }
                } else {
                    logWarn(Message.format("WARNING: Unable to close instance of unexpected adapter class: \"{0}\"", localObject1));
                }
            }
            this.adapters.clear();
        }

        if (!this.jdbcConnections.isEmpty()) {
            localIterator = this.jdbcConnections.iterator();
            while (localIterator.hasNext()) {
                localObject1 = (JDBCConnection) localIterator.next();
                if (isDebugLoggingEnabled())
                    logDebug(Message.format("\tClosing JDBC connection to: \"{0}\"", ((JDBCConnection) localObject1).getDatabaseURL()));
                try {
                    ((JDBCConnection) localObject1).close();
                } catch (Throwable localThrowable1) {
                    logWarn(Message.format("WARNING: An error occurred while closing JDBC connection: \"{0}\"", ((JDBCConnection) localObject1).getDatabaseURL()), localThrowable1);
                }
            }
            this.jdbcConnections.clear();
        }

        if (!this.remoteSessionContexts.isEmpty()) {
            localIterator = this.remoteSessionContexts.iterator();
            while (localIterator.hasNext()) {
                localObject1 = (SessionContextInterface) localIterator.next();
                Object localObject3;
                if (isDebugLoggingEnabled()) {
                    localObject3 = getSessionContextEntityKey((SessionContextInterface) localObject1);
                    logDebug(Message.format("\tDestroying remote SessionContext: \"{0}\"", localObject3));
                }

                try {
                    localObject3 = ((SessionContextInterface) localObject1).getUserContext();
                    this.remoteUserContexts.remove(localObject3);

                    ((SessionContextInterface) localObject1).unlock(this.sessionContextLocks.get(localObject1));
                    ((SessionContextInterface) localObject1).destroy();
                } catch (LockingException localLockingException1) {
                    if (isInfoLoggingEnabled()) {
                        String str1 = getSessionContextEntityKey((SessionContextInterface) localObject1);
                        logInfo(Message.format("WARNING: Remote SessionContext \"{0}\" was locked.  Unable to ensure SessionContext and its UserContext will be cleaned up.", str1));
                    }
                } catch (Throwable localThrowable2) {
                    String str1 = getSessionContextEntityKey((SessionContextInterface) localObject1);
                    logWarn(Message.format("WARNING: Error occurred during destruction of remote SessionContext: \"{0}\"", str1), localThrowable2);
                }
            }
            this.remoteSessionContexts.clear();
        }

        if (!this.remoteUserContexts.isEmpty()) {
            localIterator = this.remoteUserContexts.iterator();
            while (localIterator.hasNext()) {
                localObject1 = (UserContextInterface) localIterator.next();
                if (isDebugLoggingEnabled())
                    logDebug(Message.format("\tDestroying remote UserContext for: \"{0}\"", getUserContextName((UserContextInterface) localObject1)));
                try {
                    if (!((UserContextInterface) localObject1).isDestroyed())
                        ((UserContextInterface) localObject1).destroy();
                } catch (Throwable localThrowable3) {
                    logWarn(Message.format("WARNING: Error occurred during destruction of remote UserContext for: \"{0}\"", getUserContextName((UserContextInterface) localObject1)), localThrowable3);
                }
            }
            this.remoteUserContexts.clear();
        }

        if (!this.sessionContexts.isEmpty()) {
            localIterator = this.sessionContexts.iterator();
            while (localIterator.hasNext()) {
                localObject1 = (SessionContextInterface) localIterator.next();
                if (isDebugLoggingEnabled()) {
                    String localObject4 = getSessionContextEntityKey((SessionContextInterface) localObject1);
                    logDebug(Message.format("\tDestroying local SessionContext: \"{0}\"", localObject4));
                }
                Object localObject4 = new QueryConnector();
                String str2;
                try {
                    ((QueryConnector) localObject4).closeResources((SessionContextInterface) localObject1, 61440);
                } catch (Throwable localThrowable5) {
                    str2 = getSessionContextEntityKey((SessionContextInterface) localObject1);
                    logWarn(Message.format("WARNING: Error occurred closing query connections for local SessionContext \"{0}\"", str2), localThrowable5);
                }

                try {
                    UserContextInterface localUserContextInterface = ((SessionContextInterface) localObject1).getUserContext();
                    this.userContexts.remove(localUserContextInterface);

                    ((SessionContextInterface) localObject1).unlock(this.sessionContextLocks.get(localObject1));
                    ((SessionContextInterface) localObject1).destroy();
                } catch (LockingException localLockingException2) {
                    if (isInfoLoggingEnabled()) {
                        str2 = getSessionContextEntityKey((SessionContextInterface) localObject1);
                        logInfo(Message.format("WARNING: Local SessionContext \"{0}\" was locked.  Unable to ensure SessionContext and its UserContext will be cleaned up.", str2));
                    }
                } catch (Throwable localThrowable6) {
                    str2 = getSessionContextEntityKey((SessionContextInterface) localObject1);
                    logWarn(Message.format("WARNING: Error occurred during destruction of local SessionContext: \"{0}\"", str2), localThrowable6);
                }
            }
            this.sessionContexts.clear();
        }

        if (!this.userContexts.isEmpty()) {
            localIterator = this.userContexts.iterator();
            while (localIterator.hasNext()) {
                localObject1 = (UserContextInterface) localIterator.next();
                if (isDebugLoggingEnabled())
                    logDebug(Message.format("\tDestroying local UserContext for: \"{0}\"", getUserContextName((UserContextInterface) localObject1)));
                try {
                    if (!((UserContextInterface) localObject1).isDestroyed())
                        ((UserContextInterface) localObject1).destroy();
                } catch (Throwable localThrowable4) {
                    logWarn(Message.format("WARNING: Error occurred during destruction of local UserContext for: \"{0}\"", getUserContextName((UserContextInterface) localObject1)), localThrowable4);
                }
            }
            this.userContexts.clear();
        }
    }

    private String getSessionContextEntityKey(SessionContextInterface paramSessionContextInterface) {
        String str = "<unknown>";
        try {
            str = paramSessionContextInterface.getEntityKey();
        } catch (Throwable localThrowable) {
        }
        return str;
    }

    private String getUserContextName(UserContextInterface paramUserContextInterface) {
        String str = "<unknown>";
        try {
            str = paramUserContextInterface.getName();
        } catch (Throwable localThrowable) {
        }
        return str;
    }

    private static LoggerInterface getLogger() {
        try {
            LoggingServiceInterface localLoggingServiceInterface = (LoggingServiceInterface) GeneralizedDiscoveryService.findDiscoveryService().findService(new ServiceTemplate(new Class[]{LoggingServiceInterface.class}));

            return localLoggingServiceInterface.getLogger(SessionBindingListener.class.getName());
        } catch (Throwable localThrowable) {
        }
        return null;
    }

    public boolean isDebugLoggingEnabled() {
        if (null != this.logger) return this.logger.isDebugEnabled();
        return false;
    }

    public void logDebug(String paramString) {
        if (null != this.logger)
            this.logger.debug(paramString);
    }

    public boolean isInfoLoggingEnabled() {
        if (null != this.logger) return this.logger.isDebugEnabled();
        return false;
    }

    public void logInfo(String paramString) {
        if (null != this.logger)
            this.logger.info(paramString);
    }

    public void logWarn(String paramString) {
        if (null != this.logger)
            this.logger.warn(paramString);
    }

    public void logWarn(String paramString, Throwable paramThrowable) {
        if (null != this.logger)
            this.logger.warn(paramString, paramThrowable);
    }
}