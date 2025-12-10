package org.com.drop.global.rsData;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class RsData<T> {

	private String status;
	private String code;
	private String message;
	private T data;

}