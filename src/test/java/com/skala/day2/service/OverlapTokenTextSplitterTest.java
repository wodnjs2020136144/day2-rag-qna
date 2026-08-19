package com.skala.day2.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingType;
import com.knuddels.jtokkit.api.IntArrayList;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

import static org.assertj.core.api.Assertions.assertThat;

/** F 실험의 토큰 겹침이 실제로 적용되는지 외부 API 호출 없이 확인한다. */
class OverlapTokenTextSplitterTest {

    private final Encoding encoding = Encodings.newDefaultEncodingRegistry()
            .getEncoding(EncodingType.CL100K_BASE);

    @Test
    void 인접한_청크는_지정한_토큰만큼_겹친다() {
        String text = IntStream.range(0, 200)
                .mapToObj(number -> "token" + number)
                .collect(Collectors.joining(" "));

        OverlapTokenTextSplitter splitter = new OverlapTokenTextSplitter(40, 20);
        List<Document> chunks = splitter.apply(List.of(
                new Document(text, Map.of("source", "test.md"))));

        assertThat(chunks.size()).isGreaterThan(1);

        IntArrayList first = encoding.encode(chunks.get(0).getText());
        IntArrayList second = encoding.encode(chunks.get(1).getText());
        int overlap = splitter.overlapTokens();

        // 첫 청크의 마지막 20% 토큰과 다음 청크의 첫 20% 토큰이 같아야 한다.
        assertThat(lastTokens(first, overlap))
                .containsExactly(firstTokens(second, overlap));

        // TextSplitter가 원래 문서의 source 메타데이터도 각 청크에 복사해야 한다.
        assertThat(chunks)
                .allSatisfy(chunk -> assertThat(chunk.getMetadata().get("source"))
                        .isEqualTo("test.md"));
    }

    private Integer[] lastTokens(IntArrayList tokens, int count) {
        Integer[] result = new Integer[count];
        for (int index = 0; index < count; index++) {
            result[index] = tokens.get(tokens.size() - count + index);
        }
        return result;
    }

    private Integer[] firstTokens(IntArrayList tokens, int count) {
        Integer[] result = new Integer[count];
        for (int index = 0; index < count; index++) {
            result[index] = tokens.get(index);
        }
        return result;
    }
}
