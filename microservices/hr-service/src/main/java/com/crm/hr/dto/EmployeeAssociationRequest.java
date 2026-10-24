package com.crm.hr.dto;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.io.IOException;

/** New API uses canonical strings; legacy numeric DTOs remain on their existing routes. */
public record EmployeeAssociationRequest(
        @NotNull @Pattern(regexp = "[1-9][0-9]{0,18}")
        @JsonDeserialize(using = IdentifierString.class) String userId,
        @NotNull @Pattern(regexp = "[1-9][0-9]{0,18}")
        @JsonDeserialize(using = IdentifierString.class) String employeeId) {
    public static class IdentifierString extends JsonDeserializer<String> {
        @Override
        public String deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            if (!parser.hasToken(JsonToken.VALUE_STRING)) {
                throw JsonMappingException.from(parser, "Identifier must be a canonical decimal string");
            }
            return parser.getText();
        }
    }
}
