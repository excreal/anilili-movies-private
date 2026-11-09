package com.example.animepahedl.metadata

import com.example.animepahedl.*

/** Converts the source-complete metadata response into the compact, persistable app model. */
internal fun MetadataDetails.toAppMetadata(): SiteShowMetadata {
    val item = record
    return SiteShowMetadata(
        description = item.description,
        alternativeTitles = item.alternativeTitles,
        images = item.images.map { SiteShowImage(it.url, it.role, it.width, it.height, it.caption) },
        genres = item.genres.map { it.name },
        categories = item.categories.map { it.name },
        tags = item.tags.map { it.name },
        genreTerms = item.genres.map { SiteShowTerm(it.name, it.id, it.url) },
        categoryTerms = item.categories.map { SiteShowTerm(it.name, it.id, it.url) },
        tagTerms = item.tags.map { SiteShowTerm(it.name, it.id, it.url) },
        ratings = item.ratings.map { SiteShowRating(it.value, it.scale, it.votes, it.source, it.label) },
        people = item.people.map { SiteShowPerson(it.name, it.role, it.url) },
        countries = item.countries,
        languages = item.languages,
        studios = item.studios.map { it.name },
        status = item.status,
        format = item.format,
        year = item.year,
        episodeCount = item.episodeCount ?: episodes.size.takeIf { it > 0 },
        durationSeconds = item.durationSeconds,
        releaseDate = item.releaseDate,
        updatedAt = item.updatedAt,
        externalIds = item.externalIds,
        attributes = item.attributes,
        episodes = episodes.map { episode ->
            SiteEpisodeMetadata(episode.number, episode.title, episode.description, episode.durationSeconds,
                episode.releaseDate, episode.subtitles.map { listOfNotNull(it.label.takeIf(String::isNotBlank), it.language).distinct().joinToString(" · ") }
                    .filter(String::isNotBlank).distinct(), episode.attributes)
        },
    )
}
