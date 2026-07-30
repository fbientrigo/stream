rootProject.name = "stream"

// Este archivo indica los proyectos incluidos.
// Todos los proyectos nuevos serán incluidos automáticamente a no ser que se especifique en la variable "disabled".

val disabled = listOf<String>()

File(rootDir, ".").eachDir { dir ->
    if (!disabled.contains(dir.name) && File(dir, "build.gradle.kts").exists()) {
        include(dir.name)
    }
}

fun File.eachDir(block: (File) -> Unit) {
    listFiles()?.filter { it.isDirectory }?.forEach { block(it) }
}

// Plugins are included like this
// include("PluginName")
