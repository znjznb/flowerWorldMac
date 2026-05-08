import SwiftUI

@main
struct FlowerWorldWidgetApp: App {
    @StateObject private var settings = SettingsViewModel()

    var body: some Scene {
        MenuBarExtra("花花世界", systemImage: "leaf.fill") {
            PopoverView()
                .environmentObject(settings)
        }
        .menuBarExtraStyle(.window)
    }
}
