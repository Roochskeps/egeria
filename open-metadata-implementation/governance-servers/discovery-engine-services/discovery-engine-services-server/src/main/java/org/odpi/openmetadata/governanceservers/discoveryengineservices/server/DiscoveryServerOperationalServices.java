/* SPDX-License-Identifier: Apache 2.0 */
/* Copyright Contributors to the ODPi Egeria project. */
package org.odpi.openmetadata.governanceservers.discoveryengineservices.server;

import org.odpi.openmetadata.accessservices.discoveryengine.client.DiscoveryConfigurationClient;
import org.odpi.openmetadata.accessservices.discoveryengine.client.DiscoveryEngineClient;
import org.odpi.openmetadata.adminservices.configuration.properties.DiscoveryEngineServicesConfig;
import org.odpi.openmetadata.adminservices.configuration.registration.GovernanceServicesDescription;
import org.odpi.openmetadata.adminservices.ffdc.exception.OMAGConfigurationErrorException;
import org.odpi.openmetadata.commonservices.odf.metadatamanagement.client.ODFRESTClient;
import org.odpi.openmetadata.frameworks.connectors.ffdc.*;
import org.odpi.openmetadata.governanceservers.discoveryengineservices.handlers.DiscoveryConfigurationRefreshHandler;
import org.odpi.openmetadata.governanceservers.discoveryengineservices.auditlog.DiscoveryEngineServicesAuditCode;
import org.odpi.openmetadata.governanceservers.discoveryengineservices.ffdc.DiscoveryEngineServicesErrorCode;
import org.odpi.openmetadata.governanceservers.discoveryengineservices.handlers.DiscoveryEngineHandler;
import org.odpi.openmetadata.repositoryservices.auditlog.OMRSAuditLog;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * DiscoveryServerOperationalServices is responsible for controlling the startup and shutdown of
 * a discovery server instance.  It is passed the discovery server configuration. This configuration provides
 * bootstrap information to connect to an open metadata repository to retrieve detailed information
 * about the discovery engines that should be hosted in this discovery server
 */
public class DiscoveryServerOperationalServices
{
    private String                  localServerName;               /* Initialized in constructor */
    private String                  localServerUserId;             /* Initialized in constructor */
    private String                  localServerPassword;           /* Initialized in constructor */
    private int                     maxPageSize;                   /* Initialized in constructor */

    private DiscoveryServerInstance discoveryServerInstance = null;
    private OMRSAuditLog            auditLog                = null;


    /**
     * Constructor used at server startup.
     *
     * @param localServerName name of the local server
     * @param localServerUserId user id for this server to use if sending REST requests and
     *                          processing inbound messages.
     * @param localServerPassword password for this server to use if sending REST requests.
     * @param maxPageSize maximum number of records that can be requested on the pageSize parameter
     */
    public DiscoveryServerOperationalServices(String                   localServerName,
                                              String                   localServerUserId,
                                              String                   localServerPassword,
                                              int                      maxPageSize)
    {
        this.localServerName       = localServerName;
        this.localServerUserId     = localServerUserId;
        this.localServerPassword   = localServerPassword;
        this.maxPageSize           = maxPageSize;
    }


    /**
     * Initialize the service.  This involves setting up clients to communicate with the Discovery Engine OMAS,
     * retrieving the discovery engine configuration and setting up the discovery engine handlers ready to
     * receive new discovery requests.
     *
     * @param discoveryEngineServicesConfig config properties
     * @param auditLog destination for audit log messages.
     * @throws OMAGConfigurationErrorException there are no discovery engines defined for this server,
     * or the requested discovery engines are not recognized or are not configured properly.
     */
    public void initialize(DiscoveryEngineServicesConfig discoveryEngineServicesConfig,
                           OMRSAuditLog                  auditLog) throws OMAGConfigurationErrorException
    {
        final String             actionDescription = "initialize";
        final String             methodName = "initialize";

        DiscoveryEngineServicesAuditCode auditCode;
        ODFRESTClient                    restClient;

        this.auditLog = auditLog;

        auditCode = DiscoveryEngineServicesAuditCode.SERVER_INITIALIZING;
        auditLog.logRecord(actionDescription,
                           auditCode.getLogMessageId(),
                           auditCode.getSeverity(),
                           auditCode.getFormattedLogMessage(localServerName),
                           null,
                           auditCode.getSystemAction(),
                           auditCode.getUserAction());


        try
        {
            /*
             * Handover problem between the admin services and the discovery engine services if the config is null.
             */
            if (discoveryEngineServicesConfig == null)
            {
                DiscoveryEngineServicesErrorCode errorCode    = DiscoveryEngineServicesErrorCode.NO_CONFIG_DOC;
                String                           errorMessage = errorCode.getErrorMessageId() + errorCode.getFormattedErrorMessage(localServerName);

                throw new OMAGConfigurationErrorException(errorCode.getHTTPErrorCode(),
                                                          this.getClass().getName(),
                                                          methodName,
                                                          errorMessage,
                                                          errorCode.getSystemAction(),
                                                          errorCode.getUserAction());
            }

            /*
             * The configuration for the discovery engines is located in an open metadata server.
             * It is accessed through the Discovery Engine OMAS.  If the values needed to call
             * the Discovery Engine OMAS are not present in the configuration then there is no point in continuing
             * and an exception is thrown.
             */
            String       accessServiceRootURL    = this.getAccessServiceRootURL(discoveryEngineServicesConfig);
            String       accessServiceServerName = this.getAccessServiceServerName(discoveryEngineServicesConfig);
            List<String> discoveryEngineNames    = this.getDiscoveryEngineNames(discoveryEngineServicesConfig);


            /*
             * Create the client for accessing the configuration.  The Discovery Engine OMAS has a specific client for retrieving
             * configuration. Any problems results in an exception.
             */
            DiscoveryConfigurationClient configurationClient;
            try
            {
                if ((localServerName != null) && (localServerPassword != null))
                {
                    restClient = new ODFRESTClient(accessServiceServerName,
                                                   accessServiceRootURL,
                                                   localServerUserId,
                                                   localServerPassword);
                }
                else
                {
                    restClient = new ODFRESTClient(accessServiceServerName, accessServiceRootURL);
                }

                configurationClient = new DiscoveryConfigurationClient(accessServiceServerName,
                                                                       accessServiceRootURL,
                                                                       restClient,
                                                                       maxPageSize,
                                                                       auditLog);
            }
            catch (InvalidParameterException error)
            {
                throw new OMAGConfigurationErrorException(error.getReportedHTTPCode(),
                                                          this.getClass().getName(),
                                                          methodName,
                                                          error.getErrorMessage(),
                                                          error.getReportedSystemAction(),
                                                          error.getReportedUserAction(),
                                                          error);
            }

            /*
             * Create a discovery handler for each of the discovery engines.
             */
            Map<String, DiscoveryEngineHandler> discoveryEngineHandlers = this.getDiscoveryEngineHandlers(discoveryEngineNames,
                                                                                                          accessServiceRootURL,
                                                                                                          accessServiceServerName,
                                                                                                          configurationClient,
                                                                                                          restClient);

            if (discoveryEngineHandlers == null)
            {
                auditCode = DiscoveryEngineServicesAuditCode.NO_DISCOVERY_ENGINES_STARTED;
                auditLog.logRecord(actionDescription,
                                   auditCode.getLogMessageId(),
                                   auditCode.getSeverity(),
                                   auditCode.getFormattedLogMessage(localServerName),
                                   null,
                                   auditCode.getSystemAction(),
                                   auditCode.getUserAction());

                DiscoveryEngineServicesErrorCode errorCode    = DiscoveryEngineServicesErrorCode.NO_DISCOVERY_ENGINES_STARTED;
                String                           errorMessage = errorCode.getErrorMessageId() + errorCode.getFormattedErrorMessage(localServerName);

                throw new OMAGConfigurationErrorException(errorCode.getHTTPErrorCode(),
                                                          this.getClass().getName(),
                                                          methodName,
                                                          errorMessage,
                                                          errorCode.getSystemAction(),
                                                          errorCode.getUserAction());
            }

            /*
             * Register a listener for the Discovery Engine OMAS out topic.  This call will fail if
             * the metadata server is not running so a separate thread is created to retry the registration request at
             * intervals to wait for the metadata server to restart.  It will also try to retrieve the configuration
             * for the discovery engines.
             */
            DiscoveryConfigurationRefreshHandler configurationHandler = new DiscoveryConfigurationRefreshHandler(discoveryEngineHandlers,
                                                                                                                 configurationClient,
                                                                                                                 auditLog,
                                                                                                                 localServerUserId,
                                                                                                                 localServerName,
                                                                                                                 accessServiceServerName,
                                                                                                                 accessServiceRootURL);
            Thread thread = new Thread(configurationHandler, configurationHandler.getClass().getName());
            thread.start();

            /*
             * Set up the REST APIs.
             */
            discoveryServerInstance = new DiscoveryServerInstance(localServerName,
                                                                  GovernanceServicesDescription.DISCOVERY_ENGINE_SERVICES.getServiceName(),
                                                                  auditLog,
                                                                  localServerUserId,
                                                                  maxPageSize,
                                                                  discoveryEngineServicesConfig.getOMAGServerPlatformRootURL(),
                                                                  discoveryEngineServicesConfig.getOMAGServerName(),
                                                                  discoveryEngineHandlers);
        }
        catch (Throwable error)
        {
            auditCode = DiscoveryEngineServicesAuditCode.SERVICE_INSTANCE_FAILURE;

            auditLog.logException(actionDescription,
                                  auditCode.getLogMessageId(),
                                  auditCode.getSeverity(),
                                  auditCode.getFormattedLogMessage(localServerName, error.getMessage()),
                                  error.toString(),
                                  auditCode.getSystemAction(),
                                  auditCode.getUserAction(),
                                  error);

            DiscoveryEngineServicesErrorCode errorCode    = DiscoveryEngineServicesErrorCode.SERVICE_INSTANCE_FAILURE;
            String                           errorMessage = errorCode.getErrorMessageId() + errorCode.getFormattedErrorMessage(localServerName, error.getMessage());

            throw new OMAGConfigurationErrorException(errorCode.getHTTPErrorCode(),
                                                      this.getClass().getName(),
                                                      methodName,
                                                      errorMessage,
                                                      errorCode.getSystemAction(),
                                                      errorCode.getUserAction(),
                                                      error);
        }
    }


    /**
     * Return the open metadata server's root URL from the configuration.
     *
     * @param discoveryEngineServicesConfig configuration
     * @return root URL
     * @throws OMAGConfigurationErrorException No root URL present in the config
     */
    private String getAccessServiceRootURL(DiscoveryEngineServicesConfig discoveryEngineServicesConfig) throws OMAGConfigurationErrorException
    {
        String accessServiceRootURL = discoveryEngineServicesConfig.getOMAGServerPlatformRootURL();

        if (accessServiceRootURL == null)
        {
            final String actionDescription = "Validate discovery engine services configuration.";
            final String methodName        = "getAccessServiceRootURL";

            DiscoveryEngineServicesAuditCode auditCode = DiscoveryEngineServicesAuditCode.NO_OMAS_SERVER_URL;
            auditLog.logRecord(actionDescription,
                               auditCode.getLogMessageId(),
                               auditCode.getSeverity(),
                               auditCode.getFormattedLogMessage(localServerName),
                               null,
                               auditCode.getSystemAction(),
                               auditCode.getUserAction());

            DiscoveryEngineServicesErrorCode errorCode    = DiscoveryEngineServicesErrorCode.NO_OMAS_SERVER_URL;
            String                           errorMessage = errorCode.getErrorMessageId() + errorCode.getFormattedErrorMessage(localServerName);

            throw new OMAGConfigurationErrorException(errorCode.getHTTPErrorCode(),
                                                      this.getClass().getName(),
                                                      methodName,
                                                      errorMessage,
                                                      errorCode.getSystemAction(),
                                                      errorCode.getUserAction());
        }

        return accessServiceRootURL;
    }


    /**
     * Return the open metadata server's name from the configuration.
     *
     * @param discoveryEngineServicesConfig configuration
     * @return server name
     * @throws OMAGConfigurationErrorException No server name present in the config
     */
    private String getAccessServiceServerName(DiscoveryEngineServicesConfig discoveryEngineServicesConfig) throws OMAGConfigurationErrorException
    {
        String accessServiceServerName = discoveryEngineServicesConfig.getOMAGServerName();

        if (accessServiceServerName == null)
        {
            final String actionDescription = "Validate discovery engine services configuration.";
            final String methodName        = "getAccessServiceServerName";

            DiscoveryEngineServicesAuditCode auditCode = DiscoveryEngineServicesAuditCode.NO_OMAS_SERVER_NAME;
            auditLog.logRecord(actionDescription,
                               auditCode.getLogMessageId(),
                               auditCode.getSeverity(),
                               auditCode.getFormattedLogMessage(localServerName),
                               null,
                               auditCode.getSystemAction(),
                               auditCode.getUserAction());

            DiscoveryEngineServicesErrorCode errorCode    = DiscoveryEngineServicesErrorCode.NO_OMAS_SERVER_NAME;
            String                           errorMessage = errorCode.getErrorMessageId() + errorCode.getFormattedErrorMessage(localServerName);

            throw new OMAGConfigurationErrorException(errorCode.getHTTPErrorCode(),
                                                      this.getClass().getName(),
                                                      methodName,
                                                      errorMessage,
                                                      errorCode.getSystemAction(),
                                                      errorCode.getUserAction());
        }

        return accessServiceServerName;
    }


    /**
     * Retrieve the list of discovery engine names for this server from the configuration.
     *
     * @param discoveryEngineServicesConfig configuration
     * @return list of discovery engine names
     */
    private List<String>  getDiscoveryEngineNames(DiscoveryEngineServicesConfig discoveryEngineServicesConfig) throws OMAGConfigurationErrorException
    {
        List<String> discoveryEngineNames = discoveryEngineServicesConfig.getDiscoveryEngineNames();

        if (discoveryEngineNames.isEmpty())
        {
            final String actionDescription = "Validate discovery engine services configuration.";
            final String methodName        = "getAccessServiceRootURL";

            DiscoveryEngineServicesAuditCode auditCode = DiscoveryEngineServicesAuditCode.NO_DISCOVERY_ENGINES;
            auditLog.logRecord(actionDescription,
                               auditCode.getLogMessageId(),
                               auditCode.getSeverity(),
                               auditCode.getFormattedLogMessage(),
                               null,
                               auditCode.getSystemAction(),
                               auditCode.getUserAction());

            DiscoveryEngineServicesErrorCode errorCode    = DiscoveryEngineServicesErrorCode.NO_DISCOVERY_ENGINES;
            String                           errorMessage = errorCode.getErrorMessageId() + errorCode.getFormattedErrorMessage(localServerName);

            throw new OMAGConfigurationErrorException(errorCode.getHTTPErrorCode(),
                                                      this.getClass().getName(),
                                                      methodName,
                                                      errorMessage,
                                                      errorCode.getSystemAction(),
                                                      errorCode.getUserAction());
        }
        else
        {
            return discoveryEngineNames;
        }
    }


    /**
     * Create the list of discovery engine handlers.
     *
     * @param discoveryEngineNames list of names for the discovery engines
     * @param accessServiceRootURL URL Root for the Discovery Engine OMAS
     * @param accessServiceServerName Server Name for the Discovery Engine OMAS
     * @param configurationClient client to retrieve configuration from
     * @param odfRESTClient client for calling REST APIs
     * @return map of discovery engine GUIDs to handlers
     * @throws OMAGConfigurationErrorException problem with config
     */
    private Map<String, DiscoveryEngineHandler>  getDiscoveryEngineHandlers(List<String>                 discoveryEngineNames,
                                                                            String                       accessServiceRootURL,
                                                                            String                       accessServiceServerName,
                                                                            DiscoveryConfigurationClient configurationClient,
                                                                            ODFRESTClient                odfRESTClient) throws OMAGConfigurationErrorException
    {
        final String methodName        = "getDiscoveryEngineHandlers";

        Map<String, DiscoveryEngineHandler> discoveryEngineHandlers = new HashMap<>();

        for (String   discoveryEngineName : discoveryEngineNames)
        {
            if (discoveryEngineName != null)
            {
                DiscoveryEngineClient discoveryEngineClient = null;
                try
                {
                    discoveryEngineClient = new DiscoveryEngineClient(accessServiceServerName,
                                                                      accessServiceRootURL,
                                                                      odfRESTClient);
                }
                catch (Throwable  error)
                {
                    /*
                     * Unable to create a client to the Discovery Engine.  This is a config problem that is not possible to
                     * work around so shut down the server.
                     */
                    DiscoveryEngineServicesErrorCode errorCode    = DiscoveryEngineServicesErrorCode.NO_DISCOVERY_ENGINE_CLIENT;
                    String                           errorMessage = errorCode.getErrorMessageId()
                                                                  + errorCode.getFormattedErrorMessage(localServerName,
                                                                                                       discoveryEngineName,
                                                                                                       error.getClass().getName(),
                                                                                                       error.getMessage());

                    throw new OMAGConfigurationErrorException(errorCode.getHTTPErrorCode(),
                                                              this.getClass().getName(),
                                                              methodName,
                                                              errorMessage,
                                                              errorCode.getSystemAction(),
                                                              errorCode.getUserAction());
                }

                /*
                 * Create a handler for the discovery engine.
                 */
                DiscoveryEngineHandler  handler = new DiscoveryEngineHandler(discoveryEngineName,
                                                                             accessServiceServerName,
                                                                             localServerUserId,
                                                                             configurationClient,
                                                                             discoveryEngineClient,
                                                                             auditLog,
                                                                             maxPageSize);

                discoveryEngineHandlers.put(discoveryEngineName, handler);
            }
        }

        if (discoveryEngineHandlers.isEmpty())
        {
            return null;
        }
        else
        {
            return discoveryEngineHandlers;
        }
    }


    /**
     * Shutdown the service.
     */
    public void terminate()
    {

        final String                     actionDescription = "terminate";
        DiscoveryEngineServicesAuditCode auditCode;

        auditCode = DiscoveryEngineServicesAuditCode.SERVER_SHUTTING_DOWN;
        auditLog.logRecord(actionDescription,
                           auditCode.getLogMessageId(),
                           auditCode.getSeverity(),
                           auditCode.getFormattedLogMessage(localServerName),
                           null,
                           auditCode.getSystemAction(),
                           auditCode.getUserAction());

        discoveryServerInstance.shutdown();

        auditCode = DiscoveryEngineServicesAuditCode.SERVER_SHUTDOWN;
        auditLog.logRecord(actionDescription,
                           auditCode.getLogMessageId(),
                           auditCode.getSeverity(),
                           auditCode.getFormattedLogMessage(localServerName),
                           null,
                           auditCode.getSystemAction(),
                           auditCode.getUserAction());
    }
}
