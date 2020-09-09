package com.example.social.payload;

import java.util.List;

public class FacebookAuthResponse {
	private Data data;
	
	public Data getData() {
		return data;
	}

	public void setData(Data data) {
		this.data = data;
	}

	public FacebookAuthResponse() {
		super();
		// TODO Auto-generated constructor stub
	}

	public FacebookAuthResponse(Data data) {
		super();
		this.data = data;
	}

	public class Data {
		private String app_id;
		private String type;
		private String application;
		private Integer data_access_expires_at;
		private Integer expires_at;
		private Boolean is_valid;
		private Integer issued_at;
		private List<String> scopes;
		private String user_id;
		public String getApp_id() {
			return app_id;
		}
		public void setApp_id(String app_id) {
			this.app_id = app_id;
		}
		public String getType() {
			return type;
		}
		public void setType(String type) {
			this.type = type;
		}
		public String getApplication() {
			return application;
		}
		public void setApplication(String application) {
			this.application = application;
		}
		public Integer getData_access_expires_at() {
			return data_access_expires_at;
		}
		public void setData_access_expires_at(Integer data_access_expires_at) {
			this.data_access_expires_at = data_access_expires_at;
		}
		public Integer getExpires_at() {
			return expires_at;
		}
		public void setExpires_at(Integer expires_at) {
			this.expires_at = expires_at;
		}
		public Boolean getIs_valid() {
			return is_valid;
		}
		public void setIs_valid(Boolean is_valid) {
			this.is_valid = is_valid;
		}
		public Integer getIssued_at() {
			return issued_at;
		}
		public void setIssued_at(Integer issued_at) {
			this.issued_at = issued_at;
		}
		public List<String> getScopes() {
			return scopes;
		}
		public void setScopes(List<String> scopes) {
			this.scopes = scopes;
		}
		public String getUser_id() {
			return user_id;
		}
		public void setUser_id(String user_id) {
			this.user_id = user_id;
		}
		public Data(String app_id, String type, String application, Integer data_access_expires_at, Integer expires_at,
				Boolean is_valid, Integer issued_at, List<String> scopes, String user_id) {
			super();
			this.app_id = app_id;
			this.type = type;
			this.application = application;
			this.data_access_expires_at = data_access_expires_at;
			this.expires_at = expires_at;
			this.is_valid = is_valid;
			this.issued_at = issued_at;
			this.scopes = scopes;
			this.user_id = user_id;
		}
		public Data() {
			super();
			// TODO Auto-generated constructor stub
		}
		
	}
}
