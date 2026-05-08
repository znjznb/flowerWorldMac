import SwiftUI

struct SettingsView: View {
    @EnvironmentObject var settings: SettingsViewModel
    @Environment(\.dismiss) var dismiss

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("设置")
                .font(.title2)
                .fontWeight(.bold)

            VStack(alignment: .leading, spacing: 6) {
                Text("后端服务地址")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                TextField("http://localhost:8080", text: $settings.backendURL)
                    .textFieldStyle(.roundedBorder)
                    .frame(maxWidth: .infinity)
            }

            Text("Java 后端默认运行在 http://localhost:8080。如果你的后端在其他端口或地址，请在此修改。")
                .font(.caption)
                .foregroundColor(.secondary)
                .fixedSize(horizontal: false, vertical: true)

            HStack {
                Spacer()
                Button("关闭") {
                    dismiss()
                }
                .keyboardShortcut(.defaultAction)
            }
        }
        .padding(20)
        .frame(width: 380)
    }
}
