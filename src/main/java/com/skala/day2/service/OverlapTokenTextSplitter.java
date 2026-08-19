package com.skala.day2.service;

import java.util.ArrayList;
import java.util.List;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingType;
import com.knuddels.jtokkit.api.IntArrayList;
import org.springframework.ai.transformer.splitter.TextSplitter;

/**
 * Step 5의 F 실험을 위한 토큰 기반 겹침 분할기.
 *
 * <p>Spring AI 2.0의 {@code TokenTextSplitter.Builder}에는 overlap 설정이 없으므로,
 * 같은 기본 토크나이저({@code CL100K_BASE})를 사용해 슬라이딩 윈도우 방식으로 나눈다.
 *
 * <pre>
 * chunkSize=10, overlap=20%라면
 * 청크 1: token 0  ~ 9
 * 청크 2: token 8  ~ 17  (앞의 2토큰이 겹침)
 * 청크 3: token 16 ~ 25
 * </pre>
 *
 * <p>이 클래스는 F 실험에서만 사용한다. overlap이 0이면 기존 {@code TokenTextSplitter}를 사용해
 * A~E의 기준 조건을 바꾸지 않는다.
 */
final class OverlapTokenTextSplitter extends TextSplitter {

    private static final EncodingType ENCODING_TYPE = EncodingType.CL100K_BASE;

    private final Encoding encoding;
    private final int chunkSize;
    private final int overlapTokens;
    private final int stepSize;

    OverlapTokenTextSplitter(int chunkSize, int overlapPercent) {
        if (chunkSize < 1) {
            throw new IllegalArgumentException("chunkSize는 1 이상이어야 합니다.");
        }
        if (overlapPercent < 1 || overlapPercent >= 100) {
            throw new IllegalArgumentException("overlapPercent는 1 이상 100 미만이어야 합니다.");
        }

        this.encoding = Encodings.newDefaultEncodingRegistry().getEncoding(ENCODING_TYPE);
        this.chunkSize = chunkSize;
        this.overlapTokens = Math.max(1, chunkSize * overlapPercent / 100);
        this.stepSize = chunkSize - this.overlapTokens;
    }

    @Override
    protected List<String> splitText(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        IntArrayList tokens = this.encoding.encode(text);
        if (tokens.isEmpty()) {
            return List.of();
        }

        List<String> chunks = new ArrayList<>();

        // stepSize만큼 이동하므로 바로 전 청크의 마지막 overlapTokens개가 다음 청크에 다시 포함된다.
        for (int start = 0; start < tokens.size(); start += this.stepSize) {
            int end = Math.min(start + this.chunkSize, tokens.size());
            // trim()을 호출하면 청크 앞의 공백 토큰이 제거되어 토큰 경계와 겹침이 달라질 수 있다.
            // 디코딩 결과를 그대로 보존하고, 비어 있는지 여부만 isBlank()로 검사한다.
            String chunk = this.encoding.decode(copyRange(tokens, start, end));

            if (!chunk.isBlank()) {
                chunks.add(chunk);
            }

            // 마지막 토큰까지 포함했으면 중복된 마지막 청크를 만들지 않고 종료한다.
            if (end == tokens.size()) {
                break;
            }
        }

        return chunks;
    }

    /** jtokkit 토큰 목록에서 현재 청크 구간만 복사한다. */
    private IntArrayList copyRange(IntArrayList tokens, int start, int end) {
        IntArrayList range = new IntArrayList(end - start);
        for (int index = start; index < end; index++) {
            range.add(tokens.get(index));
        }
        return range;
    }

    /** 단위 테스트에서 실제 겹침 토큰 수를 확인하기 위한 package-private 접근자다. */
    int overlapTokens() {
        return this.overlapTokens;
    }
}
