# Documentation reader

The book card beside the application name opens ArchWiki. It is a shared reader,
not a session property. Closing/reopening the window retains the current article,
back/forward history and scroll position for the current app process. Moving
between environment documentation and ArchWiki keeps separate page snapshots.
Activity destruction releases the WebView; configuration recreation can restore
its in-memory snapshot. Finishing the app clears these snapshots. No browsing
history is written to the workspace backup or terminal environment.

Programs exposes **Documentation** at the bottom left, opposite **Back**. This
opens the selected environment's official documentation without running package
checks or closing Programs. The ten sources cover Termux, Ubuntu, Debian,
Alpine, Fedora, openSUSE, Void, Arch Linux ARM, Kali and Manjaro. Ubuntu AI uses
the Ubuntu documentation source only; this does not redirect execution/rootfs.

RU/EN selects the official section where available. A source without a reviewed
Russian section explicitly shows the English original; the app does not invent
translations. ArchWiki has native article search; other sources use find-on-page
in addition to the website's own navigation/search. Reload retrieves the current
website. Website availability and translations remain controlled by the source.

The reader needs Android WebView. A missing/crashed provider or network/TLS error
produces an explanation and an external-browser route. Internal navigation accepts HTTPS links across sites in the same reader.
The displayed host follows the page, and non-source pages are labelled as web
pages rather than official documentation. Long-press a link for Copy link / Open
in browser; automatic external intents are blocked. Local file/content access, JavaScript bridges, mixed content, file
upload, website permission grants, third-party cookies and automatic popups are
disabled. TLS errors are cancelled. JavaScript and DOM storage support official
site navigation; page content never becomes a terminal command.

Phone WebView rendering, scroll/history fidelity, keyboard and rotation remain
device checks. Robolectric tests cover integration/state/security decisions,
not a real provider or Android gestures.

## First-open viewport correction (private test8)

The test7 AlertDialog custom-content wrapper could measure the weighted browser
host to height0, leaving a loaded page invisible until the window was reopened.
The reader now uses a themed Dialog with match-parent content and a FrameLayout
browser host. Navigation/history restoration and WebView resume wait for an
attached, measured viewport. A postVisualStateCallback redraw is scoped to the
current WebView/window/navigation; callbacks from a closed/replaced view cannot
redraw the new page. Closing preserves the retained page's in-flight network
load instead of cancelling it into a partial document. Destruction still stops
loading and destroys the WebView. No global WebView timers or guest processes
are changed. The original failure and corrected bounds are verified in isolated
Android layout tests; Chromium drawing still needs developer device acceptance.

## Arch forum in the Wiki reader (private test9)

The bordered Forum button sits immediately before Reload in the top toolbar.
It opens https://bbs.archlinux.org/ inside the retained Wiki WebView; Wiki and
forum links share that window's history and page retention until app close.
All HTTPS links in Wiki and environment documentation can stay in the same
reader, including non-source sites. File/content/intent/javascript and insecure
HTTP navigation remain blocked. The forum shortcut also waits for a usable
viewport if pressed before initial layout. Long-press opens bordered Copy link /
Open in browser actions; image links resolve the anchor URL rather than the
image URL. Stale asynchronous anchor lookups cannot open a menu in another
reader window.
TLS verification, disabled file access, denied file-chooser/permission requests
and third-party cookie restrictions remain unchanged. Actual phone rendering,
forum authentication and posting have not been validated by UI mocks.

## Agent chat links (private test9)

The existing link preview now offers Copy (left), Show (centre) and Browser
(right). Show opens the explicit HTTPS URL in the same embedded reader without
opening an external application. Its history is separate from Wiki and each
environment's documentation. A new explicit link overrides saved reader state
before any navigation starts; reopening a different link does not fetch the
previous link first. HTTP links retain Copy/Browser actions, with embedded Show
disabled; no general cleartext traffic permission or TLS bypass was introduced.
The same native link preview is shared by Codex, Claude, Gemini and OpenCode.
Markdown messages themselves retain native rendering and selectable text.

The fake Robolectric WebView provider delegates neither its View bounds nor
performLongClick to View. The documentation shadow routes those two operations
to Android View for layout/listener checks. Native touch recognition and Chromium
rendering are not simulated. Anchor lookup callbacks for image links are supplied
explicitly by tests to check href selection and stale-window rejection.
