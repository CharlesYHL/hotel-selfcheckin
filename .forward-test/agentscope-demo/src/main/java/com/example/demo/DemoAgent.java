package com.example.demo;

import io.agentscope.core.ReActAgent;
import io.agentscope.core.tool.Toolkit;

/**
 * Builds the demo agent.
 *
 * <p>The generated {@link ReActAgent} is stateless and thread-safe: build it once at start-up,
 * then pass a per-call {@link io.agentscope.core.agent.RuntimeContext} to every invocation instead
 * of creating a new agent per request.
 *
 * <p>The model is referenced by its {@code provider:model} string, which
 * {@link io.agentscope.core.model.ModelRegistry} resolves through the DashScope extension
 * ({@code agentscope-extensions-model-dashscope} on the classpath, key from
 * {@code DASHSCOPE_API_KEY}). If you need a custom base URL or formatter, construct
 * {@code io.agentscope.extensions.model.dashscope.DashScopeChatModel} explicitly - the provider
 * classes live in {@code io.agentscope.extensions.model.<provider>}, never in
 * {@code io.agentscope.core.model}.
 */
public final class DemoAgent {

    /** Agent identity, also used as the fallback session slot name. */
    public static final String AGENT_NAME = "hotel-demo-agent";

    /** {@code provider:model} id resolved by {@code ModelRegistry}. */
    public static final String MODEL_ID = "dashscope:qwen-plus";

    /** Environment variable read by the DashScope provider when the model is created. */
    public static final String API_KEY_ENV = "DASHSCOPE_API_KEY";

    private static final String SYS_PROMPT =
            """
            你是一个酒店自助入住系统的示例助手。
            回答保持简洁（不超过三句话），并使用中文。
            涉及天气或时间时必须调用工具，不要凭记忆编造。
            工具返回错误时，说明原因并给出可行的替代建议。""";

    private DemoAgent() {
    }

    /**
     * Reports whether the provider credential is available, so the CLI entry points can fail fast
     * with a readable message instead of a stack trace. Secrets are only ever read from the
     * environment, never from code or config files.
     *
     * @return {@code true} when {@code DASHSCOPE_API_KEY} is set to a non-blank value
     */
    public static boolean hasApiKey() {
        String key = System.getenv(API_KEY_ENV);
        return key != null && !key.isBlank();
    }

    /**
     * Creates a ready-to-use agent with the demo tools and request timing middleware.
     *
     * @return a reusable, thread-safe agent instance
     */
    public static ReActAgent create() {
        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(new DemoTools());

        return ReActAgent.builder()
                .name(AGENT_NAME)
                .sysPrompt(SYS_PROMPT)
                .model(MODEL_ID)
                .toolkit(toolkit)
                .middleware(new LoggingMiddleware())
                .maxIters(10)
                .build();
    }
}
