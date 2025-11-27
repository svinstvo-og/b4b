package svinstvo.b4b.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import svinstvo.b4b.config.OpenAIConfig;
import svinstvo.b4b.dto.OpenAIRequest;
import svinstvo.b4b.dto.OpenAIResponse;
import svinstvo.b4b.dto.ParsedTransaction;
import svinstvo.b4b.model.RawTransaction;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class OpenAIService {

    private final WebClient openAIWebClient;
    private final OpenAIConfig openAIConfig;
    private final ObjectMapper objectMapper;

    private static final String CATEGORIZATION_SYSTEM_PROMPT = """
            You are a financial parser. You will receive a list of raw strings. 
            Extract the item name, price, currency (default CZK if missing), and infer a category 
            (Overpriced-Food, Essential-Food, Transport, Rent, Fun, Tech, Utilities, Shopping, Health, Other).
            Be harsh on food categorizing, food budget is limited, eating outside is often too expensive. 
            Return ONLY a raw JSON array. No markdown formatting, no backticks, no explanations.
            
            Input Format: ID: <id> | Text: <text>
            
            Output Format:
            [
              {"id": 101, "item_name": "3 beers and a burger", "amount": 450, "currency": "CZK", "category": "Food"},
              {"id": 102, "item_name": "rent", "amount": 15000, "currency": "CZK", "category": "Rent"}
            ]
            """;

    private static final String ADVISOR_SYSTEM_PROMPT = """
            You are a tough but fair financial advisor. The user wants to save money.
            Analyze their spending and give 3 specific, actionable bullet points on where 
            they failed or succeeded. Be concise and direct. Focus on facts and numbers.
            """;

    public List<ParsedTransaction> parseTransactions(List<RawTransaction> rawTransactions) {
        try {
            String userPrompt = buildBatchPrompt(rawTransactions);

            OpenAIRequest request = OpenAIRequest.builder()
                    .model(openAIConfig.getModelMini())
                    .temperature(0.3)
                    .maxTokens(2000)
                    .messages(List.of(
                            OpenAIRequest.Message.builder()
                                    .role("system")
                                    .content(CATEGORIZATION_SYSTEM_PROMPT)
                                    .build(),
                            OpenAIRequest.Message.builder()
                                    .role("user")
                                    .content(userPrompt)
                                    .build()
                    ))
                    .build();

            log.info("Sending {} transactions to OpenAI for parsing", rawTransactions.size());

            OpenAIResponse response = openAIWebClient.post()
                    .uri("/chat/completions")
                    .body(Mono.just(request), OpenAIRequest.class)
                    .retrieve()
                    .bodyToMono(OpenAIResponse.class)
                    .block();

            if (response == null || response.getChoices().isEmpty()) {
                log.error("Empty response from OpenAI");
                return List.of();
            }

            String jsonContent = response.getChoices().get(0).getMessage().getContent().trim();

            // Remove markdown formatting if present
            jsonContent = jsonContent.replaceAll("```json\\s*", "").replaceAll("```\\s*$", "").trim();

            log.info("OpenAI response: {} tokens used", response.getUsage().getTotalTokens());
            log.debug("Parsed JSON: {}", jsonContent);

            List<ParsedTransaction> parsed = objectMapper.readValue(
                    jsonContent,
                    new TypeReference<List<ParsedTransaction>>() {}
            );

            return parsed;

        } catch (Exception e) {
            log.error("Error parsing transactions with OpenAI", e);
            throw new RuntimeException("Failed to parse transactions", e);
        }
    }

    public String generateFinancialAdvice(String spendingSummary, Double goalAmount) {
        try {
            String userPrompt = String.format("""
                    The user wants to save %.2f CZK this month.
                    
                    Current Spending Summary:
                    %s
                    
                    Provide 3 specific, actionable recommendations.
                    """, goalAmount, spendingSummary);

            OpenAIRequest request = OpenAIRequest.builder()
                    .model(openAIConfig.getModelFull())
                    .temperature(0.7)
                    .maxTokens(500)
                    .messages(List.of(
                            OpenAIRequest.Message.builder()
                                    .role("system")
                                    .content(ADVISOR_SYSTEM_PROMPT)
                                    .build(),
                            OpenAIRequest.Message.builder()
                                    .role("user")
                                    .content(userPrompt)
                                    .build()
                    ))
                    .build();

            log.info("Requesting financial advice from OpenAI");

            OpenAIResponse response = openAIWebClient.post()
                    .uri("/chat/completions")
                    .body(Mono.just(request), OpenAIRequest.class)
                    .retrieve()
                    .bodyToMono(OpenAIResponse.class)
                    .block();

            if (response == null || response.getChoices().isEmpty()) {
                return "Unable to generate advice at this time.";
            }

            return response.getChoices().get(0).getMessage().getContent();

        } catch (Exception e) {
            log.error("Error generating financial advice", e);
            return "Error generating advice. Please try again later.";
        }
    }

    private String buildBatchPrompt(List<RawTransaction> rawTransactions) {
        return rawTransactions.stream()
                .map(rt -> String.format("ID: %d | Text: %s", rt.getId(), rt.getMessageText()))
                .collect(Collectors.joining("\n"));
    }
}