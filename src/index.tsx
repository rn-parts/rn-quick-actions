import { NativeModules, EmitterSubscription, NativeEventEmitter } from 'react-native';

export interface ShortcutItem {
  /**
   * Unique string used to identify the type of the action
   */
  id: string;

  /**
   * On Android - it's recommended to keep this under 25 characters. If there
   * isn't enough space to display this, fallsback to `shortTitle`
   */
  title: string;

  /**
   * Android only, max 10 characters recommended. This is displayed instead of
   * `title` when there is not enough space to display the title.
   */
  shortTitle?: string;

  /**
   * iOS only, ignored on Android
   */
  subtitle?: string; // only used on iOS

  /**
   * The name of the iOS Asset or Android drawable
   */
  iconName?: string;

  /**
   * [Android] The name of the person
   */
  personName?: string;

  /**
   * [Android] The url to image for person
   */
  personIcon?: string;

  /**
   * [Android] Is long-lived
   */
  longLived?: boolean;

  /**
   * Custom payload for the action
   */
  data?: any;
}

const { RNShortcuts } = NativeModules;

const ShortcutsEmitter = new NativeEventEmitter(RNShortcuts);

class Shortcuts {
  /**
   * Set the shortcut items.
   * @returns a promise with the items that were set
   */
  setShortcuts(items: ShortcutItem[]): Promise<ShortcutItem[]> {
    return RNShortcuts.setShortcuts(items);
  }

  /**
   * Add the shortcut item.
   * @returns a promise with boolean
   */
  addShortcut(shortcutItem: ShortcutItem): Promise<boolean> {
    return RNShortcuts.addShortcut(shortcutItem);
  }

  /**
   * @returns a promise with the items that were set
   */
  getShortcuts(): Promise<ShortcutItem[]> {
    return RNShortcuts.getShortcuts();
  }

  /**
   * Removes all the shortcut items
   */
  clearShortcuts(): void {
    return RNShortcuts.clearShortcuts();
  }

  /**
   * Gets the initial shortcut the app was launched with
   */
  getInitialShortcut(): Promise<ShortcutItem | null> {
    return RNShortcuts.getInitialShortcut();
  }

  /**
   * Listens for new shortcut events
   */
  onShortcutPressed(listener: (shortcut: ShortcutItem) => void): EmitterSubscription {
    return ShortcutsEmitter.addListener('onShortcutItemPressed', listener);
  }
}

export default new Shortcuts();
