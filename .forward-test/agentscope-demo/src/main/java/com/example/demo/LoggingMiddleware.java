package com.example.demo;

import io.agentscope.core.agent.Agent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.AgentEvent;
import io.agentscope.core.middleware.MiddlewareBase;
import io.agentscope.core.middleware.ModelCallInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import java.util.function.Function;

/**
 * Middleware that measures every raw model call.
 *
 * <p>{@code onModelCall} is the onion-style hook closest to the provider API: {@code next.apply(input)}
 * performs the call and returns the event stream, so wrapping it with {@code doFinally} yields the
 * end-to-end latency for success, error and cancellation alike.
 *
 * <p>Middleware instances are shared by all sessions: per-request data belongs in
 * {@link RuntimeContext}, never in an instance field.
 */
public class LoggingMiddleware implements MiddlewareBase {

    private static final Logger LOG = LoggerFactory.getLogger(LoggingMiddleware.class);

    @Override
    public Flux<AgentEvent> onModelCall(
            Agent agent,
            RuntimeContext ctx,
            ModelCallInput input,
            Function<ModelCallInput, Flux<AgentEvent>> next) {

        // Local to this invocation: safe because the lambda captures it, not the middleware.
        long startNanos = System.nanoTime();

        String agentName = (agent == null) ? "-" : agent.getName();
        String sessionId = (ctx == null) ? "-" : String.valueOf(ctx.getSessionId());
        int messageCount = (input == null || input.messages() == null) ? 0 : input.messages().size();

        LOG.debug("[model-call] start agent={} session={} messages={}", agentName, sessionId, messageCount);

        return next.apply(input)
                .doFinally(signal -> LOG.info(
                        "[model-call] agent={} session={} messages={} elapsed={}ms signal={}",
                        agentName,
                        sessionId,
                        messageCount,
                        (System.nanoTime() - startNanos) / 1_000_000L,
                        signal));
    }
}
