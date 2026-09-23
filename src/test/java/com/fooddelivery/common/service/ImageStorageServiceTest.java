package com.fooddelivery.common.service;

import com.fooddelivery.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Image uploads, and what happens when the storage root will not take a write.
 *
 * <p>Every vendor upload in production failed with
 * {@code could not create category directory: restaurants/1/logo} — two mounts
 * targeted {@code /app/images}, the bind mount that won was created root-owned
 * by the daemon, and the container runs as a non-root user. Three things made
 * that cost weeks rather than minutes: startup reported success (
 * {@code createDirectories} on an existing directory needs no write
 * permission), the {@code IOException} was discarded without being logged, and
 * the message the vendor saw described a server path they could do nothing
 * with.
 */
@DisplayName("ImageStorageService")
class ImageStorageServiceTest {

    private ImageStorageService serviceAt(Path root) {
        ImageStorageService service = new ImageStorageService();
        ReflectionTestUtils.setField(service, "storagePath", root.toString());
        ReflectionTestUtils.setField(service, "baseUrl", "https://zbrr.uz/api/v1/images");
        service.init();
        return service;
    }

    private MockMultipartFile png() {
        return new MockMultipartFile("file", "logo.png", "image/png", new byte[]{1, 2, 3});
    }

    /**
     * A root that cannot be created by anyone, root included — its parent is a
     * regular file. Permission bits would not do: the test suite may run as
     * uid 0, for whom a mode-0555 directory is still writable, and the test
     * would pass by accident everywhere except the one environment that
     * matters.
     */
    private Path unusableRootUnder(Path tmp) throws IOException {
        Path blocker = tmp.resolve("not-a-directory");
        Files.writeString(blocker, "");
        return blocker.resolve("images");
    }

    @Nested
    @DisplayName("when storage works")
    class Working {

        @Test
        @DisplayName("stores the file under its category and returns a usable URL")
        void storesFile(@TempDir Path tmp) {
            ImageStorageService service = serviceAt(tmp.resolve("images"));

            ImageStorageService.ImageInfo info = service.storeImage(png(), "restaurants/1/logo");

            assertThat(Path.of(info.getPath())).exists();
            assertThat(info.getRelativePath()).startsWith("restaurants/1/logo/");
            assertThat(info.getUrl())
                    .isEqualTo("https://zbrr.uz/api/v1/images/" + info.getRelativePath());
        }

        @Test
        @DisplayName("creates the whole category chain, not just the last segment")
        void createsNestedCategories(@TempDir Path tmp) {
            // "restaurants/1/logo" is three levels deep. A plain mkdir would
            // fail on it; this is the shape every restaurant image uses.
            Path root = tmp.resolve("images");
            serviceAt(root).storeImage(png(), "restaurants/42/cover");

            assertThat(root.resolve("restaurants/42/cover")).isDirectory();
        }

        @Test
        @DisplayName("leaves no write probe behind")
        void probeIsCleanedUp(@TempDir Path tmp) throws IOException {
            Path root = tmp.resolve("images");
            serviceAt(root);

            try (var entries = Files.list(root)) {
                assertThat(entries).isEmpty();
            }
        }
    }

    @Nested
    @DisplayName("when the storage root refuses writes")
    class Unwritable {

        @Test
        @DisplayName("the application still starts")
        void startupDoesNotFail(@TempDir Path tmp) throws IOException {
            // Images are not worth taking the whole platform down for — orders,
            // payments and deliveries do not need them. init() logs the failure
            // loudly instead of throwing.
            Path root = unusableRootUnder(tmp);

            assertThatCode(() -> serviceAt(root)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("the vendor is told the server is at fault, without a filesystem path")
        void messageIsForTheVendor(@TempDir Path tmp) throws IOException {
            ImageStorageService service = serviceAt(unusableRootUnder(tmp));

            assertThatThrownBy(() -> service.storeImage(png(), "restaurants/1/logo"))
                    .isInstanceOf(BusinessException.class)
                    // The apps display `message` verbatim in an alert. The old
                    // text named a directory on the server, which told the
                    // vendor nothing and told everyone else our layout.
                    .satisfies(e -> {
                        assertThat(e.getMessage()).doesNotContain("restaurants/1/logo");
                        assertThat(e.getMessage()).doesNotContain(tmp.toString());
                    });
        }
    }

    /**
     * The bucket used to be whatever the caller typed. It was a
     * {@code @PathVariable} used directly as a directory name, so every name
     * worked and every name created a drawer — a typo made a second one
     * silently, and no per-bucket rule could mean anything.
     */
    @Nested
    @DisplayName("buckets")
    class Buckets {

        @Test
        @DisplayName("categories is a bucket of its own, not a corner of restaurants")
        void categoriesBucketAccepted(@TempDir Path tmp) {
            Path root = tmp.resolve("images");

            ImageStorageService.ImageInfo info = serviceAt(root).storeImage(png(), "categories");

            assertThat(info.getRelativePath()).startsWith("categories/");
            assertThat(root.resolve("categories")).isDirectory();
        }

        @Test
        @DisplayName("an unknown bucket is refused, and the accepted ones are named")
        void unknownBucketRefused(@TempDir Path tmp) {
            Path root = tmp.resolve("images");
            ImageStorageService service = serviceAt(root);

            assertThatThrownBy(() -> service.storeImage(png(), "catagories"))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("catagories")
                    // Without the list, the caller's next move is guesswork.
                    .hasMessageContaining("categories");

            // And nothing was created on the way to being refused.
            assertThat(root.resolve("catagories")).doesNotExist();
        }

        @Test
        @DisplayName("a bucket in the wrong case files into the same drawer, not a twin")
        void caseDoesNotSplitTheBucket(@TempDir Path tmp) {
            // Linux filesystems are case-sensitive, so "Categories" would be a
            // second drawer that every audit and cleanup rule would miss.
            Path root = tmp.resolve("images");

            ImageStorageService.ImageInfo info = serviceAt(root).storeImage(png(), "Categories");

            assertThat(info.getRelativePath()).startsWith("categories/");
            assertThat(root.resolve("Categories")).doesNotExist();
        }

        @Test
        @DisplayName("what a caller composes below the bucket is still its own business")
        void deeperPathsAreUntouched(@TempDir Path tmp) {
            // Only the first segment is policy. restaurants/{id}/{logo|cover}
            // and profiles/{userId} are built by our own callers.
            ImageStorageService service = serviceAt(tmp.resolve("images"));

            assertThat(service.storeImage(png(), "restaurants/7/logo").getRelativePath())
                    .startsWith("restaurants/7/logo/");
            assertThat(service.storeImage(png(), "profiles/42").getRelativePath())
                    .startsWith("profiles/42/");
        }
    }

    @Nested
    @DisplayName("containment")
    class Containment {

        @Test
        @DisplayName("a category escaping the root is refused")
        void traversalRefused(@TempDir Path tmp) {
            ImageStorageService service = serviceAt(tmp.resolve("images"));

            assertThatThrownBy(() -> service.storeImage(png(), "../../etc"))
                    .isInstanceOf(BusinessException.class);
            assertThat(tmp.resolve("etc")).doesNotExist();
        }

        @Test
        @DisplayName("traversal from inside a legitimate bucket is still refused")
        void traversalBelowAValidBucketRefused(@TempDir Path tmp) {
            // The bucket check would otherwise be the only thing stopping "../"
            // — and it stops the obvious spelling, not this one. Containment has
            // to hold on its own.
            ImageStorageService service = serviceAt(tmp.resolve("images"));

            assertThatThrownBy(() -> service.storeImage(png(), "categories/../../etc"))
                    .isInstanceOf(BusinessException.class);
            assertThat(tmp.resolve("etc")).doesNotExist();
        }

        @Test
        @DisplayName("a file outside the root is not served")
        void loadOutsideRootRefused(@TempDir Path tmp) throws IOException {
            Files.writeString(tmp.resolve("secret.txt"), "not yours");
            ImageStorageService service = serviceAt(tmp.resolve("images"));

            assertThatThrownBy(() -> service.loadImage("../secret.txt"))
                    .isInstanceOf(BusinessException.class);
        }
    }
}
