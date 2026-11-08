package com.undercontroll.infrastructure.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.undercontroll.domain.model.LabelReading;

@JsonIgnoreProperties(ignoreUnknown = true)
public record LabelReadingJson(
        @JsonProperty("is_label") Boolean isLabel,
        @JsonProperty("type") String type,
        @JsonProperty("brand") String brand,
        @JsonProperty("model") String model,
        @JsonProperty("serial") String serial,
        @JsonProperty("voltage") String voltage,
        @JsonProperty("confidence") Double confidence
) {

    public LabelReading toDomain() {
        return new LabelReading(Boolean.TRUE.equals(isLabel), type, brand, model, serial, voltage,
                confidence == null ? 0.5 : confidence);
    }
}
