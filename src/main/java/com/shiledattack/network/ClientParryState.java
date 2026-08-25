package com.shiledattack.network;

/** Client-side cache of the parry cooldown remaining ticks, updated from the server. */
public final class ClientParryState {
    private static int remainingTicks = 0;

    public static void setRemaining(int ticks) {
        remainingTicks = Math.max(0, ticks);
    }

    public static int getRemaining() {
        return remainingTicks;
    }

    public static boolean isReady() {
        return remainingTicks <= 0;
    }

    /** Called every client tick to count the cooldown down. */
    public static void tick() {
        if (remainingTicks > 0) {
            remainingTicks--;
        }
    }

    private ClientParryState() {
    }
}