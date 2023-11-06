@objc(RNShortcuts)
public class RNShortcuts: RCTEventEmitter {

    let onShortcutItemPressed = "onShortcutItemPressed"

    public override func startObserving() {
        Shortcuts.shared.delegate = self
    }

    public override func stopObserving() {
        Shortcuts.shared.delegate = nil
    }

    public override func supportedEvents() -> [String]! {
        return [
            onShortcutItemPressed
        ]
    }

    public override class func requiresMainQueueSetup() -> Bool {
        return true
    }
    
    @objc(addShortcut:resolve:reject:)
    public func addShortcut(shortcutItem: [String: Any],
                            resolve: @escaping RCTPromiseResolveBlock,
                            reject: @escaping RCTPromiseRejectBlock) {
        Shortcuts.shared.getShortcuts { (shorcutItems) in
            var existing = shorcutItems ?? [];
            existing.insert(shortcutItem, at: 0);
            
            var buffer = Array<ShortcutItem>()
            var added = Set<String>()
            for elem in existing {
                if !added.contains((elem["id"] ?? elem["type"]) as! String) {
                    buffer.append(elem)
                    added.insert((elem["id"] ?? elem["type"]) as! String)
                }
            }
            
            existing = buffer
            
            do {
                let shortcutItems = try Shortcuts.shared.setShortcuts(existing)
                resolve(shortcutItems)
            } catch {
                let error = NSError(domain: "RNShortcuts", code: 1)
                reject("1", "Unable to set shortcuts", error)
            }
        }
    }

    @objc(setShortcuts:resolve:reject:)
    public func setShortcuts(shortcutItems: [[String: Any]],
                             resolve: RCTPromiseResolveBlock,
                             reject: RCTPromiseRejectBlock) {
        do {
            let shortcutItems = try Shortcuts.shared.setShortcuts(shortcutItems)
            resolve(shortcutItems)
        } catch {
            let error = NSError(domain: "RNShortcuts", code: 1)
            reject("1", "Unable to set shortcuts", error)
        }

    }

    @objc(getShortcuts:reject:)
    public func getShortcuts(resolve: @escaping RCTPromiseResolveBlock, reject: RCTPromiseRejectBlock) {
        Shortcuts.shared.getShortcuts { (shorcutItems) in
            resolve(shorcutItems)
        }
    }

    @objc
    public func clearShortcuts() {
        Shortcuts.shared.clearShortcuts()
    }

    @objc(getInitialShortcut:reject:)
    public func getInitialShortcut(resolve: @escaping RCTPromiseResolveBlock, reject: RCTPromiseRejectBlock) {
        resolve(Shortcuts.shared.getInitialShortcut())
    }

    @objc
    public class func performActionForShortcutItem(_ shortcutItem: UIApplicationShortcutItem,
                                                   completionHandler: (Bool) ->Void) {
        Shortcuts.shared.performAction(forShortcutItem: shortcutItem)
    }
}

extension RNShortcuts: ShortcutsDelegate {
    func onShortcutItemPressed(_ item: ShortcutItem) {
        sendEvent(withName: onShortcutItemPressed, body: item)
    }
}
