# rn-quick-actions

[Quick Actions(iOS)](https://developer.apple.com/design/human-interface-guidelines/ios/system-capabilities/home-screen-actions/) & [App Shortcuts(Android)](https://developer.android.com/guide/topics/ui/shortcuts/creating-shortcuts) for React Native

## Installation

```bash
yarn add rn-quick-actions
```

## Setup

### iOS

If you are using cocoapods - you may need to run `pod install` (from `ios` directory).

On iOS, Quick Actions are handled by the
[`application:performActionForShortcutItem:completionHandler`](https://developer.apple.com/documentation/uikit/uiapplicationdelegate/1622935-application?language=objc)
method of your app's [`UIApplicationDelegate`](https://developer.apple.com/documentation/uikit/uiapplicationdelegate) (i.e. `AppDelegate.m`),
so, you will therefore need to add the following code in your
project's [`AppDelegate.m`](./example/ios/ShortcutsExample/AppDelegate.m)) file.

```objective-c
// add on top of the file
#import "RNShortcuts.h"
// ...

- (void)application:(UIApplication *)application performActionForShortcutItem:(UIApplicationShortcutItem *)shortcutItem completionHandler:(void (^)(BOOL))completionHandler {
    [RNShortcuts performActionForShortcutItem:shortcutItem completionHandler:completionHandler];
}
```

### Android

Android doesn't require any additional setup.

## Usage

### Documentation

#### Imports

```js
import Shortcuts from 'rn-quick-actions';
// if using typescript, can also use the 'ShortcutItem' type
import Shortcuts, { ShortcutItem } from 'rn-quick-actions';
```

#### Initial shortcut / action

Get the initial shortcut that the app was launched with. On iOS this will be returned just once, subsequent calls will return `null`.

```js
const shortcutItem = await Shortcuts.getInitialShortcut();
```

#### Listen for shortcut / action invocations

Listen to shortcut / action invocations while app is running.

On iOS the listener is also called for the initial
invocation, unless it was already received via `Shortcuts.getInitialShortcut()`.

```js
// 1. define the handler
function handler(item) {
  const { type, data } = item;
  // your handling logic
}

// 2. add the listener in a `useEffect` hook or `componentDidMount`
const sub = Shortcuts.onShortcutPressed(handler);

// 3. remove the listener in a `useEffect` hook or `componentWillUnmount`
sub.remove();
```

#### Set shortcuts

To set shortcuts (will replace existing dynamic actions / shortcuts)

```js
const shortcutItem = {
  id: 'my.awesome.action',
  title: 'Do awesome things',
  shortTitle: 'Do it',
  subtitle: 'iOS only',
  iconName: 'ic_awesome',
  symbolName: 'house.fill', // SF Symbol Name (iOS only)
  data: {
    foo: 'bar',
  },
};

Shortcuts.setShortcuts([shortcutItem]);

// you can also `await` to get the current dynamic shortcuts / actions
const shortcutItems = await Shortcuts.setShortcuts([shortcutItem]);
```

#### Clear shortcuts

Clears all dynamic shortcuts.

```js
Shortcuts.clearShortcuts();
```

#### Get shortcuts

Get the current shortcuts. Some information may be lost, such as iconName, data,
etc.

```js
const shortcutItems = await Shortcuts.getShortcuts();
```

### Example

```js
import { useEffect } from 'react';
import Shortcuts from 'rn-quick-actions';
import { Scan, Search } from '@/components';

export default function useShortcuts() {
  useEffect(() => {
    const shortcutsItems = [
      {
        type: 'scan',
        title: ' Scan',
        iconName: 'md_scan',
        data: {},
      },
      {
        type: 'search',
        title: 'Search',
        iconName: 'md_search',
        data: {},
      },
    ];

    Shortcuts.setShortcuts(shortcutsItems.reverse());

    const handler = (item) => {
      const { type } = item || {};
      if (type === 'scan') {
        Scan();
      }
      if (type === 'search') {
        Search();
      }
    };

    const sub = Shortcuts.onShortcutPressed(handler);
    return () => {
      sub.remove();
    };
  }, []);
}
```

## Icons

To display icons with your shortcuts / actions you will need to add them to your
project. Once added use the name of your iOS asset or Android drawable as the
value for `iconName` above. You can also use SF Symbol Name like `house.fill`
or `globe.europe.africa` for `symbolName` above (iOS only). If `symbolName` is
filled, `iconName` is not taken into account.

### iOS - Asset catalog

Add new assets to your [Asset catalog](https://developer.apple.com/library/archive/documentation/ToolsLanguages/Conceptual/Xcode_Overview/AddingImages.html) by importing either `png` (scalar) or
`pdf` (vector) files.

Refer
[Custom Icons : Home Screen Quick Action Icon
Size](https://developer.apple.com/design/human-interface-guidelines/home-screen-quick-actions)
to learn about the dimensions and design specifications.

### Android - drawable

Add [drawable resources](https://developer.android.com/studio/write/resource-manager) to you Android project. In Android studio, choose:

- for vector icons (SVG / PDF): **File > New > Vector Asset**

- for scalar icons (PNG): **File > New > Image Asset**

Refer
[App Shortcuts: Icon design
guidelines](https://commondatastorage.googleapis.com/androiddevelopers/shareables/design/app-shortcuts-design-guidelines.pdf)
to learn about the dimensions and design specifications.

## Contribution

If you want to add some features, feel free to submit PR. See the [contributing guide](CONTRIBUTING.md) to learn how to contribute to the repository and the development workflow.

## License

[MIT](LICENSE).
