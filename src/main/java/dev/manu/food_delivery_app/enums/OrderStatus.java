package dev.manu.food_delivery_app.enums;

public enum OrderStatus {
    PLACED, CONFIRMED, CANCELLED, PREPARING, OUT_FOR_DELIVERY, DELIVERED;

    public boolean canTransitionTo(OrderStatus next) {
        return switch (this) {
            case PLACED -> next == CONFIRMED || next == CANCELLED;
            case CONFIRMED -> next == PREPARING;
            case PREPARING -> next == OUT_FOR_DELIVERY;
            case OUT_FOR_DELIVERY -> next == DELIVERED;
            case DELIVERED, CANCELLED -> false;
        };
    }
}
