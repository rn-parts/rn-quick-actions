import {
  NativeModules,
  EmitterSubscription,
  NativeEventEmitter,
  Platform,
} from 'react-native';

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

  //shortLabel

  /**
   * iOS only, ignored on Android
   */
  subtitle?: string; // only used on iOS

  /**
   * The name of the iOS Asset or Android drawable
   */
  iconName?: string;

  /**
   * The name of the iOS SF Symbol Name
   */
  symbolName?: string;

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

interface ShortcutItemIOS extends Omit<ShortcutItem, 'id'> {
  type: string;
}

/**
 * Maps a ShortcutItem to ShortcutItemIOS by converting id to type
 */
export const mapToIOSShortcut = (item: ShortcutItem): ShortcutItemIOS => {
  const { id, ...rest } = item;
  return {
    ...rest,
    type: id,
  };
};

/**
 * Maps a ShortcutItemIOS to ShortcutItem by converting type to id
 */
export const mapFromIOSShortcut = (item: ShortcutItemIOS): ShortcutItem => {
  const { type, ...rest } = item;
  return {
    ...rest,
    id: type,
  };
};

const { RNShortcuts } = NativeModules;

const ShortcutsEmitter = new NativeEventEmitter(RNShortcuts);

class Shortcuts {
  /**
   * Set the shortcut items.
   * @returns a promise with the items that were set
   */
  setShortcuts(items: ShortcutItem[]): Promise<ShortcutItem[]> {
    if (Platform.OS === 'ios') {
      const iosItems = items.map(mapToIOSShortcut);
      return RNShortcuts.setShortcuts(
        iosItems
      ).then((result: ShortcutItemIOS[]) => result.map(mapFromIOSShortcut));
    }
    return RNShortcuts.setShortcuts(items);
  }

  /**
   * Add the shortcut item.
   * @returns a promise with boolean
   */
  addShortcut(shortcutItem: ShortcutItem): Promise<boolean> {
    if (Platform.OS === 'ios') {
      const iosItem = mapToIOSShortcut(shortcutItem);
      return RNShortcuts.addShortcut(iosItem);
    }
    return RNShortcuts.addShortcut(shortcutItem);
  }

  /**
   * @returns a promise with the items that were set
   */
  getShortcuts(): Promise<ShortcutItem[]> {
    if (Platform.OS === 'ios') {
      return RNShortcuts.getShortcuts().then((result: ShortcutItemIOS[]) =>
        result.map(mapFromIOSShortcut)
      );
    }
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
    if (Platform.OS === 'ios') {
      return RNShortcuts.getInitialShortcut().then(
        (result: ShortcutItemIOS | null) =>
          result ? mapFromIOSShortcut(result) : null
      );
    }
    return RNShortcuts.getInitialShortcut();
  }

  /**
   * Listens for new shortcut events
   */
  onShortcutPressed(
    handler: (shortcut: ShortcutItem) => void
  ): EmitterSubscription {
    if (Platform.OS === 'ios') {
      return ShortcutsEmitter.addListener(
        'onShortcutItemPressed',
        (shortcut: ShortcutItemIOS) => {
          handler(mapFromIOSShortcut(shortcut));
        }
      );
    }
    return ShortcutsEmitter.addListener('onShortcutItemPressed', handler);
  }
}

export default new Shortcuts();
