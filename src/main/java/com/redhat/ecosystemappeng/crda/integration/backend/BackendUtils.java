/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.redhat.ecosystemappeng.crda.integration.backend;

import java.util.List;

import org.apache.camel.Exchange;
import org.apache.camel.Header;
import org.apache.camel.http.base.HttpOperationFailedException;
import org.jboss.resteasy.reactive.common.util.MediaTypeHelper;

import com.redhat.ecosystemappeng.crda.integration.Constants;
import com.redhat.ecosystemappeng.crda.model.ProviderStatus;

import io.quarkus.runtime.annotations.RegisterForReflection;

import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

@RegisterForReflection
public class BackendUtils {

  public String getResponseMediaType(@Header(Constants.ACCEPT_HEADER) String acceptHeader) {
    if (acceptHeader == null || acceptHeader.isBlank()) {
      return Constants.DEFAULT_ACCEPT_MEDIA_TYPE;
    }
    List<MediaType> requested = MediaTypeHelper.parseHeader(acceptHeader);
    MediaType match = MediaTypeHelper.getBestMatch(Constants.VALID_RESPONSE_MEDIA_TYPES, requested);
    if (match == null) {
      throw new ClientErrorException(
          "Unexpected Accept header "
              + acceptHeader
              + ". Supported content types are: "
              + Constants.VALID_RESPONSE_MEDIA_TYPES,
          Status.UNSUPPORTED_MEDIA_TYPE);
    }
    return match.toString();
  }

  public static void processResponseError(Exchange exchange, String provider) {
    ProviderStatus.Builder builder = ProviderStatus.builder().ok(false).provider(provider);
    Exception exception = (Exception) exchange.getProperty(Exchange.EXCEPTION_CAUGHT);
    Throwable cause = exception.getCause();

    if (cause != null) {
      if (cause instanceof HttpOperationFailedException) {
        HttpOperationFailedException httpException = (HttpOperationFailedException) cause;
        builder.message(prettifyHttpError(httpException)).status(httpException.getStatusCode());

      } else {
        builder
            .message(cause.getMessage())
            .status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
      }
    } else {
      builder
          .message(exception.getMessage())
          .status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    }
    exchange.getMessage().setBody(builder.build());
  }

  public static void processTokenFallBack(Exchange exchange, String provider) {
    Exception exception = (Exception) exchange.getProperty(Exchange.EXCEPTION_CAUGHT);
    Throwable cause = exception.getCause();
    String body;
    int code = Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();

    if (cause instanceof HttpOperationFailedException) {
      HttpOperationFailedException httpException = (HttpOperationFailedException) cause;
      code = httpException.getStatusCode();
      if (code == Response.Status.UNAUTHORIZED.getStatusCode()) {
        body = "Invalid token provided. Unauthorized";
      } else {
        body = "Unable to validate " + provider + " Token: " + httpException.getStatusText();
      }
    } else {
      body = "Unable to validate " + provider + " Token: " + cause.getMessage();
    }
    exchange.getMessage().setHeader(Exchange.HTTP_RESPONSE_CODE, code);
    exchange.getMessage().setBody(body);
  }

  private static String prettifyHttpError(HttpOperationFailedException httpException) {
    String text = httpException.getStatusText();
    switch (httpException.getStatusCode()) {
      case 401:
        return text + ": Verify the provided credentials are valid.";
      case 403:
        return text + ": The provided credentials don't have the required permissions.";
      case 429:
        return text + ": The rate limit has been exceeded.";
      default:
        return text;
    }
  }
}
