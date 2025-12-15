package org.com.drop.global.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;

@Getter
public enum ErrorCode {

	//public
	INVALID_PARAMETER("900", HttpStatus.BAD_REQUEST, "잘못된 요청 데이터입니다."),
	INVALID_REPRESENTATION("901", HttpStatus.BAD_REQUEST, "잘못된 표현 입니다."),
	INVALID_FILE_PATH("902", HttpStatus.BAD_REQUEST, "잘못된 파일 경로 입니다."),
	INVALID_OPTIONAL_ISPRESENT("903", HttpStatus.BAD_REQUEST, "해당 값이 존재하지 않습니다."),
	INVALID_CHECK("904", HttpStatus.BAD_REQUEST, "해당 값이 유효하지 않습니다."),
	INVALID_AUTHENTICATION("905", HttpStatus.BAD_REQUEST, "잘못된 인증입니다."),
	INTERNAL_SERVER_ERROR("906", HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요."),
	VALIDATION_ERROR("907", HttpStatus.BAD_REQUEST, "요청 값이 올바르지 않습니다."),

	//auth
	AUTH_MISSING_EMAIL("1000", HttpStatus.BAD_REQUEST, "이메일은 필수 입력 항목입니다."),
	AUTH_MISSING_PASSWORD("1001", HttpStatus.BAD_REQUEST, "비밀번호는 필수 입력 항목입니다."),
	AUTH_MISSING_NICKNAME("1002", HttpStatus.BAD_REQUEST, "닉네임은 필수 입력 항목입니다."),
	AUTH_INVALID_EMAIL("1003", HttpStatus.BAD_REQUEST, "이메일 형식이 올바르지 않습니다."),
	AUTH_INVALID_PASSWORD("1004", HttpStatus.BAD_REQUEST, "비밀번호 규칙에 맞지 않습니다. (8~30자, 영소문자+숫자+특수문자 포함)"),
	AUTH_INVALID_NICKNAME("1005", HttpStatus.BAD_REQUEST, "닉네임 규칙에 맞지 않습니다. (3~10자)"),
	AUTH_DUPLICATE_EMAIL("1006", HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
	AUTH_DUPLICATE_NICKNAME("1007", HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다."),
	AUTH_EMAIL_SEND_LIMIT("1008", HttpStatus.TOO_MANY_REQUESTS, "이메일 인증 요청 횟수를 초과했습니다. 잠시 후 다시 시도해 주세요."),
	AUTH_EMAIL_SEND_FAILED("1009", HttpStatus.INTERNAL_SERVER_ERROR, "인증 메일 발송에 실패했습니다.잠시 후 다시 시도해 주세요."),
	AUTH_CODE_MISMATCH("1010", HttpStatus.BAD_REQUEST, "인증 코드가 일치하지 않습니다."),
	AUTH_CODE_EXPIRED("1011", HttpStatus.GONE, "인증 코드가 만료되었습니다. 인증 요청을 다시 해주세요."),
	AUTH_UNAUTHORIZED("1012", HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 일치하지 않습니다."),
	AUTH_TOKEN_MISSING("1013", HttpStatus.UNAUTHORIZED, "인증 정보(토큰)가 누락되었습니다."),
	AUTH_TOKEN_INVALID("1014", HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다. 다시 로그인해 주세요."),
	AUTH_ACCESS_TOKEN_CREATE_FAILED("1015", HttpStatus.INTERNAL_SERVER_ERROR, "새로운 액세스 토큰 발급에 실패했습니다. 서버 오류가 발생했습니다."),
	AUTH_ACCESS_DENIED("1016", HttpStatus.FORBIDDEN, "접근 권한이 없습니다."),
	AUTH_PASSWORD_MISMATCH("1017", HttpStatus.UNAUTHORIZED, "비밀번호가 일치하지 않습니다."),
	AUTH_NOT_VERIFIED("1018", HttpStatus.FORBIDDEN, "이메일 인증이 완료되지 않았습니다. 인증을 먼저 진행해주세요."),

	//users
	USER_NOT_FOUND("1100", HttpStatus.NOT_FOUND, "해당 사용자를 찾을 수 없습니다."),
	USER_BOOKMARK_NOT_FOUND("1101", HttpStatus.NOT_FOUND, "찜한 상품이 아닙니다."),
	USER_ALREADY_BOOKMARKED("1102", HttpStatus.BAD_REQUEST, "이미 찜한 상품입니다."),
	USER_FORBIDDEN("1103", HttpStatus.FORBIDDEN, "관리자 권한이 필요합니다."),
	USER_NICKNAME_CONFLICT("1104", HttpStatus.BAD_REQUEST, "닉네임이 이미 사용 중입니다."),
	USER_ALREADY_EXISTS("1105", HttpStatus.CONFLICT, "이미 존재하는 회원입니다"),
	USER_INACTIVE_USER("1106", HttpStatus.FORBIDDEN, "권한이 없는 사용자입니다"),
	USER_PAYMENT_UNAUTHORIZED("1107", HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."),
	USER_INVALID_PG_TOKEN("1108", HttpStatus.BAD_REQUEST, "PG 토큰이 유효하지 않거나 만료되었습니다."),
	USER_PAYMENT_NOT_FOUND("1109", HttpStatus.NOT_FOUND, "등록된 결제 수단을 찾을 수 없습니다."),
	USER_UNAUTHORIZED("1110", HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."),
	USER_HAS_ACTIVE_AUCTIONS("1111", HttpStatus.BAD_REQUEST, "현재 진행 중인 경매 또는 미완료된 거래가 있어 탈퇴할 수 없습니다."),
	USER_INVALID_STATUS_PARAMUSER_HAS_ACTIVE_AUCTIONS("1112", HttpStatus.BAD_REQUEST, "status 값이 enum에 없습니다."),
	USER_PAGE_OUT_OF_RANGE("1113", HttpStatus.BAD_REQUEST, "요청한 page가 totalPages를 초과합니다."),

	//products
	PRODUCT_NOT_FOUND("1200", HttpStatus.NOT_FOUND, "요청하신 상품 ID를 찾을 수 없습니다."),
	PRODUCT_ALREADY_BOOKMARKED("1201", HttpStatus.CONFLICT, "이미 찜한 상품입니다."),
	PRODUCT_BOOKMARK_NOT_FOUND("1202", HttpStatus.NOT_FOUND, "찜한 상품이 아닙니다."),
	PRODUCT_INVALID_QUESTION("1203", HttpStatus.BAD_REQUEST, "질문이 입력되지 않았습니다."),
	PRODUCT_INVALID_ANSWER("1204", HttpStatus.BAD_REQUEST, "답변이 입력되지 않았습니다."),
	PRODUCT_INVALID_PRODUCT_NAME("1205", HttpStatus.BAD_REQUEST, "상품명은 필수 항목 입니다."),
	PRODUCT_INVALID_PRODUCT_DESCRIPTION("1206", HttpStatus.BAD_REQUEST, "상품 설명은 필수 항목 입니다."),
	PRODUCT_INVALID_PRODUCT_CATEGORY("1207", HttpStatus.BAD_REQUEST, "상품 카테고리는 필수 항목 입니다."),
	PRODUCT_INVALID_PRODUCT_SUB_CATEGORY("1208", HttpStatus.BAD_REQUEST, "상품 하위 카테고리는 필수 항목 입니다."),
	PRODUCT_INVALID_PRODUCT_IMAGE("1209", HttpStatus.BAD_REQUEST, "상품 이미지는 필수 항목 입니다."),
	PRODUCT_QUESTION_NOT_FOUND("1210", HttpStatus.NOT_FOUND, "질문을 찾을 수 없습니다."),
	PRODUCT_ANSWER_NOT_FOUND("1211", HttpStatus.NOT_FOUND, "답변을 찾을 수 없습니다."),
	PRODUCT_ALREADY_ON_AUCTION("1212", HttpStatus.CONFLICT, "이미 경매가 시작된 상품입니다."),

	//payments
	PAY_ALREADY_PAID("1300", HttpStatus.CONFLICT, "이미 결제한 상품입니다."),
	PAY_INVALID_CARD_EXPIRATION("1301", HttpStatus.BAD_REQUEST, "카드 정보를 다시 확인해주세요."),
	PAY_EXCEED_MAX_PAYMENT_AMOUNT("1302", HttpStatus.BAD_REQUEST, "카드 한도 초과"),
	PAY_NOT_AVAILABLE_PAYMENT("1303", HttpStatus.BAD_REQUEST, "결제 불가능한 시간대 입니다."),
	PAY_REJECT_CARD_PAYMENT("1304", HttpStatus.BAD_REQUEST, "한도초과 혹은 잔액 부족으로 결제 실패했습니다"),
	PAY_INVALID_PASSWORD("1305", HttpStatus.BAD_REQUEST, "결제 비밀번호가 일치하지 않습니다."),
	PAY_NOT_FOUND_PAYMENT("1306", HttpStatus.NOT_FOUND, "존재하지 않는 결제 정보입니다."),
	PAY_ALREADY_PROCESSED("1307", HttpStatus.CONFLICT, "이미 처리된 결제 입니다."),
	PAY_ALREADY_CANCELED_PAYMENT("1308", HttpStatus.CONFLICT, "이미 취소된 결제입니다."),
	PAY_NOT_FOUND_METHOD("1309", HttpStatus.NOT_FOUND, "지원되지 않는 결제 수단입니다."),

	//purchases
	PURCHASE_INVALID_STATUS("1400", HttpStatus.CONFLICT, "구매 확정이 불가능한 상태입니다.(현재 상태: READY)"),

	//settlement
	SETTLEMENT_INVALID_SELLER_ACCOUNT("1500", HttpStatus.BAD_REQUEST, "판매자의 계좌 정보가 유효하지 않습니다."),
	SETTLEMENT_PAYOUT_FAILED("1501", HttpStatus.BAD_REQUEST, "외부 뱅킹 시스템 연동 실패했습니다. 잠시 후 재시도 해주세요."),

	//auctions
	AUCTION_NOT_FOUND("1600", HttpStatus.NOT_FOUND, "요청하신 상품 ID를 찾을 수 없습니다."),
	AUCTION_BID_AMOUNT_TOO_LOW("1601", HttpStatus.BAD_REQUEST, "입찰 금액이 현재 최고가보다 낮거나 최소 입찰 단위를 충족하지 못했습니다."),
	AUCTION_ALREADY_ENDED("1603", HttpStatus.CONFLICT, "이미 경매가 종료되었거나, 즉시 구매가 완료되었습니다."),
	AUCTION_SELF_BIDDING_NOT_ALLOWED("1604", HttpStatus.BAD_REQUEST, "본인이 판매하는 상품에는 입찰할 수 없습니다."),
	AUCTION_INVALID_CATEGORY("1605", HttpStatus.BAD_REQUEST, "유효하지 않은 카테고리입니다.");

	private final String code;
	private final HttpStatus status;
	private final String message;
	private final String field;


	ErrorCode(String code, HttpStatus status, String message) {
		this(code, status, message, null);
	}

	ErrorCode(String code, HttpStatus status, String message, String field) {
		this.code = code;
		this.status = status;
		this.message = message;
		this.field = field;
	}

	public ServiceException serviceException(String message, Object... args) {
		return new ServiceException(this, message, args);
	}
}
