package com.bg.haitao.common;

import java.util.List;

public class Result<T> {
	private int status;
	private String msg;
	private List<T> extramsg;
	
	public Result(int status, String msg) {
		this.status = status;
		this.msg = msg;
	}
	
	public Result(int status, String msg, List<T> extramsg) {
		this.status = status;
		this.msg = msg;
		this.extramsg = extramsg;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}

	public List<T> getExtramsg() {
		return extramsg;
	}

	public void setExtramsg(List<T> extramsg) {
		this.extramsg = extramsg;
	}
}
