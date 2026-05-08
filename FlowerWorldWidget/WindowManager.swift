import SwiftUI

class WindowManager: NSObject {
    static let shared = WindowManager()
    private var window: NSWindow?

    func show(settings: SettingsViewModel) {
        if let existingWindow = window {
            existingWindow.makeKeyAndOrderFront(nil)
            NSApp.activate(ignoringOtherApps: true)
            return
        }

        let contentView = PopoverView()
            .environmentObject(settings)
            .frame(minWidth: 320, minHeight: 400)

        let hostingController = NSHostingController(rootView: contentView)

        let win = NSWindow(contentViewController: hostingController)
        win.setContentSize(NSSize(width: 420, height: 600))
        win.styleMask = [.titled, .closable, .miniaturizable, .resizable]
        win.titlebarAppearsTransparent = true
        win.isMovableByWindowBackground = true
        win.center()
        win.title = ""
        win.isReleasedWhenClosed = false
        win.delegate = self

        window = win
        win.makeKeyAndOrderFront(nil)
        NSApp.activate(ignoringOtherApps: true)
    }
}

extension WindowManager: NSWindowDelegate {
    func windowShouldClose(_ sender: NSWindow) -> Bool {
        sender.orderOut(nil)
        return false
    }
}
