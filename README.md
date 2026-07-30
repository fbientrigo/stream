# stream

Repositorio de plugins en español para [Cloudstream](https://github.com/recloudstream/cloudstream).

> [!NOTE]
> Este repositorio usa la [plantilla para plugins](https://github.com/recloudstream/TestPlugins) de Cloudstream, igual que [working](https://github.com/fbientrigo/working).

## Instalación

Para instalar este repo, ve a `Configuración > Extensiones > Agregar repositorio` en la app de Cloudstream y pega este link:

```
https://raw.githubusercontent.com/fbientrigo/stream/master/repo.json
```

Cloudstream leerá `repo.json`, que apunta a la lista de plugins ya compilados (`plugins.json`) publicada en la rama [`builds`](https://github.com/fbientrigo/stream/tree/builds). Esa rama la genera automáticamente el workflow de GitHub Actions en cada push a `main`.

## Fuentes incluidas

El plugin `EspanolLatinoProvider` registra dos proveedores:

- **PelisPlusHD** — [pelisplushd.mx](https://pelisplushd.mx/)
- **Cuevana** — [cuevanaseries.tv](https://cuevanaseries.tv/cuevana/)

> [!WARNING]
> Ambos proveedores se marcan como `status = 3` (beta) porque los selectores HTML no pudieron verificarse en vivo al escribirlos (los sitios devuelven 403 a peticiones automatizadas fuera de un navegador real). Si un sitio cambia de plantilla o de dominio, hay que actualizar los selectores en `EspanolLatinoProvider/src/main/kotlin/com/fbientrigo/*.kt`. Las PRs con correcciones son bienvenidas.

## Estructura del repositorio

```
stream/
├── repo.json                    # Manifest que se pega en Cloudstream
├── build.gradle.kts             # Configuración raíz de Gradle
├── settings.gradle.kts          # Incluye automáticamente cada carpeta con un plugin
├── EspanolLatinoProvider/       # Módulo del plugin
│   ├── build.gradle.kts         # Metadatos del plugin (nombre, idioma, tipos soportados...)
│   └── src/main/kotlin/com/fbientrigo/
│       ├── EspanolLatinoPlugin.kt   # Registra los MainAPI del plugin
│       ├── PelisPlusHDProvider.kt
│       └── CuevanaProvider.kt
└── .github/workflows/build.yml  # CI: compila los plugins y publica plugins.json + *.cs3 en la rama `builds`
```

## Compilar en local

```bash
./gradlew make            # compila los .cs3
./gradlew makePluginsJson # genera plugins.json a partir de los .cs3 compilados
```

Los artefactos quedan en `EspanolLatinoProvider/build/*.cs3` y `build/plugins.json`.

## Agregar un nuevo proveedor

1. Crea una carpeta nueva en la raíz (por ejemplo `MiSitioProvider/`) con su propio `build.gradle.kts` — `settings.gradle.kts` la detecta e incluye automáticamente.
2. Implementa un `MainAPI` en Kotlin siguiendo el patrón de `PelisPlusHDProvider.kt` o `CuevanaProvider.kt`.
3. Regístralo en un `Plugin` con `@CloudstreamPlugin` (puedes añadirlo al `EspanolLatinoPlugin` existente o crear uno nuevo).
4. Haz push a `main`: el workflow de CI se encarga de compilar y publicar en la rama `builds`.

## Contribuciones

Si deseas contribuir, siéntete libre de hacer un fork y enviar un pull request.

## Licencia

Este proyecto está bajo la licencia MIT (ver [LICENSE](LICENSE)).
