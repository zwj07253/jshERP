package com.jsh.erp.utils;

public class BaseResponseInfo {
	public int code;
	public Object data;
	
	public BaseResponseInfo() {
		code = 200;
		data = null;
	}
}
