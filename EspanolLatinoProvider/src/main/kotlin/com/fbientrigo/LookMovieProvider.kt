package com.fbientrigo

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor
import org.jsoup.nodes.Element

// AVISO: LookMovie es de los sitios más difíciles de scrapear del grupo:
// usa Cloudflare delante y buena parte del listado se arma con JavaScript
// en el cliente, algo que Jsoup (usado por app.get(...).document) no
// ejecuta. Esta implementación cubre el caso en que el HTML venga
// server-side-rendered; si getMainPage()/search() devuelven listas vacías
// en la práctica, lo más probable es que haga falta apuntar a su API JSON
// interna en lugar de parsear HTML.
class LookMovieProvider : MainAPI() {
    override var mainUrl = "https://www.lookmovie2.to"
    override var name = "LookMovie"
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
        val title = this.selectFirst("h2, h3, .title")?.text()?.trim()
            ?: this.selectFirst("img")?.attr("alt")?.trim()?.ifBlank { null }
            ?: return null
        val posterUrl = fixUrlNull(
            this.selectFirst("img")?.attr("data-src")?.ifBlank { null }
                ?: this.selectFirst("img")?.attr("src")
        )
        val type = if (href.contains("/shows/") || href.contains("/tv/")) TvType.TvSeries else TvType.Movie
        return newMovieSearchResponse(title, href, type) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val urls = listOf(
            Pair("$mainUrl/movies", "Películas"),
            Pair("$mainUrl/shows", "Series"),
        )
        val items = urls.mapNotNull { (url, sectionName) ->
            try {
                val doc = app.get(url).document
                val list = doc.select("div.movie-item, article, div.media-item")
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
        val doc = app.get("$mainUrl/search?q=$query").document
        return doc.select("div.movie-item, article, div.media-item").mapNotNull { it.toSearchResult() }
    }

    override suspend fun load(url: String): LoadResponse {
        val doc = app.get(url).document
        val title = doc.selectFirst("h1")?.text()?.trim()
            ?: throw ErrorLoadingException("No se encontró el título")
        val poster = fixUrlNull(doc.selectFirst("div.poster img, .movie-poster img")?.attr("src"))
        val plot = doc.selectFirst("div.description, .movie-description p")?.text()?.trim()
        val tags = doc.select("div.genres a, .movie-genres a").map { it.text() }

        val episodes = doc.select("div.episode-item, li.episode").mapNotNull { el ->
            val epHref = fixUrlNull(el.selectFirst("a")?.attr("href")) ?: return@mapNotNull null
            val epTitle = el.selectFirst(".episode-title")?.text()?.trim()
            val season = el.attr("data-season").toIntOrNull()
            val episode = el.attr("data-episode").toIntOrNull()
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

        doc.select("iframe").forEach { iframe ->
            val src = iframe.attr("src").ifBlank { iframe.attr("data-src") }
            if (src.isNotBlank()) {
                loadExtractor(fixUrl(src), data, subtitleCallback, callback)
                found = true
            }
        }

        // Fallback: buscar un .m3u8 embebido directamente en algún <script>
        if (!found) {
            val m3u8 = Regex("https?://[^\"'\\s]+\\.m3u8[^\"'\\s]*")
                .find(doc.html())?.value
            if (m3u8 != null) {
                loadExtractor(m3u8, data, subtitleCallback, callback)
                found = true
            }
        }

        return found
    }
}
