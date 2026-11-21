package com.example.demo;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.AssistantMessage;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.UserMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Blocking (one-shot) demo: send a question, wait for the full ReAct loop, print the final text.
 *
 * <p>This is the only kind of place where {@code .block()} is allowed - {@code main()} / tests.
 * Library or service code must return {@code Mono}/{@code Flux} and let the caller subscribe.
 */
public final class DemoMain {

    private static final Logger LOG = LoggerFactory.getLogger(DemoMain.class);

    private static final String USER_ID = "demo-user";
    private static final String SESSION_ID = "demo-session-1";

    private DemoMain() {
    }

    public static void main(String[] args) {
        if (!DemoAgent.hasApiKey()) {
            System.err.println(DemoAgent.API_KEY_ENV + " 未设置，无法调用 " + DemoAgent.MODEL_ID + "。");
            System.err.println("请先执行：export " + DemoAgent.API_KEY_ENV + "=<your-key>");
            System.exit(1);
        }

        String question = (args.length > 0)
                ? String.join(" ", args)
                : "请查一下杭州现在的天气，并告诉我现在的北京时间。";

        ReActAgent agent = DemoAgent.create();

        RuntimeContext ctx = RuntimeContext.builder()
                .userId(USER_ID)
                .sessionId(SESSION_ID)
                .put("request_id", "req-demo-1")
                .build();

        System.out.println("用户: " + question);

        // .block() 仅允许出现在 main() / 测试中：示例 CLI 需要一个同步结果。
        Msg reply = agent.call(List.of(new UserMessage(question)), ctx)
                .onErrorResume(error -> {
                    LOG.error("agent call failed, session={}", SESSION_ID, error);
                    return Mono.just(new AssistantMessage("（示例）调用失败：" + error.getMessage()));
                })
                .block();

        System.out.println("助手: " + (reply == null ? "<无回复>" : reply.getTextContent()));
    }
}
