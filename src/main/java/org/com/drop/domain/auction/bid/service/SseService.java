package org.com.drop.domain.auction.bid.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
public class SseService {

	private final Map<Long, List<SseEmitter>> sseEmitters = new ConcurrentHashMap<>();

	public SseEmitter subscribe(Long auctionId) {
		// 타임아웃 설정 (기본 30초는 너무 짧으므로 1시간 등으로 넉넉하게 설정)
		SseEmitter emitter = new SseEmitter(60 * 60 * 1000L);

		// 리스트에 추가
		sseEmitters.computeIfAbsent(auctionId, k -> new CopyOnWriteArrayList<>()).add(emitter);

		// 연결 종료/타임아웃 시 리스트에서 제거하는 콜백 등록
		emitter.onCompletion(() -> removeEmitter(auctionId, emitter));
		emitter.onTimeout(() -> removeEmitter(auctionId, emitter));
		emitter.onError((e) -> removeEmitter(auctionId, emitter));

		// 503 에러 방지를 위한 더미 데이터 전송
		try {
			emitter.send(SseEmitter.event().name("connect").data("connected!"));
		} catch (IOException e) {
			removeEmitter(auctionId, emitter);
		}

		return emitter;
	}

	public void notifyHighestPrice(Long auctionId, Long price) {
		List<SseEmitter> emitters = sseEmitters.get(auctionId);

		if (emitters != null) {
			for (SseEmitter emitter : emitters) {
				try {
					emitter.send(SseEmitter.event()
						.name("highestPrice") // 클라이언트에서 수신할 이벤트 이름
						.data(price));        // 보낼 데이터 (필요하면 DTO로 감싸서 JSON 전송)
				} catch (IOException e) {
					// 전송 실패한 emitter는 제거
					removeEmitter(auctionId, emitter);
				}
			}
		}
	}

	private void removeEmitter(Long auctionId, SseEmitter emitter) {
		List<SseEmitter> emitters = sseEmitters.get(auctionId);
		if (emitters != null) {
			emitters.remove(emitter);
		}
	}
}
