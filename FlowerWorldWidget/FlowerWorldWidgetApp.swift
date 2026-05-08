import SwiftUI

@main
struct FlowerWorldWidgetApp: App {
    @StateObject private var settings = SettingsViewModel()

    var body: some Scene {
        MenuBarExtra("花花世界", systemImage: "leaf.fill") {
            Button("打开花花世界") {
                WindowManager.shared.show(settings: settings)
            }

            Divider()

            Button("退出") {
                NSApplication.shared.terminate(nil)
            }
        }
        .menuBarExtraStyle(.menu)
    }
}
