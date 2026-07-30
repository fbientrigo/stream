// Use an integer for version numbers
version = 1

cloudstream {
    // All of these properties are optional, you can safely remove any of them.

    description = "Películas y series en español latino: Cuevana, Cuevana3 y LookMovie"
    authors = listOf("fbientrigo")

    /**
    * Status int as one of the following:
    * 0: Down
    * 1: Ok
    * 2: Slow
    * 3: Beta-only
    **/
    status = 3 // Beta: los selectores no han sido verificados contra los sitios en vivo, revisar/ajustar si algo falla

    tvTypes = listOf(
        "Movie",
        "TvSeries",
    )

    language = "es"

    // Random favicon, cámbialo si tienes uno propio
    iconUrl = "http://www.google.com/s2/favicons?domain=cuevana3.gs"
}
