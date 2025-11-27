package svinstvo.b4b.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParsedTransaction {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("item_name")
    private String itemName;

    @JsonProperty("amount")
    private BigDecimal amount;

    @JsonProperty("currency")
    private String currency;

    @JsonProperty("category")
    private String category;

    @JsonProperty("sentiment_tag")
    private String sentimentTag;
}