package cn.zjukg.lightmem.glass.camera

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExposurePolicyTest {
    @Test
    fun screenPresetUsesTheHardwareEvStep() {
        assertEquals(-6, exposureIndexForEv(-2.0, 1.0 / 3.0, -12, 12))
    }

    @Test
    fun requestedEvIsClampedToTheAvailableRange() {
        assertEquals(-3, exposureIndexForEv(-2.0, 1.0 / 3.0, -3, 3))
        assertEquals(3, exposureIndexForEv(2.0, 1.0 / 3.0, -3, 3))
    }

    @Test
    fun intermediateValuesUseTheNearestSupportedStep() {
        assertEquals(-3, exposureIndexForEv(-1.4, 0.5, -6, 6))
        assertEquals(0, exposureIndexForEv(0.0, 0.5, -6, 6))
    }

    @Test
    fun invalidCapabilitiesAreReportedAsUnavailable() {
        assertNull(exposureIndexForEv(-2.0, 0.0, 0, 0))
        assertNull(exposureIndexForEv(-2.0, Double.NaN, -3, 3))
        assertNull(exposureIndexForEv(Double.NaN, 0.5, -3, 3))
        assertNull(exposureIndexForEv(-2.0, 0.5, 3, -3))
    }
}
