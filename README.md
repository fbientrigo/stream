# stream

Repositorio de plugins en español para [Cloudstream](https://github.com/recloudstream/cloudstream).

> [!NOTE]
> Este repositorio usa la [plantilla para plugins](https://github.com/recloudstream/TestPlugins) de Cloudstream, igual que [working](https://github.com/fbientrigo/working).

## Instalación

Para instalar este repo, ve a `Configuración > Extensiones > Agregar repositorio` en la app de Cloudstream y pega este link:

```
https://raw.githubusercontent.com/fbientrigo/stream/main/repo.json
```

Cloudstream leerá `repo.json`, que apunta a la lista de plugins ya compilados (`plugins.json`) publicada en la rama [`builds`](https://github.com/fbientrigo/stream/tree/builds). Esa rama la genera automáticamente el workflow de GitHub Actions en cada push a `main`.

## Fuentes incluidas

El plugin `EspanolLatinoProvider` registra tres proveedores:

- **Cuevana** — [cuevana.com.es](https://cuevana.com.es/)
- **Cuevana3** — [cuevana3.gs](https://cuevana3.gs/) (mismo software que Cuevana, dominio espejo — comparten toda la lógica de scraping en `CuevanaProvider.kt` vía la clase base `CuevanaCloneProvider`)
- **LookMovie** — [lookmovie2.to](https://www.lookmovie2.to/)

> [!WARNING]
> Los tres proveedores se marcan como `status = 3` (beta) porque los selectores HTML no pudieron verificarse en vivo al escribirlos (los sitios devuelven 403 a peticiones automatizadas fuera de un navegador real). LookMovie en particular usa Cloudflare y arma parte del listado con JavaScript del lado del cliente, que Jsoup no ejecuta — si sus listados vienen vacíos, probablemente haga falta apuntar a su API JSON interna en vez de parsear HTML. Si un sitio cambia de plantilla o de dominio, hay que actualizar los selectores en `EspanolLatinoProvider/src/main/kotlin/com/fbientrigo/*.kt`. Las PRs con correcciones son bienvenidas.

### Candidato para una 4ta fuente

Se recomienda **Cinecalidad** (sitio histórico en español para películas, tema WordPress similar a Cuevana) — pero como los dominios de estos sitios rotan seguido (fue justo el problema que teníamos con PelisPlusHD), conviene confirmar la URL actual antes de implementarlo, en vez de asumir un dominio que puede estar muerto en unos meses.

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
│       ├── CuevanaProvider.kt       # CuevanaCloneProvider (base) + Cuevana/Cuevana3
│       └── LookMovieProvider.kt
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
2. Implementa un `MainAPI` en Kotlin siguiendo el patrón de `CuevanaProvider.kt` o `LookMovieProvider.kt`.
3. Regístralo en un `Plugin` con `@CloudstreamPlugin` (puedes añadirlo al `EspanolLatinoPlugin` existente o crear uno nuevo).
4. Haz push a `main`: el workflow de CI se encarga de compilar y publicar en la rama `builds`.

## Contribuciones

Si deseas contribuir, siéntete libre de hacer un fork y enviar un pull request.

## Licencia

Este proyecto está bajo la licencia MIT (ver [LICENSE](LICENSE)).
