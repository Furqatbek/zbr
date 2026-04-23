package com.fooddelivery.integration.restos.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Wrapper for all Restos API responses.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class RestosApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private String timestamp;
}
