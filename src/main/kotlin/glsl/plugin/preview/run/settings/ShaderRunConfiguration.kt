package glsl.plugin.preview.run.settings

import com.intellij.execution.Executor
import com.intellij.execution.configurations.*
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import glsl.plugin.preview.run.FragmentShaderRunProfileState


/**
 * Implements [RunProfile]
 * Manages GlslRunSettings
 */
class ShaderRunConfiguration(
    project: Project,
    factory: ShaderRunConfigurationFactory?,
    name: String?
) : RunConfigurationBase<ShaderOptions>(project, factory, name) {


    override fun getOptions(): ShaderOptions {
        return super.getOptions() as ShaderOptions
    }

    override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState {
        return FragmentShaderRunProfileState(project, options.fragmentFile!!)
    }

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
        return ShaderSettingsEditor()
    }

    fun setFragmentFile(path: String) {
        options.fragmentFile = path;
    }

    fun getFragmentFile(): String? {
        return options.fragmentFile;
    }





}