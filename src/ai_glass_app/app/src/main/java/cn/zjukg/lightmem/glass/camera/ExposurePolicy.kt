package cn.zjukg.lightmem.glass.camera

import kotlin.math.roundToInt

internal fun exposureIndexForEv(ev: Double, stepEv: Double, minimum: Int, maximum: Int): Int? {
    if (!ev.isFinite() || !stepEv.isFinite() || stepEv <= 0.0 || minimum > maximum) return null
    return (ev / stepEv).roundToInt().coerceIn(minimum, maximum)
}
