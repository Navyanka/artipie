/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 artipie.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.artipie;

import com.artipie.http.Headers;
import com.artipie.http.Response;
import com.artipie.http.rq.RequestLine;
import com.artipie.http.rq.RqMethod;
import com.artipie.http.rs.RsStatus;
import com.artipie.http.rs.RsWithStatus;
import com.artipie.metrics.memory.InMemoryMetrics;
import io.reactivex.Flowable;
import java.util.concurrent.CompletableFuture;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link ResponseMetricsSlice}.
 *
 * @since 0.9
 */
class ResponseMetricsSliceTest {

    /**
     * Metrics collected in tests.
     */
    private InMemoryMetrics metrics;

    @BeforeEach
    public void setUp() {
        this.metrics = new InMemoryMetrics();
    }

    @Test
    public void shouldReportSuccessResponse() {
        this.send(RqMethod.GET, new RsWithStatus(RsStatus.OK));
        MatcherAssert.assertThat(
            this.metrics.counter("get.success").value(),
            new IsEqual<>(1L)
        );
    }

    @Test
    public void shouldReportErrorResponse() {
        this.send(RqMethod.POST, new RsWithStatus(RsStatus.INTERNAL_ERROR));
        MatcherAssert.assertThat(
            this.metrics.counter("post.error").value(),
            new IsEqual<>(1L)
        );
    }

    private void send(final RqMethod method, final Response response) {
        new ResponseMetricsSlice(
            (rqline, rqheaders, rqbody) -> response,
            this.metrics
        ).response(
            new RequestLine(method.value(), "/file.txt").toString(),
            Headers.EMPTY,
            Flowable.empty()
        ).send(
            (rsstatus, rsheaders, rsbody) -> CompletableFuture.allOf()
        ).toCompletableFuture().join();
    }
}
