import SwiftUI

struct SettingsView: View {
    @Binding var url: String

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            VStack(alignment: .leading, spacing: 6) {
                Text("后端服务地址")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                TextField("http://localhost:8080", text: $url)
                    .textFieldStyle(.roundedBorder)
                    .frame(maxWidth: .infinity)
            }

            Text("Java 后端默认运行在 http://localhost:8080。如果你的后端在其他端口或地址，请在此修改。")
                .font(.caption)
                .foregroundColor(.secondary)
                .fixedSize(horizontal: false, vertical: true)

            Spacer()
        }
        .padding(20)
    }
}
