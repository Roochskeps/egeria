/* SPDX-License-Identifier: Apache-2.0 */
/* Copyright Contributors to the ODPi Egeria project. */
package org.odpi.openmetadata.commonservices.odf.metadatamanagement.rest;

import com.fasterxml.jackson.annotation.*;
import org.odpi.openmetadata.commonservices.ffdc.rest.FFDCResponseBase;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.PUBLIC_ONLY;

/**
 * ODFOMASAPIResponse provides a common header for or Open Discovery Framework (ODF) bean-based REST responses.
 * It manages information about exceptions.  If no exception has been raised exceptionClassName is null.
 */
@JsonAutoDetect(getterVisibility=PUBLIC_ONLY, setterVisibility=PUBLIC_ONLY, fieldVisibility=NONE)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "class")
@JsonSubTypes(
        {
                @JsonSubTypes.Type(value = AnnotationResponse.class, name = "AnnotationResponse"),
                @JsonSubTypes.Type(value = DiscoveryAnalysisReportResponse.class, name = "DiscoveryAnalysisReportResponse"),
                @JsonSubTypes.Type(value = DiscoveryEngineListResponse.class,        name = "DiscoveryEngineListResponse"),
                @JsonSubTypes.Type(value = DiscoveryEnginePropertiesResponse.class,  name = "DiscoveryEnginePropertiesResponse"),
                @JsonSubTypes.Type(value = DiscoveryRequestStatusResponse.class,       name = "DiscoveryRequestStatusResponse"),
                @JsonSubTypes.Type(value = DiscoveryServiceListResponse.class,       name = "DiscoveryServiceListResponse"),
                @JsonSubTypes.Type(value = DiscoveryServicePropertiesResponse.class, name = "DiscoveryServicePropertiesResponse"),
                @JsonSubTypes.Type(value = RegisteredDiscoveryServiceResponse.class, name = "RegisteredDiscoveryServiceResponse")
        })
public abstract class ODFOMASAPIResponse extends FFDCResponseBase
{
    private static final long    serialVersionUID = 1L;

    /**
     * Default constructor
     */
    public ODFOMASAPIResponse()
    {
        super();
    }


    /**
     * Copy/clone constructor
     *
     * @param template object to copy
     */
    public ODFOMASAPIResponse(ODFOMASAPIResponse template)
    {
        super(template);
    }


    /**
     * JSON-like toString
     *
     * @return string containing the property names and values
     */
    @Override
    public String toString()
    {
        return "ODFOMASAPIResponse{" +
                "relatedHTTPCode=" + getRelatedHTTPCode() +
                ", exceptionClassName='" + getExceptionClassName() + '\'' +
                ", exceptionErrorMessage='" + getExceptionErrorMessage() + '\'' +
                ", exceptionSystemAction='" + getExceptionSystemAction() + '\'' +
                ", exceptionUserAction='" + getExceptionUserAction() + '\'' +
                ", exceptionProperties=" + getExceptionProperties() +
                '}';
    }
}
