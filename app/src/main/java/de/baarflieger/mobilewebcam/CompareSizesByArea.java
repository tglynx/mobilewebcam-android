package de.baarflieger.mobilewebcam;

import java.util.Comparator;

public class CompareSizesByArea implements Comparator<android.util.Size> {
    @Override
    public int compare(android.util.Size lhs, android.util.Size rhs) {
        // We cast to ensure the multiplication won't overflow
        return Long.signum((long) lhs.getWidth() * lhs.getHeight() -
                (long) rhs.getWidth() * rhs.getHeight());
    }
}