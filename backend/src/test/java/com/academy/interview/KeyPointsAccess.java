package com.academy.interview;

import java.util.List;

/** Test-only bridge to the package-private derivation logic. */
public final class KeyPointsAccess {
    private KeyPointsAccess() {
    }

    public static List<String> derive(String answer) {
        return KeyPoints.derive(answer);
    }
}
