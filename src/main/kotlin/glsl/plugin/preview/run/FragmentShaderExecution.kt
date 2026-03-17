package glsl.plugin.preview.run

import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.filters.UrlFilter
import com.intellij.execution.process.NopProcessHandler
import com.intellij.execution.runners.ProgramRunner
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.wm.ToolWindowManager
import glsl.plugin.preview.GlContextManager

class FragmentShaderRunProfileState(private val project: Project, private val fragmentFilePath: String) : RunProfileState {

    val glContextManager = GlContextManager.getInstance(project)

    override fun execute(
        executor: Executor?,
        runner: ProgramRunner<*>
    ): ExecutionResult? {
        val processHandler: GLProcessHandler = GLProcessHandler()
        val consoleView: ConsoleView = TextConsoleBuilderFactory.getInstance()
            .createBuilder(project)
            .filters(UrlFilter(project))
            .console

        consoleView.attachToProcess(processHandler)
        processHandler.startNotify()

        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("GLSL Preview")

        if (toolWindow == null) {
            consoleView.print("Could not find tool window 'GLSL Preview'.\n", ConsoleViewContentType.ERROR_OUTPUT)
            return DefaultExecutionResult(consoleView, NopProcessHandler())
        }


        val shaderFile = VirtualFileManager.getInstance().findFileByUrl(fragmentFilePath)
        if(shaderFile == null) {
            consoleView.print("Could not find file: $fragmentFilePath", ConsoleViewContentType.ERROR_OUTPUT);
            return DefaultExecutionResult(consoleView, NopProcessHandler())
        }
        val document: Document? = FileDocumentManager.getInstance().getDocument(shaderFile)

        if (document == null) {
            consoleView.print("No editor document found", ConsoleViewContentType.ERROR_OUTPUT)
            return DefaultExecutionResult(consoleView, NopProcessHandler())
        }

        glContextManager.queueCompile(document,processHandler);
        return DefaultExecutionResult(consoleView, processHandler);
    }
}
