package org.com.drop.global.rsdata;

import lombok.Getter;

@Getter
public class RsData<T> {

	private final String code = "SUCCESS";
	private final String status = "200";
	private final String message = "요청을 성공적으로 처리했습니다.";
	private final T data;
	public RsData(T data) {
		this.data = data;
	}
}
