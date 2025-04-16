package com.igsl.job;

import java.util.Date;

import com.igsl.ScriptedFieldConversion;

/**
 * Wrapper class for JobEntity for Jackson serialization
 * In order to avoid unserializable things inherited from Entity
 */
public class JobEntityWrapper {
	public int ID;
	public String currentStatus;
	public String scriptedFieldId;
	public Date startDate;
	public Date endDate;
	public String duration;
	public String username;
	public String download;
	public String action;
	public String jobId;
	public boolean running;
	public String message;
	
	public JobEntityWrapper() {}
	public JobEntityWrapper(JobEntity entity) {
		setCurrentStatus(entity.getCurrentStatus());
		setScriptedFieldId(entity.getScriptedFieldId());
		setStartDate(entity.getStartDate());
		setEndDate(entity.getEndDate());
		setUsername(entity.getUsername());
		setDownload(entity.getDownload());
		setAction(entity.getAction());
		setJobId(entity.getJobId());
		setRunning(entity.isRunning());
		setMessage(entity.getMessage());
		setID(entity.getID());
		setDuration(
				"From: " + entity.getStartDate() + " " + 
				"To: " + entity.getEndDate() + " " + 
				"Duration: " + ScriptedFieldConversion.calculateDuration(startDate, endDate));
	}
	
	public String getCurrentStatus() {
		return currentStatus;
	}
	public void setCurrentStatus(String currentStatus) {
		this.currentStatus = currentStatus;
	}
	public String getScriptedFieldId() {
		return scriptedFieldId;
	}
	public void setScriptedFieldId(String scriptedFieldId) {
		this.scriptedFieldId = scriptedFieldId;
	}
	public Date getStartDate() {
		return startDate;
	}
	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}
	public Date getEndDate() {
		return endDate;
	}
	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getDownload() {
		return download;
	}
	public void setDownload(String download) {
		this.download = download;
	}
	public String getAction() {
		return action;
	}
	public void setAction(String action) {
		this.action = action;
	}
	public String getJobId() {
		return jobId;
	}
	public void setJobId(String jobId) {
		this.jobId = jobId;
	}
	public boolean isRunning() {
		return running;
	}
	public void setRunning(boolean running) {
		this.running = running;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	public int getID() {
		return ID;
	}
	public void setID(int ID) {
		this.ID = ID;
	}
	public String getDuration() {
		return duration;
	}
	public void setDuration(String duration) {
		this.duration = duration;
	}
}
