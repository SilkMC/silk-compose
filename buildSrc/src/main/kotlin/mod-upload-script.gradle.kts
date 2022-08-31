import com.modrinth.minotaur.TaskModrinthSyncBody
import com.modrinth.minotaur.TaskModrinthUpload
import com.modrinth.minotaur.dependencies.DependencyType
import com.modrinth.minotaur.dependencies.ModDependency

plugins {
    id("fabric-loom")
    id("com.modrinth.minotaur")
}

modrinth {
    token.set(findProperty("modrinth.token").toString())

    projectId.set("xmi76FJb")
    versionNumber.set(rootProject.version.toString())
    versionType.set("release")
    gameVersions.set(listOf(minecraftVersion))
    loaders.set(listOf("fabric"))

    uploadFile.set(tasks.remapJar.get())

    dependencies.set(
        listOf(
            ModDependency("silk", DependencyType.REQUIRED),
            ModDependency("fabric-language-kotlin", DependencyType.REQUIRED),
        )
    )

    syncBodyFrom.set(provider {
        println("reading readme.md")
        rootProject.file("readme.md").readText()
    })
}
