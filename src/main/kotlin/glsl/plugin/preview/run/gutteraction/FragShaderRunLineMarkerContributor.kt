package glsl.plugin.preview.run.gutteraction

import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.icons.AllIcons
import com.intellij.psi.PsiElement
import glsl.plugin.preview.run.FragmentShaderRunAction
import glsl.plugin.utils.idea.isInFragFile
import glsl.psi.interfaces.GlslFunctionDeclarator

class FragShaderRunLineMarkerContributor : RunLineMarkerContributor() {

    override fun getInfo(element: PsiElement): Info? {
        val isFragmentShader = isInFragFile(element)
        if (!isFragmentShader || element !is GlslFunctionDeclarator || !element.text.contains("main")) {
            return null
        }
        return Info(AllIcons.Actions.Execute, arrayOf(FragmentShaderRunAction()))
    }
}