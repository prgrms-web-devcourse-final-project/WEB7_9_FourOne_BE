package org.com.drop.domain.auction.bid.service;

import java.io.IOException;
import java.util.ArrayList;
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

		sseEmitters.computeIfAbsent(auctionId, k -> new CopyOnWriteArrayList<>()).add(emitter);

		emitter.onCompletion(() -> removeEmitter(auctionId, emitter));
		emitter.onTimeout(() -> removeEmitter(auctionId, emitter));
		emitter.onError((e) -> removeEmitter(auctionId, emitter));

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
			List<SseEmitter> emittersCopy = new ArrayList<>();
			for (SseEmitter emitter : emittersCopy) {
				try {
					emitter.send(SseEmitter.event()
						.name("highestPrice")
						.data(price));
				} catch (IOException e) {
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
