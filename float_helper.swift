import Cocoa

let markerPath = "/tmp/appdeck-activate.txt"
let posPath = "/tmp/appdeck-pos.txt"

class DragView: NSView {
    var onClick: (() -> Void)?
    var onDragEnd: ((NSPoint) -> Void)?
    private var dragStart: NSPoint = .zero
    private var windowStart: NSPoint = .zero
    private var dragging = false

    override func mouseDown(with event: NSEvent) {
        dragStart = event.locationInWindow
        windowStart = window!.frame.origin
        dragging = false
    }

    override func mouseDragged(with event: NSEvent) {
        dragging = true
        let cur = event.locationInWindow
        window!.setFrameOrigin(NSPoint(
            x: window!.frame.origin.x + cur.x - dragStart.x,
            y: window!.frame.origin.y + cur.y - dragStart.y
        ))
    }

    override func mouseUp(with event: NSEvent) {
        if dragging {
            let finalOrigin = window!.frame.origin
            onDragEnd?(finalOrigin)
        } else {
            onClick?()
        }
    }
}

class AppDelegate: NSObject, NSApplicationDelegate {
    var window: NSWindow!
    var bgColor: NSColor = NSColor(calibratedWhite: 0.18, alpha: 0.92)

    func applicationDidFinishLaunching(_ notification: Notification) {
        let iconPath: String
        var posX: CGFloat = -1
        var posY: CGFloat = -1
        if CommandLine.arguments.count > 1 {
            iconPath = CommandLine.arguments[1]
        } else {
            iconPath = Bundle.main.resourcePath! + "/app-icon.png"
        }
        if CommandLine.arguments.count > 3 {
            posX = CGFloat(Float(CommandLine.arguments[2]) ?? -1)
            posY = CGFloat(Float(CommandLine.arguments[3]) ?? -1)
        }

        let screenRect = NSScreen.main?.visibleFrame ?? NSRect(x: 0, y: 0, width: 1440, height: 900)
        let winSize = NSSize(width: 48, height: 48)
        let origin: NSPoint
        if posX >= 0 && posY >= 0 {
            origin = NSPoint(x: posX, y: posY)
        } else {
            origin = NSPoint(
                x: screenRect.maxX - winSize.width - 24,
                y: screenRect.minY + 12
            )
        }

        window = NSWindow(
            contentRect: NSRect(origin: origin, size: winSize),
            styleMask: .borderless,
            backing: .buffered,
            defer: false
        )
        window.level = .screenSaver
        window.backgroundColor = .clear
        window.isOpaque = false
        window.ignoresMouseEvents = false
        window.hasShadow = false
        window.collectionBehavior = [.canJoinAllSpaces, .stationary]

        let container = NSView(frame: NSRect(origin: .zero, size: winSize))
        container.wantsLayer = true
        container.layer?.cornerRadius = 10
        container.layer?.masksToBounds = true
        container.layer?.backgroundColor = bgColor.cgColor

        if let iconImage = NSImage(contentsOfFile: iconPath) {
            let imgView = NSImageView(frame: NSRect(x: 8, y: 8, width: 32, height: 32))
            imgView.image = iconImage
            container.addSubview(imgView)
        }

        let dragView = DragView(frame: NSRect(origin: .zero, size: winSize))
        dragView.onClick = {
            do {
                try "\(Date().timeIntervalSince1970)".write(toFile: markerPath, atomically: true, encoding: .utf8)
            } catch {}
            NSApp.terminate(nil)
        }
        dragView.onDragEnd = { pt in
            let s = "\(Int(pt.x)),\(Int(pt.y))"
            try? s.write(toFile: posPath, atomically: true, encoding: .utf8)
        }
        container.addSubview(dragView)

        window.contentView = container
        window.orderFrontRegardless()
    }
}

let app = NSApplication.shared
let delegate = AppDelegate()
app.delegate = delegate
app.setActivationPolicy(.accessory)
app.run()
