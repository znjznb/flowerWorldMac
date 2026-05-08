import SwiftUI

struct PopoverView: View {
    @EnvironmentObject var settings: SettingsViewModel
    @State private var showingSettings = false
    @State private var pendingURL: String = ""

    var body: some View {
        VStack(spacing: 0) {
            // Title bar
            HStack {
                Image(systemName: "leaf.fill")
                    .foregroundColor(.green)
                Text("花花世界")
                    .font(.headline)
                Spacer()
                if showingSettings {
                    Button("完成") {
                        settings.backendURL = pendingURL
                        showingSettings = false
                    }
                    .buttonStyle(.plain)
                    .foregroundColor(.accentColor)
                } else {
                    HStack(spacing: 4) {
                        Button {
                            pendingURL = settings.backendURL
                            showingSettings.toggle()
                        } label: {
                            Image(systemName: "gearshape")
                                .font(.system(size: 12))
                        }
                        .buttonStyle(.plain)
                        .help("设置")

                        Button {
                            NSApplication.shared.terminate(nil)
                        } label: {
                            Image(systemName: "power")
                                .font(.system(size: 11))
                        }
                        .buttonStyle(.plain)
                        .foregroundColor(.secondary)
                        .help("退出")
                    }
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 10)

            Divider()

            if showingSettings {
                SettingsView(url: $pendingURL)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                WebViewWrapper(backendURL: settings.apiBase)
            }
        }
        .frame(width: 380, height: 520)
    }
}
