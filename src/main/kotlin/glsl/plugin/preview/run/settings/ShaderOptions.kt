package glsl.plugin.preview.run.settings

import com.intellij.execution.configurations.RunConfigurationOptions
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json


val jsonParser: Json = Json { ignoreUnknownKeys = true }

@Serializable
data class GlslUniformMapping(
    var resolution: String = "uResolution",
    var time: String = "uTime"
)


class ShaderOptions() : RunConfigurationOptions() {
    var fragmentFile: String? by string()
    var vertexFile: String? by string()
    private var uniformMappingsRaw: String? by string(jsonParser.encodeToString(GlslUniformMapping()))

    fun getUniformMappings(): GlslUniformMapping {
        return jsonParser.decodeFromString<GlslUniformMapping>(
            requireNotNull(
            uniformMappingsRaw,
            { "Uniform mappings cannot be null" }))
    }

    fun setUniformMappings(mappings: GlslUniformMapping) {
        uniformMappingsRaw = jsonParser.encodeToString(mappings)
    }


}







