/* SPDX-License-Identifier: Apache 2.0 */
/* Copyright Contributors to the ODPi Egeria project. */
package org.odpi.openmetadata.adminservices;

import org.odpi.openmetadata.adminservices.configuration.properties.OMAGServerClientConfig;
import org.odpi.openmetadata.adminservices.configuration.properties.StewardshipEngineServicesConfig;
import org.odpi.openmetadata.adminservices.configuration.properties.OMAGServerConfig;
import org.odpi.openmetadata.adminservices.configuration.registration.AccessServiceDescription;
import org.odpi.openmetadata.adminservices.configuration.registration.GovernanceServicesDescription;
import org.odpi.openmetadata.adminservices.ffdc.exception.OMAGInvalidParameterException;
import org.odpi.openmetadata.adminservices.ffdc.exception.OMAGNotAuthorizedException;
import org.odpi.openmetadata.commonservices.ffdc.rest.VoidResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * OMAGServerConfigStewardshipServices supports the configuration requests for the stewardship services.
 */
public class OMAGServerConfigStewardshipEngineServices
{
    private static final String serviceName    = GovernanceServicesDescription.STEWARDSHIP_SERVICES.getServiceName();
    private static final String accessService  = AccessServiceDescription.STEWARDSHIP_ACTION_OMAS.getAccessServiceName();

    private static final Logger log = LoggerFactory.getLogger(OMAGServerConfigStewardshipEngineServices.class);

    private OMAGServerAdminStoreServices   configStore = new OMAGServerAdminStoreServices();
    private OMAGServerErrorHandler         errorHandler = new OMAGServerErrorHandler();
    private OMAGServerExceptionHandler     exceptionHandler = new OMAGServerExceptionHandler();

    /**
     * Set up the name and platform URL root for the metadata server supporting this stewardship server.
     *
     * @param userId  user that is issuing the request.
     * @param serverName  local server name.
     * @param clientConfig  URL root and server name for the metadata server.
     * @return void response or
     * OMAGNotAuthorizedException the supplied userId is not authorized to issue this command or
     * OMAGInvalidParameterException invalid serverName or serverType parameter.
     */
    public VoidResponse setAccessServiceLocation(String                    userId,
                                                 String                    serverName,
                                                 OMAGServerClientConfig clientConfig)
    {
        final String methodName = "setAccessServiceLocation";

        log.debug("Calling method: " + methodName);

        VoidResponse response = new VoidResponse();

        try
        {
            errorHandler.validateServerName(serverName, methodName);
            errorHandler.validateUserId(userId, serverName, methodName);

            String accessServiceRootURL    = null;
            String accessServiceServerName = null;

            if (clientConfig != null)
            {
                accessServiceRootURL = clientConfig.getOMAGServerPlatformRootURL();
                accessServiceServerName = clientConfig.getOMAGServerName();
            }

            errorHandler.validateAccessServiceRootURL(accessServiceRootURL, accessService, serverName, serviceName);
            errorHandler.validateAccessServiceServerName(accessServiceServerName, accessService, serverName, serviceName);

            OMAGServerConfig serverConfig = configStore.getServerConfig(userId, serverName, methodName);

            List<String> configAuditTrail = serverConfig.getAuditTrail();

            if (configAuditTrail == null)
            {
                configAuditTrail = new ArrayList<>();
            }

            if (accessServiceRootURL == null)
            {
                configAuditTrail.add(new Date().toString() + " " + userId + " removed configuration for " + serviceName + " access service root url.");
            }
            else
            {
                configAuditTrail.add(new Date().toString() + " " + userId + " updated configuration for " + serviceName + " access service root url to " + accessServiceRootURL + ".");
            }

            serverConfig.setAuditTrail(configAuditTrail);

            StewardshipEngineServicesConfig stewardshipEngineServicesConfig = serverConfig.getStewardshipEngineServicesConfig();

            if (stewardshipEngineServicesConfig == null)
            {
                stewardshipEngineServicesConfig = new StewardshipEngineServicesConfig();
            }

            stewardshipEngineServicesConfig.setOMAGServerPlatformRootURL(accessServiceRootURL);
            stewardshipEngineServicesConfig.setOMAGServerName(accessServiceServerName);

            serverConfig.setStewardshipEngineServicesConfig(stewardshipEngineServicesConfig);

            configStore.saveServerConfig(serverName, methodName, serverConfig);
        }
        catch (OMAGInvalidParameterException error)
        {
            exceptionHandler.captureInvalidParameterException(response, error);
        }
        catch (OMAGNotAuthorizedException error)
        {
            exceptionHandler.captureNotAuthorizedException(response, error);
        }
        catch (Throwable  error)
        {
            exceptionHandler.capturePlatformRuntimeException(serverName, methodName, response, error);
        }

        log.debug("Returning from method: " + methodName + " with response: " + response.toString());

        return response;
    }


    /**
     * Set up the list of stewardship engines that will run in this stewardship server.
     *
     * @param userId  user that is issuing the request.
     * @param serverName  local server name.
     * @param stewardshipEngineNames  list of stewardship engine qualified names describing which stewardship engines run in this server.
     * @return void response or
     * OMAGNotAuthorizedException the supplied userId is not authorized to issue this command or
     * OMAGInvalidParameterException invalid serverName or serverType parameter.
     */
    public VoidResponse setStewardshipEngines(String       userId,
                                            String       serverName,
                                            List<String> stewardshipEngineNames)
    {
        final String methodName = "setStewardshipEngines";

        log.debug("Calling method: " + methodName);

        VoidResponse response = new VoidResponse();

        try
        {
            errorHandler.validateServerName(serverName, methodName);
            errorHandler.validateUserId(userId, serverName, methodName);

            OMAGServerConfig serverConfig = configStore.getServerConfig(userId, serverName, methodName);

            List<String> configAuditTrail = serverConfig.getAuditTrail();

            if (configAuditTrail == null)
            {
                configAuditTrail = new ArrayList<>();
            }

            if (stewardshipEngineNames == null)
            {
                configAuditTrail.add(new Date().toString() + " " + userId + " removed configuration for " + serviceName + " inbound request " +
                                             "stewardshipEngineNames.");
            }
            else
            {
                configAuditTrail.add(new Date().toString() + " " + userId + " updated configuration for " + serviceName + " inbound request " +
                                             "stewardshipEngineNames.");
            }

            serverConfig.setAuditTrail(configAuditTrail);

            StewardshipEngineServicesConfig stewardshipEngineServicesConfig = serverConfig.getStewardshipEngineServicesConfig();

            if (stewardshipEngineServicesConfig == null)
            {
                stewardshipEngineServicesConfig = new StewardshipEngineServicesConfig();
            }

            stewardshipEngineServicesConfig.setStewardshipEngineNames(stewardshipEngineNames);

            serverConfig.setStewardshipEngineServicesConfig(stewardshipEngineServicesConfig);

            configStore.saveServerConfig(serverName, methodName, serverConfig);
        }
        catch (OMAGInvalidParameterException error)
        {
            exceptionHandler.captureInvalidParameterException(response, error);
        }
        catch (OMAGNotAuthorizedException error)
        {
            exceptionHandler.captureNotAuthorizedException(response, error);
        }
        catch (Throwable  error)
        {
            exceptionHandler.capturePlatformRuntimeException(serverName, methodName, response, error);
        }

        log.debug("Returning from method: " + methodName + " with response: " + response.toString());

        return response;
    }


    /**
     * Add this service to the server configuration.
     *
     * @param userId  user that is issuing the request.
     * @param serverName  local server name.
     * @param servicesConfig full configuration for the service.
     * @return void response
     */
    public VoidResponse addService(String userId, String serverName, StewardshipEngineServicesConfig servicesConfig)
    {
        final String methodName = "addService";

        log.debug("Calling method: " + methodName);

        VoidResponse response = new VoidResponse();

        try
        {
            this.setAccessServiceLocation(userId, serverName, servicesConfig);
            this.setStewardshipEngines(userId, serverName, servicesConfig.getStewardshipEngineNames());
        }
        catch (Throwable  error)
        {
            exceptionHandler.capturePlatformRuntimeException(serverName, methodName, response, error);
        }

        log.debug("Returning from method: " + methodName + " with response: " + response.toString());

        return response;
    }


    /**
     * Remove this service from the server configuration.
     *
     * @param userId  user that is issuing the request.
     * @param serverName  local server name.
     * @return void response
     */
    public VoidResponse deleteService(String userId, String serverName)
    {
        final String methodName = "deleteService";

        log.debug("Calling method: " + methodName);

        VoidResponse response = new VoidResponse();

        try
        {
            errorHandler.validateServerName(serverName, methodName);
            errorHandler.validateUserId(userId, serverName, methodName);

            OMAGServerConfig serverConfig = configStore.getServerConfig(userId, serverName, methodName);

            List<String> configAuditTrail = serverConfig.getAuditTrail();

            if (configAuditTrail == null)
            {
                configAuditTrail = new ArrayList<>();
            }

            configAuditTrail.add(new Date().toString() + " " + userId + " removed configuration for " + serviceName + ".");

            serverConfig.setAuditTrail(configAuditTrail);
            serverConfig.setStewardshipEngineServicesConfig(null);

            configStore.saveServerConfig(serverName, methodName, serverConfig);
        }
        catch (OMAGInvalidParameterException error)
        {
            exceptionHandler.captureInvalidParameterException(response, error);
        }
        catch (OMAGNotAuthorizedException error)
        {
            exceptionHandler.captureNotAuthorizedException(response, error);
        }
        catch (Throwable  error)
        {
            exceptionHandler.capturePlatformRuntimeException(serverName, methodName, response, error);
        }

        log.debug("Returning from method: " + methodName + " with response: " + response.toString());

        return response;
    }
}
