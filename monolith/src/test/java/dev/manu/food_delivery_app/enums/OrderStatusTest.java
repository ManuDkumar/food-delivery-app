package dev.manu.food_delivery_app.enums;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

public class OrderStatusTest {

    @Test
    public void placed_toConfirmed_isTrue() {
        assertThat(OrderStatus.PLACED.canTransitionTo(OrderStatus.CONFIRMED)).isTrue();
    }

    @Test
    public void placed_toCancelled_isTrue() {
        assertThat(OrderStatus.PLACED.canTransitionTo(OrderStatus.CANCELLED)).isTrue();
    }

    @Test
    public void confirmed_toPreparing_isTrue() {
        assertThat(OrderStatus.CONFIRMED.canTransitionTo(OrderStatus.PREPARING)).isTrue();
    }
    @Test
    public void placed_toPreparing_isFalse() {
        assertThat(OrderStatus.PLACED.canTransitionTo(OrderStatus.PREPARING)).isFalse();
    }

    @Test
    public void confirmed_toCancel_isFalse() {
        assertThat(OrderStatus.CONFIRMED.canTransitionTo(OrderStatus.CANCELLED)).isFalse();
    }

    @Test
    public void preparing_toOutForDelivery_isTrue() {
        assertThat(OrderStatus.PREPARING.canTransitionTo(OrderStatus.OUT_FOR_DELIVERY)).isTrue();
    }

    @Test
    public void outForDelivery_toDelivered_isTrue() {
        assertThat(OrderStatus.OUT_FOR_DELIVERY.canTransitionTo(OrderStatus.DELIVERED)).isTrue();
    }

    @Test
    public void outForDelivery_toCancelled_isFalse() {
        assertThat(OrderStatus.OUT_FOR_DELIVERY.canTransitionTo(OrderStatus.CANCELLED)).isFalse();
    }
    @Test
    public void cancelled_toConfirmed_isFalse() {
        assertThat(OrderStatus.CANCELLED.canTransitionTo(OrderStatus.CONFIRMED)).isFalse();
    }
    @Test
    public void sameToSame_isFalse() {
        assertThat(OrderStatus.CONFIRMED.canTransitionTo(OrderStatus.CONFIRMED)).isFalse();
    }
}
