// Copyright (c) 2024 Wishabi. All rights reserved.

package com.flipp.impressionsandbox.impression

/**
 * A predicate interface to determine whether the given width and height qualify for an impression to be reported.
 */
interface ImpressionQualifier {
    /**
     * Returns true, if the given visible dimensions qualify for an impression to be reported with respect to the
     * components total global layout dimensions.
     */
    fun isImpression(
        visibleWidthPx: Int,
        visibleHeightPx: Int,
        globalWidthPx: Int,
        globalHeightPx: Int
    ): Boolean
}

/**
 * An [ImpressionQualifier] that reports an impression when the give decimal [percentage] are of visibility is exceeded.
 * ie. 0.5F is used to represent 50% visibility.
 */
class PercentageViewableQualifier(private val percentage: Float) : ImpressionQualifier {
    override fun isImpression(
        visibleWidthPx: Int, visibleHeightPx: Int, globalWidthPx: Int, globalHeightPx: Int
    ): Boolean {
        val visibleArea = visibleWidthPx * visibleHeightPx
        val componentArea = globalWidthPx * globalHeightPx
        val visiblePercentage = visibleArea.toFloat() / componentArea.toFloat()

        return visiblePercentage >= percentage
    }
}

/**
 * An [ImpressionQualifier] that reports an impression when a given area viewable (in pixels) is exceeded.
 * NOTE: if [areaPx] exceeds that displayable by the viewport, then the qualifier is never true.
 */
class PixelsViewableQualifier(private val areaPx: Int) : ImpressionQualifier {
    override fun isImpression(
        visibleWidthPx: Int,
        visibleHeightPx: Int,
        globalWidthPx: Int,
        globalHeightPx: Int
    ): Boolean = visibleWidthPx * visibleHeightPx >= areaPx
}