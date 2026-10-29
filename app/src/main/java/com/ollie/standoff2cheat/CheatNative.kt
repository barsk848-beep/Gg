package com.ollie.standoff2cheat

object CheatNative {
    init {
        System.loadLibrary("standoffcheat")
    }

    external fun setFeature(feature: Int, enabled: Boolean)
    external fun setFloat(feature: Int, value: Float)
    external fun isRooted(): Boolean
    external fun patchGame(enabled: Boolean): Boolean

    const val FEATURE_AIM = 0
    const val FEATURE_ESP = 1
    const val FEATURE_NO_RECOIL = 2

    const val FLOAT_AIM_FOV = 10
    const val FLOAT_AIM_SMOOTH = 11
    const val FLOAT_RECOIL_MULT = 12
}
