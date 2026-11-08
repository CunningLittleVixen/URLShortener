package io.github.cunninglittlevixen.urlshortener.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class UserIdParserTest {

    private final UserIdParser parser = new UserIdParser();

    @Test
    void parsesValidUuidWithSpaces() {
        UUID id = UUID.randomUUID();

        assertEquals(id, parser.parse("  " + id + " "));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "abc", "1-1-1-1-1", "123e4567-e89b-12d3-a456-42661417400g"})
    void rejectsInvalidUuid(String text) {
        assertThrows(ServiceException.class, () -> parser.parse(text));
    }
}
