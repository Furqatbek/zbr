package com.fooddelivery.common.controller;

import com.fooddelivery.common.service.ImageStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.util.pattern.PathPatternParser;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Whether an image URL the platform itself handed out can be fetched back.
 *
 * <p>The mapping was {@code /{category}/{filename:.+}} — exactly two segments.
 * Menu items store {@code menu-items/<uuid>.png} and fitted; restaurant images
 * store {@code restaurants/<id>/logo/<uuid>.png} and did not, so every
 * restaurant logo and cover 404'd. Nothing failed at upload time: the file was
 * written, the URL was saved on the restaurant, and the apps showed a blank
 * image with no error to report. A regex path variable cannot span "/" under
 * PathPatternParser, which is why the {@code .+} looked like it should work.
 */
@DisplayName("Image URL routing")
class ImageUrlRoutingTest {

    private ImageStorageService storage;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        storage = mock(ImageStorageService.class);
        Resource png = new ByteArrayResource(new byte[]{(byte) 0x89, 'P', 'N', 'G'});
        when(storage.loadImage(anyString())).thenReturn(png);

        // The same matcher production uses. Spring Boot has defaulted to
        // PathPatternParser since 2.6, and {*path} is its syntax — a test on
        // the legacy AntPathMatcher would prove nothing about the live app.
        mockMvc = MockMvcBuilders.standaloneSetup(new ImageController(storage))
                .setPatternParser(new PathPatternParser())
                .build();
    }

    @Test
    @DisplayName("a restaurant logo URL is served")
    void restaurantLogo() throws Exception {
        // THE regression. Four segments.
        mockMvc.perform(get("/api/v1/images/restaurants/7/logo/abc.png"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"));

        verify(storage).loadImage("restaurants/7/logo/abc.png");
    }

    @Test
    @DisplayName("a restaurant cover URL is served")
    void restaurantCover() throws Exception {
        mockMvc.perform(get("/api/v1/images/restaurants/7/cover/abc.webp"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/webp"));

        verify(storage).loadImage("restaurants/7/cover/abc.webp");
    }

    @Test
    @DisplayName("menu item URLs still work")
    void menuItem() throws Exception {
        // These were the ones that worked, and every one already stored in the
        // database has this shape — widening the mapping must not break them.
        mockMvc.perform(get("/api/v1/images/menu-items/abc.jpg"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/jpeg"));

        verify(storage).loadImage("menu-items/abc.jpg");
    }

    @Test
    @DisplayName("the content type comes from the file, not from a directory")
    void contentTypeFromLastSegment() throws Exception {
        mockMvc.perform(get("/api/v1/images/restaurants/7/v1.2/abc.gif"))
                .andExpect(content().contentType("image/gif"));
    }

    @Test
    @DisplayName("the relative path handed to storage has no leading slash")
    void noLeadingSlash() throws Exception {
        // {*path} captures the remainder WITH its slash. Passed through, it
        // would resolve as an absolute path against the storage root and read
        // from the filesystem root instead.
        mockMvc.perform(get("/api/v1/images/menu-items/abc.png"));

        verify(storage).loadImage("menu-items/abc.png");
    }
}
