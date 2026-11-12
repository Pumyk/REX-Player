package xyz.mpv.rex.utils

import org.junit.Assert.assertEquals
import org.junit.Test
import xyz.mpv.rex.utils.media.MediaInfoParser

class MediaInfoParserTest {
    @Test
    fun spacedSeasonEpisodeIsParsedAndStripped() {
        val info = MediaInfoParser.parse("Demon Slayer Kimetsu no Yaiba S01 E06")
        assertEquals("Demon Slayer Kimetsu no Yaiba", info.title.trim())
        assertEquals(1, info.season)
        assertEquals(6, info.episode)
    }

    @Test
    fun standardSceneNameStillWorks() {
        val info = MediaInfoParser.parse("Jujutsu.Kaisen.S02E05.1080p.WEBRip.x265-PSA.mkv")
        assertEquals("Jujutsu Kaisen", info.title.trim())
        assertEquals(2, info.season)
        assertEquals(5, info.episode)
    }

    @Test
    fun movieKeepsItsYear() {
        val info = MediaInfoParser.parse("The.Dark.Knight.2008.1080p.BluRay.mkv")
        assertEquals("The Dark Knight", info.title.trim())
        assertEquals("2008", info.year)
        assertEquals("movie", info.type)
    }
}
