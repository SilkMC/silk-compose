## silk-compose

Silk Compose is a module for [SilkMC](https://github.com/SilkMC/silk) which makes it possible to use Compose
Multiplatform inside Minecraft, both on the server- and the client-side (the latter only planned).

### Demo

https://user-images.githubusercontent.com/52456572/175792435-1a7f0e30-76cc-4739-87dc-0ec8ed222d4a.mp4

### Dependency

#### Development Environment

Silk Compose is [available on **Maven Central**](https://repo1.maven.org/maven2/net/silkmc/silk-compose/).

```kotlin
modImplementation("net.silkmc:silk-compose:$version")
```

#### As a normal user

Download silk-compose [from **Modrinth**](https://modrinth.com/mod/silk-compose) to provide it at runtime and add it as a mod.

### Usage

The docs are located at [**silkmc.net/silk-compose/docs**](https://silkmc.net/silk-compose/docs/).

To display a basic UI for a player, do the following:

```kotlin
player.displayComposable(8, 6) {
    YourComposableFunction()
}

@Composable
fun YourComposableFunction() {
    Button(
        onClick = { logInfo("clicked button") }
    ) {
        Text("Click me")
    }
}
```
