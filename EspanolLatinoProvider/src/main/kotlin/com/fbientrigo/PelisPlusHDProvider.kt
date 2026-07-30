package com.fbientrigo

import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor
import org.jsoup.nodes.Element

// PelisPlusHD corre sobre el tema WordPress "Dooplay", así que sigue el flujo
// estándar de ese tema: listados con article.item y reproductores cargados por
// AJAX contra /wp-admin/admin-ajax.php (acción "doo_player_ajax").
// Si el sitio cambia de tema/dominio, ajustar los selectores de abajo.
class PelisPlusHDProvider : MainAPI() {
    override var mainUrl = "https://pelisplushd.mx"
    override var name = "PelisPlusHD"
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
        val title = this.selectFirst("h2.Title, h3.Title, .Title")?.text()?.trim() ?: return null
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
            Pair("$mainUrl/generos/estreno", "Estrenos"),
        )
        val items = urls.mapNotNull { (url, sectionName) ->
            try {
                val doc = app.get(url).document
                val list = doc.select("div.items article.item, article.item").mapNotNull { it.toSearchResult() }
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
        return doc.select("div.result-item article, article.item").mapNotNull { it.toSearchResult() }
    }

    override suspend fun load(url: String): LoadResponse {
        val doc = app.get(url).document
        val title = doc.selectFirst("div.data h1, h1")?.text()?.trim()
            ?: throw ErrorLoadingException("No se encontró el título")
        val poster = fixUrlNull(doc.selectFirst("div.poster img")?.attr("src"))
        val plot = doc.selectFirst("div.wp-content p")?.text()?.trim()
        val tags = doc.select("div.sgeneros a").map { it.text() }

        val episodes = doc.select("div#seasons div.se-c .episodios li, ul.episodios li").mapNotNull { el ->
            val epHref = fixUrlNull(el.selectFirst("a")?.attr("href")) ?: return@mapNotNull null
            val epTitle = el.selectFirst(".episodiotitle a, .epl-title")?.text()?.trim()
            val numerando = el.selectFirst(".numerando")?.text()
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

    data class DooPlayEmbed(
        @JsonProperty("embed_url") val embedUrl: String?,
        @JsonProperty("type") val type: String?,
    )

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val doc = app.get(data).document
        val options = doc.select("ul#playeroptionsul li, li.dooplay_player_option")
        if (options.isEmpty()) return false

        var found = false
        options.forEach { option ->
            val post = option.attr("data-post")
            val nume = option.attr("data-nume")
            val type = option.attr("data-type").ifBlank { "movie" }
            if (post.isBlank() || nume.isBlank()) return@forEach
            try {
                val embed = app.post(
                    "$mainUrl/wp-admin/admin-ajax.php",
                    data = mapOf(
                        "action" to "doo_player_ajax",
                        "post" to post,
                        "nume" to nume,
                        "type" to type,
                    ),
                    referer = data
                ).parsedSafe<DooPlayEmbed>()?.embedUrl ?: return@forEach

                loadExtractor(fixUrl(embed), data, subtitleCallback, callback)
                found = true
            } catch (e: Exception) {
                // Una opción de reproductor caída no debe tumbar al resto
            }
        }
        return found
    }
}
