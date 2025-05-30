# About

Inferno is a browser for Android based off of Mozilla's geckoView for Android, Mozilla's Android
Components, and built from Mozilla's Android Fenix browser. We're not a firefox fork like most other
browsers, but instead a firefox-based browser using Mozilla's Android Components. Although this
means more code to write initially, it allows for more customizability while maintaining similarity
to firefox, and less maintenance in the long term since we don't have to merge with changes made to
firefox for Android, which is nice.

# Features

- tabs (finally for firefox-mobile based apps)
- color customization / themes
    - preset themes and custom themes
- enhanced privacy (mozilla telemetry has been removed)

# Upcoming features:

- privacy
    - mozilla telemetry disabled
- theme (slick ui with lots of preference options)
- lots of customizability in general

that's it for now, hopefully this project doesn't die

# Currently Under Development

## Settings Pages Status

- [ ] Toolbar (Buggy)
  - [ ] reorder not working
- [ ] Tabs (Buggy)
- [ ] Search (Buggy)
- [ ] Theme (Buggy)

- [ ] Gestures (Buggy)
- [ ] Homepage (Buggy)
- [ ] OnQuit (Buggy)
- [ ] Passwords (Buggy)
  - [ ] side note: ui good, passwords not saving though when added
  - [ ] side note: exceptions not tested
  - [ ] side note: getting this error message:
```
Autofill popup isn't shown because autofill is not available.
                                                                                                    
Did you set up autofill?
1. Go to Settings > System > Languages&input > Advanced > Autofill Service
2. Pick a service

Did you add an account?
1. Go to Settings > System > Languages&input > Advanced
2. Click on the settings icon next to the Autofill Service
3. Add your account
```
- [x] Autofill (Working)

- [x] Site Permissions (Working)
  - [ ] side note: exceptions not tested
  - [ ] side note: prefs not tested (access/set in engine)
- [x] Accessibility (Working)
    - [ ] side note: some settings not applied
    - [ ] side note: slide bar looks weird (no thumb) and factor may be wrong (try 0.5-2 instead of
      50-200)
- [x] Language (Working)
- [x] Translations (Working)
    - [ ] side note: exceptions page (never translate) not tested yet

- [x] Privacy and Security (Working)

- [ ] Other stuff
    - [ ] make sure to check setting usage, still have not completed migration from android prefs to
      datastore

## Massiv Bugs

- [ ] settings
    - [ ] settings managers wrong spacing for expandable items (addressManager, passwordManager,
      etc)
    - [ ] outlined edit text taking up waaay too much space
        - [ ] probably source of weird looking ui in EditThemeDialog and PreferenceSwitch
        - [ ] for PreferenceSwitch source of error may be setting leadingIcon = {}, try setting to
          null, also try removing anchor. Setting leadingIcon = {} happens in a lot of places, try
          removing where useful if it works
    - [ ] a couple of settings pages just crash, namely passwords, among others
    - [ ] dialog buttons have no padding
    - [ ] Language settings
        - [ ] default item looks weird, looks like 2 items (replace language name with use system
          default (recommended) title)

## Under Construction

- [ ] more customization settings
    - [ ] enable / disable tab bar
    - [ ] customize toolbar items
    - [ ] toolbar and tabs position (top / bottom for each, which is above and below in case both on
      same side)
- [ ] tabs
    - [ ] use icons storage for favicon square (persisted)
- [ ] biometric
    - [ ] for components that depend on biometric/auth, you could create a fragment in
      compose (as shown [here](https://stackoverflow.com/a/71480760/14642303)), that listens for
      biometric success/fail callbacks, and takes composables as children. Put composables that
      require biometric here with callbacks for biometric/auth events. This removes the need to
      create deeply nested callbacks
    - [ ] so far components that depend on biometric/pin auth are:
        - [ ] dialog feature in browser component
        - [ ] creditCardManager in settings
        - [ ] loginManager in settings
- [ ] autofill settings page
    - [ ] show some settings depending on login state (sync cards, sync logins, etc.)
    - [ ] use authentication when click on manage cards
    - [ ] use authentication when click on manage logins


## Pending addition

`Features that will be added eventually, but are currently not a priority`

- [ ] more toolbar items
    - [ ] go to passwords page (key with bottom right profile view) (requires auth, consider setting
      a timeout like bitwarden does)
    - [ ] extensions settings direct access


# Future Features

`Features that will probably eventually be added`

- [ ] workspaces
    - Requires storing multiple engines probably, one for each workspace. Should check zen browser implementation for ideas.
- [ ] synced workspaces
    - [ ] will require a workaround, since vanilla Firefox doesn't even support workspaces currently
        - [ ] Could store / encode in bookmarks folder, there is a max of 5000 however. If implemented this way, bookmarks shown would have to hide special storage folders, and they would be visible if accessed from a Firefox browser. This would be VERY hacky, but there are a lot of fields to store different types of data, and it could be done as cleanly as possible, maybe storing everything in a base folder called "_". It would also need to be implemented in a way that if edited externally, it doesn't just completely break.
        - [ ] if implemented this way, it would also allow just pinned tabs to be synced, etc
- [ ] bug reporting system / feedback page
    - [ ] for bug reporting show select in settings: (disabled, send report auto, or ask to send
      report)
- [ ] login manager (settings)
    - [ ] add search function to filter logins, implemented by moz in [SavedLoginsFragment]
    - [ ] add login sorting and save selected in prefs
    - [ ] find duplicates? not sure how necessary
    - [ ] add exceptions page
- [ ] site permissions settings page
    - [ ] exceptions, reference moz implementation in `SitePermissionsExceptionsFragment` (
      for clearing site permissions) and `SitePermissionsDetailsExceptionsFragment` (for
      setting individual site permissions)
    - [ ] in exceptions fragment show each site as expandable item, when expanded shows individual
      settings and clear permissions on this site, clear permissions for all sites is at the bottom
      and requires dialog to confirm
- [ ] enhanced tracking protection
    - [ ] exceptions