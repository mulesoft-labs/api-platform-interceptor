package com.mulesoft.platform.interceptor;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.mule.api.MuleMessage;
import org.mule.api.routing.filter.Filter;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@Path("/")
public class Interceptor implements Filter {

	private static final String HTTP_STATUS_KEY = "http.status";
	private static final String HTTP_URI_PARAMETER_KEY = "http.uri.params";
	private static final String RUNTIME_ID_KEY = "runtimeId";
	
	private static Map<String, StatsManager> runtimes = Collections.synchronizedMap(new HashMap<String, StatsManager>());
	
	@Override
	public boolean accept(MuleMessage message) {
		@SuppressWarnings("unchecked")
		final String runtimeId = ((Map<String, String>) message.getInboundProperty(HTTP_URI_PARAMETER_KEY)).get(RUNTIME_ID_KEY);
		final String listenerPath = ((String)(message.getInboundProperty("http.listener.path"))).replaceAll("\\{runtimeId\\}", runtimeId).replace("/*", "");
		final String httpMethod = ((String)(message.getInboundProperty("http.method")));
		final String httpRequestPath = ((String)(message.getInboundProperty("http.request.uri"))).substring(listenerPath.length());
		
		StatsManager statsManager = runtimes.get(runtimeId);
		
		if (statsManager == null) {
			return true;
		}
		
		final ResponseDetail responseDetail = statsManager.getResponseDetail(httpRequestPath, httpMethod);
		if (responseDetail.isPassThru()) {
			System.out.println("Go ahead!!!!");
			return true;
		}
		
		message.setOutboundProperty(HTTP_STATUS_KEY, responseDetail.getStatusCode());
		message.setPayload(responseDetail.getMessage());
		System.out.println("YOU SHALL NOT PASS!!!!");
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
		runtimes.put(runtimeId, new StatsManager(jsonObject));
        return Response
        		.status(Response.Status.ACCEPTED)
        		.type(MediaType.TEXT_PLAIN)
        		.entity(String.format("Response strategy for %s updated", runtimeId))
        		.build();
	}
}
