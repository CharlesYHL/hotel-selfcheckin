package com.example.demo;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.AgentEventType;
import io.agentscope.core.event.TextBlockDeltaEvent;
import io.agentscope.core.event.ToolCallStartEvent;
import io.agentscope.core.message.UserMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

/**
 * Streaming demo: consume {@code Flux<AgentEvent>} and print text deltas as they arrive.
 *
 * <p>{@code streamEvents} is the reactive API - it never blocks, so it is safe to wrap in an SSE
 * endpoint. Only this {@code main()} subscribes with {@code .blockLast()} to keep the JVM alive
 * until the stream completes; cancelling the subscription would interrupt the agent.
 */
public final class StreamingDemo {

    private static final Logger LOG = LoggerFactory.getLogger(StreamingDemo.class);

    private static final String USER_ID = "demo-user";
    private static final String SESSION_ID = "demo-session-stream";

    private StreamingDemo() {
    }

    public static void main(String[] args) {
        if (!DemoAgent.hasApiKey()) {
            System.err.println(DemoAgent.API_KEY_ENV + " 未设置，无法调用 " + DemoAgent.MODEL_ID + "。");
            System.err.println("请先执行：export " + DemoAgent.API_KEY_ENV + "=<your-key>");
            System.exit(1);
        }

        String question = (args.length > 0)
                ? String.join(" ", args)
                : "先查一下上海现在的天气，再用一句话提醒我入住酒店的注意事项。";

        ReActAgent agent = DemoAgent.create();

        RuntimeContext ctx = RuntimeContext.builder()
                .userId(USER_ID)
                .sessionId(SESSION_ID)
                .put("request_id", "req-demo-stream-1")
                .build();

        System.out.println("用户: " + question);
        System.out.print("助手: ");

        // .blockLast() 仅允许出现在 main() / 测试中：等待事件流结束。
        agent.streamEvents(new UserMessage(question), ctx)
                .doOnNext(event -> {
                    if (event.getType() == AgentEventType.TEXT_BLOCK_DELTA) {
                        String delta = ((TextBlockDeltaEvent) event).getDelta();
                        if (delta != null && !delta.isEmpty()) {
                            System.out.print(delta);
                            System.out.flush();
                        }
                    } else if (event.getType() == AgentEventType.TOOL_CALL_START) {
                        System.out.println();
                        System.out.println("[tool] " + ((ToolCallStartEvent) event).getToolCallName());
                    }
                })
                .doOnError(error -> LOG.error("stream failed, session={}", SESSION_ID, error))
                .onErrorResume(error -> Flux.empty())
                .blockLast();

        System.out.println();
    }
}
