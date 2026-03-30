package com.vycepay.common.choicebank;

import com.vycepay.common.choicebank.dto.ChoiceBankHttpTraceDto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChoiceBankHttpAuditStoreTest {

    @Test
    void ringBufferDropsOldestWhenOverMax() {
        ChoiceBankHttpAuditStore store = new ChoiceBankHttpAuditStore(3);
        for (int i = 0; i < 5; i++) {
            store.record("p" + i, "req" + i, "{}", "{}", "00000", "ok", null);
        }
        List<ChoiceBankHttpTraceDto> traces = store.getRecentTraces();
        assertEquals(3, traces.size());
        assertEquals("p4", traces.get(0).getPath());
        assertEquals("p3", traces.get(1).getPath());
        assertEquals("p2", traces.get(2).getPath());
    }
}
