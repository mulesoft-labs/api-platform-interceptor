/*
 * (c) 2003-2017 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */

package com.mulesoft.platform.interceptor;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;
import org.mule.api.MuleMessage;
import org.mule.api.routing.filter.Filter;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@Path("/")
public class Interceptor implements Filter {

	private static final Logger LOG = Logger.getLogger("Message");

	private static final String NEW_LINE = System.getProperty("line.separator");

	private static final String HTTP_STATUS_KEY = "http.status";
	private static final String HTTP_URI_PARAMETER_KEY = "http.uri.params";
	private static final String RUNTIME_ID_KEY = "runtimeId";
	private static final String ENVIRONMENT_KEY = "environment";

	private static Cache<String, StatsManager> runtimes;

	static {
		runtimes = CacheBuilder.newBuilder().expireAfterAccess(1, TimeUnit.HOURS).build();
	}

	@Override
	public boolean accept(MuleMessage message) {
		@SuppressWarnings("unchecked")
		final String runtimeId = ((Map<String, String>) message.getInboundProperty(HTTP_URI_PARAMETER_KEY)).get(RUNTIME_ID_KEY);
		final String listenerPath = ((String) (message.getInboundProperty("http.listener.path"))).replaceAll("\\{runtimeId\\}", runtimeId).replace("/*", "");
		final String httpMethod = ((String) (message.getInboundProperty("http.method")));
		final String httpRequestPath = ((String) (message.getInboundProperty("http.request.uri"))).substring(listenerPath.length());

		final StatsManager statsManager = runtimes.asMap().get(listenerPath);

		if (statsManager == null) {
			LOG.debug(String.format("%s %s call %sallowed for runtime with ID %s (no criteria defined)", httpMethod.toUpperCase(), httpRequestPath, NEW_LINE, runtimeId));
			return true;
		}

		final FilterResponse filterResponse = statsManager.getFilterResponse(httpRequestPath, httpMethod);
		if (filterResponse.isPassThru()) {
			LOG.debug(String.format("%s %s call %sis allowed for runtime with ID %s", httpMethod.toUpperCase(), httpRequestPath, NEW_LINE, runtimeId));
			return true;
		}

		message.setOutboundProperty(HTTP_STATUS_KEY, filterResponse.getStatusCode());
		message.setPayload(filterResponse.getMessage());
		LOG.debug(String.format("%s %s call %sis blocked for runtime with ID %s", httpMethod.toUpperCase(), httpRequestPath, NEW_LINE, runtimeId));
		return false;
	}

	@GET
	@Produces(MediaType.TEXT_PLAIN)
	public String getHealth() {
		return "Test endpoint successfully called!";
	}

	@Path("http/status")
	@POST
	@Consumes("application/json")
	@Produces(MediaType.TEXT_PLAIN)
	public Response setStatus(String originalPayload) throws JsonParseException, JsonMappingException, IOException {
		final JsonObject jsonObject = new JsonParser().parse(originalPayload).getAsJsonObject();
		final String runtimeId = jsonObject.get(RUNTIME_ID_KEY).getAsString();
		final String environment = jsonObject.get(ENVIRONMENT_KEY).getAsString();
		runtimes.asMap().put(environment + "/" + runtimeId, new StatsManager(jsonObject));
		
		return Response.status(Response.Status.ACCEPTED).type(MediaType.TEXT_PLAIN).entity(String.format("Response strategy for %s updated", runtimeId)).build();
	}
}
