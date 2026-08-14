package com.nanotech.flux_pro_backend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Crée le {@link ChatClient} assistant après l'auto-config OpenAI.
 * Évite {@code @ConditionalOnBean(ChatModel)} qui peut s'évaluer trop tôt
 * et laisser l'assistant « enabled » sans bean ChatClient.
 */
@Configuration
@EnableConfigurationProperties(AssistantProperties.class)
@AutoConfigureAfter(name = "org.springframework.ai.model.openai.autoconfigure.OpenAiChatAutoConfiguration")
@Slf4j
public class AssistantConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "fluxpro.assistant", name = "enabled", havingValue = "true")
    ChatClient assistantChatClient(ObjectProvider<ChatModel> chatModels) {
        ChatModel chatModel = chatModels.getIfAvailable();
        if (chatModel == null) {
            throw new IllegalStateException(
                    "Assistant activé mais aucun ChatModel OpenAI. "
                            + "Vérifier spring.ai.model.chat=openai et spring.ai.openai.api-key "
                            + "dans application-secrets.properties");
        }
        log.info("Assistant IA : ChatClient prêt ({})", chatModel.getClass().getSimpleName());
        return ChatClient.create(chatModel);
    }
}
