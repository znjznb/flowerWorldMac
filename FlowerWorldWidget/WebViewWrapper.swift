import SwiftUI
import WebKit

struct WebViewWrapper: NSViewRepresentable {
    let backendURL: String

    func makeNSView(context: Context) -> WKWebView {
        let config = WKWebViewConfiguration()
        let userContentController = WKUserContentController()

        // Allow requests from local file to HTTP servers
        config.preferences.setValue(true, forKey: "allowFileAccessFromFileURLs")
        config.setValue(true, forKey: "allowUniversalAccessFromFileURLs")

        // Inject backend URL at document start (before page scripts run)
        let js = "window.__API_BASE__ = '\(backendURL)';"
        let script = WKUserScript(source: js, injectionTime: .atDocumentStart, forMainFrameOnly: false)
        userContentController.addUserScript(script)

        config.userContentController = userContentController

        let webView = WKWebView(frame: .zero, configuration: config)
        webView.setValue(false, forKey: "drawsBackground")

        // Load the bundled HTML file
        if let url = Bundle.main.url(forResource: "index", withExtension: "html") {
            webView.loadFileURL(url, allowingReadAccessTo: url.deletingLastPathComponent())
        }

        return webView
    }

    func updateNSView(_ nsView: WKWebView, context: Context) {
        guard context.coordinator.currentBackendURL != backendURL else { return }
        context.coordinator.currentBackendURL = backendURL
        let js = "window.__API_BASE__ = '\(backendURL)';"
        nsView.evaluateJavaScript(js) { _, _ in
            nsView.reload()
        }
    }

    func makeCoordinator() -> Coordinator {
        Coordinator(backendURL: backendURL)
    }

    class Coordinator: NSObject {
        var currentBackendURL: String
        init(backendURL: String) {
            self.currentBackendURL = backendURL
        }
    }
}
