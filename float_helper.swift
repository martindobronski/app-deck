import Cocoa

let markerPath = "/tmp/appdeck-activate.txt"
let posPath = "/tmp/appdeck-pos.txt"
let pidPath = "/tmp/appdeck-float.pid"

func enforceSingleInstance() {
    if let existing = try? String(contentsOfFile: pidPath, encoding: .utf8).trimmingCharacters(in: .whitespacesAndNewlines),
       let pid = Int32(existing), pid != ProcessInfo.processInfo.processIdentifier {
        kill(pid, SIGKILL)
    }
    try? "\(ProcessInfo.processInfo.processIdentifier)".write(toFile: pidPath, atomically: true, encoding: .utf8)
}

func cleanupPidFile() {
    try? FileManager.default.removeItem(atPath: pidPath)
}

class DragView: NSView {
    var onClick: (() -> Void)?
    var onDragEnd: ((NSPoint) -> Void)?
    private var dragStart: NSPoint = .zero
    private var windowStart: NSPoint = .zero
    private var dragging = false
    private var trackingArea: NSTrackingArea?
    private var popup: NSPanel?
    private var timeLabel: NSTextField?
    private var hoverTimer: Timer?

    override func mouseDown(with event: NSEvent) {
        dragStart = event.locationInWindow
        windowStart = window!.frame.origin
        dragging = false
        hidePopup()
    }

    override func mouseDragged(with event: NSEvent) {
        dragging = true
        let cur = event.locationInWindow
        window!.setFrameOrigin(NSPoint(
            x: window!.frame.origin.x + cur.x - dragStart.x,
            y: window!.frame.origin.y + cur.y - dragStart.y
        ))
        hidePopup()
    }

    override func mouseUp(with event: NSEvent) {
        if dragging {
            let finalOrigin = window!.frame.origin
            onDragEnd?(finalOrigin)
        } else {
            onClick?()
            return
        }
        dragging = false
        let mouseLoc = event.locationInWindow
        if bounds.contains(mouseLoc) {
            showPopup()
        }
    }

    override func updateTrackingAreas() {
        super.updateTrackingAreas()
        if let ta = trackingArea {
            removeTrackingArea(ta)
        }
        trackingArea = NSTrackingArea(
            rect: bounds,
            options: [.mouseEnteredAndExited, .activeAlways],
            owner: self,
            userInfo: nil
        )
        addTrackingArea(trackingArea!)
    }

    override func mouseEntered(with event: NSEvent) {
        if !dragging {
            showPopup()
        }
    }

    override func mouseExited(with event: NSEvent) {
    }

    // --- Popup ---

    func showPopup() {
        guard window != nil else { return }
        if popup == nil { createPopup() }
        updatePopupContent()
        positionPopup()
        popup?.orderFrontRegardless()
        startHoverTimer()
    }

    func hidePopup() {
        hoverTimer?.invalidate()
        hoverTimer = nil
        popup?.orderOut(nil)
        popup = nil
        timeLabel = nil
    }

    func createPopup() {
        let panel = NSPanel(
            contentRect: NSRect(x: 0, y: 0, width: 180, height: 100),
            styleMask: [.borderless, .nonactivatingPanel],
            backing: .buffered,
            defer: false
        )
        panel.isOpaque = false
        panel.backgroundColor = .clear
        panel.hasShadow = true
        panel.level = .screenSaver
        panel.collectionBehavior = [.canJoinAllSpaces, .stationary]
        panel.ignoresMouseEvents = true

        let container = NSView(frame: NSRect(x: 0, y: 0, width: 180, height: 100))
        container.wantsLayer = true
        container.layer?.cornerRadius = 8
        container.layer?.backgroundColor = NSColor(calibratedWhite: 0.15, alpha: 0.92).cgColor

        let label = NSTextField(frame: NSRect(x: 0, y: 14, width: 180, height: 72))
        label.isEditable = false
        label.isSelectable = false
        label.isBordered = false
        label.drawsBackground = false
        label.textColor = .white
        label.font = NSFont.systemFont(ofSize: 18, weight: .medium)
        label.alignment = .center
        label.lineBreakMode = .byWordWrapping

        container.addSubview(label)
        panel.contentView = container
        popup = panel
        timeLabel = label
    }

    func updatePopupContent() {
        let now = Date()
        let df = DateFormatter()
        df.locale = Locale(identifier: "de_DE")

        df.dateFormat = "EEEE"
        let dayStr = df.string(from: now)

        df.dateFormat = "dd.MM.yy"
        let dateStr = df.string(from: now)

        df.dateFormat = "HH:mm"
        let timeStr = df.string(from: now)

        timeLabel?.stringValue = "\(dayStr)\n\(dateStr)\n\(timeStr)"

        let font = NSFont.systemFont(ofSize: 18, weight: .medium)
        let size = timeLabel?.stringValue.boundingRect(
            with: NSSize(width: 180, height: 140),
            options: [.usesLineFragmentOrigin],
            attributes: [.font: font as Any]
        ).size ?? NSSize(width: 170, height: 75)
        let w = max(170, size.width + 28)
        let h = max(90, size.height + 28)
        popup?.setContentSize(NSSize(width: w, height: h))
        timeLabel?.frame = NSRect(x: 0, y: 14, width: w, height: h - 28)
    }

    func positionPopup() {
        guard let win = window else { return }
        let winFrame = win.frame
        let popupSize = popup?.frame.size ?? NSSize(width: 180, height: 100)
        let x = winFrame.midX - popupSize.width / 2
        var y = winFrame.maxY + 8
        if let screen = NSScreen.main?.visibleFrame {
            if y + popupSize.height > screen.maxY {
                y = winFrame.minY - popupSize.height - 8
            }
        }
        popup?.setFrameOrigin(NSPoint(x: x, y: y))
    }

    // --- Hover timer ---

    func startHoverTimer() {
        hoverTimer?.invalidate()
        hoverTimer = Timer.scheduledTimer(withTimeInterval: 0.2, repeats: true) { [weak self] _ in
            guard let self = self, let win = self.window else { return }
            let mouseLoc = NSEvent.mouseLocation
            let iconFrame = win.frame
            var hotZone = iconFrame.insetBy(dx: -15, dy: -15)
            if let popupFrame = self.popup?.frame {
                hotZone = hotZone.union(popupFrame)
            }
            if hotZone.contains(mouseLoc) {
                self.updatePopupContent()
            } else {
                self.hidePopup()
            }
        }
    }
}

class AppDelegate: NSObject, NSApplicationDelegate {
    var window: NSWindow!
    var bgColor: NSColor = NSColor(calibratedWhite: 0.18, alpha: 0.92)

    func applicationDidFinishLaunching(_ notification: Notification) {
        enforceSingleInstance()

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

        NotificationCenter.default.addObserver(
            self, selector: #selector(handleTerminate),
            name: NSApplication.willTerminateNotification, object: nil
        )
    }

    @objc func handleTerminate() {
        cleanupPidFile()
    }
}

let app = NSApplication.shared
let delegate = AppDelegate()
app.delegate = delegate
app.setActivationPolicy(.accessory)
app.run()
