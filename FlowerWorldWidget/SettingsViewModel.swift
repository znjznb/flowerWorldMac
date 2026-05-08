import Foundation
import Combine

class SettingsViewModel: ObservableObject {
    @Published var backendURL: String {
        didSet {
            UserDefaults.standard.set(backendURL, forKey: "backendURL")
        }
    }

    var apiBase: String {
        let url = backendURL.hasSuffix("/") ? String(backendURL.dropLast()) : backendURL
        return "\(url)/api"
    }

    init() {
        self.backendURL = UserDefaults.standard.string(forKey: "backendURL") ?? "http://localhost:8080"
    }
}
