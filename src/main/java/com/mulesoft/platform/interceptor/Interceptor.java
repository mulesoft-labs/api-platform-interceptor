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
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.log4j.Logger;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.processor.MessageProcessor;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.JsonParser;

// TODO: Add reset by runtime.
// TODO: Parameterize environment and listener ports.

@Path("/")
public class Interceptor implements MessageProcessor {
	private static final Logger LOG = Logger.getLogger("Message");

	static final String NEW_LINE = System.getProperty("line.separator");

	private static final String HTTP_STATUS_KEY = "http.status";
	private static final String HTTP_URI_PARAMETER_KEY = "http.uri.params";
	private static final String RUNTIME_ID_KEY = "runtimeId";
	
	private static final String MULE_HTTP_METHOD_PROP = "http.method";
	private static final String MULE_LISTENER_PATH_PROP = "http.listener.path";
	private static final String MULE_REQUEST_URI_PROP = "http.request.uri";
	
	private static final String URI_TEMPLATE_RUNTIME_ID = "\\{runtimeId\\}";

	private static Cache<String, StatsManager> runtimes;

	static {
		runtimes = CacheBuilder.newBuilder().expireAfterAccess(1, TimeUnit.HOURS).build();
	}

	@Override
	public MuleEvent process(MuleEvent event) {
		final MuleMessage message = event.getMessage();
		@SuppressWarnings("unchecked")
		final String runtimeId = ((Map<String, String>) message.getInboundProperty(HTTP_URI_PARAMETER_KEY)).get(RUNTIME_ID_KEY);
		final String listenerPath = ((String) (message.getInboundProperty(MULE_LISTENER_PATH_PROP))).replaceAll(URI_TEMPLATE_RUNTIME_ID, runtimeId).replace("/*", "");
		final String httpMethod = ((String) (message.getInboundProperty(MULE_HTTP_METHOD_PROP)));
		final String httpRequestPath = ((String) (message.getInboundProperty(MULE_REQUEST_URI_PROP))).substring(listenerPath.length());

		final StatsManager statsManager = runtimes.asMap().get(listenerPath);
		if (statsManager == null) {
			if (LOG.isDebugEnabled()) {
				LOG.debug(String.format("%s %s call %sALLOWED for runtime with ID %s (no criteria defined)", httpMethod.toUpperCase(), httpRequestPath, NEW_LINE, runtimeId));
			}
			return event;
		}

		final FilterResponse filterResponse = statsManager.getFilterResponse(httpRequestPath, httpMethod);
		if (filterResponse.isPassThru()) {
			if (LOG.isDebugEnabled()) {
				LOG.debug(String.format("%s %s call %ALLOWED for runtime with ID %s", httpMethod.toUpperCase(), httpRequestPath, NEW_LINE, runtimeId));
			}
			return event;
		}

		message.setOutboundProperty(HTTP_STATUS_KEY, filterResponse.getStatusCode());
		message.setPayload(filterResponse.getMessage());
		if (LOG.isDebugEnabled()) {
			LOG.debug(String.format("%s %s call %sBLOCKED for runtime with ID %s", httpMethod.toUpperCase(), httpRequestPath, NEW_LINE, runtimeId));
		}
		throw new RuntimeException("Message processing stopped");
	}

	@GET
	@Produces(MediaType.TEXT_PLAIN)
	public String getInterceptorHealth() {
		return "Test endpoint successfully called!";
	}

	@Path("statsmanager/{environment}/{runtimeId}")
	@POST
	@Consumes("application/json")
	@Produces(MediaType.TEXT_PLAIN)
	public Response setStatsManager(String originalPayload, @PathParam("environment") String environment, @PathParam("runtimeId") String runtimeId) throws JsonParseException, JsonMappingException, IOException {
		if ((runtimeId == null) || (environment == null)) {
			throw new RuntimeException("Unable to identify runtime, either or bot runtimeID and environment not provided");
		}
		final StatsManager statsManager = new StatsManager(new JsonParser().parse(originalPayload).getAsJsonObject());
		runtimes.asMap().put(getStatsManagerKey(environment, runtimeId), statsManager);
		
		return Response.status(Response.Status.ACCEPTED).type(MediaType.TEXT_PLAIN).entity(String.format("Response strategy for environment %s with runtime ID %s updated with %s", environment, runtimeId, statsManager)).build();
	}
	
	@Path("statsmanager/{environment}/{runtimeId}")
	@DELETE
	@Consumes("application/json")
	@Produces(MediaType.TEXT_PLAIN)
	public Response resetStatsManager(@PathParam("environment") String environment, @PathParam("runtimeId") String runtimeId) throws JsonParseException, JsonMappingException, IOException {
		if ((runtimeId == null) || (environment == null)) {
			throw new RuntimeException("Unable to identify runtime, either or bot runtimeID and environment not provided");
		}
		final StatsManager statsManager = runtimes.asMap().get(String.format("/%s/%s", environment, runtimeId));
		runtimes.asMap().remove(getStatsManagerKey(environment, runtimeId));
		return Response.status(Response.Status.NO_CONTENT).type(MediaType.TEXT_PLAIN).entity(String.format("Response strategy for environment %s with runtime ID %s removed. Stats removed: %s", environment, runtimeId, statsManager)).build();
	}
	
	@Path("statsmanager/{environment}/{runtimeId}")
	@GET
	@Consumes("application/json")
	@Produces(MediaType.TEXT_PLAIN)
	public Response getStatsManagerInfo(@PathParam("environment") String environment, @PathParam("runtimeId") String runtimeId) throws JsonParseException, JsonMappingException, IOException {
		if ((runtimeId == null) || (environment == null)) {
			throw new RuntimeException("Unable to identify runtime, either or bot runtimeID and environment not provided");
		}
		final StatsManager statsManager = runtimes.asMap().get(String.format("/%s/%s", environment, runtimeId));
		return Response.status(Response.Status.OK).type(MediaType.TEXT_PLAIN).entity(String.format("Response strategy for environment %s with runtime ID %s: %s", environment, runtimeId, statsManager)).build();
	}
	
	private String getStatsManagerKey(String environment, String runtimeId) {
		return String.format("/%s/%s", environment, runtimeId);
	}
}