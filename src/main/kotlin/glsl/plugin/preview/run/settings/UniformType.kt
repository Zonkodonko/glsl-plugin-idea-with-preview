package glsl.plugin.preview.run.settings

/**
 * Enum class for uniform types used in fragment shaders.
 *
 * Will maybe add a CUSTOM type later
 * todo add tooltips to add in the settings dialog
 */
enum class UniformType(val label: String,val defaultName: String) {
    TIME("Time", "uTime"),
    RESOLUTION("Resolution", "uResolution"),
    MOUSE("Mouse", "uMouse")
}