import SwiftUI
import WebKit

// WKWebView subclass that ensures proper IME/input method support
// in the menu bar popover window
class IMEWebView: WKWebView {
    override var acceptsFirstResponder: Bool { true }

    override func becomeFirstResponder() -> Bool {
        let result = super.becomeFirstResponder()
        // Force text input context to update for IME
        if let context = self.inputContext {
            context.activate()
        }
        return result
    }

    override func viewDidMoveToWindow() {
        super.viewDidMoveToWindow()
        if let window = window {
            // Ensure the webview window is key-capable for IME
            DispatchQueue.main.async { [weak self] in
                self?.window?.makeFirstResponder(self)
            }
        }
    }
}

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

        let webView = IMEWebView(frame: .zero, configuration: config)
        webView.setValue(false, forKey: "drawsBackground")

        // Load the bundled HTML file
        if let url = Bundle.main.url(forResource: "index", withExtension: "html") {
            webView.loadFileURL(url, allowingReadAccessTo: url.deletingLastPathComponent())
        }

        // Make the webview first responder for proper IME handling
        DispatchQueue.main.async {
            webView.window?.makeFirstResponder(webView)
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
