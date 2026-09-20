package com.fooddelivery.delivery.eta;

import com.fooddelivery.courier.entity.VehicleType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ETA configuration")
class EtaPropertiesTest {

    private static EtaProperties bind(Map<String, Object> properties) {
        EtaProperties target = new EtaProperties();
        new Binder(new MapConfigurationPropertySource(properties))
                .bind("app.delivery.eta", Bindable.ofInstance(target));
        return target;
    }

    @ParameterizedTest
    @EnumSource(VehicleType.class)
    @DisplayName("every vehicle can be quoted without configuration")
    void everyVehicleHasADefault(VehicleType vehicle) {
        // A vehicle with no speed is a divide-by-zero on the one code path
        // every customer sees. Adding one to the enum must not be able to
        // introduce that quietly.
        EtaProperties properties = new EtaProperties();

        assertThat(properties.speedFor(vehicle)).isPositive();
        assertThat(properties.overheadFor(vehicle)).isNotNegative();
    }

    @Test
    @DisplayName("a missing speed falls back rather than dividing by zero")
    void missingSpeedFallsBack() {
        EtaProperties properties = new EtaProperties();
        properties.getSpeedKmh().remove(VehicleType.CAR);

        assertThat(properties.speedFor(VehicleType.CAR)).isEqualTo(EtaProperties.FALLBACK_SPEED_KMH);
    }

    @Test
    @DisplayName("configuring one vehicle leaves the others at their defaults")
    void oneVehicleCanBeOverriddenAlone() {
        // The behaviour the yml comment promises: a deployment tuning cars in
        // one city does not silently drop every other vehicle to zero.
        EtaProperties properties = bind(Map.of("app.delivery.eta.speed-kmh.CAR", "33"));

        assertThat(properties.speedFor(VehicleType.CAR)).isEqualTo(33.0);
        assertThat(properties.speedFor(VehicleType.BICYCLE)).isEqualTo(12.0);
    }

    @Test
    @DisplayName("the planning vehicle is configurable")
    void planningVehicleBinds() {
        EtaProperties properties = bind(Map.of("app.delivery.eta.planning-vehicle", "MOTORCYCLE"));

        assertThat(properties.getPlanningVehicle()).isEqualTo(VehicleType.MOTORCYCLE);
    }

    @Test
    @DisplayName("a zero speed fails the deployment, not the delivery")
    void zeroSpeedIsRejected() {
        EtaProperties properties = bind(Map.of("app.delivery.eta.speed-kmh.BICYCLE", "0"));

        assertThatThrownBy(properties::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("BICYCLE");
    }

    @Test
    @DisplayName("a detour factor below one is rejected")
    void shortcutDetourFactorIsRejected() {
        // Below 1.0 quotes a journey shorter than the straight line between
        // its ends, which is not a road anyone can take.
        EtaProperties properties = bind(Map.of("app.delivery.eta.detour-factor", "0.8"));

        assertThatThrownBy(properties::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("detour-factor");
    }

    @Test
    @DisplayName("the shipped defaults are valid")
    void defaultsValidate() {
        new EtaProperties().validate();
    }
}
