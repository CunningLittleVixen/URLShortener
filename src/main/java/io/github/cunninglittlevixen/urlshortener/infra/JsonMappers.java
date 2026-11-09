package io.github.cunninglittlevixen.urlshortener.infra;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import tools.jackson.databind.json.JsonMapper;

public final class JsonMappers {

    private JsonMappers() {
    }

    public static JsonMapper create() {
        return JsonMapper.builder()
            .changeDefaultVisibility(visibility ->
                visibility.withIsGetterVisibility(JsonAutoDetect.Visibility.NONE))
            .build();
    }
}
