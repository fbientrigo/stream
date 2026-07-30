package com.fbientrigo

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor
import org.jsoup.nodes.Element

// Los clones de Cuevana cambian de dominio y de plantilla con frecuencia.
// Esta implementación cubre los selectores más comunes vistos en ese tipo de
// sitios (listas "MovieList"/"TPost" y reproductores servidos por
// "TPlayerNv"/iframe directo). Si el sitio cambia de estructura, es aquí
// donde hay que ajustar los selectores.
class CuevanaProvider : MainAPI() {
    override var mainUrl = "https://cuevanaseries.tv"
    override var name = "Cuevana"
    override var lang = "es"
    override val hasMainPage = true
    override val hasChromecastSupport = true
    override val hasDownloadSupport = true
    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.TvSeries,
    )

    private fun Element.toSearchResult(): SearchResponse? {
        val href = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val title = this.selectFirst("h2.Title, h3.Title, .Title")?.text()?.trim()
            ?: this.selectFirst("img")?.attr("alt")?.trim()?.ifBlank { null }
            ?: return null
        val posterUrl = fixUrlNull(
            this.selectFirst("img")?.attr("data-src")?.ifBlank { null }
                ?: this.selectFirst("img")?.attr("src")
        )
        val type = if (href.contains("/serie")) TvType.TvSeries else TvType.Movie
        return newMovieSearchResponse(title, href, type) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val urls = listOf(
            Pair("$mainUrl/peliculas", "Películas"),
            Pair("$mainUrl/series", "Series"),
        )
        val items = urls.mapNotNull { (url, sectionName) ->
            try {
                val doc = app.get(url).document
                val list = doc.select("ul.MovieList li article, div.Posters article, article.item")
                    .mapNotNull { it.toSearchResult() }
                if (list.isEmpty()) null else HomePageList(sectionName, list)
            } catch (e: Exception) {
                null
            }
        }
        if (items.isEmpty()) throw ErrorLoadingException()
        return newHomePageResponse(items)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val doc = app.get("$mainUrl/?s=$query").document
        return doc.select("ul.MovieList li article, article.item, div.result-item")
            .mapNotNull { it.toSearchResult() }
    }

    override suspend fun load(url: String): LoadResponse {
        val doc = app.get(url).document
        val title = doc.selectFirst("h1.Title, div.data h1, h1")?.text()?.trim()
            ?: throw ErrorLoadingException("No se encontró el título")
        val poster = fixUrlNull(doc.selectFirst("div.Image img, div.poster img")?.attr("src"))
        val plot = doc.selectFirst("div.Description p, div.wp-content p")?.text()?.trim()
        val tags = doc.select("ul.InfoList a, div.sgeneros a").map { it.text() }

        val episodes = doc.select("ul.episodios li, div.TPTblCn tr").mapNotNull { el ->
            val epHref = fixUrlNull(el.selectFirst("a")?.attr("href")) ?: return@mapNotNull null
            val epTitle = el.selectFirst(".episodiotitle a, .epl-title, td.MvTbTtl a")?.text()?.trim()
            val numerando = el.selectFirst(".numerando, td.MvTbNum")?.text()
            val season = numerando?.split("-")?.getOrNull(0)?.trim()?.toIntOrNull()
            val episode = numerando?.split("-")?.getOrNull(1)?.trim()?.toIntOrNull()
            newEpisode(epHref) {
                this.name = epTitle
                this.season = season
                this.episode = episode
            }
        }

        return if (episodes.isNotEmpty()) {
            newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
                this.posterUrl = poster
                this.plot = plot
                this.tags = tags
            }
        } else {
            newMovieLoadResponse(title, url, TvType.Movie, url) {
                this.posterUrl = poster
                this.plot = plot
                this.tags = tags
            }
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val doc = app.get(data).document
        var found = false

        // 1) iframe directo embebido en la página de reproducción
        doc.select("div.Video iframe, div.movie_player iframe, #Video iframe").forEach { iframe ->
            val src = iframe.attr("src").ifBlank { iframe.attr("data-src") }
            if (src.isNotBlank()) {
                loadExtractor(fixUrl(src), data, subtitleCallback, callback)
                found = true
            }
        }

        // 2) lista de servidores con el link embebido en un atributo data-*
        doc.select("ul.TPlayerNv li, li[data-video], li[data-server]").forEach { server ->
            val embed = server.attr("data-video").ifBlank { server.attr("data-server") }
            if (embed.isNotBlank()) {
                try {
                    loadExtractor(fixUrl(embed), data, subtitleCallback, callback)
                    found = true
                } catch (e: Exception) {
                    // Un servidor caído no debe tumbar al resto
                }
            }
        }

        return found
    }
}
