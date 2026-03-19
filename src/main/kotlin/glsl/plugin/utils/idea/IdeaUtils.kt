package glsl.plugin.utils.idea

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
        var text = if (value != null) (value as VirtualFile).name else "<empty>"
        return super.getListCellRendererComponent(list, text, index, isSelected, cellHasFocus)
    }
}
