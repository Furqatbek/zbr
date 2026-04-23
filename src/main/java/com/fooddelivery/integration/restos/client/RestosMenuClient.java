package com.fooddelivery.integration.restos.client;

import com.fooddelivery.common.exception.BusinessException;
import com.fooddelivery.integration.restos.dto.RestosApiResponse;
import com.fooddelivery.integration.restos.dto.RestosCategory;
import com.fooddelivery.integration.restos.dto.RestosProduct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;

/**
 * HTTP client for the external Restos restaurant system API.
 * Calls all 6 menu-related endpoints.
 */
@Component
@Slf4j
public class RestosMenuClient {

    private final RestTemplate restTemplate;

    public RestosMenuClient(@Qualifier("restosRestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Endpoint #1: GET /api/v1/customer/public/restaurants/{restaurantId}/categories
     * Browse categories only (no products nested).
     */
    public List<RestosCategory> fetchCategories(String baseUrl, Long restaurantId, String apiKey) {
        String url = baseUrl + "/api/v1/customer/public/restaurants/" + restaurantId + "/categories";
        log.info("Fetching categories from Restos: {}", url);

        RestosApiResponse<List<RestosCategory>> response = executeGet(
                url, apiKey, new ParameterizedTypeReference<>() {});

        List<RestosCategory> categories = extractData(response, url);
        log.info("Fetched {} categories from Restos for restaurant {}", categories.size(), restaurantId);
        return categories;
    }

    /**
     * Endpoint #2: GET /api/v1/customer/public/categories/{categoryId}/products
     * Get products for a specific category.
     */
    public List<RestosProduct> fetchProductsByCategory(String baseUrl, Long categoryId, String apiKey) {
        String url = baseUrl + "/api/v1/customer/public/categories/" + categoryId + "/products";
        log.info("Fetching products for category {} from Restos: {}", categoryId, url);

        RestosApiResponse<List<RestosProduct>> response = executeGet(
                url, apiKey, new ParameterizedTypeReference<>() {});

        List<RestosProduct> products = extractData(response, url);
        log.info("Fetched {} products for category {}", products.size(), categoryId);
        return products;
    }

    /**
     * Endpoint #3: GET /api/v1/customer/public/restaurants/{restaurantId}/menu
     * Full menu — categories with nested products.
     */
    public List<RestosCategory> fetchFullMenu(String baseUrl, Long restaurantId, String apiKey) {
        String url = baseUrl + "/api/v1/customer/public/restaurants/" + restaurantId + "/menu";
        log.info("Fetching full menu from Restos: {}", url);

        RestosApiResponse<List<RestosCategory>> response = executeGet(
                url, apiKey, new ParameterizedTypeReference<>() {});

        List<RestosCategory> menu = extractData(response, url);
        log.info("Fetched full menu: {} categories from Restos for restaurant {}", menu.size(), restaurantId);
        return menu;
    }

    /**
     * Endpoint #4: GET /api/v1/menu/public/{restaurantId}
     * Cached full menu (faster for public use).
     */
    public List<RestosCategory> fetchCachedMenu(String baseUrl, Long restaurantId, String apiKey) {
        String url = baseUrl + "/api/v1/menu/public/" + restaurantId;
        log.info("Fetching cached menu from Restos: {}", url);

        RestosApiResponse<List<RestosCategory>> response = executeGet(
                url, apiKey, new ParameterizedTypeReference<>() {});

        List<RestosCategory> menu = extractData(response, url);
        log.info("Fetched cached menu: {} categories from Restos for restaurant {}", menu.size(), restaurantId);
        return menu;
    }

    /**
     * Endpoint #5: GET /api/v1/categories?restaurantId={id}
     * Categories with kitchen station info.
     */
    public List<RestosCategory> fetchCategoriesWithKitchenInfo(String baseUrl, Long restaurantId, String apiKey) {
        String url = baseUrl + "/api/v1/categories?restaurantId=" + restaurantId;
        log.info("Fetching categories with kitchen info from Restos: {}", url);

        RestosApiResponse<List<RestosCategory>> response = executeGet(
                url, apiKey, new ParameterizedTypeReference<>() {});

        List<RestosCategory> categories = extractData(response, url);
        log.info("Fetched {} categories with kitchen info for restaurant {}", categories.size(), restaurantId);
        return categories;
    }

    /**
     * Endpoint #6: GET /api/v1/products/restaurant/{restaurantId}
     * All products with category info embedded.
     */
    public List<RestosProduct> fetchAllProducts(String baseUrl, Long restaurantId, String apiKey) {
        String url = baseUrl + "/api/v1/products/restaurant/" + restaurantId;
        log.info("Fetching all products from Restos: {}", url);

        RestosApiResponse<List<RestosProduct>> response = executeGet(
                url, apiKey, new ParameterizedTypeReference<>() {});

        List<RestosProduct> products = extractData(response, url);
        log.info("Fetched {} products from Restos for restaurant {}", products.size(), restaurantId);
        return products;
    }

    private <T> RestosApiResponse<T> executeGet(String url, String apiKey,
                                                 ParameterizedTypeReference<RestosApiResponse<T>> typeRef) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            if (apiKey != null && !apiKey.isBlank()) {
                headers.setBearerAuth(apiKey);
            }
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<RestosApiResponse<T>> responseEntity =
                    restTemplate.exchange(url, HttpMethod.GET, entity, typeRef);

            if (responseEntity.getBody() == null) {
                throw new BusinessException("Restos API returned empty response from: " + url);
            }

            return responseEntity.getBody();
        } catch (RestClientException e) {
            log.error("Failed to call Restos API at {}: {}", url, e.getMessage());
            throw new BusinessException("Failed to connect to Restos system: " + e.getMessage());
        }
    }

    private <T> T extractData(RestosApiResponse<T> response, String url) {
        if (!response.isSuccess()) {
            throw new BusinessException("Restos API returned error from " + url + ": " + response.getMessage());
        }
        if (response.getData() == null) {
            throw new BusinessException("Restos API returned null data from: " + url);
        }
        return response.getData();
    }
}
