package com.igsl.job;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.atlassian.scheduler.JobRunner;
import com.atlassian.scheduler.config.JobId;
import com.igsl.action.DataAction;
import com.igsl.session.DataRow;

public abstract class Job implements JobRunner {
	
	private static final SimpleDateFormat SDF = new SimpleDateFormat("yyyyMMddHHmmssSSS");
	
	protected static final String DOUBLE_QUOTE = "\"";
	protected static final String NEWLINE = "\r\n";

	protected static ActiveObjects ao;
	
	/**
	 * Must be invoked asap.
	 * @param ao
	 */
	public static void setActiveObejcts(ActiveObjects ao) {
		Job.ao = ao;
	}
	
	protected JobId jobId;
	protected JobEntity jobEntity;
	protected DataAction dataAction;
	protected DataRow dataRow;
	protected ApplicationUser user;
	
	/**
	 * For creating a job from ActiveObject
	 */
	public Job(JobEntity jobEntity) {
		this.jobEntity = jobEntity;
		this.jobId = JobId.of(this.jobEntity.getJobId());
		this.dataAction = DataAction.parse(this.jobEntity.getAction());
		this.dataRow = null;
	}
	
	/**
	 * For creating a Job that can be executed
	 */
	public Job(ApplicationUser user, DataRow row, DataAction dataAction) {
		this.jobEntity = ao.create(JobEntity.class);
		this.jobEntity.setScriptedFieldId(row.getScriptedField().getFullFieldId());
		this.jobEntity.setAction(dataAction.getValue());
		this.jobEntity.setDownload(null);
		this.jobEntity.setJobId(createUniqueIdentifier(user.getUsername()));
		this.jobEntity.setMessage(null);
		this.jobEntity.setRunning(false);
		this.jobEntity.setUsername(user.getUsername());
		this.jobEntity.setStartDate(null);
		this.jobEntity.setEndDate(null);
		this.dataAction = dataAction;
		this.dataRow = row;
		this.user = user;
		this.jobId = JobId.of(this.jobEntity.getJobId());
	}

	protected static Job createJobFromEntity(ApplicationUser user, JobEntity entity) {
		DataAction action = DataAction.parse(entity.getAction());
		Job job = null;
		switch (action) {
		case COPY:
			job = new CopyDataJob(entity);
			break;
		case EXPORT:
			job = new ExportDataJob(entity);
			break;
		case NONE:
			break;
		}
		return job;
	}
	
	private final static String createUniqueIdentifier(String username) {
		// Create unique job Id
		return "ScriptedFieldConversion-" + SDF.format(new Date()) + "-" + username;
	}
	
	public static JobEntity loadJobEntity(String id) {
		for (JobEntity e : ao.find(JobEntity.class, "ID = ?", id)) {
			// Return first match
			return e;
		}
		return null;
	}
	
	public static List<JobEntity> loadAllJobEntity() {
		JobEntity[] entityList = ao.executeInTransaction(new TransactionCallback<JobEntity[]>() {
			@Override
			public JobEntity[] doInTransaction() {
				return ao.find(JobEntity.class);
			}
		});
		List<JobEntity> result = new ArrayList<>();
		for (JobEntity entity : entityList) {
			result.add(entity);
		}
		return result;
	}
	
	public static void deleteJobEntity(JobEntity entity) {
		ao.executeInTransaction(new TransactionCallback<Void>() {
			@Override
			public Void doInTransaction() {
				ao.delete(entity);
				return null;
			}			
		});
	}
	
	public void save() {
		ao.executeInTransaction(new TransactionCallback<Void>() {
			@Override
			public Void doInTransaction() {
				jobEntity.save();
				return null;
			}
		});
	}
	
	/**
	 * Append message to buffer
	 */
	protected final void appendMessage(String msg) {
		if (this.jobEntity.getMessage() == null) {
			this.jobEntity.setMessage(msg + NEWLINE);
		} else {
			this.jobEntity.setMessage(this.jobEntity.getMessage() + msg + NEWLINE);
		}
	}

	/**
	 * Set startDate, running to true, and save jobEntity
	 */
	protected final void start() {
		this.jobEntity.setStartDate(new Date());
		this.jobEntity.setRunning(true);
		save();
	}
	

	/**
	 * Set endDate, running to false, and save jobEntity
	 */
	protected final void stop() {
		this.jobEntity.setEndDate(new Date());
		this.jobEntity.setRunning(false);
		save();
	}
	
	public DataAction getAction() {
		return this.dataAction;
	}
	public JobId getJobId() {
		return jobId;
	}
	public void setJobId(JobId jobId) {
		this.jobId = jobId;
	}
	public JobEntity getJobEntity() {
		return jobEntity;
	}
}
