package com.igsl.session;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Screen information
 */
public class ScreenInfo implements Comparable<ScreenInfo> {
	
	private static final String QUERY = 
			"SELECT " + 
			"	screen.NAME AS ScreenName, " + 
			"	screen.ID AS ScreenId, " + 
			"	tab.NAME AS TabName, " + 
			"	tab.ID AS TabId, " + 
			"	layout.SEQUENCE AS Sequence, " + 
			"	layout.FIELDIDENTIFIER AS FieldId, " + 
			"	layout.ID AS Id " + 
			"FROM " + 
			"	fieldscreenlayoutitem layout " + 
			"	JOIN fieldscreentab tab " + 
			"		ON tab.ID = layout.FIELDSCREENTAB " + 
			"	JOIN fieldscreen screen " + 
			"		ON screen.ID = tab.FIELDSCREEN " + 
			"WHERE layout.FIELDIDENTIFIER IN (";
	private static final String WHERE_END = ")";

	@Override
	public int compareTo(ScreenInfo o) {
		// Sort by screen name, tab name, then sequence
		int result = 1;
		if (o != null) {
			result = this.getScreenName().compareTo(o.getScreenName());
			if (result == 0) {
				result = this.getTabName().compareTo(o.getTabName());
				if (result == 0) {
					result = Integer.compare(this.getSequence(), o.getSequence());
				}
			}
		}
		return result;
	}
	
	public static String getQuerySQL(int paramCount) {
		StringBuilder query = new StringBuilder();
		for (int i = 0; i < paramCount; i++) {
			query.append(",?");
		}			
		query.delete(0, 1);
		query.insert(0, QUERY);
		query.append(WHERE_END);
		return query.toString();
	}
	
	private String screenName;
	private Long screenId;
	private String tabName;
	private Long tabId;
	private String fieldId;	// Number only, "customfield_" removed
	private int sequence;
	private int id;
	public ScreenInfo(ResultSet rs) throws SQLException {
		this.screenName = rs.getString("ScreenName");
		this.screenId = rs.getLong("ScreenId");
		this.tabName = rs.getString("TabName");
		this.tabId = rs.getLong("TabId");
		this.fieldId = rs.getString("FieldId");
		this.sequence = rs.getInt("Sequence");
		this.id = rs.getInt("Id");
	}
	public String getScreenName() {
		return screenName;
	}
	public void setScreenName(String screenName) {
		this.screenName = screenName;
	}
	public Long getScreenId() {
		return screenId;
	}
	public void setScreenId(Long screenId) {
		this.screenId = screenId;
	}
	public String getTabName() {
		return tabName;
	}
	public void setTabName(String tabName) {
		this.tabName = tabName;
	}
	public Long getTabId() {
		return tabId;
	}
	public void setTabId(Long tabId) {
		this.tabId = tabId;
	}
	public int getSequence() {
		return sequence;
	}
	public void setSequence(int sequence) {
		this.sequence = sequence;
	}
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	public String getFieldId() {
		return fieldId;
	}
	public void setFieldId(String fieldId) {
		this.fieldId = fieldId;
	}
}
