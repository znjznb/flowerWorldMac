import SwiftUI

struct PopoverView: View {
    @EnvironmentObject var settings: SettingsViewModel
    @State private var showingSettings = false

    var body: some View {
        VStack(spacing: 0) {
            // Title bar
            HStack {
                Image(systemName: "leaf.fill")
                    .foregroundColor(.green)
                Text("花花世界")
                    .font(.headline)
                Spacer()
                Button {
                    showingSettings.toggle()
                } label: {
                    Image(systemName: "gearshape")
                        .font(.system(size: 12))
                }
                .buttonStyle(.plain)
                .help("设置")
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 10)

            Divider()

            // WebView takes remaining space
            WebViewWrapper(backendURL: settings.apiBase)
        }
        .frame(width: 380, height: 520)
        .sheet(isPresented: $showingSettings) {
            SettingsView()
                .environmentObject(settings)
        }
    }
}
