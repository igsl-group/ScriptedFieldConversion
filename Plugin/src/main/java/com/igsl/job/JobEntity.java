package com.igsl.job;

import java.util.Date;

import net.java.ao.Entity;
import net.java.ao.schema.StringLength;
import net.java.ao.schema.Table;

@Table("JobEntity")
public interface JobEntity extends Entity {
	
	// currentStatus
	public String getCurrentStatus();
	public void setCurrentStatus(String currentStatus);
	
	// scriptedFieldId
	public String getScriptedFieldId();
	public void setScriptedFieldId(String scriptedFieldId);
	
	// startDate
	public Date getStartDate();
	public void setStartDate(Date startDate);
	
	// endDate
	public Date getEndDate();
	public void setEndDate(Date endDate);
	
	// username
	public String getUsername();
	public void setUsername(String username);
	
	// download
	public String getDownload();
	@StringLength(StringLength.UNLIMITED)
	public void setDownload(String download);
	
	// action (value of DataAction enum)
	public String getAction();
	public void setAction(String action);
	
	// jobId
	public String getJobId();
	public void setJobId(String jobId);
	
	// running
	public boolean isRunning();
	public void setRunning(boolean running);
	
	// message
	public String getMessage();
	@StringLength(StringLength.UNLIMITED)
	public void setMessage(String message);
}
