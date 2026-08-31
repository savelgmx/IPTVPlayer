package com.portfolio.iptvplayer.data.m3u

import com.portfolio.iptvplayer.domain.model.Channel

/**
 * Parses the extended M3U format used by virtually every IPTV playlist:
 *
 * #EXTM3U
 * #EXTINF:-1 tvg-logo="https://example.com/logo.png" group-title="News",Channel Name
 * https://example.com/stream.m3u8
 *
 * Deliberately tolerant: unknown attributes are ignored, a missing logo or
 * group is fine, and a malformed entry is skipped rather than crashing the
 * whole playlist load — a single bad line in a 500-channel list someone
 * pasted in should not take down the app.
 */
object M3UParser {

    private val logoRegex = Regex("""tvg-logo="([^"]*)"""")
    private val groupRegex = Regex("""group-title="([^"]*)"""")
    private val nameRegex = Regex(""",(.*)$""")

    fun parse(rawContent: String): List<Channel> {
        val lines = rawContent.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toList()

        val channels = mutableListOf<Channel>()
        var pendingLogo: String? = null
        var pendingGroup: String? = null
        var pendingName: String? = null

        for (line in lines) {
            when {
                line.startsWith("#EXTM3U") -> continue

                line.startsWith("#EXTINF") -> {
                    pendingLogo = logoRegex.find(line)?.groupValues?.get(1)
                    pendingGroup = groupRegex.find(line)?.groupValues?.get(1)
                    pendingName = nameRegex.find(line)?.groupValues?.get(1)?.trim()
                }

                line.startsWith("#") -> continue // any other directive/comment: ignore

                else -> {
                    // A non-comment, non-empty line after #EXTINF is the stream URL.
                    val name = pendingName
                    if (!name.isNullOrBlank()) {
                        channels += Channel(
                            // The stream URL is stable across repeated parses of the
                            // same playlist (unlike a freshly-generated UUID), which
                            // matters because the grid and the playback screen each
                            // parse the playlist independently and must agree on ids.
                            id = line,
                            name = name,
                            logoUrl = pendingLogo,
                            groupTitle = pendingGroup,
                            streamUrl = line
                        )
                    }
                    pendingLogo = null
                    pendingGroup = null
                    pendingName = null
                }
            }
        }

        return channels
    }
}
