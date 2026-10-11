import AVFoundation
import DeenKit
import MediaPlayer
import SwiftUI

/// Recites a surah verse by verse (streamed from EveryAyah, kept on the phone after the first play)
/// and reports the word being recited, for player mode's green highlight.
@MainActor
@Observable
final class QuranPlayer {
    static let shared = QuranPlayer()

    /// Playing (or paused in) surah, verse (0 = the opening Bismillah) and word.
    private(set) var surah: Int?
    private(set) var ayah: Int?
    private(set) var word: Int?
    private(set) var isPlaying = false
    private(set) var failed = false
    private(set) var reciter: Reciter = .alafasy

    private let player = AVQueuePlayer()
    private var queue: [(surah: Int, ayah: Int)] = []
    private var timeObserver: Any?
    private var endObserver: NSObjectProtocol?
    private var failObserver: NSObjectProtocol?
    private var timings: [Reciter: [Int: [WordTiming]]] = [:]
    private var title = ""

    private init() {
        timeObserver = player.addPeriodicTimeObserver(forInterval: CMTime(value: 1, timescale: 30), queue: .main) { [weak self] time in
            MainActor.assumeIsolated { self?.tick(time) }
        }
        setUpRemoteCommands()
    }

    var active: Bool { surah != nil }

    /// Recites [surah] from [ayah] to its end. Verse 1 of most surahs starts with the Bismillah.
    func play(surah: Surah, from ayah: Int, reciter: Reciter) {
        stop()
        self.reciter = reciter
        self.surah = surah.number
        title = surah.title
        queue = []
        if ayah == 1, surah.number != 1, surah.number != 9 { queue.append((surah.number, 0)) }
        queue += (ayah...surah.ayahCount).map { (surah.number, $0) }
        try? AVAudioSession.sharedInstance().setCategory(.playback, mode: .spokenAudio)
        try? AVAudioSession.sharedInstance().setActive(true)
        failed = false
        for item in queue.prefix(4) { player.insert(makeItem(item), after: nil) }
        observeEnd()
        player.play()
        isPlaying = true
        self.ayah = queue.first?.ayah
        prefetch()
        updateNowPlaying()
    }

    func toggle() {
        guard active else { return }
        if isPlaying { player.pause() } else { player.play() }
        isPlaying.toggle()
        updateNowPlaying()
    }

    func next() {
        guard let current = ayah, let s = surah, let index = queue.firstIndex(where: { $0.surah == s && $0.ayah == current }),
              index + 1 < queue.count else { return }
        restart(at: index + 1)
    }

    func previous() {
        guard let current = ayah, let s = surah, let index = queue.firstIndex(where: { $0.surah == s && $0.ayah == current }),
              index > 0 else { return }
        restart(at: index - 1)
    }

    func stop() {
        player.pause()
        player.removeAllItems()
        surah = nil
        ayah = nil
        word = nil
        isPlaying = false
        MPNowPlayingInfoCenter.default().nowPlayingInfo = nil
    }

    // MARK: Internals

    private func restart(at index: Int) {
        player.removeAllItems()
        let rest = Array(queue[index...])
        for item in rest.prefix(4) { player.insert(makeItem(item), after: nil) }
        ayah = rest.first?.ayah
        word = nil
        player.play()
        isPlaying = true
        updateNowPlaying()
    }

    /// The Bismillah is verse 1:1's recording.
    private func source(_ item: (surah: Int, ayah: Int)) -> (surah: Int, ayah: Int) {
        item.ayah == 0 ? (1, 1) : item
    }

    private func makeItem(_ item: (surah: Int, ayah: Int)) -> AVPlayerItem {
        let s = source(item)
        let local = Self.cacheFile(reciter: reciter, surah: s.surah, ayah: s.ayah)
        let url = FileManager.default.fileExists(atPath: local.path) ? local : reciter.url(surah: s.surah, ayah: s.ayah)
        let playerItem = AVPlayerItem(url: url)
        return playerItem
    }

    private func observeEnd() {
        if let endObserver { NotificationCenter.default.removeObserver(endObserver) }
        if let failObserver { NotificationCenter.default.removeObserver(failObserver) }
        endObserver = NotificationCenter.default.addObserver(forName: AVPlayerItem.didPlayToEndTimeNotification, object: nil, queue: .main) { [weak self] _ in
            MainActor.assumeIsolated { self?.advance() }
        }
        failObserver = NotificationCenter.default.addObserver(forName: AVPlayerItem.failedToPlayToEndTimeNotification, object: nil, queue: .main) { [weak self] _ in
            MainActor.assumeIsolated {
                self?.failed = true
                self?.isPlaying = false
            }
        }
    }

    /// One verse ended: move the highlight on and keep four verses queued.
    private func advance() {
        guard let current = ayah, let s = surah, let index = queue.firstIndex(where: { $0.surah == s && $0.ayah == current }) else { return }
        if index + 1 >= queue.count {
            stop()
            return
        }
        ayah = queue[index + 1].ayah
        word = nil
        let queued = player.items().count
        let nextToAdd = index + 1 + queued
        if nextToAdd < queue.count { player.insert(makeItem(queue[nextToAdd]), after: nil) }
        updateNowPlaying()
    }

    private func tick(_ time: CMTime) {
        guard let s = surah, let a = ayah, isPlaying else { return }
        let ms = Int(time.seconds * 1000)
        let key = a == 0 ? 1001 : s * 1000 + a
        word = table()[key]?.wordAt(ms)
    }

    private func table() -> [Int: [WordTiming]] {
        if let loaded = timings[reciter] { return loaded }
        let url = Bundle.main.resourceURL?.appendingPathComponent("audio/timings/\(reciter.rawValue).tsv")
        let parsed = url.flatMap { try? String(contentsOf: $0, encoding: .utf8) }.map(parseTimings) ?? [:]
        timings[reciter] = parsed
        return parsed
    }

    // MARK: Cache

    static func cacheFile(reciter: Reciter, surah: Int, ayah: Int) -> URL {
        let base = FileManager.default.urls(for: .cachesDirectory, in: .userDomainMask)[0]
        return base.appendingPathComponent("quran-audio/\(reciter.rawValue)/\(String(format: "%03d%03d", surah, ayah)).mp3")
    }

    /// Keeps the surah's recordings on the phone so they play offline next time.
    private func prefetch() {
        let items = queue.map(source)
        let reciter = reciter
        Task.detached(priority: .utility) {
            for item in items {
                let file = QuranPlayer.cacheFile(reciter: reciter, surah: item.surah, ayah: item.ayah)
                if FileManager.default.fileExists(atPath: file.path) { continue }
                guard let (data, response) = try? await URLSession.shared.data(from: reciter.url(surah: item.surah, ayah: item.ayah)),
                      (response as? HTTPURLResponse)?.statusCode == 200 else { continue }
                try? FileManager.default.createDirectory(at: file.deletingLastPathComponent(), withIntermediateDirectories: true)
                try? data.write(to: file)
            }
        }
    }

    // MARK: Lock screen

    private func updateNowPlaying() {
        guard let a = ayah else { return }
        MPNowPlayingInfoCenter.default().nowPlayingInfo = [
            MPMediaItemPropertyTitle: a == 0 ? L("feature_quran_bismillah") : "\(title) · \(L("feature_quran_verse", Formats.localDigits("\(a)")))",
            MPMediaItemPropertyArtist: reciter.name(arabic: isArabicUI),
            MPNowPlayingInfoPropertyPlaybackRate: isPlaying ? 1.0 : 0.0
        ]
    }

    private func setUpRemoteCommands() {
        let center = MPRemoteCommandCenter.shared()
        center.playCommand.addTarget { [weak self] _ in
            MainActor.assumeIsolated { if self?.isPlaying == false { self?.toggle() } }
            return .success
        }
        center.pauseCommand.addTarget { [weak self] _ in
            MainActor.assumeIsolated { if self?.isPlaying == true { self?.toggle() } }
            return .success
        }
        center.nextTrackCommand.addTarget { [weak self] _ in
            MainActor.assumeIsolated { self?.next() }
            return .success
        }
        center.previousTrackCommand.addTarget { [weak self] _ in
            MainActor.assumeIsolated { self?.previous() }
            return .success
        }
    }
}

/// Player mode's bar: reciter, verse, previous / play-pause / next, close.
struct PlayerBar: View {
    let surah: Surah
    var onReciter: () -> Void
    var onClose: () -> Void
    @State private var player = QuranPlayer.shared
    @Environment(AppModel.self) private var model

    var body: some View {
        let playingHere = player.surah == surah.number
        HStack(spacing: 14) {
            Button(action: onReciter) {
                VStack(alignment: .leading, spacing: 2) {
                    Text(reciterName).font(.subheadline.weight(.semibold)).lineLimit(1)
                    Text(status(playingHere)).font(.caption).foregroundStyle(.secondary).lineLimit(1)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            }
            .buttonStyle(.plain)
            GlassEffectContainer(spacing: 8) {
                HStack(spacing: 8) {
                    Button { player.previous() } label: { Image(systemName: "backward.fill") }
                        .accessibilityLabel(L("feature_quran_previous_verse"))
                    Button {
                        if playingHere { player.toggle() } else {
                            player.play(surah: surah, from: model.settings.lastRead?.surah == surah.number ? (model.settings.lastRead?.ayah ?? 1) : 1,
                                        reciter: Reciter(rawValue: model.settings.reciterID) ?? .alafasy)
                        }
                    } label: {
                        Image(systemName: playingHere && player.isPlaying ? "pause.fill" : "play.fill").font(.title3)
                    }
                    .buttonStyle(.glassProminent)
                    .accessibilityLabel(L(playingHere && player.isPlaying ? "feature_quran_pause" : "feature_quran_play"))
                    Button { player.next() } label: { Image(systemName: "forward.fill") }
                        .accessibilityLabel(L("feature_quran_next_verse"))
                }
                .buttonStyle(.glass)
            }
            Button(action: onClose) { Image(systemName: "xmark") }
                .buttonStyle(.glass)
                .accessibilityLabel(L("feature_quran_close_player"))
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 10)
        .glassEffect(.regular, in: .rect(cornerRadius: 28))
        .padding(.horizontal, 12)
        .padding(.bottom, 6)
    }

    private var reciterName: String {
        (Reciter(rawValue: model.settings.reciterID) ?? .alafasy).name(arabic: isArabicUI)
    }

    private func status(_ playingHere: Bool) -> String {
        if player.failed { return L("feature_quran_player_offline") }
        guard playingHere, let ayah = player.ayah else { return L("feature_quran_player_hint") }
        return ayah == 0 ? L("feature_quran_bismillah") : L("feature_quran_verse", Formats.localDigits("\(ayah)"))
    }
}

/// The verse with the recited word lit in green (words counted as in the timings: tokens with letters).
func highlighted(_ text: String, word: Int?, green: Color) -> AttributedString {
    var result = AttributedString()
    var index = 0
    for (i, token) in text.split(separator: " ", omittingEmptySubsequences: false).enumerated() {
        if i > 0 { result += AttributedString(" ") }
        let isWord = token.contains { $0.isLetter }
        var part = AttributedString(String(token))
        if isWord, index == word {
            part.foregroundColor = green
            part.backgroundColor = green.opacity(0.14)
        }
        result += part
        if isWord { index += 1 }
    }
    return result
}
