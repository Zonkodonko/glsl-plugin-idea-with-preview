package glsl.plugin.preview.run.settings

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.getOpenedProjects
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.util.ui.FormBuilder
import glsl.plugin.preview.SUPPORTED_SHADER_FILE_ENDINGS
import glsl.plugin.utils.idea.FileListCellRenderer
import java.util.Objects
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

class GlslShaderShaderRunSettings(val project: Project) {

    private val fileEditorManager: FileEditorManager = FileEditorManager.getInstance(project);


    // Form Components
    private val fileListBoxLabel: JLabel = JLabel("Shader File :")
    private val fileListBox = JComboBox(getOpenShaderFiles().toTypedArray()).apply {
        renderer = FileListCellRenderer
        addItemListener { e -> onFileChanged(e.item as? VirtualFile) }
    };

    private val form: JPanel

    private var onFileChanged: (virtualFile: VirtualFile?) -> Unit = {}
    

    init {
        fileListBoxLabel.labelFor = fileListBox
        this.form = createForm();

        // subscribe open/closing files
        project.messageBus.connect()
            .subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, object : FileEditorManagerListener {
                override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
                    updateFileListBox()
                }

                override fun fileClosed(source: FileEditorManager, file: VirtualFile) {
                    updateFileListBox()
                }
            })
    }

    private fun createForm(): JPanel {
        return FormBuilder.createFormBuilder()
            .addComponent(JLabel("Compiling :"))
            .addComponent(fileListBox)
            //todo add uniforms
            .panel
    }

    private fun getOpenShaderFiles(): List<VirtualFile> {

        val openFiles: Set<VirtualFile> = fileEditorManager.openFiles.toSet()
        return openFiles.filter { file -> file.extension in SUPPORTED_SHADER_FILE_ENDINGS }
    }

    fun getPanel(): JPanel {
        return form
    }

    fun getSelectedFile(): VirtualFile? {
        return getOpenShaderFiles().firstOrNull { fileListBox.selectedItem == it.name }
    }

    private fun updateFileListBox() {
        val currentSelection = fileListBox.selectedItem
        fileListBox.removeAllItems()
        getOpenShaderFiles().forEach { fileListBox.addItem(it) }
        if (currentSelection != null && getOpenShaderFiles().any { it.name == currentSelection }) {
            fileListBox.selectedItem = currentSelection
        }
    }

    fun setOnFileChanged(listener: (virtualFile: VirtualFile?) -> Unit ) {
        this.onFileChanged = listener;
    }

}


class ShaderSettingsEditor : SettingsEditor<ShaderRunConfiguration>() {

    val fileListbox = JComboBox(getOpenShaderFiles().toTypedArray()).apply {
        renderer = FileListCellRenderer
    }

    override fun resetEditorFrom(configuration: ShaderRunConfiguration) {
        val fileManager: VirtualFileManager = VirtualFileManager.getInstance()
        val fileEditorManager: FileEditorManager = FileEditorManager.getInstance(configuration.project);
        val currentFragFile = configuration.getFragmentFile();
        if(currentFragFile != null) {
            fileListbox.selectedItem = Objects.requireNonNull(fileManager.findFileByUrl(currentFragFile))
        } else {
            val currentFile = fileEditorManager.focusedEditor?.file
            if(currentFile?.extension in SUPPORTED_SHADER_FILE_ENDINGS) {
                fileListbox.selectedItem = currentFile
            }
        }
    }

    /**
     * Is called when user clicks "apply" on run config dialog.
     * Transfers ui state to run configuration
     */
    override fun applyEditorTo(configuration: ShaderRunConfiguration) {
        val fileEditorManager: FileEditorManager = FileEditorManager.getInstance(configuration.project);
        var selectedFile = fileListbox.selectedItem as VirtualFile?
        if(selectedFile == null) selectedFile = fileEditorManager.currentFile
        configuration.setFragmentFile(selectedFile!!.url)
    }

    override fun createEditor(): JComponent {
        return FormBuilder.createFormBuilder()
        .addLabeledComponent("Fragment Shader File:",fileListbox)
        .panel
    }

    private fun getOpenShaderFiles(): List<VirtualFile> {
        val availailableFiles: MutableList<VirtualFile> = ArrayList()
        getOpenedProjects().forEach { project ->
            val fileEditorManager: FileEditorManager = FileEditorManager.getInstance(project);
            val openFiles: Set<VirtualFile> = fileEditorManager.openFiles.toSet()
            availailableFiles += openFiles.filter { file -> file.extension in SUPPORTED_SHADER_FILE_ENDINGS }
        }
        return availailableFiles;
    }
}