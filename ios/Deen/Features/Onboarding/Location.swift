import CoreLocation
import DeenKit
import SwiftUI

/// One coarse location fix, asked for only when the user taps "Use my location". It never leaves the phone.
@MainActor
final class LocationProvider: NSObject, CLLocationManagerDelegate {
    private let manager = CLLocationManager()
    private var continuation: CheckedContinuation<CLLocation?, Never>?

    func requestOnce() async -> CLLocation? {
        manager.delegate = self
        manager.desiredAccuracy = kCLLocationAccuracyKilometer
        return await withCheckedContinuation { continuation in
            self.continuation = continuation
            switch manager.authorizationStatus {
            case .notDetermined: manager.requestWhenInUseAuthorization()
            case .denied, .restricted: finish(nil)
            default: manager.requestLocation()
            }
        }
    }

    private func finish(_ location: CLLocation?) {
        continuation?.resume(returning: location)
        continuation = nil
    }

    nonisolated func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        let status = manager.authorizationStatus
        Task { @MainActor in
            guard self.continuation != nil else { return }
            switch status {
            case .authorizedWhenInUse, .authorizedAlways: self.manager.requestLocation()
            case .denied, .restricted: self.finish(nil)
            default: break
            }
        }
    }

    nonisolated func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        let last = locations.last
        Task { @MainActor in self.finish(last) }
    }

    nonisolated func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
        Task { @MainActor in self.finish(nil) }
    }
}

/// "Use my location" plus a search over the bundled cities (works offline, Latin or Arabic script).
struct LocationPicker: View {
    var onPick: (City) -> Void
    @Environment(AppModel.self) private var model
    @State private var query = ""
    @State private var results: [City] = []
    @State private var detecting = false
    @State private var message: String?
    @State private var provider = LocationProvider()

    var body: some View {
        List {
            Section {
                Button {
                    Task { await useDevice() }
                } label: {
                    HStack {
                        Label(L(detecting ? "feature_onboarding_location_detecting" : "feature_onboarding_location_use_device"),
                              systemImage: "location.fill")
                        Spacer()
                        if detecting { ProgressView() }
                    }
                }
                .disabled(detecting)
                if let message {
                    Text(message).font(.footnote).foregroundStyle(.secondary)
                }
            }
            if !query.isEmpty {
                Section {
                    if results.isEmpty {
                        Text(L("feature_onboarding_location_no_results")).foregroundStyle(.secondary)
                    }
                    ForEach(results) { city in
                        Button {
                            onPick(city)
                        } label: {
                            VStack(alignment: .leading, spacing: 2) {
                                Text(isArabicUI ? (city.arabicName ?? city.name) : city.name).foregroundStyle(.primary)
                                Text([city.region, Locale.current.localizedString(forRegionCode: city.countryCode) ?? city.countryCode]
                                    .filter { !$0.isEmpty }.joined(separator: ", "))
                                    .font(.footnote)
                                    .foregroundStyle(.secondary)
                            }
                        }
                    }
                }
            }
        }
        .searchable(text: $query, placement: .navigationBarDrawer(displayMode: .always), prompt: L("feature_onboarding_location_search_hint"))
        .task(id: query) {
            guard let index = await model.loadCities() else { return }
            let q = query
            results = await Task.detached { index.search(q) }.value
        }
    }

    private func useDevice() async {
        detecting = true
        message = nil
        defer { detecting = false }
        guard let fix = await provider.requestOnce() else {
            message = L(CLLocationManager().authorizationStatus == .denied
                ? "feature_onboarding_location_permission_denied" : "feature_onboarding_location_failed")
            return
        }
        guard let index = await model.loadCities(),
              let nearest = index.nearest(to: Coordinates(latitude: fix.coordinate.latitude, longitude: fix.coordinate.longitude)) else {
            message = L("feature_onboarding_location_failed")
            return
        }
        // The exact coordinates, with the nearest city's name and time zone.
        let city = nearest.city
        onPick(City(id: city.id, name: city.name, arabicName: city.arabicName, countryCode: city.countryCode, region: city.region,
                    coordinates: Coordinates(latitude: fix.coordinate.latitude, longitude: fix.coordinate.longitude),
                    population: city.population, timeZoneID: city.timeZoneID))
    }
}

/// The picker as its own screen (from Today's location button or Settings).
struct LocationSearchView: View {
    var onPick: (City) -> Void
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        LocationPicker { city in
            onPick(city)
            dismiss()
        }
        .navigationTitle(L("feature_onboarding_location_picker_title"))
        .navigationBarTitleDisplayMode(.inline)
    }
}
