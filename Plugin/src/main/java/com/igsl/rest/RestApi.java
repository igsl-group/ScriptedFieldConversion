package com.igsl.rest;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.igsl.job.Job;
import com.igsl.job.JobEntity;
import com.igsl.job.JobEntityWrapper;
/**
 * REST API for IGSL custom field types.
 */
@Path("/")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class RestApi {

	private static ObjectMapper OM = new ObjectMapper();
	private static Logger LOGGER = LoggerFactory.getLogger(RestApi.class);

	/**
	 * Exposes full list of JobEntity via REST API.
	 */
	@GET
	@Path("/getAllJobEntity")
	public Response getAllJobEntity() {
		List<JobEntity> jobEntityList = Job.loadAllJobEntity();
		List<JobEntityWrapper> wraperList = new ArrayList<>();
		for (JobEntity e : jobEntityList) {
			wraperList.add(new JobEntityWrapper(e));
		}
		try {
			LOGGER.info("REST: " + OM.writeValueAsString(wraperList));
		} catch (JsonProcessingException jpex) {
			LOGGER.error("REST", jpex);
		}
		return Response.ok().entity(wraperList).build();
	}
	
}
