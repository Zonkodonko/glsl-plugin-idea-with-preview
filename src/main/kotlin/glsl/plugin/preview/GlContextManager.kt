package glsl.plugin.preview

import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessListener
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import fleet.util.computeIfAbsentShim
import glsl.plugin.preview.run.GLProcessHandler
import glsl.plugin.preview.run.ShaderProgramCompiler
import glsl.plugin.preview.run.settings.FragShaderRunOptions
import glsl.plugin.preview.run.settings.UniformType
import glsl.plugin.utils.exceptions.ShaderCompilerException
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL20.*
import org.lwjgl.opengl.awt.AWTGLCanvas
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent

class GlContextManager : Disposable {

    private var glCanvas: AWTGLCanvas;
    private val project: Project;


    // OpenGL resources
    private var initialized = false
    private var startNs = 0L
    private var programId: Int = -1

    // uniforms (optional)
    private var uTimeLocation = -1
    private var uResolutionLocation = -1
    private var uMouseLocation = -1

    // rendering variables
    private var positionLocation = -1
    private var positionBuffer = -1
    private var vertexArrayBuffer: Int = 0


    private var pendingCompile: CompileRun? = null
    private var pendingStop: Boolean = false

    private data class CompileRun(val settings: FragShaderRunOptions, val processHandler: GLProcessHandler);

    val processTerminatedListener = object : ProcessListener {
        override fun processTerminated(processEvent: ProcessEvent) =
            this@GlContextManager.onProcessTerminated(processEvent)
    }


    companion object {
        @Volatile
        private var instances: MutableMap<Project, GlContextManager> = HashMap()

        fun getInstance(project: Project): GlContextManager {
            return instances[project] ?: synchronized(this) {
                instances.computeIfAbsentShim(project, ::GlContextManager)
            }
        }
    }

    private constructor(project: Project) {
        this.project = project;
    }

    init {
        this.glCanvas = object : AWTGLCanvas() {

            override fun addNotify() {
                super.addNotify()
                println("AWTGLCanvas addNotify: displayable=$isDisplayable showing=$isShowing size=$size")
            }

            override fun initGL() {
                GL.createCapabilities()
                initialized = true
                startNs = System.nanoTime()

                glClearColor(0.5f, 0.1f, 0.5f, 1f)
                println("GL initialized.")

            }

            override fun paintGL() {
                if (pendingCompile != null) {
                    pendingCompile!!.processHandler.addProcessListener(processTerminatedListener)
                    compile(pendingCompile!!)
                    pendingCompile = null
                }
                if (pendingStop) {
                    glDeleteProgram(programId)
                    programId = -1
                    clearCanvas()
                    pendingStop = false
                    return

                }
                if (programId != -1) {
                    this@GlContextManager.render()
                }
                swapBuffers()//need to call that always because otherwise the canvas would not react to resize
            }

            /**
             * Empty front and back buffers.
             */
            fun clearCanvas() {
                for (i in 0 until 2) {
                    glClearColor(0f, 0f, 0f, 1f)
                    glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT or GL_STENCIL_BUFFER_BIT)
                    if (positionBuffer != 0) {
                        glDeleteBuffers(positionBuffer)
                        positionBuffer = 0
                    }
                    swapBuffers()
                }
            }

        }

        glCanvas.addComponentListener(object : ComponentAdapter() {
            override fun componentResized(e: ComponentEvent?) {
                println("AWTGLCanvas resized: size=${glCanvas.size}, bounds=${glCanvas.bounds}")
            }

            override fun componentShown(e: ComponentEvent?) {
                println("AWTGLCanvas shown: size=${glCanvas.size}, bounds=${glCanvas.bounds}")
            }
        })
    }

    /**
     * Deletes the current shader program and clears the canvas.
     */
    private fun onProcessTerminated(processEvent: ProcessEvent) {
        (processEvent.processHandler as GLProcessHandler).printStdout("Process terminated");
        pendingStop = true;
    }

    /**
     * Add a compile request to the queue. All glsl logs will be printed to the process handler.
     * The request will be handled with the next render cycle.
     */
    fun queueCompile(runOptions: FragShaderRunOptions, processHandler: GLProcessHandler) {
        pendingCompile = CompileRun(runOptions, processHandler)
    }

    private fun compile(compileRun: CompileRun) {
        try {
            println("Compiling shader program:")
            val shaderProgramCompiler = ShaderProgramCompiler(compileRun.processHandler);
            this.programId = shaderProgramCompiler.getProgramFromFrag(compileRun.settings.getFragDocument().text)
            runShaderProgram()
        } catch (e: Exception) {
            if (e is ShaderCompilerException) {
                compileRun.processHandler.printStderr(e.shaderInfoLog)
                compileRun.processHandler.terminate(69)
            } else {
                throw e;
            }
            this.programId = -1
        }
    }

    fun runShaderProgram() {
        if (!initialized) {
            throw IllegalStateException("GL not initialized")
        }
        if (programId == -1) {
            throw IllegalStateException("Program not compiled")
        }
        startNs = System.nanoTime()
        glUseProgram(programId)
        glCanvas.requestFocus()
        glCanvas.setVisible(true)
    }

    /**
     * @return the shader canvas
     */
    fun getCanvas(): AWTGLCanvas {
        return this.glCanvas;
    }


    override fun dispose() {
        if (!initialized) return

        if (programId != 0) glDeleteProgram(programId)
        if (vertexArrayBuffer != 0) glDeleteBuffers(vertexArrayBuffer)

        glCanvas.disposeCanvas();
    }


    private fun setupRenderContext(uniformMapping: Map<UniformType, String>) {
        println("Setup render context:")
        positionBuffer = glGenBuffers()
        glBindBuffer(GL_ARRAY_BUFFER, positionBuffer)
        // Fullscreen triangle in NDC:
        val positions = floatArrayOf(
            -1f, -1f,
            1f, -1f,
            -1f, 1f,
            -1f, 1f,
            1f, -1f,
            1f, 1f
        )

        glBufferData(GL_ARRAY_BUFFER, positions, GL_STATIC_DRAW)

        //todo make layout feature possible

        positionLocation = glGetAttribLocation(programId, "position")
        uTimeLocation = glGetUniformLocation(programId, uniformMapping.getOrDefault(UniformType.TIME, UniformType.TIME.defaultName))
        uResolutionLocation = glGetUniformLocation(programId, uniformMapping.getOrDefault(UniformType.RESOLUTION, UniformType.RESOLUTION.defaultName))
        uMouseLocation = glGetUniformLocation(programId, uniformMapping.getOrDefault(UniformType.MOUSE, UniformType.MOUSE.defaultName))

    }

    /**
     * Rende GL context stuff and update uniforms.
     */
    private fun render() {
        val w = glCanvas.width.coerceAtLeast(1)
        val h = glCanvas.height.coerceAtLeast(1)

        glViewport(0, 0, w, h)
        glClearColor(0f, 0f, 0f, 1f)
        glClear(GL_COLOR_BUFFER_BIT)

        glUseProgram(programId)
        glEnableVertexAttribArray(positionLocation)
        glBindBuffer(GL_ARRAY_BUFFER, positionBuffer)
        glVertexAttribPointer(positionLocation, 2, GL_FLOAT, false, 0, 0)

        val time = (System.nanoTime() - startNs) / 1_000_000_000.0f //time in seconds
        glUniform1f(uTimeLocation, time)
        glUniform2f(uResolutionLocation, w.toFloat(), h.toFloat())
        val mousePos = glCanvas.mousePosition;
        if(mousePos == null) {
            glUniform2f(uMouseLocation, -1f, -1f)
        } else {
            glUniform2f(uMouseLocation, mousePos.x.toFloat(), mousePos.y.toFloat())
        }

        glDrawArrays(GL_TRIANGLES, 0, 6)
    }
}