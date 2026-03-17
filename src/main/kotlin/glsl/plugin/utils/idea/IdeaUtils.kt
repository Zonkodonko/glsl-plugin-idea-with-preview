package glsl.plugin.utils.idea

import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.vfs.VirtualFile
import java.awt.Component
import javax.swing.DefaultListCellRenderer
import javax.swing.JList

/**
 * Renders [VirtualFile]s in list cells
 */
val FileListCellRenderer: DefaultListCellRenderer = object : DefaultListCellRenderer() {
    override fun getListCellRendererComponent(
        list: JList<*>?,
        value: Any?,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean
    ): Component {

        val text = if(value != null) (value as VirtualFile).name else "" //no idea why this can be null
        return super.getListCellRendererComponent(list, text, index, isSelected, cellHasFocus)
    }
}

interface ConsoleLogWrapper {
    fun print(text: String)
}