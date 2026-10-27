#!/usr/bin/env bash
set -euo pipefail
# Requires host ffmpeg and adb. Creates a continuous 24-minute H.264/AAC HLS
# fixture outside the repository and supplies it to the optional device test.
fixture_dir="$(mktemp -d /tmp/animepahe-benchmark.XXXXXX)"
trap 'rm -rf "$fixture_dir"' EXIT
ffmpeg -hide_banner -loglevel error -f lavfi -i color=c=0x203050:size=160x90:rate=24 \
  -f lavfi -i sine=frequency=440:sample_rate=22050 -t 1440 \
  -c:v libx264 -preset ultrafast -crf 40 -g 144 -c:a aac -b:a 16k \
  -f hls -hls_time 6 -hls_playlist_type vod \
  -hls_segment_filename "$fixture_dir/segment%d.ts" "$fixture_dir/index.m3u8"
adb push "$fixture_dir/." /sdcard/Android/data/com.example.animepahedl/files/benchmark-hls/
adb shell am instrument -w -e class \
  com.example.animepahedl.HlsExportTest#fullLengthEpisodeExportsWithoutRepeatedSeekTableRebuilds \
  com.example.animepahedl.test/androidx.test.runner.AndroidJUnitRunner
